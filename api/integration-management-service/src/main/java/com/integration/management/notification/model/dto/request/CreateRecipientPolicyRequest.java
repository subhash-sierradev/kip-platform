package com.integration.management.notification.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateRecipientPolicyRequest {

    @NotNull(message = "Rule ID is required")
    private UUID ruleId;

    @NotBlank(message = "Recipient type is required")
    private String recipientType;

    private List<String> userIds;
}
