package com.integration.management.messaging;

import com.integration.execution.contract.message.ConfluenceExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.management.service.IntegrationJobExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceExecutionResultListener")
class ConfluenceExecutionResultListenerTest {

    @Mock
    private IntegrationJobExecutionService jobExecutionService;

    private ConfluenceExecutionResultListener listener;

    @BeforeEach
    void setUp() {
        listener = new ConfluenceExecutionResultListener(jobExecutionService);
    }

    @Test
    @DisplayName("should persist execution result on success")
    void onExecutionResult_success_persistsResult() {
        ConfluenceExecutionResult result = buildResult(JobExecutionStatus.SUCCESS);

        listener.onExecutionResult(result);

        verify(jobExecutionService).completeConfluenceJobExecution(result);
    }

    @Test
    @DisplayName("should persist failure result")
    void onExecutionResult_failure_persistsResult() {
        ConfluenceExecutionResult result = buildResult(JobExecutionStatus.FAILED);

        listener.onExecutionResult(result);

        verify(jobExecutionService).completeConfluenceJobExecution(result);
    }

    @Test
    @DisplayName("should rethrow exception when persistence fails")
    void onExecutionResult_persistenceThrows_rethrows() {
        ConfluenceExecutionResult result = buildResult(JobExecutionStatus.FAILED);
        doThrow(new RuntimeException("persistence failure"))
                .when(jobExecutionService).completeConfluenceJobExecution(result);

        assertThatThrownBy(() -> listener.onExecutionResult(result))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("persistence failure");

        verify(jobExecutionService).completeConfluenceJobExecution(result);
    }

    private ConfluenceExecutionResult buildResult(JobExecutionStatus status) {
        return ConfluenceExecutionResult.builder()
                .jobExecutionId(UUID.randomUUID())
                .status(status)
                .errorMessage(status == JobExecutionStatus.FAILED ? "some error" : null)
                .build();
    }
}


