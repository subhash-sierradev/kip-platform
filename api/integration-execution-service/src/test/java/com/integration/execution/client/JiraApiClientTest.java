package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.auth.ConnectionAuthHeaderBuilder;
import com.integration.execution.config.properties.JiraApiProperties;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.service.VaultService;
import com.integration.execution.util.JiraRateLimiter;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraApiClientTest {

    @Mock
    private VaultService vaultService;

    @Mock
    private CloseableHttpClient jiraHttpClient;

    @Mock
    private JiraRateLimiter rateLimiter;

    @Mock
    private ConnectionAuthHeaderBuilder authHeaderBuilder;

    @Mock
    private ClassicHttpResponse classicHttpResponse;

    private JiraApiClient client;

    @BeforeEach
    void setUp() {
        JiraApiProperties properties = new JiraApiProperties();
        client = new JiraApiClient(
                vaultService,
                jiraHttpClient,
                new ObjectMapper(),
                rateLimiter,
                properties,
                authHeaderBuilder
        );
    }

    @Test
    void searchProjects_success_returnsJson() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"values\":[{\"key\":\"PRJ\"}]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        JsonNode response = client.searchProjects("secret");

        assertThat(response.path("values")).hasSize(1);
    }

    @Test
    void get_non200_throwsIntegrationApiException() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(500);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("boom"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.get("/rest/api/3/project/search", "secret"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Jira API call failed. Status: 500");
    }

    @Test
    void post_success_returnsApiResponse() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(201);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"id\":\"10001\"}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        ApiResponse response = client.post("/rest/api/3/issue", Map.of("k", "v"), "secret");

        assertThat(response.success()).isTrue();
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void post_nullSecret_throwsNullPointerException() {
        when(vaultService.getSecret("secret")).thenReturn(null);

        assertThatThrownBy(() -> client.post("/rest/api/3/issue", Map.of("k", "v"), "secret"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("getBaseUrl");
    }

    @Test
    void sendToJira_clientThrows_returnsFailureApiResponse() {
        when(vaultService.getSecret("secret")).thenThrow(new RuntimeException("vault down"));

        ApiResponse response = client.sendToJira("secret", "{}");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.message()).contains("Failed to send webhook data to Jira");
    }

    @Test
    void searchTeams_whenGetFails_wrapsAsIntegrationApiException400() {
        when(vaultService.getSecret("secret")).thenThrow(new RuntimeException("vault down"));

        assertThatThrownBy(() -> client.searchTeams("secret", "query", 0, 20))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Team search API call failed")
                .extracting("statusCode")
                .isEqualTo(400);
    }

    @Test
    void get_withProvidedSecret_success_returnsJson() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"id\":\"1\"}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        JsonNode response = client.get("/rest/api/3/field", secret);

        assertThat(response.path("id").asText()).isEqualTo("1");
    }

    @Test
    void getString_success_returnsBody() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("raw-response"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        String response = client.getString("/rest/api/3/serverInfo", "secret");

        assertThat(response).isEqualTo("raw-response");
    }

    @Test
    void put_and_delete_returnApiResponses() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(204);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(""));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        ApiResponse put = client.put("/rest/api/3/issue/1", Map.of("k", "v"), "secret");
        ApiResponse delete = client.delete("https://jira.example.com", "/rest/api/3/issue/1", "secret");

        assertThat(put.success()).isTrue();
        assertThat(put.statusCode()).isEqualTo(204);
        assertThat(delete.success()).isTrue();
        assertThat(delete.statusCode()).isEqualTo(204);
    }

    @Test
    void convenienceMethods_delegateToGetAndReturnJson() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"values\":[{\"id\":1}]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        assertThat(client.getAssignableUsers("secret", "PRJ").path("values")).hasSize(1);
        assertThat(client.getProjectStatuses("PRJ", "secret").path("values")).hasSize(1);
        assertThat(client.getFields("secret").path("values")).hasSize(1);
        assertThat(client.getBoardsByProject("secret", "PRJ").path("values")).hasSize(1);
        assertThat(client.getSprintsByBoard("secret", 12L, 0, 50, "active").path("values")).hasSize(1);
    }

    @Test
    void createIssue_usesPostPath() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(201);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"ok\":true}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        ApiResponse response = client.createIssue(Map.of("summary", "Issue"), "secret");

        assertThat(response.success()).isTrue();
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void searchTeams_withNullQuery_buildsEndpointAndSucceeds() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"values\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        JsonNode teams = client.searchTeams("secret", null, null, 20);

        assertThat(teams.path("values").isArray()).isTrue();
    }

    @Test
    void searchParentIssues_withQuery_buildsQueryAwareEndpoint() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"issues\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        client.searchParentIssues("secret", "PRJ", "PRJ-10", 0, 20);

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(jiraHttpClient).execute(requestCaptor.capture(), any(HttpClientResponseHandler.class));
        String uri = requestCaptor.getValue().getUri().toString();
        assertThat(uri).contains("jql=");
        assertThat(uri).contains("PRJ-10");
        assertThat(uri).contains("startAt=0");
        assertThat(uri).contains("maxResults=20");
    }

    @Test
    void rateLimiterNonDomainException_wrapsAsIntegrationApiException500() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(rateLimiter.executeWithRateLimit(anyString(), any()))
                .thenThrow(new RuntimeException("rate limiter failed"));

        assertThatThrownBy(() -> client.get("/rest/api/3/field", "secret"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Rate limited API call failed");
    }

    @Test
    void fallbackMethods_returnServiceUnavailablePayload() {
        ApiResponse postFallback = client.fallbackPostApiResponse("/a", Map.of(), "secret", new RuntimeException());
        ApiResponse putFallback = client.fallbackPutApiResponse("/a", Map.of(), "secret", new RuntimeException());
        ApiResponse deleteFallback = client.fallbackDeleteApiResponse("https://base", "/a", "secret",
                new RuntimeException());

        assertThat(client.fallbackStringResponse("/a", "secret", new RuntimeException()))
                .isEqualTo("Service temporarily unavailable");
        assertThat(client.fallbackJsonResponse("/a", "secret", new RuntimeException()).path("error").asText())
                .isEqualTo("Service temporarily unavailable");
        assertThat(postFallback.statusCode()).isEqualTo(503);
        assertThat(putFallback.statusCode()).isEqualTo(503);
        assertThat(deleteFallback.statusCode()).isEqualTo(503);
    }

    @Test
    void getSprintsByBoard_blankStateAndNullPaging_buildsEndpointWithoutQueryParams() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"values\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        client.getSprintsByBoard("secret", 42L, null, null, "   ");

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(jiraHttpClient).execute(requestCaptor.capture(), any(HttpClientResponseHandler.class));
        String uri = requestCaptor.getValue().getUri().toString();
        assertThat(uri).contains("/board/42/sprint");
        assertThat(uri).doesNotContain("startAt=");
        assertThat(uri).doesNotContain("maxResults=");
        assertThat(uri).doesNotContain("state=");
    }

    @Test
    void privateBuildRequestHeaders_nullSecret_throwsIntegrationApiException() throws Exception {
        Method method = JiraApiClient.class.getDeclaredMethod("buildRequestHeaders", IntegrationSecret.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(client, new Object[]{null}))
                .hasCauseInstanceOf(IntegrationApiException.class);
    }

    @Test
    void privateBuildRequestHeaders_nullAuthHeaders_keepsDefaultHeaders() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(null);
        Method method = JiraApiClient.class.getDeclaredMethod("buildRequestHeaders", IntegrationSecret.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) method.invoke(client, secret);

        assertThat(headers).containsEntry("Accept", "application/json");
        assertThat(headers).containsEntry("Content-Type", "application/json");
        assertThat(headers).doesNotContainKey("Authorization");
    }

    @Test
    void privateParseApiResponse_whenResponseCodeThrows_returnsServiceUnavailable() throws Exception {
        when(classicHttpResponse.getCode()).thenThrow(new RuntimeException("boom"));
        Method method = JiraApiClient.class.getDeclaredMethod(
                "parseApiResponse",
                ClassicHttpResponse.class,
                String.class
        );
        method.setAccessible(true);

        ApiResponse response = (ApiResponse) method.invoke(client, classicHttpResponse, "https://jira.example.com");

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Failed to process response");
    }

    @Test
    void getString_withSecret_returnsBody() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("string-body"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        String response = client.getString("/rest/api/3/serverInfo", secret);

        assertThat(response).isEqualTo("string-body");
    }

    @Test
    void getString_non200_throwsIntegrationApiException() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(404);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("not found"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.getString("/rest/api/3/serverInfo", "secret"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Jira API call failed. Status: 404");
    }

    @Test
    void searchParentIssues_nullQuery_buildsEndpointWithoutQueryClause() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"issues\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        client.searchParentIssues("secret", "PRJ", null, null, null);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(jiraHttpClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        String uri = captor.getValue().getUri().toString();
        assertThat(uri).contains("project");
        // No AND ( ... ) clause
        assertThat(uri).doesNotContain("AND%20(");
    }

    @Test
    void searchParentIssues_nonIssueKeyQuery_usesOnlySummaryClause() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"issues\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        client.searchParentIssues("secret", "PRJ", "search text with \"quotes\"", 0, 10);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(jiraHttpClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        String uri = captor.getValue().getUri().toString();
        assertThat(uri).contains("jql=");
        // Non-issue-key query should not include issueKey clause
        assertThat(uri).doesNotContain("issueKey");
    }

    @Test
    void searchTeams_withOnlyMaxResults_buildsEndpointCorrectly() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"values\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.searchTeams("secret", null, null, 50);

        assertThat(result.path("values").isArray()).isTrue();
    }

    @Test
    void searchTeams_withQueryAndStartAt_buildsEndpointCorrectly() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"values\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.searchTeams("secret", "dev", 10, null);

        assertThat(result.path("values").isArray()).isTrue();
    }

    @Test
    void getProjectMetaFields_delegatesToGet() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"fields\":[]}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        JsonNode result = client.getProjectMetaFields("PROJ", "secret");

        assertThat(result.path("fields").isArray()).isTrue();
    }

    @Test
    void rateLimiterThrowsIntegrationApiException_rethrowsDirectly() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(rateLimiter.executeWithRateLimit(anyString(), any()))
                .thenThrow(new IntegrationApiException("upstream error", 503));

        assertThatThrownBy(() -> client.get("/rest/api/3/field", "secret"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("upstream error");
    }

    @Test
    void post_withStringBody_doesNotSerialize() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"id\":\"1\"}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        // String body should be sent as-is without serialization
        ApiResponse response = client.post("/rest/api/3/issue", "{\"summary\":\"test\"}", "secret");

        assertThat(response.success()).isTrue();
    }

    @Test
    void put_withStringBody_doesNotSerialize() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        ApiResponse response = client.put("/rest/api/3/issue/1", "{\"summary\":\"test\"}", "secret");

        assertThat(response.success()).isTrue();
    }

    @Test
    void sendToJira_success_returnsSuccessApiResponse() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(201);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"id\":\"1\"}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        ApiResponse response = client.sendToJira("secret", "{}");

        assertThat(response.success()).isTrue();
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void get_post_withEndpointWithoutLeadingSlash_prependsSlash() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        // endpoint without leading slash
        JsonNode result = client.get("rest/api/3/field", "secret");

        assertThat(result).isNotNull();
    }

    @Test
    void fallbackStringResponse_withIntegrationSecret_returnsServiceUnavailable() {
        IntegrationSecret secret = basicSecret();
        String result = client.fallbackStringResponse("/endpoint", secret, new RuntimeException("err"));

        assertThat(result).isEqualTo("Service temporarily unavailable");
    }

    @Test
    void post_non200Status_returnsFailureApiResponse() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(400);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("bad request"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        ApiResponse response = client.post("/rest/api/3/issue", Map.of(), "secret");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void get_post_nullBody_doesNotAddEntity() throws Exception {
        IntegrationSecret secret = basicSecret();
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(authHeaderBuilder.buildAuthHeaders(secret)).thenReturn(Map.of("Authorization", "Basic abc"));
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"ok\":true}"));
        mockRateLimitPassThrough();
        mockHttpExecution(classicHttpResponse);

        ApiResponse response = client.post("/rest/api/3/issue", null, "secret");

        assertThat(response.success()).isTrue();
    }

    private void mockRateLimitPassThrough() throws Exception {
        when(rateLimiter.executeWithRateLimit(anyString(), any()))
                .thenAnswer(invocation -> {
                    JiraRateLimiter.JiraApiCall<?> apiCall = invocation.getArgument(1);
                    return apiCall.execute();
                });
    }

    private void mockHttpExecution(ClassicHttpResponse response) throws Exception {
        when(jiraHttpClient.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                    return handler.handleResponse(response);
                });
    }

    private IntegrationSecret basicSecret() {
        return IntegrationSecret.builder()
                .baseUrl("https://jira.example.com")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u").password("p").build())
                .build();
    }
}
