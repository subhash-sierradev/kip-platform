package com.integration.execution.contract.model;

/**
 * Lightweight cache DTO representing a resolved ArcGIS external object mapping.
 * Replaces the former ArcGISExternalObjectMapping JPA entity.
 * Mapping data is cached in-memory (24h TTL) only — no database persistence.
 *
 * @param externalLocationId     The Kaseware location identifier
 * @param arcgisEndpointUrlHash  SHA-256 hash of the ArcGIS endpoint URL
 * @param objectid               The corresponding ArcGIS OBJECTID
 */
public record ArcGISObjectMappingEntry(
        String externalLocationId,
        String arcgisEndpointUrlHash,
        Long objectid
) {
}
