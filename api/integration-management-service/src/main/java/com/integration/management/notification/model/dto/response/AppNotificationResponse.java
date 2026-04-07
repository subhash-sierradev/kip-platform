package com.integration.management.notification.model.dto.response;

import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppNotificationResponse {

    private UUID id;
    private String tenantId;
    private String userId;
    private NotificationEventKey type;
    private NotificationSeverity severity;
    private String title;
    private String message;
    private Map<String, Object> metadata;
    private Boolean isRead;
    private Instant createdDate;
}
