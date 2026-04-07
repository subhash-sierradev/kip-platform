package com.integration.management.notification.service;

import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.response.AppNotificationResponse;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.repository.AppNotificationRepository;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import com.integration.management.notification.sse.SseEmitterRegistry;
import com.integration.management.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDispatchService")
class NotificationDispatchServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String TRIGGER_USER = "user-initiator";
    private static final String EVENT_KEY = "SITE_CONFIG_UPDATED";

    @Mock private NotificationTemplateService notificationTemplateService;
    @Mock private NotificationRuleRepository notificationRuleRepository;
    @Mock private NotificationRecipientPolicyRepository recipientPolicyRepository;
    @Mock private NotificationRecipientUserRepository recipientUserRepository;
    @Mock private AppNotificationRepository appNotificationRepository;
    @Mock private UserProfileService userProfileService;
    @Mock private NotificationMapper notificationMapper;
    @Mock private SseEmitterRegistry sseEmitterRegistry;

    @InjectMocks
    private NotificationDispatchService notificationDispatchService;

    @Nested
    @DisplayName("dispatch - early exits")
    class DispatchEarlyExits {

        @Test
        @DisplayName("skips dispatch when no enabled rule found")
        void skips_when_no_rule_found() {
            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.empty());

            notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, TRIGGER_USER, Map.of());

            verify(recipientPolicyRepository, never()).findByRuleId(any());
            verify(appNotificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips dispatch when no recipient policy found")
        void skips_when_no_policy_found() {
            NotificationRule rule = buildRule(false);
            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.empty());

            notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, TRIGGER_USER, Map.of());

            verify(appNotificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips SSE when all target users are excluded as initiator")
        void skips_sse_when_all_targets_excluded_as_initiator() {
            NotificationRule rule = buildRule(false);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile(TRIGGER_USER, false)));

            notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, TRIGGER_USER, Map.of());

            verify(appNotificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("dispatch - ALL_USERS success")
    class DispatchAllUsersSuccess {

        @Test
        @DisplayName("persists notification and pushes SSE after commit for ALL_USERS")
        void persists_and_pushes_sse_for_all_users() {
            NotificationRule rule = buildRule(true);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());
            String targetUser = "user-target";

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile(targetUser, false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, null, Map.of());
                List<TransactionSynchronization> synchronizations =
                        TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);
                synchronizations.getFirst().afterCommit();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(sseEmitterRegistry).send(targetUser, response);
        }
    }

    @Nested
    @DisplayName("dispatch - ADMINS_ONLY filtering")
    class DispatchAdminsOnly {

        @Test
        @DisplayName("sends only to admin users for ADMINS_ONLY policy")
        void sends_only_to_admin_users() {
            NotificationRule rule = buildRule(true);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ADMINS_ONLY);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(
                            buildProfile("admin-user", true),
                            buildProfile("regular-user", false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, null, Map.of());
                List<TransactionSynchronization> synchronizations =
                        TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);
                synchronizations.getFirst().afterCommit();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(sseEmitterRegistry).send("admin-user", response);
            verify(sseEmitterRegistry, never()).send(eq("regular-user"), any());
        }
    }

    @Nested
    @DisplayName("dispatch - SELECTED_USERS")
    class DispatchSelectedUsers {

        @Test
        @DisplayName("sends to policy user list for SELECTED_USERS policy")
        void sends_to_selected_users() {
            NotificationRule rule = buildRule(true);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.SELECTED_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());
            String selectedUser = "user-selected";

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(recipientUserRepository.findByRecipientPolicyId(policy.getId()))
                    .thenReturn(List.of(buildRecipientUser(policy, selectedUser)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, null, Map.of());
                List<TransactionSynchronization> synchronizations =
                        TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);
                synchronizations.getFirst().afterCommit();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(sseEmitterRegistry).send(selectedUser, response);
            verify(userProfileService, never()).getAllUsersByTenant(any());
        }
    }

    @Nested
    @DisplayName("dispatch - initiator exclusion")
    class DispatchInitiatorExclusion {

        @Test
        @DisplayName("excludes initiator when notify_initiator is false")
        void excludes_initiator_when_notify_initiator_false() {
            NotificationRule rule = buildRule(false);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());
            String otherUser = "user-other";

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(
                            buildProfile(TRIGGER_USER, false),
                            buildProfile(otherUser, false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, TRIGGER_USER,
                        Map.of());
                List<TransactionSynchronization> synchronizations =
                        TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);
                synchronizations.getFirst().afterCommit();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(sseEmitterRegistry, never()).send(eq(TRIGGER_USER), any());
            verify(sseEmitterRegistry).send(otherUser, response);
        }

        @Test
        @DisplayName("includes initiator when notify_initiator is true")
        void includes_initiator_when_notify_initiator_true() {
            NotificationRule rule = buildRule(true);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile(TRIGGER_USER, false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, TRIGGER_USER,
                        Map.of());
                List<TransactionSynchronization> synchronizations =
                        TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);
                synchronizations.getFirst().afterCommit();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(sseEmitterRegistry).send(TRIGGER_USER, response);
        }
    }

    @Nested
    @DisplayName("dispatch - template rendering")
    class DispatchTemplateRendering {

        @Test
        @DisplayName("uses template title and message when template present")
        void uses_template_when_present() {
            NotificationRule rule = buildRule(true);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());
            NotificationTemplateResponse template = NotificationTemplateResponse.builder()
                    .eventKey(EVENT_KEY)
                    .titleTemplate("Config updated by {{updatedBy}}")
                    .messageTemplate("Changed: {{configSection}}")
                    .build();

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of(template));
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile("user-a", false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            ArgumentCaptor<AppNotification> notifCaptor =
                    ArgumentCaptor.forClass(AppNotification.class);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, TRIGGER_USER,
                        Map.of("configSection", "theme"));
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(appNotificationRepository).save(notifCaptor.capture());
            String title = notifCaptor.getValue().getTitle();
            assertThat(title).contains(TRIGGER_USER);
        }
    }

    @Nested
    @DisplayName("dispatch - unknown NotificationEventKey fallback")
    class DispatchUnknownNotificationEventKey {

        @Test
        @DisplayName("falls back to SITE_CONFIG_UPDATED when eventKey is not a NotificationEventKey")
        void unknownEventKey_fallsBackToSiteConfigUpdated() {
            String unknownKey = "SOME_NEW_EVENT_KEY";
            NotificationRule rule = buildRule(true);
            // Override the eventKey on the rule's event to match unknownKey
            rule.getEvent().setEventKey(unknownKey);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(unknownKey, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile("user-a", false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(unknownKey, TENANT_ID, null, Map.of());
                var syncs = TransactionSynchronizationManager.getSynchronizations();
                assertThat(syncs).hasSize(1);
                syncs.getFirst().afterCommit();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);
            verify(appNotificationRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationEventKey.SITE_CONFIG_UPDATED);
        }
    }

    @Nested
    @DisplayName("dispatch - metadata enrichment")
    class DispatchMetadataEnrichment {

        @Test
        @DisplayName("derives configSection from configKey and sets createdBy when eventKey ends with _CREATED")
        void metadataEnrichment_setsConfigSection_andByKey() {
            String eventKey = "ARCGIS_INTEGRATION_CREATED";
            NotificationRule rule = buildRule(true);
            rule.getEvent().setEventKey(eventKey);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);

            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(eventKey, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile("user-a", false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            Map<String, Object> metadata = Map.of("configKey", "featureFlagX");

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(eventKey, TENANT_ID, TRIGGER_USER, metadata);
                TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);
            verify(appNotificationRepository).save(captor.capture());
            assertThat(captor.getValue().getMetadata()).containsEntry("configSection", "featureFlagX");
            assertThat(captor.getValue().getMetadata()).containsEntry("triggeredBy", TRIGGER_USER);
            assertThat(captor.getValue().getMetadata()).containsEntry("createdBy", TRIGGER_USER);
        }
    }

    @Nested
    @DisplayName("enrichMetadata")
    class EnrichMetadataTests {

        @Test
        @DisplayName("copies configKey to configSection when configSection absent")
        void copies_config_key_to_config_section() {
            NotificationRule rule = buildRule(true);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(EVENT_KEY, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile("user-a", false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            ArgumentCaptor<AppNotification> notifCaptor =
                    ArgumentCaptor.forClass(AppNotification.class);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService.dispatch(EVENT_KEY, TENANT_ID, null,
                        Map.of("configKey", "branding"));
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(appNotificationRepository).save(notifCaptor.capture());
            assertThat(notifCaptor.getValue().getMetadata()).containsKey("configSection");
            assertThat(notifCaptor.getValue().getMetadata().get("configSection"))
                    .isEqualTo("branding");
        }

        @Test
        @DisplayName("adds createdBy key for _CREATED event keys")
        void adds_created_by_for_created_event_key() {
            String createdEventKey = "ARCGIS_INTEGRATION_CREATED";
            NotificationRule rule = buildRuleForEvent(createdEventKey, true);
            NotificationRecipientPolicy policy = buildPolicy(rule, RecipientType.ALL_USERS);
            AppNotification saved = buildNotification();
            AppNotificationResponse response = buildNotificationResponse(saved.getId());

            when(notificationRuleRepository
                    .findByEventEventKeyAndTenantIdAndIsEnabledTrue(createdEventKey, TENANT_ID))
                    .thenReturn(Optional.of(rule));
            when(recipientPolicyRepository.findByRuleId(rule.getId()))
                    .thenReturn(Optional.of(policy));
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenReturn(List.of(buildProfile(TRIGGER_USER, false)));
            when(appNotificationRepository.save(any())).thenReturn(saved);
            when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

            ArgumentCaptor<AppNotification> notifCaptor =
                    ArgumentCaptor.forClass(AppNotification.class);

            TransactionSynchronizationManager.initSynchronization();
            try {
                notificationDispatchService
                        .dispatch(createdEventKey, TENANT_ID, TRIGGER_USER, Map.of());
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(appNotificationRepository).save(notifCaptor.capture());
            assertThat(notifCaptor.getValue().getMetadata()).containsKey("createdBy");
        }
    }

    private NotificationRule buildRule(boolean notifyInitiator) {
        return buildRuleForEvent(EVENT_KEY, notifyInitiator);
    }

    private NotificationRule buildRuleForEvent(String eventKey, boolean notifyInitiator) {
        NotificationEventCatalog catalog = NotificationEventCatalog.builder()
                .eventKey(eventKey)
                .entityType(NotificationEntityType.SITE_CONFIG)
                .notifyInitiator(notifyInitiator)
                .isEnabled(true)
                .build();
        catalog.setId(UUID.randomUUID());

        NotificationRule rule = NotificationRule.builder()
                .event(catalog)
                .severity(NotificationSeverity.INFO)
                .isEnabled(true)
                .build();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(TENANT_ID);
        return rule;
    }

    private NotificationRecipientPolicy buildPolicy(NotificationRule rule, RecipientType type) {
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .rule(rule).recipientType(type).build();
        policy.setId(UUID.randomUUID());
        policy.setTenantId(TENANT_ID);
        return policy;
    }

    private NotificationRecipientUser buildRecipientUser(
            NotificationRecipientPolicy policy, String userId) {
        return NotificationRecipientUser.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID)
                .recipientPolicy(policy).userId(userId).build();
    }

    private AppNotification buildNotification() {
        AppNotification n = AppNotification.builder()
                .tenantId(TENANT_ID).userId("user-a")
                .type(NotificationEventKey.SITE_CONFIG_UPDATED)
                .severity(NotificationSeverity.INFO)
                .title("title").message("message")
                .isRead(false).build();
        n.setId(UUID.randomUUID());
        return n;
    }

    private AppNotificationResponse buildNotificationResponse(UUID id) {
        return AppNotificationResponse.builder()
                .id(id).tenantId(TENANT_ID).build();
    }

    private UserProfileResponse buildProfile(String userId, boolean isAdmin) {
        return UserProfileResponse.builder()
                .keycloakUserId(userId).isTenantAdmin(isAdmin)
                .email(userId + "@test.com").build();
    }
}
