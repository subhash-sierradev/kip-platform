package com.integration.execution.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionCoverageTest {

    @Test
    void baseException_exposesTypeAndContext() {
        IntegrationExecutionException exception = new IntegrationExecutionException("boom");

        assertThat(exception.getExceptionType()).isEqualTo("IntegrationExecutionException");
        assertThat(exception.getContextInfo()).isEqualTo("boom");
    }

    @Test
    void baseException_additionalConstructors_areCovered() {
        class TestBaseException extends IntegrationBaseException {
            TestBaseException(Throwable cause) {
                super(cause);
            }

            TestBaseException() {
                super();
            }
        }

        RuntimeException cause = new RuntimeException("cause");

        IntegrationBaseException fromCause = new IntegrationExecutionException(cause.getMessage(), cause);
        IntegrationBaseException empty = new IntegrationExecutionException((String) null);
        IntegrationBaseException baseFromCause = new TestBaseException(cause);
        IntegrationBaseException baseEmpty = new TestBaseException();

        assertThat(fromCause.getCause()).isEqualTo(cause);
        assertThat(empty.getMessage()).isNull();
        assertThat(baseFromCause.getCause()).isEqualTo(cause);
        assertThat(baseEmpty.getMessage()).isNull();
    }

    @Test
    void endpointAndCacheExceptions_storeMessage() {
        EndpointValidationException endpoint = new EndpointValidationException("invalid endpoint");
        CacheNotFoundException cache = new CacheNotFoundException("cache missing");

        assertThat(endpoint.getMessage()).isEqualTo("invalid endpoint");
        assertThat(cache.getMessage()).isEqualTo("cache missing");
    }

    @Test
    void integrationExceptions_keepCauseAndMessage() {
        RuntimeException cause = new RuntimeException("cause");

        IntegrationPersistenceException persistence = new IntegrationPersistenceException("persist", cause);
        SchedulingException scheduling = new SchedulingException("schedule", cause);
        IntegrationNameAlreadyExistsException duplicate = new IntegrationNameAlreadyExistsException("dup", cause);

        assertThat(persistence.getCause()).isEqualTo(cause);
        assertThat(scheduling.getCause()).isEqualTo(cause);
        assertThat(duplicate.getCause()).isEqualTo(cause);
    }

    @Test
    void azureKeyVaultException_factoriesBuildExpectedMessages() {
        RuntimeException cause = new RuntimeException("cause");

        assertThat(AzureKeyVaultException.credentialNotFound("s1").getMessage())
                .isEqualTo("Credential not found for key: s1");
        assertThat(AzureKeyVaultException.storeOperationFailed("s1", cause).getMessage())
                .isEqualTo("Failed to store credential with key: s1");
        assertThat(AzureKeyVaultException.retrieveOperationFailed("s1", cause).getMessage())
                .isEqualTo("Failed to retrieve credential with key: s1");
        assertThat(AzureKeyVaultException.deleteOperationFailed("s1", cause).getMessage())
                .isEqualTo("Failed to delete credential with key: s1");
        assertThat(AzureKeyVaultException.serializationFailed(cause).getMessage())
                .isEqualTo("Failed to serialize credential data");
        assertThat(AzureKeyVaultException.deserializationFailed("s1", cause).getMessage())
                .isEqualTo("Failed to deserialize credential data for key: s1");
        assertThat(AzureKeyVaultException.listOperationFailed(cause).getMessage())
                .isEqualTo("Failed to list credentials in Azure Key Vault");
    }
}
