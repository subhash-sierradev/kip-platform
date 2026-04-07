package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.config.cache.TokenCache;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.integration.execution.constants.ArcGisConstants.ARCGIS_FIELD_EXTERNAL_LOCATION_ID;
import static com.integration.execution.constants.ArcGisConstants.ARCGIS_FIELD_OBJECTID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArcGISApiClient {

    private final TokenCache tokenCache;
    private final CloseableHttpClient arcgisHttpClient;
    private final VaultService vaultService;
    private final ObjectMapper mapper;

    public String getAccessToken(String secretName) {
        String cached = tokenCache.getValidToken(secretName);
        if (cached != null) {
            return cached;
        }
        IntegrationSecret secret = vaultService.getSecret(secretName);
        return getAccessToken(secretName, secret);
    }

    public String getAccessToken(String secretName, IntegrationSecret secret) {
        if (!(secret.getCredentials() instanceof OAuthClientCredential oauth)) {
            throw new IntegrationApiException("Invalid credential type for ArcGIS OAuth2", 400);
        }
        HttpPost post = buildTokenRequest(oauth);
        return this.execute(post, response -> {
            JsonNode json = readJson(response);

            String token = json.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new IntegrationApiException("Token missing in response", 401);
            }
            Long ttl = json.has("expires_in")
                    ? json.get("expires_in").asLong()
                    : null;
            if (secretName != null && !secretName.isBlank()) {
                tokenCache.store(secretName, token, ttl);
            }
            return token;
        }, "Token generation failed");
    }

    private HttpPost buildTokenRequest(OAuthClientCredential oauth) {
        List<NameValuePair> params = List.of(
                new BasicNameValuePair("client_id", oauth.getClientId()),
                new BasicNameValuePair("client_secret", oauth.getClientSecret()),
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("f", "json")
        );
        HttpPost post = new HttpPost(oauth.getTokenUrl());
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        post.setHeader(HttpHeaders.CONTENT_TYPE,
                ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        return post;
    }

    public JsonNode queryFeatures(String secretName) {
        IntegrationSecret secret = vaultService.getSecret(secretName);

        URI uri = UriComponentsBuilder
                .fromUriString(secret.getBaseUrl())
                .pathSegment("0")
                .queryParam("f", "json")
                .queryParam("outFields", "*")
                .queryParam("returnGeometry", false)
                .queryParam("orderByFields", ARCGIS_FIELD_OBJECTID)
                .queryParam("token", getAccessToken(secretName))
                .build(true)
                .toUri();

        return execute(new HttpGet(uri), this::readArcGisJson,
                "ArcGIS query failed");
    }

    public JsonNode queryFeaturesWithWhere(String secretName, String whereClause) {
        IntegrationSecret secret = vaultService.getSecret(secretName);

        URI uri = UriComponentsBuilder
                .fromUriString(secret.getBaseUrl())
                .pathSegment("0", "query")
                .queryParam("f", "json")
                .queryParam("where", whereClause)
                .queryParam("outFields",
                        ARCGIS_FIELD_OBJECTID + "," + ARCGIS_FIELD_EXTERNAL_LOCATION_ID)
                .queryParam("returnGeometry", false)
                .queryParam("token", getAccessToken(secretName))
                .build()
                .encode()
                .toUri();

        return execute(new HttpGet(uri), this::readArcGisJson,
                "ArcGIS query failed");
    }

    public JsonNode applyEditsWithPartition(String secretName, String payloadJson) {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        String token = getAccessToken(secretName);
        String url = secret.getBaseUrl().replaceAll("/?$", "/0/applyEdits");
        try {
            JsonNode payload = mapper.readTree(payloadJson);
            List<NameValuePair> params = List.of(
                    new BasicNameValuePair("adds",
                            payload.has("adds")
                                    ? mapper.writeValueAsString(payload.get("adds"))
                                    : "[]"),
                    new BasicNameValuePair("updates",
                            payload.has("updates")
                                    ? mapper.writeValueAsString(payload.get("updates"))
                                    : "[]"),
                    new BasicNameValuePair("f", "json"),
                    new BasicNameValuePair("token", token)
            );

            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            post.setHeader(HttpHeaders.CONTENT_TYPE,
                    ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

            return execute(post, this::readArcGisJson,
                    "ArcGIS applyEdits failed");

        } catch (Exception e) {
            throw wrap("ArcGIS applyEdits failed", e);
        }
    }

    private <T> T execute(
            HttpUriRequest request,
            ResponseHandler<T> handler,
            String errorMessage
    ) {
        try {
            return arcgisHttpClient.execute(request, response -> {
                validateResponse(response);
                try {
                    return handler.handle(response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IntegrationApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw wrap(errorMessage, e);
        }
    }

    private void validateResponse(ClassicHttpResponse response) {
        int status = response.getCode();
        if (status < 200 || status >= 300) {
            throw new IntegrationApiException("HTTP " + status, status);
        }
        if (response.getEntity() == null) {
            throw new IntegrationApiException("Empty HTTP response", status);
        }
    }

    private JsonNode readJson(ClassicHttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        return mapper.readTree(
                EntityUtils.toString(entity, StandardCharsets.UTF_8));
    }

    private JsonNode readArcGisJson(ClassicHttpResponse response) throws Exception {
        JsonNode node = readJson(response);

        if (node.has("error")) {
            JsonNode err = node.get("error");
            throw new IntegrationApiException(
                    err.path("message").asText("ArcGIS error"),
                    err.path("code").asInt(response.getCode())
            );
        }
        return node;
    }

    private IntegrationApiException wrap(String msg, Exception e) {
        return new IntegrationApiException(msg, 502, e);
    }

    public List<ArcGISFieldDto> fetchArcGISFields(final String secretName) {
        JsonNode arcgisNode = queryFeatures(secretName);
        JsonNode fieldsNode = arcgisNode.get("fields");
        if (fieldsNode == null || !fieldsNode.isArray()) {
            log.warn("ArcGIS response missing 'fields' array");
            return List.of();
        }
        List<ArcGISFieldDto> fields = new ArrayList<>();
        for (JsonNode fieldNode : fieldsNode) {
            fields.add(mapFieldNodeToArcGISField(fieldNode));
        }
        return fields;
    }

    private ArcGISFieldDto mapFieldNodeToArcGISField(final JsonNode fieldNode) {
        ArcGISFieldDto.ArcGISFieldDtoBuilder builder = ArcGISFieldDto.builder()
                .name(fieldNode.path("name").asText(null))
                .type(fieldNode.path("type").asText(null))
                .alias(fieldNode.path("alias").asText(null))
                .sqlType(fieldNode.path("sqlType").asText(null))
                .nullable(fieldNode.path("nullable").asBoolean(true))
                .editable(fieldNode.path("editable").asBoolean(true));

        builder.domain(getNullableNode(fieldNode, "domain"));
        builder.defaultValue(getNullableNode(fieldNode, "defaultValue"));
        builder.length(getNullableInt(fieldNode, "length"));
        builder.precision(getNullableInt(fieldNode, "precision"));
        builder.description(getNullableText(fieldNode, "description"));
        return builder.build();
    }

    private Integer getNullableInt(final JsonNode node, final String fieldName) {
        if (!node.has(fieldName) || node.path(fieldName).isNull()) {
            return null;
        }
        return node.path(fieldName).asInt();
    }

    private String getNullableText(final JsonNode node, final String fieldName) {
        if (!node.has(fieldName) || node.path(fieldName).isNull()) {
            return null;
        }
        return node.path(fieldName).asText();
    }

    private JsonNode getNullableNode(final JsonNode node, final String fieldName) {
        if (!node.has(fieldName) || node.path(fieldName).isNull()) {
            return null;
        }
        return node.path(fieldName);
    }

    @FunctionalInterface
    private interface ResponseHandler<T> {
        T handle(ClassicHttpResponse response) throws Exception;
    }
}
