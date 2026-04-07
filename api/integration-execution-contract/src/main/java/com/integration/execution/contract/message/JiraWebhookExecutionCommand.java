package com.integration.execution.contract.message;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Contains all data IES needs to process a Jira webhook event without database access.
 * Sent synchronously from IMS → IES via Feign (fast op).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraWebhookExecutionCommand {

    private String webhookId;
    private String connectionSecretName;
    private String webhookName;
    private String samplePayload;
    private List<JiraFieldMappingDto> fieldMappings;
    private String incomingPayload;
    /** Tracking ID of the JiraWebhookEvent record to be created by IMS after result is returned. */
    private UUID triggerEventId;
    private String tenantId;
    private String triggeredBy;
    private String originalEventId;
    private int retryAttempt;
}
