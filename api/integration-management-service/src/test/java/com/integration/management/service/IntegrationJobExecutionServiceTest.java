package com.integration.management.service;

import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.exception.IntegrationExecutionException;
import com.integration.management.mapper.IntegrationJobExecutionMapper;
import com.integration.management.repository.IntegrationJobExecutionRepository;
import com.integration.management.repository.IntegrationScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationJobExecutionService")
class IntegrationJobExecutionServiceTest {

    @Mock
    private IntegrationJobExecutionRepository jobExecutionRepository;
    @Mock
    private IntegrationScheduleRepository scheduleRepository;
    @Mock
    private IntegrationJobExecutionMapper jobExecutionMapper;

    @InjectMocks
    private IntegrationJobExecutionService service;

    @Test
    @DisplayName("getPreviousJobExecution should delegate when excludeExecutionId is null")
    void getPreviousJobExecution_excludeNull_delegates() {
        UUID scheduleId = UUID.randomUUID();
        IntegrationJobExecution expected = new IntegrationJobExecution();
        when(jobExecutionRepository.findTopByScheduleIdOrderByStartedAtDesc(scheduleId)).thenReturn(expected);

        IntegrationJobExecution actual = service.getPreviousJobExecution(scheduleId, null);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("getPreviousJobExecution should exclude execution id when provided")
    void getPreviousJobExecution_excludeProvided_usesAlternateQuery() {
        UUID scheduleId = UUID.randomUUID();
        UUID excludeId = UUID.randomUUID();
        IntegrationJobExecution expected = new IntegrationJobExecution();
        when(jobExecutionRepository.findTopByScheduleIdAndIdNotOrderByStartedAtDesc(scheduleId, excludeId))
                .thenReturn(expected);

        IntegrationJobExecution actual = service.getPreviousJobExecution(scheduleId, excludeId);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("createRunningExecution should persist RUNNING execution")
    void createRunningExecution_persistsRunning() {
        UUID scheduleId = UUID.randomUUID();
        Instant windowStart = Instant.parse("2026-03-03T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-03-03T01:00:00Z");

        when(jobExecutionRepository.save(any(IntegrationJobExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        IntegrationJobExecution execution = service.createRunningExecution(
                scheduleId,
                TriggerType.SCHEDULER,
                "user-1",
                "tenant-1",
                windowStart,
                windowEnd);

        assertThat(execution.getScheduleId()).isEqualTo(scheduleId);
        assertThat(execution.getStatus()).isEqualTo(JobExecutionStatus.RUNNING);
        assertThat(execution.getWindowStart()).isEqualTo(windowStart);
        assertThat(execution.getWindowEnd()).isEqualTo(windowEnd);
        assertThat(execution.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("completeJobExecution should update existing execution and save")
    void completeJobExecution_updatesAndSaves() {
        UUID executionId = UUID.randomUUID();
        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any(IntegrationJobExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.SUCCESS);
        result.setTotalRecords(10);
        result.setAddedRecords(4);
        result.setUpdatedRecords(5);
        result.setFailedRecords(1);
        result.setErrorMessage(null);
        result.setCompletedAt(Instant.parse("2026-03-03T02:00:00Z"));
        result.setAddedRecordsMetadata(List.of(new RecordMetadata("d1", "t1", "l1", null, null, null, null)));
        result.setUpdatedRecordsMetadata(List.of());
        result.setTotalRecordsMetadata(null);
        FailedRecordMetadata failed = new FailedRecordMetadata(
                "doc-1",
                "Doc",
                "loc-1",
                1L,
                2L,
                3L,
                4L,
                "bad");
        result.setFailedMetadata(List.of(failed));

        service.completeJobExecution(result);

        ArgumentCaptor<IntegrationJobExecution> captor = ArgumentCaptor.forClass(IntegrationJobExecution.class);
        verify(jobExecutionRepository).save(captor.capture());

        IntegrationJobExecution saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(saved.getCompletedAt()).isEqualTo(Instant.parse("2026-03-03T02:00:00Z"));
        assertThat(saved.getTotalRecords()).isEqualTo(10);
        assertThat(saved.getFailedRecordsMetadata()).containsExactly(failed);
        assertThat(saved.getAddedRecordsMetadata()).hasSize(1);
        assertThat(saved.getAddedRecordsMetadata().getFirst().documentId()).isEqualTo("d1");
    }

    @Test
    @DisplayName("completeJobExecution should not save when execution record missing")
    void completeJobExecution_missingRecord_doesNotSave() {
        UUID executionId = UUID.randomUUID();
        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.empty());

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.FAILED);

        service.completeJobExecution(result);

        verify(jobExecutionRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeJobExecution falls back to Instant.now() when completedAt is null")
    void completeJobExecution_nullCompletedAt_fallsBackToNow() {
        UUID executionId = UUID.randomUUID();
        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.FAILED);
        result.setCompletedAt(null);  // null → should fall back to Instant.now()

        Instant before = Instant.now();
        service.completeJobExecution(result);
        Instant after = Instant.now();

        ArgumentCaptor<IntegrationJobExecution> captor = ArgumentCaptor.forClass(IntegrationJobExecution.class);
        verify(jobExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getCompletedAt())
                .isNotNull()
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("completeJobExecution should advance processedUntil on SUCCESS")
    void completeJobExecution_successStatus_advancesProcessedUntil() {
        UUID executionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Instant windowEnd = Instant.parse("2026-03-03T23:59:59.999Z");

        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);
        existing.setScheduleId(scheduleId);
        existing.setWindowEnd(windowEnd);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(scheduleId);
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.SUCCESS);
        result.setCompletedAt(Instant.now());

        service.completeJobExecution(result);

        ArgumentCaptor<IntegrationSchedule> captor = ArgumentCaptor.forClass(IntegrationSchedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getProcessedUntil()).isEqualTo(windowEnd);
    }

    @Test
    @DisplayName("completeJobExecution should add 1ms for FIXED_DAY_BOUNDARY mode")
    void completeJobExecution_fixedDayBoundary_adds1ms() {
        UUID executionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Instant windowEnd = Instant.parse("2026-03-03T23:59:59.999Z");

        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);
        existing.setScheduleId(scheduleId);
        existing.setWindowEnd(windowEnd);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(scheduleId);
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.SUCCESS);
        result.setCompletedAt(Instant.now());

        service.completeJobExecution(result);

        ArgumentCaptor<IntegrationSchedule> captor = ArgumentCaptor.forClass(IntegrationSchedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getProcessedUntil()).isEqualTo(windowEnd.plusMillis(1));
    }

    @Test
    @DisplayName("completeJobExecution should not advance processedUntil on FAILED status")
    void completeJobExecution_failedStatus_doesNotAdvanceProcessedUntil() {
        UUID executionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);
        existing.setScheduleId(scheduleId);
        existing.setWindowEnd(Instant.parse("2026-03-03T23:59:59.999Z"));

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.FAILED);
        result.setCompletedAt(Instant.now());

        service.completeJobExecution(result);

        verify(scheduleRepository, never()).findById(any());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeJobExecution should not advance processedUntil when windowEnd is null")
    void completeJobExecution_nullWindowEnd_doesNotAdvanceProcessedUntil() {
        UUID executionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);
        existing.setScheduleId(scheduleId);
        existing.setWindowEnd(null);

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.SUCCESS);
        result.setCompletedAt(Instant.now());

        service.completeJobExecution(result);

        verify(scheduleRepository, never()).findById(any());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeJobExecution should handle missing schedule gracefully")
    void completeJobExecution_missingSchedule_doesNotThrow() {
        UUID executionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);
        existing.setScheduleId(scheduleId);
        existing.setWindowEnd(Instant.now());

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        ArcGISExecutionResult result = new ArcGISExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.SUCCESS);
        result.setCompletedAt(Instant.now());

