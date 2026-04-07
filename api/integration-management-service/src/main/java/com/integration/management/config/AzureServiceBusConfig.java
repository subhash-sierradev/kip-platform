package com.integration.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Azure Service Bus messaging configuration stub for IMS.
 * <p>
 * Placeholder for future Azure Service Bus integration.
 * Currently empty; will be populated when Azure Service Bus implementation is added.
 * Active only when {@code app.messaging.transport=azureservicebus}.
 * </p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "azureservicebus")
public class AzureServiceBusConfig {

    public AzureServiceBusConfig() {
        log.warn("Azure Service Bus transport is configured, but infrastructure wiring is currently a stub.");
    }
}
