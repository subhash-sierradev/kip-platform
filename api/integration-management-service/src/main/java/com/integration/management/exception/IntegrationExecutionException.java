package com.integration.management.exception;

public class IntegrationExecutionException extends IntegrationBaseException {

    public IntegrationExecutionException(String message) {
        super(message);
    }

    public IntegrationExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
