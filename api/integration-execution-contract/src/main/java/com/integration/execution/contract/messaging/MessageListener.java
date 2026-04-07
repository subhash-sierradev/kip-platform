package com.integration.execution.contract.messaging;

/**
 * Platform-agnostic message listener abstraction.
 * <p>
 * Allows decoupling consumer business logic from framework-specific message consumption patterns.
 * RabbitMQ uses {@code @RabbitListener} with method-level dispatching;
 * Azure Service Bus may use polling or async processing.
 * </p>
 *
 * <h3>Usage Patterns:</h3>
 * <ul>
 *   <li>RabbitMQ: Listener adopters call {@link #onMessageReceived(String, Object)} from @RabbitListener methods</li>
 *   <li>Azure Service Bus: Listener implementations adapt polling/async reception to this interface</li>
 * </ul>
 *
 * @see MessagePublisher for related publish-side abstraction
 */
public interface MessageListener {

    /**
     * Handle a message received from a queue.
     *
     * @param queueName the queue name the message was received from
     * @param message the deserialized message payload object
     * @throws Exception if message processing fails
     */
    void onMessageReceived(String queueName, Object message) throws Exception;
}
