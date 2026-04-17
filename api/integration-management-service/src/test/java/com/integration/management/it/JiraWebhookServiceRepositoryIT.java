package com.integration.management.it;

import com.integration.execution.contract.model.enums.FetchMode;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.model.enums.TriggerStatus;
import com.integration.management.entity.IntegrationConnection;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.JiraWebhookEventRepository;
import com.integration.management.repository.JiraWebhookRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Jira Webhook service repository layer.
 *
 * <p>Verifies webhook creation, lookup, trigger event persistence, and retry lineage tracking
 * against a real PostgreSQL Testcontainer with all schema migrations applied via Flyway.
 */
@DisplayName("Jira Webhook Service Repository — integration tests")
class JiraWebhookServiceRepositoryIT extends AbstractImsIT {

    private static final String TENANT = "GLOBAL";
    private static final String USER = "system";

    @Autowired
    private JiraWebhookRepository jiraWebhookRepository;

    @Autowired
    private JiraWebhookEventRepository jiraWebhookEventRepository;

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    private final List<String> createdWebhookIds = new ArrayList<>();
    private final List<UUID> createdEventIds = new ArrayList<>();
    private final List<UUID> createdConnectionIds = new ArrayList<>();

    private IntegrationConnection savedConnection;

    @BeforeEach
    void setUp() {
        createdWebhookIds.clear();
        createdEventIds.clear();
        createdConnectionIds.clear();

        savedConnection = connectionRepository.save(
                IntegrationConnection.builder()
                        .id(UUID.randomUUID())
                        .name("Jira IT Connection " + UUID.randomUUID())
                        .secretName("jira-it-secret-" + UUID.randomUUID())
                        .serviceType(ServiceType.JIRA)
                        .fetchMode(FetchMode.POST)
                        .connectionHashKey("hash-" + UUID.randomUUID())
                        .tenantId(TENANT)
                        .createdBy(USER)
                        .lastModifiedBy(USER)
                        .build());
        createdConnectionIds.add(savedConnection.getId());
    }

    @AfterEach
    void tearDown() {
        jiraWebhookEventRepository.deleteAllById(createdEventIds);
        jiraWebhookRepository.findAllById(createdWebhookIds).forEach(w -> {
            w.setIsDeleted(true);
            jiraWebhookRepository.save(w);
        });
        connectionRepository.findAllById(createdConnectionIds).forEach(c -> {
            c.setIsDeleted(true);
            connectionRepository.save(c);
        });
    }

