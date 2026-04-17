package com.integration.management.service;

import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.exception.IntegrationExecutionException;
import com.integration.management.mapper.IntegrationJobExecutionMapper;
import com.integration.management.repository.IntegrationJobExecutionRepository;
import com.integration.management.repository.IntegrationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationJobExecutionService {

    private final IntegrationJobExecutionRepository jobExecutionRepository;
    private final IntegrationScheduleRepository scheduleRepository;
    private final IntegrationJobExecutionMapper jobExecutionMapper;

    IntegrationJobExecution getPreviousJobExecution(UUID scheduleId) {
        return jobExecutionRepository.findTopByScheduleIdOrderByStartedAtDesc(scheduleId);
    }

    public IntegrationJobExecution getPreviousJobExecution(UUID scheduleId, UUID excludeExecutionId) {
        if (excludeExecutionId == null) {
            return getPreviousJobExecution(scheduleId);
        }
        return jobExecutionRepository.findTopByScheduleIdAndIdNotOrderByStartedAtDesc(scheduleId, excludeExecutionId);
    }

    @Transactional
    public IntegrationJobExecution createRunningExecution(
            UUID scheduleId,
            TriggerType triggeredBy,
            String triggeredByUser,
            String tenantId,
            Instant windowStart,
            Instant windowEnd) {
        IntegrationJobExecution execution = new IntegrationJobExecution();
        execution.setScheduleId(scheduleId);
        execution.setTriggeredBy(triggeredBy);
        execution.setTriggeredByUser(triggeredByUser);
        execution.setWindowStart(windowStart);
        execution.setWindowEnd(windowEnd);
        execution.setStatus(JobExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        return jobExecutionRepository.save(execution);
    }

    @Transactional
    public IntegrationJobExecution createRunningExecutionWithLineage(
            UUID scheduleId,
            TriggerType triggeredBy,
            String triggeredByUser,
            String tenantId,
            Instant windowStart,
            Instant windowEnd) {

        IntegrationJobExecution execution = createRunningExecution(
                scheduleId, triggeredBy, triggeredByUser, tenantId, windowStart, windowEnd);

        if (execution.getOriginalJobId() == null && execution.getId() != null) {
            execution.setOriginalJobId(execution.getId());
            execution.setRetryAttempt(0);
            return jobExecutionRepository.save(execution);
        }
        return execution;
    }

    @Transactional
    public IntegrationJobExecution createConfluenceRetryExecution(
            UUID originalJobId,
            UUID integrationId,
            String tenantId,
            TriggerType triggeredBy,
            String triggeredByUser) {

        IntegrationJobExecution latestOriginalJobExecution =
                findLatestRetriableConfluenceExecution(originalJobId, integrationId, tenantId);
        IntegrationJobExecution retryExecution = jobExecutionMapper.toRetryExecution(latestOriginalJobExecution);
        retryExecution.setTriggeredBy(triggeredBy);
        retryExecution.setTriggeredByUser(triggeredByUser);
        retryExecution.setRetryAttempt(latestOriginalJobExecution.getRetryAttempt() + 1);
        retryExecution.setStatus(JobExecutionStatus.RUNNING);
        return jobExecutionRepository.save(retryExecution);
    }

    @Transactional
    public void completeJobExecution(ArcGISExecutionResult result) {
        jobExecutionRepository.findById(result.getJobExecutionId()).ifPresentOrElse(execution -> {
            execution.setStatus(result.getStatus());
            execution.setCompletedAt(result.getCompletedAt() != null ? result.getCompletedAt() : Instant.now());
            execution.setTotalRecords(result.getTotalRecords());
            execution.setAddedRecords(result.getAddedRecords());
            execution.setUpdatedRecords(result.getUpdatedRecords());
            execution.setFailedRecords(result.getFailedRecords());
            execution.setAddedRecordsMetadata(result.getAddedRecordsMetadata());
            execution.setUpdatedRecordsMetadata(result.getUpdatedRecordsMetadata());
            execution.setTotalRecordsMetadata(result.getTotalRecordsMetadata());
            execution.setFailedRecordsMetadata(result.getFailedMetadata());
            execution.setErrorMessage(result.getErrorMessage());
            jobExecutionRepository.save(execution);
            advanceProcessedUntilOnSuccess(execution);
            log.info("Completed job execution {} with status {}", result.getJobExecutionId(), result.getStatus());
        }, () -> log.error("Job execution {} not found — cannot persist ArcGIS result",
                result.getJobExecutionId()));
    }

    public IntegrationJobExecution save(IntegrationJobExecution jobExecution) {
        return jobExecutionRepository.save(jobExecution);
    }

    /**
     * Persists the final result of a Confluence execution returned by IES via RabbitMQ.
     * Looks up the existing RUNNING record by {@code jobExecutionId} and updates it.
     */
    @Transactional
    public void completeConfluenceJobExecution(ConfluenceExecutionResult result) {
        jobExecutionRepository.findById(result.getJobExecutionId()).ifPresentOrElse(execution -> {
            execution.setStatus(result.getStatus());
            execution.setCompletedAt(result.getCompletedAt() != null ? result.getCompletedAt() : Instant.now());
            execution.setTotalRecords(result.getTotalRecords());
            execution.setExecutionMetadata(result.getExecutionMetadata());
            execution.setErrorMessage(result.getErrorMessage());
            jobExecutionRepository.save(execution);
            advanceProcessedUntilOnSuccess(execution);
            log.info("Completed Confluence job execution {} with status {}",
                    result.getJobExecutionId(), result.getStatus());
        }, () -> log.error("Job execution {} not found — cannot persist Confluence result",
                result.getJobExecutionId()));
    }

    /**
     * Advances {@code processedUntil} on the owning schedule when the execution succeeds.
     * This ensures the next run starts from where the last successful run ended.
     */
    private void advanceProcessedUntilOnSuccess(IntegrationJobExecution execution) {
        if (execution.getStatus() != JobExecutionStatus.SUCCESS || execution.getWindowEnd() == null) {
            return;
        }
        scheduleRepository.findById(execution.getScheduleId()).ifPresent(schedule -> {
            Instant nextProcessedUntil = execution.getWindowEnd();
            if (schedule.getTimeCalculationMode() == TimeCalculationMode.FIXED_DAY_BOUNDARY) {
                nextProcessedUntil = nextProcessedUntil.plusMillis(1);
            }
            schedule.setProcessedUntil(nextProcessedUntil);
            scheduleRepository.save(schedule);
            log.info("Advanced processedUntil to {} for schedule {}",
                    nextProcessedUntil, execution.getScheduleId());
        });
    }

    public List<IntegrationJobExecution> getConfluenceJobHistory(UUID integrationId, String tenantId) {
        return jobExecutionRepository.findByConfluenceIntegrationAndTenant(integrationId, tenantId);
    }

    public IntegrationJobExecution findLatestRetriableConfluenceExecution(UUID originalJobId,
                                                                          UUID integrationId,
                                                                          String tenantId) {
        return jobExecutionRepository
                .findConfluenceLatestRetriableExecutionByOriginalJobId(
                        originalJobId, integrationId, tenantId)
                .orElseThrow(() -> new IntegrationExecutionException(
                        String.format("No retriable (FAILED or ABORTED) execution found for originalJobId %s",
                                originalJobId)));
    }

}
