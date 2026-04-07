package com.integration.management.service;

import com.integration.execution.contract.model.JobReloadResultDto;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.entity.SchedulableIntegration;
import com.integration.management.exception.SchedulingException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.SYSTEM_USER;

/**
 * Abstract base service for managing Quartz job schedules for scheduled integrations.
 * Subclasses supply the job group, trigger group, Quartz job class, a human-readable
 * integration label, and a repository query via five template methods.
 *
 * @param <T> the integration entity type; must implement {@link SchedulableIntegration}
 */
@Slf4j
@Transactional(readOnly = true)
public abstract class BaseIntegrationScheduleService<T extends SchedulableIntegration> {

    public static final String JOB_DATA_TRIGGERED_BY = "triggeredBy";
    public static final String JOB_DATA_TRIGGERED_BY_USER = "triggeredByUser";

    private static final String UNKNOWN = "Unknown";

    protected final Scheduler scheduler;

    protected BaseIntegrationScheduleService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    // -------------------------------------------------------------------------
    // Template methods — must be implemented by each concrete schedule service
    // -------------------------------------------------------------------------

    protected abstract String getJobGroup();

    protected abstract String getTriggerGroup();

    protected abstract Class<? extends Job> getJobClass();

    /** Human-readable label used in log messages and Quartz job descriptions, e.g. "ArcGIS". */
    protected abstract String getIntegrationLabel();

    protected abstract List<T> findAllIntegrationsWithActiveSchedules();

    @Transactional
    public void scheduleJob(T integration) throws SchedulingException {
        doScheduleJob(integration);
    }

    @Transactional
    public void updateSchedule(T integration) throws SchedulingException {
        try {
            doUnscheduleJob(integration);
            doScheduleJob(integration);
        } catch (Exception e) {
            log.error("Failed to update {} integration job schedule for integration: {}",
                    getIntegrationLabel(), integration.getId(), e);
            throw new SchedulingException("Failed to update job schedule for integration: "
                    + integration.getId(), e);
        }
    }

    @Transactional
    public void unscheduleJob(T integration) throws SchedulingException {
        doUnscheduleJob(integration);
    }

