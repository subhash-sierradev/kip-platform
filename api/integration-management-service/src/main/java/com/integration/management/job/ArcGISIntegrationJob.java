package com.integration.management.job;

import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.management.constants.ManagementSecurityConstants;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.entity.IntegrationJobExecution;

import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_ARCGIS;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.model.ExecutionWindow;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.service.ArcGISIntegrationService;
import com.integration.management.service.ArcGISScheduleService;
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
import java.util.List;
import java.util.UUID;

/**
 * Quartz job that runs in IMS, builds an {@link ArcGISExecutionCommand},
 * creates a RUNNING job-execution record and publishes the command to IES via messaging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class ArcGISIntegrationJob implements Job {

    private final ArcGISIntegrationRepository arcGISIntegrationRepository;
    private final ArcGISIntegrationService arcGISIntegrationService;
    private final IntegrationConnectionService integrationConnectionService;
    private final IntegrationJobExecutionService integrationJobExecutionService;
    private final ExecutionWindowResolverService executionWindowResolver;
    private final MessagePublisher messagePublisher;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("ArcGISIntegrationJob started execution in IMS");
        JobDataMap dataMap = context.getMergedJobDataMap();
        ArcGISJobData jobData = extractJobData(dataMap);

        ArcGISIntegration integration;
        try {
            integration = arcGISIntegrationRepository
                    .findByIdAndTenantIdAndIsDeletedFalseWithSchedule(jobData.integrationId(), jobData.tenantId())
                    .orElseThrow(() -> new IntegrationNotFoundException(
                            "ArcGIS integration not found/enabled [integrationId=" + jobData.integrationId()
                                    + ", tenantId=" + jobData.tenantId() + "]"));
        } catch (IntegrationNotFoundException ex) {
            log.warn("Skipping ArcGIS job; integration not found/enabled [integrationId={}, tenantId={}]: {}",
                    jobData.integrationId(), jobData.tenantId(), ex.getMessage());
            return;
        }

        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            log.warn("Skipping ArcGIS job; integration is disabled [integrationId={}]", jobData.integrationId());
            return;
        }

        TriggerType triggerType = extractTriggerType(dataMap);
        String triggeredByUser = extractTriggeredByUser(dataMap);

        // Resolve execution window via centralized resolver (respects TimeCalculationMode)
        IntegrationSchedule schedule = integration.getSchedule();
        UUID scheduleId = schedule.getId();
        Instant triggerTime = Instant.now();

        ExecutionWindow window = executionWindowResolver.resolve(
                schedule, triggerTime, jobData.tenantId(), INTEGRATION_TYPE_ARCGIS);
        Instant windowStart = window.windowStart();
        Instant windowEnd = window.windowEnd();

        // Persist a RUNNING execution record so IES can reference it by jobExecutionId
        IntegrationJobExecution jobExecution = integrationJobExecutionService.createRunningExecution(
                scheduleId, triggerType, triggeredByUser, jobData.tenantId(), windowStart, windowEnd);

        // Resolve field mappings (already as DTOs from contract)
        List<IntegrationFieldMappingDto> fieldMappings =
                arcGISIntegrationService.getFieldMappings(jobData.integrationId(), jobData.tenantId());

        // Resolve connection secret name (vault key used by IES to fetch credentials)
        String connectionSecretName = integrationConnectionService
                .getIntegrationConnectionNameById(integration.getConnectionId().toString(), jobData.tenantId());

        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder()
                .integrationId(integration.getId())
                .jobExecutionId(jobExecution.getId())
                .integrationName(integration.getName())
                .connectionSecretName(connectionSecretName)
                .itemType(integration.getItemType())
                .itemSubtype(integration.getItemSubtype())
                .dynamicDocumentType(integration.getDynamicDocumentType())
                .tenantId(jobData.tenantId())
                .triggeredBy(triggerType)
                .triggeredByUser(triggeredByUser)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .fieldMappings(fieldMappings)
                .build();

        messagePublisher.publish(QueueNames.ARCGIS_EXCHANGE, QueueNames.ARCGIS_EXECUTION_COMMAND_QUEUE, command);
        log.info("Published ArcGISExecutionCommand for integration: {} jobExecutionId: {}",
                integration.getId(), jobExecution.getId());
    }

    private ArcGISJobData extractJobData(JobDataMap dataMap) throws JobExecutionException {
        try {
            return new ArcGISJobData(
                    UUID.fromString(dataMap.getString("integrationId")),
                    dataMap.getString("tenantId"));
        } catch (Exception e) {
            log.error("Failed to extract job data from JobDataMap: {}", dataMap, e);
            throw new JobExecutionException("Invalid job data configuration", e);
        }
    }

    private TriggerType extractTriggerType(JobDataMap dataMap) {
        Object raw = dataMap.get(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY);
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
        Object raw = dataMap.get(ArcGISScheduleService.JOB_DATA_TRIGGERED_BY_USER);
        if (raw instanceof String value && !value.isBlank()) {
            return value;
        }
        return ManagementSecurityConstants.SYSTEM_USER;
    }

    private record ArcGISJobData(@NonNull UUID integrationId, @NonNull String tenantId) {
    }
}
