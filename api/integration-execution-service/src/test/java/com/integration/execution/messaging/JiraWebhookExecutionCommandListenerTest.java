package com.integration.execution.messaging;

import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.TriggerStatus;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.service.processor.JiraWebhookProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraWebhookExecutionCommandListener")
class JiraWebhookExecutionCommandListenerTest {

    @Mock
    private JiraWebhookProcessor jiraWebhookProcessor;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private JiraWebhookExecutionCommandListener listener;

    @BeforeEach
    void setUp() {
        listener = new JiraWebhookExecutionCommandListener(
                jiraWebhookProcessor, messagePublisher, notificationEventPublisher);
    }

    @Nested
    @DisplayName("handleExecutionCommand")
    class HandleExecutionCommand {

        @Test
        @DisplayName("should process command and publish success result to result queue")
        void handleExecutionCommand_successfulProcessing_publishesSuccessResult() {
            UUID eventId = UUID.randomUUID();
            JiraWebhookExecutionCommand command = buildCommand(eventId);

            JiraWebhookExecutionResult expectedResult = JiraWebhookExecutionResult.builder()
                    .triggerEventId(eventId)
                    .success(true)
                    .status(TriggerStatus.SUCCESS)
                    .transformedPayload("{\"fields\":{\"summary\":\"Test\"}}")
                    .responseStatusCode(201)
                    .responseBody("{\"id\":\"10001\",\"key\":\"TEST-1\"}")
                    .jiraIssueUrl("https://jira.example.com/browse/TEST-1")
                    .build();

            when(jiraWebhookProcessor.processWebhookExecution(command)).thenReturn(expectedResult);

            listener.handleExecutionCommand(command);

            verify(jiraWebhookProcessor).processWebhookExecution(command);
            verify(messagePublisher).publish(
                    eq(QueueNames.JIRA_WEBHOOK_EXCHANGE),
                    eq(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                    eq(expectedResult));
        }

        @Test
        @DisplayName("should publish failed result when processor throws exception")
        void handleExecutionCommand_processorThrows_publishesFailureResult() {
            UUID eventId = UUID.randomUUID();
            JiraWebhookExecutionCommand command = buildCommand(eventId);
            String errorMessage = "Connection refused to Jira";

            when(jiraWebhookProcessor.processWebhookExecution(command))
                    .thenThrow(new RuntimeException(errorMessage));

            listener.handleExecutionCommand(command);

            verify(jiraWebhookProcessor).processWebhookExecution(command);
            verify(messagePublisher).publish(
                    eq(QueueNames.JIRA_WEBHOOK_EXCHANGE),
                    eq(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                    eq(JiraWebhookExecutionResult.failure(eventId, errorMessage)));
        }

        @Test
        @DisplayName("should always publish result even when processor returns a failed status")
        void handleExecutionCommand_processorReturnsFailedStatus_publishesResult() {
            UUID eventId = UUID.randomUUID();
            JiraWebhookExecutionCommand command = buildCommand(eventId);

            JiraWebhookExecutionResult failedResult = JiraWebhookExecutionResult.failure(
                    eventId, "Invalid field mapping");

            when(jiraWebhookProcessor.processWebhookExecution(command)).thenReturn(failedResult);

            listener.handleExecutionCommand(command);

            verify(messagePublisher).publish(
                    eq(QueueNames.JIRA_WEBHOOK_EXCHANGE),
                    eq(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                    eq(failedResult));

            var publishOrder = inOrder(notificationEventPublisher);
            publishOrder.verify(notificationEventPublisher)
                    .publish(org.mockito.ArgumentMatchers.argThat(event ->
                            "JIRAWEBHOOK_EVENT_FAILED".equals(event.getEventKey())));
        }

        @Test
        @DisplayName("should publish received and completed notifications for successful result")
        void handleExecutionCommand_success_notificationsIncludeExpectedMetadata() {
            UUID eventId = UUID.randomUUID();
            JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                    .webhookId("webhook-abc")
                    .connectionSecretName("secret-jira-prod")
                    .webhookName("My Jira Webhook")
                    .samplePayload("{\"issue\":{\"key\":\"TEST-1\"}}")
                    .fieldMappings(List.of())
                    .incomingPayload("{\"issue\":{\"key\":\"TEST-1\"}}")
                    .triggerEventId(eventId)
                    .tenantId("tenant-123")
                    .triggeredBy("user-456")
                    .originalEventId("ISSUE-123")
                    .retryAttempt(0)
                    .build();

            JiraWebhookExecutionResult expectedResult = JiraWebhookExecutionResult.builder()
                    .triggerEventId(eventId)
                    .success(true)
                    .status(TriggerStatus.SUCCESS)
                    .build();

            when(jiraWebhookProcessor.processWebhookExecution(command)).thenReturn(expectedResult);

            listener.handleExecutionCommand(command);

            var publishOrder = inOrder(notificationEventPublisher);
            publishOrder.verify(notificationEventPublisher)
                    .publish(org.mockito.ArgumentMatchers.argThat(event -> isCompletedEvent(event,
                            "ISSUE-123")));
        }

        @Test
        @DisplayName("should publish failed notification with empty fallback metadata for null values")
        void handleExecutionCommand_failure_notificationsUseEmptyFallbackMetadata() {
            UUID eventId = UUID.randomUUID();
            JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                    .webhookId(null)
                    .webhookName(null)
                    .samplePayload("{}")
                    .fieldMappings(List.of())
                    .incomingPayload("{}")
                    .triggerEventId(eventId)
                    .tenantId("tenant-123")
                    .triggeredBy("user-456")
                    .retryAttempt(0)
                    .build();

            JiraWebhookExecutionResult failedResult = JiraWebhookExecutionResult.builder()
                    .triggerEventId(eventId)
                    .success(false)
                    .status(TriggerStatus.FAILED)
                    .errorMessage(null)
                    .build();

            when(jiraWebhookProcessor.processWebhookExecution(command)).thenReturn(failedResult);

            listener.handleExecutionCommand(command);

            var publishOrder = inOrder(notificationEventPublisher);
            publishOrder.verify(notificationEventPublisher)
                    .publish(org.mockito.ArgumentMatchers.argThat(event -> {
                        if (!"JIRAWEBHOOK_EVENT_FAILED".equals(event.getEventKey())) {
                            return false;
                        }
                        return "".equals(event.getMetadata().get("webhookName"))
                                && "".equals(event.getMetadata().get("webhookId"))
                                && "".equals(event.getMetadata().get("issueKey"))
                                && "".equals(event.getMetadata().get("errorMessage"));
                    }));
        }

        private JiraWebhookExecutionCommand buildCommand(final UUID eventId) {
            return JiraWebhookExecutionCommand.builder()
                    .webhookId("webhook-abc")
                    .connectionSecretName("secret-jira-prod")
                    .webhookName("My Jira Webhook")
                    .samplePayload("{\"issue\":{\"key\":\"TEST-1\"}}")
                    .fieldMappings(List.of())
                    .incomingPayload("{\"issue\":{\"key\":\"TEST-1\"}}")
                    .triggerEventId(eventId)
                    .tenantId("tenant-123")
                    .triggeredBy("user-456")
                    .retryAttempt(0)
                    .build();
        }


        private boolean isCompletedEvent(final NotificationEvent event, final String issueKey) {
            return "JIRAWEBHOOK_EVENT_COMPLETED".equals(event.getEventKey())
                    && "tenant-123".equals(event.getTenantId())
                    && "user-456".equals(event.getTriggeredByUserId())
                    && "My Jira Webhook".equals(event.getMetadata().get("webhookName"))
                    && "webhook-abc".equals(event.getMetadata().get("webhookId"))
                    && issueKey.equals(event.getMetadata().get("issueKey"));
        }
    }
}
