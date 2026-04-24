package com.integration.management.model.dto.request;

import com.integration.execution.contract.model.JiraFieldMappingDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraWebhookCreateUpdateRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name cannot exceed {max} characters")
    private String name;
    @Size(max = 500, message = "Description cannot exceed {max} characters")
    private String description;
    private UUID connectionId;
    @Valid
    @NotNull(message = "Fields mapping is required")
    @Size(min = 3, max = 100, message = "Fields mapping must contain between {min} and {max} items")
    private List<JiraFieldMappingDto> fieldsMapping;
    @NotBlank
    private String samplePayload;
    @Size(max = 50, message = "Requested tenant ID cannot exceed 50 characters")
    private String requestedTenantId;
}