        service.completeJobExecution(result);

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRunningExecutionWithLineage should set originalJobId and retryAttempt when null")
    void createRunningExecutionWithLineage_nullOriginalJobId_setsLineage() {
        UUID scheduleId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        Instant windowStart = Instant.parse("2026-03-03T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-03-03T01:00:00Z");

        IntegrationJobExecution firstSave = new IntegrationJobExecution();
        firstSave.setId(executionId);
        firstSave.setOriginalJobId(null);

        when(jobExecutionRepository.save(any(IntegrationJobExecution.class)))
                .thenReturn(firstSave)
                .thenAnswer(inv -> inv.getArgument(0));

        IntegrationJobExecution execution = service.createRunningExecutionWithLineage(
                scheduleId, TriggerType.SCHEDULER, "user-1", "tenant-1", windowStart, windowEnd);

        verify(jobExecutionRepository, times(2)).save(any(IntegrationJobExecution.class));
        assertThat(execution.getOriginalJobId()).isEqualTo(executionId);
        assertThat(execution.getRetryAttempt()).isZero();
    }

    @Test
    @DisplayName("createRunningExecutionWithLineage should not update when originalJobId already set")
    void createRunningExecutionWithLineage_existingOriginalJobId_noUpdate() {
        UUID scheduleId = UUID.randomUUID();
        UUID existingOriginalJobId = UUID.randomUUID();
        Instant windowStart = Instant.parse("2026-03-03T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-03-03T01:00:00Z");

        IntegrationJobExecution firstSave = new IntegrationJobExecution();
        firstSave.setId(UUID.randomUUID());
        firstSave.setOriginalJobId(existingOriginalJobId);

        when(jobExecutionRepository.save(any(IntegrationJobExecution.class)))
                .thenReturn(firstSave);

        IntegrationJobExecution execution = service.createRunningExecutionWithLineage(
                scheduleId, TriggerType.SCHEDULER, "user-1", "tenant-1", windowStart, windowEnd);

        verify(jobExecutionRepository, times(1)).save(any(IntegrationJobExecution.class));
        assertThat(execution.getOriginalJobId()).isEqualTo(existingOriginalJobId);
    }

