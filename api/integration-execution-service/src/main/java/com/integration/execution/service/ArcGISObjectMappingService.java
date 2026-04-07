package com.integration.execution.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Pure in-memory cache service for ArcGIS external-location-id → OBJECTID mappings.
 */
@Slf4j
@Service
public class ArcGISObjectMappingService {

    private String abbreviateUrlHash(String urlHash) {
        if (urlHash == null) {
            return "null";
        }
        if (urlHash.length() <= 8) {
            return urlHash;
        }
        return urlHash.substring(0, 8) + "...";
    }

    /**
     * Returns the cached OBJECTID for the given external location ID + URL hash combination,
     * or {@code null} on a cache miss.
     */
    @Cacheable(value = "arcgisObjectMappingCache",
               key = "#externalLocationId + '::' + #urlHash",
               unless = "#result == null")
    public Long getMapping(String externalLocationId, String urlHash) {
        log.debug("Cache miss for: {}::{}", externalLocationId, abbreviateUrlHash(urlHash));
        return null;
    }

    /**
     * Stores a resolved OBJECTID into the cache — called after ArcGIS API returns a new mapping.
     */
    @CachePut(value = "arcgisObjectMappingCache",
              key = "#externalLocationId + '::' + #urlHash")
    public Long putMapping(String externalLocationId, String urlHash, Long objectId) {
        log.debug("Cached mapping: {}::{} = {}", externalLocationId, abbreviateUrlHash(urlHash), objectId);
        return objectId;
    }
}
