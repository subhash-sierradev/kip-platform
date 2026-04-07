package com.integration.execution.contract.model;

import com.integration.execution.contract.model.enums.FieldTransformationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationFieldMappingDto {

    private UUID id;

    private UUID integrationId;

    @NotBlank(message = "Source field path is required")
    @Size(max = 100, message = "Source field path cannot exceed {max} characters")
    private String sourceFieldPath;

    @NotBlank(message = "Target field path is required")
    @Size(max = 100, message = "Target field path cannot exceed {max} characters")
    private String targetFieldPath;

    // Transformation configuration
    @NotNull(message = "Transformation type is required")
    private FieldTransformationType transformationType;
    private Map<String, Object> transformationConfig;

    @Builder.Default
    private Boolean isMandatory = false;
    private String defaultValue;

    private Integer displayOrder;
}