    @Test
    @DisplayName("createRunningExecutionWithLineage should not update when execution ID is null")
    void createRunningExecutionWithLineage_nullExecutionId_noUpdate() {
        UUID scheduleId = UUID.randomUUID();
        Instant windowStart = Instant.parse("2026-03-03T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-03-03T01:00:00Z");

        IntegrationJobExecution firstSave = new IntegrationJobExecution();
        firstSave.setId(null);
        firstSave.setOriginalJobId(null);

        when(jobExecutionRepository.save(any(IntegrationJobExecution.class)))
                .thenReturn(firstSave);

        IntegrationJobExecution execution = service.createRunningExecutionWithLineage(
                scheduleId, TriggerType.SCHEDULER, "user-1", "tenant-1", windowStart, windowEnd);

        verify(jobExecutionRepository, times(1)).save(any(IntegrationJobExecution.class));
        assertThat(execution.getOriginalJobId()).isNull();
    }

    @Test
    @DisplayName("createConfluenceRetryExecution should increment retryAttempt")
    void createConfluenceRetryExecution_incrementsRetryAttempt() {
        UUID originalJobId = UUID.randomUUID();
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";

        IntegrationJobExecution latestExecution = new IntegrationJobExecution();
        latestExecution.setId(UUID.randomUUID());
        latestExecution.setOriginalJobId(originalJobId);
        latestExecution.setRetryAttempt(2);
        latestExecution.setScheduleId(UUID.randomUUID());

        IntegrationJobExecution retryTemplate = new IntegrationJobExecution();
        retryTemplate.setScheduleId(latestExecution.getScheduleId());
        retryTemplate.setOriginalJobId(originalJobId);

        when(jobExecutionRepository.findConfluenceLatestRetriableExecutionByOriginalJobId(
                originalJobId, integrationId, tenantId))
                .thenReturn(Optional.of(latestExecution));
        when(jobExecutionMapper.toRetryExecution(latestExecution)).thenReturn(retryTemplate);
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IntegrationJobExecution retry = service.createConfluenceRetryExecution(
                originalJobId, integrationId, tenantId, TriggerType.API, "user-1");

        assertThat(retry.getRetryAttempt()).isEqualTo(3);
        assertThat(retry.getStatus()).isEqualTo(JobExecutionStatus.RUNNING);
        assertThat(retry.getTriggeredBy()).isEqualTo(TriggerType.API);
        assertThat(retry.getTriggeredByUser()).isEqualTo("user-1");
        verify(jobExecutionRepository).save(any(IntegrationJobExecution.class));
    }

    @Test
    @DisplayName("completeConfluenceJobExecution should update execution and advance processedUntil")
    void completeConfluenceJobExecution_updatesAndAdvances() {
        UUID executionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Instant windowEnd = Instant.parse("2026-03-03T23:59:59.999Z");

        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);
        existing.setScheduleId(scheduleId);
        existing.setWindowEnd(windowEnd);

