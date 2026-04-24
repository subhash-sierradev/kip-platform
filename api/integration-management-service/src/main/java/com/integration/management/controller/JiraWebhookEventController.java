package com.integration.management.controller;

import com.integration.management.config.aspect.AuditLoggable;
import com.integration.management.model.dto.response.JiraWebhookEventResponse;
import com.integration.management.service.JiraWebhookEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static com.integration.execution.contract.model.enums.AuditActivity.EXECUTE;
import static com.integration.execution.contract.model.enums.AuditActivity.RETRY;
import static com.integration.execution.contract.model.enums.EntityType.JIRA_WEBHOOK;
import static com.integration.execution.contract.model.enums.EntityType.JIRA_WEBHOOK_EVENT;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/jira")
@PreAuthorize("hasRole('feature_jira_webhook')")
@RequiredArgsConstructor
public class JiraWebhookEventController {

    private final JiraWebhookEventService jiraWebhookEventService;

    @GetMapping("/triggers/{webhookId}")
    public ResponseEntity<List<JiraWebhookEventResponse>> getTriggerHistory(
            @PathVariable String webhookId,
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        return ResponseEntity.ok(jiraWebhookEventService.getWebhookEventsByWebhookId(webhookId, tenantId));
    }

    @PreAuthorize("hasRole('webhook_client')")
    @AuditLoggable(entityType = JIRA_WEBHOOK_EVENT, action = RETRY)
    @PostMapping("/triggers/retry/{id}")
    public ResponseEntity<JiraWebhookEventResponse> retryTrigger(
            @PathVariable String id,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        return jiraWebhookEventService.retryTrigger(id, tenantId, userId);
    }

    @PreAuthorize("hasRole('webhook_client')")
    @AuditLoggable(entityType = JIRA_WEBHOOK, action = EXECUTE)
    @PostMapping("/execute/{id}")
    public ResponseEntity<JiraWebhookEventResponse> executeWebhook(
            @PathVariable String id,
            @RequestBody String jiraWebhookPayload,
            @RequestAttribute(X_TENANT_ID) String tenantId,
            @RequestAttribute(X_USER_ID) String userId) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("Webhook ID must not be null or empty");
        }
        return jiraWebhookEventService.executeWebhook(id, jiraWebhookPayload, tenantId, userId);
    }
}
