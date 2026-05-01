package com.integration.execution.mapper;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct mapper for building {@link ConfluenceExecutionResult} from the execution command
 * and the orchestrator result.
 */
@Mapper(componentModel = "spring")
public interface ConfluenceExecutionMapper {

    default ConfluenceExecutionResult toExecutionResult(
            final ConfluenceExecutionCommand command,
            final ConfluenceJobExecutionResult orchResult,
            final Instant startedAt) {
        JobExecutionStatus status = orchResult.errorMessage() == null
                ? JobExecutionStatus.SUCCESS : JobExecutionStatus.FAILED;
        return ConfluenceExecutionResult.builder()
                .jobExecutionId(command.getJobExecutionId())
                .status(status)
                .startedAt(startedAt)
                .completedAt(Instant.now())
                .totalRecords(orchResult.totalRecords())
                .executionMetadata(buildExecutionMetadata(orchResult))
                .errorMessage(orchResult.errorMessage())
                .build();
    }

    default Map<String, String> buildExecutionMetadata(final ConfluenceJobExecutionResult orchResult) {
        if (orchResult.pageUrl() == null) {
            return Map.of();
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("confluencePageUrl", orchResult.pageUrl());
        metadata.put("confluencePageId", orchResult.pageId() != null ? orchResult.pageId() : "");
        return Map.copyOf(metadata);
    }
}
