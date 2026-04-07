package com.integration.management.service;

import com.integration.execution.contract.model.JobReloadResultDto;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.exception.SchedulingException;
import com.integration.management.repository.ArcGISIntegrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcGISScheduleService")
class ArcGISScheduleServiceTest {

    @Mock
    private Scheduler scheduler;
    @Mock
    private ArcGISIntegrationRepository arcGISIntegrationRepository;

    @InjectMocks
    private ArcGISScheduleService service;

    private static ArcGISIntegration integrationWithSchedule(UUID id, String tenantId, String name, String cron) {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .id(UUID.randomUUID())
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .cronExpression(cron)
                .build();
        return ArcGISIntegration.builder()
                .id(id)
                .tenantId(tenantId)
                .createdBy("u")
                .lastModifiedBy("u")
                .name(name)
                .itemType("DOCUMENT")
                .itemSubtype("X")
                .connectionId(UUID.randomUUID())
                .schedule(schedule)
                .isEnabled(true)
                .build();
    }

    @Test
    @DisplayName("scheduleJob should throw when schedule is missing")
    void scheduleJob_missingSchedule_throws() {
        ArcGISIntegration integration = ArcGISIntegration.builder()
                .id(UUID.randomUUID())
                .tenantId("t")
                .createdBy("u")
                .lastModifiedBy("u")
                .name("n")
                .itemType("DOCUMENT")
                .itemSubtype("X")
                .connectionId(UUID.randomUUID())
                .schedule(null)
                .build();

        assertThatThrownBy(() -> service.scheduleJob(integration))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("No schedule");
    }

    @Test
    @DisplayName("scheduleJob should build and schedule Quartz job")
    void scheduleJob_schedules() throws Exception {
        ArcGISIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");

        service.scheduleJob(integration);

        verify(scheduler).scheduleJob(any(), any());
    }

    @Test
    @DisplayName("unscheduleJob should delete job")
    void unscheduleJob_deletes() throws Exception {
        ArcGISIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        service.unscheduleJob(integration);

        verify(scheduler).deleteJob(any(JobKey.class));
    }

    @Test
    @DisplayName("updateSchedule should unschedule existing job before scheduling replacement")
    void updateSchedule_unschedulesThenSchedules() throws Exception {
        ArcGISIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        service.updateSchedule(integration);

        verify(scheduler).deleteJob(any(JobKey.class));
        verify(scheduler).scheduleJob(any(), any());
    }

