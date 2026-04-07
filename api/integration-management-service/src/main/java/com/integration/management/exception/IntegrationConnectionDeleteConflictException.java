package com.integration.management.exception;

import lombok.Getter;

@Getter
public class IntegrationConnectionDeleteConflictException extends IntegrationBaseException {

    private final Object details;

    public IntegrationConnectionDeleteConflictException(String message, Object details) {
        super(message);
        this.details = details;
    }
}
