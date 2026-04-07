package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.auth.ConnectionAuthHeaderBuilder;
import com.integration.execution.config.properties.JiraApiProperties;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.service.VaultService;
import com.integration.execution.util.JiraRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraApiClient {

    private final VaultService vaultService;
    private final CloseableHttpClient jiraHttpClient;
    private final ObjectMapper objectMapper;
    private final JiraRateLimiter rateLimiter;
    private final JiraApiProperties jiraApiProperties;
    private final ConnectionAuthHeaderBuilder authHeaderBuilder;

    public JsonNode get(String endpoint, String secretName) throws IntegrationApiException {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        String url = buildUrl(secret.getBaseUrl(), endpoint);
        Map<String, String> headers = buildRequestHeaders(secret);
        return executeWithRateLimit(secret.getBaseUrl(), () -> {
            HttpGet httpGet = buildGetRequest(url, headers);
            return jiraHttpClient.execute(httpGet, response -> parseJsonResponse(response, url));
        });
    }

    public JsonNode get(String endpoint, IntegrationSecret secret) throws IntegrationApiException {
        String url = buildUrl(secret.getBaseUrl(), endpoint);
        Map<String, String> headers = buildRequestHeaders(secret);
        return executeWithRateLimit(secret.getBaseUrl(), () -> {
            HttpGet httpGet = buildGetRequest(url, headers);
            return jiraHttpClient.execute(httpGet, response -> parseJsonResponse(response, url));
        });
    }

    public String getString(String endpoint, String secretName) throws IntegrationApiException {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        String url = buildUrl(secret.getBaseUrl(), endpoint);
        Map<String, String> headers = buildRequestHeaders(secret);
        return executeWithRateLimit(secret.getBaseUrl(), () -> {
            HttpGet httpGet = buildGetRequest(url, headers);
            return jiraHttpClient.execute(httpGet, response -> parseStringResponse(response, url));
        });
    }

    public String getString(String endpoint, IntegrationSecret secret) throws IntegrationApiException {
        String url = buildUrl(secret.getBaseUrl(), endpoint);
        Map<String, String> headers = buildRequestHeaders(secret);
        return executeWithRateLimit(secret.getBaseUrl(), () -> {
            HttpGet httpGet = buildGetRequest(url, headers);
            return jiraHttpClient.execute(httpGet, response -> parseStringResponse(response, url));
        });
    }

    public ApiResponse post(String endpoint, Object requestBody, String secretName)
            throws IntegrationApiException {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        String url = buildUrl(secret.getBaseUrl(), endpoint);
        Map<String, String> headers = buildRequestHeaders(secret);
        return executeWithRateLimit(secret.getBaseUrl(), () -> {
            HttpPost httpPost = buildPostRequest(url, requestBody, headers);
            return jiraHttpClient.execute(httpPost, response -> parseApiResponse(response, url));
        });
    }

    public ApiResponse put(String endpoint, Object requestBody, String secretName)
            throws IntegrationApiException {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        String url = buildUrl(secret.getBaseUrl(), endpoint);
        Map<String, String> headers = buildRequestHeaders(secret);
        return executeWithRateLimit(secret.getBaseUrl(), () -> {
            HttpPut httpPut = buildPutRequest(url, requestBody, headers);
            return jiraHttpClient.execute(httpPut, response -> parseApiResponse(response, url));
        });
    }

    public ApiResponse delete(String baseUrl, String endpoint, String secretName)
            throws IntegrationApiException {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        String url = buildUrl(baseUrl, endpoint);
        Map<String, String> headers = buildRequestHeaders(secret);
        return executeWithRateLimit(baseUrl, () -> {
            HttpDelete httpDelete = buildDeleteRequest(url, headers);
            return jiraHttpClient.execute(httpDelete, response -> parseApiResponse(response, url));
        });
    }

    public JsonNode searchProjects(String secretName) throws IntegrationApiException {
        String endpoint = jiraApiProperties.getPaths().getProjectSearch();
        return get(endpoint, secretName);
    }

    public JsonNode getAssignableUsers(String secretName, String project)
            throws IntegrationApiException {
        String endpoint = jiraApiProperties.getPaths().getUserAssignableSearch() + "?project=" + project;
        return get(endpoint, secretName);
    }

    public JsonNode getProjectStatuses(String projectKey, String secretName)
            throws IntegrationApiException {
        String endpoint = jiraApiProperties.getPaths().getProjectStatuses().replace("{projectKey}", projectKey);
        return get(endpoint, secretName);
    }

    public JsonNode getFields(String secretName) throws IntegrationApiException {
        return get(jiraApiProperties.getPaths().getFields(), secretName);
    }

    /**
     * Fetches Jira create-metadata fields for a given project and issue type.
     */
    public JsonNode getProjectMetaFields(String projectKey, String secretName)
            throws IntegrationApiException {
        String endpoint = jiraApiProperties.getPaths().getCreateMetaFields()
                .replace("{projectKey}", projectKey);
        return get(endpoint, secretName);
    }

    public JsonNode searchParentIssues(
            String secretName,
            String projectKey,
            String query,
            Integer startAt,
            Integer maxResults)
            throws IntegrationApiException {

        StringBuilder jqlBuilder = new StringBuilder("project = \"")
                .append(escapeJqlValue(projectKey))
                .append("\" ")
                .append("AND issuetype not in (subTaskIssueTypes(), Epic) ");

        String normalizedQuery = query == null ? "" : query.trim();

        if (!normalizedQuery.isEmpty()) {

            String escapedQuery = escapeJqlValue(normalizedQuery);

            jqlBuilder.append("AND (");

            if (isIssueKeyQuery(normalizedQuery)) {

                String normalizedKey = normalizeIssueKey(normalizedQuery);

                jqlBuilder.append("issueKey ~ \"")
                        .append(escapeJqlValue(normalizedKey))
                        .append("*\" OR ");
            }

            jqlBuilder.append("summary ~ \"")
                    .append(escapedQuery)
                    .append("*\" ");

            jqlBuilder.append(") ");
        }

        jqlBuilder.append("ORDER BY updated DESC");

        String endpoint = UriComponentsBuilder
                .fromPath(jiraApiProperties.getPaths().getIssueSearch())
                .queryParam("jql", jqlBuilder.toString())
                .queryParam("fields", "summary,issuetype")
                .queryParam("startAt", startAt == null ? 0 : startAt)
                .queryParam("maxResults", maxResults == null ? 100 : maxResults)
                .build()
                .encode()
                .toUriString();

        return get(endpoint, secretName);
    }

    private String escapeJqlValue(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private boolean isIssueKeyQuery(String query) {
        if (query == null) {
            return false;
        }

        return query.matches("^[A-Za-z][A-Za-z0-9_]*-\\d+$");
    }

    private String normalizeIssueKey(String query) {
        return query.trim().toUpperCase(Locale.ROOT);
    }

    public ApiResponse createIssue(Object issuePayload, String secretName)
            throws IntegrationApiException {
        return post(jiraApiProperties.getPaths().getIssueCreate(), issuePayload, secretName);
    }

    public ApiResponse sendToJira(String secretName, String jiraPayload) {
        try {
            return createIssue(jiraPayload, secretName);
        } catch (Exception e) {
            log.error("Failed to send webhook data to Jira for connection with secretName {}: {}",
                    secretName, e.getMessage(), e);
            return new ApiResponse(500, false,
                    "Failed to send webhook data to Jira: " + e.getMessage());
        }
    }

    public JsonNode getBoardsByProject(String secretName, String projectKey) throws IntegrationApiException {
        String endpoint = jiraApiProperties.getPaths().getBoardsByProject()
                .replace("{projectKey}", projectKey);
        return get(endpoint, secretName);
    }

    public JsonNode getSprintsByBoard(String secretName, long boardId, Integer startAt,
            Integer maxResults, String state)
            throws IntegrationApiException {
        String path = jiraApiProperties.getPaths().getSprintsByBoard()
                .replace("{boardId}", String.valueOf(boardId));

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (startAt != null) {
            builder.queryParam("startAt", startAt);
        }
        if (maxResults != null) {
            builder.queryParam("maxResults", maxResults);
        }
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state.trim());
        }

        String endpoint = builder.build().toUriString();
        return get(endpoint, secretName);
    }

    public JsonNode searchTeams(String secretName, String query, Integer startAt, Integer maxResults)
            throws IntegrationApiException {
        StringBuilder endpoint = new StringBuilder(jiraApiProperties.getPaths().getTeamsSearch());
        boolean hasQuery = false;
        if (query != null) {
            endpoint.append("?query=").append(query.trim());
            hasQuery = true;
        }
        if (startAt != null) {
            endpoint.append(hasQuery ? "&" : "?").append("startAt=").append(startAt);
            hasQuery = true;
        }
        if (maxResults != null) {
            endpoint.append(hasQuery ? "&" : "?").append("maxResults=").append(maxResults);
        }
        try {
            return get(endpoint.toString(), secretName);
        } catch (Exception e) {
            throw new IntegrationApiException("Team search API call failed: " + e.getMessage(), 400, e);
        }
    }

    private <T> T executeWithRateLimit(String baseUrl, JiraRateLimiter.JiraApiCall<T> apiCall)
            throws IntegrationApiException {
        try {
            return rateLimiter.executeWithRateLimit(baseUrl, apiCall);
        } catch (Exception e) {
            if (e instanceof IntegrationApiException) {
                throw (IntegrationApiException) e;
            }
            throw new IntegrationApiException("Rate limited API call failed: " + e.getMessage(), 500, e);
        }
    }

    private String buildUrl(String baseUrl, String endpoint) {
        String cleanBaseUrl = StringUtils.trimTrailingCharacter(baseUrl.trim(), '/');
        String cleanEndpoint = StringUtils.hasText(endpoint) ? endpoint.trim() : "";

        if (!cleanEndpoint.startsWith("/")) {
            cleanEndpoint = "/" + cleanEndpoint;
        }

        return cleanBaseUrl + cleanEndpoint;
    }

    private Map<String, String> buildRequestHeaders(IntegrationSecret secret) {
        if (secret == null) {
            throw new IntegrationApiException(
                    "IntegrationSecret must not be null while building request headers",
                    401);
        }
        // Start with common headers
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> authHeaders = authHeaderBuilder.buildAuthHeaders(secret);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            headers.putAll(authHeaders);
        }
        return headers;
    }

    private HttpGet buildGetRequest(String url, Map<String, String> headers) {
        HttpGet request = new HttpGet(url);
        headers.forEach(request::setHeader);
        return request;
    }

    private HttpPost buildPostRequest(String url, Object requestBody, Map<String, String> headers)
            throws IntegrationApiException {
        HttpPost request = new HttpPost(url);
        headers.forEach(request::setHeader);

        if (requestBody != null) {
            try {
                String jsonPayload = requestBody instanceof String
                        ? (String) requestBody
                        : objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
            } catch (Exception e) {
                throw new IntegrationApiException("Failed to serialize request body: " + e.getMessage(), 400, e);
            }
        }

        return request;
    }

    private HttpPut buildPutRequest(String url, Object requestBody, Map<String, String> headers)
            throws IntegrationApiException {
        HttpPut request = new HttpPut(url);
        headers.forEach(request::setHeader);
        if (requestBody != null) {
            try {
                String jsonPayload = requestBody instanceof String
                        ? (String) requestBody
                        : objectMapper.writeValueAsString(requestBody);
                request.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
            } catch (Exception e) {
                throw new IntegrationApiException("Failed to serialize request body: " + e.getMessage(), 400, e);
            }
        }
        return request;
    }

    private HttpDelete buildDeleteRequest(String url, Map<String, String> headers) {
        HttpDelete request = new HttpDelete(url);
        headers.forEach(request::setHeader);
        return request;
    }

    private JsonNode parseJsonResponse(ClassicHttpResponse response, String url) throws IntegrationApiException {
        try {
            int statusCode = response.getCode();
            String responseBody = getResponseBody(response);

            if (statusCode == HttpStatus.SC_OK) {
                return objectMapper.readTree(responseBody);
            } else {
                String errorMessage = String.format(
                        "Jira API call failed. Status: %d, URL: %s, Body: %s",
                        statusCode, url, responseBody);
                log.error(errorMessage);
                throw new IntegrationApiException(errorMessage, statusCode);
            }
        } catch (IOException e) {
            throw new IntegrationApiException("Failed to parse JSON response: " + e.getMessage(), 500, e);
        }
    }

    private String parseStringResponse(ClassicHttpResponse response, String url) throws IntegrationApiException {
        int statusCode = response.getCode();
        String responseBody = getResponseBody(response);

        if (statusCode == HttpStatus.SC_OK) {
            return responseBody;
        } else {
            String errorMessage = String.format(
                    "Jira API call failed. Status: %d, URL: %s, Body: %s",
                    statusCode, url, responseBody);
            log.error(errorMessage);
            throw new IntegrationApiException(errorMessage, statusCode);
        }
    }

    private ApiResponse parseApiResponse(ClassicHttpResponse response, String url) {
        try {
            int statusCode = response.getCode();
            String responseBody = getResponseBody(response);
            boolean isSuccess = statusCode >= 200 && statusCode < 300;
            if (!isSuccess) {
                log.warn("Jira API request failed with status {}: {} for URL: {}", statusCode, responseBody, url);
            } else {
                log.debug("Jira API request successful with status {} for URL: {}", statusCode, url);
            }
            return new ApiResponse(statusCode, isSuccess, responseBody);
        } catch (Exception e) {
            log.error("Failed to process HTTP response for URL: {}", url, e);
            return new ApiResponse(503, false, "Failed to process response");
        }
    }

    private String getResponseBody(ClassicHttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";
        } catch (IOException | ParseException e) {
            log.error("Failed to read response body", e);
            return "{\"error\":\"Failed to read response\"}";
        }
    }

    public JsonNode fallbackJsonResponse(String endpoint, String secretName, Exception ex) {
        log.error("Circuit breaker activated for Jira API GET request to {}", endpoint, ex);
        return objectMapper.createObjectNode()
                .put("error", "Service temporarily unavailable");
    }

    public String fallbackStringResponse(String endpoint, String secretName, Exception ex) {
        log.error("Circuit breaker activated for Jira API GET string request to {}", endpoint, ex);
        return "Service temporarily unavailable";
    }

    public String fallbackStringResponse(String endpoint, IntegrationSecret secret, Exception ex) {
        log.error("Circuit breaker activated for Jira API GET string request to {}", endpoint, ex);
        return "Service temporarily unavailable";
    }

    public ApiResponse fallbackPostApiResponse(String endpoint, Object requestBody, String secretName, Exception ex) {
        log.error("Circuit breaker activated for Jira API POST request to {}", endpoint, ex);
        return new ApiResponse(503, false, "Service temporarily unavailable");
    }

    public ApiResponse fallbackPutApiResponse(String endpoint, Object requestBody, String secretName, Exception ex) {
        log.error("Circuit breaker activated for Jira API PUT request to {}", endpoint, ex);
        return new ApiResponse(503, false, "Service temporarily unavailable");
    }

    public ApiResponse fallbackDeleteApiResponse(String baseUrl, String endpoint, String secretName, Exception ex) {
        log.error("Circuit breaker activated for Jira API DELETE request to {}{}", baseUrl, endpoint, ex);
        return new ApiResponse(503, false, "Service temporarily unavailable");
    }
}
