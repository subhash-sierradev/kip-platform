package com.integration.management.service;

import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.job.ConfluenceIntegrationJob;
import com.integration.management.repository.ConfluenceIntegrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing Confluence integration Quartz job schedules within IMS.
 * Extends {@link BaseIntegrationScheduleService} - all scheduling logic lives there.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ConfluenceScheduleService extends BaseIntegrationScheduleService<ConfluenceIntegration> {

    private static final String JOB_GROUP = "confluence-integrations";
    private static final String TRIGGER_GROUP = "confluence-triggers";

    private final ConfluenceIntegrationRepository confluenceIntegrationRepository;

    public ConfluenceScheduleService(Scheduler scheduler,
            ConfluenceIntegrationRepository confluenceIntegrationRepository) {
        super(scheduler);
        this.confluenceIntegrationRepository = confluenceIntegrationRepository;
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
        return ConfluenceIntegrationJob.class;
    }

    @Override
    protected String getIntegrationLabel() {
        return "Confluence";
    }

    @Override
    protected List<ConfluenceIntegration> findAllIntegrationsWithActiveSchedules() {
        return confluenceIntegrationRepository.findAllIntegrationsWithActiveSchedules();
    }
}
