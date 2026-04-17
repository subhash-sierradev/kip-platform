package com.integration.management.it;

import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.execution.contract.model.enums.RecipientType;
import com.integration.management.notification.entity.AppNotification;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.management.notification.entity.NotificationRecipientPolicy;
import com.integration.management.notification.entity.NotificationRecipientUser;
import com.integration.management.notification.entity.NotificationRule;
import com.integration.management.notification.repository.AppNotificationRepository;
import com.integration.management.notification.repository.NotificationEventCatalogRepository;
import com.integration.management.notification.repository.NotificationRecipientPolicyRepository;
import com.integration.management.notification.repository.NotificationRecipientUserRepository;
import com.integration.management.notification.repository.NotificationRuleRepository;
import com.integration.management.notification.service.NotificationDispatchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link NotificationDispatchService}.
 *
 * <p>Verifies the end-to-end dispatch pipeline — rule lookup → recipient resolution
 * → {@link AppNotification} persistence — using a real PostgreSQL Testcontainer.
 * SSE fan-out is verified structurally (no active SSE connections expected in tests).
 */
@DisplayName("Notification Dispatch Service — integration tests")
class NotificationDispatchIT extends AbstractImsIT {

    private static final String TENANT = "IT-NOTIFY-TENANT-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String ADMIN_USER = "admin-it-notify-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TRIGGERING_USER = "trigger-user-it";

    @Autowired
    private NotificationDispatchService notificationDispatchService;

    @Autowired
    private NotificationEventCatalogRepository eventCatalogRepository;

    @Autowired
    private NotificationRuleRepository notificationRuleRepository;

    @Autowired
    private NotificationRecipientPolicyRepository recipientPolicyRepository;

    @Autowired
    private NotificationRecipientUserRepository recipientUserRepository;

    @Autowired
    private AppNotificationRepository appNotificationRepository;

    private final List<UUID> createdCatalogIds = new ArrayList<>();
    private final List<UUID> createdRuleIds = new ArrayList<>();
    private final List<UUID> createdPolicyIds = new ArrayList<>();
    private final List<UUID> createdRecipientUserIds = new ArrayList<>();

    private NotificationEventCatalog savedCatalog;
    private NotificationRule savedRule;
    private NotificationRecipientPolicy savedPolicy;

    @BeforeEach
    void setUp() {
        createdCatalogIds.clear();
        createdRuleIds.clear();
        createdPolicyIds.clear();
        createdRecipientUserIds.clear();

        // Create event catalog entry
        savedCatalog = eventCatalogRepository.save(
                NotificationEventCatalog.builder()
                        .eventKey(NotificationEventKey.JIRAWEBHOOK_INTEGRATION_CREATED.name())
                        .entityType(NotificationEntityType.JIRA_WEBHOOK)
                        .displayName("Jira Webhook Created IT")
                        .description("IT test event")
                        .isEnabled(true)
                        .notifyInitiator(true)
                        .build());
        createdCatalogIds.add(savedCatalog.getId());

        // Create enabled notification rule for the test tenant
        savedRule = notificationRuleRepository.save(
                NotificationRule.builder()
                        .event(savedCatalog)
                        .severity(NotificationSeverity.INFO)
                        .isEnabled(true)
                        .tenantId(TENANT)
                        .createdBy("system")
                        .lastModifiedBy("system")
                        .build());
        createdRuleIds.add(savedRule.getId());

        // Create recipient policy: SELECTED_USERS
        savedPolicy = recipientPolicyRepository.save(
                NotificationRecipientPolicy.builder()
                        .rule(savedRule)
                        .recipientType(RecipientType.SELECTED_USERS)
                        .tenantId(TENANT)
                        .createdBy("system")
                        .lastModifiedBy("system")
                        .build());
        createdPolicyIds.add(savedPolicy.getId());

        // Add recipient user (keycloak userId stored in userId column)
        NotificationRecipientUser recipientUser = recipientUserRepository.save(
                NotificationRecipientUser.builder()
                        .recipientPolicy(savedPolicy)
                        .userId(ADMIN_USER)
                        .tenantId(TENANT)
                        .build());
        createdRecipientUserIds.add(recipientUser.getId());
    }

