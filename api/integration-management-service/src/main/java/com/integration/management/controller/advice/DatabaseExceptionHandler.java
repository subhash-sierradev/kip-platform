package com.integration.management.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
@Order(2)
public class DatabaseExceptionHandler {

    public record ErrorResponse(LocalDateTime timestamp,
                                int status,
                                String error,
                                String message,
                                String errorCode,
                                String path,
                                Object details) {
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status,
                                                     String message,
                                                     String errorCode,
                                                     WebRequest request,
                                                     Object details) {
        String path = getRequestPath(request);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        errorCode,
                        path,
                        details));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                      WebRequest request) {
        log.error("Data integrity violation occurred: {}", ex.getMessage(), ex);

        String userMessage = "Data integrity constraint violation occurred";
        String details = null;

        // Extract more specific information from the exception message
        String exceptionMessage = ex.getMessage();
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("foreign key constraint")) {
                userMessage = "Cannot perform operation due to related data constraints";
                details = "Referenced data exists that prevents this operation";
            } else if (exceptionMessage.contains("unique constraint") || exceptionMessage.contains("duplicate key")) {
                userMessage = "Duplicate entry - record with this information already exists";
                details = "A record with similar data already exists in the system";
            } else if (exceptionMessage.contains("not-null constraint")) {
                userMessage = "Required field is missing";
                details = "All mandatory fields must be provided";
            } else if (exceptionMessage.contains("check constraint")) {
                userMessage = "Data validation failed";
                details = "The provided data does not meet system requirements";
            }
        }

        return buildError(HttpStatus.CONFLICT, userMessage, "DATA_INTEGRITY_VIOLATION", request, details);
    }

    private String getRequestPath(WebRequest request) {
        return request.getDescription(false)
                .replace("uri=", "");
    }
}
