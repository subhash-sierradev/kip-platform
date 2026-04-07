package com.integration.management.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntegrationApiExceptionTest {

    @Test
    void constructor_message_setsDefaultStatusCode() {
        IntegrationApiException ex = new IntegrationApiException("default error");

        assertThat(ex.getMessage()).isEqualTo("default error");
        assertThat(ex.getStatusCode()).isEqualTo(500);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructor_messageAndCause_setsDefaultStatusCodeAndCause() {
        Throwable cause = new IllegalStateException("root cause");

        IntegrationApiException ex = new IntegrationApiException("wrapped error", cause);

        assertThat(ex.getMessage()).isEqualTo("wrapped error");
        assertThat(ex.getStatusCode()).isEqualTo(500);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_messageAndStatusCode_setsProvidedStatusCode() {
        IntegrationApiException ex = new IntegrationApiException("custom status", 404);

        assertThat(ex.getMessage()).isEqualTo("custom status");
        assertThat(ex.getStatusCode()).isEqualTo(404);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructor_messageStatusCodeAndCause_setsAllFields() {
        Throwable cause = new RuntimeException("api failure");

        IntegrationApiException ex = new IntegrationApiException("failed", 422, cause);

        assertThat(ex.getMessage()).isEqualTo("failed");
        assertThat(ex.getStatusCode()).isEqualTo(422);
        assertThat(ex.getCause()).isSameAs(cause);
    }
}

