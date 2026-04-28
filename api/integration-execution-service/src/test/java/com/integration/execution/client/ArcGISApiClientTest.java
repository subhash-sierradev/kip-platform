package com.integration.execution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.config.cache.TokenCache;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.service.VaultService;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ArcGISApiClientTest {

    @Mock
    private TokenCache tokenCache;

    @Mock
    private CloseableHttpClient arcgisHttpClient;

    @Mock
    private VaultService vaultService;

    @Mock
    private ClassicHttpResponse classicHttpResponse;

    private ArcGISApiClient client;

    @BeforeEach
    void setUp() {
        client = new ArcGISApiClient(tokenCache, arcgisHttpClient, vaultService, new ObjectMapper());
    }

    @Test
    void getAccessToken_cachedToken_returnsWithoutVaultLookup() {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");

        String token = client.getAccessToken("secret");

        assertThat(token).isEqualTo("cached-token");
        verify(vaultService, never()).getSecret(anyString());
    }

    @Test
    void getAccessToken_cacheMiss_fetchesSecretFromVault() throws Exception {
        IntegrationSecret secret = oauthSecret("https://example.com/FeatureServer");
        when(tokenCache.getValidToken("secret")).thenReturn(null);
        when(vaultService.getSecret("secret")).thenReturn(secret);
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"fresh-token\",\"expires_in\":120}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getAccessToken("secret");

        assertThat(token).isEqualTo("fresh-token");
        verify(vaultService).getSecret("secret");
    }

    @Test
    void getAccessToken_invalidCredentialType_throwsIntegrationApiException() {
        IntegrationSecret basicSecret = IntegrationSecret.builder()
                .baseUrl("https://example.com")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u").password("p").build())
                .build();

        assertThatThrownBy(() -> client.getAccessToken("secret", basicSecret))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Invalid credential type");
    }

    @Test
    void getAccessToken_successfulResponse_storesAndReturnsToken() throws Exception {
        OAuthClientCredential oauth = oauthCredential();
        IntegrationSecret secret = IntegrationSecret.builder()
                .baseUrl("https://example.com/FeatureServer")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(oauth)
                .build();

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"new-token\",\"expires_in\":120}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getAccessToken("secret", secret);

        assertThat(token).isEqualTo("new-token");
        verify(tokenCache).store("secret", "new-token", 120L);
    }

    @Test
    void getAccessToken_httpError_throwsIntegrationApiExceptionWithStatus() throws Exception {
        OAuthClientCredential oauth = oauthCredential();
        IntegrationSecret secret = IntegrationSecret.builder()
                .baseUrl("https://example.com/FeatureServer")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(oauth)
                .build();

        when(classicHttpResponse.getCode()).thenReturn(401);
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.getAccessToken("secret", secret))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    void queryFeatures_arcGisErrorPayload_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"error\":{\"code\":498,\"message\":\"invalid token\"}}"));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.queryFeatures("secret"))
                .isInstanceOf(IntegrationApiException.class)
            .hasMessageContaining("ArcGIS query failed");
    }

    @Test
    void applyEditsWithPartition_invalidPayload_throwsWrappedIntegrationApiException() {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        assertThatThrownBy(() -> client.applyEditsWithPartition("secret", "not-json"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("ArcGIS applyEdits failed");
    }

    @Test
    void applyEditsWithPartition_validPayload_returnsResponseNode() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(new StringEntity("{\"addResults\":[]}"));
        mockHttpExecution(classicHttpResponse);

        JsonNode response = client.applyEditsWithPartition(
                "secret",
                "{\"adds\":[],\"updates\":[]}"
        );

        assertThat(response.has("addResults")).isTrue();
    }

    @Test
    void getAccessToken_whenSecretNameBlank_doesNotStoreToken() throws Exception {
        OAuthClientCredential oauth = oauthCredential();
        IntegrationSecret secret = IntegrationSecret.builder()
                .baseUrl("https://example.com/FeatureServer")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(oauth)
                .build();

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"new-token\",\"expires_in\":120}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getAccessToken("   ", secret);

        assertThat(token).isEqualTo("new-token");
        verify(tokenCache, never()).store(anyString(), anyString(), any());
    }

    @Test
    void getAccessToken_whenTokenMissing_throwsUnauthorizedException() throws Exception {
        IntegrationSecret secret = oauthSecret("https://example.com/FeatureServer");

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"expires_in\":120}"));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.getAccessToken("secret", secret))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Token generation failed")
                .cause()
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IntegrationApiException.class)
                .rootCause()
                .hasMessageContaining("Token missing in response");
    }

    @Test
    void getAccessToken_whenExpiresInMissing_storesNullTtl() throws Exception {
        IntegrationSecret secret = oauthSecret("https://example.com/FeatureServer");

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"new-token\"}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getAccessToken("secret", secret);

        assertThat(token).isEqualTo("new-token");
        verify(tokenCache).store("secret", "new-token", null);
    }

    @Test
    void queryFeatures_success_returnsJsonNode() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"features\":[{\"id\":1}]}"));
        mockHttpExecution(classicHttpResponse);

        JsonNode response = client.queryFeatures("secret");

        assertThat(response.path("features")).hasSize(1);
    }

    @Test
    void queryFeaturesWithWhere_success_encodesAndReturnsJsonNode() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"features\":[{\"attributes\":{\"OBJECTID\":1}}]}"));
        mockHttpExecution(classicHttpResponse);

        JsonNode response = client.queryFeaturesWithWhere("secret", "status = 'OPEN'");

        assertThat(response.path("features")).hasSize(1);
    }

    @Test
    void queryFeaturesWithWhere_whenHttpReturns404_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(404);
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.queryFeaturesWithWhere("secret", "1=1"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("HTTP 404");
    }

    @Test
    void queryFeaturesWithWhere_whenArcGisBodyContainsError_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"error\":{\"code\":400,\"message\":\"invalid where clause\"}}"));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.queryFeaturesWithWhere("secret", "invalid ="))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("ArcGIS query failed");
    }

    @Test
    void queryFeaturesWithWhere_whenEntityMissing_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(null);
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.queryFeaturesWithWhere("secret", "1=1"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Empty HTTP response");
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryFeaturesWithWhere_whenHttpClientThrows_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));
        when(arcgisHttpClient.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class)))
                .thenThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> client.queryFeaturesWithWhere("secret", "1=1"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("ArcGIS query failed");
    }

    @Test
    void getAccessToken_whenTokenBlank_throwsUnauthorizedException() throws Exception {
        // Covers token.isBlank() = true branch in "token == null || token.isBlank()"
        IntegrationSecret secret = oauthSecret("https://example.com/FeatureServer");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"  \",\"expires_in\":120}"));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.getAccessToken("secret", secret))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Token generation failed");
    }

    @Test
    void getAccessToken_whenSecretNameNull_doesNotStoreToken() throws Exception {
        // Covers secretName != null = false branch in "secretName != null && !secretName.isBlank()"
        IntegrationSecret secret = oauthSecret("https://example.com/FeatureServer");
        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"token\",\"expires_in\":120}"));
        mockHttpExecution(classicHttpResponse);

        String token = client.getAccessToken(null, secret);

        assertThat(token).isEqualTo("token");
        verify(tokenCache, never()).store(anyString(), anyString(), any());
    }

    @Test
    void queryFeatures_statusBelow200_throwsIntegrationApiException() throws Exception {
        // Covers "status < 200" = true branch in validateResponse
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));
        when(classicHttpResponse.getCode()).thenReturn(102);
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.queryFeatures("secret"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("HTTP 102");
    }

    @Test
    void fetchArcGISFields_whenFieldsNodeIsNotArray_returnsEmptyList() {
        // Covers !fieldsNode.isArray() = true branch in fetchArcGISFields
        ArcGISApiClient spyClient = org.mockito.Mockito.spy(client);
        doReturn(new ObjectMapper().createObjectNode().put("fields", "not-an-array"))
                .when(spyClient).queryFeatures("secret");

        assertThat(spyClient.fetchArcGISFields("secret")).isEmpty();
    }

    @Test
    void fetchArcGISFields_fieldWithNoOptionalKeys_mapsWithNullsForAbsentKeys() throws Exception {
        // Covers !node.has(fieldName) = true in getNullableInt, getNullableText, getNullableNode
        ArcGISApiClient spyClient = org.mockito.Mockito.spy(client);
        JsonNode root = new ObjectMapper().readTree("""
                {
                  "fields": [
                    {
                      "name": "minimal_field",
                      "type": "esriFieldTypeString",
                      "alias": "Minimal",
                      "sqlType": "sqlTypeNVarchar",
                      "nullable": true,
                      "editable": true
                    }
                  ]
                }
                """);
        doReturn(root).when(spyClient).queryFeatures("secret");

        List<ArcGISFieldDto> fields = spyClient.fetchArcGISFields("secret");

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).getName()).isEqualTo("minimal_field");
        assertThat(fields.get(0).getLength()).isNull();
        assertThat(fields.get(0).getPrecision()).isNull();
        assertThat(fields.get(0).getDescription()).isNull();
        assertThat(fields.get(0).getDefaultValue()).isNull();
        assertThat(fields.get(0).getDomain()).isNull();
    }

    @Test
    void queryFeatures_whenResponseEntityMissing_throwsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity()).thenReturn(null);
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.queryFeatures("secret"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Empty HTTP response");
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryFeatures_whenHttpClientFails_wrapsAsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));
        when(arcgisHttpClient.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class)))
                .thenThrow(new RuntimeException("connection failed"));

        assertThatThrownBy(() -> client.queryFeatures("secret"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("ArcGIS query failed");
    }

    @Test
    void applyEditsWithPartition_whenArcGisReturnsError_wrapsAsIntegrationApiException() throws Exception {
        when(tokenCache.getValidToken("secret")).thenReturn("cached-token");
        when(vaultService.getSecret("secret")).thenReturn(oauthSecret("https://example.com/FeatureServer"));

        when(classicHttpResponse.getCode()).thenReturn(200);
        when(classicHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"error\":{\"code\":400,\"message\":\"bad edits\"}}"));
        mockHttpExecution(classicHttpResponse);

        assertThatThrownBy(() -> client.applyEditsWithPartition("secret", "{\"adds\":[],\"updates\":[]}"))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("ArcGIS applyEdits failed");
    }

    @Test
    void fetchArcGISFields_whenFieldsMissing_returnsEmptyList() {
        ArcGISApiClient spyClient = org.mockito.Mockito.spy(client);
        doReturn(new ObjectMapper().createObjectNode())
                .when(spyClient).queryFeatures("secret");

        assertThat(spyClient.fetchArcGISFields("secret")).isEmpty();
    }

    @Test
    void fetchArcGISFields_mapsNullableAndPresentFieldValues() throws Exception {
        ArcGISApiClient spyClient = org.mockito.Mockito.spy(client);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree("""
                {
                  "fields": [
                    {
                      "name": "external_location_id",
                      "type": "esriFieldTypeString",
                      "alias": "External Location Id",
                      "sqlType": "sqlTypeNVarchar",
                      "nullable": false,
                      "editable": true,
                      "length": 255,
                      "precision": 0,
                      "description": "external id",
                      "defaultValue": "n/a",
                      "domain": {"type": "codedValue"}
                    },
                    {
                      "name": "optional_field",
                      "type": "esriFieldTypeInteger",
                      "alias": "Optional",
                      "sqlType": "sqlTypeInteger",
                      "nullable": true,
                      "editable": false,
                      "length": null,
                      "precision": null,
                      "description": null,
                      "defaultValue": null,
                      "domain": null
                    }
                  ]
                }
                """);
        doReturn(root).when(spyClient).queryFeatures("secret");

        List<ArcGISFieldDto> fields = spyClient.fetchArcGISFields("secret");

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).getName()).isEqualTo("external_location_id");
        assertThat(fields.get(0).getLength()).isEqualTo(255);
        assertThat(fields.get(0).getDescription()).isEqualTo("external id");
        assertThat(fields.get(1).getLength()).isNull();
        assertThat(fields.get(1).getDescription()).isNull();
        assertThat(fields.get(1).getDefaultValue()).isNull();
    }

    @SuppressWarnings("unchecked")
    private void mockHttpExecution(ClassicHttpResponse response) throws Exception {
        when(arcgisHttpClient.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                    return handler.handleResponse(response);
                });
    }

    private OAuthClientCredential oauthCredential() {
        return OAuthClientCredential.builder()
                .clientId("client-id")
                .clientSecret("client-secret")
                .tokenUrl("https://example.com/oauth2/token")
                .scope("scope")
                .build();
    }

    private IntegrationSecret oauthSecret(String baseUrl) {
        return IntegrationSecret.builder()
                .baseUrl(baseUrl)
                .authType(CredentialAuthType.OAUTH2)
                .credentials(oauthCredential())
                .build();
    }
}
