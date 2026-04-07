package com.integration.execution.contract.message;

import com.integration.execution.contract.model.enums.TriggerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Result returned synchronously from IES to IMS after a Jira webhook event is processed.
 * IMS persists this as a JiraWebhookEvent entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraWebhookExecutionResult {

    private UUID triggerEventId;
    private boolean success;
    private TriggerStatus status;
    private String transformedPayload;
    private int responseStatusCode;
    private String responseBody;
    private String errorMessage;
    private String jiraIssueUrl;

    public static JiraWebhookExecutionResult failure(final UUID triggerEventId, final String errorMessage) {
        return JiraWebhookExecutionResult.builder()
                .triggerEventId(triggerEventId)
                .success(false)
                .status(TriggerStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
