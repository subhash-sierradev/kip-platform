package com.integration.execution.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.queue.QueueNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for RabbitMQ message publishing and queue interaction.
 *
 * <p>Uses a real RabbitMQ container to verify that the publisher correctly
 * sends messages to exchanges and that the queue receives them in the expected format.
 */
@DisplayName("Message Publisher — integration tests")
class MessagePublisherIT extends AbstractIesIT {

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("MessagePublisher publishes JiraWebhookExecutionResult to result queue")
    void publish_jiraResultToResultQueue_messageCanBeConsumed() throws Exception {
        UUID eventId = UUID.randomUUID();
        JiraWebhookExecutionResult result = JiraWebhookExecutionResult.builder()
                .triggerEventId(eventId)
                .success(true)
                .responseStatusCode(201)
                .responseBody("Created")
                .build();

        messagePublisher.publish(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE,
                result);

        // Receive the message from the queue (consume it)
        JiraWebhookExecutionResult received = await()
                .atMost(10, TimeUnit.SECONDS)
                .until(
                        () -> (JiraWebhookExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(received).isNotNull();
        assertThat(received.getTriggerEventId()).isEqualTo(eventId);
        assertThat(received.isSuccess()).isTrue();
        assertThat(received.getResponseStatusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("MessagePublisher.publishDirect sends message directly to queue")
    void publishDirect_toResultQueue_messageReceived() {
        UUID eventId = UUID.randomUUID();
        JiraWebhookExecutionResult result = JiraWebhookExecutionResult.failure(
                eventId, "test-error-message");

        messagePublisher.publishDirect(
                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE,
                result);

        JiraWebhookExecutionResult received = await()
                .atMost(10, TimeUnit.SECONDS)
                .until(
                        () -> (JiraWebhookExecutionResult) rabbitTemplate.receiveAndConvert(
                                QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE),
                        r -> r != null);

        assertThat(received).isNotNull();
        assertThat(received.getTriggerEventId()).isEqualTo(eventId);
        assertThat(received.isSuccess()).isFalse();
        assertThat(received.getErrorMessage()).contains("test-error-message");
    }

    @Test
    @DisplayName("RabbitTemplate can receive and convert JSON message from queue")
    void rabbitTemplate_receiveAndConvert_deserializesCorrectly() throws Exception {
        UUID eventId = UUID.randomUUID();
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder()
                .webhookId("wh-it-01")
                .triggerEventId(eventId)
                .tenantId("it-tenant")
                .incomingPayload("{\"issue\":{\"key\":\"TEST-1\"}}")
                .fieldMappings(Collections.emptyList())
                .build();

        String json = objectMapper.writeValueAsString(command);
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setHeader("__TypeId__", JiraWebhookExecutionCommand.class.getName());
        Message amqpMessage = new Message(json.getBytes(), props);

        rabbitTemplate.send(
                QueueNames.JIRA_WEBHOOK_EXCHANGE,
                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE,
                amqpMessage);

        JiraWebhookExecutionCommand received = await()
                .atMost(10, TimeUnit.SECONDS)
                .until(
                        () -> (JiraWebhookExecutionCommand) rabbitTemplate.receiveAndConvert(
                                QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE),
                        r -> r != null);

        assertThat(received).isNotNull();
        assertThat(received.getWebhookId()).isEqualTo("wh-it-01");
        assertThat(received.getTenantId()).isEqualTo("it-tenant");
    }
}

