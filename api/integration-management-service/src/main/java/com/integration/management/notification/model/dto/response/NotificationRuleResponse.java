package com.integration.management.notification.model.dto.response;

import com.integration.execution.contract.model.enums.NotificationSeverity;
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
public class NotificationRuleResponse {

    private UUID id;
    private String tenantId;
    private UUID eventId;
    private String eventKey;
    private String entityType;
    private NotificationSeverity severity;
    private Boolean isEnabled;
    private Instant createdDate;
    private String createdBy;
    private Instant lastModifiedDate;
    private String lastModifiedBy;
}
