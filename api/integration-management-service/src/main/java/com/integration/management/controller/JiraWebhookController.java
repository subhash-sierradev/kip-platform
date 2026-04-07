package com.integration.management.controller;

import com.integration.management.config.aspect.AuditLoggable;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.model.dto.request.JiraWebhookCreateUpdateRequest;
import com.integration.management.model.dto.response.JiraWebhookDetailResponse;
import com.integration.management.model.dto.response.JiraWebhookSummaryResponse;
import com.integration.execution.contract.rest.response.WebhookCreationResponse;
import com.integration.management.service.JiraWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

import static com.integration.management.constants.IntegrationManagementConstants.STATUS_DISABLED;
import static com.integration.management.constants.IntegrationManagementConstants.STATUS_ENABLED;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static com.integration.execution.contract.model.enums.AuditActivity.CREATE;
import static com.integration.execution.contract.model.enums.AuditActivity.DELETE;
import static com.integration.execution.contract.model.enums.AuditActivity.PENDING;
import static com.integration.execution.contract.model.enums.AuditActivity.UPDATE;
import static com.integration.execution.contract.model.enums.EntityType.JIRA_WEBHOOK;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/jira")
@PreAuthorize("hasRole('feature_jira_webhook')")
public class JiraWebhookController {

    private final JiraWebhookService jiraWebhookService;


    @AuditLoggable(entityType = JIRA_WEBHOOK, action = CREATE)
    @PostMapping
    public ResponseEntity<WebhookCreationResponse> create(
            @Valid @RequestBody JiraWebhookCreateUpdateRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId
    ) {
        log.info("Creating Jira webhook for tenant: {} by user: {}", tenantId, userId);
        JiraWebhook createdWebhook = jiraWebhookService.create(request, tenantId, userId);

        // Create minimal response with only essential fields for confirmation dialog
        WebhookCreationResponse response = WebhookCreationResponse.builder()
                .id(createdWebhook.getId())
                .name(createdWebhook.getName())
                .webhookUrl(createdWebhook.getWebhookUrl())
                .build();

        log.info("Successfully created Jira webhook '{}' with URL: {}",
                response.getName(), response.getWebhookUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{webhookId}")
    public ResponseEntity<JiraWebhookDetailResponse> getById(
            @PathVariable String webhookId,
            @RequestAttribute(X_TENANT_ID) String tenantId
    ) {
        if (webhookId == null || webhookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Jira webhook ID must not be null or empty");
        }

        try {
            JiraWebhookDetailResponse response = jiraWebhookService.getById(webhookId, tenantId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid webhookId format: {}", webhookId, e);
            throw new IllegalArgumentException("Invalid webhookId format: " + webhookId, e);
        }
    }

    @AuditLoggable(entityType = JIRA_WEBHOOK, action = UPDATE)
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable String id,
            @Valid @RequestBody JiraWebhookCreateUpdateRequest request,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId
    ) {
        jiraWebhookService.update(id, request, tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @AuditLoggable(entityType = JIRA_WEBHOOK, action = DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId
    ) {
        jiraWebhookService.delete(id, tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<JiraWebhookSummaryResponse>> getAll(
            @RequestAttribute(X_TENANT_ID) String tenantId
    ) {
        List<JiraWebhookSummaryResponse> responses = jiraWebhookService.getAllByTenantId(tenantId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/connections/{connectionId}")
    public ResponseEntity<List<JiraWebhookDetailResponse>> getByConnectionId(
            @PathVariable UUID connectionId
    ) {
        List<JiraWebhookDetailResponse> responses = jiraWebhookService.getByConnectionId(connectionId);
        return ResponseEntity.ok(responses);
    }

    @AuditLoggable(entityType = JIRA_WEBHOOK, action = PENDING)
    @PostMapping("/{id}/active")
    public ResponseEntity<Boolean> toggleWebhookActive(
            @PathVariable String id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId
    ) {
        // Get the new status from the service
        boolean newStatus = jiraWebhookService.toggleActiveStatus(id, tenantId, userId);
        log.info("Webhook {} toggled to status: {}", id, newStatus ? STATUS_ENABLED : STATUS_DISABLED);
        return ResponseEntity.ok(newStatus);
    }

    @GetMapping("/normalized/names")
    public ResponseEntity<List<String>> getAllNormalizedNamesByTenantId(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all Jira webhook normalized names for tenant: {}", tenantId);
        return ResponseEntity.ok(jiraWebhookService.getAllNormalizedNamesByTenantId(tenantId));
    }
}
