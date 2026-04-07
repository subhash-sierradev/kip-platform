package com.integration.management.messaging.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQMessagePublisher")
class RabbitMQMessagePublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMQMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMQMessagePublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("publish should delegate to RabbitTemplate with non-null payload")
    void publish_nonNullPayload_delegatesToRabbitTemplate() {
        String payload = "test-message";
        publisher.publish("exchange", "routing.key", payload);

        verify(rabbitTemplate).convertAndSend("exchange", "routing.key", payload);
    }

    @Test
    @DisplayName("publish should delegate to RabbitTemplate with null payload")
    void publish_nullPayload_delegatesToRabbitTemplate() {
        publisher.publish("exchange", "routing.key", null);

        verify(rabbitTemplate).convertAndSend("exchange", "routing.key", (Object) null);
    }

    @Test
    @DisplayName("publish should wrap RabbitTemplate exception in RuntimeException")
    void publish_rabbitTemplateThrows_wrapsInRuntimeException() {
        doThrow(new RuntimeException("connection lost"))
                .when(rabbitTemplate).convertAndSend("exchange", "key", "payload");

        assertThatThrownBy(() -> publisher.publish("exchange", "key", "payload"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish message to RabbitMQ")
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("connection lost");
    }

    @Test
    @DisplayName("publish should handle complex payload objects")
    void publish_complexPayload_delegatesToRabbitTemplate() {
        TestPayload payload = new TestPayload("id-123", "value");
        publisher.publish("test.exchange", "test.routing.key", payload);

        verify(rabbitTemplate).convertAndSend("test.exchange", "test.routing.key", payload);
    }

    @Test
    @DisplayName("publishDirect should delegate to RabbitTemplate with non-null payload")
    void publishDirect_nonNullPayload_delegatesToRabbitTemplate() {
        String payload = "direct-message";
        publisher.publishDirect("queue-name", payload);

        verify(rabbitTemplate).convertAndSend("queue-name", payload);
    }

    @Test
    @DisplayName("publishDirect should delegate to RabbitTemplate with null payload")
    void publishDirect_nullPayload_delegatesToRabbitTemplate() {
        publisher.publishDirect("queue-name", null);

        verify(rabbitTemplate).convertAndSend("queue-name", (Object) null);
    }

    @Test
    @DisplayName("publishDirect should wrap RabbitTemplate exception in RuntimeException")
    void publishDirect_rabbitTemplateThrows_wrapsInRuntimeException() {
        doThrow(new RuntimeException("connection lost"))
                .when(rabbitTemplate).convertAndSend("queue-name", "payload");

        assertThatThrownBy(() -> publisher.publishDirect("queue-name", "payload"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish message to RabbitMQ")
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("connection lost");
    }

    @Test
    @DisplayName("publishDirect should handle complex payload objects")
    void publishDirect_complexPayload_delegatesToRabbitTemplate() {
        TestPayload payload = new TestPayload("id-456", "direct-value");
        publisher.publishDirect("test.queue", payload);

        verify(rabbitTemplate).convertAndSend("test.queue", payload);
    }

    @Test
    @DisplayName("publish should handle AmqpException from RabbitTemplate")
    void publish_amqpException_wrapsInRuntimeException() {
        org.springframework.amqp.AmqpException amqpException =
                new org.springframework.amqp.AmqpException("AMQP error");
        doThrow(amqpException)
                .when(rabbitTemplate).convertAndSend("exchange", "key", "payload");

        assertThatThrownBy(() -> publisher.publish("exchange", "key", "payload"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish message to RabbitMQ")
                .hasCause(amqpException);
    }

    @Test
    @DisplayName("publishDirect should handle AmqpException from RabbitTemplate")
    void publishDirect_amqpException_wrapsInRuntimeException() {
        org.springframework.amqp.AmqpException amqpException =
                new org.springframework.amqp.AmqpException("AMQP error");
        doThrow(amqpException)
                .when(rabbitTemplate).convertAndSend("queue", "payload");

        assertThatThrownBy(() -> publisher.publishDirect("queue", "payload"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish message to RabbitMQ")
                .hasCause(amqpException);
    }

    // Test payload class
    private static class TestPayload {
        private final String id;
        private final String value;

        TestPayload(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }
}