    @Test
    @DisplayName("updateSchedule should wrap unschedule failures and stop before scheduling")
    void updateSchedule_unscheduleFailure_throws() throws Exception {
        ArcGISIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class)))
                .thenThrow(new org.quartz.SchedulerException("quartz error"));

        assertThatThrownBy(() -> service.updateSchedule(integration))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("Failed to update job schedule");

        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    @DisplayName("triggerJob should throw when job does not exist")
    void triggerJob_missing_throws() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        assertThatThrownBy(() -> service.triggerJob(integrationId, "tenant-1"))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("Job not scheduled");
    }

    @Test
    @DisplayName("triggerJob should pass TriggerType and user into JobDataMap")
    void triggerJob_passesJobData() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        service.triggerJob(integrationId, "tenant-1", TriggerType.API, "user-1");

        ArgumentCaptor<JobDataMap> captor = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(any(JobKey.class), captor.capture());
        assertThat(captor.getValue()).containsEntry(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY, "API");
        assertThat(captor.getValue()).containsEntry(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY_USER, "user-1");
    }

    @Test
    @DisplayName("reloadAllJobs should reschedule when cron mismatch")
    void reloadAllJobs_reschedulesMismatch() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));

        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        CronTrigger existing = org.mockito.Mockito.mock(CronTrigger.class);
        when(existing.getCronExpression()).thenReturn("0 0 9 * * ?");
        when(existing.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existing);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
    }

    @Test
    @DisplayName("resumeErrorTriggers should resume ERROR and PAUSED triggers")
    void resumeErrorTriggers_resumes() throws Exception {
        TriggerKey t1 = new TriggerKey("t1", "arcgis-triggers");
        TriggerKey t2 = new TriggerKey("t2", "arcgis-triggers");
        TriggerKey t3 = new TriggerKey("t3", "arcgis-triggers");
        when(scheduler.getTriggerKeys(any())).thenReturn(Set.of(t1, t2, t3));
        when(scheduler.getTriggerState(t1)).thenReturn(Trigger.TriggerState.ERROR);
        when(scheduler.getTriggerState(t2)).thenReturn(Trigger.TriggerState.PAUSED);
        when(scheduler.getTriggerState(t3)).thenReturn(Trigger.TriggerState.NORMAL);

        int resumed = service.resumeErrorTriggers();

        assertThat(resumed).isEqualTo(2);
        verify(scheduler).resumeTrigger(t1);
        verify(scheduler).resumeTrigger(t2);
        verify(scheduler, never()).resumeTrigger(t3);
    }

        // Additional branch coverage tests

    @Test
    @DisplayName("scheduleJob should wrap SchedulerException in SchedulingException")
    void scheduleJob_schedulerException_throws() throws Exception {
        ArcGISIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.scheduleJob(any(), any()))
                .thenThrow(new org.quartz.SchedulerException("quartz error"));

        assertThatThrownBy(() -> service.scheduleJob(integration))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("Failed to schedule job");
    }

    @Test
    @DisplayName("unscheduleJob should log warning when job not found")
    void unscheduleJob_jobNotFound_doesNotThrow() throws Exception {
        ArcGISIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(false);

        service.unscheduleJob(integration);

        verify(scheduler).deleteJob(any(JobKey.class));
    }

    @Test
    @DisplayName("unscheduleJob should wrap SchedulerException in SchedulingException")
    void unscheduleJob_schedulerException_throws() throws Exception {
        ArcGISIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class)))
                .thenThrow(new org.quartz.SchedulerException("quartz error"));

        assertThatThrownBy(() -> service.unscheduleJob(integration))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("Failed to unschedule job");
    }

    @Test
    @DisplayName("triggerJob should not add blank user to JobDataMap")
    void triggerJob_blankUser_omitsUserEntry() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        service.triggerJob(integrationId, "tenant-1", TriggerType.API, "");

        ArgumentCaptor<JobDataMap> captor = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(any(JobKey.class), captor.capture());
        assertThat(captor.getValue()).doesNotContainKey(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY_USER);
    }

    @Test
    @DisplayName("triggerJob with null triggerType omits TriggerType entry")
    void triggerJob_nullTriggerType_omitsTypeEntry() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        service.triggerJob(integrationId, "tenant-1", null, "user1");

        ArgumentCaptor<JobDataMap> captor = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(any(JobKey.class), captor.capture());
        assertThat(captor.getValue()).doesNotContainKey(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY);
    }

    @Test
    @DisplayName("reloadAllJobs should schedule new job when job does not exist")
    void reloadAllJobs_schedulesNew_whenJobMissing() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).scheduleJob(any(), any());
    }

    @Test
    @DisplayName("reloadAllJobs should handle orphan trigger (trigger exists without job)")
    void reloadAllJobs_orphanTrigger_unschedulesAndReschedules() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        // job does NOT exist, trigger DOES
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);
        when(scheduler.unscheduleJob(any(TriggerKey.class))).thenReturn(true);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).unscheduleJob(any(TriggerKey.class));
        verify(scheduler).scheduleJob(any(), any());
    }

    @Test
    @DisplayName("reloadAllJobs should recreate missing trigger when job exists but trigger missing")
    void reloadAllJobs_recreatesMissingTrigger() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).scheduleJob(any(Trigger.class));
    }

    @Test
    @DisplayName("reloadAllJobs should recreate trigger when getTrigger returns null")
    void reloadAllJobs_nullExistingTrigger_recreates() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).scheduleJob(any(Trigger.class));
    }

    @Test
    @DisplayName("reloadAllJobs should skip rescheduling when cron and timezone match")
    void reloadAllJobs_noMismatch_skipsReschedule() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        CronTrigger existing = org.mockito.Mockito.mock(CronTrigger.class);
        when(existing.getCronExpression()).thenReturn("0 0 10 * * ?");  // same as DB
        when(existing.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existing);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler, never()).rescheduleJob(any(), any());
    }

    @Test
    @DisplayName("reloadAllJobs should produce failure entry when exception thrown for one integration")
    void reloadAllJobs_oneFailure_addsFailureEntry() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class)))
                .thenThrow(new org.quartz.SchedulerException("quartz down"));

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("reloadAllJobs should handle non-CronTrigger gracefully")
    void reloadAllJobs_nonCronTrigger_skipsReschedule() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        // Use a non-CronTrigger type
        Trigger existing = org.mockito.Mockito.mock(Trigger.class);
        when(existing.getKey()).thenReturn(new TriggerKey("t", "arcgis-triggers"));
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existing);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler, never()).rescheduleJob(any(), any());
    }

    @Test
    @DisplayName("getJobSchedulingHealthStats returns expected and actual counts")
    void getJobSchedulingHealthStats_returnsStats() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ArcGISIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.getJobKeys(any())).thenReturn(Set.of(new JobKey("j1", "arcgis-integrations")));

        int[] stats = service.getJobSchedulingHealthStats();

        assertThat(stats[0]).isEqualTo(1); // expected
        assertThat(stats[1]).isEqualTo(1); // actual
    }

    @Test
    @DisplayName("getJobSchedulingHealthStats returns [-1,-1] on SchedulerException")
    void getJobSchedulingHealthStats_schedulerException_returnsMinusOne() throws Exception {
        when(arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of());
        when(scheduler.getJobKeys(any())).thenThrow(new org.quartz.SchedulerException("err"));

        int[] stats = service.getJobSchedulingHealthStats();

        assertThat(stats[0]).isEqualTo(-1);
        assertThat(stats[1]).isEqualTo(-1);
    }

    @Test
    @DisplayName("resumeErrorTriggers throws SchedulingException on SchedulerException")
    void resumeErrorTriggers_schedulerException_throwsWrapped() throws Exception {
        when(scheduler.getTriggerKeys(any())).thenThrow(new org.quartz.SchedulerException("err"));

        assertThatThrownBy(() -> service.resumeErrorTriggers())
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("Failed to resume error triggers");
    }
}
