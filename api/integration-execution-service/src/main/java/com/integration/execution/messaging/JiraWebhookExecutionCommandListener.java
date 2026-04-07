package com.integration.execution.messaging;

import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.service.processor.JiraWebhookProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listens on the Jira webhook execution command queue, processes each event via
 * {@link JiraWebhookProcessor}, and publishes a {@link JiraWebhookExecutionResult} back to IMS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JiraWebhookExecutionCommandListener {

    private final JiraWebhookProcessor jiraWebhookProcessor;
    private final MessagePublisher messagePublisher;
    private final NotificationEventPublisher notificationEventPublisher;

    public void handleExecutionCommand(final JiraWebhookExecutionCommand command) {
        log.info("Received Jira webhook execution command for webhook: {} (eventId: {})",
                command.getWebhookId(), command.getTriggerEventId());

        JiraWebhookExecutionResult result;

        try {
            result = jiraWebhookProcessor.processWebhookExecution(command);

            log.info("Successfully processed Jira webhook event: {} — status: {}",
                    command.getTriggerEventId(), result.getStatus());

        } catch (Exception e) {
            log.error("Fatal error processing Jira webhook command for event: {}",
                    command.getTriggerEventId(), e);

            result = JiraWebhookExecutionResult.failure(command.getTriggerEventId(), e.getMessage());
        }

        publishCompletionEvent(command, result);

        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE,
                result);

        log.info("Published Jira webhook execution result for eventId: {} with status: {}",
                result.getTriggerEventId(), result.getStatus());
    }

    private void publishCompletionEvent(final JiraWebhookExecutionCommand command,
            final JiraWebhookExecutionResult result) {
        if (result.isSuccess()) {
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(NotificationEventKey.JIRAWEBHOOK_EVENT_COMPLETED.name())
                    .tenantId(command.getTenantId())
                    .triggeredByUserId(command.getTriggeredBy())
                    .metadata(java.util.Map.of(
                            "webhookName", command.getWebhookName() != null ? command.getWebhookName() : "",
                            "webhookId", command.getWebhookId() != null ? command.getWebhookId() : "",
                            "issueKey", command.getOriginalEventId() != null ? command.getOriginalEventId() : ""))
                    .build());
        } else {
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(NotificationEventKey.JIRAWEBHOOK_EVENT_FAILED.name())
                    .tenantId(command.getTenantId())
                    .triggeredByUserId(command.getTriggeredBy())
                    .metadata(java.util.Map.of(
                            "webhookName", command.getWebhookName() != null ? command.getWebhookName() : "",
                            "webhookId", command.getWebhookId() != null ? command.getWebhookId() : "",
                            "issueKey", command.getOriginalEventId() != null ? command.getOriginalEventId() : "",
                            "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : ""))
                    .build());
        }
    }
}
