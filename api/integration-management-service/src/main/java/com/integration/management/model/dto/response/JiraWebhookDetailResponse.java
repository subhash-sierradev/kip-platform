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
public class JiraWebhookDetailResponse {
    private String id;
    private String name;
    private String description;
    private String webhookUrl;
    private String connectionId;
    private List<JiraFieldMappingDto> jiraFieldMappings;
    private String samplePayload;
    private Boolean isEnabled;
    private Boolean isDeleted;
    private String normalizedName;
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
    private String tenantId;
    private Long version;
}