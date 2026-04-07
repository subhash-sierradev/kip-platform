package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import com.integration.execution.service.VaultService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfluenceApiClientTest {

    @Mock
    private VaultService vaultService;

    @Mock
    private CloseableHttpClient confluenceHttpClient;

    @Mock
    private ClassicHttpResponse classicHttpResponse;

    private ConfluenceApiClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = new ConfluenceApiClient(vaultService, confluenceHttpClient, objectMapper);
    }

    // -----------------------------------------------------------------------
    // searchPage
    // -----------------------------------------------------------------------

    @Test
    void searchPage_pageFound_returnsOptionalWithPage() throws Exception {
        String body = """
                {"results":[{"id":"42","version":{"number":3}}]}
                """;
        mockHttp(200, body);

        Optional<ConfluenceApiClient.ConfluencePage> result = client.searchPage(
                "https://site.atlassian.net", "SPACE", "My Page", "user@e.com", "token");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("42");
        assertThat(result.get().version()).isEqualTo(3);
    }

    @Test
    void searchPage_noResults_returnsEmpty() throws Exception {
        String body = """
                {"results":[]}
                """;
        mockHttp(200, body);

        Optional<ConfluenceApiClient.ConfluencePage> result = client.searchPage(
                "https://site.atlassian.net", "SPACE", "Unknown Page", "u@e.com", "t");

        assertThat(result).isEmpty();
    }

    @Test
    void searchPage_httpError_throwsRuntimeException() throws Exception {
        mockHttp(401, "Unauthorized");

        assertThatThrownBy(() -> client.searchPage(
                "https://site.atlassian.net", "SPACE", "Page", "u@e.com", "bad-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Confluence search failed with HTTP 401");
    }

    // -----------------------------------------------------------------------
    // createPage
    // -----------------------------------------------------------------------

    @Test
    void createPage_success_returnsPageId() throws Exception {
        String body = """
                {"id":"new-page-99"}
                """;
        mockHttp(200, body);

        String pageId = client.createPage("https://site.atlassian.net", "SPACE",
                "parent-1", "New Page", "<p>content</p>", "u@e.com", "token");

        assertThat(pageId).isEqualTo("new-page-99");
    }

    @Test
    void createPage_httpError_throwsRuntimeException() throws Exception {
        mockHttp(400, "Bad Request");

        assertThatThrownBy(() -> client.createPage("https://site.atlassian.net", "SPACE",
                null, "Page", "<p/>", "u@e.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Confluence create failed with HTTP 400");
    }

    // -----------------------------------------------------------------------
    // updatePage
    // -----------------------------------------------------------------------

    @Test
    void updatePage_success_completesWithoutException() throws Exception {
        // updatePage success path only checks status, never reads entity — stub only what's used
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(confluenceHttpClient.execute(any(), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                    return handler.handleResponse(classicHttpResponse);
                });

        client.updatePage("https://site.atlassian.net", "page-1", "parent-1",
                "Updated Title", "<p>updated</p>", 2, "u@e.com", "token");
        // no exception = pass
    }

    @Test
    void updatePage_httpError_throwsRuntimeException() throws Exception {
        mockHttp(403, "Forbidden");

        assertThatThrownBy(() -> client.updatePage("https://site.atlassian.net", "page-1",
                "parent-1", "Title", "<p/>", 2, "u@e.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Confluence update failed with HTTP 403");
    }

    // -----------------------------------------------------------------------
    // createOrUpdatePage — update existing
    // -----------------------------------------------------------------------

    @Test
    void createOrUpdatePage_pageExists_updatesAndReturnsUrl() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://mysite.atlassian.net");
        when(vaultService.getSecret("my-secret")).thenReturn(secret);

        // First HTTP call = searchPage → returns a page
        // Second HTTP call = updatePage → 200
        when(classicHttpResponse.getCode())
                .thenReturn(200)
                .thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("""
                        {"results":[{"id":"100","version":{"number":1}}]}
                        """))
                .thenReturn(new StringEntity("{}"));
        mockHttpMulti();

        ConfluenceApiClient.ConfluencePublishResult result = client.createOrUpdatePage(
                new ConfluenceApiClient.ConfluencePublishRequest(
                        "my-secret", "SP", "parent-id", "Title", "<p>body</p>"));

        assertThat(result.confluencePageId()).isEqualTo("100");
        assertThat(result.confluencePageUrl()).contains("mysite.atlassian.net");
    }

    @Test
    void createOrUpdatePage_pageDoesNotExist_createsAndReturnsUrl() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://mysite.atlassian.net");
        when(vaultService.getSecret("new-secret")).thenReturn(secret);

        when(classicHttpResponse.getCode())
                .thenReturn(200)
                .thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("""
                        {"results":[]}
                        """))
                .thenReturn(new StringEntity("""
                        {"id":"new-55"}
                        """));
        mockHttpMulti();

        ConfluenceApiClient.ConfluencePublishResult result = client.createOrUpdatePage(
                new ConfluenceApiClient.ConfluencePublishRequest(
                        "new-secret", "SP", null, "New Page", "<p>body</p>"));

        assertThat(result.confluencePageId()).isEqualTo("new-55");
    }

    // -----------------------------------------------------------------------
    // getCurrentUser
    // -----------------------------------------------------------------------

    @Test
    void getCurrentUser_success_returnsUserNode() throws Exception {
        String body = """
                {"accountId":"abc123","displayName":"Alice"}
                """;
        mockHttp(200, body);

        JsonNode user = client.getCurrentUser("https://site.atlassian.net", "u@e.com", "token");

        assertThat(user.path("accountId").asText()).isEqualTo("abc123");
        assertThat(user.path("displayName").asText()).isEqualTo("Alice");
    }

    @Test
    void getCurrentUser_httpError_throwsRuntimeException() throws Exception {
        mockHttp(401, "Unauthorized");

        assertThatThrownBy(() -> client.getCurrentUser("https://site.atlassian.net", "u@e.com", "bad"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Confluence connection test failed with HTTP 401");
    }

    @Test
    void getCurrentUser_byIntegrationSecret_delegatesToCredentials() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://site.atlassian.net");
        mockHttp(200, """
                {"accountId":"xyz"}
                """);

        JsonNode user = client.getCurrentUser(secret);

        assertThat(user.path("accountId").asText()).isEqualTo("xyz");
    }

    @Test
    void getCurrentUser_bySecretName_loadsVaultAndDelegates() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://site.atlassian.net");
        when(vaultService.getSecret("vault-key")).thenReturn(secret);
        mockHttp(200, """
                {"accountId":"from-vault"}
                """);

        JsonNode user = client.getCurrentUser("vault-key");

        assertThat(user.path("accountId").asText()).isEqualTo("from-vault");
    }

    // -----------------------------------------------------------------------
    // getUserTimezone
    // -----------------------------------------------------------------------

    @Test
    void getUserTimezone_accountIdFound_andTimezonePresent_returnsZoneId() throws Exception {
        when(classicHttpResponse.getCode()).thenReturn(200).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("""
                        {"accountId":"user-1"}
                        """))
                .thenReturn(new StringEntity("""
                        {"results":[{"timeZone":"America/Chicago"}]}
                        """));
        mockHttpMulti();

        ZoneId tz = client.getUserTimezone(
                "https://site.atlassian.net", "u@e.com", "token", ZoneId.of("UTC"));

        assertThat(tz).isEqualTo(ZoneId.of("America/Chicago"));
    }

    @Test
    void getUserTimezone_noAccountId_returnsFallback() throws Exception {
        mockHttp(200, """
                {"accountId":""}
                """);

        ZoneId tz = client.getUserTimezone(
                "https://site.atlassian.net", "u@e.com", "token", ZoneId.of("Europe/Paris"));

        assertThat(tz).isEqualTo(ZoneId.of("Europe/Paris"));
    }

    @Test
    void getUserTimezone_nullFallback_usesUTC() throws Exception {
        mockHttp(200, """
                {"accountId":""}
                """);

        ZoneId tz = client.getUserTimezone(
                "https://site.atlassian.net", "u@e.com", "token", null);

        assertThat(tz).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void getUserTimezone_bulkApiNoTimezoneField_returnsFallback() throws Exception {
        when(classicHttpResponse.getCode()).thenReturn(200).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("""
                        {"accountId":"user-9"}
                        """))
                .thenReturn(new StringEntity("""
                        {"results":[{"displayName":"Bob"}]}
                        """));
        mockHttpMulti();

        ZoneId tz = client.getUserTimezone(
                "https://site.atlassian.net", "u@e.com", "token", ZoneId.of("Asia/Tokyo"));

        assertThat(tz).isEqualTo(ZoneId.of("Asia/Tokyo"));
    }

    @Test
    void getUserTimezone_exceptionDuringHttpCall_returnsFallback() throws Exception {
        when(confluenceHttpClient.execute(any(), any(HttpClientResponseHandler.class)))
                .thenThrow(new RuntimeException("network failure"));

        ZoneId tz = client.getUserTimezone(
                "https://site.atlassian.net", "u@e.com", "token", ZoneId.of("UTC"));

        assertThat(tz).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void getUserTimezone_byIntegrationSecret_delegatesToCredentials() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://site.atlassian.net");
        mockHttp(200, """
                {"accountId":""}
                """);

        ZoneId tz = client.getUserTimezone(secret, ZoneId.of("UTC"));

        assertThat(tz).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void getUserTimezone_bySecretName_loadsVaultAndDelegates() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://site.atlassian.net");
        when(vaultService.getSecret("tz-key")).thenReturn(secret);
        mockHttp(200, """
                {"accountId":""}
                """);

        ZoneId tz = client.getUserTimezone("tz-key", ZoneId.of("UTC"));

        assertThat(tz).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void getUserTimezone_bySecretNameNoFallback_usesUTC() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://site.atlassian.net");
        when(vaultService.getSecret("tz-key-no-fallback")).thenReturn(secret);
        mockHttp(200, """
                {"accountId":""}
                """);

        ZoneId tz = client.getUserTimezone("tz-key-no-fallback");

        assertThat(tz).isEqualTo(ZoneId.of("UTC"));
    }

    // -----------------------------------------------------------------------
    // getSpaces
    // -----------------------------------------------------------------------

    @Test
    void getSpaces_success_returnsSpaceList() throws Exception {
        String body = """
                {"results":[
                  {"key":"DEV","name":"Development","type":"global","description":{"plain":{"value":"Dev space"}}},
                  {"key":"OPS","name":"Operations","type":"global","description":{"plain":{"value":""}}}
                ]}
                """;
        mockHttp(200, body);

        List<ConfluenceSpaceDto> spaces = client.getSpaces(
                "https://site.atlassian.net", "u@e.com", "token");

        assertThat(spaces).hasSize(2);
        assertThat(spaces.get(0).getKey()).isEqualTo("DEV");
        assertThat(spaces.get(0).getDescription()).isEqualTo("Dev space");
        assertThat(spaces.get(1).getKey()).isEqualTo("OPS");
    }

    @Test
    void getSpaces_emptyResults_returnsEmptyList() throws Exception {
        mockHttp(200, """
                {"results":[]}
                """);

        List<ConfluenceSpaceDto> spaces = client.getSpaces(
                "https://site.atlassian.net", "u@e.com", "token");

        assertThat(spaces).isEmpty();
    }

    @Test
    void getSpaces_httpError_throwsRuntimeException() throws Exception {
        mockHttp(403, "Forbidden");

        assertThatThrownBy(() -> client.getSpaces(
                "https://site.atlassian.net", "u@e.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Confluence get spaces failed with HTTP 403");
    }

    @Test
    void getSpaces_bySecretName_loadsVaultAndFetches() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://site.atlassian.net");
        when(vaultService.getSecret("space-secret")).thenReturn(secret);
        mockHttp(200, """
                {"results":[]}
                """);

        List<ConfluenceSpaceDto> spaces = client.getSpaces("space-secret");

        assertThat(spaces).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getPages
    // -----------------------------------------------------------------------

    @Test
    void getPages_withAncestors_returnsPagesWithParentTitle() throws Exception {
        String body = """
                {"results":[
                  {"content":{"id":"99","title":"Reports","ancestors":[{"title":"Root"},{"title":"Parent"}]}},
                  {"content":{"id":"100","title":"NoParent","ancestors":[]}}
                ]}
                """;
        mockHttp(200, body);

        List<ConfluencePageDto> pages = client.getPages(
                "https://site.atlassian.net", "SPACE", "u@e.com", "token");

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).getId()).isEqualTo("99");
        assertThat(pages.get(0).getParentTitle()).isEqualTo("Parent");
        assertThat(pages.get(1).getParentTitle()).isNull();
    }

    @Test
    void getPages_missingContentNode_skipsEntry() throws Exception {
        String body = """
                {"results":[
                  {"noContent":true},
                  {"content":{"id":"77","title":"Valid","ancestors":[]}}
                ]}
                """;
        mockHttp(200, body);

        List<ConfluencePageDto> pages = client.getPages(
                "https://site.atlassian.net", "SPACE", "u@e.com", "token");

        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).getId()).isEqualTo("77");
    }

    @Test
    void getPages_httpError_throwsRuntimeException() throws Exception {
        mockHttp(500, "Internal Server Error");

        assertThatThrownBy(() -> client.getPages(
                "https://site.atlassian.net", "SPACE", "u@e.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Confluence get folders failed with HTTP 500");
    }

    @Test
    void getPages_bySecretName_loadsVaultAndFetches() throws Exception {
        IntegrationSecret secret = buildBasicSecret("https://site.atlassian.net");
        when(vaultService.getSecret("page-secret")).thenReturn(secret);
        mockHttp(200, """
                {"results":[]}
                """);

        List<ConfluencePageDto> pages = client.getPages("page-secret", "SPACE");

        assertThat(pages).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void mockHttp(int status, String responseBody) throws Exception {
        when(classicHttpResponse.getCode()).thenReturn(status);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity(responseBody));
        when(confluenceHttpClient.execute(any(), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                    return handler.handleResponse(classicHttpResponse);
                });
    }

    @SuppressWarnings("unchecked")
    private void mockHttpMulti() throws Exception {
        when(confluenceHttpClient.execute(any(), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<Object> handler = invocation.getArgument(1);
                    return handler.handleResponse(classicHttpResponse);
                });
    }

    private IntegrationSecret buildBasicSecret(String baseUrl) {
        return IntegrationSecret.builder()
                .baseUrl(baseUrl)
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder()
                        .username("user@example.com")
                        .password("api-token")
                        .build())
                .build();
    }
}
