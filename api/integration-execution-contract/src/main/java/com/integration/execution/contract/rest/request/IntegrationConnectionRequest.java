package com.integration.execution.contract.rest.request;

import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IntegrationConnectionRequest(

        @NotBlank(message = "Name cannot be blank")
        @NotNull(message = "Name is required")
        String name,

        @NotNull(message = "Service type is required")
        ServiceType serviceType,

        @Valid
        @NotNull(message = "Credential details are required")
        IntegrationSecret integrationSecret
) {
}