    private void doScheduleJob(T integration) throws SchedulingException {
        log.info("Scheduling {} integration job for integration: {} and tenant: {}",
                getIntegrationLabel(), integration.getId(), integration.getTenantId());
        try {
            IntegrationSchedule schedule = integration.getSchedule();
            if (schedule == null) {
                throw new SchedulingException("No schedule configuration found for integration: "
                        + integration.getId());
            }
            JobKey jobKey = createJobKey(integration.getId(), integration.getTenantId());
            JobDetail jobDetail = JobBuilder.newJob(getJobClass())
                    .withIdentity(jobKey)
                    .withDescription(getIntegrationLabel() + " Integration: " + integration.getName())
                    .usingJobData(createJobDataMap(integration.getId(), integration.getTenantId(), schedule.getId()))
                    .storeDurably(false)
                    .requestRecovery(true)
                    .build();

            Date startDate = computeTriggerStartDate(schedule);
            TriggerKey triggerKey = createTriggerKey(integration.getId(), integration.getTenantId());
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withDescription("Trigger for " + getIntegrationLabel() + " Integration: " + integration.getName())
                    .forJob(jobDetail)
                    .startAt(startDate)
                    .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronExpression())
                            .inTimeZone(TimeZone.getTimeZone("UTC"))
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Successfully scheduled {} integration job: {} with cron: {}",
                    getIntegrationLabel(), jobKey, schedule.getCronExpression());
        } catch (SchedulerException e) {
            log.error("Failed to schedule {} integration job for integration: {}",
                    getIntegrationLabel(), integration.getId(), e);
            throw new SchedulingException("Failed to schedule job for integration: " + integration.getId(), e);
        }
    }

    private void doUnscheduleJob(T integration) throws SchedulingException {
        log.info("Unscheduling {} integration job for integration: {} and tenant: {}",
                getIntegrationLabel(), integration.getId(), integration.getTenantId());
        try {
            JobKey jobKey = createJobKey(integration.getId(), integration.getTenantId());
            boolean deleted = scheduler.deleteJob(jobKey);
            if (deleted) {
                log.info("Successfully unscheduled {} integration job: {}", getIntegrationLabel(), jobKey);
            } else {
                log.warn("{} integration job not found for deletion: {}", getIntegrationLabel(), jobKey);
            }
        } catch (SchedulerException e) {
            log.error("Failed to unschedule {} integration job for integration: {}",
                    getIntegrationLabel(), integration.getId(), e);
            throw new SchedulingException("Failed to unschedule job for integration: " + integration.getId(), e);
        }
    }

    @Transactional
    public void triggerJob(UUID integrationId, String tenantId) throws SchedulingException {
        doTriggerJob(integrationId, tenantId, TriggerType.SCHEDULER, SYSTEM_USER);
    }

    @Transactional
    public void triggerJob(UUID integrationId, String tenantId, TriggerType triggerType,
            String triggeredByUser) throws SchedulingException {
        doTriggerJob(integrationId, tenantId, triggerType, triggeredByUser);
    }

    private void doTriggerJob(UUID integrationId, String tenantId, TriggerType triggerType,
            String triggeredByUser) throws SchedulingException {
        log.info("Manually triggering {} integration job for integration: {} tenant: {} "
                + "(triggerType={}, user={})", getIntegrationLabel(), integrationId, tenantId,
                triggerType, triggeredByUser);
        try {
            JobKey jobKey = createJobKey(integrationId, tenantId);
            if (!scheduler.checkExists(jobKey)) {
                throw new SchedulingException("Job not scheduled for integration: " + integrationId);
            }
            JobDataMap jobDataMap = new JobDataMap();
            if (triggerType != null) {
                jobDataMap.put(JOB_DATA_TRIGGERED_BY, triggerType.name());
            }
            if (triggeredByUser != null && !triggeredByUser.isBlank()) {
                jobDataMap.put(JOB_DATA_TRIGGERED_BY_USER, triggeredByUser);
            }
            scheduler.triggerJob(jobKey, jobDataMap);
            log.info("Successfully triggered {} integration job: {}", getIntegrationLabel(), jobKey);
        } catch (SchedulerException e) {
            log.error("Failed to trigger {} integration job for integration: {}",
                    getIntegrationLabel(), integrationId, e);
            throw new SchedulingException("Failed to trigger job for integration: " + integrationId, e);
        }
    }

    @Transactional
    public List<JobReloadResultDto> reloadAllJobs() {
        log.info("Starting bulk job reload for all active {} integration schedules", getIntegrationLabel());
        List<JobReloadResultDto> results = new ArrayList<>();
        List<T> integrationsWithSchedules = findAllIntegrationsWithActiveSchedules();
        log.info("Found {} {} integrations with active schedules to reload",
                integrationsWithSchedules.size(), getIntegrationLabel());

        for (T integration : integrationsWithSchedules) {
            try {
                results.add(reloadOneIntegration(integration));
            } catch (Exception e) {
                String integrationName = integration.getName() != null ? integration.getName() : UNKNOWN;
                UUID integrationId = integration.getId();
                String tenantId = integration.getTenantId();
                results.add(JobReloadResultDto.failure(integrationId, tenantId, integrationName, e.getMessage()));
                log.error("Failed to reload job for {} integration: {} ({}): {}",
                        getIntegrationLabel(), integrationId, integrationName, e.getMessage());
            }
        }

        long successCount = results.stream().filter(JobReloadResultDto::isSuccess).count();
        long failureCount = results.size() - successCount;
        log.info("{} bulk job reload completed: {} successful, {} failed out of {} total",
                getIntegrationLabel(), successCount, failureCount, results.size());
        return results;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private JobReloadResultDto reloadOneIntegration(T integration) throws SchedulerException {
        String integrationName = integration.getName() != null ? integration.getName() : UNKNOWN;
        IntegrationSchedule schedule = integration.getSchedule();

        if (schedule == null) {
            throw new SchedulingException("No schedule configuration found for integration: " + integration.getId());
        }

        UUID integrationId = integration.getId();
        String tenantId = integration.getTenantId();
        String cronExpression = schedule.getCronExpression();
        JobKey jobKey = createJobKey(integrationId, tenantId);
        TriggerKey triggerKey = createTriggerKey(integrationId, tenantId);

        boolean jobExists = scheduler.checkExists(jobKey);
        boolean triggerExists = scheduler.checkExists(triggerKey);

        if (!jobExists && triggerExists) {
            log.warn("Found orphan trigger without job for {} integration: {} ({}). Unscheduling trigger: {}",
                    getIntegrationLabel(), integrationId, integrationName, triggerKey.getName());
            scheduler.unscheduleJob(triggerKey);
            triggerExists = false;
        }

        if (!jobExists) {
            doScheduleJob(integration);
            log.debug("Successfully reloaded job for {} integration: {} ({})",
                    getIntegrationLabel(), integrationId, integrationName);
            return JobReloadResultDto.success(integrationId, tenantId, cronExpression, integrationName);
        }

        if (!triggerExists) {
            scheduler.scheduleJob(buildCronTrigger(triggerKey, jobKey, integrationName, schedule));
            log.info("Recreated missing trigger for existing {} job: {} / {}",
                    getIntegrationLabel(), jobKey.getName(), triggerKey.getName());
            return JobReloadResultDto.success(integrationId, tenantId, cronExpression, integrationName);
        }

        Trigger existingTrigger = scheduler.getTrigger(triggerKey);
        if (existingTrigger == null) {
            scheduler.scheduleJob(buildCronTrigger(triggerKey, jobKey, integrationName, schedule));
            log.warn("TriggerKey exists but trigger could not be loaded; recreated trigger: {}",
                    triggerKey.getName());
            return JobReloadResultDto.success(integrationId, tenantId, cronExpression, integrationName);
        }

        if (existingTrigger instanceof CronTrigger cronTrigger) {
            rescheduleIfMismatch(cronTrigger, triggerKey, jobKey, integrationId, integrationName, schedule);
        } else {
            log.warn("Existing {} trigger is not a CronTrigger; leaving as-is: {}",
                    getIntegrationLabel(), existingTrigger.getKey().getName());
        }
        return JobReloadResultDto.success(integrationId, tenantId, cronExpression, integrationName);
    }

    private void rescheduleIfMismatch(
            CronTrigger cronTrigger,
            TriggerKey triggerKey,
            JobKey jobKey,
            UUID integrationId,
            String integrationName,
            IntegrationSchedule schedule) throws SchedulerException {
        String dbCron = schedule.getCronExpression();
        String quartzCron = cronTrigger.getCronExpression();
        String quartzTz = cronTrigger.getTimeZone() != null ? cronTrigger.getTimeZone().getID() : null;
        String expectedTz = TimeZone.getTimeZone("UTC").getID();

        if (dbCron.equals(quartzCron) && expectedTz.equals(quartzTz)) {
            return;
        }

        Trigger newTrigger = buildCronTrigger(triggerKey, jobKey, integrationName, schedule);
        scheduler.rescheduleJob(triggerKey, newTrigger);
        log.info("Rescheduled {} trigger for integration: {} ({}) cron={} tz={} -> cron={} tz={}",
                getIntegrationLabel(), integrationId, integrationName, quartzCron, quartzTz, dbCron, expectedTz);
    }

    private Trigger buildCronTrigger(TriggerKey triggerKey, JobKey jobKey,
            String integrationName, IntegrationSchedule schedule) {
        Date startDate = computeTriggerStartDate(schedule);
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withDescription("Trigger for " + getIntegrationLabel() + " Integration: " + integrationName)
                .forJob(jobKey)
                .startAt(startDate)
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronExpression())
                        .inTimeZone(TimeZone.getTimeZone("UTC"))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    private Date computeTriggerStartDate(IntegrationSchedule schedule) {
        if (schedule.getExecutionDate() == null) {
            return new Date();
        }
        return Date.from(
                schedule.getExecutionDate()
                        .atTime(schedule.getExecutionTime())
                        .atZone(ZoneId.systemDefault())
                        .toInstant());
    }

    protected JobKey createJobKey(UUID integrationId, String tenantId) {
        return new JobKey(tenantId + "-" + integrationId.toString(), getJobGroup());
    }

    protected TriggerKey createTriggerKey(UUID integrationId, String tenantId) {
        return new TriggerKey(tenantId + "-" + integrationId.toString(), getTriggerGroup());
    }

    private JobDataMap createJobDataMap(UUID integrationId, String tenantId, UUID scheduleId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("integrationId", integrationId.toString());
        jobDataMap.put("tenantId", tenantId);
        jobDataMap.put("scheduleId", scheduleId.toString());
        return jobDataMap;
    }
}
