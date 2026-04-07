package com.integration.management.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Jira webhook settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "integration.jira.webhook")
public class JiraWebhookProperties {

    private int idLength = 11;
    private int maxRetries = 5;
    private String urlTemplate = "https://kaseware.sierradev.com/api/webhooks/jira/execute/{webhookId}";
}
