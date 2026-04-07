package com.integration.management.notification.aop;

import com.integration.management.repository.JiraWebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves notification metadata for Jira webhooks.
 * Fetches the webhook name from the repository before the advised method proceeds
 * so the entity is still present in the database (used for delete/toggle scenarios).
 */
@Component("jiraWebhookNotificationMetadataProvider")
@RequiredArgsConstructor
public class JiraWebhookNotificationMetadataProvider implements NotificationMetadataProvider {

    private final JiraWebhookRepository jiraWebhookRepository;

    @Override
    public Map<String, Object> resolve(final Object entityId, final String tenantId) {
        return jiraWebhookRepository
                .findByIdAndTenantIdAndIsDeletedFalse((String) entityId, tenantId)
                .map(w -> Map.<String, Object>of(
                        "webhookName", w.getName() != null ? w.getName() : "",
                        "webhookId", w.getId() != null ? w.getId() : ""))
                .orElse(Map.of());
    }
}
