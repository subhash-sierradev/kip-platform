package com.integration.management.repository.projection;

import java.util.UUID;

/**
 * Base projection for entities with UUID IDs.
 */
public interface UuidIdProjection {
    UUID getId();
}
