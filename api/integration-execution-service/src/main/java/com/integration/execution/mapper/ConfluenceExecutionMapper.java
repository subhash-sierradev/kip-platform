package com.integration.execution.mapper;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Map;

/**
 * MapStruct mapper for building {@link ConfluenceExecutionResult} from the execution command
 * and the orchestrator result. Multi-source + runtime values (Instant.now) are handled via
 * a default method, following the MapStruct idiom for complex aggregation mappings.
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
                .addedRecords(0)
                .updatedRecords(0)
                .failedRecords(0)
                .executionMetadata(buildExecutionMetadata(orchResult))
                .errorMessage(orchResult.errorMessage())
                .build();
    }

    default Map<String, String> buildExecutionMetadata(final ConfluenceJobExecutionResult orchResult) {
        if (orchResult.confluencePageUrl() == null) {
            return Map.of();
        }
        String pageId = orchResult.confluencePageId() != null ? orchResult.confluencePageId() : "";
        return Map.of("confluencePageUrl", orchResult.confluencePageUrl(), "confluencePageId", pageId);
    }
}
