package com.integration.execution.contract.rest.response;

import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.model.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationConnectionResponse {
    private String id;
    private String name;
    private String secretName;
    private String tenantId;
    private Long version;

    private ServiceType serviceType;
    private Boolean isDeleted;

    private ConnectionStatus lastConnectionStatus;
    private String lastConnectionMessage;
    private Instant lastConnectionTest;

    // Audit fields
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
}
