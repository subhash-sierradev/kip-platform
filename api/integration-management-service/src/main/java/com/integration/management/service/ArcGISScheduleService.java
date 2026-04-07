package com.integration.management.service;

import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.exception.SchedulingException;
import com.integration.management.job.ArcGISIntegrationJob;
import com.integration.management.repository.ArcGISIntegrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing ArcGIS integration Quartz job schedules within IMS.
 * Directly manages Quartz jobs in-process; no longer delegates to IES via Feign.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ArcGISScheduleService extends BaseIntegrationScheduleService<ArcGISIntegration> {

    private static final String JOB_GROUP = "arcgis-integrations";
    private static final String TRIGGER_GROUP = "arcgis-triggers";

    private final ArcGISIntegrationRepository arcGISIntegrationRepository;

    public ArcGISScheduleService(Scheduler scheduler, ArcGISIntegrationRepository arcGISIntegrationRepository) {
        super(scheduler);
        this.arcGISIntegrationRepository = arcGISIntegrationRepository;
    }

    @Override
    protected String getJobGroup() {
        return JOB_GROUP;
    }

    @Override
    protected String getTriggerGroup() {
        return TRIGGER_GROUP;
    }

    @Override
    protected Class<? extends Job> getJobClass() {
        return ArcGISIntegrationJob.class;
    }

    @Override
    protected String getIntegrationLabel() {
        return "ArcGIS";
    }

    @Override
    protected List<ArcGISIntegration> findAllIntegrationsWithActiveSchedules() {
        return arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules();
    }

    public int[] getJobSchedulingHealthStats() {
        try {
            List<ArcGISIntegration> integrationsWithSchedules = arcGISIntegrationRepository
                    .findAllIntegrationsWithActiveSchedules();
            int expectedCount = integrationsWithSchedules.size();
            int actualCount = scheduler.getJobKeys(
                    org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(JOB_GROUP)).size();
            return new int[]{expectedCount, actualCount};
        } catch (SchedulerException e) {
            log.error("Error getting job scheduling health stats", e);
            return new int[]{-1, -1};
        }
    }

    public int resumeErrorTriggers() {
        log.info("Resuming all triggers in ERROR state");
        int resumedCount = 0;
        try {
            var triggerKeys = scheduler.getTriggerKeys(
                    org.quartz.impl.matchers.GroupMatcher.triggerGroupEquals(TRIGGER_GROUP));
            for (TriggerKey triggerKey : triggerKeys) {
                Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
                if (state == Trigger.TriggerState.ERROR || state == Trigger.TriggerState.PAUSED) {
                    scheduler.resumeTrigger(triggerKey);
                    resumedCount++;
                    log.info("Resumed trigger (state={}): {}", state, triggerKey.getName());
                }
            }
            log.info("Successfully resumed {} triggers from ERROR state", resumedCount);
            return resumedCount;
        } catch (SchedulerException e) {
            log.error("Error resuming triggers in ERROR state", e);
            throw new SchedulingException("Failed to resume error triggers", e);
        }
    }

}
