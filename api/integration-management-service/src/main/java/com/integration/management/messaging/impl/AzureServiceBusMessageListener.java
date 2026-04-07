package com.integration.management.messaging.impl;

import com.integration.execution.contract.messaging.MessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Azure Service Bus implementation stub of MessageListener for IMS.
 * <p>
 * Placeholder for future Azure Service Bus integration.
 * Throwing UnsupportedOperationException ensures intentional migration work is required.
 * Active only when {@code app.messaging.transport=azureservicebus}.
 * </p>
 *
 * @see com.integration.management.messaging.impl.RabbitMQMessageListener for RabbitMQ implementation
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "azureservicebus")
public class AzureServiceBusMessageListener implements MessageListener {

    public AzureServiceBusMessageListener() {
        log.warn(
                "Azure Service Bus messaging transport is selected, "
                        + "but listener implementation is still a stub.");
    }

    @Override
    public void onMessageReceived(String queueName, Object message) throws Exception {
        throw new UnsupportedOperationException(
                "Azure Service Bus MessageListener implementation is pending. "
                        + "Set app.messaging.transport=rabbitmq for RabbitMQ "
                        + "or implement this method for Azure Service Bus.");
    }
}
