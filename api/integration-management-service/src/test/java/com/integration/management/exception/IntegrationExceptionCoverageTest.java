package com.integration.management.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration Exceptions")
class IntegrationExceptionCoverageTest {

    @Test
    @DisplayName("IntegrationExecutionException exposes type and context info")
    void integrationExecutionExceptionExposesTypeAndContextInfo() {
        IntegrationExecutionException exception = new IntegrationExecutionException("execution failed");

        assertThat(exception.getExceptionType()).isEqualTo("IntegrationExecutionException");
        assertThat(exception.getContextInfo()).isEqualTo("execution failed");
        assertThat(exception).isInstanceOf(IntegrationBaseException.class);
    }

    @Test
    @DisplayName("IntegrationExecutionException keeps message and cause")
    void integrationExecutionExceptionKeepsMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        IntegrationExecutionException exception = new IntegrationExecutionException("failed", cause);

        assertThat(exception.getMessage()).isEqualTo("failed");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("IntegrationBaseException constructors are covered")
    void integrationBaseExceptionConstructorsAreCovered() {
        class TestBaseException extends IntegrationBaseException {
            TestBaseException(String message) {
                super(message);
            }

            TestBaseException(Throwable cause) {
                super(cause);
            }

            TestBaseException() {
                super();
            }
        }

        RuntimeException cause = new RuntimeException("cause-message");

        IntegrationBaseException withMessage = new TestBaseException("base-message");
        IntegrationBaseException withCause = new TestBaseException(cause);
        IntegrationBaseException empty = new TestBaseException();

        assertThat(withMessage.getMessage()).isEqualTo("base-message");
        assertThat(withMessage.getContextInfo()).isEqualTo("base-message");

        assertThat(withCause.getCause()).isEqualTo(cause);
        assertThat(withCause.getMessage()).contains("cause-message");

        assertThat(empty.getMessage()).isNull();
        assertThat(empty.getContextInfo()).isNull();
        assertThat(empty.getExceptionType()).isEqualTo("TestBaseException");
    }
}