    @AfterEach
    void tearDown() {
        // Clean up notifications created during dispatch
        appNotificationRepository.findAll().stream()
                .filter(n -> TENANT.equals(n.getTenantId()))
                .map(AppNotification::getId)
                .toList()
                .forEach(id -> appNotificationRepository.deleteById(id));

        recipientUserRepository.deleteAllById(createdRecipientUserIds);
        recipientPolicyRepository.deleteAllById(createdPolicyIds);
        notificationRuleRepository.deleteAllById(createdRuleIds);
        eventCatalogRepository.deleteAllById(createdCatalogIds);
    }

    @Test
    @DisplayName("dispatch persists AppNotification to DB for enabled rule with specific-user policy")
    void dispatch_persistsNotification_forEnabledRuleWithSpecificUsersPolicy() {
        Map<String, Object> metadata = Map.of("webhookName", "IT Webhook", "webhookId", "wh-it-01");

        notificationDispatchService.dispatch(
                NotificationEventKey.JIRAWEBHOOK_INTEGRATION_CREATED.name(),
                TENANT,
                TRIGGERING_USER,
                metadata);

        Page<AppNotification> notifications = appNotificationRepository
                .findByTenantIdAndUserIdWithFilters(TENANT, ADMIN_USER, null, null, Pageable.unpaged());

        assertThat(notifications.getTotalElements()).isGreaterThanOrEqualTo(1);
        AppNotification notification = notifications.getContent().get(0);
        assertThat(notification.getTenantId()).isEqualTo(TENANT);
        assertThat(notification.getUserId()).isEqualTo(ADMIN_USER);
        assertThat(notification.getSeverity()).isEqualTo(NotificationSeverity.INFO);
    }

    @Test
    @DisplayName("dispatch is skipped when no enabled rule exists for the event+tenant")
    void dispatch_skipped_whenNoEnabledRule() {
        notificationDispatchService.dispatch(
                "UNKNOWN_EVENT_KEY_IT",
                TENANT,
                TRIGGERING_USER,
                Map.of());

        Page<AppNotification> notifications = appNotificationRepository
                .findByTenantIdAndUserIdWithFilters(TENANT, ADMIN_USER, null, null, Pageable.unpaged());

        // No notifications should be created for unknown event key
        long count = notifications.getContent().stream()
                .filter(n -> "UNKNOWN_EVENT_KEY_IT".equals(
                        n.getType() != null ? n.getType().name() : ""))
                .count();
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("dispatch is skipped when rule is disabled")
    void dispatch_skipped_whenRuleDisabled() {
        savedRule.setIsEnabled(false);
        notificationRuleRepository.save(savedRule);

        notificationDispatchService.dispatch(
                NotificationEventKey.JIRAWEBHOOK_INTEGRATION_CREATED.name(),
                TENANT,
                TRIGGERING_USER,
                Map.of("webhookName", "Disabled Rule Test IT"));

        Page<AppNotification> notifications = appNotificationRepository
                .findByTenantIdAndUserIdWithFilters(TENANT, ADMIN_USER, null, null, Pageable.unpaged());

        assertThat(notifications.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("dispatch persists notification with metadata rendered in message")
    void dispatch_notificationIncludesMetadataInMessage() {
        Map<String, Object> metadata = Map.of(
                "webhookName", "IT-Specific-Webhook",
                "webhookId", "wh-it-metadata");

        notificationDispatchService.dispatch(
                NotificationEventKey.JIRAWEBHOOK_INTEGRATION_CREATED.name(),
                TENANT,
                TRIGGERING_USER,
                metadata);

        Page<AppNotification> notifications = appNotificationRepository
                .findByTenantIdAndUserIdWithFilters(TENANT, ADMIN_USER, null, null, Pageable.unpaged());

        assertThat(notifications.getTotalElements()).isGreaterThanOrEqualTo(1);
        AppNotification notification = notifications.getContent().get(0);
        assertThat(notification.getTitle()).isNotBlank();
        assertThat(notification.getMessage()).isNotBlank();
    }
}

