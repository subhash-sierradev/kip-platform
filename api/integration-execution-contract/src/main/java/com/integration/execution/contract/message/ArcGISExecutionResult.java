package com.integration.execution.contract.message;

import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message sent from IES back to IMS via RabbitMQ after an ArcGIS execution completes.
 * IMS uses this to persist the final state of the IntegrationJobExecution record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArcGISExecutionResult {

    private UUID jobExecutionId;
    private JobExecutionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private int totalRecords;
    private int addedRecords;
    private int updatedRecords;
    private int failedRecords;
    private List<RecordMetadata> addedRecordsMetadata;
    private List<RecordMetadata> updatedRecordsMetadata;
    private List<RecordMetadata> totalRecordsMetadata;
    private List<FailedRecordMetadata> failedMetadata;
    private String errorMessage;
}
