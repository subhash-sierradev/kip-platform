package com.integration.management.mapper;

import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.model.enums.FetchMode;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.entity.IntegrationConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntegrationConnectionMapper")
class IntegrationConnectionMapperTest {

    @Test
    @DisplayName("toResponse(IntegrationConnection) maps id and key fields")
    void toResponse_entity_mapsIdAndFields() {
        IntegrationConnectionMapper mapper = new IntegrationConnectionMapperImpl();

        IntegrationConnection entity = IntegrationConnection.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000021"))
                .name("ArcGIS")
                .secretName("sec-1")
                .serviceType(ServiceType.ARCGIS)
                .fetchMode(FetchMode.GET)
                .lastConnectionStatus(ConnectionStatus.SUCCESS)
                .lastConnectionMessage("ok")
                .lastConnectionTest(Instant.parse("2026-03-01T00:00:00Z"))
                .tenantId("tenant-1")
                .isDeleted(false)
                .version(2L)
                .createdBy("user")
                .createdDate(Instant.parse("2026-02-01T00:00:00Z"))
                .lastModifiedBy("user2")
                .lastModifiedDate(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        IntegrationConnectionResponse response = mapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo("00000000-0000-0000-0000-000000000021");
        assertThat(response.getName()).isEqualTo("ArcGIS");
        assertThat(response.getLastConnectionStatus()).isEqualTo(ConnectionStatus.SUCCESS);
    }

    @Test
    @DisplayName("toResponse(IntegrationConnection) handles nulls")
    void toResponse_entity_nullBranches() {
        IntegrationConnectionMapper mapper = new IntegrationConnectionMapperImpl();

        assertThat(mapper.toResponse((IntegrationConnection) null)).isNull();

        IntegrationConnection entity = IntegrationConnection.builder()
                .id(null)
                .name("ArcGIS")
                .secretName("sec-1")
                .serviceType(ServiceType.ARCGIS)
                .fetchMode(FetchMode.GET)
                .build();

        IntegrationConnectionResponse response = mapper.toResponse(entity);
        assertThat(response.getId()).isNull();
        assertThat(response.getSecretName()).isEqualTo("sec-1");
    }

    @Test
    @DisplayName("toResponse(ApiResponse) creates failed response with message and timestamp")
    void toResponse_apiResponse_buildsFailedResponse() {
        IntegrationConnectionMapper mapper = new IntegrationConnectionMapperImpl();

        ApiResponse api = new ApiResponse(409, false, "Duplicate connection");

        IntegrationConnectionResponse response = mapper.toResponse(api);

        assertThat(response).isNotNull();
        assertThat(response.getLastConnectionStatus()).isEqualTo(ConnectionStatus.FAILED);
        assertThat(response.getLastConnectionMessage()).isEqualTo("Duplicate connection");
        assertThat(response.getLastConnectionTest()).isNotNull();
    }
}
