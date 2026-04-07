package com.integration.execution.controller;

import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.request.ConnectionReTestRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.service.IntegrationConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationConnectionControllerTest {

    @Mock
    private IntegrationConnectionService integrationConnectionService;

    private IntegrationConnectionController controller;

    @BeforeEach
    void setUp() {
        controller = new IntegrationConnectionController(integrationConnectionService);
    }

    @Test
    void testAndCreateConnection_validRequest_forwardsTenantAndUserToService() {
        IntegrationConnectionRequest request = new IntegrationConnectionRequest(
                "jira",
                ServiceType.JIRA,
                basicSecret("pwd")
        );

        ConnectionTestResponse response = ConnectionTestResponse.builder()
                .success(true)
                .statusCode(200)
                .message("ok")
                .connectionStatus(ConnectionStatus.SUCCESS)
                .lastConnectionTest(Instant.now())
                .secretName("jira-tenant-a-uuid")
                .build();

        when(integrationConnectionService.testAndCreateConnection(request, "tenant-a", "user-a"))
                .thenReturn(ResponseEntity.ok(response));

        ResponseEntity<ConnectionTestResponse> entity =
                controller.testAndCreateConnection(request, "tenant-a", "user-a");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getSecretName()).isEqualTo("jira-tenant-a-uuid");
        verify(integrationConnectionService).testAndCreateConnection(request, "tenant-a", "user-a");
    }

    @Test
    void testConnection_validRequest_returnsServicePayload() {
        UUID connectionId = UUID.randomUUID();
        ConnectionReTestRequest request = new ConnectionReTestRequest("secret-a", ServiceType.JIRA);

        ConnectionTestResponse response = ConnectionTestResponse.builder()
                .success(true)
                .statusCode(200)
                .message("healthy")
                .connectionStatus(ConnectionStatus.SUCCESS)
                .lastConnectionTest(Instant.now())
                .build();

        when(integrationConnectionService.testExistingConnection(connectionId, request)).thenReturn(response);

        ResponseEntity<ConnectionTestResponse> entity =
                controller.testConnection(connectionId, request, "tenant-a");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isTrue();
        verify(integrationConnectionService).testExistingConnection(connectionId, request);
    }

    @Test
    void rotateConnectionSecret_validRequest_returnsNoContentAndForwardsTenantUser() {
        UUID connectionId = UUID.randomUUID();
        IntegrationConnectionSecretRotateRequest request =
                new IntegrationConnectionSecretRotateRequest("secret-a", ServiceType.JIRA, "new-secret");

        ResponseEntity<Void> entity =
                controller.rotateConnectionSecret(connectionId, request, "tenant-a", "user-a");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(integrationConnectionService).rotateConnectionSecret(connectionId, "tenant-a", "user-a", request);
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
}
