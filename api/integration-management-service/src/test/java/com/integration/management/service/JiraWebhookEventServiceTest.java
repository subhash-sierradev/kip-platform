package com.integration.management.service;

import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.contract.model.enums.TriggerStatus;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.IntegrationPersistenceException;
import com.integration.management.mapper.JiraFieldMappingMapper;
import com.integration.management.mapper.JiraWebhookEventMapper;
import com.integration.management.model.dto.response.JiraWebhookEventResponse;
import com.integration.management.repository.JiraWebhookEventRepository;
import com.integration.management.repository.JiraWebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraWebhookEventService")
class JiraWebhookEventServiceTest {

    private static final String TENANT_ID = "tenant-abc";
    private static final String OTHER_TENANT_ID = "tenant-xyz";
    private static final String WEBHOOK_ID = "webhook-001";
    private static final String USER_ID = "user-001";

    @Mock
    private JiraWebhookEventRepository triggerHistoryRepository;

    @Mock
    private JiraWebhookRepository jiraWebhookRepository;

    @Mock
    private JiraWebhookEventMapper mapper;

    @Mock
    private JiraFieldMappingMapper jiraFieldMappingMapper;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private IntegrationConnectionService integrationConnectionService;

    private JiraWebhookEventService jiraWebhookEventService;

    @BeforeEach
    void setUp() {
        jiraWebhookEventService = new JiraWebhookEventService(
                triggerHistoryRepository,
                jiraWebhookRepository,
                mapper,
                jiraFieldMappingMapper,
                messagePublisher,
                integrationConnectionService);
    }

    // ──────────────────────────────────────────────────────────────
    // executeWebhook
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("executeWebhook")
    class ExecuteWebhook {

        @Test
        @DisplayName("publishes command and returns ACCEPTED")
        void executeWebhook_publishesCommand_andReturnsAccepted() {
            JiraWebhook webhook = JiraWebhook.builder()
                    .id(WEBHOOK_ID)
                    .tenantId(TENANT_ID)
                    .createdBy(USER_ID)
                    .lastModifiedBy(USER_ID)
                    .name("Webhook A")
                    .connectionId(UUID.randomUUID())
                    .webhookUrl("https://example.test")
                    .samplePayload("{}")
                    .isEnabled(true)
                    .build();
            when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(WEBHOOK_ID, TENANT_ID))
                    .thenReturn(Optional.of(webhook));
            when(integrationConnectionService.getIntegrationConnectionNameById(
                    webhook.getConnectionId().toString(), TENANT_ID))
                    .thenReturn("secret-1");

            when(triggerHistoryRepository.save(any(JiraWebhookEvent.class))).thenAnswer(inv -> {
                JiraWebhookEvent e = inv.getArgument(0);
                if (e.getId() == null) {
                    e.setId(UUID.randomUUID());
                }
                return e;
            });

            JiraWebhookEventResponse response = JiraWebhookEventResponse.builder().id("x").build();
            when(mapper.toResponse(any(JiraWebhookEvent.class))).thenReturn(response);

            var out = jiraWebhookEventService.executeWebhook(WEBHOOK_ID, "{\"k\":\"v\"}", TENANT_ID,
                    USER_ID);

            assertThat(out.getStatusCode().value()).isEqualTo(202);
            assertThat(out.getBody()).isSameAs(response);

