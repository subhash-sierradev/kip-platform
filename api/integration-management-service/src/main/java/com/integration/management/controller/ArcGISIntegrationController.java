package com.integration.management.controller;

import com.integration.management.config.aspect.AuditLoggable;
import com.integration.management.exception.SchedulingException;
import com.integration.management.model.dto.request.ArcGISIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.response.ArcGISIntegrationResponse;
import com.integration.management.model.dto.response.ArcGISIntegrationSummaryResponse;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import com.integration.management.service.ArcGISIntegrationService;
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

import static com.integration.management.constants.IntegrationManagementConstants.STATUS_DISABLED;
import static com.integration.management.constants.IntegrationManagementConstants.STATUS_ENABLED;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static com.integration.execution.contract.model.enums.AuditActivity.CREATE;
import static com.integration.execution.contract.model.enums.AuditActivity.DELETE;
import static com.integration.execution.contract.model.enums.AuditActivity.PENDING;
import static com.integration.execution.contract.model.enums.AuditActivity.RUN_NOW;
import static com.integration.execution.contract.model.enums.AuditActivity.UPDATE;
import static com.integration.execution.contract.model.enums.EntityType.ARCGIS_INTEGRATION;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/arcgis")
@PreAuthorize("hasRole('feature_arcgis_integration')")
public class ArcGISIntegrationController {

    private final ArcGISIntegrationService arcGISIntegrationService;

    @AuditLoggable(entityType = ARCGIS_INTEGRATION, action = CREATE)
    @PostMapping
    public ResponseEntity<CreationResponse> create(
            @Valid @RequestBody ArcGISIntegrationCreateUpdateRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Creating ArcGIS integration: {} for tenant: {}", request.getName(), tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(arcGISIntegrationService.create(request, tenantId, userId));
    }

    @AuditLoggable(entityType = ARCGIS_INTEGRATION, action = UPDATE)
    @PutMapping("/{id}")
    public ResponseEntity<CreationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ArcGISIntegrationCreateUpdateRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Updating ArcGIS integration: {} (ID: {}) for tenant: {}", request.getName(), id, tenantId);
        try {
            return ResponseEntity.ok(arcGISIntegrationService.update(id, request, tenantId, userId));
        } catch (com.integration.management.exception.SchedulingException e) {
            // Provide a clear client-facing error when scheduling fails (e.g., invalid
            // cron)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to update integration schedule due to scheduling error: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public ResponseEntity<List<ArcGISIntegrationSummaryResponse>> getAllByTenant(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all ArcGIS integrations for tenant: {}", tenantId);
        return ResponseEntity.ok(arcGISIntegrationService.getAllByTenant(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArcGISIntegrationResponse> getById(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching ArcGIS integration with ID: {} for tenant: {}", id, tenantId);
        return ResponseEntity.ok(arcGISIntegrationService.getByIdAndTenantWithDetails(id, tenantId));
    }

    @GetMapping("/{id}/field-mappings")
    public ResponseEntity<List<IntegrationFieldMappingDto>> getFieldMappings(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching field mappings for ArcGIS integration with ID: {} for tenant: {}", id, tenantId);
        return ResponseEntity.ok(arcGISIntegrationService.getFieldMappings(id, tenantId));
    }

    @AuditLoggable(entityType = ARCGIS_INTEGRATION, action = DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Deleting ArcGIS integration with ID: {} for tenant: {}", id, tenantId);
        arcGISIntegrationService.delete(id, tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<IntegrationJobExecutionDto>> getJobHistory(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching job execution history for ArcGIS integration with "
                + "ID: {} for tenant: {}", id, tenantId);
        return ResponseEntity.ok(arcGISIntegrationService.getJobHistory(id, tenantId));
    }

    @AuditLoggable(entityType = ARCGIS_INTEGRATION, action = RUN_NOW)
    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerJobExecution(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Triggering manual job execution for ArcGIS integration with ID: {} for tenant: {} by user: {}",
                id, tenantId, userId);
        arcGISIntegrationService.triggerJobExecution(id, tenantId, userId);
        return ResponseEntity.ok().build();
    }

    @AuditLoggable(entityType = ARCGIS_INTEGRATION, action = PENDING)
    @PostMapping("/{id}/toggle")
    public ResponseEntity<Boolean> toggleIntegration(
            @PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        try {
            boolean newStatus = arcGISIntegrationService.toggleActiveStatus(id, tenantId, userId);
            log.info("ArcGIS integration {} toggled to status: {}", id, newStatus ? STATUS_ENABLED : STATUS_DISABLED);
            return ResponseEntity.ok(newStatus);
        } catch (SchedulingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to toggle integration active status due to scheduling error: " + e.getMessage(), e);
        }
    }

    @GetMapping("/connections/{connectionId}/features")
    public ResponseEntity<List<ArcGISFieldDto>> fetchArcGISFields(
            @PathVariable("connectionId") UUID connectionId,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching ArcGIS fields for connectionId: {} and tenantId: {}", connectionId, tenantId);
        return ResponseEntity.ok(arcGISIntegrationService.fetchArcGISFields(connectionId, tenantId));
    }

    @GetMapping("/normalized/names")
    public ResponseEntity<List<String>> getAllArcGISNormalizedNamesByTenantId(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all ArcGIS integrations normalized names for tenant: {}", tenantId);
        return ResponseEntity.ok(arcGISIntegrationService.getAllArcGISNormalizedNamesByTenantId(tenantId));
    }

}
