package com.integration.management.config.authorization;

import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.security.context.SecurityContextHelper;
import com.integration.management.service.IntegrationConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.ROLE_FEATURE_ARCGIS_INTEGRATION;
import static com.integration.management.constants.ManagementSecurityConstants.ROLE_FEATURE_CONFLUENCE_INTEGRATION;
import static com.integration.management.constants.ManagementSecurityConstants.ROLE_FEATURE_JIRA_WEBHOOK;

/**
 * Authorization helper component for service-type specific access control.
 * Ensures users can only access connections and resources for their authorized service types.
 */
@Slf4j
@Component("serviceTypeAuth")
@RequiredArgsConstructor
public class ServiceTypeAuthorizationHelper {

    @Lazy
    private final IntegrationConnectionService integrationConnectionService;

    public boolean hasAccessToServiceType(ServiceType serviceType) {
        if (serviceType == null) {
            log.warn("ServiceType is null, denying access");
            return false;
        }

        boolean hasAccess = switch (serviceType) {
            case JIRA -> SecurityContextHelper.hasRole(ROLE_FEATURE_JIRA_WEBHOOK);
            case ARCGIS -> SecurityContextHelper.hasRole(ROLE_FEATURE_ARCGIS_INTEGRATION);
            case CONFLUENCE -> SecurityContextHelper.hasRole(ROLE_FEATURE_CONFLUENCE_INTEGRATION);
        };

        log.debug("Access check for ServiceType {}: {}", serviceType, hasAccess);
        return hasAccess;
    }

    /**
     * Used in Spring Security SpEL expressions via @PreAuthorize.
     * Do NOT remove even if IDE shows no usages.
     */
    public boolean hasAccessToConnection(UUID connectionId, String tenantId) {
        if (connectionId == null || tenantId == null) {
            log.warn("Connection ID or tenant ID is null, denying access");
            return false;
        }
        try {
            IntegrationConnectionResponse connection =
                    integrationConnectionService.getConnectionById(connectionId, tenantId);
            return hasAccessToServiceType(connection.getServiceType());
        } catch (Exception e) {
            log.warn("Connection not found or not accessible for id: {}, tenant: {}", connectionId, tenantId);
            return false;
        }
    }
}
