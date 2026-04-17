package com.integration.management.model.dto.response;

import com.integration.execution.contract.rest.response.IntegrationScheduleResponse;
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
public class ArcGISIntegrationResponse {
    private UUID id;
    private String name;
    private String normalizedName;
    private String description;
    private String itemType;
    private String itemSubtype;
    private String itemSubtypeLabel;
    private String dynamicDocumentType;
    private String dynamicDocumentTypeLabel;

    private String connectionId;
    private String tenantId;
    private Boolean isEnabled;

    private IntegrationScheduleResponse schedule;

    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;

    private Instant nextRunAtUtc;

    private Long version;
}
