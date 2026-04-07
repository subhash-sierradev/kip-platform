package com.integration.management.notification.model.dto.request;

import com.integration.execution.contract.model.enums.NotificationSeverity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRuleRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Severity is required")
    private NotificationSeverity severity;

    @Builder.Default
    private Boolean isEnabled = true;
}
