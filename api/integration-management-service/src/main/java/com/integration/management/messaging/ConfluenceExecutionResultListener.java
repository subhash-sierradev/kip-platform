package com.integration.management.messaging;

import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.management.service.IntegrationJobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listens on the Confluence execution result queue for completion messages from IES.
 * On receipt, persists the final state of the corresponding {@code IntegrationJobExecution} record.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceExecutionResultListener {

    private final IntegrationJobExecutionService jobExecutionService;

    public void onExecutionResult(ConfluenceExecutionResult result) {
        log.info("Received Confluence execution result for job {}: status={}",
                result.getJobExecutionId(), result.getStatus());
        try {
            jobExecutionService.completeConfluenceJobExecution(result);
        } catch (Exception e) {
            log.error("Failed to persist Confluence execution result for job {}: {}",
                    result.getJobExecutionId(), e.getMessage(), e);
            throw e;
        }
    }
}
