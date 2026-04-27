package com.integration.management.model.dto.response;

import com.integration.execution.contract.rest.response.IntegrationScheduleResponse;
import com.integration.management.entity.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full details DTO for a Confluence integration, including all configuration and schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfluenceIntegrationResponse {

    private UUID id;
    private String name;
    private String normalizedName;
    private String description;
    private String itemType;
    private String itemSubtype;
    private String itemSubtypeLabel;
    private String dynamicDocumentType;
    private String dynamicDocumentTypeLabel;

    private List<Language> languages;
    private List<String> languageCodes;
    /** BCP-47 source language of the report template (e.g. "en"). */
    private String sourceLanguage;

    private String reportNameTemplate;
    private String confluenceSpaceKey;
    private String confluenceSpaceLabel;
    private String confluenceSpaceKeyFolderKey;
    private String confluenceSpaceFolderLabel;
    private Boolean includeTableOfContents;

    private String connectionId;
    private String tenantId;

    private IntegrationScheduleResponse schedule;

    private Boolean isEnabled;

    private Instant createdDate;
    private String createdBy;
    private Instant lastModifiedDate;
    private String lastModifiedBy;

    private Instant nextRunAtUtc;
    private Long version;
}
