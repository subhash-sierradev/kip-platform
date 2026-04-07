package com.integration.execution.contract.rest.request;

import com.integration.execution.contract.model.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;

public record IntegrationConnectionSecretRotateRequest(
        String secretName,
        ServiceType serviceType,
        @NotBlank(message = "New secret cannot be blank")
        String newSecret) {
}