    private JiraWebhook buildWebhook(final String name) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 11);
        JiraWebhook webhook = JiraWebhook.builder()
                .id(id)
                .name(name)
                .normalizedName(name.toLowerCase().replace(" ", "-"))
                .webhookUrl("https://test.example.com/webhook/" + id)
                .connectionId(savedConnection.getId())
                .samplePayload("{\"key\":\"TEST-1\"}")
                .isEnabled(true)
                .tenantId(TENANT)
                .createdBy(USER)
                .lastModifiedBy(USER)
                .build();
        createdWebhookIds.add(id);
        return webhook;
    }

    private JiraWebhookEvent buildEvent(final String webhookId, final TriggerStatus status) {
        return buildEvent(webhookId, status, null, null, null);
    }

    private JiraWebhookEvent buildEvent(final String webhookId, final TriggerStatus status,
            final String originalEventId, final Integer retryAttempt, final Instant triggeredAt) {
        return JiraWebhookEvent.builder()
                .webhookId(webhookId)
                .tenantId(TENANT)
                .triggeredBy(USER)
                .triggeredAt(triggeredAt != null ? triggeredAt : Instant.now())
                .incomingPayload("{\"key\":\"TEST-1\"}")
                .status(status)
                .originalEventId(originalEventId)
                .retryAttempt(retryAttempt != null ? retryAttempt : 0)
                .build();
    }

    // ------------------------------------------------------------------ WEBHOOK CRUD

    @Test
    @DisplayName("webhook saved and retrieved with all fields intact")
    void saveAndFind_webhookFieldsIntact() {
        JiraWebhook saved = jiraWebhookRepository.save(buildWebhook("Field Integrity IT"));

        Optional<JiraWebhook> found =
                jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(saved.getId(), TENANT);

        assertThat(found).isPresent();
        assertThat(found.get().getIsEnabled()).isTrue();
        assertThat(found.get().getConnectionId()).isEqualTo(savedConnection.getId());
        assertThat(found.get().getWebhookUrl()).contains(saved.getId());
    }

    @Test
    @DisplayName("update webhook enabled flag persists correctly")
    void updateEnabledFlag_persistsCorrectly() {
        JiraWebhook webhook = jiraWebhookRepository.save(buildWebhook("Toggle Enabled IT"));

        webhook.setIsEnabled(false);
        jiraWebhookRepository.save(webhook);

        Optional<JiraWebhook> found =
                jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(webhook.getId(), TENANT);

        assertThat(found).isPresent();
        assertThat(found.get().getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("findByConnectionIdAndIsDeletedFalse returns all active webhooks for connection")
    void findByConnection_returnsAllActiveWebhooks() {
        JiraWebhook wh1 = jiraWebhookRepository.save(buildWebhook("Conn Webhook 1 IT"));
        JiraWebhook wh2 = jiraWebhookRepository.save(buildWebhook("Conn Webhook 2 IT"));

        List<JiraWebhook> results =
                jiraWebhookRepository.findByConnectionIdAndIsDeletedFalse(savedConnection.getId());

        List<String> ids = results.stream().map(JiraWebhook::getId).toList();
        assertThat(ids).contains(wh1.getId(), wh2.getId());
    }

    @Test
    @DisplayName("findByConnectionIdAndIsDeletedFalse excludes soft-deleted webhooks")
    void findByConnection_excludesSoftDeleted() {
        JiraWebhook active = jiraWebhookRepository.save(buildWebhook("Active IT"));
        JiraWebhook deleted = jiraWebhookRepository.save(buildWebhook("Deleted IT"));

        deleted.setIsDeleted(true);
        jiraWebhookRepository.save(deleted);

        List<JiraWebhook> results =
                jiraWebhookRepository.findByConnectionIdAndIsDeletedFalse(savedConnection.getId());

        List<String> ids = results.stream().map(JiraWebhook::getId).toList();
        assertThat(ids).contains(active.getId());
        assertThat(ids).doesNotContain(deleted.getId());
    }

    // ------------------------------------------------------------------ EVENT PERSISTENCE

    @Test
    @DisplayName("webhook event saved and retrieved with correct status")
    void saveEvent_retrieveWithCorrectStatus() {
        JiraWebhook webhook = jiraWebhookRepository.save(buildWebhook("Event Test IT"));
        JiraWebhookEvent event = buildEvent(webhook.getId(), TriggerStatus.PENDING);

        JiraWebhookEvent saved = jiraWebhookEventRepository.save(event);
        createdEventIds.add(saved.getId());

        Optional<JiraWebhookEvent> found = jiraWebhookEventRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TriggerStatus.PENDING);
        assertThat(found.get().getWebhookId()).isEqualTo(webhook.getId());
    }

    @Test
    @DisplayName("findLatestEventsPerOriginalTriggerByWebhook returns most recent events for webhook")
    void findLatestEvents_returnsMostRecentPerWebhook() {
        JiraWebhook webhook = jiraWebhookRepository.save(buildWebhook("Latest Events IT"));
        String originalEventId = UUID.randomUUID().toString();

        // Two events for same original, different retry attempts
        JiraWebhookEvent firstEvent = jiraWebhookEventRepository.save(
                buildEvent(webhook.getId(), TriggerStatus.FAILED,
                        originalEventId, 0, Instant.now().minusSeconds(60)));
        createdEventIds.add(firstEvent.getId());

        JiraWebhookEvent retryEvent = jiraWebhookEventRepository.save(
                buildEvent(webhook.getId(), TriggerStatus.SUCCESS,
                        originalEventId, 1, Instant.now()));
        createdEventIds.add(retryEvent.getId());

        List<JiraWebhookEvent> latest =
                jiraWebhookEventRepository.findLatestEventsPerOriginalTriggerByWebhook(
                        webhook.getId(), TENANT);

        // Only the latest event for that original event should be returned
        List<UUID> latestIds = latest.stream().map(JiraWebhookEvent::getId).toList();
        assertThat(latestIds).contains(retryEvent.getId());
        assertThat(latestIds).doesNotContain(firstEvent.getId());
    }

    @Test
    @DisplayName("findTopByOriginalEventIdOrderByRetryAttemptDesc returns last retry for original event")
    void findTopByOriginalEventId_returnsHighestRetry() {
        JiraWebhook webhook = jiraWebhookRepository.save(buildWebhook("Top Retry IT"));
        String originalEventId = UUID.randomUUID().toString();

        JiraWebhookEvent attempt0 = jiraWebhookEventRepository.save(
                buildEvent(webhook.getId(), TriggerStatus.FAILED, originalEventId, 0, null));
        createdEventIds.add(attempt0.getId());

        JiraWebhookEvent attempt1 = jiraWebhookEventRepository.save(
                buildEvent(webhook.getId(), TriggerStatus.FAILED, originalEventId, 1, null));
        createdEventIds.add(attempt1.getId());

        Optional<JiraWebhookEvent> top =
                jiraWebhookEventRepository.findTopByOriginalEventIdOrderByRetryAttemptDesc(originalEventId);

        assertThat(top).isPresent();
        assertThat(top.get().getRetryAttempt()).isEqualTo(1);
        assertThat(top.get().getId()).isEqualTo(attempt1.getId());
    }

    @Test
    @DisplayName("findLatestEventsForWebhookIds returns the latest event per webhook")
    void findLatestEventsForWebhookIds_returnsLatestPerWebhook() {
        JiraWebhook webhookA = jiraWebhookRepository.save(buildWebhook("Latest For A IT"));
        JiraWebhook webhookB = jiraWebhookRepository.save(buildWebhook("Latest For B IT"));

        JiraWebhookEvent oldEventA = jiraWebhookEventRepository.save(
                buildEvent(webhookA.getId(), TriggerStatus.SUCCESS,
                        null, null, Instant.now().minusSeconds(120)));
        createdEventIds.add(oldEventA.getId());

        JiraWebhookEvent newEventA = jiraWebhookEventRepository.save(
                buildEvent(webhookA.getId(), TriggerStatus.FAILED,
                        null, null, Instant.now()));
        createdEventIds.add(newEventA.getId());

        JiraWebhookEvent eventB = jiraWebhookEventRepository.save(
                buildEvent(webhookB.getId(), TriggerStatus.PENDING,
                        null, null, Instant.now()));
        createdEventIds.add(eventB.getId());

        List<JiraWebhookEvent> results =
                jiraWebhookEventRepository.findLatestEventsForWebhookIds(
                        List.of(webhookA.getId(), webhookB.getId()));

        assertThat(results).hasSize(2);
        List<UUID> resultIds = results.stream().map(JiraWebhookEvent::getId).toList();
        assertThat(resultIds).contains(newEventA.getId(), eventB.getId());
        assertThat(resultIds).doesNotContain(oldEventA.getId());
    }
}

