package com.integration.management.notification.service;

import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.config.authorization.ServiceTypeAuthorizationHelper;
import com.integration.management.notification.repository.NotificationEventCatalogRepository;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Service for initializing default notification rules for tenants based on their assigned features.
 * Default rules are created for all notification events matching the tenant's feature roles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDefaultRulesService {

    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationEventCatalogRepository eventCatalogRepository;
    private final NotificationRecipientPolicyRepository recipientPolicyRepository;
    private final NotificationRecipientUserRepository recipientUserRepository;
    private final ServiceTypeAuthorizationHelper serviceTypeAuth;

    @Transactional
    public int initializeDefaultRulesForTenant(String tenantId, String userId) {
        log.info("Initializing default notification rules for tenant: {} from user: {}", tenantId, userId);

        Set<NotificationEntityType> assignedEntityTypes = getAssignedEntityTypes();
        log.debug("Tenant: {} has assigned entity types: {}", tenantId, assignedEntityTypes);

        // Fetch all enabled events for the assigned entity types
        List<NotificationEventCatalog> applicableEvents = eventCatalogRepository
                .findByIsEnabledTrueAndEntityTypeIn(assignedEntityTypes);

        if (applicableEvents.isEmpty()) {
            log.info("No applicable notification events found for tenant: {} with entity types: {}",
                    tenantId, assignedEntityTypes);
            return 0;
        }

        // Create default rules and policies for all events in one batch
        createDefaultRulesWithPolicies(tenantId, userId, applicableEvents);

        log.info("Successfully initialized {} default notification rules for tenant: {}",
                applicableEvents.size(), tenantId);
        return applicableEvents.size();
    }

    @Transactional
    @CacheEvict(value = {"notificationRulesByTenantCache", "notificationPoliciesByTenantCache"},
            key = "#tenantId")
    public int resetRulesToDefaults(String tenantId, String userId) {
        log.info("Resetting notification rules to defaults for tenant: {} by user: {}", tenantId, userId);

        // delete existing rules and associated policies
        List<NotificationRule> existingRules = notificationRuleRepository
                .findByTenantIdOrderByLastModifiedDateDesc(tenantId);

        // Delete policies and users BEFORE rules to prevent Hibernate from nullifying
        // the rule_id FK (nullable=false) during rule deletion
        for (NotificationRule rule : existingRules) {
            recipientPolicyRepository.findByRuleId(rule.getId()).ifPresent(policy -> {
                // delete associated users first
                List<NotificationRecipientUser> users = recipientUserRepository.findByRecipientPolicyId(policy.getId());
                for (NotificationRecipientUser user : users) {
                    recipientUserRepository.delete(user);
                }
                recipientUserRepository.flush();
                recipientPolicyRepository.delete(policy);
            });
        }
        recipientPolicyRepository.flush();

        // Now safe to delete rules — no FK references remain
        for (NotificationRule rule : existingRules) {
            notificationRuleRepository.delete(rule);
        }
        notificationRuleRepository.flush();

        log.debug("Soft-deleted {} existing rules for tenant: {}", existingRules.size(), tenantId);

        // Re-initialize with defaults and return count of newly created rules
        return initializeDefaultRulesForTenant(tenantId, userId);
    }

    private Set<NotificationEntityType> getAssignedEntityTypes() {
        Set<NotificationEntityType> types = EnumSet.of(NotificationEntityType.SITE_CONFIG);

        boolean hasJira = serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA);
        if (hasJira) {
            types.add(NotificationEntityType.JIRA_WEBHOOK);
        }

        boolean hasArcGIS = serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS);
        if (hasArcGIS) {
            types.add(NotificationEntityType.ARCGIS_INTEGRATION);
        }

        boolean hasConfluence = serviceTypeAuth.hasAccessToServiceType(ServiceType.CONFLUENCE);
        if (hasConfluence) {
            types.add(NotificationEntityType.CONFLUENCE_INTEGRATION);
        }

        if (hasJira || hasArcGIS || hasConfluence) {
            types.add(NotificationEntityType.INTEGRATION_CONNECTION);
        }

        return types;
    }

    private void createDefaultRulesWithPolicies(String tenantId, String userId,
                                                List<NotificationEventCatalog> events) {
        List<NotificationRule> rules = events.stream()
                .map(event -> (NotificationRule) NotificationRule.builder()
                        .tenantId(tenantId)
                        .event(event)
                        .severity(getDefaultSeverityForEvent(event))
                        .isEnabled(true)
                        .createdBy(userId)
                        .lastModifiedBy(userId)
                        .build())
                .toList();
        List<NotificationRule> savedRules = notificationRuleRepository.saveAll(rules);
        log.debug("Created {} default rules for tenant: {}", savedRules.size(), tenantId);

        List<NotificationRecipientPolicy> policies = savedRules.stream()
                .map(savedRule -> (NotificationRecipientPolicy) NotificationRecipientPolicy.builder()
                        .tenantId(tenantId)
                        .rule(savedRule)
                        .recipientType(RecipientType.ALL_USERS)
                        .createdBy(userId)
                        .lastModifiedBy(userId)
                        .build())
                .toList();

        recipientPolicyRepository.saveAll(policies);
        log.debug("Created {} default ALL_USERS policies for tenant: {}", policies.size(), tenantId);
    }

    private NotificationSeverity getDefaultSeverityForEvent(NotificationEventCatalog event) {
        String key = event.getEventKey();
        if (key == null) {
            return NotificationSeverity.WARNING;
        }
        if (key.endsWith("_FAILED")) {
            return NotificationSeverity.ERROR;
        }
        if (key.endsWith("_COMPLETED") || key.endsWith("_ENABLED")) {
            return NotificationSeverity.SUCCESS;
        }
        if (key.endsWith("_DELETED") || key.endsWith("_DISABLED")
                || key.endsWith("_RETRIED") || key.endsWith("_ABORTED")
                || key.endsWith("_SECRET_UPDATED")) {
            return NotificationSeverity.WARNING;
        }
        return NotificationSeverity.INFO;
    }
}
