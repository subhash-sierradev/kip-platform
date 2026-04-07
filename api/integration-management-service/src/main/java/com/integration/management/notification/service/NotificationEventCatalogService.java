package com.integration.management.notification.service;

import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.config.authorization.ServiceTypeAuthorizationHelper;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.response.NotificationEventCatalogResponse;
import com.integration.management.notification.repository.NotificationEventCatalogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventCatalogService {

    private final NotificationEventCatalogRepository eventCatalogRepository;
    private final NotificationMapper notificationMapper;
    private final ServiceTypeAuthorizationHelper serviceTypeAuth;

    /**
     * Resolves the set of NotificationEntityTypes the current authenticated user
     * is allowed to see, based on their Keycloak feature roles.
     * Mirrors the same role-to-service-type mapping used by ServiceTypeAuthorizationHelper.
     */
    private Set<NotificationEntityType> resolveActiveEntityTypes() {
        Set<NotificationEntityType> types = EnumSet.of(NotificationEntityType.SITE_CONFIG);
        boolean hasJira = serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA);
        boolean hasArcGIS = serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS);
        if (hasJira) {
            types.add(NotificationEntityType.JIRA_WEBHOOK);
        }
        if (hasArcGIS) {
            types.add(NotificationEntityType.ARCGIS_INTEGRATION);
        }
        if (hasJira || hasArcGIS) {
            types.add(NotificationEntityType.INTEGRATION_CONNECTION);
        }
        return types;
    }

    @Cacheable(value = "notificationEventsByTenantCache", key = "#tenantId")
    public List<NotificationEventCatalogResponse> getAllEnabled(String tenantId) {
        Set<NotificationEntityType> activeTypes = resolveActiveEntityTypes();
        log.debug("Fetching notification events for tenant: {} entity types: {}", tenantId, activeTypes);
        return eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(activeTypes)
            .stream()
            .map(notificationMapper::toEventCatalogResponse)
            .toList();
    }

    public NotificationEventCatalogResponse getById(UUID id) {
        log.debug("Fetching notification event catalog entry: {}", id);
        return eventCatalogRepository.findById(id)
            .map(notificationMapper::toEventCatalogResponse)
            .orElseThrow(() -> new EntityNotFoundException(
                "Notification event catalog entry not found: " + id));
    }
}
