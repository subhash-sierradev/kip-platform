package com.integration.management.mapper;

import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import com.integration.execution.contract.rest.response.IntegrationScheduleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper interface for converting between IntegrationSchedule entities and their corresponding DTOs.
 * Utilizes MapStruct for automatic generation of mapping implementations.
 * UI Will handle utc to convert to local time, so all times in the service are stored and processed in UTC.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IntegrationSchedulerMapper {

    IntegrationScheduleResponse toResponse(IntegrationSchedule integrationSchedule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "processedUntil", ignore = true)
    IntegrationSchedule toEntity(IntegrationScheduleRequest scheduleRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "processedUntil", ignore = true)
    void updateEntity(IntegrationScheduleRequest scheduleDto, @MappingTarget IntegrationSchedule schedule);
}
