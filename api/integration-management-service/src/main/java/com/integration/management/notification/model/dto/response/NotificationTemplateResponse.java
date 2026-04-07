package com.integration.management.notification.model.dto.response;

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
public class NotificationTemplateResponse {

    private UUID id;
    private String tenantId;
    private UUID eventId;
    private String eventKey;
    private String titleTemplate;
    private String messageTemplate;
    private Instant createdDate;
}
