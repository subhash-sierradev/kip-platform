package com.integration.management.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Jira webhook settings.
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "integration-platform.jira.webhook")
public class JiraWebhookProperties {

    private int idLength = 11;
    private int maxRetries = 5;

    @NotBlank(message = "integration-platform.jira.webhook.url-template must not be blank")
    private String urlTemplate;
}
