package com.integration.management.controller.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Exception handler for Feign client errors when communicating with the
 * execution service.
 * Extracts and returns error messages from the execution service response.
 */
@Slf4j
@RestControllerAdvice
@Order(3)
public class FeignClientExceptionHandler {

    private static final String GENERIC_UNAVAILABLE_MESSAGE =
            "External service is temporarily unavailable. "
                    + "Please try again later.";
    private static final String ERROR_CODE_EXTERNAL_SERVICE_UNAVAILABLE = "EXTERNAL_SERVICE_UNAVAILABLE";
    private static final String ERROR_CODE_DOWNSTREAM_SERVICE_ERROR = "DOWNSTREAM_SERVICE_ERROR";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ErrorResponse(LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String errorCode,
            String path,
            Object details) {
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            FeignException ex, WebRequest request) {
        int statusCode = ex.status();
        if (isNetworkFailure(statusCode) || isServerError(statusCode)) {
            log.error("Execution service unavailable (status {}): {}", statusCode, ex.getMessage(), ex);
            return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                    GENERIC_UNAVAILABLE_MESSAGE,
                    ERROR_CODE_EXTERNAL_SERVICE_UNAVAILABLE,
                    request);
        }

        String message = sanitizeMessage(extractMessageFromResponse(ex));
        HttpStatus status = resolveHttpStatus(statusCode);

        log.error("Execution service error [{}]: {}", statusCode, message, ex);

        return buildErrorResponse(status, message, ERROR_CODE_DOWNSTREAM_SERVICE_ERROR, request);
    }

    private boolean isNetworkFailure(int statusCode) {
        // Feign uses -1 for network / IO exceptions (connection refused, timeouts, DNS,
        // etc)
        return statusCode == -1;
    }

    private boolean isServerError(int statusCode) {
        return statusCode >= 500;
    }

    private String extractMessageFromResponse(FeignException ex) {
        try {
            String responseBody = ex.contentUTF8();
            if (responseBody != null && !responseBody.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsedBody = objectMapper.readValue(responseBody, Map.class);
                if (parsedBody.containsKey("message") && parsedBody.get("message") != null) {
                    return parsedBody.get("message").toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse execution service error response: {}", e.getMessage());
        }
        return String.format("Execution service error (status %d)", ex.status());
    }

    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        return URL_PATTERN.matcher(message).replaceAll("[redacted]");
    }

    private HttpStatus resolveHttpStatus(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status : HttpStatus.BAD_GATEWAY;
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status,
            String message,
            String errorCode,
            WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                errorCode,
                path,
                null);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
