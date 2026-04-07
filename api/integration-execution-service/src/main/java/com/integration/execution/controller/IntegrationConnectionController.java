package com.integration.execution.controller;

import com.integration.execution.contract.rest.request.ConnectionReTestRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.service.IntegrationConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.integration.execution.constants.ExecutionSecurityConstants.X_TENANT_ID;
import static com.integration.execution.constants.ExecutionSecurityConstants.X_USER_ID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/connections")
public class IntegrationConnectionController {

    private final IntegrationConnectionService integrationConnectionService;

    @PostMapping("/test-connection")
    public ResponseEntity<ConnectionTestResponse> testAndCreateConnection(
            @Valid @RequestBody IntegrationConnectionRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Testing and creating {} connection for tenant: {} by user: {}",
                request.serviceType(), tenantId, userId);
        return integrationConnectionService
                .testAndCreateConnection(request, tenantId, userId);
    }

    @PostMapping("/{connectionId}/test")
    public ResponseEntity<ConnectionTestResponse> testConnection(
            @PathVariable UUID connectionId,
            @Valid @RequestBody ConnectionReTestRequest reTestRequest,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Testing connection {} for tenant: {}", connectionId, tenantId);
        return ResponseEntity.ok(integrationConnectionService
                .testExistingConnection(connectionId, reTestRequest));
    }

    @PutMapping("/{connectionId}/secret")
    public ResponseEntity<Void> rotateConnectionSecret(
            @PathVariable UUID connectionId,
            @Valid @RequestBody IntegrationConnectionSecretRotateRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Rotating connection secret for connection {} tenant {} by user {}",
                connectionId, tenantId, userId);
        integrationConnectionService.rotateConnectionSecret(connectionId, tenantId, userId, request);
        return ResponseEntity.noContent().build();
    }

}
