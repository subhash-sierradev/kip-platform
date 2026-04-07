package com.integration.execution.messaging;

import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.ArcGISJobExecutionResult;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.service.processor.KwToArcGISOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArcGISExecutionCommandListener {

    private final KwToArcGISOrchestrator orchestrator;
    private final MessagePublisher messagePublisher;
    private final NotificationEventPublisher notificationEventPublisher;

    public void handleExecutionCommand(final ArcGISExecutionCommand command) {
        log.info("Received ArcGIS execution command for integration: {} (jobExecutionId: {})",
                command.getIntegrationId(), command.getJobExecutionId());

        Instant startedAt = Instant.now();
        ArcGISExecutionResult result;

        try {
            ArcGISJobExecutionResult executionResult = orchestrator.processExecution(command);

            result = ArcGISExecutionResult.builder()
                    .jobExecutionId(command.getJobExecutionId())
                    .status(executionResult.failedRecords() == 0
                            ? JobExecutionStatus.SUCCESS
                            : JobExecutionStatus.FAILED)
                    .startedAt(startedAt)
                    .completedAt(Instant.now())
                    .totalRecords(executionResult.totalRecords())
                    .addedRecords(executionResult.addedRecords())
                    .updatedRecords(executionResult.updatedRecords())
                    .failedRecords(executionResult.failedRecords())
                    .addedRecordsMetadata(executionResult.addedRecordsMetadata())
                    .updatedRecordsMetadata(executionResult.updatedRecordsMetadata())
                    .totalRecordsMetadata(executionResult.totalRecordsMetadata())
                    .failedMetadata(executionResult.failedRecordsMetadata())
                    .errorMessage(executionResult.errorMessage())
                    .build();

            log.info("Successfully processed ArcGIS integration: {} — added: {}, updated: {}, failed: {}",
                    command.getIntegrationId(),
                    executionResult.addedRecords(),
                    executionResult.updatedRecords(),
                    executionResult.failedRecords());

        } catch (Exception e) {
            log.error("Fatal error processing ArcGIS execution command for integration: {}",
                    command.getIntegrationId(), e);

            result = ArcGISExecutionResult.builder()
                    .jobExecutionId(command.getJobExecutionId())
                    .status(JobExecutionStatus.FAILED)
                    .startedAt(startedAt)
                    .completedAt(Instant.now())
                    .totalRecords(0)
                    .addedRecords(0)
                    .updatedRecords(0)
                    .failedRecords(0)
                    .errorMessage(e.getMessage())
                    .build();
        }

        publishCompletionEvent(command, result);

        messagePublisher.publish(
                QueueNames.ARCGIS_EXCHANGE,
                QueueNames.ARCGIS_EXECUTION_RESULT_QUEUE,
                result);

        log.info("Published ArcGIS execution result for jobExecutionId: {} with status: {}",
                result.getJobExecutionId(), result.getStatus());
    }

    private void publishCompletionEvent(final ArcGISExecutionCommand command,
            final ArcGISExecutionResult result) {
        if (result.getStatus() == JobExecutionStatus.SUCCESS) {
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(NotificationEventKey.ARCGIS_INTEGRATION_JOB_COMPLETED.name())
                    .tenantId(command.getTenantId())
                    .triggeredByUserId(command.getTriggeredByUser())
                    .metadata(java.util.Map.of(
                            "integrationName",
                            command.getIntegrationName() != null ? command.getIntegrationName() : "",
                            "integrationId", command.getIntegrationId().toString(),
                            "recordCount", result.getTotalRecords()))
                    .build());
        } else {
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(NotificationEventKey.ARCGIS_INTEGRATION_JOB_FAILED.name())
                    .tenantId(command.getTenantId())
                    .triggeredByUserId(command.getTriggeredByUser())
                    .metadata(java.util.Map.of(
                            "integrationName",
                            command.getIntegrationName() != null ? command.getIntegrationName() : "",
                            "integrationId", command.getIntegrationId().toString(),
                            "errorMessage",
                            result.getErrorMessage() != null ? result.getErrorMessage() : ""))
                    .build());
        }
    }
}
