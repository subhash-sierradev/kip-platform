package com.integration.management.exception;

public class IntegrationPersistenceException extends IntegrationBaseException {

    public IntegrationPersistenceException(String message) {
        super(message);
    }

    public IntegrationPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
