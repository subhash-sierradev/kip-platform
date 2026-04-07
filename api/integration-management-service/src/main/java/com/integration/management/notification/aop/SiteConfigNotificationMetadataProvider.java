package com.integration.management.notification.aop;

import com.integration.management.repository.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Resolves notification metadata for site config changes.
 * Fetches the {@code configKey} from the repository before the advised method proceeds
 * so the data is available even for delete operations where the row is removed.
 */
@Component("siteConfigNotificationMetadataProvider")
@RequiredArgsConstructor
public class SiteConfigNotificationMetadataProvider implements NotificationMetadataProvider {

    private final SiteConfigRepository siteConfigRepository;

    @Override
    public Map<String, Object> resolve(final Object entityId, final String tenantId) {
        return siteConfigRepository.findByTenant((UUID) entityId, tenantId)
                .map(sc -> Map.<String, Object>of("configKey", sc.getConfigKey()))
                .orElse(Map.of());
    }
}
