package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.integration.execution.config.cache.TokenCache;
import com.integration.execution.config.properties.KwProperties;
import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.mapper.KwDocumentMapper;
import com.integration.execution.contract.model.AuthResponse;
import com.integration.execution.util.InstantConverters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.integration.execution.constants.KasewareConstants.DOCUMENT_FINAL_DYNAMIC;
import static com.integration.execution.constants.KasewareConstants.KW_GRAPHQL_TOKEN_CACHE_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class KwGraphqlClient {

    private static final String DOCUMENT_TYPE = "DOCUMENT";
    private static final int SEARCH_START = 0;
    private static final int SEARCH_LIMIT = 10000;

    private static final String MONITORING_DOCUMENTS_QUERY = """
            query monitoringReports(
                $type: String,
                $subType: String,
                $start: Int,
                $limit: Int,
                $startCreatedTimestamp: Int,
                $endCreatedTimestamp: Int
            ) {
                searchWithData(
                    type: $type
                    subType: $subType
                    start: $start
                    limit: $limit
                    startCreatedTimestamp: $startCreatedTimestamp
                    endCreatedTimestamp: $endCreatedTimestamp
                ) {
                    ... on FinalDynamicDocument {
                        id
                        title
                        body
                        createdTimestamp
                        updatedTimestamp
                        dynamicFormDefinitionId
                        dynamicFormDefinitionName
                        dynamicFormVersionNumber
                        dynamicData
                        documentType
                        classifications
                        occurrenceDate
                        occurrenceTime
                        legacyId
                        attachments {
                            attachmentId
                            originalFilename
                            sizeInBytes
                            hash
                            description
                        }
                        tags
                        tenantId
                        authors {
                            id
                            displayFullName
                            displayJobTitle
                            signedTimestamp
                        }
                        approvers {
                            id
                            displayFullName
                            displayJobTitle
                            signedTimestamp
                        }
                        serials {
                            id
                            filingNumberDisplay
                        }
                        caseLabels {
                            id
                            displayValue
                            colorHexCode
                            caseId
                            caseTitle
                        }
                        relatedEntities {
                            id
                            entityType
                            createdTimestamp
                            updatedTimestamp
                            tags
                            comments
                        }
                    }
                }
            }
            """;

    private static final String FORM_DEFINITION_QUERY = """
            query getFormDefinition($id: ID!) {
                dynamicFormDefinition(id: $id) {
                    id
                    formName
                    versions {
                        versionNumber
                        status
                        formFields {
                            name
                            label
                            type
                            values {
                                value
                                label
                            }
                        }
                    }
                }
            }
            """;

    private static final String DOCUMENT_WITH_LOCATIONS_SELECTION = """
                    id
                    title
                    createdTimestamp
                    updatedTimestamp
                    relatedEntities {
                        entityType
                        ... on Location {
                            id
                            locationName
                            locationType
                            street1
                            street2
                            district
                            city
                            county
                            state
                            zipCode
                            country
                            latitude
                            longitude
                            full
                        }
                    }
                    """;

    private static final String FETCH_DYNAMIC_DOCUMENTS_WITH_LOCATIONS_QUERY = """
                    query dynamicDocuments(
                        $type: String,
                        $subType: String,
                        $startCreatedTimestamp: Int,
                        $endCreatedTimestamp: Int,
                        $startUpdatedTimestamp: Int,
                        $endUpdatedTimestamp: Int,
                        $start: Int,
                        $limit: Int
                    ) {
                        searchWithData(
                            type: $type
                            subType: $subType
                            startCreatedTimestamp: $startCreatedTimestamp
                            endCreatedTimestamp: $endCreatedTimestamp
                            startUpdatedTimestamp: $startUpdatedTimestamp
                            endUpdatedTimestamp: $endUpdatedTimestamp
                            start: $start
                            limit: $limit
                        ) {
                            ... on FinalDynamicDocument {
                                dynamicFormDefinitionId
                                %s
                            }
                            ... on Document {
                                %s
                            }
                        }
                    }
                    """;

    private static final String SEARCH_DOCUMENTS_WITH_LOCATIONS_QUERY = """
                    query ($type: String, $subType: String, $startTs: Int, $endTs: Int, $start: Int, $limit: Int) {
                        searchWithData(
                            type: $type
                            subType: $subType
                            startCreatedTimestamp: $startTs
                            endCreatedTimestamp: $endTs
                            startUpdatedTimestamp: $startTs
                            endUpdatedTimestamp: $endTs
                            start: $start
                            limit: $limit
                        ) {
                            ... on Document {
                                %s
                            }
                        }
                    }
                    """;

    private final KwProperties kwProperties;
    private final TokenCache tokenCache;
    private final KwDocumentMapper kwDocumentMapper;
    private final CloseableHttpClient kwHttpClient;
    private final ObjectMapper objectMapper;

    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0))
    public List<KwDocumentDto> queryDocumentsWithLocations(final ArcGISExecutionCommand cmd) {
        final String itemType = cmd.getItemType();
        final String itemSubtype = cmd.getItemSubtype();
        final String dynamicDocumentType = cmd.getDynamicDocumentType();
        final boolean hasDynamicDocumentType = dynamicDocumentType != null
            && !dynamicDocumentType.trim().isEmpty();

        Integer startTs = InstantConverters.toEpochSecondsInt(cmd.getWindowStart());
        Integer endTs = InstantConverters.toEpochSecondsInt(cmd.getWindowEnd());

        String gql;
        Map<String, Object> queryMap = new LinkedHashMap<>();
        if (hasDynamicDocumentType) {
            gql = FETCH_DYNAMIC_DOCUMENTS_WITH_LOCATIONS_QUERY.formatted(
                DOCUMENT_WITH_LOCATIONS_SELECTION,
                DOCUMENT_WITH_LOCATIONS_SELECTION);
            queryMap.put("type", DOCUMENT_TYPE);
            queryMap.put("subType", DOCUMENT_FINAL_DYNAMIC);
            queryMap.put("startCreatedTimestamp", startTs);
            queryMap.put("endCreatedTimestamp", endTs);
            queryMap.put("startUpdatedTimestamp", startTs);
            queryMap.put("endUpdatedTimestamp", endTs);
            queryMap.put("start", SEARCH_START);
            queryMap.put("limit", SEARCH_LIMIT);

            log.info(
                "KW GraphQL searchWithData request (FinalDynamicDocument mode): "
                    + "startCreatedTs={}, endCreatedTs={}, dynamicFormDefinitionId={}",
                startTs, endTs, dynamicDocumentType);
        } else {
            gql = SEARCH_DOCUMENTS_WITH_LOCATIONS_QUERY.formatted(DOCUMENT_WITH_LOCATIONS_SELECTION);
            queryMap.put("type", itemType);
            queryMap.put("subType", itemSubtype);
            queryMap.put("startTs", startTs);
            queryMap.put("endTs", endTs);
            queryMap.put("start", SEARCH_START);
            queryMap.put("limit", SEARCH_LIMIT);

            log.info(
                "KW GraphQL searchWithData request (type/subType mode): type={}, subType={}, "
                    + "startCreatedTs={}, endCreatedTs={}",
                itemType, itemSubtype, startTs, endTs);
        }

        Map<String, Object> payload = new LinkedHashMap<>(5);
        payload.put("query", gql);
        payload.put("variables", queryMap);

        HttpPost request = buildGraphQLRequest(getValidAccessToken(), payload);
        List<KwDocumentDto> documents;
        if (hasDynamicDocumentType) {
            documents = execute(request,
                response -> parseDynamicDocumentsResponse(response, dynamicDocumentType),
                "Failed to query documents with locations from KW");
        } else {
            documents = execute(request, this::parseDocumentLocationsResponse,
                "Failed to query documents with locations from KW");
        }
        log.info("KW GraphQL response: docsReturned={}", documents.size());
        return documents;
    }

    /**
     * Fetch monitoring documents (FinalDynamicDocument) from Kaseware GraphQL.
     * Optionally filters by {@code dynamicFormDefinitionId}.
     */
    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0))
    public JsonNode fetchMonitoringDocuments(String dynamicFormDefinitionId,
                                             Integer startTimestamp,
                                             Integer endTimestamp,
                                             int start,
                                             int limit) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("type", DOCUMENT_TYPE);
        variables.put("subType", DOCUMENT_FINAL_DYNAMIC);
        variables.put("start", start);
        variables.put("limit", limit);
        variables.put("startCreatedTimestamp", startTimestamp);
        variables.put("endCreatedTimestamp", endTimestamp);

        Map<String, Object> payload = Map.of(
                "query", MONITORING_DOCUMENTS_QUERY,
                "variables", variables);

        log.info("Fetching monitoring documents: dynamicFormDefId={}, startTs={}, endTs={}, start={}, limit={}",
                dynamicFormDefinitionId, startTimestamp, endTimestamp, start, limit);

        String token = getValidAccessToken();
        HttpPost request = buildGraphQLRequest(token, payload);
        return execute(request, response -> {
            String body = readBody(response);
            JsonNode root = objectMapper.readTree(body);
            validateGraphQLResponse(root);
            JsonNode results = normalizeSearchWithData(root.path("data").path("searchWithData"));

            // Filter by dynamicFormDefinitionId if provided
            if (dynamicFormDefinitionId != null && !dynamicFormDefinitionId.isBlank() && results.isArray()) {
                ArrayNode filtered = objectMapper.createArrayNode();
                for (JsonNode node : results) {
                    if (dynamicFormDefinitionId.equals(node.path("dynamicFormDefinitionId").asText(null))) {
                        filtered.add(node);
                    }
                }
                log.info("Monitoring documents: total={}, matchingFormDefId={}", results.size(), filtered.size());
                return filtered;
            }
            return results;
        }, "Failed to fetch monitoring documents from KW");
    }

    /**
     * Fetch a dynamic form definition by ID from Kaseware GraphQL.
     * Returns the full {@code dynamicFormDefinition} node including versions and formFields.
     */
    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0))
    public JsonNode fetchFormDefinition(String formDefinitionId) {
        Map<String, Object> payload = Map.of(
                "query", FORM_DEFINITION_QUERY,
                "variables", Map.of("id", formDefinitionId));

        log.info("Fetching form definition: id={}", formDefinitionId);

        String token = getValidAccessToken();
        HttpPost request = buildGraphQLRequest(token, payload);
        return execute(request, response -> {
            String body = readBody(response);
            JsonNode root = objectMapper.readTree(body);
            validateGraphQLResponse(root);
            return root.path("data").path("dynamicFormDefinition");
        }, "Failed to fetch form definition from KW");
    }

    public String executeGraphQLQuery(final Map<String, Object> payload) {
        String token = getValidAccessToken();
        return executeGraphQLQuery(token, payload);
    }

    private String executeGraphQLQuery(final String token, final Map<String, Object> payload) {
        HttpPost request = buildGraphQLRequest(token, payload);
        return execute(request, response -> {
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            validateGraphQLResponse(jsonNode);
            return responseBody;
        }, "Failed to execute GraphQL query");
    }

    private HttpPost buildGraphQLRequest(final String token, final Map<String, Object> payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpPost post = new HttpPost(kwProperties.getGraphql().getEndpoint());
            post.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return post;
        } catch (Exception e) {
            throw new IntegrationApiException("Failed to build GraphQL request", 502, e);
        }
    }

    private <T> T execute(
            final HttpUriRequest request,
            final ResponseHandler<T> handler,
            final String errorMessage) {
        try {
            return kwHttpClient.execute(request, response -> {
                validateResponse(response);
                try {
                    return handler.handle(response);
                } catch (IntegrationApiException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IntegrationApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw new IntegrationApiException(errorMessage, 502, e);
        }
    }

    private void validateResponse(final ClassicHttpResponse response) {
        int status = response.getCode();
        if (status < 200 || status >= 300) {
            throw new IntegrationApiException("KW API returned HTTP " + status, status);
        }
        if (response.getEntity() == null) {
            throw new IntegrationApiException("Empty HTTP response from KW", status);
        }
    }

    private String readBody(final ClassicHttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, StandardCharsets.UTF_8);
    }

    private void validateGraphQLResponse(final JsonNode root) {
        JsonNode errors = root.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new IntegrationApiException("KW GraphQL returned errors: " + errors, 502);
        }
    }

    private List<KwDocumentDto> parseDocumentLocationsResponse(final ClassicHttpResponse response) throws Exception {
        String responseBody = readBody(response);
        JsonNode root = objectMapper.readTree(responseBody);
        validateGraphQLResponse(root);
        JsonNode jsonNode = root.path("data").path("searchWithData");
        JsonNode normalized = normalizeSearchWithData(jsonNode);
        if (normalized != null && !normalized.isArray()) {
            log.error("Unexpected searchWithData shape: nodeType={}, fields={}",
                    normalized.getNodeType(), normalized.isObject() ? normalized.fieldNames() : "n/a");
            return List.of();
        }
        log.info("RAW KW response: total nodes returned by searchWithData={}",
                normalized != null ? normalized.size() : 0);
        return kwDocumentMapper.convertToDocumentDtos(normalized);
    }

    private List<KwDocumentDto> parseDynamicDocumentsResponse(
            final ClassicHttpResponse response,
            final String dynamicFormDefinitionId) throws Exception {
        String responseBody = readBody(response);
        JsonNode root = objectMapper.readTree(responseBody);
        validateGraphQLResponse(root);
        JsonNode searchWithDataNode = root.path("data").path("searchWithData");
        JsonNode normalized = normalizeSearchWithData(searchWithDataNode);
        if (normalized == null || !normalized.isArray()) {
            log.error("Unexpected dynamic searchWithData shape: nodeType={}",
                    normalized != null ? normalized.getNodeType() : "null");
            return List.of();
        }
        log.info("RAW KW response: total nodes returned by searchWithData={}",
                normalized.size());
        ArrayNode filtered = objectMapper.createArrayNode();
        for (JsonNode node : normalized) {
            if (dynamicFormDefinitionId.equals(node.path("dynamicFormDefinitionId").asText(null))) {
                filtered.add(node);
            }
        }
        log.info("After dynamicFormDefinitionId filter: rawTotal={}, matchingFormDef={}, dropped={}",
                normalized.size(), filtered.size(), normalized.size() - filtered.size());
        return kwDocumentMapper.convertToDocumentDtos(filtered);
    }

    private JsonNode normalizeSearchWithData(final JsonNode node) {
        if (node == null) {
            return objectMapper.getNodeFactory().missingNode();
        }
        if (node.isArray()) {
            return node;
        }
        if (node.isObject()) {
            JsonNode results = node.get("results");
            if (results != null && results.isArray()) {
                return results;
            }
            JsonNode items = node.get("items");
            if (items != null && items.isArray()) {
                return items;
            }
            JsonNode nodes = node.get("nodes");
            if (nodes != null && nodes.isArray()) {
                return nodes;
            }
        }
        return node;
    }

    public String getValidAccessToken() {
        return getValidAccessToken(kwProperties.getAuth().getUsername(), kwProperties.getAuth().getPassword());
    }

    public String getValidAccessToken(final String username, final String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String cachedToken = tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY);

        if (cachedToken != null) {
            log.debug("Using cached access token for KW.");
            return cachedToken;
        }

        log.info("Fetching new access token for KW");
        AuthResponse authResponse = fetchAccessToken(username, password);

        if (authResponse != null && authResponse.isValid()) {
            tokenCache.store(KW_GRAPHQL_TOKEN_CACHE_KEY, authResponse.getAccessToken(), authResponse.getExpiresIn());
            log.info("Successfully cached access token for KW with TTL: {} seconds", authResponse.getExpiresIn());
            return authResponse.getAccessToken();
        }
        throw new RuntimeException("Failed to obtain valid access token for KW.");
    }

    private AuthResponse fetchAccessToken(final String username, final String password) {
        HttpPost httpPost = new HttpPost(kwProperties.getAuth().getTokenUrl());

        try {
            // Set headers from configuration
            KwProperties.Auth.Headers headers = kwProperties.getAuth().getHeaders();
            httpPost.setHeader("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            httpPost.setHeader("Accept", "*/*");
            httpPost.setHeader("User-Agent", headers.getUserAgent());
            httpPost.setHeader("Accept-Encoding", headers.getAcceptEncoding());
            httpPost.setHeader("Connection", "keep-alive");
            httpPost.setHeader("Origin", headers.getOrigin());
            httpPost.setHeader("Referer", headers.getReferer());

            // Build form data
            List<NameValuePair> formParams = new java.util.ArrayList<>();
            formParams.add(new BasicNameValuePair("grant_type", kwProperties.getAuth().getGrantType()));
            formParams.add(new BasicNameValuePair("client_id", kwProperties.getAuth().getClientId()));
            formParams.add(new BasicNameValuePair("username", username));
            formParams.add(new BasicNameValuePair("password", password));

            if (kwProperties.getAuth().getScope() != null && !kwProperties.getAuth().getScope().isBlank()) {
                formParams.add(new BasicNameValuePair("scope", kwProperties.getAuth().getScope()));
            }

            httpPost.setEntity(new UrlEncodedFormEntity(formParams));

            log.debug("Requesting OAuth2 token from: {}", kwProperties.getAuth().getTokenUrl());

            // Execute request with automatic connection management
            return kwHttpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == HttpStatus.SC_OK && responseBody != null) {
                    JsonNode responseNode = objectMapper.readTree(responseBody);

                    AuthResponse authResponse = AuthResponse.builder()
                            .accessToken(responseNode.path("access_token").asText())
                            .tokenType(responseNode.path("token_type").asText("Bearer"))
                            .expiresIn(responseNode.path("expires_in").asLong(3600))
                            .refreshToken(responseNode.path("refresh_token").asText())
                            .scope(responseNode.path("scope").asText())
                            .issuedAt(java.time.Instant.now())
                            .build();

                    log.info("Successfully obtained access token for KW expires in: {} seconds",
                            authResponse.getExpiresIn());
                    return authResponse;
                }

                log.error("Failed to fetch access token. Status: {}, Body: {}", statusCode, responseBody);
                return null;
            });

        } catch (Exception e) {
            log.error("Error fetching access token, Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch OAuth2 token", e);
        }
    }

    @FunctionalInterface
    private interface ResponseHandler<T> {
        T handle(ClassicHttpResponse response) throws Exception;
    }
}
