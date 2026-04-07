package com.integration.management.controller;

import com.integration.management.config.aspect.AuditLoggable;
import com.integration.execution.contract.model.SiteConfigDto;
import com.integration.management.model.dto.response.AuditLogResponse;
import com.integration.execution.contract.model.enums.AuditActivity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.integration.management.service.SettingsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.CacheConstants.ALL_CACHE;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static com.integration.execution.contract.model.enums.EntityType.CACHE;
import static com.integration.execution.contract.model.enums.EntityType.SITE_CONFIG;

@Slf4j
@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
public class SettingsController {
    private final SettingsService settingsService;

    @AuditLoggable(entityType = CACHE, action = AuditActivity.CLEAR_CACHE, entityIdValue = ALL_CACHE)
    @DeleteMapping("/caches")
    public ResponseEntity<?> clearAllCaches() {
        settingsService.clearAllCaches();
        return ResponseEntity.noContent().build();
    }

    @AuditLoggable(entityType = CACHE, action = AuditActivity.CLEAR_CACHE, entityIdParam = "cacheName")
    @DeleteMapping("/caches/{cacheName}")
    public ResponseEntity<?> clearCacheByName(@PathVariable String cacheName) {
        settingsService.clearCacheByName(cacheName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/caches/stats")
    public ResponseEntity<Map<String, Object>> getAllCacheStats() {
        return ResponseEntity.ok(settingsService.getCacheStatsForAllCaches());
    }

    @GetMapping("/caches/stats/{cacheName}")
    public ResponseEntity<Object> getCacheStats(@PathVariable String cacheName) {
        Object stats = settingsService.getCacheStats(cacheName);
        if (stats == null) {
            return ResponseEntity.status(404).body("Cache '" + cacheName + "' not found.");
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/audits/logs")
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs(@RequestAttribute(X_TENANT_ID) String tenantId) {
        return ResponseEntity.ok(settingsService.getAllAuditLogs(tenantId));
    }

    @GetMapping("/site-configs")
    public ResponseEntity<List<SiteConfigDto>> getAllSiteConfigs(@RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all site configs for tenant: {}", tenantId);
        return ResponseEntity.ok(settingsService.getAllSiteConfigs(tenantId));
    }

    @GetMapping("/site-configs/{id}")
    public ResponseEntity<SiteConfigDto> getSiteConfigs(@PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        return ResponseEntity.ok(settingsService.getSiteConfig(id, tenantId));
    }

    @AuditLoggable(entityType = SITE_CONFIG, action = AuditActivity.UPDATE)
    @PutMapping("/site-configs/{id}")
    public ResponseEntity<SiteConfigDto> updateSiteConfig(@PathVariable UUID id,
            @Valid @RequestBody SiteConfigDto request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        return ResponseEntity.ok(settingsService.updateSiteConfig(id, request, tenantId, userId));
    }

    @AuditLoggable(entityType = SITE_CONFIG, action = AuditActivity.DELETE)
    @DeleteMapping("/site-configs/{id}")
    public ResponseEntity<Void> deleteSiteConfig(@PathVariable UUID id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        settingsService.deleteSiteConfig(id, tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
