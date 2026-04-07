package com.integration.management.mapper;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.management.entity.JiraFieldMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JiraFieldMappingMapper {

    @Mapping(target = "jiraWebhook", ignore = true)
    JiraFieldMapping toEntity(JiraFieldMappingDto dto);

    @Mapping(target = "jiraWebhook", ignore = true)
    List<JiraFieldMapping> toEntity(List<JiraFieldMappingDto> dto);

    JiraFieldMappingDto toDto(JiraFieldMapping entity);
}
