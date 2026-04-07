package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.enums.JobExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Summary DTO returned in list queries for Confluence integrations.
 * Limited set of fields for efficient list rendering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfluenceIntegrationSummaryResponse {

    private UUID id;
    private String name;
    private String itemType;
    private String itemSubtype;
    private String itemSubtypeLabel;
    private String dynamicDocumentType;
    private String dynamicDocumentTypeLabel;
    private String confluenceSpaceKey;
    private List<String> languageCodes;

    private String frequencyPattern;
    private Integer dailyExecutionInterval;
    private LocalDate executionDate;
    private LocalTime executionTime;
    private String daySchedule;
    private String monthSchedule;
    private Boolean isExecuteOnMonthEnd;
    private String cronExpression;

    private Instant createdDate;
    private String createdBy;
    private Instant lastModifiedDate;
    private String lastModifiedBy;

    private Boolean isEnabled;

    private Instant lastAttemptTimeUtc;
    private JobExecutionStatus lastStatus;
    private Instant nextRunAtUtc;
}
