package com.integration.management.mapper;

import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.entity.IntegrationConnection;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IntegrationConnectionMapper {
    IntegrationConnectionResponse toResponse(IntegrationConnection entity);

    /**
     * Create a lightweight IntegrationConnectionResponse from an ApiResponse.
     * Used for early-return error cases (e.g., duplicate connection detected).
     */
    default IntegrationConnectionResponse toResponse(ApiResponse api) {
        return IntegrationConnectionResponse.builder()
                .lastConnectionStatus(ConnectionStatus.FAILED)
                .lastConnectionMessage(api.message())
                .lastConnectionTest(Instant.now())
                .build();
    }
}
