package com.integration.management.mapper;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.management.entity.IntegrationFieldMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IntegrationFieldMappingMapper {
    IntegrationFieldMappingDto toDto(IntegrationFieldMapping integrationFieldMapping);

    IntegrationFieldMapping toEntity(IntegrationFieldMappingDto integrationFieldMappingDto);

    default List<IntegrationFieldMapping> toEntities(List<IntegrationFieldMappingDto> fieldMappings) {
        return fieldMappings.stream().map(this::toEntity).toList();
    }
}
