package com.integration.management.notification.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationTemplateRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotBlank(message = "Title template is required")
    @Size(max = 255, message = "Title template cannot exceed {max} characters")
    private String titleTemplate;

    @NotBlank(message = "Message template is required")
    private String messageTemplate;
}
