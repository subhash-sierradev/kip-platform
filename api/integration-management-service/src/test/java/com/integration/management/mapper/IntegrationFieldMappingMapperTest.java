package com.integration.management.mapper;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import com.integration.management.entity.IntegrationFieldMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntegrationFieldMappingMapper")
class IntegrationFieldMappingMapperTest {

    @Test
    @DisplayName("toDto/toEntity handle null and map cloning")
    void toDtoToEntity_nullAndMapBranches() {
        IntegrationFieldMappingMapper mapper = new IntegrationFieldMappingMapperImpl();

        assertThat(mapper.toDto(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();

        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID integrationId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        IntegrationFieldMapping entity = IntegrationFieldMapping.builder()
                .id(id)
                .integrationId(integrationId)
                .sourceFieldPath("a")
                .targetFieldPath("b")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .transformationConfig(Map.of("k", "v"))
                .isMandatory(true)
                .defaultValue("d")
                .displayOrder(1)
                .build();

        IntegrationFieldMappingDto dto = mapper.toDto(entity);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getTransformationConfig()).containsEntry("k", "v");

        IntegrationFieldMappingDto dtoNoMap = IntegrationFieldMappingDto.builder()
                .id(id)
                .integrationId(integrationId)
                .sourceFieldPath("a")
                .targetFieldPath("b")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .transformationConfig(null)
                .isMandatory(false)
                .build();

        IntegrationFieldMapping mappedNoMap = mapper.toEntity(dtoNoMap);
        assertThat(mappedNoMap.getTransformationConfig()).isNull();

        IntegrationFieldMapping mappedWithMap = mapper.toEntity(dto);
        assertThat(mappedWithMap.getTransformationConfig()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("toEntities maps list")
    void toEntities_mapsList() {
        IntegrationFieldMappingMapper mapper = new IntegrationFieldMappingMapperImpl();

        IntegrationFieldMappingDto dto = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("a")
                .targetFieldPath("b")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .build();

        List<IntegrationFieldMapping> entities = mapper.toEntities(List.of(dto));
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getSourceFieldPath()).isEqualTo("a");
    }
}
