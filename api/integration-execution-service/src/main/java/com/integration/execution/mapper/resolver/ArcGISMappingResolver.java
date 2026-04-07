package com.integration.execution.mapper.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.contract.model.ApplyEditsPartition;
import com.integration.execution.service.ArcGISObjectMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.integration.execution.constants.ArcGisConstants.ARCGIS_FIELD_EXTERNAL_LOCATION_ID;
import static com.integration.execution.constants.ArcGisConstants.ARCGIS_FIELD_OBJECTID;

/**
 * URL-aware ArcGIS mapping resolver.
 * Resolves external_location_id to OBJECTID mappings for specific ArcGIS endpoints.
 * Uses SHA-256 hash of endpoint URL for secure storage (URL not stored in plain text).
 * Uses a two-tier resolution strategy: Cache (24h TTL) → ArcGIS API.
 * The former DB tier has been removed; all mappings are held in-memory only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArcGISMappingResolver {

    private static final int ARCGIS_BATCH_SIZE = 1000;

    private final ArcGISApiClient arcgisApiClient;
    private final ObjectMapper objectMapper;
    private final ArcGISObjectMappingService cacheService;

    public ApplyEditsPartition partitionFeaturesForAddOrUpdate(
            ArrayNode features,
            String secretName,
            String arcgisEndpointUrl) {
        Set<String> allExternalIds = extractExternalLocationIds(features);
        if (allExternalIds.isEmpty()) {
            log.warn("No external_location_id found in features");
            return new ApplyEditsPartition(features, objectMapper.createArrayNode());
        }

        String urlHash = hashUrl(arcgisEndpointUrl);
        log.info("Extracted {} unique external_location_id values from {} features for (hash: {})",
                allExternalIds.size(), features.size(), urlHash.substring(0, 8) + "...");

        Map<String, Long> resolvedMappings = resolveAllMappings(allExternalIds, urlHash, secretName, arcgisEndpointUrl);

        return partitionFeatures(features, resolvedMappings);
    }

    private Map<String, Long> resolveAllMappings(
            Set<String> allExternalIds,
            String urlHash,
            String secretName,
            String arcgisEndpointUrl) {

        Map<String, Long> resolvedMappings = resolveMappingsFromCache(allExternalIds, urlHash);
        Set<String> unresolvedIds = new HashSet<>(allExternalIds);
        unresolvedIds.removeAll(resolvedMappings.keySet());
        log.info("Cache resolved {} mappings, {} remaining (two-tier: cache → ArcGIS API)",
                resolvedMappings.size(), unresolvedIds.size());

        if (!unresolvedIds.isEmpty()) {
            resolveMappingsFromArcGIS(unresolvedIds, urlHash, secretName, arcgisEndpointUrl, resolvedMappings);
        }

        log.info("Total resolved mappings: {} out of {} unique external_location_ids",
                resolvedMappings.size(), allExternalIds.size());

        return resolvedMappings;
    }

    private void resolveMappingsFromArcGIS(
            Set<String> unresolvedIds,
            String urlHash,
            String secretName,
            String arcgisEndpointUrl,
            Map<String, Long> resolvedMappings) {

        try {
            Map<String, Long> arcgisMappings = queryArcGISForMappings(unresolvedIds, secretName);
            resolvedMappings.putAll(arcgisMappings);

            if (!arcgisMappings.isEmpty()) {
                // Cache-only: store in 24h TTL cache — no database write
                arcgisMappings.forEach((externalId, objectId) ->
                        cacheService.putMapping(externalId, urlHash, objectId));
            }

            log.info("ArcGIS resolved {} new mappings for endpoint: {}",
                    arcgisMappings.size(), arcgisEndpointUrl);
        } catch (Exception e) {
            log.error("Error querying ArcGIS for mappings: {}", e.getMessage(), e);
        }
    }

    private ApplyEditsPartition partitionFeatures(ArrayNode features, Map<String, Long> resolvedMappings) {
        ArrayNode addFeatures = objectMapper.createArrayNode();
        ArrayNode updateFeatures = objectMapper.createArrayNode();

        for (JsonNode feature : features) {
            if (!feature.isObject()) {
                continue;
            }

            processFeature((ObjectNode) feature, resolvedMappings, addFeatures, updateFeatures);
        }

        log.info("Partitioned {} features into {} adds and {} updates.",
                features.size(), addFeatures.size(), updateFeatures.size());

        return new ApplyEditsPartition(addFeatures, updateFeatures);
    }

    private void processFeature(
            ObjectNode featureObj,
            Map<String, Long> resolvedMappings,
            ArrayNode addFeatures,
            ArrayNode updateFeatures) {

        JsonNode attributesNode = featureObj.get("attributes");
        if (attributesNode == null || !attributesNode.isObject()) {
            log.warn("Feature missing attributes node, skipping");
            return;
        }

        ObjectNode attributes = (ObjectNode) attributesNode;
        String externalLocationId = attributes.path(ARCGIS_FIELD_EXTERNAL_LOCATION_ID).asText(null);

        if (externalLocationId == null) {
            addFeatures.add(featureObj);
            return;
        }

        Long objectId = resolvedMappings.get(externalLocationId);
        if (objectId != null) {
            attributes.put(ARCGIS_FIELD_OBJECTID, objectId);
            updateFeatures.add(featureObj);
        } else {
            attributes.remove(ARCGIS_FIELD_OBJECTID);
            addFeatures.add(featureObj);
        }
    }

    private Set<String> extractExternalLocationIds(ArrayNode features) {
        Set<String> externalIds = new HashSet<>();

        for (JsonNode feature : features) {
            if (!feature.isObject()) {
                continue;
            }

            JsonNode attributes = feature.get("attributes");
            if (attributes != null && attributes.isObject()) {
                String externalLocationId = attributes.path(ARCGIS_FIELD_EXTERNAL_LOCATION_ID).asText(null);
                if (externalLocationId != null && !externalLocationId.isBlank()) {
                    externalIds.add(externalLocationId);
                }
            }
        }

        return externalIds;
    }

    private String hashUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash URL", e);
        }
    }

    private Map<String, Long> resolveMappingsFromCache(Set<String> externalLocationIds, String urlHash) {
        Map<String, Long> cached = new HashMap<>();
        for (String externalId : externalLocationIds) {
            Long objectId = cacheService.getMapping(externalId, urlHash);
            if (objectId != null) {
                cached.put(externalId, objectId);
            }
        }
        return cached;
    }

    private Map<String, Long> queryArcGISForMappings(Set<String> externalLocationIds, String secretName) {
        if (externalLocationIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> allMappings = new HashMap<>();
        List<String> idList = new ArrayList<>(externalLocationIds);

        for (int i = 0; i < idList.size(); i += ARCGIS_BATCH_SIZE) {
            int end = Math.min(i + ARCGIS_BATCH_SIZE, idList.size());
            List<String> batch = idList.subList(i, end);

            try {
                Map<String, Long> batchMappings = queryArcGISBatch(batch, secretName);
                allMappings.putAll(batchMappings);

                log.debug("ArcGIS batch {}-{}: resolved {} mappings", i, end, batchMappings.size());
            } catch (Exception e) {
                log.error("Error querying ArcGIS batch {}-{}: {}", i, end, e.getMessage());
            }
        }

        return allMappings;
    }

    private Map<String, Long> queryArcGISBatch(List<String> externalLocationIds, String secretName) {
        String inClause = externalLocationIds.stream()
                .map(id -> "'" + id.replace("'", "''") + "'")
                .collect(Collectors.joining(","));

        String whereClause = ARCGIS_FIELD_EXTERNAL_LOCATION_ID + " IN (" + inClause + ")";

        try {
            JsonNode response = arcgisApiClient.queryFeaturesWithWhere(secretName, whereClause);
            return parseArcGISQueryResponse(response);
        } catch (Exception e) {
            log.error("Error in ArcGIS batch query: {}", e.getMessage(), e);
            throw new RuntimeException("ArcGIS query failed", e);
        }
    }

    private Map<String, Long> parseArcGISQueryResponse(JsonNode response) {

        if (response == null || !response.has("features")) {
            log.warn("ArcGIS response missing features array");
            return Map.of();
        }

        JsonNode features = response.get("features");
        if (!features.isArray()) {
            return Map.of();
        }

        Map<String, Long> mappings = new HashMap<>();
        for (JsonNode feature : features) {
            JsonNode attributes = feature.get("attributes");
            if (attributes == null) {
                continue;
            }

            long objectId = attributes.path(ARCGIS_FIELD_OBJECTID).asLong(0L);
            String externalLocationId = attributes.path(ARCGIS_FIELD_EXTERNAL_LOCATION_ID).asText(null);

            if (objectId > 0 && externalLocationId != null && !externalLocationId.isBlank()) {
                mappings.put(externalLocationId, objectId);
            }
        }
        return mappings;
    }

}
