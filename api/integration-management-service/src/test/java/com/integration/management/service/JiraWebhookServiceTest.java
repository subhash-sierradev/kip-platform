package com.integration.management.service;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.management.config.properties.JiraWebhookProperties;
import com.integration.management.entity.JiraFieldMapping;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.entity.JiraWebhookEvent;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.IntegrationPersistenceException;
import com.integration.management.mapper.JiraFieldMappingMapper;
import com.integration.management.mapper.JiraWebhookMapper;
import com.integration.management.model.dto.request.JiraWebhookCreateUpdateRequest;
import com.integration.management.model.dto.response.JiraWebhookSummaryResponse;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.repository.JiraWebhookEventRepository;
import com.integration.management.repository.JiraWebhookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraWebhookService")
class JiraWebhookServiceTest {

    @Mock
    private JiraWebhookProperties jiraWebhookProperties;

    @Mock
    private JiraWebhookRepository jiraWebhookRepository;

    @Mock
    private JiraWebhookMapper jiraWebhookMapper;

    @Mock
    private JiraFieldMappingMapper jiraFieldMappingMapper;

    @Mock
    private JiraWebhookEventRepository jiraWebhookEventRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private JiraWebhookService jiraWebhookService;

    @Test
    @DisplayName("create should set tenant/audit fields, generate id/url, and persist")
    void create_setsFieldsAndPersists() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("My Webhook")
                .connectionId(UUID.randomUUID())
                .samplePayload("{}")
                .fieldsMapping(null)
                .build();

        JiraWebhook entity = JiraWebhook.builder()
                .name("My Webhook")
                .connectionId(request.getConnectionId())
                .samplePayload(request.getSamplePayload())
                .jiraFieldMappings(new ArrayList<>())
                .build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(3);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/api/{webhookId}");
        when(jiraWebhookRepository.save(any(JiraWebhook.class))).thenAnswer(inv -> inv.getArgument(0));

        JiraWebhook saved = jiraWebhookService.create(request, "tenant1", "user1");

