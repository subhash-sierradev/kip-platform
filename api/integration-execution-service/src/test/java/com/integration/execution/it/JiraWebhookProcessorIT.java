package com.integration.execution.it;

import com.integration.execution.client.JiraApiClient;
import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.service.VaultService;
import com.integration.execution.service.processor.JiraWebhookProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the Jira Webhook Processor pipeline.
 *
 * <p>Loads the full Spring context with a real RabbitMQ container.
 * {@link JiraApiClient} and {@link VaultService} are provided as mocks via {@link TestConfiguration}
 * because they depend on external HTTP services not available in the test environment.
 */
@DisplayName("Jira Webhook Processor — integration tests")
class JiraWebhookProcessorIT extends AbstractIesIT {

    @Autowired
    private JiraWebhookProcessor jiraWebhookProcessor;

    @Autowired
    private JiraApiClient jiraApiClient;

    @Autowired
    private VaultService vaultService;

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
    @DisplayName("processWebhookExecution returns failure result when payload is null")
    void processWebhookExecution_nullPayload_returnsFailure() {
        UUID eventId = UUID.randomUUID();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-001")
                .triggerEventId(eventId)
                .tenantId("it-tenant")
                .incomingPayload(null)
                .fieldMappings(Collections.emptyList())
                .connectionSecretName("secret-001")
                .build();

        JiraWebhookExecutionResult result = jiraWebhookProcessor.processWebhookExecution(command);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTriggerEventId()).isEqualTo(eventId);
        assertThat(result.getErrorMessage()).contains("payload cannot be null or empty");
    }

    @Test
    @DisplayName("processWebhookExecution returns failure result when payload is empty")
    void processWebhookExecution_emptyPayload_returnsFailure() {
        UUID eventId = UUID.randomUUID();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-002")
                .triggerEventId(eventId)
                .tenantId("it-tenant")
                .incomingPayload("")
                .fieldMappings(Collections.emptyList())
                .connectionSecretName("secret-002")
                .build();

        JiraWebhookExecutionResult result = jiraWebhookProcessor.processWebhookExecution(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("payload cannot be null or empty");
    }

    @Test
    @DisplayName("processWebhookExecution returns success when Jira API returns 201")
    void processWebhookExecution_validPayload_jiraReturnsCreated_returnsSuccess() {
        UUID eventId = UUID.randomUUID();
        when(jiraApiClient.sendToJira(eq("secret-ok"), any(String.class)))
                .thenReturn(new ApiResponse(201, true, "{\"id\":\"10001\",\"key\":\"TEST-1\"}"));

        JiraFieldMappingDto projectMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("project")
                .jiraFieldName("Project")
                .dataType(JiraDataType.OBJECT)
                .defaultValue("{\"key\":\"TEST\"}")
                .build();

        JiraFieldMappingDto issueTypeMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("issuetype")
                .jiraFieldName("Issue Type")
                .dataType(JiraDataType.OBJECT)
                .defaultValue("{\"name\":\"Task\"}")
                .build();

        JiraFieldMappingDto summaryMapping = JiraFieldMappingDto.builder()
                .jiraFieldId("summary")
                .jiraFieldName("Summary")
                .dataType(JiraDataType.STRING)
                .defaultValue("Test issue from IT")
                .build();

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-ok")
                .triggerEventId(eventId)
                .tenantId("it-tenant")
                .incomingPayload("{\"issue\":{\"key\":\"SRC-1\",\"fields\":{\"summary\":\"Test\"}}}")
                .fieldMappings(List.of(projectMapping, issueTypeMapping, summaryMapping))
                .connectionSecretName("secret-ok")
                .build();

        JiraWebhookExecutionResult result = jiraWebhookProcessor.processWebhookExecution(command);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTriggerEventId()).isEqualTo(eventId);
        assertThat(result.getResponseStatusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("processWebhookExecution returns failure when Jira API returns non-201")
    void processWebhookExecution_jiraReturnsFailed_returnsFailureResult() {
        UUID eventId = UUID.randomUUID();
        when(jiraApiClient.sendToJira(eq("secret-fail"), any(String.class)))
                .thenReturn(new ApiResponse(400, false, "Bad Request"));

        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-fail")
                .triggerEventId(eventId)
                .tenantId("it-tenant")
                .incomingPayload("{\"issue\":{\"key\":\"SRC-2\"}}")
                .fieldMappings(Collections.emptyList())
                .connectionSecretName("secret-fail")
                .build();

        JiraWebhookExecutionResult result = jiraWebhookProcessor.processWebhookExecution(command);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResponseStatusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("processWebhookExecution handles invalid JSON payload gracefully")
    void processWebhookExecution_invalidJson_returnsFailure() {
        UUID eventId = UUID.randomUUID();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-badjson")
                .triggerEventId(eventId)
                .tenantId("it-tenant")
                .incomingPayload("{this is not valid json}")
                .fieldMappings(Collections.emptyList())
                .connectionSecretName("secret-badjson")
                .build();

        JiraWebhookExecutionResult result = jiraWebhookProcessor.processWebhookExecution(command);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTriggerEventId()).isEqualTo(eventId);
    }
}

