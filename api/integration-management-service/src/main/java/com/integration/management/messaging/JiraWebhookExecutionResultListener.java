package com.integration.management.messaging;

import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.management.service.JiraWebhookEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listens on the Jira webhook execution result queue for completion messages from IES.
 * On receipt, persists the final state of the corresponding {@code JiraWebhookEvent} record.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JiraWebhookExecutionResultListener {

    private final JiraWebhookEventService jiraWebhookEventService;

    public void onExecutionResult(final JiraWebhookExecutionResult result) {
        log.info("Received Jira webhook execution result for event {}: status={}",
                result.getTriggerEventId(), result.getStatus());
        try {
            jiraWebhookEventService.applyResult(result);
        } catch (Exception e) {
            log.error("Failed to persist Jira webhook execution result for event {}: {}",
                    result.getTriggerEventId(), e.getMessage(), e);
            throw e;
        }
    }
}
