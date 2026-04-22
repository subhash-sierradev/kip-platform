package com.integration.management.it;

import com.integration.execution.contract.model.enums.FetchMode;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.entity.IntegrationConnection;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.JiraWebhookRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Jira Webhook CRUD operations via the repository layer.
 *
 * <p>Uses pre-seeded GLOBAL tenant (V2 migration) to satisfy FK constraints.
 */
@DisplayName("Jira Webhook Repository — integration tests")
class JiraWebhookRepositoryIT extends AbstractImsIT {

    private static final String TENANT = "GLOBAL";
    private static final String USER = "system";

    @Autowired
    private JiraWebhookRepository jiraWebhookRepository;

    @Autowired
    private IntegrationConnectionRepository integrationConnectionRepository;

    private final List<String> createdWebhookIds = new ArrayList<>();
    private final List<UUID> createdConnectionIds = new ArrayList<>();

    private IntegrationConnection savedConnection;

    @BeforeEach
    void setUp() {
        createdWebhookIds.clear();
        createdConnectionIds.clear();

        savedConnection = integrationConnectionRepository.save(
                IntegrationConnection.builder()
                        .id(UUID.randomUUID())
                        .name("Jira IT Connection")
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
        jiraWebhookRepository.findAllById(createdWebhookIds)
                .forEach(w -> {
                    w.setIsDeleted(true);
                    jiraWebhookRepository.save(w);
                });
        integrationConnectionRepository.findAllById(createdConnectionIds)
                .forEach(c -> {
                    c.setIsDeleted(true);
                    integrationConnectionRepository.save(c);
                });
    }

    private JiraWebhook buildWebhook(final String name) {
        String id = UUID.randomUUID().toString().substring(0, 11);
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

    @Test
    @DisplayName("save and findByIdAndTenantIdAndIsDeletedFalse returns the webhook")
    void saveAndFind_returnsWebhook() {
        JiraWebhook saved = jiraWebhookRepository.save(buildWebhook("Test Webhook IT"));

        Optional<JiraWebhook> found = jiraWebhookRepository
                .findByIdAndTenantIdAndIsDeletedFalse(saved.getId(), TENANT);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Webhook IT");
        assertThat(found.get().getTenantId()).isEqualTo(TENANT);
        assertThat(found.get().getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("findByIdAndTenantIdAndIsDeletedFalse returns empty for soft-deleted webhook")
    void softDeleted_notFoundByActiveQuery() {
        JiraWebhook webhook = jiraWebhookRepository.save(buildWebhook("Delete Me IT"));

        webhook.setIsDeleted(true);
        jiraWebhookRepository.save(webhook);

        Optional<JiraWebhook> found = jiraWebhookRepository
                .findByIdAndTenantIdAndIsDeletedFalse(webhook.getId(), TENANT);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findJiraWebhookNameById returns webhook name")
    void findJiraWebhookNameById_returnsName() {
        JiraWebhook saved = jiraWebhookRepository.save(buildWebhook("Named Webhook IT"));

        String name = jiraWebhookRepository.findJiraWebhookNameById(saved.getId());

        assertThat(name).isEqualTo("Named Webhook IT");
    }

    @Test
    @DisplayName("findByConnectionIdAndIsDeletedFalse returns webhooks for given connection")
    void findByConnectionId_returnsActiveWebhooks() {
        JiraWebhook webhook1 = jiraWebhookRepository.save(buildWebhook("Connection Webhook 1 IT"));
        JiraWebhook webhook2 = jiraWebhookRepository.save(buildWebhook("Connection Webhook 2 IT"));

        List<JiraWebhook> results = jiraWebhookRepository
                .findByConnectionIdAndIsDeletedFalse(savedConnection.getId());

        assertThat(results).extracting(JiraWebhook::getId)
                .contains(webhook1.getId(), webhook2.getId());
    }

    @Test
    @DisplayName("findByConnectionIdAndIsDeletedFalse excludes soft-deleted webhooks")
    void findByConnectionId_excludesSoftDeleted() {
        JiraWebhook active = jiraWebhookRepository.save(buildWebhook("Active Webhook IT"));
        JiraWebhook deleted = jiraWebhookRepository.save(buildWebhook("Deleted Webhook IT"));

        deleted.setIsDeleted(true);
        jiraWebhookRepository.save(deleted);

        List<JiraWebhook> results = jiraWebhookRepository
                .findByConnectionIdAndIsDeletedFalse(savedConnection.getId());

        assertThat(results).extracting(JiraWebhook::getId).contains(active.getId());
        assertThat(results).extracting(JiraWebhook::getId).doesNotContain(deleted.getId());
    }

    @Test
    @DisplayName("isEnabled can be toggled and persisted")
    void toggleEnabled_persistsCorrectly() {
        JiraWebhook webhook = jiraWebhookRepository.save(buildWebhook("Toggle Webhook IT"));
        assertThat(webhook.getIsEnabled()).isTrue();

        webhook.setIsEnabled(false);
        jiraWebhookRepository.save(webhook);

        Optional<JiraWebhook> found = jiraWebhookRepository
                .findByIdAndTenantIdAndIsDeletedFalse(webhook.getId(), TENANT);

        assertThat(found).isPresent();
        assertThat(found.get().getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse returns webhook regardless of tenant context")
    void findByIdAndIsDeletedFalse_returnsWebhook() {
        JiraWebhook saved = jiraWebhookRepository.save(buildWebhook("Execute IT Webhook"));

        Optional<JiraWebhook> found = jiraWebhookRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getTenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse returns empty for soft-deleted webhook")
    void findByIdAndIsDeletedFalse_excludesSoftDeleted() {
        JiraWebhook webhook = jiraWebhookRepository.save(buildWebhook("Soft Delete Execute IT"));

        webhook.setIsDeleted(true);
        jiraWebhookRepository.save(webhook);

        Optional<JiraWebhook> found = jiraWebhookRepository.findByIdAndIsDeletedFalse(webhook.getId());

        assertThat(found).isEmpty();
    }
}