        assertThat(saved.getTenantId()).isEqualTo("tenant1");
        assertThat(saved.getCreatedBy()).isEqualTo("user1");
        assertThat(saved.getLastModifiedBy()).isEqualTo("user1");
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getWebhookUrl()).isEqualTo("https://host/api/" + saved.getId());
        verify(jiraWebhookRepository).save(entity);
    }

    @Test
    @DisplayName("create should retry when a duplicate key DataAccessException occurs")
    void create_retriesOnDuplicateKey() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("My Webhook")
                .connectionId(UUID.randomUUID())
                .samplePayload("{}")
                .build();

        JiraWebhook entity = JiraWebhook.builder()
                .name("My Webhook")
                .connectionId(request.getConnectionId())
                .samplePayload(request.getSamplePayload())
                .jiraFieldMappings(new ArrayList<>())
                .build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(5);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/api/{webhookId}");

        DataAccessResourceFailureException duplicateKey = new DataAccessResourceFailureException(
                "fail",
                new RuntimeException("duplicate key value violates unique constraint"));

        when(jiraWebhookRepository.save(any(JiraWebhook.class)))
                .thenThrow(duplicateKey)
                .thenAnswer(inv -> inv.getArgument(0));

        JiraWebhook saved = jiraWebhookService.create(request, "tenant1", "user1");

        assertThat(saved.getId()).isNotBlank();
        verify(jiraWebhookRepository, times(2)).save(entity);
    }

    @Test
    @DisplayName("create should rethrow DataAccessException when it is not a duplicate key")
    void create_dataAccessNonDuplicate_rethrows() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("My Webhook")
                .connectionId(UUID.randomUUID())
                .samplePayload("{}")
                .build();

        JiraWebhook entity = JiraWebhook.builder()
                .name("My Webhook")
                .connectionId(request.getConnectionId())
                .samplePayload(request.getSamplePayload())
                .jiraFieldMappings(new ArrayList<>())
                .build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(1);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/api/{webhookId}");

        DataAccessException notDuplicate = new DataAccessResourceFailureException(
                "fail",
                new RuntimeException("connection refused"));
        when(jiraWebhookRepository.save(any(JiraWebhook.class))).thenThrow(notDuplicate);

        assertThatThrownBy(() -> jiraWebhookService.create(request, "tenant1", "user1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create Jira webhook")
                .hasCauseInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("create should return null after exhausting retries on duplicate key DataAccessException")
    void create_duplicateKey_exhaustsRetries_returnsNull() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("My Webhook")
                .connectionId(UUID.randomUUID())
                .samplePayload("{}")
                .build();

        JiraWebhook entity = JiraWebhook.builder()
                .name("My Webhook")
                .connectionId(request.getConnectionId())
                .samplePayload(request.getSamplePayload())
                .jiraFieldMappings(new ArrayList<>())
                .build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(2);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/api/{webhookId}");

        DataAccessResourceFailureException duplicateKey = new DataAccessResourceFailureException(
                "fail",
                new RuntimeException("duplicate key value violates unique constraint"));
        when(jiraWebhookRepository.save(any(JiraWebhook.class)))
                .thenThrow(duplicateKey)
                .thenThrow(duplicateKey);

        JiraWebhook out = jiraWebhookService.create(request, "tenant1", "user1");

        assertThat(out).isNull();
        verify(jiraWebhookRepository, times(2)).save(entity);
    }

    @Test
    @DisplayName("create should map unique constraint violations to IntegrationNameAlreadyExistsException")
    void create_uniqueConstraint_throwsNameAlreadyExists() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("Dup")
                .connectionId(UUID.randomUUID())
                .samplePayload("{}")
                .build();

        JiraWebhook entity = JiraWebhook.builder()
                .name("Dup")
                .connectionId(request.getConnectionId())
                .samplePayload(request.getSamplePayload())
                .jiraFieldMappings(new ArrayList<>())
                .build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(1);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/api/{webhookId}");

        DataIntegrityViolationException uniqueViolation = new DataIntegrityViolationException(
                "integrity",
                new RuntimeException("UNIQUE CONSTRAINT"));
        when(jiraWebhookRepository.save(any(JiraWebhook.class))).thenThrow(uniqueViolation);

        assertThatThrownBy(() -> jiraWebhookService.create(request, "tenant1", "user1"))
                .isInstanceOf(IntegrationNameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("update should not replace mappings when snapshot is unchanged")
    void update_doesNotReplaceMappings_whenUnchanged() {
        UUID connectionId = UUID.randomUUID();
        JiraWebhook existing = JiraWebhook.builder()
                .id("w1")
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .name("Old")
                .webhookUrl("https://host/api/w1")
                .connectionId(connectionId)
                .samplePayload("{}")
                .jiraFieldMappings(new ArrayList<>())
                .build();

        JiraFieldMapping existingMapping = JiraFieldMapping.builder()
                .jiraWebhook(existing)
                .jiraFieldId("fid")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("t")
                .required(true)
                .defaultValue("d")
                .build();
        existing.getJiraFieldMappings().add(existingMapping);

        JiraFieldMappingDto incomingDto = JiraFieldMappingDto.builder()
                .jiraFieldId("fid")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("t")
                .required(true)
                .defaultValue("d")
                .build();

        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("New")
                .connectionId(connectionId)
                .samplePayload("{}")
                .fieldsMapping(List.of(incomingDto))
                .build();

        JiraFieldMapping incomingMapping = JiraFieldMapping.builder()
                .jiraFieldId("fid")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("t")
                .required(true)
                .defaultValue("d")
                .build();

        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w1", "tenant1"))
                .thenReturn(Optional.of(existing));
        doNothing().when(jiraWebhookMapper).updateEntity(eq(existing), eq(request));
        when(jiraFieldMappingMapper.toEntity(request.getFieldsMapping())).thenReturn(List.of(incomingMapping));
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/api/{webhookId}");

        jiraWebhookService.update("w1", request, "tenant1", "user2");

        assertThat(existing.getJiraFieldMappings())
                .hasSize(1)
                .containsExactly(existingMapping);
        verify(jiraWebhookRepository).save(existing);
    }

    @Test
    @DisplayName("update should replace mappings when snapshot changed")
    void update_replacesMappings_whenChanged() {
        UUID connectionId = UUID.randomUUID();
        JiraWebhook existing = JiraWebhook.builder()
                .id("w1")
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .name("Old")
                .webhookUrl("https://host/api/w1")
                .connectionId(connectionId)
                .samplePayload("{}")
                .jiraFieldMappings(new ArrayList<>())
                .build();

        JiraFieldMapping existingMapping = JiraFieldMapping.builder()
                .jiraWebhook(existing)
                .jiraFieldId("fid")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("t")
                .required(false)
                .defaultValue(null)
                .build();
        existing.getJiraFieldMappings().add(existingMapping);

        JiraFieldMappingDto incomingDto = JiraFieldMappingDto.builder()
                .jiraFieldId("fid2")
                .jiraFieldName("Other")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("t")
                .required(false)
                .build();

        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("New")
                .connectionId(connectionId)
                .samplePayload("{}")
                .fieldsMapping(List.of(incomingDto))
                .build();

        JiraFieldMapping incomingMapping = JiraFieldMapping.builder()
                .jiraFieldId("fid2")
                .jiraFieldName("Other")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("t")
                .required(false)
                .build();

        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w1", "tenant1"))
                .thenReturn(Optional.of(existing));
        doNothing().when(jiraWebhookMapper).updateEntity(eq(existing), eq(request));
        when(jiraFieldMappingMapper.toEntity(request.getFieldsMapping())).thenReturn(List.of(incomingMapping));
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/api/{webhookId}");

        jiraWebhookService.update("w1", request, "tenant1", "user2");

        assertThat(existing.getJiraFieldMappings())
                .hasSize(1);
        assertThat(existing.getJiraFieldMappings().get(0).getJiraFieldId()).isEqualTo("fid2");
        verify(jiraWebhookRepository).save(existing);
    }

    @Test
    @DisplayName("toggleActiveStatus should flip enabled and return new value")
    void toggleActiveStatus_flipsAndReturnsNewValue() {
        JiraWebhook webhook = JiraWebhook.builder()
                .id("w1")
                .tenantId("tenant1")
                .createdBy("u")
                .lastModifiedBy("u")
                .name("n")
                .webhookUrl("url")
                .connectionId(UUID.randomUUID())
                .samplePayload("{}")
                .isEnabled(true)
                .build();

        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w1", "tenant1"))
                .thenReturn(Optional.of(webhook));

        boolean newStatus = jiraWebhookService.toggleActiveStatus("w1", "tenant1", "user2");

        assertThat(newStatus).isFalse();
        assertThat(webhook.getIsEnabled()).isFalse();
        verify(jiraWebhookRepository).save(webhook);
    }

    @Test
    @DisplayName("toggleActiveStatus should throw IntegrationNotFoundException when webhook missing")
    void toggleActiveStatus_missing_throwsNotFound() {
        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w1", "tenant1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jiraWebhookService.toggleActiveStatus("w1", "tenant1", "user"))
                .isInstanceOf(IntegrationNotFoundException.class);
        verify(jiraWebhookRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleActiveStatus should wrap DataAccessException in IntegrationPersistenceException")
    void toggleActiveStatus_dataAccess_wraps() {
        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w1", "tenant1"))
                .thenThrow(new DataAccessResourceFailureException("down"));

        assertThatThrownBy(() -> jiraWebhookService.toggleActiveStatus("w1", "tenant1", "user"))
                .isInstanceOf(IntegrationPersistenceException.class);
    }

    @Test
    @DisplayName("getAllByTenantId should enrich summary with latest event")
    void getAllByTenantId_enrichesWithLatestEvent() {
        JiraWebhook w1 = JiraWebhook.builder()
                .id("w1")
                .tenantId("tenant1")
                .name("A")
                .webhookUrl("u1")
                .isEnabled(true)
                .isDeleted(false)
                .createdBy("u")
                .createdDate(Instant.parse("2025-01-01T00:00:00Z"))
                .jiraFieldMappings(List.of())
                .build();
        JiraWebhook w2 = JiraWebhook.builder()
                .id("w2")
                .tenantId("tenant1")
                .name("B")
                .webhookUrl("u2")
                .isEnabled(true)
                .isDeleted(false)
                .createdBy("u")
                .createdDate(Instant.parse("2025-01-02T00:00:00Z"))
                .jiraFieldMappings(List.of())
                .build();

        JiraWebhookEvent ev = JiraWebhookEvent.builder()
                .id(UUID.randomUUID())
                .webhookId("w2")
                .triggeredBy("user")
                .triggeredAt(Instant.parse("2025-01-03T00:00:00Z"))
                .jiraIssueUrl("https://jira/browse/ABC-1")
                .build();

        when(jiraWebhookRepository.findAllSummaryByTenantId("tenant1")).thenReturn(List.of(w1, w2));
        when(jiraWebhookEventRepository.findLatestEventsForWebhookIds(List.of("w1", "w2")))
                .thenReturn(List.of(ev));

        List<JiraWebhookSummaryResponse> result = jiraWebhookService.getAllByTenantId("tenant1");

        assertThat(result).hasSize(2);
        JiraWebhookSummaryResponse r2 = result.stream().filter(r -> "w2".equals(r.getId())).findFirst().orElseThrow();
        assertThat(r2.getLastEventHistory()).isNotNull();
        assertThat(r2.getLastEventHistory().getTriggeredBy()).isEqualTo("user");
        assertThat(r2.getLastEventHistory().getJiraIssueUrl()).isEqualTo("https://jira/browse/ABC-1");
        JiraWebhookSummaryResponse r1 = result.stream().filter(r -> "w1".equals(r.getId())).findFirst().orElseThrow();
        assertThat(r1.getLastEventHistory()).isNull();
    }

    @Test
    @DisplayName("getJiraWebhookNameById should return null when repository throws")
    void getJiraWebhookNameById_returnsNullOnException() {
        when(jiraWebhookRepository.findJiraWebhookNameById("w1")).thenThrow(new RuntimeException("boom"));

        assertThat(jiraWebhookService.getJiraWebhookNameById("w1")).isNull();
    }

    @Test
    @DisplayName("getAllByTenantId should return empty list when repository returns none")
    void getAllByTenantId_empty_returnsEmptyList() {
        when(jiraWebhookRepository.findAllSummaryByTenantId("t")).thenReturn(List.of());

        List<JiraWebhookSummaryResponse> out = jiraWebhookService.getAllByTenantId("t");

        assertThat(out).isEmpty();
        verify(jiraWebhookEventRepository, never()).findLatestEventsForWebhookIds(any());
    }

    // ── Additional branch tests ────────────────────────────────────────────────

    @Test
    @DisplayName("create with fieldsMapping sets bidirectional relationship")
    void create_withFieldsMapping_setsRelationship() {
        UUID connectionId = UUID.randomUUID();
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("Webhook B")
                .connectionId(connectionId)
                .samplePayload("{}")
                .fieldsMapping(List.of(com.integration.execution.contract.model.JiraFieldMappingDto.builder()
                    .jiraFieldId("f1").jiraFieldName("Field1")
                    .dataType(com.integration.execution.contract.model.enums.JiraDataType.STRING)
                    .build()))
                .build();

        JiraWebhook entity = JiraWebhook.builder()
                .name("Webhook B")
                .connectionId(connectionId)
                .samplePayload("{}")
                .build();

        JiraFieldMapping mapping = JiraFieldMapping.builder()
                .jiraFieldId("f1").jiraFieldName("Field1")
                .dataType(com.integration.execution.contract.model.enums.JiraDataType.STRING)
                .build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(3);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/{webhookId}");
        when(jiraFieldMappingMapper.toEntity(request.getFieldsMapping())).thenReturn(List.of(mapping));
        when(jiraWebhookRepository.save(any(JiraWebhook.class))).thenAnswer(inv -> inv.getArgument(0));

        JiraWebhook saved = jiraWebhookService.create(request, "t1", "u1");

        assertThat(saved.getJiraFieldMappings()).isNotNull().hasSize(1);
        assertThat(saved.getJiraFieldMappings().get(0).getJiraWebhook()).isSameAs(saved);
    }

    @Test
    @DisplayName("create with DataIntegrityViolationException not unique - throws IntegrationPersistenceException")
    void create_dataIntegrityNonUnique_throwsPersistence() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("N").connectionId(UUID.randomUUID()).samplePayload("{}").build();
        JiraWebhook entity = JiraWebhook.builder().name("N").connectionId(request.getConnectionId())
                .samplePayload("{}").jiraFieldMappings(new ArrayList<>()).build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(1);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/{webhookId}");
        when(jiraWebhookRepository.save(any())).thenThrow(
            new DataIntegrityViolationException("other violation", new RuntimeException("other")));

        assertThatThrownBy(() -> jiraWebhookService.create(request, "t1", "u1"))
            .isInstanceOf(com.integration.management.exception.IntegrationPersistenceException.class);
    }

    @Test
    @DisplayName("create unexpected exception - throws RuntimeException")
    void create_unexpectedException_throwsRuntime() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("N").connectionId(UUID.randomUUID()).samplePayload("{}").build();

        when(jiraWebhookMapper.toEntity(request)).thenThrow(new RuntimeException("unexpected"));

        assertThatThrownBy(() -> jiraWebhookService.create(request, "t1", "u1"))
            .isInstanceOf(RuntimeException.class).hasMessageContaining("Failed to create");
    }

    @Test
    @DisplayName("update with null fieldsMapping skips mapping update")
    void update_nullFieldsMapping_skipsMappingUpdate() {
        UUID connectionId = UUID.randomUUID();
        JiraWebhook existing = JiraWebhook.builder()
                .id("w2").tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("Old").webhookUrl("url").connectionId(connectionId)
                .samplePayload("{}").jiraFieldMappings(new ArrayList<>()).build();
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("New").connectionId(connectionId).samplePayload("{}")
                .fieldsMapping(null).build();

        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w2", "t1"))
                .thenReturn(Optional.of(existing));
        doNothing().when(jiraWebhookMapper).updateEntity(eq(existing), eq(request));
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/{webhookId}");

        jiraWebhookService.update("w2", request, "t1", "u2");

        verify(jiraWebhookRepository).save(existing);
        verify(jiraFieldMappingMapper, never()).toEntity(
            org.mockito.ArgumentMatchers.<List<com.integration.execution.contract.model.JiraFieldMappingDto>>any());
    }

    @Test
    @DisplayName("delete sets isDeleted=true and isEnabled=false")
    void delete_setsDeletedAndDisabled() {
        JiraWebhook existing = JiraWebhook.builder()
                .id("w3").tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("W3").webhookUrl("url").connectionId(UUID.randomUUID())
                .samplePayload("{}").isEnabled(true).isDeleted(false).build();
        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w3", "t1"))
                .thenReturn(Optional.of(existing));

        jiraWebhookService.delete("w3", "t1", "u");

        assertThat(existing.getIsDeleted()).isTrue();
        assertThat(existing.getIsEnabled()).isFalse();
        verify(jiraWebhookRepository).save(existing);
    }

    @Test
    @DisplayName("delete throws IntegrationNotFoundException when webhook not found")
    void delete_notFound_throws() {
        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w99", "t1"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> jiraWebhookService.delete("w99", "t1", "u"))
            .isInstanceOf(IntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("getById with field mappings returns populated response")
    void getById_withMappings_returnsPopulatedResponse() {
        JiraFieldMapping mapping = JiraFieldMapping.builder()
                .jiraFieldId("f1").jiraFieldName("F")
                .dataType(com.integration.execution.contract.model.enums.JiraDataType.STRING).build();
        JiraWebhook webhook = JiraWebhook.builder()
                .id("w4").tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("W4").webhookUrl("url").connectionId(UUID.randomUUID())
                .samplePayload("{}").jiraFieldMappings(new ArrayList<>(List.of(mapping))).build();
        com.integration.management.model.dto.response.JiraWebhookDetailResponse resp =
            com.integration.management.model.dto.response.JiraWebhookDetailResponse.builder()
                .id("w4").name("W4").build();
        com.integration.execution.contract.model.JiraFieldMappingDto dto =
            com.integration.execution.contract.model.JiraFieldMappingDto.builder()
                .jiraFieldId("f1").build();
        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w4", "t1"))
                .thenReturn(Optional.of(webhook));
        when(jiraWebhookMapper.toResponse(webhook)).thenReturn(resp);
        when(jiraFieldMappingMapper.toDto(mapping)).thenReturn(dto);

        com.integration.management.model.dto.response.JiraWebhookDetailResponse out =
            jiraWebhookService.getById("w4", "t1");

        assertThat(out).isSameAs(resp);
        assertThat(out.getJiraFieldMappings()).hasSize(1);
    }

    @Test
    @DisplayName("getById without mappings returns empty list")
    void getById_withoutMappings_returnsEmptyList() {
        JiraWebhook webhook = JiraWebhook.builder()
                .id("w5").tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("W5").webhookUrl("url").connectionId(UUID.randomUUID())
                .samplePayload("{}").jiraFieldMappings(new ArrayList<>()).build();
        com.integration.management.model.dto.response.JiraWebhookDetailResponse resp =
            com.integration.management.model.dto.response.JiraWebhookDetailResponse.builder()
                .id("w5").name("W5").build();
        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w5", "t1"))
                .thenReturn(Optional.of(webhook));
        when(jiraWebhookMapper.toResponse(webhook)).thenReturn(resp);

        com.integration.management.model.dto.response.JiraWebhookDetailResponse out =
            jiraWebhookService.getById("w5", "t1");

        assertThat(out.getJiraFieldMappings()).isEmpty();
    }

    @Test
    @DisplayName("getByConnectionId returns mapped webhook list")
    void getByConnectionId_returnsMapped() {
        UUID connectionId = UUID.randomUUID();
        JiraWebhook w = JiraWebhook.builder().id("w6").connectionId(connectionId).build();
        com.integration.management.model.dto.response.JiraWebhookDetailResponse resp =
            com.integration.management.model.dto.response.JiraWebhookDetailResponse.builder()
                .id("w6").build();
        when(jiraWebhookRepository.findByConnectionIdAndIsDeletedFalse(connectionId))
            .thenReturn(List.of(w));
        when(jiraWebhookMapper.toResponse(w)).thenReturn(resp);

        var out = jiraWebhookService.getByConnectionId(connectionId);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo("w6");
    }

    @Test
    @DisplayName("getAllNormalizedNamesByTenantId delegates to repository")
    void getAllNormalizedNamesByTenantId_delegates() {
        when(jiraWebhookRepository.findAllNormalizedNamesByTenantId("t1"))
            .thenReturn(List.of("wh-a", "wh-b"));
        List<String> out = jiraWebhookService.getAllNormalizedNamesByTenantId("t1");
        assertThat(out).containsExactly("wh-a", "wh-b");
    }

    @Test
    @DisplayName("toggleActiveStatus: null isEnabled treated as false, enables to true")
    void toggleActiveStatus_nullEnabled_enablesToTrue() {
        JiraWebhook webhook = JiraWebhook.builder()
                .id("w7").tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("n").webhookUrl("url").connectionId(UUID.randomUUID())
                .samplePayload("{}").isEnabled(null).build();
        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w7", "t1"))
                .thenReturn(Optional.of(webhook));

        boolean newStatus = jiraWebhookService.toggleActiveStatus("w7", "t1", "u2");
        assertThat(newStatus).isTrue();
        verify(jiraWebhookRepository).save(webhook);
    }

    @Test
    @DisplayName("create: duplicate key in loop retries and succeeds on second attempt")
    void create_duplicateKeyInLoop_retriesAndSucceeds() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("N").connectionId(UUID.randomUUID()).samplePayload("{}").build();
        JiraWebhook entity = JiraWebhook.builder().name("N").connectionId(request.getConnectionId())
                .samplePayload("{}").jiraFieldMappings(new ArrayList<>()).build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(3);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/{webhookId}");

        // First save throws DataAccessException with "duplicate key" in the cause,
        // triggering the retry loop. Second attempt succeeds.
        RuntimeException dupKeyCause = new RuntimeException("duplicate key value violates unique constraint");
        when(jiraWebhookRepository.save(any(JiraWebhook.class)))
                .thenThrow(new DataAccessResourceFailureException("DB error", dupKeyCause))
                .thenReturn(entity);

        JiraWebhook result = jiraWebhookService.create(request, "t1", "u1");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("create: DataIntegrityViolationException with unique constraint throws IntegrationNameAlreadyExistsException")
    void create_uniqueConstraintViolation_throwsNameAlreadyExists() {
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("N").connectionId(UUID.randomUUID()).samplePayload("{}").build();
        JiraWebhook entity = JiraWebhook.builder().name("N").connectionId(request.getConnectionId())
                .samplePayload("{}").jiraFieldMappings(new ArrayList<>()).build();

        when(jiraWebhookMapper.toEntity(request)).thenReturn(entity);
        when(jiraWebhookProperties.getMaxRetries()).thenReturn(1);
        when(jiraWebhookProperties.getIdLength()).thenReturn(8);
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/{webhookId}");

        // Cause chain has "unique constraint" text
        RuntimeException cause = new RuntimeException("unique constraint violated");
        when(jiraWebhookRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("constraint", cause));

        assertThatThrownBy(() -> jiraWebhookService.create(request, "t1", "u1"))
                .isInstanceOf(IntegrationNameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("getAllByTenantId: webhook with null jiraFieldMappings returns null in summary")
    void getAllByTenantId_nullMappings_returnsNullInSummary() {
        JiraWebhook w = JiraWebhook.builder()
                .id("w8").tenantId("t1").name("W8").webhookUrl("url")
                .isEnabled(true).isDeleted(false).createdBy("u")
                .createdDate(Instant.now())
                .jiraFieldMappings(null)  // explicitly null
                .build();
        JiraWebhookEvent ev = JiraWebhookEvent.builder()
                .id(UUID.randomUUID()).webhookId("w8")
                .triggeredBy("u").triggeredAt(Instant.now())
                .status(com.integration.execution.contract.model.enums.TriggerStatus.SUCCESS)
                .jiraIssueUrl("https://jira/KW-1").build();

        when(jiraWebhookRepository.findAllSummaryByTenantId("t1")).thenReturn(List.of(w));
        when(jiraWebhookEventRepository.findLatestEventsForWebhookIds(List.of("w8")))
                .thenReturn(List.of(ev));

        List<JiraWebhookSummaryResponse> result = jiraWebhookService.getAllByTenantId("t1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJiraFieldMappings()).isNull();
        assertThat(result.get(0).getLastEventHistory()).isNotNull();
        assertThat(result.get(0).getLastEventHistory().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getAllByTenantId: event with null status maps to null status string in summary")
    void getAllByTenantId_nullEventStatus_mapsToNullString() {
        JiraWebhook w = JiraWebhook.builder()
                .id("w9").tenantId("t1").name("W9").webhookUrl("url")
                .isEnabled(true).isDeleted(false).createdBy("u")
                .createdDate(Instant.now())
                .jiraFieldMappings(new ArrayList<>()).build();
        JiraWebhookEvent ev = JiraWebhookEvent.builder()
                .id(UUID.randomUUID()).webhookId("w9")
                .triggeredBy("u").triggeredAt(Instant.now())
                .status(null)  // null status
                .jiraIssueUrl(null).build();

        when(jiraWebhookRepository.findAllSummaryByTenantId("t1")).thenReturn(List.of(w));
        when(jiraWebhookEventRepository.findLatestEventsForWebhookIds(List.of("w9")))
                .thenReturn(List.of(ev));

        List<JiraWebhookSummaryResponse> result = jiraWebhookService.getAllByTenantId("t1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastEventHistory().getStatus()).isNull();
    }

    @Test
    @DisplayName("update: unchanged mappings do not trigger clear+add (no touch)")
    void update_unchangedMappings_doesNotClearAndAdd() {
        UUID connectionId = UUID.randomUUID();
        JiraFieldMapping existing = JiraFieldMapping.builder()
                .jiraFieldId("f1").jiraFieldName("Field1")
                .dataType(JiraDataType.STRING).displayLabel("L")
                .template("t").required(false).defaultValue("d").build();
        JiraWebhook webhook = JiraWebhook.builder()
                .id("w10").tenantId("t1").createdBy("u").lastModifiedBy("u")
                .name("W10").webhookUrl("url").connectionId(connectionId)
                .samplePayload("{}").jiraFieldMappings(new ArrayList<>(List.of(existing))).build();

        JiraFieldMappingDto dto = JiraFieldMappingDto.builder()
                .jiraFieldId("f1").jiraFieldName("Field1")
                .dataType(JiraDataType.STRING).displayLabel("L")
                .template("t").required(false).defaultValue("d").build();
        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("W10").connectionId(connectionId).samplePayload("{}")
                .fieldsMapping(List.of(dto)).build();

        JiraFieldMapping incomingMapping = JiraFieldMapping.builder()
                .jiraFieldId("f1").jiraFieldName("Field1")
                .dataType(JiraDataType.STRING).displayLabel("L")
                .template("t").required(false).defaultValue("d").build();

        when(jiraWebhookRepository.findByIdAndTenantIdAndIsDeletedFalse("w10", "t1"))
                .thenReturn(Optional.of(webhook));
        doNothing().when(jiraWebhookMapper).updateEntity(eq(webhook), eq(request));
        when(jiraFieldMappingMapper.toEntity(request.getFieldsMapping()))
                .thenReturn(List.of(incomingMapping));
        when(jiraWebhookProperties.getUrlTemplate()).thenReturn("https://host/{webhookId}");

        jiraWebhookService.update("w10", request, "t1", "u2");

        // Mappings are unchanged → list should still have original mapping
        assertThat(webhook.getJiraFieldMappings()).hasSize(1);
        verify(jiraWebhookRepository).save(webhook);
    }
}


