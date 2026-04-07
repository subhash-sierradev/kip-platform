package com.integration.management.messaging.impl;

import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.message.JiraWebhookExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.management.messaging.ArcGISExecutionResultListener;
import com.integration.management.messaging.ConfluenceExecutionResultListener;
import com.integration.management.messaging.JiraWebhookExecutionResultListener;
import com.integration.management.notification.messaging.NotificationListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQMessageListener")
class RabbitMQMessageListenerTest {

    @Mock
    private ArcGISExecutionResultListener arcGISExecutionResultListener;

    @Mock
    private ConfluenceExecutionResultListener confluenceExecutionResultListener;

    @Mock
    private JiraWebhookExecutionResultListener jiraWebhookExecutionResultListener;

    @Mock
    private NotificationListener notificationListener;

    private RabbitMQMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new RabbitMQMessageListener(
                arcGISExecutionResultListener,
                confluenceExecutionResultListener,
                jiraWebhookExecutionResultListener,
                notificationListener);
    }

    @Test
    @DisplayName("routes ArcGIS result queue to ArcGIS handler")
    void onMessageReceived_routesArcGISResult() throws Exception {
        ArcGISExecutionResult result = ArcGISExecutionResult.builder().build();

        listener.onMessageReceived(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE, result);

        verify(arcGISExecutionResultListener).onExecutionResult(result);
        verifyNoInteractions(
                confluenceExecutionResultListener,
                jiraWebhookExecutionResultListener,
                notificationListener);
    }

    @Test
    @DisplayName("routes Confluence result queue to Confluence handler")
    void onMessageReceived_routesConfluenceResult() throws Exception {
        ConfluenceExecutionResult result = ConfluenceExecutionResult.builder().build();

        listener.onMessageReceived(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE, result);

        verify(confluenceExecutionResultListener).onExecutionResult(result);
        verifyNoInteractions(
                arcGISExecutionResultListener,
                jiraWebhookExecutionResultListener,
                notificationListener);
    }

    @Test
    @DisplayName("routes Jira webhook result queue to Jira handler")
    void onMessageReceived_routesJiraWebhookResult() throws Exception {
        JiraWebhookExecutionResult result = JiraWebhookExecutionResult.builder().build();

        listener.onMessageReceived(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE, result);

        verify(jiraWebhookExecutionResultListener).onExecutionResult(result);
        verifyNoInteractions(
                arcGISExecutionResultListener,
                confluenceExecutionResultListener,
                notificationListener);
    }

    @Test
    @DisplayName("routes notification queue to notification handler")
    void onMessageReceived_routesNotificationEvent() throws Exception {
        NotificationEvent event = NotificationEvent.builder().build();

        listener.onMessageReceived(QueueNames.NOTIFICATION_QUEUE_IMS, event);

        verify(notificationListener).handleNotificationEvent(event);
        verifyNoInteractions(
                arcGISExecutionResultListener,
                confluenceExecutionResultListener,
                jiraWebhookExecutionResultListener);
    }

    @Test
    @DisplayName("rejects unsupported queue names")
    void onMessageReceived_rejectsUnsupportedQueue() {
        assertThatThrownBy(() -> listener.onMessageReceived("unsupported.queue", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported RabbitMQ queue");

        verifyNoInteractions(
                arcGISExecutionResultListener,
                confluenceExecutionResultListener,
                jiraWebhookExecutionResultListener,
                notificationListener);
    }

    @Test
    @DisplayName("rejects null message payload")
    void onMessageReceived_rejectsNullMessage() {
        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Received null message payload");

        verifyNoInteractions(
                arcGISExecutionResultListener,
                confluenceExecutionResultListener,
                jiraWebhookExecutionResultListener,
                notificationListener);
    }

    @Test
    @DisplayName("onArcGISExecutionResult delegates to onMessageReceived")
    void onArcGISExecutionResult_delegatesToOnMessageReceived() throws Exception {
        ArcGISExecutionResult result = ArcGISExecutionResult.builder().build();

        listener.onArcGISExecutionResult(result);

        verify(arcGISExecutionResultListener).onExecutionResult(result);
    }

    @Test
    @DisplayName("onConfluenceExecutionResult delegates to onMessageReceived")
    void onConfluenceExecutionResult_delegatesToOnMessageReceived() throws Exception {
        ConfluenceExecutionResult result = ConfluenceExecutionResult.builder().build();

        listener.onConfluenceExecutionResult(result);

        verify(confluenceExecutionResultListener).onExecutionResult(result);
    }

    @Test
    @DisplayName("onJiraWebhookExecutionResult delegates to onMessageReceived")
    void onJiraWebhookExecutionResult_delegatesToOnMessageReceived() throws Exception {
        JiraWebhookExecutionResult result = JiraWebhookExecutionResult.builder().build();

        listener.onJiraWebhookExecutionResult(result);

        verify(jiraWebhookExecutionResultListener).onExecutionResult(result);
    }

    @Test
    @DisplayName("onNotificationEvent delegates to onMessageReceived")
    void onNotificationEvent_delegatesToOnMessageReceived() throws Exception {
        NotificationEvent event = NotificationEvent.builder().build();

        listener.onNotificationEvent(event);

        verify(notificationListener).handleNotificationEvent(event);
    }

    @Test
    @DisplayName("propagates exceptions from ArcGIS handler")
    void onMessageReceived_propagatesArcGISHandlerException() throws Exception {
        ArcGISExecutionResult result = ArcGISExecutionResult.builder().build();
        Exception expectedException = new RuntimeException("ArcGIS processing failed");
        doThrow(expectedException).when(arcGISExecutionResultListener).onExecutionResult(any());

        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE, result))
                .isEqualTo(expectedException);
    }

    @Test
    @DisplayName("propagates exceptions from Confluence handler")
    void onMessageReceived_propagatesConfluenceHandlerException() throws Exception {
        ConfluenceExecutionResult result = ConfluenceExecutionResult.builder().build();
        Exception expectedException = new RuntimeException("Confluence processing failed");
        doThrow(expectedException).when(confluenceExecutionResultListener).onExecutionResult(any());

        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE, result))
                .isEqualTo(expectedException);
    }

    @Test
    @DisplayName("propagates exceptions from Jira handler")
    void onMessageReceived_propagatesJiraHandlerException() throws Exception {
        JiraWebhookExecutionResult result = JiraWebhookExecutionResult.builder().build();
        Exception expectedException = new RuntimeException("Jira processing failed");
        doThrow(expectedException).when(jiraWebhookExecutionResultListener).onExecutionResult(any());

        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE, result))
                .isEqualTo(expectedException);
    }

    @Test
    @DisplayName("propagates exceptions from Notification handler")
    void onMessageReceived_propagatesNotificationHandlerException() throws Exception {
        NotificationEvent event = NotificationEvent.builder().build();
        Exception expectedException = new RuntimeException("Notification processing failed");
        doThrow(expectedException).when(notificationListener).handleNotificationEvent(any());

        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.NOTIFICATION_QUEUE_IMS, event))
                .isEqualTo(expectedException);
    }
}