package com.integration.management.notification.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateNotificationRuleRequest {

    @NotEmpty
    @Valid
    private List<CreateNotificationRuleRequest> rules;

    @NotBlank
    private String recipientType;

    private List<String> userIds;
}
