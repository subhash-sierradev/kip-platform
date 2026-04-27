package com.integration.translation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP-response mapping for the Translation Service.
 *
 * <p>Uses the RFC 7807 {@link ProblemDetail} format (natively supported by
 * Spring Boot 6+) so clients receive a structured, machine-readable error body
 * with a stable {@code type} URI, a human-readable {@code detail}, and the
 * current server timestamp.</p>
 *
 * <h3>Handled exceptions</h3>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request
 *       (Bean Validation failures on the request body)</li>
 *   <li>{@link TranslationException} → 422 Unprocessable Entity
 *       (non-recoverable translation error)</li>
 *   <li>{@link Exception} → 500 Internal Server Error
 *       (unexpected runtime failures)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Problem type URN prefix. */
    private static final String PROBLEM_TYPE_BASE = "https://kip-platform/translation/errors/";

    /**
     * Handles Bean Validation failures on the request body, collecting all field
     * error messages into a single human-readable detail string.
     *
     * @param ex the validation exception thrown by Spring MVC
     * @return 400 {@link ProblemDetail} with individual field error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(final MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", details);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, details);
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "validation-error"));
        problem.setTitle("Request Validation Failed");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    /**
     * Handles domain-level translation failures that cannot be recovered from.
     *
     * @param ex the domain exception
     * @return 422 {@link ProblemDetail}
     */
    @ExceptionHandler(TranslationException.class)
    public ProblemDetail handleTranslationException(final TranslationException ex) {
        log.error("Translation error: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "translation-error"));
        problem.setTitle("Translation Failed");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    /**
     * Catch-all handler for unexpected runtime errors.
     *
     * @param ex the unexpected exception
     * @return 500 {@link ProblemDetail}
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(final Exception ex) {
        log.error("Unexpected error in Translation Service: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please check service logs.");
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}

