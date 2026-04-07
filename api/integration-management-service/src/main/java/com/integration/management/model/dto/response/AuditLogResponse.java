package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.EntityType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AuditLogResponse {
    private UUID id;
    private EntityType entityType;
    private String entityId;
    private String entityName;
    private String action;
    private AuditResult result;
    private String performedBy;
    private String tenantId;
    private String clientIpAddress;

    //
    private String tenantName;
    private Instant timestamp;
    private String details;
}
