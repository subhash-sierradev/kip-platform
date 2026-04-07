package com.integration.management.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
@Order
public class GenericExceptionHandler {

    private static final String GENERIC_INTERNAL_ERROR_MESSAGE =
            "An unexpected error occurred. Please try again later.";

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

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
        log.debug("SSE client disconnected: {}", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                GENERIC_INTERNAL_ERROR_MESSAGE,
                "INTERNAL_SERVER_ERROR",
                request,
                null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        log.error("ResponseStatusException occurred: {}", message, ex);
        return buildError(status, message, "RESPONSE_STATUS_EXCEPTION", request, null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                GENERIC_INTERNAL_ERROR_MESSAGE,
                "INTERNAL_SERVER_ERROR",
                request,
                null);
    }

    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
