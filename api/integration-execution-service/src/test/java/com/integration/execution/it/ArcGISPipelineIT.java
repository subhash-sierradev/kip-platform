package com.integration.execution.it;

import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.client.KwGraphqlClient;
import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.service.VaultService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for the ArcGIS execution pipeline.
 *
 * <p>Publishes an {@link ArcGISExecutionCommand} to the ArcGIS command queue via
 * {@link MessagePublisher} and polls the result queue with {@link RabbitTemplate} to
 * verify that the full listener → orchestrator → publisher chain is wired correctly.
 *
 * <p>External dependencies ({@link KwGraphqlClient}, {@link VaultService},
 * {@link ArcGISApiClient}) are provided as mocks via {@link TestConfiguration}.
 */
@DisplayName("ArcGIS Execution Pipeline — integration tests")
class ArcGISPipelineIT extends AbstractIesIT {

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private KwGraphqlClient kwGraphqlClient;

    @Autowired
    private VaultService vaultService;

    @TestConfiguration
    static class MockDependencies {

        @Bean
        @Primary
        public KwGraphqlClient kwGraphqlClient() {
            return mock(KwGraphqlClient.class);
        }

        @Bean
        @Primary
        public VaultService vaultService() {
            return mock(VaultService.class);
        }

        @Bean
        @Primary
        public ArcGISApiClient arcgisApiClient() {
            return mock(ArcGISApiClient.class);
        }
    }

    // ------------------------------------------------------------------ helpers

    private IntegrationSecret buildOAuthSecret() {
        return IntegrationSecret.builder()
                .baseUrl("https://mock-arcgis.example.com")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(OAuthClientCredential.builder()
                        .clientId("test-client")
                        .clientSecret("test-secret")
                        .tokenUrl("https://mock-arcgis.example.com/oauth/token")
                        .build())
                .build();
    }

    private ArcGISExecutionCommand buildCommand(final UUID jobId, final String tenantId,
            final String secretName) {
        return ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .integrationName("ArcGIS IT Integration")
                .jobExecutionId(jobId)
                .tenantId(tenantId)
                .connectionSecretName(secretName)
                .arcgisEndpointUrl("https://mock-arcgis.example.com/rest/services/test/FeatureServer/0")
                .itemType("Feature Layer")
                .itemSubtype("subtype")
                .fieldMappings(Collections.emptyList())
                .triggeredBy(TriggerType.SCHEDULER)
                .triggeredByUser("it-test-user")
                .windowStart(Instant.now().minusSeconds(3600))
                .windowEnd(Instant.now())
                .build();
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("command with no Kaseware documents produces SUCCESS result (empty run)")
    void fullPipeline_noDocuments_producesSuccessResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "arcgis-secret-empty";

        when(kwGraphqlClient.queryDocumentsWithLocations(any())).thenReturn(Collections.emptyList());
        when(vaultService.getSecret(secretName)).thenReturn(buildOAuthSecret());

        messagePublisher.publish(
                QueueNames.ARCGIS_EXCHANGE,
                QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "arcgis-tenant", secretName));

        ArcGISExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ArcGISExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isZero();
        assertThat(result.getFailedRecords()).isZero();
    }

    @Test
    @DisplayName("command with Kaseware documents having no locations produces SUCCESS result")
    void fullPipeline_documentsWithNoLocations_producesSuccessResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "arcgis-secret-noloc";

        KwDocumentDto docWithNoLocations = new KwDocumentDto(
                "doc-001", "Test Doc", "Feature Layer", 0L, 0L, Collections.emptyList());

        when(kwGraphqlClient.queryDocumentsWithLocations(any()))
                .thenReturn(List.of(docWithNoLocations));
        when(vaultService.getSecret(secretName)).thenReturn(buildOAuthSecret());

        messagePublisher.publish(
                QueueNames.ARCGIS_EXCHANGE,
                QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "arcgis-tenant-noloc", secretName));

        ArcGISExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ArcGISExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
    }

    @Test
    @DisplayName("orchestrator exception produces FAILED result on result queue")
    void fullPipeline_orchestratorException_producesFailedResult() {
        UUID jobId = UUID.randomUUID();
        String secretName = "arcgis-secret-fail";

        when(kwGraphqlClient.queryDocumentsWithLocations(any()))
                .thenThrow(new RuntimeException("Simulated Kaseware API failure"));
        when(vaultService.getSecret(anyString())).thenReturn(buildOAuthSecret());

        messagePublisher.publish(
                QueueNames.ARCGIS_EXCHANGE,
                QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "arcgis-tenant-fail", secretName));

        ArcGISExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ArcGISExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Simulated Kaseware API failure");
    }

    @Test
    @DisplayName("multiple sequential ArcGIS commands produce results for all job IDs")
    void fullPipeline_multipleCommands_allResultsReceived() {
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        String secretName = "arcgis-secret-multi";

        when(kwGraphqlClient.queryDocumentsWithLocations(any())).thenReturn(Collections.emptyList());
        when(vaultService.getSecret(secretName)).thenReturn(buildOAuthSecret());

        messagePublisher.publish(
                QueueNames.ARCGIS_EXCHANGE,
                QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId1, "arcgis-tenant-multi", secretName));

        messagePublisher.publish(
                QueueNames.ARCGIS_EXCHANGE,
                QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId2, "arcgis-tenant-multi", secretName));

        List<UUID> receivedJobIds = await()
                .atMost(25, TimeUnit.SECONDS)
                .until(() -> {
                    java.util.List<UUID> collected = new java.util.ArrayList<>();
                    ArcGISExecutionResult r1 = (ArcGISExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
                    ArcGISExecutionResult r2 = (ArcGISExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE);
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

    @Test
    @DisplayName("result message contains correct timing metadata")
    void fullPipeline_result_containsTimingMetadata() {
        UUID jobId = UUID.randomUUID();
        String secretName = "arcgis-secret-timing";

        when(kwGraphqlClient.queryDocumentsWithLocations(any())).thenReturn(Collections.emptyList());
        when(vaultService.getSecret(secretName)).thenReturn(buildOAuthSecret());

        Instant beforePublish = Instant.now();
        messagePublisher.publish(
                QueueNames.ARCGIS_EXCHANGE,
                QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE,
                buildCommand(jobId, "arcgis-tenant-timing", secretName));

        ArcGISExecutionResult result = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(
                        () -> (ArcGISExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getStartedAt()).isAfterOrEqualTo(beforePublish.minusSeconds(1));
        assertThat(result.getCompletedAt()).isAfterOrEqualTo(result.getStartedAt());
    }
}

