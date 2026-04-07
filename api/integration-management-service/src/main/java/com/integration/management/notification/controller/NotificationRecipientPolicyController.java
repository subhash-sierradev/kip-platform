package com.integration.management.notification.controller;

import com.integration.management.notification.model.dto.request.CreateRecipientPolicyRequest;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import com.integration.management.notification.service.NotificationRecipientPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;

@Slf4j
@RestController
@RequestMapping("/api/management/notifications/policies")
@RequiredArgsConstructor
public class NotificationRecipientPolicyController {

    private final NotificationRecipientPolicyService recipientPolicyService;

    @GetMapping
    public ResponseEntity<List<RecipientPolicyResponse>> getPolicies(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching recipient policies for tenant: {}", tenantId);
        return ResponseEntity.ok(recipientPolicyService.getPoliciesForTenant(tenantId));
    }

    @PostMapping
    public ResponseEntity<RecipientPolicyResponse> create(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @Valid @RequestBody CreateRecipientPolicyRequest request) {
        log.info("Creating recipient policy for tenant: {}", tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(recipientPolicyService.create(tenantId, userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipientPolicyResponse> update(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateRecipientPolicyRequest request) {
        log.info("Updating recipient policy: {} for tenant: {}", id, tenantId);
        return ResponseEntity.ok(recipientPolicyService.update(tenantId, userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @PathVariable UUID id) {
        log.info("Deleting recipient policy: {} for tenant: {}", id, tenantId);
        recipientPolicyService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
