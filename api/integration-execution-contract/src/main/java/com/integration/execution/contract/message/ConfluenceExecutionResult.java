package com.integration.execution.contract.message;

import com.integration.execution.contract.model.enums.JobExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Message sent from IES back to IMS via RabbitMQ after a Confluence execution completes.
 * IMS uses this to persist the final state of the IntegrationJobExecution record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfluenceExecutionResult {

    private UUID jobExecutionId;
    private JobExecutionStatus status;
    private Instant startedAt;
    private Instant completedAt;

    private int totalRecords;
    private int addedRecords;
    private int updatedRecords;
    private int failedRecords;

    /** Confluence page URL and page ID stored as generic key-value pairs. */
    private Map<String, String> executionMetadata;

    private String errorMessage;
}
