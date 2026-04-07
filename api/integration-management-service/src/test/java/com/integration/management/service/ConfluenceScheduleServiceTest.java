package com.integration.management.service;

import com.integration.execution.contract.model.JobReloadResultDto;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.exception.SchedulingException;
import com.integration.management.repository.ConfluenceIntegrationRepository;
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
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceScheduleService")
class ConfluenceScheduleServiceTest {

    @Mock
    private Scheduler scheduler;
    @Mock
    private ConfluenceIntegrationRepository confluenceIntegrationRepository;

    @InjectMocks
    private ConfluenceScheduleService service;

    private static ConfluenceIntegration integrationWithSchedule(UUID id, String tenantId,
            String name, String cron) {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .id(UUID.randomUUID())
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .cronExpression(cron)
                .build();
        return ConfluenceIntegration.builder()
                .id(id)
                .tenantId(tenantId)
                .createdBy("user")
                .lastModifiedBy("user")
                .name(name)
                .documentItemType("DOCUMENT")
                .documentItemSubtype("DAILY_REPORT")
                .reportNameTemplate("Daily Report {{date}}")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .connectionId(UUID.randomUUID())
                .schedule(schedule)
                .isEnabled(true)
                .build();
    }

    @Test
    @DisplayName("scheduleJob should throw when schedule is missing")
    void scheduleJob_missingSchedule_throws() {
        ConfluenceIntegration integration = ConfluenceIntegration.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .createdBy("user")
                .lastModifiedBy("user")
                .name("Integration A")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("DAILY_REPORT")
                .reportNameTemplate("Daily Report {{date}}")
                .confluenceSpaceKey("SPACE")
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
        ConfluenceIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");

        service.scheduleJob(integration);

        verify(scheduler).scheduleJob(any(), any());
    }

    @Test
    @DisplayName("scheduleJob should wrap SchedulerException in SchedulingException")
    void scheduleJob_schedulerException_throws() throws Exception {
        ConfluenceIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.scheduleJob(any(), any()))
                .thenThrow(new org.quartz.SchedulerException("quartz error"));

