package com.integration.execution.exception;

import lombok.Getter;

@Getter
public class IntegrationApiException extends RuntimeException {
    private final int statusCode;

    public IntegrationApiException(String message) {
        super(message);
        this.statusCode = 500; // Default status code
    }

    public IntegrationApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500; // Default status code
    }

    public IntegrationApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public IntegrationApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

}
