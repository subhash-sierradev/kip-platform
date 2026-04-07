package com.integration.management.exception;

/**
 * Exception thrown when an invalid language code is provided.
 */
public class InvalidLanguageCodeException extends IntegrationBaseException {

    public InvalidLanguageCodeException(String message) {
        super(message);
    }
}
