package com.integration.management.job;

import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.management.constants.ManagementSecurityConstants;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.model.ExecutionWindow;
import com.integration.management.service.ArcGISIntegrationService;
import com.integration.management.service.ArcGISScheduleService;
import com.integration.management.service.ExecutionWindowResolverService;
import com.integration.management.service.IntegrationConnectionService;
import com.integration.management.service.IntegrationJobExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcGISIntegrationJob")
class ArcGISIntegrationJobTest {

    @Mock
    private ArcGISIntegrationRepository arcGISIntegrationRepository;
    @Mock
    private ArcGISIntegrationService arcGISIntegrationService;
    @Mock
    private IntegrationConnectionService integrationConnectionService;
    @Mock
    private IntegrationJobExecutionService integrationJobExecutionService;
    @Mock
    private ExecutionWindowResolverService executionWindowResolver;
    @Mock
    private MessagePublisher messagePublisher;

    private ArcGISIntegrationJob newJob() {
        return new ArcGISIntegrationJob(
                arcGISIntegrationRepository,
                arcGISIntegrationService,
                integrationConnectionService,
                integrationJobExecutionService,
                executionWindowResolver,
                messagePublisher);
    }

    private static ArcGISIntegration integration(UUID integrationId, String tenantId) {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .id(UUID.randomUUID())
                .executionTime(LocalTime.of(10, 0))
                .cronExpression("0 0 10 * * ?")
                .build();

        return ArcGISIntegration.builder()
                .id(integrationId)
                .tenantId(tenantId)
                .createdBy("u")
                .lastModifiedBy("u")
                .name("ArcGIS A")
                .itemType("DOCUMENT")
                .itemSubtype("S")
                .dynamicDocumentType("DT")
                .connectionId(UUID.randomUUID())
                .schedule(schedule)
                .isEnabled(true)
                .build();
    }

    private static JobExecutionContext ctx(JobDataMap map) {
        JobExecutionContext ctx = mock(JobExecutionContext.class);
        when(ctx.getMergedJobDataMap()).thenReturn(map);
        return ctx;
    }

    @Test
    @DisplayName("execute should publish command with configured windowStart on first run")
    void execute_firstRun_usesConfiguredStart_andPublishes() throws Exception {
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";
        ArcGISIntegration integration = integration(integrationId, tenantId);

        JobDataMap map = new JobDataMap();
        map.put("integrationId", integrationId.toString());
        map.put("tenantId", tenantId);
        map.put(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY, "API");
        map.put(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY_USER, "user-1");

        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalseWithSchedule(integrationId, tenantId))
                .thenReturn(Optional.of(integration));
        Instant configuredStart = Instant.parse("2026-03-03T00:00:00Z");
        Instant windowEnd = Instant.now();
        when(executionWindowResolver.resolve(eq(integration.getSchedule()), any(Instant.class), eq(tenantId), eq("ARCGIS")))
                .thenReturn(new ExecutionWindow(configuredStart, windowEnd));

        IntegrationJobExecution jobExecution = IntegrationJobExecution.builder()
                .id(UUID.randomUUID())
                .scheduleId(integration.getSchedule().getId())
                .triggeredBy(TriggerType.API)
                .status(JobExecutionStatus.RUNNING)
                .startedAt(Instant.now())
                .build();
        when(integrationJobExecutionService.createRunningExecution(
                eq(integration.getSchedule().getId()), eq(TriggerType.API), eq("user-1"), eq(tenantId),
                eq(configuredStart), eq(windowEnd)))
                .thenReturn(jobExecution);

        List<IntegrationFieldMappingDto> mappings = List.of(IntegrationFieldMappingDto.builder()
                .sourceFieldPath("a")
                .targetFieldPath("b")
                .build());
        when(arcGISIntegrationService.getFieldMappings(integrationId, tenantId)).thenReturn(mappings);
        when(integrationConnectionService.getIntegrationConnectionNameById(
                integration.getConnectionId().toString(), tenantId))
                .thenReturn("secret-1");

        newJob().execute(ctx(map));

