package com.integration.management.messaging;

import com.integration.execution.contract.message.ArcGISExecutionResult;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.management.exception.AzureKeyVaultException;
import com.integration.management.exception.EndpointValidationException;
import com.integration.management.exception.IntegrationApiException;
import com.integration.management.exception.IntegrationExecutionException;
import com.integration.management.service.IntegrationJobExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcGISExecutionResultListener")
class ArcGISExecutionResultListenerTest {

    @Mock
    private IntegrationJobExecutionService jobExecutionService;

    @InjectMocks
    private ArcGISExecutionResultListener listener;

    @Nested
    @DisplayName("onExecutionResult")
    class OnExecutionResult {

        @Test
        @DisplayName("should delegate to service completeJobExecution")
        void onExecutionResult_delegatesToService() {
            ArcGISExecutionResult result = ArcGISExecutionResult.builder()
                    .jobExecutionId(UUID.randomUUID())
                    .status(JobExecutionStatus.SUCCESS)
                    .build();

            listener.onExecutionResult(result);

            verify(jobExecutionService).completeJobExecution(result);
        }

        @Test
        @DisplayName("should rethrow exception from service to let Spring AMQP nack the message")
        void onExecutionResult_serviceThrows_rethrowsException() {
            ArcGISExecutionResult result = ArcGISExecutionResult.builder()
                    .jobExecutionId(UUID.randomUUID())
                    .status(JobExecutionStatus.FAILED)
                    .errorMessage("boom")
                    .build();

            doThrow(new RuntimeException("DB unavailable"))
                    .when(jobExecutionService)
                    .completeJobExecution(result);

            assertThatThrownBy(() -> listener.onExecutionResult(result))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB unavailable");

            verify(jobExecutionService).completeJobExecution(result);
        }

        @Test
        @DisplayName("should rethrow IntegrationExecutionException from service")
        void onExecutionResult_serviceThrowsIntegrationExecutionException_rethrows() {
            ArcGISExecutionResult result = ArcGISExecutionResult.builder()
                    .jobExecutionId(UUID.randomUUID())
                    .status(JobExecutionStatus.FAILED)
                    .errorMessage("boom")
                    .build();

            doThrow(new IntegrationExecutionException("execution failed"))
                    .when(jobExecutionService)
                    .completeJobExecution(result);

            assertThatThrownBy(() -> listener.onExecutionResult(result))
                    .isInstanceOf(IntegrationExecutionException.class)
                    .hasMessage("execution failed");

            verify(jobExecutionService).completeJobExecution(result);
        }

        @Test
        @DisplayName("should rethrow IntegrationApiException from service")
        void onExecutionResult_serviceThrowsIntegrationApiException_rethrows() {
            ArcGISExecutionResult result = ArcGISExecutionResult.builder()
                    .jobExecutionId(UUID.randomUUID())
                    .status(JobExecutionStatus.FAILED)
                    .errorMessage("boom")
                    .build();

            doThrow(new IntegrationApiException("api failed", 502))
                    .when(jobExecutionService)
                    .completeJobExecution(result);

            assertThatThrownBy(() -> listener.onExecutionResult(result))
                    .isInstanceOf(IntegrationApiException.class)
                    .hasMessage("api failed");

            verify(jobExecutionService).completeJobExecution(result);
        }

        @Test
        @DisplayName("should rethrow EndpointValidationException from service")
        void onExecutionResult_serviceThrowsEndpointValidationException_rethrows() {
            ArcGISExecutionResult result = ArcGISExecutionResult.builder()
                    .jobExecutionId(UUID.randomUUID())
                    .status(JobExecutionStatus.FAILED)
                    .errorMessage("boom")
                    .build();

            doThrow(new EndpointValidationException("invalid endpoint"))
                    .when(jobExecutionService)
                    .completeJobExecution(result);

            assertThatThrownBy(() -> listener.onExecutionResult(result))
                    .isInstanceOf(EndpointValidationException.class)
                    .hasMessage("invalid endpoint");

            verify(jobExecutionService).completeJobExecution(result);
        }

        @Test
        @DisplayName("should rethrow AzureKeyVaultException from service")
        void onExecutionResult_serviceThrowsAzureKeyVaultException_rethrows() {
            ArcGISExecutionResult result = ArcGISExecutionResult.builder()
                    .jobExecutionId(UUID.randomUUID())
                    .status(JobExecutionStatus.FAILED)
                    .errorMessage("boom")
                    .build();

            doThrow(AzureKeyVaultException.credentialNotFound("secret"))
                    .when(jobExecutionService)
                    .completeJobExecution(result);

            assertThatThrownBy(() -> listener.onExecutionResult(result))
                    .isInstanceOf(AzureKeyVaultException.class)
                    .hasMessageContaining("Credential not found");

            verify(jobExecutionService).completeJobExecution(result);
        }
    }
}
