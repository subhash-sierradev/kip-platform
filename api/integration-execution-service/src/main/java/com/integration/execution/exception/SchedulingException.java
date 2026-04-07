package com.integration.execution.exception;

/**
 * Exception thrown when there are issues with job scheduling operations.
 * Used for Quartz scheduler-related errors and validation failures.
 */
public class SchedulingException extends RuntimeException {
    public SchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}
