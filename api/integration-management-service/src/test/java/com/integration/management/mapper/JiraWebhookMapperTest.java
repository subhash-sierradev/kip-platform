package com.integration.management.mapper;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.management.entity.JiraFieldMapping;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.model.dto.request.JiraWebhookCreateUpdateRequest;
import com.integration.management.model.dto.response.JiraWebhookDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookMapper")
class JiraWebhookMapperTest {

    @Test
    @DisplayName("null inputs return null / no-op")
    void nullInputs_returnNullOrNoop() {
        JiraWebhookMapper mapper = new JiraWebhookMapperImpl();

        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();

        JiraWebhook existing = JiraWebhook.builder()
                .name("Old")
                .normalizedName("old")
                .build();

        mapper.updateEntity(existing, null);
        assertThat(existing.getName()).isEqualTo("Old");
        assertThat(existing.getNormalizedName()).isEqualTo("old");
    }

    @Test
    @DisplayName("toEntity normalizes name")
    void toEntity_normalizesName() {
        JiraWebhookMapper mapper = new JiraWebhookMapperImpl();

        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name("  Hello, World!!  ")
                .description("d")
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .samplePayload("{}")
                .build();

        JiraWebhook entity = mapper.toEntity(request);
        assertThat(entity.getName()).isEqualTo("  Hello, World!!  ");
        assertThat(entity.getNormalizedName()).isEqualTo("hello_world");
    }

    @Test
    @DisplayName("updateEntity updates and normalizes name")
    void updateEntity_updatesAndNormalizes() {
        JiraWebhookMapper mapper = new JiraWebhookMapperImpl();

        JiraWebhook existing = JiraWebhook.builder()
                .name("Old")
                .normalizedName("old")
                .build();

        JiraWebhookCreateUpdateRequest request = JiraWebhookCreateUpdateRequest.builder()
                .name(" New Name ")
                .description("desc")
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .samplePayload("payload")
                .build();

        mapper.updateEntity(existing, request);
        assertThat(existing.getName()).isEqualTo(" New Name ");
        assertThat(existing.getNormalizedName()).isEqualTo("new_name");
        assertThat(existing.getDescription()).isEqualTo("desc");
    }

    @Test
    @DisplayName("toResponse maps nested field mappings")
    void toResponse_mapsFieldMappings() {
        JiraWebhookMapper mapper = new JiraWebhookMapperImpl();

        JiraFieldMapping field = JiraFieldMapping.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000020"))
                .jiraFieldId("customfield_1")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .required(true)
                .metadata(Map.of("m", 1))
                .build();

        JiraWebhook webhook = JiraWebhook.builder()
                .id("wh-1")
                .name("Name")
                .description("Desc")
                .webhookUrl("https://example")
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .samplePayload("{}")
                .jiraFieldMappings(List.of(field))
                .normalizedName("name")
                .isEnabled(true)
                .isDeleted(false)
                .tenantId("t")
                .createdBy("u")
                .createdDate(Instant.parse("2026-02-17T00:00:00Z"))
                .version(1L)
                .build();

        JiraWebhookDetailResponse response = mapper.toResponse(webhook);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("wh-1");
        assertThat(response.getJiraFieldMappings()).hasSize(1);

        JiraFieldMappingDto mappedField = response.getJiraFieldMappings().get(0);
        assertThat(mappedField.getId()).isEqualTo("00000000-0000-0000-0000-000000000020");
        assertThat(mappedField.getJiraFieldId()).isEqualTo("customfield_1");
        assertThat(mappedField.getMetadata()).containsEntry("m", 1);
    }

    @Test
    @DisplayName("toResponse handles null connectionId and null field-mapping list")
    void toResponse_nullConnectionId_andNullList() {
        JiraWebhookMapper mapper = new JiraWebhookMapperImpl();

        JiraWebhook webhook = JiraWebhook.builder()
                .id("wh-1")
                .name("Name")
                .connectionId(null)
                .jiraFieldMappings(null)
                .build();

        JiraWebhookDetailResponse response = mapper.toResponse(webhook);
        assertThat(response.getConnectionId()).isNull();
        assertThat(response.getJiraFieldMappings()).isNull();
    }

    @Test
    @DisplayName("toResponse maps field mapping with null id and null metadata")
    void toResponse_fieldMappingNullBranches() {
        JiraWebhookMapper mapper = new JiraWebhookMapperImpl();

        JiraFieldMapping field = JiraFieldMapping.builder()
                .id(null)
                .jiraFieldId("customfield_1")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .required(false)
                .metadata(null)
                .build();

        JiraWebhook webhook = JiraWebhook.builder()
                .id("wh-1")
                .name("Name")
                .jiraFieldMappings(List.of(field))
                .build();

        JiraWebhookDetailResponse response = mapper.toResponse(webhook);
        assertThat(response.getJiraFieldMappings()).hasSize(1);
        assertThat(response.getJiraFieldMappings().get(0).getId()).isNull();
        assertThat(response.getJiraFieldMappings().get(0).getMetadata()).isNull();
    }
}
