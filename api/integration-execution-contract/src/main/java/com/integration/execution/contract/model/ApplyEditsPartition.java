package com.integration.execution.contract.model;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Result of partitioning ArcGIS features into ADD and UPDATE operations.
 *
 * @param adds    Features to be added (no existing objectId in ArcGIS)
 * @param updates Features to be updated (objectId injected from mapping resolution)
 */
public record ApplyEditsPartition(
        ArrayNode adds,
        ArrayNode updates
) {
}