        ArgumentCaptor<ArcGISExecutionCommand> cmdCaptor = ArgumentCaptor.forClass(ArcGISExecutionCommand.class);
        verify(messagePublisher).publish(
                eq(QueueNames.ARCGIS_EXCHANGE),
                eq(QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE),
                cmdCaptor.capture());
        ArcGISExecutionCommand cmd = cmdCaptor.getValue();
        assertThat(cmd.getIntegrationId()).isEqualTo(integrationId);
        assertThat(cmd.getJobExecutionId()).isEqualTo(jobExecution.getId());
        assertThat(cmd.getTenantId()).isEqualTo(tenantId);
        assertThat(cmd.getTriggeredBy()).isEqualTo(TriggerType.API);
        assertThat(cmd.getTriggeredByUser()).isEqualTo("user-1");
        assertThat(cmd.getWindowStart()).isEqualTo(configuredStart);
        assertThat(cmd.getWindowEnd()).isEqualTo(windowEnd);
        assertThat(cmd.getFieldMappings()).isEqualTo(mappings);
        assertThat(cmd.getConnectionSecretName()).isEqualTo("secret-1");
    }

    @Test
    @DisplayName("execute should use previous SUCCESS windowEnd as next windowStart")
    void execute_prevSuccess_usesPrevEnd() throws Exception {
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";
        ArcGISIntegration integration = integration(integrationId, tenantId);

        JobDataMap map = new JobDataMap();
        map.put("integrationId", integrationId.toString());
        map.put("tenantId", tenantId);

        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalseWithSchedule(integrationId, tenantId))
                .thenReturn(Optional.of(integration));
        Instant prevWindowEnd = Instant.parse("2026-03-03T01:00:00Z");
        Instant windowEnd = Instant.now();
        when(executionWindowResolver.resolve(eq(integration.getSchedule()), any(Instant.class), eq(tenantId), eq("ARCGIS")))
                .thenReturn(new ExecutionWindow(prevWindowEnd, windowEnd));

        IntegrationJobExecution running = IntegrationJobExecution.builder()
                .id(UUID.randomUUID())
                .scheduleId(integration.getSchedule().getId())
                .triggeredBy(TriggerType.SCHEDULER)
                .status(JobExecutionStatus.RUNNING)
                .startedAt(Instant.now())
                .build();
        when(integrationJobExecutionService.createRunningExecution(
                eq(integration.getSchedule().getId()), eq(TriggerType.SCHEDULER),
                eq(ManagementSecurityConstants.SYSTEM_USER),
                eq(tenantId), eq(prevWindowEnd), eq(windowEnd)))
                .thenReturn(running);

        when(arcGISIntegrationService.getFieldMappings(integrationId, tenantId)).thenReturn(List.of());
        when(integrationConnectionService.getIntegrationConnectionNameById(
                integration.getConnectionId().toString(), tenantId))
                .thenReturn("secret-1");

        newJob().execute(ctx(map));

        ArgumentCaptor<ArcGISExecutionCommand> cmdCaptor = ArgumentCaptor.forClass(ArcGISExecutionCommand.class);
        verify(messagePublisher).publish(any(), any(), cmdCaptor.capture());
        assertThat(cmdCaptor.getValue().getWindowStart()).isEqualTo(prevWindowEnd);
        assertThat(cmdCaptor.getValue().getTriggeredBy()).isEqualTo(TriggerType.SCHEDULER);
        assertThat(cmdCaptor.getValue().getTriggeredByUser()).isEqualTo(ManagementSecurityConstants.SYSTEM_USER);
    }

    @Test
    @DisplayName("execute should use previous FAILED windowStart (retry same window)")
    void execute_prevFailed_retriesSameWindow() throws Exception {
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";
        ArcGISIntegration integration = integration(integrationId, tenantId);

        JobDataMap map = new JobDataMap();
        map.put("integrationId", integrationId.toString());
        map.put("tenantId", tenantId);
        map.put(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY, "NOT_A_TRIGGER");
        map.put(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY_USER, " ");

        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalseWithSchedule(integrationId, tenantId))
                .thenReturn(Optional.of(integration));
        Instant retryStart = Instant.parse("2026-03-03T02:00:00Z");
        Instant windowEnd = Instant.now();
        when(executionWindowResolver.resolve(eq(integration.getSchedule()), any(Instant.class), eq(tenantId), eq("ARCGIS")))
                .thenReturn(new ExecutionWindow(retryStart, windowEnd));

        IntegrationJobExecution running = IntegrationJobExecution.builder()
                .id(UUID.randomUUID())
                .scheduleId(integration.getSchedule().getId())
                .triggeredBy(TriggerType.SCHEDULER)
                .status(JobExecutionStatus.RUNNING)
                .startedAt(Instant.now())
                .build();
        when(integrationJobExecutionService.createRunningExecution(
                eq(integration.getSchedule().getId()), eq(TriggerType.SCHEDULER),
                eq(ManagementSecurityConstants.SYSTEM_USER),
                eq(tenantId), eq(retryStart), eq(windowEnd)))
                .thenReturn(running);

        when(arcGISIntegrationService.getFieldMappings(integrationId, tenantId)).thenReturn(List.of());
        when(integrationConnectionService.getIntegrationConnectionNameById(
                integration.getConnectionId().toString(), tenantId))
                .thenReturn("secret-1");

        newJob().execute(ctx(map));

        ArgumentCaptor<ArcGISExecutionCommand> cmdCaptor = ArgumentCaptor.forClass(ArcGISExecutionCommand.class);
        verify(messagePublisher).publish(any(), any(), cmdCaptor.capture());
        assertThat(cmdCaptor.getValue().getWindowStart()).isEqualTo(retryStart);
        assertThat(cmdCaptor.getValue().getTriggeredBy()).isEqualTo(TriggerType.SCHEDULER);
        assertThat(cmdCaptor.getValue().getTriggeredByUser()).isEqualTo(ManagementSecurityConstants.SYSTEM_USER);
    }

    @Test
    @DisplayName("execute should skip when integration not found")
    void execute_missingIntegration_skips() throws Exception {
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";

        JobDataMap map = new JobDataMap();
        map.put("integrationId", integrationId.toString());
        map.put("tenantId", tenantId);

        when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalseWithSchedule(integrationId, tenantId))
                .thenReturn(Optional.empty());

        newJob().execute(ctx(map));

        verify(messagePublisher, never()).publish(anyString(), anyString(), any(ArcGISExecutionCommand.class));
        verify(integrationJobExecutionService, never()).createRunningExecution(any(), any(), any(), any(), any(),
                any());
    }

    @Test
    @DisplayName("execute should throw JobExecutionException on invalid JobDataMap")
    void execute_invalidData_throws() {
        JobDataMap map = new JobDataMap();
        map.put("integrationId", "not-a-uuid");
        map.put("tenantId", "tenant-1");

        assertThatThrownBy(() -> newJob().execute(ctx(map)))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("Invalid job data configuration");
    }
}
