package com.integration.execution.service;

import com.integration.execution.config.cache.TokenCache;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.rest.request.ConnectionReTestRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.util.SecretKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

import static com.integration.execution.contract.model.enums.ConnectionStatus.FAILED;
import static com.integration.execution.contract.model.enums.ConnectionStatus.SUCCESS;
import static com.integration.execution.contract.model.enums.CredentialAuthType.OAUTH2;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConnectionService {

    private final IntegrationConnectionTestService integrationConnectionTestService;
    private final VaultService vaultService;
    private final TokenCache tokenCache;

    public ResponseEntity<ConnectionTestResponse> testAndCreateConnection(
            IntegrationConnectionRequest request,
            String tenantId,
            String userId) {

        UUID connectionId = UUID.randomUUID();
        String secretName = SecretKeyUtil.generate(request.serviceType(), tenantId, connectionId);
        Instant now = Instant.now();

        ApiResponse testResult = integrationConnectionTestService
                .testConnection(request.serviceType(), request.integrationSecret());

        ConnectionTestResponse response = ConnectionTestResponse.builder()
                .success(testResult.success())
                .statusCode(testResult.statusCode())
                .message(testResult.message())
                .connectionStatus(testResult.success() ? SUCCESS : FAILED)
                .lastConnectionTest(now)
                .build();

        if (!testResult.success()) {
            return ResponseEntity.ok(response);
        }

        vaultService.saveSecret(secretName, request.integrationSecret());
        response.setSecretName(secretName);
        return ResponseEntity.ok(response);
    }

    public ConnectionTestResponse testExistingConnection(UUID connectionId, ConnectionReTestRequest request) {
        Instant now = Instant.now();
        ApiResponse result = integrationConnectionTestService
                .testConnection(request.serviceType(), request.secretName());
        return ConnectionTestResponse.builder()
                .success(result.success())
                .statusCode(result.statusCode())
                .message(result.message())
                .connectionStatus(result.success() ? SUCCESS : FAILED)
                .lastConnectionTest(now)
                .build();
    }

    @Transactional(noRollbackFor = IntegrationApiException.class)
    public void rotateConnectionSecret(
            UUID connectionId,
            String tenantId,
            String userId,
            IntegrationConnectionSecretRotateRequest request) {

        if (connectionId == null) {
            throw new IllegalArgumentException("connectionId must be provided");
        }
        String secretName = request.secretName();
        if (!StringUtils.hasText(secretName)) {
            throw new IllegalArgumentException(
                    "secretName must be provided in the request for connection id=" + connectionId);
        }
        if (request.serviceType() == null) {
            throw new IllegalArgumentException(
                    "serviceType must be provided in the request for connection id=" + connectionId);
        }

        IntegrationSecret secret = vaultService.getSecret(secretName);
        IntegrationSecret rotatedSecret = buildRotatedSecret(connectionId, secret, request.newSecret());

        ApiResponse testResult = integrationConnectionTestService
                .testConnection(request.serviceType(), rotatedSecret);

        if (!testResult.success()) {
            throw new IntegrationApiException(
                    "Invalid secret key: " + testResult.message(),
                    testResult.statusCode());
        }

        vaultService.saveSecret(secretName, rotatedSecret);

        if (secret.getAuthType().equals(OAUTH2)) {
            tokenCache.invalidate(secretName);
        }
    }

    private IntegrationSecret buildRotatedSecret(UUID connectionId, IntegrationSecret secret, String newSecret) {
        CredentialAuthType authType = secret.getAuthType();
        if (authType == null) {
            throw new IllegalStateException("Unsupported authType=null for connection id=" + connectionId);
        }
        return switch (authType) {
            case BASIC_AUTH -> buildRotatedBasicAuthSecret(connectionId, secret, newSecret);
            case OAUTH2 -> buildRotatedOAuth2Secret(connectionId, secret, newSecret);
        };
    }

    private IntegrationSecret buildRotatedBasicAuthSecret(
            UUID connectionId,
            IntegrationSecret secret,
            String newSecret
    ) {
        if (secret.getCredentials() instanceof BasicAuthCredential basic) {
            return IntegrationSecret.builder()
                    .baseUrl(secret.getBaseUrl())
                    .authType(CredentialAuthType.BASIC_AUTH)
                    .credentials(BasicAuthCredential.builder()
                            .username(basic.getUsername())
                            .password(newSecret)
                            .build())
                    .build();
        } else {
            throw new IllegalStateException(
                    "Invalid BASIC_AUTH credentials stored for connection id=" + connectionId
            );
        }
    }

    private IntegrationSecret buildRotatedOAuth2Secret(
            UUID connectionId,
            IntegrationSecret secret,
            String newSecret
    ) {
        if (secret.getCredentials() instanceof OAuthClientCredential oauth) {
            return IntegrationSecret.builder()
                    .baseUrl(secret.getBaseUrl())
                    .authType(OAUTH2)
                    .credentials(OAuthClientCredential.builder()
                            .clientId(oauth.getClientId())
                            .clientSecret(newSecret)
                            .tokenUrl(oauth.getTokenUrl())
                            .scope(oauth.getScope())
                            .build())
                    .build();
        } else {
            throw new IllegalStateException(
                    "Invalid OAUTH2 credentials stored for connection id=" + connectionId
            );
        }
    }

}
