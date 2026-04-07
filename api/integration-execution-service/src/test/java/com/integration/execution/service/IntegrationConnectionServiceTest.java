package com.integration.execution.service;

import com.integration.execution.config.cache.TokenCache;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.request.ConnectionReTestRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.exception.IntegrationApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationConnectionService")
class IntegrationConnectionServiceTest {

    @Mock
    private IntegrationConnectionTestService integrationConnectionTestService;

    @Mock
    private VaultService vaultService;

    @Mock
    private TokenCache tokenCache;

    private IntegrationConnectionService service;

    @BeforeEach
    void setUp() {
        service = new IntegrationConnectionService(integrationConnectionTestService, vaultService, tokenCache);
    }

    @Test
    void testAndCreateConnection_connectionValid_savesSecretAndReturnsSuccessResponse() {
        IntegrationConnectionRequest request = new IntegrationConnectionRequest(
                "Conn",
                ServiceType.JIRA,
                basicSecret("old-password")
        );
        when(integrationConnectionTestService.testConnection(ServiceType.JIRA, request.integrationSecret()))
                .thenReturn(new ApiResponse(SC_OK, true, "ok"));

        ResponseEntity<ConnectionTestResponse> entity =
                service.testAndCreateConnection(request, "tenant-a", "user-a");

        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isTrue();
        assertThat(entity.getBody().getConnectionStatus()).isEqualTo(ConnectionStatus.SUCCESS);
        assertThat(entity.getBody().getSecretName()).isNotBlank();
        assertThat(entity.getBody().getSecretName()).contains("tenant-a");
        verify(vaultService).saveSecret(any(String.class), eq(request.integrationSecret()));
    }

    @Test
    void testAndCreateConnection_connectionInvalid_doesNotSaveSecretAndReturnsFailedResponse() {
        IntegrationConnectionRequest request = new IntegrationConnectionRequest(
                "Conn",
                ServiceType.ARCGIS,
                basicSecret("old-password")
        );
        when(integrationConnectionTestService.testConnection(ServiceType.ARCGIS, request.integrationSecret()))
                .thenReturn(new ApiResponse(SC_BAD_REQUEST, false, "invalid"));

        ResponseEntity<ConnectionTestResponse> entity =
                service.testAndCreateConnection(request, "tenant-b", "user-b");

        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isFalse();
        assertThat(entity.getBody().getConnectionStatus()).isEqualTo(ConnectionStatus.FAILED);
        assertThat(entity.getBody().getSecretName()).isNull();
        verify(vaultService, never()).saveSecret(any(String.class), any(IntegrationSecret.class));
    }

    @Test
    void testExistingConnection_resultMapped_returnsExpectedResponse() {
        ConnectionReTestRequest request = new ConnectionReTestRequest("secret-x", ServiceType.JIRA);
        when(integrationConnectionTestService.testConnection(ServiceType.JIRA, "secret-x"))
                .thenReturn(new ApiResponse(SC_OK, true, "healthy"));

        ConnectionTestResponse response = service.testExistingConnection(UUID.randomUUID(), request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatusCode()).isEqualTo(SC_OK);
        assertThat(response.getConnectionStatus()).isEqualTo(ConnectionStatus.SUCCESS);
        assertThat(response.getLastConnectionTest()).isNotNull();
    }

