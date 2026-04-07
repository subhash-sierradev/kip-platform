package com.integration.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.client.JiraApiClient;
import com.integration.execution.config.properties.JiraApiProperties;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.exception.IntegrationApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static jakarta.servlet.http.HttpServletResponse.SC_GATEWAY_TIMEOUT;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationConnectionTestService")
class IntegrationConnectionTestServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ArcGISApiClient arcGisApiClient;

    @Mock
    private JiraApiClient jiraApiClient;

    @Mock
    private ConfluenceApiClient confluenceApiClient;

    private IntegrationConnectionTestService service;

    private IntegrationSecret integrationSecret;

    @BeforeEach
    void setUp() {
        JiraApiProperties jiraApiProperties = new JiraApiProperties();
        service = new IntegrationConnectionTestService(
                arcGisApiClient, jiraApiClient, jiraApiProperties, confluenceApiClient);
        integrationSecret = IntegrationSecret.builder()
                .baseUrl("https://jira.example.com")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder()
                        .username("user")
                        .password("password")
                        .build())
                .build();
    }

    @Test
    void testConnection_arcGisWithSecret_validToken_returnsSuccess() {
        when(arcGisApiClient.getAccessToken("", integrationSecret)).thenReturn("token-123");

        ApiResponse response = service.testConnection(ServiceType.ARCGIS, integrationSecret);

        assertThat(response.success()).isTrue();
        assertThat(response.statusCode()).isEqualTo(SC_OK);
        assertThat(response.message()).isEqualTo("ArcGIS connection successful");
    }

    @Test
    void testConnection_arcGisWithSecret_blankToken_returnsFailure() {
        when(arcGisApiClient.getAccessToken("", integrationSecret)).thenReturn(" ");

        ApiResponse response = service.testConnection(ServiceType.ARCGIS, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("token not retrieved");
    }

    @Test
    void testConnection_arcGisWithSecretName_apiException_returnsApiStatus() {
        when(arcGisApiClient.getAccessToken("secret-1"))
                .thenThrow(new IntegrationApiException("Unauthorized", SC_UNAUTHORIZED));

        ApiResponse response = service.testConnection(ServiceType.ARCGIS, "secret-1");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_UNAUTHORIZED);
        assertThat(response.message()).contains("ArcGIS connection test failed");
    }

    @Test
    void testConnection_jiraWithSecret_nullResponse_returnsFailure() {
        when(jiraApiClient.get("/rest/api/3/project/search", integrationSecret)).thenReturn(null);

        ApiResponse response = service.testConnection(ServiceType.JIRA, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("no response from Jira server");
    }

    @Test
    void testConnection_jiraWithSecret_serviceUnavailableFallback_returns503() throws Exception {
        JsonNode unavailable = OBJECT_MAPPER.readTree("{\"error\":\"Service temporarily unavailable\"}");
        when(jiraApiClient.get("/rest/api/3/project/search", integrationSecret)).thenReturn(unavailable);

        ApiResponse response = service.testConnection(ServiceType.JIRA, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_SERVICE_UNAVAILABLE);
        assertThat(response.message()).contains("temporarily unavailable");
    }

    @Test
    void testConnection_jiraWithSecret_emptyValues_returnsFailure() throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("{\"values\":[]}");
        when(jiraApiClient.get("/rest/api/3/project/search", integrationSecret)).thenReturn(payload);

        ApiResponse response = service.testConnection(ServiceType.JIRA, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("no projects found or accessible");
    }

    @Test
    void testConnection_jiraWithSecret_valuesPresent_returnsSuccess() throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("{\"values\":[{\"id\":1},{\"id\":2}]}");
        when(jiraApiClient.get("/rest/api/3/project/search", integrationSecret)).thenReturn(payload);

        ApiResponse response = service.testConnection(ServiceType.JIRA, integrationSecret);

        assertThat(response.success()).isTrue();
        assertThat(response.statusCode()).isEqualTo(SC_OK);
        assertThat(response.message()).contains("2 project(s)");
    }

    @Test
    void testConnection_jiraWithSecretName_unauthorizedMessage_returns401AndFriendlyMessage() {
        when(jiraApiClient.searchProjects("secret-2"))
                .thenThrow(new RuntimeException("401 Unauthorized"));

        ApiResponse response = service.testConnection(ServiceType.JIRA, "secret-2");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_UNAUTHORIZED);
        assertThat(response.message()).isEqualTo("Invalid credentials - authentication failed");
    }

    @Test
    void testConnection_jiraWithSecretName_notFoundMessage_returns404AndFriendlyMessage() {
        when(jiraApiClient.searchProjects("secret-3"))
                .thenThrow(new RuntimeException("404 Not Found"));

        ApiResponse response = service.testConnection(ServiceType.JIRA, "secret-3");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_NOT_FOUND);
        assertThat(response.message()).isEqualTo("Jira server not found - verify the base URL");
    }

    @Test
    void testConnection_jiraWithSecretName_timeoutMessage_returns504AndFriendlyMessage() {
        when(jiraApiClient.searchProjects("secret-4"))
                .thenThrow(new RuntimeException("request timed out"));

        ApiResponse response = service.testConnection(ServiceType.JIRA, "secret-4");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_GATEWAY_TIMEOUT);
        assertThat(response.message()).isEqualTo("Connection timeout - unable to reach Jira server");
    }

    @Test
    void testConnection_jiraWithSecretName_forbiddenMessage_returns403AndFriendlyMessage() {
        when(jiraApiClient.searchProjects("secret-5"))
                .thenThrow(new RuntimeException("403 Forbidden"));

        ApiResponse response = service.testConnection(ServiceType.JIRA, "secret-5");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_FORBIDDEN);
        assertThat(response.message()).isEqualTo("Invalid credentials - access denied");
    }

    @Test
    void testConnection_jiraWithSecretName_unknownException_returns500AndGenericMessage() {
        when(jiraApiClient.searchProjects("secret-6"))
                .thenThrow(new RuntimeException("boom"));

        ApiResponse response = service.testConnection(ServiceType.JIRA, "secret-6");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).isEqualTo("Jira connection test failed: boom");
    }

    @Test
    void testConnection_jiraWithSecret_integrationApiException_returnsStatusAndMessage() {
        when(jiraApiClient.get("/rest/api/3/project/search", integrationSecret))
                .thenThrow(new IntegrationApiException("Bad credentials", SC_UNAUTHORIZED));

        ApiResponse response = service.testConnection(ServiceType.JIRA, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_UNAUTHORIZED);
        assertThat(response.message()).isEqualTo("Jira connection test failed: Bad credentials");
    }

    @Test
    void testConnection_arcGisWithSecretName_runtimeException_returns500() {
        when(arcGisApiClient.getAccessToken("secret-7"))
                .thenThrow(new RuntimeException("downstream unavailable"));

        ApiResponse response = service.testConnection(ServiceType.ARCGIS, "secret-7");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("ArcGIS connection test failed");
    }

    // --- Confluence: IntegrationSecret overload ---

    @Test
    void testConnection_confluenceWithSecret_success_returnsVerifiedMessage() throws Exception {
        IntegrationSecret secret = confluenceSecret();
        JsonNode user = OBJECT_MAPPER.readTree(
                "{\"accountId\":\"acc123\",\"displayName\":\"Alice\"}");
        when(confluenceApiClient.getCurrentUser(secret)).thenReturn(user);

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isTrue();
        assertThat(response.statusCode()).isEqualTo(SC_OK);
        assertThat(response.message()).contains("verified as Alice");
    }

    @Test
    void testConnection_confluenceWithSecret_nullResponse_returnsFailure() {
        IntegrationSecret secret = confluenceSecret();
        when(confluenceApiClient.getCurrentUser(secret)).thenReturn(null);

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("no response from Confluence server");
    }

    @Test
    void testConnection_confluenceWithSecret_serviceUnavailableFallback_returns503() throws Exception {
        IntegrationSecret secret = confluenceSecret();
        JsonNode unavailable = OBJECT_MAPPER.readTree(
                "{\"error\":\"Service temporarily unavailable\"}");
        when(confluenceApiClient.getCurrentUser(secret)).thenReturn(unavailable);

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_SERVICE_UNAVAILABLE);
        assertThat(response.message()).contains("temporarily unavailable");
    }

    @Test
    void testConnection_confluenceWithSecret_missingAccountId_returnsFailure() throws Exception {
        IntegrationSecret secret = confluenceSecret();
        JsonNode noAccount = OBJECT_MAPPER.readTree("{\"displayName\":\"Alice\"}");
        when(confluenceApiClient.getCurrentUser(secret)).thenReturn(noAccount);

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("no user information returned");
    }

    @Test
    void testConnection_confluenceWithSecret_unauthorizedException_returns401AndFriendlyMessage() {
        IntegrationSecret secret = confluenceSecret();
        when(confluenceApiClient.getCurrentUser(secret))
                .thenThrow(new RuntimeException("401 Unauthorized"));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_UNAUTHORIZED);
        assertThat(response.message()).isEqualTo("Invalid credentials - authentication failed");
    }

    @Test
    void testConnection_confluenceWithSecret_unknownException_returns500AndGenericMessage() {
        IntegrationSecret secret = confluenceSecret();
        when(confluenceApiClient.getCurrentUser(secret))
                .thenThrow(new RuntimeException("boom"));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).isEqualTo("Confluence connection test failed: boom");
    }

    // --- Confluence: secretName overload ---

    @Test
    void testConnection_confluenceWithSecretName_success_returnsVerifiedMessage() throws Exception {
        JsonNode user = OBJECT_MAPPER.readTree(
                "{\"accountId\":\"acc456\",\"displayName\":\"Bob\"}");
        when(confluenceApiClient.getCurrentUser("conf-secret-1")).thenReturn(user);

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, "conf-secret-1");

        assertThat(response.success()).isTrue();
        assertThat(response.statusCode()).isEqualTo(SC_OK);
        assertThat(response.message()).contains("verified as Bob");
    }

    @Test
    void testConnection_confluenceWithSecretName_forbiddenMessage_returns403AndFriendlyMessage() {
        when(confluenceApiClient.getCurrentUser("conf-secret-2"))
                .thenThrow(new RuntimeException("403 Forbidden"));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, "conf-secret-2");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_FORBIDDEN);
        assertThat(response.message()).isEqualTo("Invalid credentials - access denied");
    }

    @Test
    void testConnection_confluenceWithSecretName_notFoundMessage_returns404AndFriendlyMessage() {
        when(confluenceApiClient.getCurrentUser("conf-secret-3"))
                .thenThrow(new RuntimeException("404 Not Found"));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, "conf-secret-3");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_NOT_FOUND);
        assertThat(response.message()).isEqualTo("Confluence server not found - verify the base URL");
    }

    @Test
    void testConnection_confluenceWithSecretName_timeoutMessage_returns504AndFriendlyMessage() {
        when(confluenceApiClient.getCurrentUser("conf-secret-4"))
                .thenThrow(new RuntimeException("request timed out"));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, "conf-secret-4");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_GATEWAY_TIMEOUT);
        assertThat(response.message()).isEqualTo(
                "Connection timeout - unable to reach Confluence server");
    }

    private IntegrationSecret confluenceSecret() {
        return IntegrationSecret.builder()
                .baseUrl("https://example.atlassian.net")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder()
                        .username("user@example.com")
                        .password("api-token-abc")
                        .build())
                .build();
    }

    // --- Additional branch-coverage tests ---

    @Test
    void testConnection_arcGisWithSecret_nullToken_returnsFailure() {
        when(arcGisApiClient.getAccessToken("", integrationSecret)).thenReturn(null);

        ApiResponse response = service.testConnection(ServiceType.ARCGIS, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("token not retrieved");
    }

    @Test
    void testConnection_arcGisWithSecret_integrationApiException_returnsApiStatus() {
        when(arcGisApiClient.getAccessToken("", integrationSecret))
                .thenThrow(new IntegrationApiException("ArcGIS auth failed", SC_UNAUTHORIZED));

        ApiResponse response = service.testConnection(ServiceType.ARCGIS, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_UNAUTHORIZED);
        assertThat(response.message()).contains("ArcGIS connection test failed");
    }

    @Test
    void testConnection_confluenceWithSecret_integrationApiException_returnsApiStatus() {
        IntegrationSecret secret = confluenceSecret();
        when(confluenceApiClient.getCurrentUser(secret))
                .thenThrow(new IntegrationApiException("Bad auth", SC_UNAUTHORIZED));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_UNAUTHORIZED);
    }

    @Test
    void testConnection_confluenceWithSecret_exceptionWithNullMessage_returns500WithGenericFallback() {
        IntegrationSecret secret = confluenceSecret();
        when(confluenceApiClient.getCurrentUser(secret))
                .thenThrow(new RuntimeException((String) null));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("Confluence connection test failed");
    }

    @Test
    void testConnection_jiraWithSecret_valuesFieldNotArray_returnsFailure() throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("{\"values\":\"not-an-array\"}");
        when(jiraApiClient.get("/rest/api/3/project/search", integrationSecret)).thenReturn(payload);

        ApiResponse response = service.testConnection(ServiceType.JIRA, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("no projects found or accessible");
    }

    @Test
    void testConnection_jiraWithSecret_valuesFieldMissing_returnsFailure() throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("{\"other\":\"field\"}");
        when(jiraApiClient.get("/rest/api/3/project/search", integrationSecret)).thenReturn(payload);

        ApiResponse response = service.testConnection(ServiceType.JIRA, integrationSecret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("no projects found or accessible");
    }

    @Test
    void testConnection_confluenceWithSecretName_integrationApiException_returnsApiStatus() {
        when(confluenceApiClient.getCurrentUser("conf-secret-ex"))
                .thenThrow(new IntegrationApiException("bad token", SC_UNAUTHORIZED));

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, "conf-secret-ex");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_UNAUTHORIZED);
    }

    @Test
    void testConnection_jiraWithSecretName_exceptionWithNullMessage_returns500WithGenericFallback() {
        when(jiraApiClient.searchProjects("secret-nullmsg"))
                .thenThrow(new RuntimeException((String) null));

        ApiResponse response = service.testConnection(ServiceType.JIRA, "secret-nullmsg");

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("Unknown error");
    }

    @Test
    void testConnection_confluenceWithSecret_blankAccountId_returnsFailure() throws Exception {
        IntegrationSecret secret = confluenceSecret();
        JsonNode user = OBJECT_MAPPER.readTree("{\"accountId\":\"\",\"displayName\":\"Empty\"}");
        when(confluenceApiClient.getCurrentUser(secret)).thenReturn(user);

        ApiResponse response = service.testConnection(ServiceType.CONFLUENCE, secret);

        assertThat(response.success()).isFalse();
        assertThat(response.statusCode()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
        assertThat(response.message()).contains("no user information returned");
    }
}