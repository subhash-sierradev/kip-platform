package com.integration.execution.contract.model;

import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationJobExecutionDto {
    private UUID id;
    private UUID scheduleId;

    private UUID originalJobId;
    private Integer retryAttempt;

    private TriggerType triggeredBy;

    private Instant windowStart;
    private Instant windowEnd;

    private JobExecutionStatus status;

    private Instant startedAt;
    private Instant completedAt;

    // Record counts
    private Integer addedRecords;
    private Integer updatedRecords;
    private Integer failedRecords;
    private Integer totalRecords;

    // Metadata fields for detailed record tracking
    private List<RecordMetadata> addedRecordsMetadata;
    private List<RecordMetadata> updatedRecordsMetadata;
    private List<FailedRecordMetadata> failedRecordsMetadata;
    private List<RecordMetadata> totalRecordsMetadata;

    private String errorMessage;
    private String triggeredByUser;

    /** Generic key-value metadata (e.g. confluencePageUrl, confluencePageId). */
    private java.util.Map<String, String> executionMetadata;
}
