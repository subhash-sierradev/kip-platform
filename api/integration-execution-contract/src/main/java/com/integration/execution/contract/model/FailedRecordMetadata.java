package com.integration.execution.contract.model;

/**
 * Model for storing metadata about failed records with error details
 * Used in JSONB columns for detailed error tracking
 * Timestamp fields are nullable - null represents unset/unavailable timestamps
 */
public record FailedRecordMetadata(
        String documentId,
        String title,
        String locationId,
        Long documentCreatedAt,
        Long documentUpdatedAt,
        Long locationCreatedAt,
        Long locationUpdatedAt,
        String errorMessage
) {
}
