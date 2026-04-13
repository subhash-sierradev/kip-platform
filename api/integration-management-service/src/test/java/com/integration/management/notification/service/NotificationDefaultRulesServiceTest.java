package com.integration.management.notification.service;

import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.config.authorization.ServiceTypeAuthorizationHelper;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.repository.NotificationEventCatalogRepository;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDefaultRulesService")
class NotificationDefaultRulesServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "user-1";

    @Mock private NotificationRuleRepository notificationRuleRepository;
    @Mock private NotificationEventCatalogRepository eventCatalogRepository;
    @Mock private NotificationRecipientPolicyRepository recipientPolicyRepository;
    @Mock private NotificationRecipientUserRepository recipientUserRepository;
    @Mock private ServiceTypeAuthorizationHelper serviceTypeAuth;

    @InjectMocks
    private NotificationDefaultRulesService notificationDefaultRulesService;

    @Nested
    @DisplayName("initializeDefaultRulesForTenant")
    class InitializeDefaultRulesForTenant {

        @Test
        @DisplayName("creates rules and ALL_USERS policies for each applicable event")
        void creates_rules_and_policies_for_applicable_events() {
            NotificationEventCatalog event = buildEvent("SITE_CONFIG_UPDATED",
                    NotificationEntityType.SITE_CONFIG);

            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(false);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of(event));
            when(notificationRuleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recipientPolicyRepository.saveAll(any())).thenReturn(List.of());

            int result = notificationDefaultRulesService
                    .initializeDefaultRulesForTenant(TENANT_ID, USER_ID);

            assertThat(result).isEqualTo(1);
            verify(notificationRuleRepository).saveAll(any());
            verify(recipientPolicyRepository).saveAll(any());
        }

        @Test
        @DisplayName("returns zero when no applicable events are found")
        void returns_zero_when_no_events() {
            when(serviceTypeAuth.hasAccessToServiceType(any())).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of());

            int result = notificationDefaultRulesService
                    .initializeDefaultRulesForTenant(TENANT_ID, USER_ID);

            assertThat(result).isZero();
            verify(notificationRuleRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("includes JIRA entity types when user has Jira role")
        void includes_jira_types_when_has_jira_role() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(true);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of());

            notificationDefaultRulesService.initializeDefaultRulesForTenant(TENANT_ID, USER_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<java.util.Set<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(java.util.Set.class);
            verify(eventCatalogRepository).findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(captor.capture());
            assertThat(captor.getValue()).contains(
                    NotificationEntityType.JIRA_WEBHOOK,
                    NotificationEntityType.INTEGRATION_CONNECTION);
        }

        @Test
        @DisplayName("includes CONFLUENCE_INTEGRATION and INTEGRATION_CONNECTION when user has Confluence role")
        void includes_confluence_type_and_connection_when_has_confluence_role() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(false);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(false);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.CONFLUENCE)).thenReturn(true);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of());

            notificationDefaultRulesService.initializeDefaultRulesForTenant(TENANT_ID, USER_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<java.util.Set<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(java.util.Set.class);
            verify(eventCatalogRepository).findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(captor.capture());
            assertThat(captor.getValue())
                    .contains(
                            NotificationEntityType.CONFLUENCE_INTEGRATION,
                            NotificationEntityType.INTEGRATION_CONNECTION)
                    .doesNotContain(
                            NotificationEntityType.JIRA_WEBHOOK,
                            NotificationEntityType.ARCGIS_INTEGRATION);
        }

        @Test
        @DisplayName("creates policies with ALL_USERS recipient type")
        void creates_all_users_policies() {
            NotificationEventCatalog event = buildEvent("SITE_CONFIG_UPDATED",
                    NotificationEntityType.SITE_CONFIG);
            when(serviceTypeAuth.hasAccessToServiceType(any())).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of(event));
            when(notificationRuleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationRecipientPolicy>> captor =
                    ArgumentCaptor.forClass(List.class);
            when(recipientPolicyRepository.saveAll(captor.capture())).thenReturn(List.of());

            notificationDefaultRulesService.initializeDefaultRulesForTenant(TENANT_ID, USER_ID);

            assertThat(captor.getValue()).allMatch(
                    p -> RecipientType.ALL_USERS.equals(p.getRecipientType()));
        }
    }

    @Nested
    @DisplayName("resetRulesToDefaults")
    class ResetRulesToDefaults {

        @Test
        @DisplayName("deletes existing rules in cascade order then re-initializes")
        void deletes_existing_rules_in_cascade_order_then_reinitializes() {
            NotificationRule rule = buildRule();
            NotificationRecipientPolicy policy = buildPolicy(rule);
            NotificationRecipientUser user = buildUser(policy);

            when(notificationRuleRepository
                    .findByTenantIdOrderByLastModifiedDateDesc(TENANT_ID))
                    .thenReturn(List.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(List.of(user));
            when(serviceTypeAuth.hasAccessToServiceType(any())).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of());

            notificationDefaultRulesService.resetRulesToDefaults(TENANT_ID, USER_ID);

            var order = inOrder(
                    recipientUserRepository, recipientPolicyRepository, notificationRuleRepository);
            order.verify(recipientUserRepository).delete(user);
            order.verify(recipientUserRepository).flush();
            order.verify(recipientPolicyRepository).delete(policy);
            order.verify(recipientPolicyRepository).flush();
            order.verify(notificationRuleRepository, atLeastOnce()).delete(rule);
            order.verify(notificationRuleRepository).flush();
        }

        @Test
        @DisplayName("re-initializes after deletion")
        void reinitializes_after_deletion() {
            when(notificationRuleRepository
                    .findByTenantIdOrderByLastModifiedDateDesc(TENANT_ID))
                    .thenReturn(List.of());
            when(serviceTypeAuth.hasAccessToServiceType(any())).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of());

            int result = notificationDefaultRulesService
                    .resetRulesToDefaults(TENANT_ID, USER_ID);

            assertThat(result).isZero();
            verify(eventCatalogRepository).findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any());
        }
    }

    @Nested
    @DisplayName("getDefaultSeverityForEvent (via initializeDefaultRulesForTenant)")
    class GetDefaultSeverityForEvent {

        @Test
        @DisplayName("_FAILED suffix maps to ERROR severity")
        void failed_suffix_maps_to_error() {
            verifySeverityForKey("ARCGIS_JOB_FAILED", NotificationSeverity.ERROR);
        }

        @Test
        @DisplayName("_COMPLETED suffix maps to SUCCESS severity")
        void completed_suffix_maps_to_success() {
            verifySeverityForKey("ARCGIS_JOB_COMPLETED", NotificationSeverity.SUCCESS);
        }

        @Test
        @DisplayName("_ENABLED suffix maps to SUCCESS severity")
        void enabled_suffix_maps_to_success() {
            verifySeverityForKey("ARCGIS_INTEGRATION_ENABLED", NotificationSeverity.SUCCESS);
        }

        @Test
        @DisplayName("_DELETED suffix maps to WARNING severity")
        void deleted_suffix_maps_to_warning() {
            verifySeverityForKey("ARCGIS_INTEGRATION_DELETED", NotificationSeverity.WARNING);
        }

        @Test
        @DisplayName("_DISABLED suffix maps to WARNING severity")
        void disabled_suffix_maps_to_warning() {
            verifySeverityForKey("ARCGIS_INTEGRATION_DISABLED", NotificationSeverity.WARNING);
        }

        @Test
        @DisplayName("_SECRET_UPDATED suffix maps to WARNING severity")
        void secret_updated_suffix_maps_to_warning() {
            verifySeverityForKey("CONNECTION_SECRET_UPDATED", NotificationSeverity.WARNING);
        }

        @Test
        @DisplayName("unknown suffix maps to INFO severity")
        void unknown_suffix_maps_to_info() {
            verifySeverityForKey("ARCGIS_INTEGRATION_CREATED", NotificationSeverity.INFO);
        }

        @Test
        @DisplayName("null event key maps to WARNING severity")
        void null_key_maps_to_warning() {
            verifySeverityForKey(null, NotificationSeverity.WARNING);
        }

        @Test
        @DisplayName("_RETRIED suffix maps to WARNING severity")
        void retried_suffix_maps_to_warning() {
            verifySeverityForKey("ARCGIS_JOB_RETRIED", NotificationSeverity.WARNING);
        }

        @Test
        @DisplayName("_ABORTED suffix maps to WARNING severity")
        void aborted_suffix_maps_to_warning() {
            verifySeverityForKey("ARCGIS_JOB_ABORTED", NotificationSeverity.WARNING);
        }

        @SuppressWarnings("unchecked")
        private void verifySeverityForKey(String eventKey, NotificationSeverity expectedSeverity) {
            NotificationEventCatalog event = buildEvent(eventKey,
                    NotificationEntityType.SITE_CONFIG);
            when(serviceTypeAuth.hasAccessToServiceType(any())).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(any()))
                    .thenReturn(List.of(event));

            ArgumentCaptor<List<NotificationRule>> ruleCaptor =
                    ArgumentCaptor.forClass(List.class);
            when(notificationRuleRepository.saveAll(ruleCaptor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(recipientPolicyRepository.saveAll(any())).thenReturn(List.of());

            notificationDefaultRulesService.initializeDefaultRulesForTenant(TENANT_ID, USER_ID);

            assertThat(ruleCaptor.getValue()).allMatch(
                    r -> expectedSeverity.equals(r.getSeverity()));
        }
    }

    private NotificationEventCatalog buildEvent(String eventKey, NotificationEntityType type) {
        return NotificationEventCatalog.builder()
                .id(UUID.randomUUID()).eventKey(eventKey)
                .entityType(type).isEnabled(true).build();
    }

    private NotificationRule buildRule() {
        NotificationRule rule = NotificationRule.builder()
                .severity(NotificationSeverity.INFO).isEnabled(true).build();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(TENANT_ID);
        return rule;
    }

    private NotificationRecipientPolicy buildPolicy(NotificationRule rule) {
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .rule(rule).recipientType(RecipientType.ALL_USERS).build();
        policy.setId(UUID.randomUUID());
        policy.setTenantId(TENANT_ID);
        return policy;
    }

    private NotificationRecipientUser buildUser(NotificationRecipientPolicy policy) {
        return NotificationRecipientUser.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID)
                .recipientPolicy(policy).userId(USER_ID).build();
    }
}
