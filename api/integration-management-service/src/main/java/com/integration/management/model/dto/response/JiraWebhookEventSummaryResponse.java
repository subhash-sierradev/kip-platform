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
public class JiraWebhookEventSummaryResponse {
    private String id;
    private String triggeredBy;
    private Instant triggeredAt;
    private String status;
    private String jiraIssueUrl;
}