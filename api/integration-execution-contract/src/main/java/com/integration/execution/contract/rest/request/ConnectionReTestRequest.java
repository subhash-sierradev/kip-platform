package com.integration.execution.contract.rest.request;

import com.integration.execution.contract.model.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConnectionReTestRequest(

        @NotBlank(message = "Secret name cannot be blank")
        String secretName,

        @NotNull(message = "Service type is required")
        ServiceType serviceType) {
}
