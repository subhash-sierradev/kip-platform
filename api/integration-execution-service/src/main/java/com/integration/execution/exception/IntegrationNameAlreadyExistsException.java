package com.integration.execution.exception;

public class IntegrationNameAlreadyExistsException extends IntegrationBaseException {
    public IntegrationNameAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntegrationNameAlreadyExistsException(String message) {
        super(message);
    }
}
