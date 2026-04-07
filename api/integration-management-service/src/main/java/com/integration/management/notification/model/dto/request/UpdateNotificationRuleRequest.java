package com.integration.management.notification.model.dto.request;

import com.integration.execution.contract.model.enums.NotificationSeverity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationRuleRequest {

    @NotNull
    private NotificationSeverity severity;
}
