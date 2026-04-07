package com.integration.execution.contract.messaging;

/**
 * Platform-agnostic messaging publisher abstraction.
 * <p>
 * Hides differences between RabbitMQ and Azure Service Bus publish operations.
 * Implementations are selected and routed to platform-specific transports
 * based on the {@code app.messaging.transport} Spring property (via {@code @ConditionalOnProperty}).
 * </p>
 *
 * <h3>RabbitMQ Interpretation:</h3>
 * <ul>
 *   <li>{@link #publish(String, String, Object)}: Sends message to DirectExchange with routing key</li>
 *   <li>{@link #publishDirect(String, Object)}: Sends directly to named queue (default exchange)</li>
 * </ul>
 *
 * <h3>Azure Service Bus Interpretation:</h3>
 * <ul>
 *   <li>{@link #publish(String, String, Object)}: Sends to Topic with routing/subscription rules</li>
 *   <li>{@link #publishDirect(String, Object)}: Sends directly to named Queue</li>
 * </ul>
 */
public interface MessagePublisher {

    /**
     * Publish a message to an exchange/topic with a routing key.
     *
     * @param exchange the exchange name (RabbitMQ) or topic name (Azure Service Bus)
     * @param routingKey the routing key (RabbitMQ) or subscription rule (Azure Service Bus)
     * @param payload the message payload object (will be serialized to JSON)
     * @throws RuntimeException if publish fails
     */
    void publish(String exchange, String routingKey, Object payload);

    /**
     * Publish a message directly to a queue/destination without routing.
     * Uses default exchange (RabbitMQ) or direct queue (Azure Service Bus).
     *
     * @param queueName the queue name (RabbitMQ) or queue name (Azure Service Bus)
     * @param payload the message payload object (will be serialized to JSON)
     * @throws RuntimeException if publish fails
     */
    void publishDirect(String queueName, Object payload);
}
