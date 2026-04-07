package com.integration.management.job;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.management.constants.ManagementSecurityConstants;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationJobExecution;

import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_CONFLUENCE;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.mapper.ConfluenceIntegrationMapper;
import com.integration.management.model.ExecutionWindow;
import com.integration.management.service.ConfluenceIntegrationService;
import com.integration.management.service.ConfluenceScheduleService;
import com.integration.management.service.ExecutionWindowResolverService;
import com.integration.management.service.IntegrationConnectionService;
import com.integration.management.service.IntegrationJobExecutionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Quartz job that runs in IMS for Confluence integrations.
 * Resolves the execution window via {@link ExecutionWindowResolverService}
 * (respecting the schedule's {@code timeCalculationMode} and {@code businessTimeZone}),
 * creates a RUNNING job-execution record, and publishes a
 * {@link ConfluenceExecutionCommand} to IES via messaging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class ConfluenceIntegrationJob implements Job {

    private final ConfluenceIntegrationService confluenceIntegrationService;
    private final ConfluenceIntegrationMapper confluenceIntegrationMapper;
    private final IntegrationConnectionService integrationConnectionService;
    private final IntegrationJobExecutionService integrationJobExecutionService;
    private final ExecutionWindowResolverService executionWindowResolver;
    private final MessagePublisher messagePublisher;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("ConfluenceIntegrationJob started execution in IMS");
        JobDataMap dataMap = context.getMergedJobDataMap();
        ConfluenceJobData jobData = extractJobData(dataMap);

        ConfluenceIntegration integration = confluenceIntegrationService
                .getEnabledIntegrationForScheduledExecution(jobData.integrationId(), jobData.tenantId())
                .orElseThrow(() -> new IntegrationNotFoundException(
                        "Confluence integration not found with id: " + jobData.integrationId()));

        TriggerType triggerType = extractTriggerType(dataMap);
        String triggeredByUser = extractTriggeredByUser(dataMap);
        Instant triggerTime = Instant.now();

        ExecutionWindow window = executionWindowResolver.resolve(
                integration.getSchedule(), triggerTime, jobData.tenantId(), INTEGRATION_TYPE_CONFLUENCE);

        IntegrationJobExecution jobExecution = integrationJobExecutionService
                .createRunningExecutionWithLineage(integration.getSchedule().getId(),
                        triggerType, triggeredByUser, jobData.tenantId(), window.windowStart(), window.windowEnd());

        String connectionSecretName = integrationConnectionService
                .getIntegrationConnectionNameById(integration.getConnectionId().toString(), jobData.tenantId());

        ConfluenceExecutionCommand command = confluenceIntegrationMapper.toExecutionCommand(
                integration,
                jobExecution,
                connectionSecretName,
                jobData.tenantId(),
                triggerType,
                triggeredByUser,
                window.windowStart(),
                window.windowEnd());

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_COMMAND_QUEUE,
                command);
        log.info("Published ConfluenceExecutionCommand "
                + "for integration: {} jobExecutionId: {} window: [{}, {}]",
                integration.getId(), jobExecution.getId(), window.windowStart(), window.windowEnd());
    }

    private ConfluenceJobData extractJobData(JobDataMap dataMap) throws JobExecutionException {
        try {
            return new ConfluenceJobData(
                    UUID.fromString(dataMap.getString("integrationId")),
                    dataMap.getString("tenantId"));
        } catch (Exception e) {
            log.error("Failed to extract job data from JobDataMap: {}", dataMap, e);
            throw new JobExecutionException("Invalid job data configuration", e);
        }
    }

    private TriggerType extractTriggerType(JobDataMap dataMap) {
        Object raw = dataMap.get(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY);
        if (raw instanceof TriggerType triggerType) {
            return triggerType;
        }
        if (raw instanceof String value && !value.isBlank()) {
            try {
                return TriggerType.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                log.warn("Unknown trigger type value in JobDataMap: {}", value);
            }
        }
        return TriggerType.SCHEDULER;
    }

    private String extractTriggeredByUser(JobDataMap dataMap) {
        Object raw = dataMap.get(ConfluenceScheduleService.JOB_DATA_TRIGGERED_BY_USER);
        if (raw instanceof String value && !value.isBlank()) {
            return value;
        }
        return ManagementSecurityConstants.SYSTEM_USER;
    }

    private record ConfluenceJobData(@NonNull UUID integrationId, @NonNull String tenantId) {
    }
}
