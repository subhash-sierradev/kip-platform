package com.integration.management.mapper;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.management.entity.JiraFieldMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraFieldMappingMapper")
class JiraFieldMappingMapperTest {

    @Test
    @DisplayName("toEntity/toDto cover id and metadata branches")
    void toEntityToDto_idAndMetadataBranches() {
        JiraFieldMappingMapper mapper = new JiraFieldMappingMapperImpl();

        assertThat(mapper.toDto(null)).isNull();
        assertThat(mapper.toEntity((JiraFieldMappingDto) null)).isNull();
        assertThat(mapper.toEntity((List<JiraFieldMappingDto>) null)).isNull();

        JiraFieldMappingDto dto = JiraFieldMappingDto.builder()
                .id("00000000-0000-0000-0000-000000000020")
                .jiraFieldId("field")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("t")
                .required(true)
                .defaultValue("dv")
                .metadata(Map.of("m", 1))
                .build();

        JiraFieldMapping entity = mapper.toEntity(dto);
        assertThat(entity.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(entity.getMetadata()).containsEntry("m", 1);

        JiraFieldMappingDto dtoNoIdNoMetadata = JiraFieldMappingDto.builder()
                .id(null)
                .jiraFieldId("f")
                .jiraFieldName("n")
                .dataType(JiraDataType.STRING)
                .required(false)
                .metadata(null)
                .build();

        JiraFieldMapping entityNoId = mapper.toEntity(dtoNoIdNoMetadata);
        assertThat(entityNoId.getId()).isNull();
        assertThat(entityNoId.getMetadata()).isNull();

        JiraFieldMapping entityWithNullId = JiraFieldMapping.builder()
                .id(null)
                .jiraFieldId("f")
                .jiraFieldName("n")
                .dataType(JiraDataType.STRING)
                .required(false)
                .metadata(null)
                .build();

        JiraFieldMappingDto backNoId = mapper.toDto(entityWithNullId);
        assertThat(backNoId.getId()).isNull();
        assertThat(backNoId.getMetadata()).isNull();

        JiraFieldMapping entityWithIdAndMeta = JiraFieldMapping.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000021"))
                .jiraFieldId("f")
                .jiraFieldName("n")
                .dataType(JiraDataType.STRING)
                .required(false)
                .metadata(Map.of("x", "y"))
                .build();

        JiraFieldMappingDto back = mapper.toDto(entityWithIdAndMeta);
        assertThat(back.getId()).isEqualTo("00000000-0000-0000-0000-000000000021");
        assertThat(back.getMetadata()).containsEntry("x", "y");
    }

    @Test
    @DisplayName("toEntity(List) with non-empty list covers for-loop branches")
    void toEntityList_nonEmptyList_convertsAllItems() {
        JiraFieldMappingMapper mapper = new JiraFieldMappingMapperImpl();

        JiraFieldMappingDto dto1 = JiraFieldMappingDto.builder()
                .id("00000000-0000-0000-0000-000000000030")
                .jiraFieldId("f1").jiraFieldName("Field1").displayLabel("L1")
                .dataType(JiraDataType.STRING).required(true).build();
        JiraFieldMappingDto dto2 = JiraFieldMappingDto.builder()
                .id(null)
                .jiraFieldId("f2").jiraFieldName("Field2").displayLabel("L2")
                .dataType(JiraDataType.NUMBER).required(false).build();

        List<JiraFieldMapping> result = mapper.toEntity(List.of(dto1, dto2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getJiraFieldId()).isEqualTo("f1");
        assertThat(result.get(1).getJiraFieldId()).isEqualTo("f2");
        assertThat(result.get(1).getId()).isNull();
    }
}
