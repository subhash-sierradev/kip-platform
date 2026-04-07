package com.integration.execution.messaging.impl;

import com.integration.execution.contract.messaging.MessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Azure Service Bus implementation stub of MessagePublisher.
 * <p>
 * Placeholder for future Azure Service Bus integration.
 * Throwing UnsupportedOperationException ensures intentional migration work is required.
 * Active only when {@code app.messaging.transport=azureservicebus}.
 * </p>
 *
 * @see com.integration.execution.messaging.impl.RabbitMQMessagePublisher for RabbitMQ implementation
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "azureservicebus")
public class AzureServiceBusMessagePublisher implements MessagePublisher {

    public AzureServiceBusMessagePublisher() {
        log.warn(
                "Azure Service Bus messaging transport is selected, "
                        + "but publisher implementation is still a stub.");
    }

    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        throw new UnsupportedOperationException(
                "Azure Service Bus MessagePublisher implementation is pending. "
                        + "Set app.messaging.transport=rabbitmq for RabbitMQ "
                        + "or implement this method for Azure Service Bus.");
    }

    @Override
    public void publishDirect(String queueName, Object payload) {
        throw new UnsupportedOperationException(
                "Azure Service Bus MessagePublisher implementation is pending. "
                        + "Set app.messaging.transport=rabbitmq for RabbitMQ "
                        + "or implement this method for Azure Service Bus.");
    }
}
