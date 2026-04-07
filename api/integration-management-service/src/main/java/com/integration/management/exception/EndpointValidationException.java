package com.integration.management.exception;

public class EndpointValidationException extends RuntimeException {
    public EndpointValidationException(String message) {
        super(message);
    }

    public EndpointValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

