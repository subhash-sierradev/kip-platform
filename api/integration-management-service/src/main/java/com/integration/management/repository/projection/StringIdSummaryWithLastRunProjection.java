package com.integration.management.repository.projection;

import java.time.Instant;

/**
 * Summary projection for entities with String IDs (e.g., JiraWebhook).
 */
public interface StringIdSummaryWithLastRunProjection extends StringIdProjection {
    String getName();
    String getDescription();
    Boolean getIsEnabled();
    Instant getLastRunAt();
}
