package com.integration.management.messaging.impl;

import com.integration.execution.contract.messaging.MessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of MessagePublisher for IMS.
 * <p>
 * Delegates all publish operations to RabbitTemplate for delivery to RabbitMQ exchanges and queues.
 * Active only when {@code app.messaging.transport=rabbitmq}.
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMQMessagePublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish to a DirectExchange using routing key.
     *
     * @param exchange the RabbitMQ DirectExchange name
     * @param routingKey the routing key for message delivery
     * @param payload the message payload (serialized to JSON by MessageConverter)
     */
    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        try {
            String payloadType = (payload != null) ? payload.getClass().getSimpleName() : "null";
            log.debug("Publishing message to exchange='{}', routingKey='{}', payloadType={}",
                    exchange, routingKey, payloadType);
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish message to exchange='{}', routingKey='{}'",
                    exchange, routingKey, e);
            throw new RuntimeException("Failed to publish message to RabbitMQ", e);
        }
    }

    /**
     * Publish directly to a queue using the default exchange.
     *
     * @param queueName the RabbitMQ queue name
     * @param payload the message payload (serialized to JSON by MessageConverter)
     */
    @Override
    public void publishDirect(String queueName, Object payload) {
        try {
            String payloadType = (payload != null) ? payload.getClass().getSimpleName() : "null";
            log.debug("Publishing message directly to queue='{}', payloadType={}",
                    queueName, payloadType);
            rabbitTemplate.convertAndSend(queueName, payload);
        } catch (Exception e) {
            log.error("Failed to publish message directly to queue='{}'", queueName, e);
            throw new RuntimeException("Failed to publish message to RabbitMQ", e);
        }
    }
}
