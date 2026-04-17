package com.integration.execution.it;

import com.integration.execution.client.JiraApiClient;
import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.service.VaultService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for the Jira Webhook pipeline.
 *
 * <p>Publishes a {@link JiraWebhookExecutionCommand} directly to the command queue via
 * {@link MessagePublisher} and then polls the result queue with {@link RabbitTemplate} to
 * verify that the full listener → processor → publisher chain is wired correctly.
 *
 * <p>External dependencies ({@link JiraApiClient}, {@link VaultService}) are provided as
 * mocks via {@link TestConfiguration}.
 */
@DisplayName("Jira Webhook Full Pipeline — integration tests")
class JiraWebhookFullPipelineIT extends AbstractIesIT {

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JiraApiClient jiraApiClient;

    @TestConfiguration
    static class MockDependencies {
        @Bean
        @Primary
        public JiraApiClient jiraApiClient() {
            return mock(JiraApiClient.class);
        }

        @Bean
        @Primary
        public VaultService vaultService() {
            return mock(VaultService.class);
        }
    }

    @Test
    @DisplayName("valid command published to command queue produces SUCCESS result on result queue")
    void fullPipeline_validCommand_producesSuccessResult() {
        UUID eventId = UUID.randomUUID();
        when(jiraApiClient.sendToJira(eq("pipeline-secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true, "{\"id\":\"10001\",\"key\":\"TEST-1\"}"));

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-pipeline-ok")
                .triggerEventId(eventId)
                .tenantId("pipeline-tenant")
                .incomingPayload("{\"issue\":{\"key\":\"SRC-1\",\"fields\":{\"summary\":\"Pipeline test\"}}}")
                .fieldMappings(List.of(
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("project")
                                .jiraFieldName("Project")
                                .dataType(JiraDataType.OBJECT)
                                .defaultValue("{\"key\":\"TEST\"}")
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("issuetype")
                                .jiraFieldName("Issue Type")
                                .dataType(JiraDataType.OBJECT)
                                .defaultValue("{\"name\":\"Task\"}")
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("summary")
                                .jiraFieldName("Summary")
                                .dataType(JiraDataType.STRING)
                                .defaultValue("Pipeline test summary")
                                .build()))
                .connectionSecretName("pipeline-secret")
                .build();

        // Publish command — the RabbitMQ listener picks it up and processes it
        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                command);

        // Poll the result queue for the processed outcome
        JiraWebhookExecutionResult result = await()
                .atMost(15, TimeUnit.SECONDS)
                .until(
                        () -> (JiraWebhookExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getTriggerEventId()).isEqualTo(eventId);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResponseStatusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("command with null payload produces FAILED result on result queue")
    void fullPipeline_nullPayload_producesFailedResult() {
        UUID eventId = UUID.randomUUID();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-pipeline-null")
                .triggerEventId(eventId)
                .tenantId("pipeline-tenant")
                .incomingPayload(null)
                .fieldMappings(Collections.emptyList())
                .connectionSecretName("pipeline-secret-null")
                .build();

        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                command);

        JiraWebhookExecutionResult result = await()
                .atMost(15, TimeUnit.SECONDS)
                .until(
                        () -> (JiraWebhookExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getTriggerEventId()).isEqualTo(eventId);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotBlank();
    }

    @Test
    @DisplayName("command with Jira API returning 4xx produces FAILED result on result queue")
    void fullPipeline_jiraApiFailure_producesFailedResult() {
        UUID eventId = UUID.randomUUID();
        when(jiraApiClient.sendToJira(eq("pipeline-secret-400"), any(String.class)))
                .thenReturn(new ApiResponse(400, false, "Bad Request from Jira"));

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-pipeline-400")
                .triggerEventId(eventId)
                .tenantId("pipeline-tenant")
                .incomingPayload("{\"issue\":{\"key\":\"SRC-3\",\"fields\":{\"summary\":\"Fail\"}}}")
                .fieldMappings(Collections.emptyList())
                .connectionSecretName("pipeline-secret-400")
                .build();

        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                command);

        JiraWebhookExecutionResult result = await()
                .atMost(15, TimeUnit.SECONDS)
                .until(
                        () -> (JiraWebhookExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(result).isNotNull();
        assertThat(result.getTriggerEventId()).isEqualTo(eventId);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResponseStatusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("multiple sequential commands are processed in order and produce results")
    void fullPipeline_sequentialCommands_allResultsReceived() {
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();

        when(jiraApiClient.sendToJira(eq("seq-secret"), any(String.class)))
                .thenReturn(new ApiResponse(201, true, "{\"id\":\"10002\",\"key\":\"SEQ-1\"}"));

        JiraWebhookExecutionCommand command1 = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-seq-1")
                .triggerEventId(eventId1)
                .tenantId("seq-tenant")
                .incomingPayload("{\"issue\":{\"key\":\"SEQ-1\"}}")
                .fieldMappings(List.of(
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("project")
                                .dataType(JiraDataType.OBJECT)
                                .defaultValue("{\"key\":\"SEQ\"}")
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("issuetype")
                                .dataType(JiraDataType.OBJECT)
                                .defaultValue("{\"name\":\"Task\"}")
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("summary")
                                .dataType(JiraDataType.STRING)
                                .defaultValue("Sequential command 1")
                                .build()))
                .connectionSecretName("seq-secret")
                .build();

        JiraWebhookExecutionCommand command2 = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-seq-2")
                .triggerEventId(eventId2)
                .tenantId("seq-tenant")
                .incomingPayload("{\"issue\":{\"key\":\"SEQ-2\"}}")
                .fieldMappings(command1.getFieldMappings())
                .connectionSecretName("seq-secret")
                .build();

        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                command1);
        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                command2);

        List<UUID> receivedEventIds = await()
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    java.util.List<UUID> collected = new java.util.ArrayList<>();
                    JiraWebhookExecutionResult r1 = (JiraWebhookExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE);
                    JiraWebhookExecutionResult r2 = (JiraWebhookExecutionResult)
                            rabbitTemplate.receiveAndConvert(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE);
                    if (r1 != null) {
                        collected.add(r1.getTriggerEventId());
                    }
                    if (r2 != null) {
                        collected.add(r2.getTriggerEventId());
                    }
                    return collected;
                }, ids -> ids.size() == 2);

        assertThat(receivedEventIds).containsExactlyInAnyOrder(eventId1, eventId2);
    }
}

