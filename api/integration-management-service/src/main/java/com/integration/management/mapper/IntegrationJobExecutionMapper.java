package com.integration.management.mapper;

import com.integration.management.entity.IntegrationJobExecution;
import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IntegrationJobExecutionMapper {

    IntegrationJobExecutionDto toDto(IntegrationJobExecution entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "triggeredBy", ignore = true)
    @Mapping(target = "triggeredByUser", ignore = true)
    @Mapping(target = "retryAttempt", ignore = true)
    @Mapping(target = "addedRecords", constant = "0")
    @Mapping(target = "updatedRecords", constant = "0")
    @Mapping(target = "failedRecords", constant = "0")
    @Mapping(target = "totalRecords", constant = "0")
    @Mapping(target = "addedRecordsMetadata", ignore = true)
    @Mapping(target = "updatedRecordsMetadata", ignore = true)
    @Mapping(target = "failedRecordsMetadata", ignore = true)
    @Mapping(target = "totalRecordsMetadata", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "executionMetadata", ignore = true)
    IntegrationJobExecution toRetryExecution(IntegrationJobExecution source);

}
