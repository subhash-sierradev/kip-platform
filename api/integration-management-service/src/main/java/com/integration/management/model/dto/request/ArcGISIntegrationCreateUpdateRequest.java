package com.integration.management.model.dto.request;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.management.model.validation.ValidArcGISFieldMappings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArcGISIntegrationCreateUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed {max} characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed {max} characters")
    private String description;

    @NotBlank(message = "Item type is required")
    @Pattern(
            regexp = "DOCUMENT",
            message = "Item type must be DOCUMENT"
    )
    private String itemType;

    @NotBlank(message = "Item sub type is required")
    @Size(max = 100, message = "Item sub type cannot exceed {max} characters")
    private String itemSubtype;

    @Size(max = 100, message = "Dynamic document type cannot exceed {max} characters")
    private String dynamicDocumentType;

    @Size(max = 100, message = "Dynamic document type label cannot exceed {max} characters")
    private String dynamicDocumentTypeLabel;

    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @NotNull(message = "Schedule is required")
    @Valid
    private IntegrationScheduleRequest schedule;

    @Valid
    @ValidArcGISFieldMappings
    @Builder.Default
    private List<IntegrationFieldMappingDto> fieldMappings = new ArrayList<>();

}
