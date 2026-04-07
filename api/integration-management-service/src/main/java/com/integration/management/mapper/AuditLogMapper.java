package com.integration.management.mapper;

import com.integration.management.entity.AuditLog;
import com.integration.management.model.dto.response.AuditLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "details", source = "metadata")
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "clientIpAddress", source = "clientIpAddress")
    AuditLogResponse toResponse(AuditLog auditLog);

    // Helper method for Map → String conversion
    default String mapMetadataToString(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return metadata.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
