package com.integration.management.controller.advice;

import com.integration.management.exception.IntegrationBaseException;
import com.integration.management.exception.IntegrationConnectionDeleteConflictException;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.EndpointValidationException;
import com.integration.management.exception.IntegrationApiException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(1)
public class SpecificExceptionHandler {

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

    @ExceptionHandler(IntegrationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleIntegrationNotFoundException(
            IntegrationNotFoundException ex, WebRequest request) {
        log.error("Integration not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getContextInfo(), "INTEGRATION_NOT_FOUND", request, null);
    }

    @ExceptionHandler(EndpointValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleSourceEndpointValidationException(
            EndpointValidationException ex, WebRequest request) {
        log.error("Source endpoint validation failed: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_FAILED", request, null);
    }

    @ExceptionHandler(IntegrationNameAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleIntegrationNameAlreadyExistsException(
            IntegrationNameAlreadyExistsException ex, WebRequest request) {
        log.error("Integration name already exists: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getContextInfo(), "INTEGRATION_NAME_ALREADY_EXISTS", request, null);
    }

    @ExceptionHandler(IntegrationConnectionDeleteConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleIntegrationConnectionDeleteConflictException(
            IntegrationConnectionDeleteConflictException ex, WebRequest request) {
        log.error("Integration connection delete conflict: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getContextInfo(), "INTEGRATION_CONNECTION_DELETE_CONFLICT", request,
                ex.getDetails());
    }

    @ExceptionHandler(IntegrationBaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleIntegrationBaseException(
            IntegrationBaseException ex, WebRequest request) {
        log.error("Integration base exception: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getContextInfo(), "INTEGRATION_BASE_EXCEPTION", request,
                null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            Exception ex, WebRequest request) {
        BindingResult bindingResult = ex instanceof MethodArgumentNotValidException obj
                ? obj.getBindingResult()
                : ((BindException) ex).getBindingResult();

        Map<String, String> errors = extractFieldErrors(bindingResult);
        log.error("Validation failed for [{}]: {}", getRequestPath(request), errors);
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_FAILED", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        log.error("Constraint violations: {}", errors);
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "CONSTRAINT_VIOLATION", request, errors);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex, WebRequest request) {
        log.error("Missing request header: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "MISSING_REQUEST_HEADER", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        log.error("Missing request parameter: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "MISSING_REQUEST_PARAMETER", request, null);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingPathVariableException(
            MissingPathVariableException ex, WebRequest request) {
        log.error("Missing path variable: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "MISSING_PATH_VARIABLE", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.error("Method argument type mismatch: {}", ex.getMessage());
        String errorMessage = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() == null ? "Unknown" : ex.getRequiredType().getSimpleName());
        return buildError(HttpStatus.BAD_REQUEST, errorMessage, "INVALID_PARAMETER", request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.error("HTTP message not readable: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_REQUEST_BODY", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.error("HTTP method not supported: {}", ex.getMessage());
        String errorMessage = String.format("Method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, errorMessage, "METHOD_NOT_ALLOWED", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {
        log.error("Media type not supported: {}", ex.getMessage());
        String errorMessage = String.format("Media type '%s' is not supported. Supported media types: %s",
                ex.getContentType(), ex.getSupportedMediaTypes());
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, errorMessage, "UNSUPPORTED_MEDIA_TYPE", request, null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        log.error("No handler found: {}", ex.getMessage());
        String errorMessage = String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL());
        return buildError(HttpStatus.NOT_FOUND, errorMessage, "NOT_FOUND", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "FORBIDDEN", request, null);
    }

    @ExceptionHandler(IntegrationApiException.class)
    public ResponseEntity<ErrorResponse> handleArcGisApiException(
            IntegrationApiException ex, WebRequest request) {
        log.error("ArcGIS API exception: {}", ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return buildError(status, ex.getMessage(), "INTEGRATION_API_EXCEPTION", request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidEnum(IllegalArgumentException ex, WebRequest request) {
        log.error("Invalid request: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "BAD_REQUEST",
                request, Map.of("error", "BAD_REQUEST"));
    }

    private Map<String, String> extractFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing));
    }

    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

}
