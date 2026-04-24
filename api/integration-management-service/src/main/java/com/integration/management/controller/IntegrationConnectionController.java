package com.integration.management.controller;

import com.integration.execution.contract.model.enums.AuditActivity;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.config.aspect.AuditLoggable;
import com.integration.management.model.dto.response.ConnectionDependentsResponse;
import com.integration.management.service.IntegrationConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.integration.execution.contract.model.enums.AuditActivity.CREATE;
import static com.integration.execution.contract.model.enums.AuditActivity.DELETE;
import static com.integration.execution.contract.model.enums.EntityType.INTEGRATION_CONNECTION;

import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/connections")
public class IntegrationConnectionController {

    private final IntegrationConnectionService integrationConnectionService;

    @PostMapping("/test-connection")
    @PreAuthorize("@serviceTypeAuth.hasAccessToServiceType(#request.serviceType)")
    @AuditLoggable(entityType = INTEGRATION_CONNECTION, action = CREATE)
    public ResponseEntity<?> testAndCreateConnection(
            @Valid @RequestBody IntegrationConnectionRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Testing and creating {} connection for tenant: {} by user: {}",
                request.serviceType(), tenantId, userId);
        return integrationConnectionService
                .testAndCreateConnection(request, tenantId, userId);
    }

    @GetMapping
    @PreAuthorize("@serviceTypeAuth.hasAccessToServiceType(#serviceType)")
    public ResponseEntity<List<IntegrationConnectionResponse>> getConnections(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestParam ServiceType serviceType) {
        log.info("Fetching connections for tenant: {} and service type: {}",
                tenantId, serviceType);
        return ResponseEntity.ok(
                integrationConnectionService
                        .getConnectionsByTenantAndServiceType(tenantId, serviceType));
    }

    @GetMapping("/{connectionId}")
    @PreAuthorize("@serviceTypeAuth.hasAccessToConnection(#connectionId, #tenantId)")
    public ResponseEntity<IntegrationConnectionResponse> getConnectionById(
            @PathVariable UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching connection {} for tenant: {}", connectionId, tenantId);
        return ResponseEntity.ok(integrationConnectionService.getConnectionById(connectionId, tenantId));
    }

    @PostMapping("/{connectionId}/test")
    @PreAuthorize("@serviceTypeAuth.hasAccessToConnection(#connectionId, #tenantId)")
    public ResponseEntity<ConnectionTestResponse> testConnection(
            @PathVariable UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Testing connection {} for tenant: {}", connectionId, tenantId);
        return ResponseEntity.ok(integrationConnectionService
                .testExistingConnection(connectionId, tenantId));
    }

    @GetMapping("/{connectionId}/dependents")
    @PreAuthorize("@serviceTypeAuth.hasAccessToConnection(#connectionId, #tenantId)")
    public ResponseEntity<ConnectionDependentsResponse> getDependents(
            @PathVariable UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching dependents for connection {} tenant {}", connectionId, tenantId);
        return ResponseEntity.ok(integrationConnectionService.getDependents(connectionId, tenantId));
    }

    @DeleteMapping("/{connectionId}")
    @PreAuthorize("@serviceTypeAuth.hasAccessToConnection(#connectionId, #tenantId)")
    @AuditLoggable(entityType = INTEGRATION_CONNECTION, action = DELETE, entityIdParam = "connectionId")
    public ResponseEntity<Void> deleteConnection(
            @PathVariable UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Deleting connection {} for tenant: {}", connectionId, tenantId);
        try {
            integrationConnectionService.deleteConnection(connectionId, tenantId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid connectionId format: {}", connectionId, e);
            throw new IllegalArgumentException("Invalid connectionId format: " + connectionId, e);
        }
    }

    @PutMapping("/{connectionId}/secret")
    @PreAuthorize("hasRole('tenant_admin') and @serviceTypeAuth.hasAccessToConnection(#connectionId, #tenantId)")
    @AuditLoggable(entityType = INTEGRATION_CONNECTION, action = AuditActivity.UPDATE, entityIdParam = "connectionId")
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
