package com.integration.management.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraWebhookEventResponse {
    private String id;
    private String tenantId;
    private String triggeredBy;
    private Instant triggeredAt;
    private String webhookId;
    private String incomingPayload;
    private String transformedPayload;
    private Integer responseStatusCode;
    private String responseBody;
    private String status;
    @Builder.Default
    private Integer retryAttempt = 0;
    private String originalEventId;
    private String jiraIssueUrl;
}