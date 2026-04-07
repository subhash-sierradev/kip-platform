package com.integration.management.notification.aop;

import com.integration.management.repository.ConfluenceIntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Resolves notification metadata for Confluence integrations.
 * Fetches the integration name from the repository before the advised method proceeds
 * so the entity is still present in the database (used for soft-delete scenarios).
 */
@Component("confluenceNotificationMetadataProvider")
@RequiredArgsConstructor
public class ConfluenceNotificationMetadataProvider implements NotificationMetadataProvider {

    private final ConfluenceIntegrationRepository confluenceIntegrationRepository;

    @Override
    public Map<String, Object> resolve(final Object entityId, final String tenantId) {
        return confluenceIntegrationRepository
                .findByIdAndTenantIdAndIsDeletedFalse((UUID) entityId, tenantId)
                .map(i -> Map.<String, Object>of(
                        "integrationName", i.getName() != null ? i.getName() : "",
                        "integrationId", i.getId().toString()))
                .orElse(Map.of());
    }
}
