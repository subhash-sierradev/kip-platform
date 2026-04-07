package com.integration.execution.messaging;

import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.ArcGISJobExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.service.processor.KwToArcGISOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArcGISExecutionCommandListenerTest {

    @Mock
    private KwToArcGISOrchestrator orchestrator;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private ArcGISExecutionCommandListener listener;

    @BeforeEach
    void setUp() {
        listener = new ArcGISExecutionCommandListener(orchestrator, messagePublisher, notificationEventPublisher);
    }

    @Test
    void handleExecutionCommand_success_publishesSuccessResultAndCompletionNotification() {
        ArcGISExecutionCommand command = command("Integration A");
        ArcGISJobExecutionResult executionResult = new ArcGISJobExecutionResult(
                2, 1, 0, 3, List.of(), List.of(), List.of(), List.of(), null
        );
        when(orchestrator.processExecution(command)).thenReturn(executionResult);

        listener.handleExecutionCommand(command);

        ArgumentCaptor<ArcGISExecutionResult> resultCaptor = ArgumentCaptor.forClass(ArcGISExecutionResult.class);
        verify(messagePublisher).publish(
            eq(QueueNames.ARCGIS_EXCHANGE),
            eq(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                resultCaptor.capture()
        );

        ArcGISExecutionResult result = resultCaptor.getValue();
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isEqualTo(3);
        assertThat(result.getFailedRecords()).isZero();

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey()).isEqualTo("ARCGIS_INTEGRATION_JOB_COMPLETED");
    }

    @Test
    void handleExecutionCommand_zeroRecords_isMarkedSuccess() {
        ArcGISExecutionCommand command = command("Integration Z");
        ArcGISJobExecutionResult executionResult = new ArcGISJobExecutionResult(
                0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), null
        );
        when(orchestrator.processExecution(command)).thenReturn(executionResult);

        listener.handleExecutionCommand(command);

        ArgumentCaptor<ArcGISExecutionResult> resultCaptor = ArgumentCaptor.forClass(ArcGISExecutionResult.class);
        verify(messagePublisher).publish(
                eq(QueueNames.ARCGIS_EXCHANGE),
                eq(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                resultCaptor.capture()
        );

        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
    }

    @Test
    void handleExecutionCommand_whenOrchestratorThrows_publishesFailedResultAndFailureNotification() {
        ArcGISExecutionCommand command = command("Integration C");
        when(orchestrator.processExecution(command)).thenThrow(new RuntimeException("fatal"));

        listener.handleExecutionCommand(command);

        ArgumentCaptor<ArcGISExecutionResult> resultCaptor = ArgumentCaptor.forClass(ArcGISExecutionResult.class);
        verify(messagePublisher).publish(
            eq(QueueNames.ARCGIS_EXCHANGE),
            eq(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                resultCaptor.capture()
        );

        ArcGISExecutionResult result = resultCaptor.getValue();
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("fatal");

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey()).isEqualTo("ARCGIS_INTEGRATION_JOB_FAILED");
    }

    @Test
    void handleExecutionCommand_withFailedRecords_publishesFailedStatusAndFailureNotification() {
        ArcGISExecutionCommand command = command("Integration A");
        ArcGISJobExecutionResult executionResult = new ArcGISJobExecutionResult(
                1, 0, 2, 3, List.of(), List.of(), List.of(), List.of(), null
        );
        when(orchestrator.processExecution(command)).thenReturn(executionResult);

        listener.handleExecutionCommand(command);

        ArgumentCaptor<ArcGISExecutionResult> resultCaptor = ArgumentCaptor.forClass(ArcGISExecutionResult.class);
        verify(messagePublisher).publish(
                eq(QueueNames.ARCGIS_EXCHANGE),
                eq(QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE),
                resultCaptor.capture());

        assertThat(resultCaptor.getValue().getStatus()).isEqualTo(JobExecutionStatus.FAILED);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey()).isEqualTo("ARCGIS_INTEGRATION_JOB_FAILED");
    }

    @Test
    void handleExecutionCommand_withNullIntegrationName_successPublishesEmptyName() {
        ArcGISExecutionCommand command = commandWithNullName();
        ArcGISJobExecutionResult executionResult = new ArcGISJobExecutionResult(
                1, 0, 0, 1, List.of(), List.of(), List.of(), List.of(), null
        );
        when(orchestrator.processExecution(command)).thenReturn(executionResult);

        listener.handleExecutionCommand(command);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey()).isEqualTo("ARCGIS_INTEGRATION_JOB_COMPLETED");
        assertThat(eventCaptor.getValue().getMetadata()).containsEntry("integrationName", "");
    }

    @Test
    void handleExecutionCommand_orchestratorThrowsWithNullNameAndNullMessage_publishesFailedWithEmptyFields() {
        ArcGISExecutionCommand command = commandWithNullName();
        when(orchestrator.processExecution(command)).thenThrow(new RuntimeException((String) null));

        listener.handleExecutionCommand(command);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey()).isEqualTo("ARCGIS_INTEGRATION_JOB_FAILED");
        assertThat(eventCaptor.getValue().getMetadata()).containsEntry("integrationName", "");
        assertThat(eventCaptor.getValue().getMetadata()).containsEntry("errorMessage", "");
    }

    private ArcGISExecutionCommand command(String name) {
        return ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .jobExecutionId(UUID.randomUUID())
                .integrationName(name)
                .tenantId("tenant-1")
                .triggeredByUser("user-1")
                .build();
    }

    private ArcGISExecutionCommand commandWithNullName() {
        return ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .jobExecutionId(UUID.randomUUID())
                .integrationName(null)
                .tenantId("tenant-1")
                .triggeredByUser("user-1")
                .build();
    }
}