            var cmdCaptor = org.mockito.ArgumentCaptor.forClass(JiraWebhookExecutionCommand.class);
            verify(messagePublisher).publish(
                    eq(QueueNames.JIRA_WEBHOOK_EXCHANGE),
                    eq(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE),
                    cmdCaptor.capture());
            JiraWebhookExecutionCommand cmd = cmdCaptor.getValue();
            assertThat(cmd.getWebhookId()).isEqualTo(WEBHOOK_ID);
            assertThat(cmd.getWebhookName()).isEqualTo("Webhook A");
            assertThat(cmd.getConnectionSecretName()).isEqualTo("secret-1");
            assertThat(cmd.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(cmd.getTriggeredBy()).isEqualTo(USER_ID);
            assertThat(cmd.getIncomingPayload()).isEqualTo("{\"k\":\"v\"}");
            assertThat(cmd.getTriggerEventId()).isNotNull();
            assertThat(cmd.getOriginalEventId()).isNotBlank();
            assertThat(cmd.getRetryAttempt()).isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // retryTrigger
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("retryTrigger")
    class RetryTrigger {

        @Test
        @DisplayName("creates retry event with incremented retryAttempt and publishes command")
        void retryTrigger_incrementsAttempt_andPublishes() {
            JiraWebhookEvent last = JiraWebhookEvent.builder()
                    .id(UUID.randomUUID())
                    .webhookId(WEBHOOK_ID)
                    .tenantId(TENANT_ID)
                    .triggeredBy(USER_ID)
                    .incomingPayload("{payload}")
                    .originalEventId("orig-1")
                    .retryAttempt(2)
                    .status(TriggerStatus.FAILED)
                    .build();
            when(triggerHistoryRepository.findTopByOriginalEventIdOrderByRetryAttemptDesc("orig-1"))
                    .thenReturn(Optional.of(last));

            JiraWebhook webhook = JiraWebhook.builder()
                    .id(WEBHOOK_ID)
                    .tenantId(TENANT_ID)
                    .createdBy(USER_ID)
                    .lastModifiedBy(USER_ID)
                    .name("Webhook A")
                    .connectionId(UUID.randomUUID())
                    .webhookUrl("https://example.test")
                    .samplePayload("{}")
                    .isEnabled(true)
                    .build();
            when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(WEBHOOK_ID, TENANT_ID))
                    .thenReturn(Optional.of(webhook));
            when(integrationConnectionService.getIntegrationConnectionNameById(
                    webhook.getConnectionId().toString(), TENANT_ID))
                    .thenReturn("secret-1");

            when(triggerHistoryRepository.save(any(JiraWebhookEvent.class))).thenAnswer(inv -> {
                JiraWebhookEvent e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            JiraWebhookEventResponse response = JiraWebhookEventResponse.builder().id("x").build();
            when(mapper.toResponse(any(JiraWebhookEvent.class))).thenReturn(response);

            var out = jiraWebhookEventService.retryTrigger("orig-1", TENANT_ID, USER_ID);
            assertThat(out.getStatusCode().value()).isEqualTo(202);

            var savedCaptor = org.mockito.ArgumentCaptor.forClass(JiraWebhookEvent.class);
            verify(triggerHistoryRepository).save(savedCaptor.capture());
            JiraWebhookEvent saved = savedCaptor.getValue();
            assertThat(saved.getOriginalEventId()).isEqualTo("orig-1");
            assertThat(saved.getRetryAttempt()).isEqualTo(3);
            assertThat(saved.getIncomingPayload()).isEqualTo("{payload}");

            var cmdCaptor = org.mockito.ArgumentCaptor.forClass(JiraWebhookExecutionCommand.class);
            verify(messagePublisher).publish(anyString(), anyString(), cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().getRetryAttempt()).isEqualTo(3);
            assertThat(cmdCaptor.getValue().getOriginalEventId()).isEqualTo("orig-1");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // applyResult
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyResult")
    class ApplyResult {

        @Test
        @DisplayName("null result does nothing")
        void applyResult_null_doesNothing() {
            jiraWebhookEventService.applyResult(null);
            verify(triggerHistoryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("success result updates trigger and maps responseStatusCode=0 to null")
        void applyResult_success_updatesEvent() {
            UUID triggerId = UUID.randomUUID();
            JiraWebhookEvent existing = buildEvent(WEBHOOK_ID, "orig-1");
            existing.setId(triggerId);
            when(triggerHistoryRepository.findById(triggerId)).thenReturn(Optional.of(existing));
            when(triggerHistoryRepository.save(existing)).thenReturn(existing);

            JiraWebhookExecutionResult result = JiraWebhookExecutionResult.builder()
                    .triggerEventId(triggerId)
                    .success(true)
                    .transformedPayload("{x}")
                    .responseStatusCode(0)
                    .responseBody("ok")
                    .jiraIssueUrl("https://jira/1")
                    .build();

            jiraWebhookEventService.applyResult(result);

            assertThat(existing.getStatus()).isEqualTo(TriggerStatus.SUCCESS);
            assertThat(existing.getTransformedPayload()).isEqualTo("{x}");
            assertThat(existing.getResponseStatusCode()).isNull();
            assertThat(existing.getResponseBody()).isEqualTo("ok");
            assertThat(existing.getJiraIssueUrl()).isEqualTo("https://jira/1");
            verify(triggerHistoryRepository).save(existing);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // recordJiraWebhookEvent
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordJiraWebhookEvent")
    class RecordJiraWebhookEvent {

        @Test
        @DisplayName("generates originalEventId when not provided")
        void recordJiraWebhookEvent_generatesOriginalEventId() {
            when(triggerHistoryRepository.save(any(JiraWebhookEvent.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            JiraWebhookEvent out = jiraWebhookEventService.recordJiraWebhookEvent(
                    WEBHOOK_ID, TENANT_ID, USER_ID, "payload", null, 0);

            assertThat(out.getOriginalEventId()).isNotBlank();
            assertThat(out.getStatus()).isEqualTo(TriggerStatus.PENDING);
            assertThat(out.getRetryAttempt()).isEqualTo(0);
        }

        @Test
        @DisplayName("uses provided originalEventId")
        void recordJiraWebhookEvent_usesProvidedOriginalEventId() {
            when(triggerHistoryRepository.save(any(JiraWebhookEvent.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            JiraWebhookEvent out = jiraWebhookEventService.recordJiraWebhookEvent(
                    WEBHOOK_ID, TENANT_ID, USER_ID, "payload", "orig-123", 5);

            assertThat(out.getOriginalEventId()).isEqualTo("orig-123");
            assertThat(out.getRetryAttempt()).isEqualTo(5);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getWebhookEventsByWebhookId
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWebhookEventsByWebhookId")
    class GetWebhookEventsByWebhookId {

        @Test
        @DisplayName("matchingTenantId_returnsLatestMappedEvents")
        void getWebhookEventsByWebhookId_matchingTenantId_returnsLatestMappedEvents() {
            JiraWebhookEvent event1 = buildEvent(WEBHOOK_ID, "orig-1");
            JiraWebhookEvent event2 = buildEvent(WEBHOOK_ID, "orig-2");

            JiraWebhookEventResponse response1 = buildEventResponse(event1.getId().toString());
            JiraWebhookEventResponse response2 = buildEventResponse(event2.getId().toString());

            when(triggerHistoryRepository.findLatestEventsPerOriginalTriggerByWebhook(
                    WEBHOOK_ID, TENANT_ID)).thenReturn(List.of(event1, event2));
            when(mapper.toResponse(event1)).thenReturn(response1);
            when(mapper.toResponse(event2)).thenReturn(response2);

            List<JiraWebhookEventResponse> result = jiraWebhookEventService
                    .getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID);

            assertThat(result).hasSize(2).containsExactly(response1, response2);
            verify(triggerHistoryRepository).findLatestEventsPerOriginalTriggerByWebhook(
                    eq(WEBHOOK_ID), eq(TENANT_ID));
        }

        @Test
        @DisplayName("wrongTenantId_returnsEmptyList")
        void getWebhookEventsByWebhookId_wrongTenantId_returnsEmptyList() {
            // The JOIN in the query filters out rows where jw.tenantId != :tenantId,
            // so the repository returns an empty list for a cross-tenant request.
            when(triggerHistoryRepository.findLatestEventsPerOriginalTriggerByWebhook(
                    WEBHOOK_ID, OTHER_TENANT_ID)).thenReturn(List.of());

            List<JiraWebhookEventResponse> result = jiraWebhookEventService
                    .getWebhookEventsByWebhookId(WEBHOOK_ID, OTHER_TENANT_ID);

            assertThat(result).isEmpty();
            verify(mapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("softDeletedWebhook_returnsEmptyList")
        void getWebhookEventsByWebhookId_softDeletedWebhook_returnsEmptyList() {
            // The JOIN excludes soft-deleted webhooks (jw.isDeleted = false),
            // so events for a deleted webhook are never returned.
            when(triggerHistoryRepository.findLatestEventsPerOriginalTriggerByWebhook(
                    WEBHOOK_ID, TENANT_ID)).thenReturn(List.of());

            List<JiraWebhookEventResponse> result = jiraWebhookEventService
                    .getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID);

            assertThat(result).isEmpty();
            verify(mapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("noEventsExist_returnsEmptyList")
        void getWebhookEventsByWebhookId_noEventsExist_returnsEmptyList() {
            when(triggerHistoryRepository.findLatestEventsPerOriginalTriggerByWebhook(
                    WEBHOOK_ID, TENANT_ID)).thenReturn(List.of());

            List<JiraWebhookEventResponse> result = jiraWebhookEventService
                    .getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID);

            assertThat(result).isEmpty();
            verify(triggerHistoryRepository).findLatestEventsPerOriginalTriggerByWebhook(
                    eq(WEBHOOK_ID), eq(TENANT_ID));
        }

        @Test
        @DisplayName("singleEvent_returnsSingleMappedResponse")
        void getWebhookEventsByWebhookId_singleEvent_returnsSingleMappedResponse() {
            JiraWebhookEvent event = buildEvent(WEBHOOK_ID, "orig-single");
            JiraWebhookEventResponse response = buildEventResponse(event.getId().toString());

            when(triggerHistoryRepository.findLatestEventsPerOriginalTriggerByWebhook(
                    WEBHOOK_ID, TENANT_ID)).thenReturn(List.of(event));
            when(mapper.toResponse(event)).thenReturn(response);

            List<JiraWebhookEventResponse> result = jiraWebhookEventService
                    .getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID);

            assertThat(result).hasSize(1).containsExactly(response);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // findByOriginalEventIdOrderByRetryAttempt
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByOriginalEventIdOrderByRetryAttempt")
    class FindByOriginalEventId {

        @Test
        @DisplayName("existingOriginalEventId_returnsEvent")
        void findByOriginalEventId_existingId_returnsEvent() {
            String originalEventId = "orig-123";
            JiraWebhookEvent event = buildEvent(WEBHOOK_ID, originalEventId);

            when(triggerHistoryRepository.findTopByOriginalEventIdOrderByRetryAttemptDesc(
                    originalEventId)).thenReturn(Optional.of(event));

            JiraWebhookEvent result = jiraWebhookEventService
                    .findByOriginalEventIdOrderByRetryAttempt(originalEventId);

            assertThat(result).isEqualTo(event);
        }

        @Test
        @DisplayName("unknownOriginalEventId_throwsIntegrationPersistenceException")
        void findByOriginalEventId_unknownId_throwsIntegrationPersistenceException() {
            String unknownId = "unknown-id";
            when(triggerHistoryRepository.findTopByOriginalEventIdOrderByRetryAttemptDesc(
                    unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jiraWebhookEventService
                    .findByOriginalEventIdOrderByRetryAttempt(unknownId))
                    .isInstanceOf(IntegrationPersistenceException.class)
                    .hasMessageContaining(unknownId);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // updateTriggerResult
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTriggerResult")
    class UpdateTriggerResult {

        @Test
        @DisplayName("existingTrigger_updatesFieldsAndSaves")
        void updateTriggerResult_existingTrigger_updatesFieldsAndSaves() {
            UUID triggerId = UUID.randomUUID();
            JiraWebhookEvent event = buildEvent(WEBHOOK_ID, "orig-upd");
            event.setId(triggerId);

            when(triggerHistoryRepository.findById(triggerId)).thenReturn(Optional.of(event));
            when(triggerHistoryRepository.save(event)).thenReturn(event);

            jiraWebhookEventService.updateTriggerResult(
                    triggerId, USER_ID, TriggerStatus.SUCCESS,
                    "{\"key\":\"value\"}", 200, "OK", "https://jira.example.com/issue/KW-1");

            assertThat(event.getStatus()).isEqualTo(TriggerStatus.SUCCESS);
            assertThat(event.getResponseStatusCode()).isEqualTo(200);
            assertThat(event.getResponseBody()).isEqualTo("OK");
            assertThat(event.getJiraIssueUrl()).isEqualTo("https://jira.example.com/issue/KW-1");
            verify(triggerHistoryRepository).save(event);
        }

        @Test
        @DisplayName("missingTrigger_throwsIntegrationNotFoundException")
        void updateTriggerResult_missingTrigger_throwsIntegrationNotFoundException() {
            UUID triggerId = UUID.randomUUID();
            when(triggerHistoryRepository.findById(triggerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jiraWebhookEventService.updateTriggerResult(
                    triggerId, USER_ID, TriggerStatus.FAILED,
                    null, null, null, null))
                    .isInstanceOf(IntegrationNotFoundException.class)
                    .hasMessageContaining(triggerId.toString());
        }

        @Test
        @DisplayName("dataIntegrityViolation_throwsIntegrationPersistenceException")
        void updateTriggerResult_dataIntegrityViolation_throwsIntegrationPersistenceException() {
            UUID triggerId = UUID.randomUUID();
            JiraWebhookEvent event = buildEvent(WEBHOOK_ID, "orig-div");
            event.setId(triggerId);

            when(triggerHistoryRepository.findById(triggerId)).thenReturn(Optional.of(event));
            when(triggerHistoryRepository.save(any())).thenThrow(
                    new DataIntegrityViolationException("constraint violation"));

            assertThatThrownBy(() -> jiraWebhookEventService.updateTriggerResult(
                    triggerId, USER_ID, TriggerStatus.FAILED,
                    null, null, null, null))
                    .isInstanceOf(IntegrationPersistenceException.class)
                    .hasMessageContaining("data integrity violation");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private JiraWebhookEvent buildEvent(final String webhookId, final String originalEventId) {
        return JiraWebhookEvent.builder()
                .id(UUID.randomUUID())
                .webhookId(webhookId)
                .tenantId(TENANT_ID)
                .triggeredBy(USER_ID)
                .triggeredAt(Instant.now())
                .status(TriggerStatus.PENDING)
                .retryAttempt(0)
                .originalEventId(originalEventId)
                .build();
    }

    private JiraWebhookEventResponse buildEventResponse(final String id) {
        return JiraWebhookEventResponse.builder()
                .id(id)
                .build();
    }

    // ── Additional branch tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("executeWebhook - blank id")
    class ExecuteWebhookValidation {

        @Test
        @DisplayName("throws IllegalArgumentException for blank webhook ID")
        void executeWebhook_blankId_throws() {
            assertThatThrownBy(() -> jiraWebhookEventService.executeWebhook("", "{}", TENANT_ID, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Webhook ID must not be null");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null webhook ID")
        void executeWebhook_nullId_throws() {
            assertThatThrownBy(() -> jiraWebhookEventService.executeWebhook(null, "{}", TENANT_ID, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("retryTrigger - validation")
    class RetryTriggerValidation {

        @Test
        @DisplayName("throws IllegalArgumentException for blank trigger ID")
        void retryTrigger_blankId_throws() {
            assertThatThrownBy(() -> jiraWebhookEventService.retryTrigger("  ", TENANT_ID, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Trigger ID must not be null");
        }

        @Test
        @DisplayName("uses id as originalEventId when lastEvent has no originalEventId")
        void retryTrigger_nullOriginalEventId_usesId() {
            JiraWebhookEvent last = JiraWebhookEvent.builder()
                    .id(UUID.randomUUID())
                    .webhookId(WEBHOOK_ID)
                    .tenantId(TENANT_ID)
                    .triggeredBy(USER_ID)
                    .incomingPayload("{payload}")
                    .originalEventId(null)   // no original event id
                    .retryAttempt(null)      // null retryAttempt -> nextRetry = 1
                    .status(com.integration.execution.contract.model.enums.TriggerStatus.FAILED)
                    .build();
            when(triggerHistoryRepository.findTopByOriginalEventIdOrderByRetryAttemptDesc("orig-1"))
                    .thenReturn(java.util.Optional.of(last));

            JiraWebhook webhook = JiraWebhook.builder()
                    .id(WEBHOOK_ID).tenantId(TENANT_ID).createdBy(USER_ID)
                    .lastModifiedBy(USER_ID).name("Webhook A")
                    .connectionId(UUID.randomUUID()).webhookUrl("https://example.test")
                    .samplePayload("{}").isEnabled(true).build();
            when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse(WEBHOOK_ID, TENANT_ID))
                    .thenReturn(java.util.Optional.of(webhook));
            when(integrationConnectionService.getIntegrationConnectionNameById(
                    webhook.getConnectionId().toString(), TENANT_ID)).thenReturn("sec");
            when(triggerHistoryRepository.save(any(JiraWebhookEvent.class))).thenAnswer(inv -> {
                JiraWebhookEvent e = inv.getArgument(0);
                if (e.getId() == null) e.setId(UUID.randomUUID());
                return e;
            });
            when(mapper.toResponse(any(JiraWebhookEvent.class)))
                    .thenReturn(JiraWebhookEventResponse.builder().id("r").build());

            var out = jiraWebhookEventService.retryTrigger("orig-1", TENANT_ID, USER_ID);
            assertThat(out.getStatusCode().value()).isEqualTo(202);

            var savedCaptor = org.mockito.ArgumentCaptor.forClass(JiraWebhookEvent.class);
            verify(triggerHistoryRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().getRetryAttempt()).isEqualTo(1);
            // originalEventId should fall back to "orig-1" (the id passed in)
            assertThat(savedCaptor.getValue().getOriginalEventId()).isEqualTo("orig-1");
        }
    }

    @Nested
    @DisplayName("applyResult - null triggerEventId")
    class ApplyResultNullTrigger {

        @Test
        @DisplayName("null triggerEventId in result does nothing")
        void applyResult_nullTriggerEventId_doesNothing() {
            jiraWebhookEventService.applyResult(
                    com.integration.execution.contract.message.JiraWebhookExecutionResult
                            .builder()
                            .triggerEventId(null)
                            .success(true)
                            .build());
            verify(triggerHistoryRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("updateTriggerResult - unexpected exception")
    class UpdateTriggerUnexpected {

        @Test
        @DisplayName("unexpected exception is wrapped in RuntimeException")
        void updateTriggerResult_unexpectedException_throws() {
            UUID triggerId = UUID.randomUUID();
            JiraWebhookEvent event = buildEvent(WEBHOOK_ID, "orig-ue");
            event.setId(triggerId);
            when(triggerHistoryRepository.findById(triggerId)).thenReturn(java.util.Optional.of(event));
            when(triggerHistoryRepository.save(any()))
                    .thenThrow(new RuntimeException("unexpected DB failure"));

            assertThatThrownBy(() -> jiraWebhookEventService.updateTriggerResult(
                    triggerId, USER_ID,
                    com.integration.execution.contract.model.enums.TriggerStatus.FAILED,
                    null, null, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to update webhook trigger");
        }
    }
}
