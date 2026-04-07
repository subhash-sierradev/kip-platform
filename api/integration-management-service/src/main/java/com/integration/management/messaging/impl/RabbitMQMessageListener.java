package com.integration.management.messaging.impl;

import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessageListener;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.management.messaging.ArcGISExecutionResultListener;
import com.integration.management.messaging.ConfluenceExecutionResultListener;
import com.integration.management.messaging.JiraWebhookExecutionResultListener;
import com.integration.management.notification.messaging.NotificationListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of MessageListener for IMS.
 * <p>
 * Centralizes RabbitMQ-specific {@code @RabbitListener} bindings and delegates
 * result and notification payloads to transport-agnostic business handlers.
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

    private final ArcGISExecutionResultListener arcGISExecutionResultListener;
    private final ConfluenceExecutionResultListener confluenceExecutionResultListener;
    private final JiraWebhookExecutionResultListener jiraWebhookExecutionResultListener;
    private final NotificationListener notificationListener;

    @RabbitListener(queues = QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE)
    public void onArcGISExecutionResult(final ArcGISExecutionResult result) throws Exception {
        onMessageReceived(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE, result);
    }

    @RabbitListener(queues = QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE)
    public void onConfluenceExecutionResult(final ConfluenceExecutionResult result) throws Exception {
        onMessageReceived(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE, result);
    }

    @RabbitListener(queues = QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE)
    public void onJiraWebhookExecutionResult(final JiraWebhookExecutionResult result) throws Exception {
        onMessageReceived(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE, result);
    }

    @RabbitListener(queues = QueueNames.NOTIFICATION_QUEUE_IMS)
    public void onNotificationEvent(final NotificationEvent event) throws Exception {
        onMessageReceived(QueueNames.NOTIFICATION_QUEUE_IMS, event);
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
            case QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE ->
                    arcGISExecutionResultListener.onExecutionResult((ArcGISExecutionResult) message);
            case QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE ->
                    confluenceExecutionResultListener.onExecutionResult((ConfluenceExecutionResult) message);
            case QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE ->
                    jiraWebhookExecutionResultListener.onExecutionResult((JiraWebhookExecutionResult) message);
            case QueueNames.NOTIFICATION_QUEUE_IMS ->
                    notificationListener.handleNotificationEvent((NotificationEvent) message);
            default -> throw new IllegalArgumentException("Unsupported RabbitMQ queue: " + queueName);
        }
    }
}
