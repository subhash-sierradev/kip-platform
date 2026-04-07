package com.integration.management.notification.aop;

import com.integration.management.repository.ArcGISIntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Resolves notification metadata for ArcGIS integrations.
 * Fetches the integration name from the repository before the advised method proceeds
 * so the entity is still present in the database (used for soft-delete scenarios).
 */
@Component("arcGISNotificationMetadataProvider")
@RequiredArgsConstructor
public class ArcGISNotificationMetadataProvider implements NotificationMetadataProvider {

    private final ArcGISIntegrationRepository arcGISIntegrationRepository;

    @Override
    public Map<String, Object> resolve(final Object entityId, final String tenantId) {
        return arcGISIntegrationRepository
                .findByIdAndTenantIdAndIsDeletedFalse((UUID) entityId, tenantId)
                .map(i -> Map.<String, Object>of(
                        "integrationName", i.getName() != null ? i.getName() : "",
                        "integrationId", i.getId().toString()))
                .orElse(Map.of());
    }
}
