package com.integration.management.messaging;

import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.model.enums.TriggerStatus;
import com.integration.management.service.JiraWebhookEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraWebhookExecutionResultListener")
class JiraWebhookExecutionResultListenerTest {

    @Mock
    private JiraWebhookEventService jiraWebhookEventService;

    @InjectMocks
    private JiraWebhookExecutionResultListener listener;

    @Nested
    @DisplayName("onExecutionResult")
    class OnExecutionResult {

        @Test
        @DisplayName("should delegate to service applyResult on success result")
        void onExecutionResult_successResult_delegatesToService() {
            JiraWebhookExecutionResult result = JiraWebhookExecutionResult.builder()
                    .triggerEventId(UUID.randomUUID())
                    .success(true)
                    .status(TriggerStatus.SUCCESS)
                    .transformedPayload("{\"fields\":{\"summary\":\"Test\"}}")
                    .responseStatusCode(201)
                    .responseBody("{\"id\":\"10001\",\"key\":\"TEST-1\"}")
                    .jiraIssueUrl("https://jira.example.com/browse/TEST-1")
                    .build();

            listener.onExecutionResult(result);

            verify(jiraWebhookEventService).applyResult(result);
        }

        @Test
        @DisplayName("should delegate to service applyResult on failure result")
        void onExecutionResult_failureResult_delegatesToService() {
            JiraWebhookExecutionResult result = JiraWebhookExecutionResult.failure(
                    UUID.randomUUID(), "Connection refused");

            listener.onExecutionResult(result);

            verify(jiraWebhookEventService).applyResult(result);
        }

        @Test
        @DisplayName("should rethrow exception from service to let Spring AMQP nack the message")
        void onExecutionResult_serviceThrows_rethrowsException() {
            JiraWebhookExecutionResult result = JiraWebhookExecutionResult.builder()
                    .triggerEventId(UUID.randomUUID())
                    .success(true)
                    .status(TriggerStatus.SUCCESS)
                    .build();

            doThrow(new RuntimeException("DB unavailable")).when(jiraWebhookEventService).applyResult(result);

            assertThatThrownBy(() -> listener.onExecutionResult(result))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB unavailable");

            verify(jiraWebhookEventService).applyResult(result);
        }
    }
}