        assertThatThrownBy(() -> service.scheduleJob(integration))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("Failed to schedule job");
    }

    @Test
    @DisplayName("unscheduleJob should delete job")
    void unscheduleJob_deletes() throws Exception {
        ConfluenceIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        service.unscheduleJob(integration);

        verify(scheduler).deleteJob(any(JobKey.class));
    }

    @Test
    @DisplayName("unscheduleJob should log warning when job not found")
    void unscheduleJob_jobNotFound_doesNotThrow() throws Exception {
        ConfluenceIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(false);

        service.unscheduleJob(integration);

        verify(scheduler).deleteJob(any(JobKey.class));
    }

    @Test
    @DisplayName("unscheduleJob should wrap SchedulerException in SchedulingException")
    void unscheduleJob_schedulerException_throws() throws Exception {
        ConfluenceIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class)))
                .thenThrow(new org.quartz.SchedulerException("quartz error"));

        assertThatThrownBy(() -> service.unscheduleJob(integration))
                .isInstanceOf(SchedulingException.class)
                .hasMessageContaining("Failed to unschedule job");
    }

    @Test
    @DisplayName("updateSchedule should unschedule existing job before scheduling replacement")
    void updateSchedule_unschedulesThenSchedules() throws Exception {
        ConfluenceIntegration integration = integrationWithSchedule(
                UUID.randomUUID(), "tenant-1", "Integration A", "0 0 10 * * ?");
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        service.updateSchedule(integration);

        verify(scheduler).deleteJob(any(JobKey.class));
        verify(scheduler).scheduleJob(any(), any());
    }

    @Test
    @DisplayName("updateSchedule should wrap unschedule failures and stop before scheduling")
    void updateSchedule_unscheduleFailure_throws() throws Exception {
        ConfluenceIntegration integration = integrationWithSchedule(
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
        assertThat(captor.getValue())
                .containsEntry(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY, "API");
        assertThat(captor.getValue())
                .containsEntry(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY_USER, "user-1");
    }

    @Test
    @DisplayName("triggerJob should not add blank user to JobDataMap")
    void triggerJob_blankUser_omitsUserEntry() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        service.triggerJob(integrationId, "tenant-1", TriggerType.API, "");

        ArgumentCaptor<JobDataMap> captor = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(any(JobKey.class), captor.capture());
        assertThat(captor.getValue())
                .doesNotContainKey(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY_USER);
    }

    @Test
    @DisplayName("triggerJob with null triggerType omits TriggerType entry")
    void triggerJob_nullTriggerType_omitsTypeEntry() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        service.triggerJob(integrationId, "tenant-1", null, "user1");

        ArgumentCaptor<JobDataMap> captor = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(any(JobKey.class), captor.capture());
        assertThat(captor.getValue())
                .doesNotContainKey(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY);
    }

    @Test
    @DisplayName("reloadAllJobs should reschedule when cron mismatch")
    void reloadAllJobs_reschedulesMismatch() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));

        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        CronTrigger existing = mock(CronTrigger.class);
        when(existing.getCronExpression()).thenReturn("0 0 9 * * ?");
        when(existing.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existing);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
    }

    @Test
    @DisplayName("reloadAllJobs should schedule new job when job does not exist")
    void reloadAllJobs_schedulesNew_whenJobMissing() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
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
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));

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
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
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
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
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
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        CronTrigger existing = mock(CronTrigger.class);
        when(existing.getCronExpression()).thenReturn("0 0 10 * * ?");
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
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
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
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        Trigger existing = mock(Trigger.class);
        when(existing.getKey()).thenReturn(new TriggerKey("t", "confluence-triggers"));
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existing);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler, never()).rescheduleJob(any(), any());
    }

    @Test
    @DisplayName("reloadAllJobs should handle null integration name gracefully")
    void reloadAllJobs_nullIntegrationName_handlesFailure() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", null, "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class)))
                .thenThrow(new org.quartz.SchedulerException("error"));

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isFalse();
        assertThat(results.getFirst().getIntegrationName()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("reloadAllJobs should handle multiple integrations with mixed success")
    void reloadAllJobs_multipleIntegrations_mixedResults() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        ConfluenceIntegration integration1 = integrationWithSchedule(
                id1, "tenant-1", "Integration A", "0 0 10 * * ?");
        ConfluenceIntegration integration2 = integrationWithSchedule(
                id2, "tenant-1", "Integration B", "0 0 11 * * ?");
        ConfluenceIntegration integration3 = integrationWithSchedule(
                id3, "tenant-1", "Integration C", "0 0 12 * * ?");

        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration1, integration2, integration3));

        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);

        when(scheduler.scheduleJob(any(), any()))
                .thenReturn(null)
                .thenThrow(new org.quartz.SchedulerException("error for integration 2"))
                .thenReturn(null);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(3);
        assertThat(results.stream().filter(JobReloadResultDto::isSuccess).count()).isEqualTo(2);
        assertThat(results.stream().filter(r -> !r.isSuccess()).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("triggerJob with SCHEDULER triggerType uses system user by default")
    void triggerJob_defaultParams_usesSchedulerAndSystemUser() throws Exception {
        UUID integrationId = UUID.randomUUID();
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        service.triggerJob(integrationId, "tenant-1");

        ArgumentCaptor<JobDataMap> captor = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(any(JobKey.class), captor.capture());
        assertThat(captor.getValue())
                .containsEntry(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY, "SCHEDULER");
        assertThat(captor.getValue())
                .containsEntry(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY_USER, "system");
    }

    @Test
    @DisplayName("scheduleJob should throw SchedulingException when integration ID is null")
    void scheduleJob_nullIntegrationId_throws() {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .id(UUID.randomUUID())
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .cronExpression("0 0 10 * * ?")
                .build();
        ConfluenceIntegration integration = ConfluenceIntegration.builder()
                .id(null)
                .tenantId("tenant-1")
                .createdBy("user")
                .lastModifiedBy("user")
                .name("Integration A")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("DAILY_REPORT")
                .reportNameTemplate("Daily Report {{date}}")
                .confluenceSpaceKey("SPACE")
                .connectionId(UUID.randomUUID())
                .schedule(schedule)
                .isEnabled(true)
                .build();

        assertThatThrownBy(() -> service.scheduleJob(integration))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("reloadAllJobs should reschedule when timezone mismatch")
    void reloadAllJobs_timezoneMismatch_reschedules() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        CronTrigger existing = mock(CronTrigger.class);
        when(existing.getCronExpression()).thenReturn("0 0 10 * * ?");
        when(existing.getTimeZone()).thenReturn(TimeZone.getTimeZone("America/New_York"));
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existing);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
    }

    @Test
    @DisplayName("reloadAllJobs should not reschedule when both cron and timezone are null")
    void reloadAllJobs_nullTimezone_handlesGracefully() throws Exception {
        UUID integrationId = UUID.randomUUID();
        ConfluenceIntegration integration = integrationWithSchedule(
                integrationId, "tenant-1", "Integration A", "0 0 10 * * ?");
        when(confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules())
                .thenReturn(List.of(integration));
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        CronTrigger existing = mock(CronTrigger.class);
        when(existing.getCronExpression()).thenReturn("0 0 10 * * ?");
        when(existing.getTimeZone()).thenReturn(null);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(existing);

        List<JobReloadResultDto> results = service.reloadAllJobs();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isSuccess()).isTrue();
        verify(scheduler).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
    }
}
