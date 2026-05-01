package com.integration.execution.mapper;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConfluenceExecutionMapperTest {

    /**
     * MapStruct generates the implementation at compile time from the interface default methods.
     * We test the default methods directly through a simple anonymous implementation.
     */
    private ConfluenceExecutionMapper mapper;

    @BeforeEach
    void setUp() {
        // Anonymous implementation: default methods live in the interface itself, so we can test them
        // without the Spring-generated implementation class.
        mapper = new ConfluenceExecutionMapper() {
        };
    }

    @Test
    void toExecutionResult_successfulOrchestration_returnsSuccessResult() {
        UUID jobId = UUID.randomUUID();
        ConfluenceExecutionCommand command = buildCommand(jobId);
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.success(
                10, "https://example.com/page/99", "99");
        Instant startedAt = Instant.now().minusSeconds(5);

        ConfluenceExecutionResult result = mapper.toExecutionResult(command, orchResult, startedAt);

        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isEqualTo(10);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getStartedAt()).isEqualTo(startedAt);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getExecutionMetadata()).containsEntry("confluencePageUrl", "https://example.com/page/99");
        assertThat(result.getExecutionMetadata()).containsEntry("confluencePageId", "99");
    }

    @Test
    void toExecutionResult_failedOrchestration_returnsFailedResult() {
        UUID jobId = UUID.randomUUID();
        ConfluenceExecutionCommand command = buildCommand(jobId);
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.failed("Connection refused");
        Instant startedAt = Instant.now().minusSeconds(2);

        ConfluenceExecutionResult result = mapper.toExecutionResult(command, orchResult, startedAt);

        assertThat(result.getJobExecutionId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Connection refused");
        assertThat(result.getTotalRecords()).isZero();
        assertThat(result.getExecutionMetadata()).isEmpty();
    }

    @Test
    void buildExecutionMetadata_withPageUrl_containsBothUrlAndId() {
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.success(
                3, "https://example.com/wiki/spaces/DEV/pages/42", "42");

        Map<String, String> metadata = mapper.buildExecutionMetadata(orchResult);

        assertThat(metadata).containsEntry("confluencePageUrl", "https://example.com/wiki/spaces/DEV/pages/42");
        assertThat(metadata).containsEntry("confluencePageId", "42");
    }

    @Test
    void buildExecutionMetadata_withNullPageUrl_returnsEmptyMap() {
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.failed("error");

        Map<String, String> metadata = mapper.buildExecutionMetadata(orchResult);

        assertThat(metadata).isEmpty();
    }

    @Test
    void buildExecutionMetadata_withNullPageId_usesEmptyStringForId() {
        ConfluenceJobExecutionResult orchResult = ConfluenceJobExecutionResult.success(
                5, "https://example.com/page/33", null);

        Map<String, String> metadata = mapper.buildExecutionMetadata(orchResult);

        assertThat(metadata).containsEntry("confluencePageId", "");
        assertThat(metadata).containsEntry("confluencePageUrl", "https://example.com/page/33");
    }

    private ConfluenceExecutionCommand buildCommand(UUID jobId) {
        return ConfluenceExecutionCommand.builder()
                .jobExecutionId(jobId)
                .integrationId(UUID.randomUUID())
                .integrationName("Test Integration")
                .tenantId("tenant-1")
                .build();
    }
}
