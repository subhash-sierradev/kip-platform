package com.integration.execution.messaging;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.mapper.ConfluenceExecutionMapper;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import com.integration.execution.model.ConfluenceJobExecutionResult.PublishedPage;
import com.integration.execution.service.processor.KwToConfluenceOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfluenceExecutionCommandListenerTest {

    @Mock
    private KwToConfluenceOrchestrator orchestrator;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private ConfluenceExecutionMapper confluenceExecutionMapper;

    private ConfluenceExecutionCommandListener listener;

    @BeforeEach
    void setUp() {
        listener = new ConfluenceExecutionCommandListener(
                orchestrator, messagePublisher, notificationEventPublisher, confluenceExecutionMapper);
    }

    @Test
    void handleExecutionCommand_success_publishesSuccessResultAndCompletionNotification() {
        ConfluenceExecutionCommand command = buildCommand("Integration A", "tenant-1");
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.success(
                5, List.of(new PublishedPage("en", "https://confluence.example.com/page/123", "123")));
        ConfluenceExecutionResult mappedResult = buildMappedResult(
                command.getJobExecutionId(), JobExecutionStatus.SUCCESS, null, 5);

        when(orchestrator.processExecution(command)).thenReturn(orchResult);
        when(confluenceExecutionMapper.toExecutionResult(eq(command), eq(orchResult), any(Instant.class)))
                .thenReturn(mappedResult);

        listener.handleExecutionCommand(command);

        verify(messagePublisher).publish(
                eq(QueueNames.CONFLUENCE_EXCHANGE),
                eq(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                eq(mappedResult));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey())
                .isEqualTo("CONFLUENCE_INTEGRATION_JOB_COMPLETED");
        assertThat(eventCaptor.getValue().getTenantId()).isEqualTo("tenant-1");
        assertThat(eventCaptor.getValue().getMetadata()).containsKey("integrationName");
        assertThat(eventCaptor.getValue().getMetadata()).containsKey("recordCount");
    }

    @Test
    void handleExecutionCommand_failure_publishesFailedResultAndFailureNotification() {
        ConfluenceExecutionCommand command = buildCommand("Integration B", "tenant-2");
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.failed("Some error");
        ConfluenceExecutionResult mappedResult = buildMappedResult(
                command.getJobExecutionId(), JobExecutionStatus.FAILED, "Some error", 0);

        when(orchestrator.processExecution(command)).thenReturn(orchResult);
        when(confluenceExecutionMapper.toExecutionResult(eq(command), eq(orchResult), any(Instant.class)))
                .thenReturn(mappedResult);

        listener.handleExecutionCommand(command);

        verify(messagePublisher).publish(
                eq(QueueNames.CONFLUENCE_EXCHANGE),
                eq(QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE),
                eq(mappedResult));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey())
                .isEqualTo("CONFLUENCE_INTEGRATION_JOB_FAILED");
        assertThat(eventCaptor.getValue().getMetadata()).containsKey("errorMessage");
        assertThat(eventCaptor.getValue().getMetadata().get("errorMessage")).isEqualTo("Some error");
    }

    @Test
    void handleExecutionCommand_nullIntegrationName_usesEmptyStringInNotification() {
        ConfluenceExecutionCommand command = buildCommandNullName("tenant-3");
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.success(
                2, List.of(new PublishedPage("en", "url", "456")));
        ConfluenceExecutionResult mappedResult = buildMappedResult(
                command.getJobExecutionId(), JobExecutionStatus.SUCCESS, null, 2);

        when(orchestrator.processExecution(command)).thenReturn(orchResult);
        when(confluenceExecutionMapper.toExecutionResult(eq(command), eq(orchResult), any(Instant.class)))
                .thenReturn(mappedResult);

        listener.handleExecutionCommand(command);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getMetadata().get("integrationName")).isEqualTo("");
    }

    @Test
    void handleExecutionCommand_nullErrorMessage_usesEmptyStringInFailureNotification() {
        ConfluenceExecutionCommand command = buildCommand("MyInt", "tenant-4");
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.failed(null);
        ConfluenceExecutionResult mappedResult = buildMappedResult(
                command.getJobExecutionId(), JobExecutionStatus.FAILED, null, 0);

        when(orchestrator.processExecution(command)).thenReturn(orchResult);
        when(confluenceExecutionMapper.toExecutionResult(eq(command), eq(orchResult), any(Instant.class)))
                .thenReturn(mappedResult);

        listener.handleExecutionCommand(command);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getMetadata().get("errorMessage")).isEqualTo("");
    }

    @Test
    void handleExecutionCommand_nullIntegrationId_usesEmptyStringInNotification() {
        ConfluenceExecutionCommand command = buildCommandNullId("tenant-5");
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.success(
                1, List.of(new PublishedPage("en", "url", "789")));
        ConfluenceExecutionResult mappedResult = buildMappedResult(
                command.getJobExecutionId(), JobExecutionStatus.SUCCESS, null, 1);

        when(orchestrator.processExecution(command)).thenReturn(orchResult);
        when(confluenceExecutionMapper.toExecutionResult(eq(command), eq(orchResult), any(Instant.class)))
                .thenReturn(mappedResult);

        listener.handleExecutionCommand(command);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getMetadata().get("integrationId")).isEqualTo("");
    }

    private ConfluenceExecutionCommand buildCommand(String name, String tenantId) {
        return ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .integrationName(name)
                .tenantId(tenantId)
                .triggeredByUser("user-1")
                .build();
    }

    private ConfluenceExecutionCommand buildCommandNullName(String tenantId) {
        return ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .integrationName(null)
                .tenantId(tenantId)
                .triggeredByUser("user-1")
                .build();
    }

    private ConfluenceExecutionCommand buildCommandNullId(String tenantId) {
        return ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(null)
                .integrationName("Integration NullId")
                .tenantId(tenantId)
                .triggeredByUser("user-1")
                .build();
    }

    private ConfluenceExecutionResult buildMappedResult(UUID jobId, JobExecutionStatus status,
                                                        String errorMessage, int records) {
        return ConfluenceExecutionResult.builder()
                .jobExecutionId(jobId)
                .status(status)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .totalRecords(records)
                .errorMessage(errorMessage)
                .build();
    }
}
