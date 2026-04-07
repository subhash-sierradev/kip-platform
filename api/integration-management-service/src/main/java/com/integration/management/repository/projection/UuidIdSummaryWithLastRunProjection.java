package com.integration.management.repository.projection;

import java.time.Instant;

/**
 * Summary projection for entities with UUID IDs (e.g., ArcGIS, Confluence).
 */
public interface UuidIdSummaryWithLastRunProjection extends UuidIdProjection {
    String getName();
    String getDescription();
    Boolean getIsEnabled();
    Instant getLastRunAt();
}
