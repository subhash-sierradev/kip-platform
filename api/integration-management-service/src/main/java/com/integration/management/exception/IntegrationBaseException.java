package com.integration.management.exception;

/**
 * Base exception for all integration-related errors.
 * <p>
 * Defaults to HTTP 500 unless overridden in {@code GlobalExceptionHandler}.
 */
public abstract class IntegrationBaseException extends RuntimeException {

    public IntegrationBaseException(String message) {
        super(message);
    }

    public IntegrationBaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntegrationBaseException(Throwable cause) {
        super(cause);
    }

    public IntegrationBaseException() {
        super();
    }

    public String getExceptionType() {
        return this.getClass().getSimpleName();
    }

    public String getContextInfo() {
        return getMessage();
    }
}
