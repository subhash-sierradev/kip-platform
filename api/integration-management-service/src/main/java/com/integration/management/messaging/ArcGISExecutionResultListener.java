package com.integration.management.messaging;

import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.management.service.IntegrationJobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listens on the ArcGIS execution result queue for completion messages from IES.
 * On receipt, persists the final state of the corresponding {@code IntegrationJobExecution} record.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArcGISExecutionResultListener {

    private final IntegrationJobExecutionService jobExecutionService;

    public void onExecutionResult(ArcGISExecutionResult result) {
        log.info("Received ArcGIS execution result for job {}: status={}",
                result.getJobExecutionId(), result.getStatus());
        try {
            jobExecutionService.completeJobExecution(result);
        } catch (Exception e) {
            log.error("Failed to persist ArcGIS execution result for job {}: {}",
                    result.getJobExecutionId(), e.getMessage(), e);
            throw e;
        }
    }
}
