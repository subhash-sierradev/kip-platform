package com.integration.management.notification.controller;

import com.integration.management.notification.model.dto.request.BatchCreateNotificationRuleRequest;
import com.integration.management.notification.model.dto.request.CreateNotificationRuleRequest;
import com.integration.management.notification.model.dto.request.UpdateNotificationRuleRequest;
import com.integration.management.notification.model.dto.response.NotificationRuleResponse;
import com.integration.management.notification.service.NotificationDefaultRulesService;
import com.integration.management.notification.service.NotificationRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;

@Slf4j
@RestController
@RequestMapping("/api/management/notifications/rules")
@RequiredArgsConstructor
public class NotificationRuleController {

    private final NotificationRuleService notificationRuleService;
    private final NotificationDefaultRulesService notificationDefaultRulesService;

    @GetMapping
    public ResponseEntity<List<NotificationRuleResponse>> getRules(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching notification rules for tenant: {}", tenantId);
        return ResponseEntity.ok(notificationRuleService.getRulesForTenant(tenantId));
    }

    @PostMapping
    public ResponseEntity<NotificationRuleResponse> create(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @Valid @RequestBody CreateNotificationRuleRequest request) {
        log.info("Creating notification rule for tenant: {}", tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(notificationRuleService.create(tenantId, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @PathVariable UUID id) {
        log.info("Deleting notification rule: {} for tenant: {}", id, tenantId);
        notificationRuleService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationRuleResponse> update(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNotificationRuleRequest request) {
        log.info("Updating notification rule: {} for tenant: {}", id, tenantId);
        return ResponseEntity.ok(notificationRuleService.updateRule(tenantId, userId, id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<NotificationRuleResponse> toggleEnabled(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @PathVariable UUID id) {
        log.info("Toggling enabled state for notification rule: {} for tenant: {}", id, tenantId);
        return ResponseEntity.ok(notificationRuleService.toggleEnabled(tenantId, id));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<NotificationRuleResponse>> createBatch(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @Valid @RequestBody BatchCreateNotificationRuleRequest request) {
        log.info("Creating notification rules in batch for tenant: {}", tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(notificationRuleService.createBatch(tenantId, userId, request));
    }

    @PostMapping("/reset-defaults")
    public ResponseEntity<Map<String, Object>> resetToDefaults(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        log.info("Resetting notification rules to defaults for tenant: {} by user: {}", tenantId, userId);
        int restoredCount = notificationDefaultRulesService.resetRulesToDefaults(tenantId, userId);
        return ResponseEntity.ok(Map.of(
            "message", "Notification rules reset to defaults successfully",
            "rulesRestored", restoredCount
        ));
    }
}
