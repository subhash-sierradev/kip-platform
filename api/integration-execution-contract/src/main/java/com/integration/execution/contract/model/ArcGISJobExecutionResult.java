package com.integration.execution.contract.model;

import java.util.List;

public record ArcGISJobExecutionResult(
        int addedRecords,
        int updatedRecords,
        int failedRecords,
        int totalRecords,
        List<RecordMetadata> addedRecordsMetadata,
        List<RecordMetadata> updatedRecordsMetadata,
        List<FailedRecordMetadata> failedRecordsMetadata,
        List<RecordMetadata> totalRecordsMetadata,
        String errorMessage
) {
    /** Convenience constructor for results without an error message. */
    public ArcGISJobExecutionResult(
            int addedRecords,
            int updatedRecords,
            int failedRecords,
            int totalRecords,
            List<RecordMetadata> addedRecordsMetadata,
            List<RecordMetadata> updatedRecordsMetadata,
            List<FailedRecordMetadata> failedRecordsMetadata,
            List<RecordMetadata> totalRecordsMetadata) {
        this(addedRecords, updatedRecords, failedRecords, totalRecords,
                addedRecordsMetadata, updatedRecordsMetadata,
                failedRecordsMetadata, totalRecordsMetadata, null);
    }
}