        IntegrationSchedule schedule = new IntegrationSchedule();
        schedule.setId(scheduleId);
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfluenceExecutionResult result = new ConfluenceExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.SUCCESS);
        result.setTotalRecords(5);
        result.setAddedRecords(3);
        result.setUpdatedRecords(2);
        result.setFailedRecords(0);
        result.setExecutionMetadata(Map.of("pageId", "123456"));
        result.setCompletedAt(Instant.parse("2026-03-03T10:00:00Z"));

        service.completeConfluenceJobExecution(result);

        ArgumentCaptor<IntegrationJobExecution> executionCaptor = ArgumentCaptor.forClass(
                IntegrationJobExecution.class);
        verify(jobExecutionRepository).save(executionCaptor.capture());
        IntegrationJobExecution saved = executionCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(saved.getTotalRecords()).isEqualTo(5);
        assertThat(saved.getExecutionMetadata()).containsEntry("pageId", "123456");

        ArgumentCaptor<IntegrationSchedule> scheduleCaptor = ArgumentCaptor.forClass(
                IntegrationSchedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().getProcessedUntil()).isEqualTo(windowEnd);
    }

    @Test
    @DisplayName("completeConfluenceJobExecution should not save when execution not found")
    void completeConfluenceJobExecution_missingExecution_doesNotSave() {
        UUID executionId = UUID.randomUUID();
        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.empty());

        ConfluenceExecutionResult result = new ConfluenceExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.FAILED);

        service.completeConfluenceJobExecution(result);

        verify(jobExecutionRepository, never()).save(any());
        verify(scheduleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("completeConfluenceJobExecution should fall back to Instant.now when completedAt is null")
    void completeConfluenceJobExecution_nullCompletedAt_fallsBackToNow() {
        UUID executionId = UUID.randomUUID();
        IntegrationJobExecution existing = new IntegrationJobExecution();
        existing.setId(executionId);

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(existing));
        when(jobExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfluenceExecutionResult result = new ConfluenceExecutionResult();
        result.setJobExecutionId(executionId);
        result.setStatus(JobExecutionStatus.ABORTED);
        result.setCompletedAt(null);

        Instant before = Instant.now();
        service.completeConfluenceJobExecution(result);
        Instant after = Instant.now();

        ArgumentCaptor<IntegrationJobExecution> captor = ArgumentCaptor.forClass(
                IntegrationJobExecution.class);
        verify(jobExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getCompletedAt())
                .isNotNull()
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("getConfluenceJobHistory should delegate to repository")
    void getConfluenceJobHistory_delegates() {
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";
        List<IntegrationJobExecution> expected = List.of(
                new IntegrationJobExecution(),
                new IntegrationJobExecution()
        );

        when(jobExecutionRepository.findByConfluenceIntegrationAndTenant(integrationId, tenantId))
                .thenReturn(expected);

        List<IntegrationJobExecution> actual = service.getConfluenceJobHistory(integrationId, tenantId);

        assertThat(actual).isSameAs(expected);
        verify(jobExecutionRepository).findByConfluenceIntegrationAndTenant(integrationId, tenantId);
    }

    @Test
    @DisplayName("findLatestRetriableConfluenceExecution should return execution when found")
    void findLatestRetriableConfluenceExecution_found_returnsExecution() {
        UUID originalJobId = UUID.randomUUID();
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";

        IntegrationJobExecution expected = new IntegrationJobExecution();
        expected.setId(UUID.randomUUID());
        expected.setStatus(JobExecutionStatus.FAILED);

        when(jobExecutionRepository.findConfluenceLatestRetriableExecutionByOriginalJobId(
                originalJobId, integrationId, tenantId))
                .thenReturn(Optional.of(expected));

        IntegrationJobExecution actual = service.findLatestRetriableConfluenceExecution(
                originalJobId, integrationId, tenantId);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("findLatestRetriableConfluenceExecution should throw when not found")
    void findLatestRetriableConfluenceExecution_notFound_throws() {
        UUID originalJobId = UUID.randomUUID();
        UUID integrationId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(jobExecutionRepository.findConfluenceLatestRetriableExecutionByOriginalJobId(
                originalJobId, integrationId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findLatestRetriableConfluenceExecution(
                originalJobId, integrationId, tenantId))
                .isInstanceOf(IntegrationExecutionException.class)
                .hasMessageContaining("No retriable")
                .hasMessageContaining(originalJobId.toString());
    }

    @Test
    @DisplayName("save should delegate to repository")
    void save_delegates() {
        IntegrationJobExecution execution = new IntegrationJobExecution();
        execution.setId(UUID.randomUUID());

        when(jobExecutionRepository.save(execution)).thenReturn(execution);

        IntegrationJobExecution saved = service.save(execution);

        assertThat(saved).isSameAs(execution);
        verify(jobExecutionRepository).save(execution);
    }
}
