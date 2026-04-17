package com.integration.execution.it;

import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.service.KwGraphQLService;
import com.integration.execution.service.VaultService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for the Confluence execution pipeline.
 *
 * <p>Publishes a {@link ConfluenceExecutionCommand} to the Confluence command queue via
 * {@link MessagePublisher} and polls the result queue with {@link RabbitTemplate} to
 * verify that the full listener → orchestrator → publisher chain is wired correctly.
 *
 * <p>External dependencies ({@link KwGraphQLService}, {@link VaultService},
 * {@link ConfluenceApiClient}) are provided as mocks via {@link TestConfiguration}.
 */
@DisplayName("Confluence Execution Pipeline — integration tests")
class ConfluencePipelineIT extends AbstractIesIT {

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private KwGraphQLService kwGraphQLService;

    @Autowired
    private ConfluenceApiClient confluenceApiClient;

    @TestConfiguration
    static class MockDependencies {

        @Bean
        @Primary
        public KwGraphQLService kwGraphQLService() {
            return mock(KwGraphQLService.class);
        }

        @Bean
        @Primary
        public VaultService vaultService() {
            return mock(VaultService.class);
        }

        @Bean
        @Primary
        public ConfluenceApiClient confluenceApiClient() {
            return mock(ConfluenceApiClient.class);
        }
    }

    // ------------------------------------------------------------------ helpers

    private ConfluenceExecutionCommand buildCommand(final UUID jobId, final String tenantId,
            final String secretName) {
        return ConfluenceExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .integrationName("Confluence IT Integration")
                .jobExecutionId(jobId)
                .tenantId(tenantId)
                .connectionSecretName(secretName)
                .confluenceSpaceKey("IT")
                .confluenceSpaceKeyFolderKey("it-folder")
                .reportNameTemplate("IT Monitoring Report {date}")
                .dynamicDocumentType("MONITORING_DOC")
                .businessTimeZone("UTC")
                .triggeredBy(TriggerType.SCHEDULER)
                .triggeredByUser("it-confluence-user")
                .windowStart(Instant.now().minusSeconds(86400))
                .windowEnd(Instant.now())
                .build();
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("command with no monitoring data produces SUCCESS result with zero records")
    void fullPipeline_noMonitoringData_producesSuccessResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-empty";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isZero();
    }

    @Test
    @DisplayName("command with monitoring data publishes page and produces SUCCESS result")
    void fullPipeline_withMonitoringData_producesSuccessResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-data";
        String pageUrl = "https://mock-confluence.example.com/wiki/spaces/IT/pages/12345";
        String pageId = "12345";

        KwMonitoringDocument doc = new KwMonitoringDocument();

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(doc));
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(ZoneId.of("UTC"));
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluenceApiClient.ConfluencePublishResult(pageUrl, pageId));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant-data", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isEqualTo(1);
        assertThat(result.getExecutionMetadata()).isNotNull();
        assertThat(result.getExecutionMetadata()).containsEntry("confluencePageUrl", pageUrl);
    }

    @Test
    @DisplayName("orchestrator exception produces FAILED result on result queue")
    void fullPipeline_orchestratorException_producesFailedResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-fail";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Simulated Confluence Kaseware API failure"));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant-fail", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Simulated Confluence Kaseware API failure");
    }

    @Test
    @DisplayName("result message contains correct timing metadata")
    void fullPipeline_result_containsTimingMetadata() {
        UUID jobId = UUID.randomUUID();
        String secretName = "confluence-secret-timing";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        Instant beforePublish = Instant.now();
        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "confluence-tenant-timing", secretName));

        ConfluenceExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ConfluenceExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getStartedAt()).isAfterOrEqualTo(beforePublish.minusSeconds(1));
        assertThat(result.getCompletedAt()).isAfterOrEqualTo(result.getStartedAt());
    }

    @Test
    @DisplayName("multiple sequential Confluence commands produce results for all job IDs")
    void fullPipeline_multipleCommands_allResultsReceived() {
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        String secretName = "confluence-secret-multi";

        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId1, "confluence-tenant-multi", secretName));

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId2, "confluence-tenant-multi", secretName));

        List<UUID> receivedJobIds = await()
                .atMost(25, TimeUnit.SECONDS)
                .until(() -> {
                    java.util.List<UUID> collected = new java.util.ArrayList<>();
                    ConfluenceExecutionResult r1 = (ConfluenceExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
                    ConfluenceExecutionResult r2 = (ConfluenceExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE);
                    if (r1 != null) {
                        collected.add(r1.getJobExecutionId());
                    }
                    if (r2 != null) {
                        collected.add(r2.getJobExecutionId());
                    }
                    return collected;
                }, ids -> ids.size() == 2);

        assertThat(receivedJobIds).containsExactlyInAnyOrder(jobId1, jobId2);
    }
}

