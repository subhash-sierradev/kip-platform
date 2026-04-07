package com.integration.execution.contract.model;

/**
 * Model for storing metadata about successfully processed records (added/updated)
 * Used in JSONB columns for detailed record tracking
 * Timestamp fields are nullable - null represents unset/unavailable timestamps
 */
public record RecordMetadata(
        String documentId,
        String title,
        String locationId,
        Long documentCreatedAt,
        Long documentUpdatedAt,
        Long locationCreatedAt,
        Long locationUpdatedAt
) {
}
