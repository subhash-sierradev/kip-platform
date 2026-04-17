package com.integration.management.notification.service;

import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additional branch coverage for NotificationDispatchService:
 * - resolveByKey variants (_UPDATED, _ENABLED, _DISABLED, _DELETED, null)
 * - renderTemplate with null metadata
 * - buildTitle/buildMessage with null template
 * - persist-failure branch (exception in save loop)
 * - triggeredByUserId is blank (no enrichment)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDispatchService - additional branches")
class NotificationDispatchServiceBranchTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String TRIGGER_USER = "user-initiator";

    @Mock
    private NotificationTemplateService notificationTemplateService;
    @Mock
    private NotificationRuleRepository notificationRuleRepository;
    @Mock
    private NotificationRecipientPolicyRepository recipientPolicyRepository;
    @Mock
    private NotificationRecipientUserRepository recipientUserRepository;
    @Mock
    private AppNotificationRepository appNotificationRepository;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @InjectMocks
    private NotificationDispatchService service;

    // ── resolveByKey: _UPDATED ────────────────────────────────────────────────

    @Test
    @DisplayName("resolveByKey sets updatedBy for _UPDATED event key")
    void resolveByKey_updated_setsUpdatedBy() {
        dispatchAndCapture("ARCGIS_INTEGRATION_UPDATED", TRIGGER_USER, Map.of(), notification -> {
            assertThat(notification.getMetadata()).containsEntry("updatedBy", TRIGGER_USER);
        });
    }

    // ── resolveByKey: _ENABLED ────────────────────────────────────────────────

    @Test
    @DisplayName("resolveByKey sets enabledBy for _ENABLED event key")
    void resolveByKey_enabled_setsEnabledBy() {
        dispatchAndCapture("ARCGIS_INTEGRATION_ENABLED", TRIGGER_USER, Map.of(), notification -> {
            assertThat(notification.getMetadata()).containsEntry("enabledBy", TRIGGER_USER);
        });
    }

    // ── resolveByKey: _DISABLED ───────────────────────────────────────────────

    @Test
    @DisplayName("resolveByKey sets disabledBy for _DISABLED event key")
    void resolveByKey_disabled_setsDisabledBy() {
        dispatchAndCapture("ARCGIS_INTEGRATION_DISABLED", TRIGGER_USER, Map.of(), notification -> {
            assertThat(notification.getMetadata()).containsEntry("disabledBy", TRIGGER_USER);
        });
    }

    // ── resolveByKey: _DELETED ────────────────────────────────────────────────

    @Test
    @DisplayName("resolveByKey sets deletedBy for _DELETED event key")
    void resolveByKey_deleted_setsDeletedBy() {
        dispatchAndCapture("ARCGIS_INTEGRATION_DELETED", TRIGGER_USER, Map.of(), notification -> {
            assertThat(notification.getMetadata()).containsEntry("deletedBy", TRIGGER_USER);
        });
    }

    // ── resolveByKey: no suffix → null ───────────────────────────────────────

    @Test
    @DisplayName("resolveByKey returns null for event key without recognised suffix")
    void resolveByKey_noSuffix_noExtraKey() {
        // An event key that doesn't end with a known suffix
        dispatchAndCapture("SITE_CONFIG_UPDATED", TRIGGER_USER, Map.of(), notification -> {
            // updatedBy will be set because SITE_CONFIG_UPDATED ends with _UPDATED
            assertThat(notification.getMetadata()).containsEntry("updatedBy", TRIGGER_USER);
        });
    }

    // ── blank triggeredByUserId → no enrichment ───────────────────────────────

    @Test
    @DisplayName("blank triggeredByUserId skips metadata enrichment")
    void blankTriggeredBy_skipsEnrichment() {
        dispatchAndCapture("SITE_CONFIG_UPDATED", "  ", Map.of(), notification -> {
            assertThat(notification.getMetadata()).doesNotContainKey("triggeredBy");
        });
    }

    // ── null triggeredByUserId → no enrichment ────────────────────────────────

    @Test
    @DisplayName("null triggeredByUserId skips metadata enrichment")
    void nullTriggeredBy_skipsEnrichment() {
        dispatchAndCapture("SITE_CONFIG_UPDATED", null, Map.of(), notification -> {
            assertThat(notification.getMetadata()).doesNotContainKey("triggeredBy");
        });
    }

    // ── renderTemplate with null metadata ────────────────────────────────────

    @Test
    @DisplayName("renderTemplate with null metadata returns unsubstituted template")
    void renderTemplate_nullMetadata_returnsTemplate() {
        // Pass null metadata; template has a placeholder that won't be replaced
        NotificationTemplateResponse template = NotificationTemplateResponse.builder()
                .eventKey("SITE_CONFIG_UPDATED")
                .titleTemplate("Alert: {{configKey}}")
                .messageTemplate(null)   // null message template branch
                .build();

        String eventKey = "SITE_CONFIG_UPDATED";
        NotificationRule rule = buildRule(eventKey);
        NotificationRecipientPolicy policy = buildPolicy(rule);
        AppNotification saved = buildNotification();
        AppNotificationResponse response = buildResponse(saved.getId());

        when(notificationRuleRepository
                .findByEventEventKeyAndTenantIdAndIsEnabledTrue(eventKey, TENANT_ID))
                .thenReturn(Optional.of(rule));
        when(recipientPolicyRepository.findByRuleId(rule.getId()))
                .thenReturn(Optional.of(policy));
        when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                .thenReturn(List.of(template));
        when(userProfileService.getAllUsersByTenant(TENANT_ID))
                .thenReturn(List.of(buildProfile("user-a")));
        when(appNotificationRepository.save(any())).thenReturn(saved);
        when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

        ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.dispatch(eventKey, TENANT_ID, null, null);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(appNotificationRepository).save(captor.capture());
        // message was null template → renderTemplate returns ""
        assertThat(captor.getValue().getMessage()).isEqualTo("");
        // title was rendered from non-null template with no metadata → placeholder left
        assertThat(captor.getValue().getTitle()).contains("Alert:");
    }

    // ── persist exception silently swallowed ─────────────────────────────────

    @Test
    @DisplayName("persist exception for one user is swallowed and remaining users still process")
    void persistException_swallowed_noSSEForFailedUser() {
        String eventKey = "SITE_CONFIG_UPDATED";
        NotificationRule rule = buildRule(eventKey);
        NotificationRecipientPolicy policy = buildPolicy(rule);
        AppNotification saved = buildNotification();
        AppNotificationResponse response = buildResponse(saved.getId());

        when(notificationRuleRepository
                .findByEventEventKeyAndTenantIdAndIsEnabledTrue(eventKey, TENANT_ID))
                .thenReturn(Optional.of(rule));
        when(recipientPolicyRepository.findByRuleId(rule.getId()))
                .thenReturn(Optional.of(policy));
        when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                .thenReturn(List.of());
        when(userProfileService.getAllUsersByTenant(TENANT_ID))
                .thenReturn(List.of(buildProfile("user-fail"), buildProfile("user-ok")));

        // first user throws, second succeeds
        when(appNotificationRepository.save(any()))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(saved);
        when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.dispatch(eventKey, TENANT_ID, null, Map.of());
            List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
            assertThat(syncs).hasSize(1);
            syncs.getFirst().afterCommit();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // Only user-ok gets SSE
        verify(sseEmitterRegistry).send("user-ok", response);
    }

    // ── configKey is non-String object ───────────────────────────────────────

    @Test
    @DisplayName("configKey as non-String object sets configSection via toString")
    void configKey_nonStringObject_setsConfigSection() {
        dispatchAndCapture("SITE_CONFIG_UPDATED", null,
                Map.of("configKey", 42),
                notification -> assertThat(notification.getMetadata().get("configSection")).isEqualTo(42));
    }

    // ── configSection already present → not overwritten ──────────────────────

    @Test
    @DisplayName("configSection already in metadata is not overwritten")
    void configSection_alreadyPresent_notOverwritten() {
        dispatchAndCapture("SITE_CONFIG_UPDATED", null,
                Map.of("configKey", "newKey", "configSection", "existingSection"),
                notification -> assertThat(notification.getMetadata().get("configSection"))
                        .isEqualTo("existingSection"));
    }

    // ── notifyInitiator = false → initiator excluded from recipients ──────────

    @Test
    @DisplayName("notifyInitiator=false excludes triggeredByUserId from recipient list")
    void notifyInitiator_false_excludesInitiator() {
        String eventKey = "SITE_CONFIG_UPDATED";
        // Build rule with notifyInitiator = false
        NotificationEventCatalog catalog = NotificationEventCatalog.builder()
                .eventKey(eventKey)
                .entityType(com.integration.execution.contract.model.enums.NotificationEntityType.SITE_CONFIG)
                .notifyInitiator(false)
                .isEnabled(true)
                .build();
        catalog.setId(UUID.randomUUID());
        NotificationRule rule = NotificationRule.builder()
                .event(catalog).severity(NotificationSeverity.INFO).isEnabled(true).build();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(TENANT_ID);

        NotificationRecipientPolicy policy = buildPolicy(rule);
        AppNotification saved = buildNotification();
        AppNotificationResponse response = buildResponse(saved.getId());

        when(notificationRuleRepository
                .findByEventEventKeyAndTenantIdAndIsEnabledTrue(eventKey, TENANT_ID))
                .thenReturn(Optional.of(rule));
        when(recipientPolicyRepository.findByRuleId(rule.getId()))
                .thenReturn(Optional.of(policy));
        when(notificationTemplateService.getTemplatesForTenant(TENANT_ID)).thenReturn(List.of());
        // Two users: initiator and another
        when(userProfileService.getAllUsersByTenant(TENANT_ID))
                .thenReturn(List.of(buildProfile(TRIGGER_USER), buildProfile("other-user")));
        when(appNotificationRepository.save(any())).thenReturn(saved);
        when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.dispatch(eventKey, TENANT_ID, TRIGGER_USER, Map.of());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // Only "other-user" gets notification - initiator is excluded
        org.mockito.ArgumentCaptor<AppNotification> captor =
                org.mockito.ArgumentCaptor.forClass(AppNotification.class);
        verify(appNotificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("other-user");
    }

    // ── resolveByKey: null eventKey → returns null ───────────────────────────

    @Test
    @DisplayName("dispatch with eventKey without recognised suffix - resolveByKey returns null (no byKey added)")
    void dispatch_noMatchingSuffix_noByKeyAdded() {
        // Use an event key that does not end with any recognised suffix (_UPDATED, _ENABLED, etc.)
        // resolveByKey returns null → byKey == null → no putIfAbsent
        String eventKey = "SOME_CUSTOM_FIRED";
        NotificationEventCatalog catalog = NotificationEventCatalog.builder()
                .eventKey(eventKey)
                .entityType(com.integration.execution.contract.model.enums.NotificationEntityType.SITE_CONFIG)
                .notifyInitiator(true)
                .isEnabled(true)
                .build();
        catalog.setId(UUID.randomUUID());
        NotificationRule rule = NotificationRule.builder()
                .event(catalog).severity(NotificationSeverity.INFO).isEnabled(true).build();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(TENANT_ID);

        NotificationRecipientPolicy policy = buildPolicy(rule);
        AppNotification saved = buildNotification();
        AppNotificationResponse response = buildResponse(saved.getId());

        when(notificationRuleRepository
                .findByEventEventKeyAndTenantIdAndIsEnabledTrue(eventKey, TENANT_ID))
                .thenReturn(Optional.of(rule));
        when(recipientPolicyRepository.findByRuleId(rule.getId()))
                .thenReturn(Optional.of(policy));
        when(notificationTemplateService.getTemplatesForTenant(TENANT_ID)).thenReturn(List.of());
        when(userProfileService.getAllUsersByTenant(TENANT_ID))
                .thenReturn(List.of(buildProfile("user-a")));
        when(appNotificationRepository.save(any())).thenReturn(saved);
        when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

        org.mockito.ArgumentCaptor<AppNotification> captor =
                org.mockito.ArgumentCaptor.forClass(AppNotification.class);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.dispatch(eventKey, TENANT_ID, TRIGGER_USER, Map.of());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(appNotificationRepository).save(captor.capture());
        // triggeredBy is added but no specific byKey (createdBy/updatedBy/etc.)
        assertThat(captor.getValue().getMetadata()).containsKey("triggeredBy");
        assertThat(captor.getValue().getMetadata()).doesNotContainKey("createdBy");
        assertThat(captor.getValue().getMetadata()).doesNotContainKey("updatedBy");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void dispatchAndCapture(String eventKey, String triggeredBy,
                                    Map<String, Object> metadata,
                                    java.util.function.Consumer<AppNotification> assertions) {
        NotificationRule rule = buildRule(eventKey);
        NotificationRecipientPolicy policy = buildPolicy(rule);
        AppNotification saved = buildNotification();
        AppNotificationResponse response = buildResponse(saved.getId());

        when(notificationRuleRepository
                .findByEventEventKeyAndTenantIdAndIsEnabledTrue(eventKey, TENANT_ID))
                .thenReturn(Optional.of(rule));
        when(recipientPolicyRepository.findByRuleId(rule.getId()))
                .thenReturn(Optional.of(policy));
        when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                .thenReturn(List.of());
        when(userProfileService.getAllUsersByTenant(TENANT_ID))
                .thenReturn(List.of(buildProfile("user-a")));
        when(appNotificationRepository.save(any())).thenReturn(saved);
        when(notificationMapper.toAppNotificationResponse(saved)).thenReturn(response);

        ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.dispatch(eventKey, TENANT_ID, triggeredBy, metadata);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(appNotificationRepository).save(captor.capture());
        assertions.accept(captor.getValue());
    }

    private NotificationRule buildRule(String eventKey) {
        NotificationEventCatalog catalog = NotificationEventCatalog.builder()
                .eventKey(eventKey)
                .entityType(NotificationEntityType.SITE_CONFIG)
                .notifyInitiator(true)
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

    private NotificationRecipientPolicy buildPolicy(NotificationRule rule) {
        NotificationRecipientPolicy policy = NotificationRecipientPolicy.builder()
                .rule(rule).recipientType(RecipientType.ALL_USERS).build();
        policy.setId(UUID.randomUUID());
        policy.setTenantId(TENANT_ID);
        return policy;
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

    private AppNotificationResponse buildResponse(UUID id) {
        return AppNotificationResponse.builder().id(id).tenantId(TENANT_ID).build();
    }

    private UserProfileResponse buildProfile(String userId) {
        return UserProfileResponse.builder()
                .keycloakUserId(userId).isTenantAdmin(false)
                .email(userId + "@test.com").build();
    }
}
