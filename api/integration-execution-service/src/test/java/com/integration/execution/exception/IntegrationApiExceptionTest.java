package com.integration.execution.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationApiExceptionTest {

    @Test
    void constructors_setStatusAndCauseAsExpected() {
        RuntimeException cause = new RuntimeException("down");

        IntegrationApiException defaultException = new IntegrationApiException("m1");
        IntegrationApiException defaultWithCause = new IntegrationApiException("m2", cause);
        IntegrationApiException customStatus = new IntegrationApiException("m3", 400);
        IntegrationApiException customWithCause = new IntegrationApiException("m4", 404, cause);

        assertThat(defaultException.getStatusCode()).isEqualTo(500);
        assertThat(defaultWithCause.getStatusCode()).isEqualTo(500);
        assertThat(customStatus.getStatusCode()).isEqualTo(400);
        assertThat(customWithCause.getStatusCode()).isEqualTo(404);
        assertThat(customWithCause.getCause()).isEqualTo(cause);
    }
}