    @Test
    void rotateConnectionSecret_connectionIdNull_throwsIllegalArgumentException() {
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret", ServiceType.JIRA, "new-secret");

        assertThatThrownBy(() -> service.rotateConnectionSecret(null, "tenant", "user", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectionId must be provided");
    }

    @Test
    void rotateConnectionSecret_secretNameMissing_throwsIllegalArgumentException() {
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest(" ", ServiceType.JIRA, "new-secret");

        assertThatThrownBy(() -> service.rotateConnectionSecret(UUID.randomUUID(), "tenant", "user", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secretName must be provided");
    }

    @Test
    void rotateConnectionSecret_serviceTypeMissing_throwsIllegalArgumentException() {
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret", null, "new-secret");

        assertThatThrownBy(() -> service.rotateConnectionSecret(UUID.randomUUID(), "tenant", "user", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceType must be provided");
    }

    @Test
    void rotateConnectionSecret_basicAuthAndValidSecret_updatesPasswordAndKeepsUsername() {
        UUID connectionId = UUID.randomUUID();
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret-name", ServiceType.JIRA, "new-password");
        IntegrationSecret existingSecret = basicSecret("old-password");

        when(vaultService.getSecret("secret-name")).thenReturn(existingSecret);
        when(integrationConnectionTestService.testConnection(eq(ServiceType.JIRA), any(IntegrationSecret.class)))
                .thenReturn(new ApiResponse(SC_OK, true, "ok"));

        service.rotateConnectionSecret(connectionId, "tenant", "user", request);

        ArgumentCaptor<IntegrationSecret> secretCaptor = ArgumentCaptor.forClass(IntegrationSecret.class);
        verify(vaultService).saveSecret(eq("secret-name"), secretCaptor.capture());
        IntegrationSecret rotated = secretCaptor.getValue();
        assertThat(rotated.getAuthType()).isEqualTo(CredentialAuthType.BASIC_AUTH);
        assertThat(((BasicAuthCredential) rotated.getCredentials()).getUsername()).isEqualTo("user");
        assertThat(((BasicAuthCredential) rotated.getCredentials()).getPassword()).isEqualTo("new-password");
        verify(tokenCache, never()).invalidate(any(String.class));
    }

    @Test
    void rotateConnectionSecret_oauthAndValidSecret_updatesSecretAndInvalidatesCache() {
        UUID connectionId = UUID.randomUUID();
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret-name", ServiceType.ARCGIS, "new-client-secret");
        IntegrationSecret existingSecret = oauthSecret("old-client-secret");

        when(vaultService.getSecret("secret-name")).thenReturn(existingSecret);
        when(integrationConnectionTestService.testConnection(eq(ServiceType.ARCGIS), any(IntegrationSecret.class)))
                .thenReturn(new ApiResponse(SC_OK, true, "ok"));

        service.rotateConnectionSecret(connectionId, "tenant", "user", request);

        ArgumentCaptor<IntegrationSecret> secretCaptor = ArgumentCaptor.forClass(IntegrationSecret.class);
        verify(vaultService).saveSecret(eq("secret-name"), secretCaptor.capture());
        IntegrationSecret rotated = secretCaptor.getValue();
        OAuthClientCredential rotatedCreds = (OAuthClientCredential) rotated.getCredentials();
        assertThat(rotatedCreds.getClientId()).isEqualTo("client-id");
        assertThat(rotatedCreds.getClientSecret()).isEqualTo("new-client-secret");
        verify(tokenCache).invalidate("secret-name");
    }

    @Test
    void rotateConnectionSecret_connectionTestFails_throwsIntegrationApiException() {
        UUID connectionId = UUID.randomUUID();
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret-name", ServiceType.JIRA, "new-password");

        when(vaultService.getSecret("secret-name")).thenReturn(basicSecret("old-password"));
        when(integrationConnectionTestService.testConnection(eq(ServiceType.JIRA), any(IntegrationSecret.class)))
                .thenReturn(new ApiResponse(SC_BAD_REQUEST, false, "invalid"));

        assertThatThrownBy(() -> service.rotateConnectionSecret(connectionId, "tenant", "user", request))
                .isInstanceOf(IntegrationApiException.class)
                .hasMessageContaining("Invalid secret key: invalid");

        verify(vaultService, never()).saveSecret(any(String.class), any(IntegrationSecret.class));
        verify(tokenCache, never()).invalidate(any(String.class));
    }

    @Test
    void rotateConnectionSecret_authTypeNull_throwsIllegalStateException() {
        UUID connectionId = UUID.randomUUID();
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret-name", ServiceType.JIRA, "new-password");
        IntegrationSecret brokenSecret = IntegrationSecret.builder()
                .baseUrl("https://jira.example.com")
                .authType(null)
                .credentials(BasicAuthCredential.builder().username("user").password("old").build())
                .build();

        when(vaultService.getSecret("secret-name")).thenReturn(brokenSecret);

        assertThatThrownBy(() -> service.rotateConnectionSecret(connectionId, "tenant", "user", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported authType=null");
    }

    @Test
    void rotateConnectionSecret_oauthWithWrongCredentialType_throwsIllegalStateException() {
        UUID connectionId = UUID.randomUUID();
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret-name", ServiceType.JIRA, "new-password");
        IntegrationSecret wrongType = IntegrationSecret.builder()
                .baseUrl("https://jira.example.com")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(BasicAuthCredential.builder().username("user").password("old").build())
                .build();

        when(vaultService.getSecret("secret-name")).thenReturn(wrongType);

        assertThatThrownBy(() -> service.rotateConnectionSecret(connectionId, "tenant", "user", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid OAUTH2 credentials stored");
    }

    private IntegrationSecret basicSecret(String password) {
        return IntegrationSecret.builder()
                .baseUrl("https://jira.example.com")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder()
                        .username("user")
                        .password(password)
                        .build())
                .build();
    }

    private IntegrationSecret oauthSecret(String clientSecret) {
        return IntegrationSecret.builder()
                .baseUrl("https://arcgis.example.com")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(OAuthClientCredential.builder()
                        .clientId("client-id")
                        .clientSecret(clientSecret)
                        .tokenUrl("https://arcgis.example.com/oauth2/token")
                        .scope("scope")
                        .build())
                .build();
    }
}