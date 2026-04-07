package com.integration.management.mapper;

import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntegrationConnectionMapper")
class IntegrationConnectionMapperTest {

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
