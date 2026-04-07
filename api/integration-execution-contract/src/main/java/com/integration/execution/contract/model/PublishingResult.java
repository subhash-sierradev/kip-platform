package com.integration.execution.contract.model;

import java.util.List;

public record PublishingResult(
        int addedCount,
        int updatedCount,
        int failedCount,
        List<RecordMetadata> addedMetadata,
        List<RecordMetadata> updatedMetadata,
        List<FailedRecordMetadata> failedMetadata
) {
}
