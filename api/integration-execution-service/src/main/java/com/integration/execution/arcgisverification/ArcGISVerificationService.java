package com.integration.execution.arcgisverification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.config.properties.ArcGISVerificationProperties;
import com.integration.execution.contract.rest.response.arcgis.ArcGISVerificationPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO KIP-547 REMOVE — Temporary ArcGIS feature service verification service
@Slf4j
@Service
public class ArcGISVerificationService {

    static final int PAGE_SIZE = 1000;
    private static final String FIELD_OBJECTID = "OBJECTID";
    private static final String FIELD_LOCATION_ID = "external_location_id";
    private static final String ORDER_BY_OBJECTID_DESC = FIELD_OBJECTID + " DESC";
    private static final String QUERY_FORMAT = "json";
    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final long TOKEN_BUFFER_SECONDS = 60L;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final ArcGISVerificationProperties properties;
    private final RestTemplate arcGISVerificationRestTemplate;
    private final ObjectMapper objectMapper;

    private volatile String cachedToken;
    private volatile long tokenExpiryEpochMs;

    public ArcGISVerificationService(
            ArcGISVerificationProperties properties,
            RestTemplate arcGISVerificationRestTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.arcGISVerificationRestTemplate = arcGISVerificationRestTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Queries the ArcGIS feature service with optional filters and pagination.
     *
     * @param offset      zero-based record offset for pagination
     * @param objectId    filter by OBJECTID (numeric string); null means no filter
     * @param locationId  filter by external_location_id; null means no filter
     * @return paged response containing feature attribute maps
     * @throws NumberFormatException if objectId is non-numeric
     * @throws IllegalStateException if the ArcGIS API returns an error
     */
    public ArcGISVerificationPageResponse queryFeatures(
            int offset, String objectId, String locationId) {
        String token = getAccessToken();
        String whereClause = buildWhereClause(objectId, locationId);
        log.debug("Querying ArcGIS: offset={}, where={}", offset, whereClause);

        URI queryUri = UriComponentsBuilder
                .fromUriString(properties.getFeatureServiceUrl())
                .pathSegment("0", "query")
                .queryParam("f", QUERY_FORMAT)
                .queryParam("where", whereClause)
                .queryParam("outFields", "*")
                .queryParam("returnGeometry", false)
                .queryParam("orderByFields", ORDER_BY_OBJECTID_DESC)
                .queryParam("resultOffset", offset)
                .queryParam("resultRecordCount", PAGE_SIZE)
                .queryParam("token", token)
                .build()
                .encode()
                .toUri();

        String responseBody = arcGISVerificationRestTemplate.getForObject(queryUri, String.class);
        return parseQueryResponse(responseBody, offset);
    }

    /**
     * Builds the ArcGIS WHERE clause from optional search filters.
     * Validates that objectId is numeric to prevent injection.
     *
     * @param objectId   numeric OBJECTID value, or null/blank for no filter
     * @param locationId external_location_id string value, or null/blank for no filter
     * @return a safe WHERE clause string
     * @throws NumberFormatException if objectId is provided but not a valid number
     */
    String buildWhereClause(String objectId, String locationId) {
        if (objectId != null && !objectId.isBlank()) {
            Long.parseLong(objectId.trim()); // validates numeric — prevents injection
            return FIELD_OBJECTID + " = " + objectId.trim();
        }
        if (locationId != null && !locationId.isBlank()) {
            String safeId = locationId.trim().replace("'", "''");
            return FIELD_LOCATION_ID + " = '" + safeId + "'";
        }
        return "1=1";
    }

    /**
     * Returns a valid access token, fetching a new one only when the cached token is expired.
     */
    String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryEpochMs) {
            log.debug("Using cached ArcGIS verification token");
            return cachedToken;
        }
        return fetchAndCacheToken();
    }

    private String fetchAndCacheToken() {
        log.info("Fetching new ArcGIS verification access token");
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS);
        params.add("f", QUERY_FORMAT);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        String responseBody = arcGISVerificationRestTemplate.postForObject(
                properties.getTokenUrl(), request, String.class);

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            if (json.has("error")) {
                throw new IllegalStateException(
                        "ArcGIS token endpoint returned error: " + json.path("error").toString());
            }
            String token = json.path("access_token").asText();
            long expiresIn = json.path("expires_in").asLong(3600L);
            long bufferMs = TOKEN_BUFFER_SECONDS * MILLIS_PER_SECOND;
            tokenExpiryEpochMs = System.currentTimeMillis()
                    + (expiresIn * MILLIS_PER_SECOND) - bufferMs;
            cachedToken = token;
            log.info("ArcGIS verification token cached, expires in {}s", expiresIn);
            return token;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse ArcGIS token response", e);
        }
    }

    private ArcGISVerificationPageResponse parseQueryResponse(String responseBody, int offset) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                throw new IllegalStateException(
                        "ArcGIS query returned error: " + root.path("error").toString());
            }
            boolean exceededTransferLimit =
                    root.path("exceededTransferLimit").asBoolean(false);
            JsonNode featuresNode = root.path("features");
            List<Map<String, Object>> features = new ArrayList<>();

            if (featuresNode.isArray()) {
                for (JsonNode featureNode : featuresNode) {
                    JsonNode attributesNode = featureNode.path("attributes");
                    Map<String, Object> attributes = new HashMap<>();
                    Iterator<Map.Entry<String, JsonNode>> fields = attributesNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        attributes.put(entry.getKey(), resolveNodeValue(entry.getValue()));
                    }
                    features.add(attributes);
                }
            }

            log.info("ArcGIS verification query returned {} features (offset={})",
                    features.size(), offset);
            return ArcGISVerificationPageResponse.builder()
                    .features(features)
                    .fetchedCount(features.size())
                    .offset(offset)
                    .exceededTransferLimit(exceededTransferLimit)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse ArcGIS query response", e);
        }
    }

    private Object resolveNodeValue(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isLong() || node.isInt()) {
            return node.longValue();
        }
        if (node.isDouble() || node.isFloat()) {
            return node.doubleValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText(null);
    }
}
