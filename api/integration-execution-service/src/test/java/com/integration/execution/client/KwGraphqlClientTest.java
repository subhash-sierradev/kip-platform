package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.config.cache.TokenCache;
import com.integration.execution.config.properties.KwProperties;
import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.mapper.KwDocumentMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.integration.execution.constants.KasewareConstants.KW_GRAPHQL_TOKEN_CACHE_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KwGraphqlClientTest {

    @Mock
    private TokenCache tokenCache;

    @Mock
    private KwDocumentMapper kwDocumentMapper;

    @Mock
    private CloseableHttpClient kwHttpClient;

    @Mock
    private ClassicHttpResponse classicHttpResponse;

    private KwGraphqlClient client;

    @BeforeEach
    void setUp() {
        KwProperties properties = properties();
        client = new KwGraphqlClient(properties, tokenCache, kwDocumentMapper, kwHttpClient, new ObjectMapper());
    }

    @Test
    void getValidAccessToken_cachedToken_returnsFromCache() {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");

        String token = client.getValidAccessToken();

        assertThat(token).isEqualTo("cached-token");
    }

    @Test
    void getValidAccessToken_blankUsername_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> client.getValidAccessToken(" ", "pwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot be null or empty");
    }

    @Test
    void getValidAccessToken_fetchesAndCachesToken() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn(null);
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"new-token\",\"expires_in\":120}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getValidAccessToken();

        assertThat(token).isEqualTo("new-token");
        verify(tokenCache).store(KW_GRAPHQL_TOKEN_CACHE_KEY, "new-token", 120L);
    }

    @Test
    void executeGraphQLQuery_graphqlErrors_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"errors\":[{\"message\":\"boom\"}]}"));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.executeGraphQLQuery(Map.of("query", "{ a }")))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("KW GraphQL returned errors");
    }

    @Test
    void queryDocumentsWithLocations_validArrayResponse_returnsMappedDocs() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":[{\"id\":\"doc-1\"}]}}"
        ));
        mockHttpExecution(classicHttpResponse);
        when(kwDocumentMapper.convertToDocumentDtos(any()))
                .thenReturn(List.of(new KwDocumentDto("doc-1", "Doc", "DOCUMENT", 1L, 2L)));

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getId()).isEqualTo("doc-1");
    }

    @Test
    void queryDocumentsWithLocations_unexpectedShape_returnsEmptyList() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":{\"foo\":\"bar\"}}}"
        ));
        mockHttpExecution(classicHttpResponse);

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).isEmpty();
    }

    @Test
    void queryDocumentsWithLocations_non200Response_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(500);
        mockHttpExecution(classicHttpResponse);

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        assertThatThrownBy(() -> client.queryDocumentsWithLocations(cmd))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("KW API returned HTTP 500");
    }

    @Test
    void getValidAccessToken_blankPassword_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> client.getValidAccessToken("user", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null or empty");
    }

    @Test
    void getValidAccessToken_tokenRequestFails_throwsRuntimeException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn(null);
        when(classicHttpResponse.getCode()).thenReturn(401);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("unauthorized"));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.getValidAccessToken())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to obtain valid access token for KW");
    }

    @Test
    void queryDocumentsWithLocations_dynamicDocumentType_fetchesAndFiltersClientSide() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":["
                + "{\"id\":\"doc-1\",\"dynamicFormDefinitionId\":\"dyn-123\"}"
                + ",{\"id\":\"doc-2\",\"dynamicFormDefinitionId\":\"other-456\"}"
                + "]}}"
        ));
        mockHttpExecution(classicHttpResponse);
        ArgumentCaptor<JsonNode> mapperCaptor = ArgumentCaptor.forClass(JsonNode.class);
        when(kwDocumentMapper.convertToDocumentDtos(mapperCaptor.capture()))
                .thenReturn(List.of(new KwDocumentDto("doc-1", "Doc", "DOCUMENT_FINAL_DYNAMIC", 1L, 2L)));

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .dynamicDocumentType("dyn-123")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(kwHttpClient).execute(requestCaptor.capture(), any(HttpClientResponseHandler.class));
        String requestBody = EntityUtils.toString(((HttpPost) requestCaptor.getValue()).getEntity());

        assertThat(documents).hasSize(1);
        assertThat(requestBody).contains("DOCUMENT_FINAL_DYNAMIC");
        assertThat(requestBody).doesNotContain("termsFilters");
        // Only the document matching dynamicFormDefinitionId must have reached the mapper
        assertThat(mapperCaptor.getValue()).hasSize(1);
        assertThat(mapperCaptor.getValue().iterator().next().path("id").asText()).isEqualTo("doc-1");
    }

    @Test
    void queryDocumentsWithLocations_itemsContainer_normalizesAndMaps() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":{\"items\":[{\"id\":\"doc-2\"}]}}}"
        ));
        mockHttpExecution(classicHttpResponse);
        when(kwDocumentMapper.convertToDocumentDtos(any()))
                .thenReturn(List.of(new KwDocumentDto("doc-2", "Doc2", "DOCUMENT", 1L, 2L)));

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getId()).isEqualTo("doc-2");
    }

    @Test
    void queryDocumentsWithLocations_nodesContainer_normalizesAndMaps() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":{\"nodes\":[{\"id\":\"doc-3\"}]}}}"
        ));
        mockHttpExecution(classicHttpResponse);
        when(kwDocumentMapper.convertToDocumentDtos(any()))
                .thenReturn(List.of(new KwDocumentDto("doc-3", "Doc3", "DOCUMENT", 1L, 2L)));

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getId()).isEqualTo("doc-3");
    }

    @Test
    void queryDocumentsWithLocations_emptyHttpEntity_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(null);
        mockHttpExecution(classicHttpResponse);

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        assertThatThrownBy(() -> client.queryDocumentsWithLocations(cmd))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Empty HTTP response from KW");
    }

    @Test
    void executeGraphQLQuery_nonSerializablePayload_throwsIntegrationApiException() {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        Map<String, Object> cyclicPayload = new HashMap<>();
        cyclicPayload.put("query", "query { x }");
        cyclicPayload.put("self", cyclicPayload);

        assertThatThrownBy(() -> client.executeGraphQLQuery(cyclicPayload))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Failed to build GraphQL request");
    }

    @Test
    void queryDocumentsWithLocations_dynamicDocumentType_unexpectedShape_returnsEmptyList() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":{\"unexpected\":\"shape\"}}}"
        ));
        mockHttpExecution(classicHttpResponse);

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT").itemSubtype("REPORT")
                .dynamicDocumentType("dyn-123")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).isEmpty();
    }

    @Test
    void queryDocumentsWithLocations_dynamicDocumentType_graphqlErrors_throwsIntegrationApiException()
            throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"errors\":[{\"message\":\"dynamic fetch failed\"}]}"
        ));
        mockHttpExecution(classicHttpResponse);

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT").itemSubtype("REPORT")
                .dynamicDocumentType("dyn-123")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        assertThatThrownBy(() -> client.queryDocumentsWithLocations(cmd))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("KW GraphQL returned errors");
    }

    @Test
    void queryDocumentsWithLocations_dynamicDocumentType_noMatchingId_returnsEmpty() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":["
                + "{\"id\":\"doc-1\",\"dynamicFormDefinitionId\":\"other-111\"},"
                + "{\"id\":\"doc-2\",\"dynamicFormDefinitionId\":\"other-222\"}"
                + "]}}"
        ));
        mockHttpExecution(classicHttpResponse);
        when(kwDocumentMapper.convertToDocumentDtos(any())).thenReturn(List.of());

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT").itemSubtype("REPORT")
                .dynamicDocumentType("dyn-999")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).isEmpty();
    }

    @Test
    void fetchMonitoringDocuments_withFormDefinitionId_filtersResults() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":["
                + "{\"id\":\"doc-1\",\"dynamicFormDefinitionId\":\"match-123\",\"title\":\"Report 1\"},"
                + "{\"id\":\"doc-2\",\"dynamicFormDefinitionId\":\"other-456\",\"title\":\"Report 2\"},"
                + "{\"id\":\"doc-3\",\"dynamicFormDefinitionId\":\"match-123\",\"title\":\"Report 3\"}"
                + "]}}"
        ));
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.fetchMonitoringDocuments("match-123", 1609459200, 1609545600, 0, 100);

        assertThat(result.isArray()).isTrue();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).path("id").asText()).isEqualTo("doc-1");
        assertThat(result.get(1).path("id").asText()).isEqualTo("doc-3");
    }

    @Test
    void fetchMonitoringDocuments_withoutFormDefinitionId_returnsAllResults() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":["
                + "{\"id\":\"doc-1\",\"title\":\"Report 1\"},"
                + "{\"id\":\"doc-2\",\"title\":\"Report 2\"}"
                + "]}}"
        ));
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.fetchMonitoringDocuments(null, 1609459200, 1609545600, 0, 100);

        assertThat(result.isArray()).isTrue();
        assertThat(result).hasSize(2);
    }

    @Test
    void fetchMonitoringDocuments_blankFormDefinitionId_returnsAllResults() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":[{\"id\":\"doc-1\"}]}}"
        ));
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.fetchMonitoringDocuments("  ", 1609459200, 1609545600, 0, 100);

        assertThat(result.isArray()).isTrue();
        assertThat(result).hasSize(1);
    }

    @Test
    void fetchMonitoringDocuments_nonArrayResult_returnsAsIs() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":{\"totalCount\":5,\"results\":[{\"id\":\"doc-1\"}]}}}"
        ));
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.fetchMonitoringDocuments(null, 1609459200, 1609545600, 0, 100);

        assertThat(result.isArray()).isTrue();
        assertThat(result).hasSize(1); // normalizeSearchWithData extracts "results"
    }

    @Test
    void fetchMonitoringDocuments_graphqlErrors_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"errors\":[{\"message\":\"Access denied\"}]}"
        ));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.fetchMonitoringDocuments("form-123", 1609459200, 1609545600, 0, 100))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("KW GraphQL returned errors");
    }

    @Test
    void fetchFormDefinition_success_returnsFormNode() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"dynamicFormDefinition\":{\"id\":\"form-123\",\"name\":\"Test Form\",\"versions\":[{\"versionNumber\":1}]}}}"
        ));
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.fetchFormDefinition("form-123");

        assertThat(result.path("id").asText()).isEqualTo("form-123");
        assertThat(result.path("name").asText()).isEqualTo("Test Form");
        assertThat(result.path("versions").isArray()).isTrue();
    }

    @Test
    void fetchFormDefinition_graphqlErrors_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"errors\":[{\"message\":\"Form not found\"}]}"
        ));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.fetchFormDefinition("missing-form"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("KW GraphQL returned errors");
    }

    @Test
    void getValidAccessToken_nullUsername_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> client.getValidAccessToken(null, "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot be null or empty");
    }

    @Test
    void getValidAccessToken_nullPassword_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> client.getValidAccessToken("user", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null or empty");
    }

    @Test
    void getValidAccessToken_httpException_throwsRuntimeException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn(null);
        when(kwHttpClient.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class)))
                .thenThrow(new RuntimeException("Network error"));

        assertThatThrownBy(() -> client.getValidAccessToken())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch OAuth2 token");
    }

    @Test
    void getValidAccessToken_withScope_includesScopeInRequest() throws Exception {
        KwProperties propertiesWithScope = properties();
        propertiesWithScope.getAuth().setScope("read write");
        client = new KwGraphqlClient(propertiesWithScope, tokenCache, kwDocumentMapper, kwHttpClient, new ObjectMapper());

        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn(null);
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"token\",\"expires_in\":3600}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getValidAccessToken();

        assertThat(token).isEqualTo("token");
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(kwHttpClient).execute(requestCaptor.capture(), any(HttpClientResponseHandler.class));
        String requestBody = EntityUtils.toString(((HttpPost) requestCaptor.getValue()).getEntity());
        assertThat(requestBody).contains("scope=read+write");
    }

    @Test
    void getValidAccessToken_emptyScope_omitsScopeFromRequest() throws Exception {
        KwProperties propertiesNoScope = properties();
        propertiesNoScope.getAuth().setScope("");
        client = new KwGraphqlClient(propertiesNoScope, tokenCache, kwDocumentMapper, kwHttpClient, new ObjectMapper());

        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn(null);
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"token\",\"expires_in\":3600}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getValidAccessToken();

        assertThat(token).isEqualTo("token");
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(kwHttpClient).execute(requestCaptor.capture(), any(HttpClientResponseHandler.class));
        String requestBody = EntityUtils.toString(((HttpPost) requestCaptor.getValue()).getEntity());
        assertThat(requestBody).doesNotContain("scope=");
    }

    @Test
    void normalizeSearchWithData_nullNode_returnsMissingNode() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"data\":{}}"));
        mockHttpExecution(classicHttpResponse);

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT").itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).isEmpty();
    }

    @Test
    void queryDocumentsWithLocations_resultsContainer_normalizesAndMaps() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(
                "{\"data\":{\"searchWithData\":{\"results\":[{\"id\":\"doc-4\"}]}}}"
        ));
        mockHttpExecution(classicHttpResponse);
        when(kwDocumentMapper.convertToDocumentDtos(any()))
                .thenReturn(List.of(new KwDocumentDto("doc-4", "Doc4", "DOCUMENT", 1L, 2L)));

        ArcGISExecutionCommand cmd = ArcGISExecutionCommand.builder()
                .itemType("DOCUMENT").itemSubtype("REPORT")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        List<KwDocumentDto> documents = client.queryDocumentsWithLocations(cmd);

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getId()).isEqualTo("doc-4");
    }

    @Test
    void executeGraphQLQuery_runtimeExceptionDuringHandling_wrapsInIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken(KW_GRAPHQL_TOKEN_CACHE_KEY)).thenReturn("cached-token");
        when(kwHttpClient.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                    throw new RuntimeException("Handler failed");
                });

        assertThatThrownBy(() -> client.executeGraphQLQuery(Map.of("query", "{ test }")))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Failed to execute GraphQL query");
    }

    private void mockHttpExecution(ClassicHttpResponse response) throws Exception {
        when(kwHttpClient.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                    return handler.handleResponse(response);
                });
    }

    private KwProperties properties() {
        KwProperties properties = new KwProperties();
        KwProperties.Auth auth = new KwProperties.Auth();
        auth.setTokenUrl("https://kw.example.com/oauth/token");
        auth.setClientId("client-id");
        auth.setUsername("user");
        auth.setPassword("pass");
        auth.setGrantType("password");
        auth.setTokenRefreshInterval(Duration.ofMinutes(5));

        KwProperties.Auth.Headers headers = new KwProperties.Auth.Headers();
        headers.setUserAgent("JUnit");
        headers.setOrigin("https://kw.example.com");
        headers.setReferer("https://kw.example.com/");
        headers.setAcceptEncoding("gzip");
        auth.setHeaders(headers);

        KwProperties.GraphQL graphql = new KwProperties.GraphQL();
        graphql.setEndpoint("https://kw.example.com/graphql");

        properties.setAuth(auth);
        properties.setGraphql(graphql);
        return properties;
    }
}
