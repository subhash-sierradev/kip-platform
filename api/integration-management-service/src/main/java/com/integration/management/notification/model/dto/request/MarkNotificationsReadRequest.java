package com.integration.management.notification.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkNotificationsReadRequest {

    @NotEmpty(message = "At least one notification ID is required")
    private List<UUID> notificationIds;
}
