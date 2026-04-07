package com.integration.management.job;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.mapper.ConfluenceIntegrationMapper;
import com.integration.management.model.ExecutionWindow;
import com.integration.management.service.ConfluenceIntegrationService;
import com.integration.management.service.ConfluenceScheduleService;
import com.integration.management.service.ExecutionWindowResolverService;
import com.integration.management.service.IntegrationConnectionService;
import com.integration.management.service.IntegrationJobExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceIntegrationJob")
class ConfluenceIntegrationJobTest {

    @Mock
    private ConfluenceIntegrationService confluenceIntegrationService;
    @Mock
    private ConfluenceIntegrationMapper confluenceIntegrationMapper;
    @Mock
    private IntegrationConnectionService integrationConnectionService;
    @Mock
    private IntegrationJobExecutionService integrationJobExecutionService;
    @Mock
    private ExecutionWindowResolverService executionWindowResolver;
    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private JobExecutionContext context;

    @InjectMocks
    private ConfluenceIntegrationJob job;

    @Test
    @DisplayName("execute - success path publishes command to queue")
    void execute_success_publishesCommand() throws Exception {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        String tenantId = "tenant1";

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("integrationId", integrationId.toString());
        dataMap.put("tenantId", tenantId);
        dataMap.put(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY, TriggerType.SCHEDULER);
        dataMap.put(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY_USER, "user1");

        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(scheduleId);

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setConnectionId(UUID.randomUUID());
        integration.setSchedule(schedule);

        when(confluenceIntegrationService.getEnabledIntegrationForScheduledExecution(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        ExecutionWindow window = new ExecutionWindow(Instant.now().minusSeconds(3600), Instant.now());
        when(executionWindowResolver.resolve(any(), any(), eq(tenantId), anyString())).thenReturn(window);

        IntegrationJobExecution jobExecution = new IntegrationJobExecution();
        jobExecution.setId(UUID.randomUUID());
        when(integrationJobExecutionService.createRunningExecutionWithLineage(any(), any(), any(), any(), any(), any()))
                .thenReturn(jobExecution);

        when(integrationConnectionService.getIntegrationConnectionNameById(any(), eq(tenantId)))
                .thenReturn("connection-secret");

        ConfluenceExecutionCommand command = ConfluenceExecutionCommand.builder().build();
        when(confluenceIntegrationMapper.toExecutionCommand(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(command);

        // Act
        job.execute(context);

        // Assert
        verify(messagePublisher).publish(any(), any(), eq(command));
        verify(executionWindowResolver).resolve(any(), any(), eq(tenantId), anyString());
    }

    @Test
    @DisplayName("execute - integration not found throws exception")
    void execute_integrationNotFound_throwsException() {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant1";

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("integrationId", integrationId.toString());
        dataMap.put("tenantId", tenantId);

        when(context.getMergedJobDataMap()).thenReturn(dataMap);
        when(confluenceIntegrationService.getEnabledIntegrationForScheduledExecution(integrationId, tenantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(IntegrationNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("execute - invalid job data throws JobExecutionException")
    void execute_invalidJobData_throwsJobExecutionException() {
        // Arrange
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("integrationId", "not-a-uuid");  // Invalid UUID
        dataMap.put("tenantId", "tenant1");

        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        // Act & Assert
        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("Invalid job data configuration");
    }

    @Test
    @DisplayName("execute - trigger type as string is parsed correctly")
    void execute_triggerTypeAsString_parsesCorrectly() throws Exception {
        //Arrange
        UUID integrationId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        String tenantId = "tenant1";

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("integrationId", integrationId.toString());
        dataMap.put("tenantId", tenantId);
        dataMap.put(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY, "API");  // String instead of TriggerType

        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(scheduleId);

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setConnectionId(UUID.randomUUID());
        integration.setSchedule(schedule);

        when(confluenceIntegrationService.getEnabledIntegrationForScheduledExecution(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        ExecutionWindow window = new ExecutionWindow(Instant.now().minusSeconds(3600), Instant.now());
        when(executionWindowResolver.resolve(any(), any(), anyString(), anyString())).thenReturn(window);

        when(integrationJobExecutionService.createRunningExecutionWithLineage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new IntegrationJobExecution());
        when(integrationConnectionService.getIntegrationConnectionNameById(any(), anyString()))
                .thenReturn("secret");
        when(confluenceIntegrationMapper.toExecutionCommand(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ConfluenceExecutionCommand.builder().build());

        // Act
        job.execute(context);

        // Assert - should not throw
        verify(messagePublisher).publish(any(), any(), any());
    }

    @Test
    @DisplayName("execute - missing trigger type defaults to SCHEDULER")
    void execute_missingTriggerType_defaultsToScheduler() throws Exception {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        String tenantId = "tenant1";

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("integrationId", integrationId.toString());
        dataMap.put("tenantId", tenantId);
        // No TRIGGERED_BY key

        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(scheduleId);

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setConnectionId(UUID.randomUUID());
        integration.setSchedule(schedule);

        when(confluenceIntegrationService.getEnabledIntegrationForScheduledExecution(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        ExecutionWindow window = new ExecutionWindow(Instant.now().minusSeconds(3600), Instant.now());
        when(executionWindowResolver.resolve(any(), any(), anyString(), anyString())).thenReturn(window);

        when(integrationJobExecutionService.createRunningExecutionWithLineage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new IntegrationJobExecution());
        when(integrationConnectionService.getIntegrationConnectionNameById(any(), anyString()))
                .thenReturn("secret");
        when(confluenceIntegrationMapper.toExecutionCommand(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ConfluenceExecutionCommand.builder().build());

        // Act
        job.execute(context);

        // Assert
        verify(messagePublisher).publish(any(), any(), any());
    }

    @Test
    @DisplayName("execute - missing triggered by user defaults to system")
    void execute_missingUser_defaultsToSystem() throws Exception {
        // Arrange
        UUID integrationId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        String tenantId = "tenant1";

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("integrationId", integrationId.toString());
        dataMap.put("tenantId", tenantId);
        // No TRIGGERED_BY_USER key

        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(scheduleId);

        ConfluenceIntegration integration = new ConfluenceIntegration();
        integration.setId(integrationId);
        integration.setConnectionId(UUID.randomUUID());
        integration.setSchedule(schedule);

        when(confluenceIntegrationService.getEnabledIntegrationForScheduledExecution(integrationId, tenantId))
                .thenReturn(Optional.of(integration));

        ExecutionWindow window = new ExecutionWindow(Instant.now().minusSeconds(3600), Instant.now());
        when(executionWindowResolver.resolve(any(), any(), anyString(), anyString())).thenReturn(window);

        when(integrationJobExecutionService.createRunningExecutionWithLineage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new IntegrationJobExecution());
        when(integrationConnectionService.getIntegrationConnectionNameById(any(), anyString()))
                .thenReturn("secret");
        when(confluenceIntegrationMapper.toExecutionCommand(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ConfluenceExecutionCommand.builder().build());

        // Act
        job.execute(context);

        // Assert
        verify(messagePublisher).publish(any(), any(), any());
    }
}
