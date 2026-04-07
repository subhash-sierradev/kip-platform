package com.integration.execution.messaging.impl;

import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.JiraWebhookExecutionCommand;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.messaging.ArcGISExecutionCommandListener;
import com.integration.execution.messaging.ConfluenceExecutionCommandListener;
import com.integration.execution.messaging.JiraWebhookExecutionCommandListener;
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
    private ArcGISExecutionCommandListener arcGISExecutionCommandListener;

    @Mock
    private ConfluenceExecutionCommandListener confluenceExecutionCommandListener;

    @Mock
    private JiraWebhookExecutionCommandListener jiraWebhookExecutionCommandListener;

    private RabbitMQMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new RabbitMQMessageListener(
                arcGISExecutionCommandListener,
                confluenceExecutionCommandListener,
                jiraWebhookExecutionCommandListener);
    }

    @Test
    @DisplayName("routes ArcGIS command queue to ArcGIS handler")
    void onMessageReceived_routesArcGISCommand() throws Exception {
        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder().build();

        listener.onMessageReceived(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE, command);

        verify(arcGISExecutionCommandListener).handleExecutionCommand(command);
        verifyNoInteractions(confluenceExecutionCommandListener, jiraWebhookExecutionCommandListener);
    }

    @Test
    @DisplayName("routes Confluence command queue to Confluence handler")
    void onMessageReceived_routesConfluenceCommand() throws Exception {
        ConfluenceExecutionCommand command = ConfluenceExecutionCommand.builder().build();

        listener.onMessageReceived(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE, command);

        verify(confluenceExecutionCommandListener).handleExecutionCommand(command);
        verifyNoInteractions(arcGISExecutionCommandListener, jiraWebhookExecutionCommandListener);
    }

    @Test
    @DisplayName("routes Jira webhook command queue to Jira handler")
    void onMessageReceived_routesJiraWebhookCommand() throws Exception {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder().build();

        listener.onMessageReceived(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE, command);

        verify(jiraWebhookExecutionCommandListener).handleExecutionCommand(command);
        verifyNoInteractions(arcGISExecutionCommandListener, confluenceExecutionCommandListener);
    }

    @Test
    @DisplayName("rejects unsupported queue names")
    void onMessageReceived_rejectsUnsupportedQueue() {
        assertThatThrownBy(() -> listener.onMessageReceived("unsupported.queue", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported RabbitMQ queue");

        verifyNoInteractions(
                arcGISExecutionCommandListener,
                confluenceExecutionCommandListener,
                jiraWebhookExecutionCommandListener);
    }

    @Test
    @DisplayName("rejects null message payload")
    void onMessageReceived_rejectsNullMessage() {
        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Received null message payload");

        verifyNoInteractions(
                arcGISExecutionCommandListener,
                confluenceExecutionCommandListener,
                jiraWebhookExecutionCommandListener);
    }

    @Test
    @DisplayName("onArcGISExecutionCommand delegates to onMessageReceived")
    void onArcGISExecutionCommand_delegatesToOnMessageReceived() throws Exception {
        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder().build();

        listener.onArcGISExecutionCommand(command);

        verify(arcGISExecutionCommandListener).handleExecutionCommand(command);
    }

    @Test
    @DisplayName("onConfluenceExecutionCommand delegates to onMessageReceived")
    void onConfluenceExecutionCommand_delegatesToOnMessageReceived() throws Exception {
        ConfluenceExecutionCommand command = ConfluenceExecutionCommand.builder().build();

        listener.onConfluenceExecutionCommand(command);

        verify(confluenceExecutionCommandListener).handleExecutionCommand(command);
    }

    @Test
    @DisplayName("onJiraWebhookExecutionCommand delegates to onMessageReceived")
    void onJiraWebhookExecutionCommand_delegatesToOnMessageReceived() throws Exception {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder().build();

        listener.onJiraWebhookExecutionCommand(command);

        verify(jiraWebhookExecutionCommandListener).handleExecutionCommand(command);
    }

    @Test
    @DisplayName("propagates exceptions from ArcGIS handler")
    void onMessageReceived_propagatesArcGISHandlerException() throws Exception {
        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder().build();
        Exception expectedException = new RuntimeException("ArcGIS processing failed");
        doThrow(expectedException).when(arcGISExecutionCommandListener).handleExecutionCommand(any());

        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE, command))
                .isEqualTo(expectedException);
    }

    @Test
    @DisplayName("propagates exceptions from Confluence handler")
    void onMessageReceived_propagatesConfluenceHandlerException() throws Exception {
        ConfluenceExecutionCommand command = ConfluenceExecutionCommand.builder().build();
        Exception expectedException = new RuntimeException("Confluence processing failed");
        doThrow(expectedException).when(confluenceExecutionCommandListener).handleExecutionCommand(any());

        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE, command))
                .isEqualTo(expectedException);
    }

    @Test
    @DisplayName("propagates exceptions from Jira handler")
    void onMessageReceived_propagatesJiraHandlerException() throws Exception {
        JiraWebhookExecutionCommand command = JiraWebhookExecutionCommand.builder().build();
        Exception expectedException = new RuntimeException("Jira processing failed");
        doThrow(expectedException).when(jiraWebhookExecutionCommandListener).handleExecutionCommand(any());

        assertThatThrownBy(() -> listener.onMessageReceived(QueueNames.JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE, command))
                .isEqualTo(expectedException);
    }
}