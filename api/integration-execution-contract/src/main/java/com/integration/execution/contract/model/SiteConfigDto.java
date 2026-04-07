package com.integration.execution.contract.model;

import com.integration.execution.contract.model.enums.ConfigValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfigDto {

    private UUID id;

    @NotBlank(message = "Config key is required")
    @Size(max = 100, message = "Key must be at most {max} characters long")
    private String configKey;

    @NotBlank(message = "Config value is required")
    @Size(max = 500, message = "Value must be at most {max} characters long")
    private String configValue;

    @NotNull(message = "Value type is required")
    private ConfigValueType type;

    @Size(max = 200, message = "Description must be at most {max} characters long")
    private String description;

    private String tenantId;
    private Instant createdDate;
    private Instant lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;
    @Builder.Default
    private Boolean isDeleted = false;
}
