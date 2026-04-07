package com.integration.execution.messaging.impl;

import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.messaging.MessageListener;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.messaging.ArcGISExecutionCommandListener;
import com.integration.execution.messaging.ConfluenceExecutionCommandListener;
import com.integration.execution.messaging.JiraWebhookExecutionCommandListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of MessageListener.
 * <p>
 * Centralizes RabbitMQ-specific {@code @RabbitListener} bindings and delegates
 * deserialized command payloads to transport-agnostic business handlers.
 * </p>
 * <p>
 * Active only when {@code app.messaging.transport=rabbitmq}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMQMessageListener implements MessageListener {

    private final ArcGISExecutionCommandListener arcGISExecutionCommandListener;
    private final ConfluenceExecutionCommandListener confluenceExecutionCommandListener;
    private final JiraWebhookExecutionCommandListener jiraWebhookExecutionCommandListener;

    @RabbitListener(queues = QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE)
    public void onArcGISExecutionCommand(final ArcGISExecutionCommand command) throws Exception {
        onMessageReceived(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE, command);
    }

    @RabbitListener(queues = QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE)
    public void onConfluenceExecutionCommand(final ConfluenceExecutionCommand command) throws Exception {
        onMessageReceived(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE, command);
    }

    @RabbitListener(queues = QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE)
    public void onJiraWebhookExecutionCommand(final JiraWebhookExecutionCommand command) throws Exception {
        onMessageReceived(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE, command);
    }

    /**
     * Routes a message received from RabbitMQ to the correct business handler.
     *
     * @param queueName the queue name
     * @param message the message payload
     * @throws Exception if message processing fails
     */
    @Override
    public void onMessageReceived(String queueName, Object message) throws Exception {
        if (message == null) {
            log.warn("RabbitMQMessageListener.onMessageReceived - queue='{}', message is null", queueName);
            throw new IllegalArgumentException("Received null message payload for queue: " + queueName);
        }
        log.debug("RabbitMQMessageListener.onMessageReceived - queue='{}', message type={}",
                queueName, message.getClass().getSimpleName());

        switch (queueName) {
            case QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE ->
                    arcGISExecutionCommandListener.handleExecutionCommand((ArcGISExecutionCommand) message);
            case QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE ->
                    confluenceExecutionCommandListener.handleExecutionCommand((ConfluenceExecutionCommand) message);
            case QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE ->
                    jiraWebhookExecutionCommandListener.handleExecutionCommand((JiraWebhookExecutionCommand) message);
            default -> throw new IllegalArgumentException("Unsupported RabbitMQ queue: " + queueName);
        }
    }
}
