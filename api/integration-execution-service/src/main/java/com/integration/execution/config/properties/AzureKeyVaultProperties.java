package com.integration.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Azure Key Vault integration.
 * Maps to integration.azure.keyvault.* properties in application.yml
 */
@Data
@Component
@ConfigurationProperties(prefix = "integration.azure.keyvault")
public class AzureKeyVaultProperties {
    private String url;
    private String clientId;
    private String clientSecret;
    private String tenantId;
}
