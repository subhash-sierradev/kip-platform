package com.integration.execution.messaging;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.messaging.MessagePublisher;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.queue.QueueNames;
import com.integration.execution.mapper.ConfluenceExecutionMapper;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import com.integration.execution.service.processor.KwToConfluenceOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Listens on the Confluence execution command queue, processes each job via
 * {@link KwToConfluenceOrchestrator}, and publishes a {@link ConfluenceExecutionResult} back to IMS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceExecutionCommandListener {

    private final KwToConfluenceOrchestrator orchestrator;
    private final MessagePublisher messagePublisher;
    private final NotificationEventPublisher notificationEventPublisher;
    private final ConfluenceExecutionMapper confluenceExecutionMapper;

    public void handleExecutionCommand(final ConfluenceExecutionCommand command) {
        log.info("Received Confluence execution command for integration: {} (jobExecutionId: {})",
                command.getIntegrationId(), command.getJobExecutionId());

        Instant startedAt = Instant.now();
        ConfluenceJobExecutionResult executionResult = orchestrator.processExecution(command);

        ConfluenceExecutionResult result = confluenceExecutionMapper
                .toExecutionResult(command, executionResult, startedAt);

        publishCompletionEvent(command, result);

        messagePublisher.publish(
                QueueNames.CONFLUENCE_EXCHANGE,
                QueueNames.CONFLUENCE_EXECUTION_RESULT_QUEUE,
                result);

        log.info("Published Confluence execution result for jobExecutionId: {} with status: {}",
                result.getJobExecutionId(), result.getStatus());
    }

    private void publishCompletionEvent(final ConfluenceExecutionCommand command,
            final ConfluenceExecutionResult result) {
        String integrationName = command.getIntegrationName() != null ? command.getIntegrationName() : "";
        String integrationId = command.getIntegrationId() != null ? command.getIntegrationId().toString() : "";

        if (result.getStatus() == JobExecutionStatus.SUCCESS) {
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(NotificationEventKey.CONFLUENCE_INTEGRATION_JOB_COMPLETED.name())
                    .tenantId(command.getTenantId())
                    .triggeredByUserId(command.getTriggeredByUser())
                    .metadata(Map.of(
                            "integrationName", integrationName,
                            "integrationId", integrationId,
                            "recordCount", result.getTotalRecords()))
                    .build());
        } else {
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .eventKey(NotificationEventKey.CONFLUENCE_INTEGRATION_JOB_FAILED.name())
                    .tenantId(command.getTenantId())
                    .triggeredByUserId(command.getTriggeredByUser())
                    .metadata(Map.of(
                            "integrationName", integrationName,
                            "integrationId", integrationId,
                            "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : ""))
                    .build());
        }
    }
}
