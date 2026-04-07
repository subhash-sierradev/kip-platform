package com.integration.execution.config;

import com.integration.execution.contract.messaging.MessageListener;
import com.integration.execution.contract.messaging.MessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Messaging abstraction configuration.
 * <p>
 * Ensures that the active transport-specific messaging implementations
 * (RabbitMQMessagePublisher/Listener or AzureServiceBusMessagePublisher/Listener)
 * are available for dependency injection throughout the application.
 * </p>
 *
 * <h3>Transport Behavior:</h3>
 * <ul>
 *   <li>{@code app.messaging.transport=rabbitmq} (default): Uses RabbitMQ implementations</li>
 *   <li>{@code app.messaging.transport=azureservicebus}: Uses Azure stub implementations (see pending)</li>
 * </ul>
 *
 * @see MessagePublisher for publishing abstraction
 * @see MessageListener for listening abstraction
 * @see RabbitMQConfig for RabbitMQ-specific beans
 * @see AzureServiceBusConfig for Azure-specific beans (stub)
 */
@Slf4j
@Configuration
public class MessagingConfig {

    /**
     * Constructor logs which messaging implementation is active.
     */
    public MessagingConfig(
            MessagePublisher messagePublisher,
            MessageListener messageListener,
            @Value("${app.messaging.transport:rabbitmq}") String configuredTransport) {

        String publisherType = messagePublisher != null
                ? messagePublisher.getClass().getSimpleName()
                : "NONE";
        String listenerType = messageListener != null
                ? messageListener.getClass().getSimpleName()
                : "NONE";

        log.info("Messaging abstraction initialized - transport: {}, Publisher: {}, Listener: {}",
                configuredTransport, publisherType, listenerType);
    }
}
