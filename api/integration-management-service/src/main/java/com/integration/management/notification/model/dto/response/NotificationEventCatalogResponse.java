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
public class NotificationEventCatalogResponse {
    private UUID id;
    private String eventKey;
    private String entityType;
    private String displayName;
    private String description;
    private Boolean isEnabled;
    private Boolean notifyInitiator;
    private Instant createdDate;
}
