package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraWebhookSummaryResponse {
    private String id;
    private String name;
    private String webhookUrl;
    private List<JiraFieldMappingDto> jiraFieldMappings;
    private Boolean isEnabled;
    private Boolean isDeleted;
    private String createdBy;
    private Instant createdDate;
    private JiraWebhookEventSummaryResponse lastEventHistory;
}