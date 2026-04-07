package com.integration.management.controller;

import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import com.integration.management.config.aspect.AuditLoggable;
import com.integration.management.exception.SchedulingException;
import com.integration.management.model.dto.request.ConfluenceIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.response.ConfluenceIntegrationResponse;
import com.integration.management.model.dto.response.ConfluenceIntegrationSummaryResponse;
import com.integration.management.service.ConfluenceLookupService;
import com.integration.management.service.ConfluenceIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.integration.execution.contract.model.enums.AuditActivity.CREATE;
import static com.integration.execution.contract.model.enums.AuditActivity.DELETE;
import static com.integration.execution.contract.model.enums.AuditActivity.PENDING;
import static com.integration.execution.contract.model.enums.AuditActivity.RETRY;
import static com.integration.execution.contract.model.enums.AuditActivity.RUN_NOW;
import static com.integration.execution.contract.model.enums.AuditActivity.UPDATE;
import static com.integration.execution.contract.model.enums.EntityType.CONFLUENCE_INTEGRATION;
import static com.integration.management.constants.IntegrationManagementConstants.STATUS_DISABLED;
import static com.integration.management.constants.IntegrationManagementConstants.STATUS_ENABLED;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/confluence")
@PreAuthorize("hasRole('feature_confluence_integration')")
public class ConfluenceIntegrationController {

    private final ConfluenceIntegrationService confluenceIntegrationService;
    private final ConfluenceLookupService confluenceLookupService;

    @AuditLoggable(entityType = CONFLUENCE_INTEGRATION, action = CREATE)
    @PostMapping
    public ResponseEntity<CreationResponse> create(
            @Valid @RequestBody ConfluenceIntegrationCreateUpdateRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Creating Confluence integration: {} for tenant: {}", request.getName(), tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(confluenceIntegrationService.create(request, tenantId, userId));
    }

    @AuditLoggable(entityType = CONFLUENCE_INTEGRATION, action = UPDATE)
    @PutMapping("/{id}")
    public ResponseEntity<CreationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ConfluenceIntegrationCreateUpdateRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Updating Confluence integration: {} (ID: {}) for tenant: {}", request.getName(), id, tenantId);
        try {
            return ResponseEntity.ok(confluenceIntegrationService.update(id, request, tenantId, userId));
        } catch (SchedulingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to update integration schedule due to scheduling error: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public ResponseEntity<List<ConfluenceIntegrationSummaryResponse>> getAllByTenant(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all Confluence integrations for tenant: {}", tenantId);
        return ResponseEntity.ok(confluenceIntegrationService.getAllByTenant(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfluenceIntegrationResponse> getById(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching Confluence integration with ID: {} for tenant: {}", id, tenantId);
        return ResponseEntity.ok(confluenceIntegrationService.getByIdAndTenantWithDetails(id, tenantId));
    }

    @AuditLoggable(entityType = CONFLUENCE_INTEGRATION, action = DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Deleting Confluence integration with ID: {} for tenant: {}", id, tenantId);
        confluenceIntegrationService.delete(id, tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<IntegrationJobExecutionDto>> getJobHistory(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching job execution history for Confluence integration with ID: {} for tenant: {}",
                id, tenantId);
        return ResponseEntity.ok(confluenceIntegrationService.getJobHistory(id, tenantId));
    }

    @AuditLoggable(entityType = CONFLUENCE_INTEGRATION, action = RUN_NOW)
    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerJobExecution(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Triggering manual job execution for Confluence integration with ID: {} "
                + "for tenant: {} by user: {}", id, tenantId, userId);
        confluenceIntegrationService.triggerJobExecution(id, tenantId, userId);
        return ResponseEntity.ok().build();
    }

    @AuditLoggable(entityType = CONFLUENCE_INTEGRATION, action = RETRY)
    @PostMapping("/{integrationId}/executions/{originalJobId}/retry")
    public ResponseEntity<Void> retryExecution(
            @PathVariable UUID integrationId,
            @PathVariable UUID originalJobId,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Retrying Confluence execution {} for integration {} tenant {} by user {}",
                originalJobId, integrationId, tenantId, userId);
        confluenceIntegrationService.retryJobExecution(integrationId, originalJobId, tenantId, userId);
        return ResponseEntity.accepted().build();
    }

    @AuditLoggable(entityType = CONFLUENCE_INTEGRATION, action = PENDING)
    @PostMapping("/{id}/toggle")
    public ResponseEntity<Boolean> toggleIntegration(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        try {
            boolean newStatus = confluenceIntegrationService.toggleActiveStatus(id, tenantId, userId);
            log.info("Confluence integration {} toggled to status: {}",
                    id, newStatus ? STATUS_ENABLED : STATUS_DISABLED);
            return ResponseEntity.ok(newStatus);
        } catch (SchedulingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to toggle integration active status due to scheduling error: " + e.getMessage(), e);
        }
    }

    @GetMapping("/normalized/names")
    public ResponseEntity<List<String>> getAllConfluenceNormalizedNamesByTenantId(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all Confluence integration normalized names for tenant: {}", tenantId);
        return ResponseEntity.ok(confluenceIntegrationService.getAllConfluenceNormalizedNamesByTenantId(tenantId));
    }

    @GetMapping("/connections/{connectionId}/spaces")
    public ResponseEntity<List<ConfluenceSpaceDto>> getSpacesByConnectionId(
            @PathVariable UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching Confluence spaces for connection ID: {} and tenant: {}", connectionId, tenantId);
        return ResponseEntity.ok(confluenceLookupService.getSpacesByConnectionId(connectionId, tenantId));
    }

    @GetMapping("/connections/{connectionId}/spaces/{spaceKey}/pages")
    public ResponseEntity<List<ConfluencePageDto>> getPagesByConnectionId(
            @PathVariable UUID connectionId,
            @PathVariable String spaceKey,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching Confluence pages for connection ID: {}, spaceKey: {}, tenant: {}",
                connectionId, spaceKey, tenantId);
        return ResponseEntity.ok(
                confluenceLookupService.getPagesByConnectionIdAndSpaceKey(connectionId, tenantId, spaceKey));
    }
}
