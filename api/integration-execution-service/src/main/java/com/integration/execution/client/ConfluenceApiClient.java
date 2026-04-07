package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import com.integration.execution.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the Confluence Cloud REST API v1.
 * Handles search, create, and update of Confluence pages using Basic Auth (API token).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceApiClient {

    private static final String CONTENT_API_PATH = "/wiki/rest/api/content";
    private static final String USER_CURRENT_API_PATH = "/wiki/rest/api/user/current";
    private static final String USER_BULK_API_PATH = "/wiki/rest/api/user/bulk";

    private final VaultService vaultService;
    private final CloseableHttpClient confluenceHttpClient;
    private final ObjectMapper objectMapper;

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
    public Optional<ConfluencePage> searchPage(
            final String siteUrl,
            final String spaceKey,
            final String title,
            final String email,
            final String apiToken) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
            String uriStr = siteUrl + CONTENT_API_PATH
                    + "?title=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                    + "&spaceKey=" + spaceKey
                    + "&expand=version";
            HttpGet get = new HttpGet(uriStr);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            get.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            return confluenceHttpClient.execute(get, response -> {
                int status = response.getCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("Confluence search failed with HTTP " + status + ": " + body);
                }
                JsonNode root = objectMapper.readTree(body);
                JsonNode results = root.path("results");
                if (results.isArray() && !results.isEmpty()) {
                    JsonNode page = results.get(0);
                    String pageId = page.path("id").asText();
                    int version = page.path("version").path("number").asInt();
                    return Optional.of(new ConfluencePage(pageId, version));
                }
                return Optional.empty();
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search Confluence page: " + e.getMessage(), e);
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
    public String createPage(
            final String siteUrl,
            final String spaceKey,
            final String parentPageId,
            final String title,
            final String body,
            final String email,
            final String apiToken) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = buildCreatePayload(spaceKey, parentPageId, title, body);
            String json = objectMapper.writeValueAsString(payload);

            HttpPost post = new HttpPost(siteUrl + CONTENT_API_PATH);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            return confluenceHttpClient.execute(post, response -> {
                int status = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("Confluence create failed with HTTP " + status + ": " + responseBody);
                }
                JsonNode root = objectMapper.readTree(responseBody);
                return root.path("id").asText();
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Confluence page: " + e.getMessage(), e);
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
    public void updatePage(
            final String siteUrl,
            final String pageId,
            final String parentPageId,
            final String title,
            final String body,
            final int nextVersion,
            final String email,
            final String apiToken) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = buildUpdatePayload(parentPageId, title, body, nextVersion);
            String json = objectMapper.writeValueAsString(payload);

            HttpPut put = new HttpPut(siteUrl + CONTENT_API_PATH + "/" + pageId);
            put.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            put.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            put.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            confluenceHttpClient.execute(put, response -> {
                int status = response.getCode();
                if (status < 200 || status >= 300) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    throw new RuntimeException("Confluence update failed with HTTP " + status + ": " + responseBody);
                }
                return null;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update Confluence page: " + e.getMessage(), e);
        }
    }

    /**
     * Search for an existing page and create or update it accordingly.
     * Fetches credentials from Azure Key Vault using the provided secret name.
     *
     * @return result containing the final page URL and page id
     */
    public ConfluencePublishResult createOrUpdatePage(final ConfluencePublishRequest request) {
        IntegrationSecret secret = vaultService.getSecret(request.secretName());
        BasicAuthCredential credentials = (BasicAuthCredential) secret.getCredentials();
        String siteUrl = secret.getBaseUrl();
        String email = credentials.getUsername();
        String apiToken = credentials.getPassword();

        Optional<ConfluencePage> existing = searchPage(
                siteUrl, request.spaceKey(), request.pageTitle(),
                email, apiToken);

        String pageId;
        if (existing.isPresent()) {
            ConfluencePage page = existing.get();
            updatePage(siteUrl, page.id(), request.parentPageId(),
                    request.pageTitle(), request.body(),
                    page.version() + 1, email, apiToken);
            pageId = page.id();
            log.info("Updated existing Confluence page id={} title='{}'", pageId, request.pageTitle());
        } else {
            pageId = createPage(siteUrl, request.spaceKey(), request.parentPageId(),
                    request.pageTitle(), request.body(), email, apiToken);
            log.info("Created new Confluence page id={} title='{}'", pageId, request.pageTitle());
        }

        String pageUrl = siteUrl + "/wiki/spaces/" + request.spaceKey()
                + "/pages/" + pageId;
        return new ConfluencePublishResult(pageUrl, pageId);
    }

    /**
     * Verify Confluence credentials and account existence by calling the current-user endpoint.
     * Not retryable — connection tests must fail fast.
     *
     * @return the current user JSON (contains {@code accountId} and {@code displayName})
     * @throws RuntimeException on non-2xx response or I/O failure
     */
    public JsonNode getCurrentUser(final String siteUrl, final String email, final String apiToken) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
            HttpGet get = new HttpGet(siteUrl + USER_CURRENT_API_PATH);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            get.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            return confluenceHttpClient.execute(get, response -> {
                int status = response.getCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new RuntimeException(
                            "Confluence connection test failed with HTTP " + status + ": " + body);
                }
                return objectMapper.readTree(body);
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to test Confluence connection: " + e.getMessage(), e);
        }
    }

    public JsonNode getCurrentUser(final IntegrationSecret secret) {
        BasicAuthCredential cred = (BasicAuthCredential) secret.getCredentials();
        return getCurrentUser(secret.getBaseUrl(), cred.getUsername(), cred.getPassword());
    }

    public JsonNode getCurrentUser(final String secretName) {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        return getCurrentUser(secret);
    }

    public ZoneId getUserTimezone(final String siteUrl, final String email, final String apiToken,
                                  final ZoneId fallbackTimezone) {
        ZoneId fallback = fallbackTimezone != null ? fallbackTimezone : ZoneId.of("UTC");
        try {
            // Get current user to extract accountId
            JsonNode currentUser = getCurrentUser(siteUrl, email, apiToken);
            String accountId = currentUser.path("accountId").asText(null);

            if (accountId == null || accountId.isBlank()) {
                log.warn("Confluence user accountId not found, using fallback: {}", fallback.getId());
                return fallback;
            }

            // Fetch user details with timezone using bulk endpoint
            String encoded = Base64.getEncoder().encodeToString(
                    (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
            String uriStr = siteUrl + USER_BULK_API_PATH + "?accountId="
                    + URLEncoder.encode(accountId, StandardCharsets.UTF_8);
            HttpGet get = new HttpGet(uriStr);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            get.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

            JsonNode userDetails = confluenceHttpClient.execute(get, response -> {
                int status = response.getCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new RuntimeException(
                            "Confluence get user bulk failed with HTTP " + status + ": " + body);
                }
                JsonNode root = objectMapper.readTree(body);
                JsonNode results = root.path("results");
                if (results.isArray() && !results.isEmpty()) {
                    return results.get(0);
                }
                return objectMapper.createObjectNode();
            });

            String timezoneStr = userDetails.path("timeZone").asText(null);
            if (timezoneStr != null && !timezoneStr.isBlank()) {
                log.info("Using Confluence user timezone: {}", timezoneStr);
                return ZoneId.of(timezoneStr);
            }

            log.info("Confluence user timezone not found, using fallback: {}", fallback.getId());
            return fallback;
        } catch (Exception e) {
            log.warn("Failed to get Confluence user timezone, using fallback {}: {}",
                    fallback.getId(), e.getMessage());
            return fallback;
        }
    }

    public ZoneId getUserTimezone(final IntegrationSecret secret, final ZoneId fallbackTimezone) {
        BasicAuthCredential cred = (BasicAuthCredential) secret.getCredentials();
        return getUserTimezone(secret.getBaseUrl(), cred.getUsername(), cred.getPassword(), fallbackTimezone);
    }

    public ZoneId getUserTimezone(final String secretName, final ZoneId fallbackTimezone) {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        return getUserTimezone(secret, fallbackTimezone);
    }

    public ZoneId getUserTimezone(final String secretName) {
        return getUserTimezone(secretName, null);
    }

    public List<ConfluenceSpaceDto> getSpaces(final String secretName) {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        BasicAuthCredential cred = (BasicAuthCredential) secret.getCredentials();
        return getSpaces(secret.getBaseUrl(), cred.getUsername(), cred.getPassword());
    }

    public List<ConfluenceSpaceDto> getSpaces(
            final String siteUrl,
            final String email,
            final String apiToken) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
            String uriStr = siteUrl + "/wiki/rest/api/space?limit=250&expand=description.plain";
            HttpGet get = new HttpGet(uriStr);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            get.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

            return confluenceHttpClient.execute(get, response -> {
                int status = response.getCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new RuntimeException(
                            "Confluence get spaces failed with HTTP " + status + ": " + body);
                }
                JsonNode root = objectMapper.readTree(body);
                JsonNode results = root.path("results");
                List<ConfluenceSpaceDto> spaces = new ArrayList<>();
                if (results.isArray()) {
                    for (JsonNode spaceNode : results) {
                        String description = spaceNode.path("description")
                                .path("plain").path("value").asText("");
                        spaces.add(ConfluenceSpaceDto.builder()
                                .key(spaceNode.path("key").asText())
                                .name(spaceNode.path("name").asText())
                                .type(spaceNode.path("type").asText())
                                .description(description)
                                .build());
                    }
                }
                return spaces;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Confluence spaces: " + e.getMessage(), e);
        }
    }

    public List<ConfluencePageDto> getPages(final String secretName, final String spaceKey) {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        BasicAuthCredential cred = (BasicAuthCredential) secret.getCredentials();
        return getPages(secret.getBaseUrl(), spaceKey, cred.getUsername(), cred.getPassword());
    }

    public List<ConfluencePageDto> getPages(
            final String siteUrl,
            final String spaceKey,
            final String email,
            final String apiToken) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
            // v1 /content endpoint never returns folder-type content — use CQL search API instead
            String cql = "type = folder AND space.key = \"" + spaceKey + "\"";
            String uriStr = siteUrl + "/wiki/rest/api/search"
                    + "?cql=" + URLEncoder.encode(cql, StandardCharsets.UTF_8)
                    + "&limit=250&expand=content.ancestors";
            HttpGet get = new HttpGet(uriStr);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            get.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

            return confluenceHttpClient.execute(get, response -> {
                int status = response.getCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new RuntimeException(
                            "Confluence get folders failed with HTTP " + status + ": " + body);
                }
                JsonNode root = objectMapper.readTree(body);
                JsonNode results = root.path("results");
                List<ConfluencePageDto> pages = new ArrayList<>();
                if (results.isArray()) {
                    for (JsonNode resultNode : results) {
                        // CQL search API wraps each folder inside a "content" node
                        JsonNode folderNode = resultNode.path("content");
                        if (folderNode.isMissingNode()) {
                            continue;
                        }
                        JsonNode ancestors = folderNode.path("ancestors");
                        String parentTitle = null;
                        if (ancestors.isArray() && !ancestors.isEmpty()) {
                            parentTitle = ancestors.get(ancestors.size() - 1)
                                    .path("title").asText(null);
                        }
                        pages.add(ConfluencePageDto.builder()
                                .id(folderNode.path("id").asText())
                                .title(folderNode.path("title").asText())
                                .parentTitle(parentTitle)
                                .type("folder")
                                .build());
                    }
                }
                return pages;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Confluence folders: " + e.getMessage(), e);
        }
    }

    public record ConfluencePage(String id, int version) {
    }

    public record ConfluencePublishRequest(
            String secretName,
            String spaceKey,
            String parentPageId,
            String pageTitle,
            String body
    ) {
    }

    public record ConfluencePublishResult(String confluencePageUrl, String confluencePageId) {
    }

    private Map<String, Object> buildCreatePayload(
            final String spaceKey,
            final String parentPageId,
            final String title,
            final String body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "page");
        payload.put("title", title);
        payload.put("space", Map.of("key", spaceKey));
        payload.put("body", Map.of("storage", Map.of(
                "value", body,
                "representation", "storage")));
        payload.put("metadata", buildFullWidthMetadata());
        if (parentPageId != null && !parentPageId.isBlank()
                && parentPageId.chars().allMatch(Character::isDigit)) {
            payload.put("ancestors", List.of(Map.of("id", parentPageId)));
        }
        return payload;
    }

    private Map<String, Object> buildUpdatePayload(
            final String parentPageId,
            final String title,
            final String body,
            final int version) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "page");
        payload.put("title", title);
        payload.put("version", Map.of("number", version));
        payload.put("metadata", buildFullWidthMetadata());
        payload.put("body", Map.of("storage", Map.of(
                "value", body,
                "representation", "storage")));
        if (parentPageId != null && !parentPageId.isBlank()
                && parentPageId.chars().allMatch(Character::isDigit)) {
            payload.put("ancestors", List.of(Map.of("id", parentPageId)));
        }
        return payload;
    }

    private Map<String, Object> buildFullWidthMetadata() {
        return Map.of(
                "properties", Map.of(
                        "editor", Map.of("value", "v2"),
                        "content-appearance-draft", Map.of("value", "full-width"),
                        "content-appearance-published", Map.of("value", "full-width")));
    }
}
