package com.integration.translation.exception;

/**
 * Domain exception thrown when the Translation Service cannot complete a translation.
 *
 * <p>This exception is typically thrown by the service layer when the underlying
 * provider returns an error that is not recoverable from (e.g. the request is
 * structurally invalid).  Transient failures — such as Ollama being temporarily
 * unavailable — trigger the fallback path in
 * {@link com.integration.translation.service.OllamaTranslationService} instead
 * and do not surface as this exception.</p>
 *
 * <p>Instances of this class are handled by
 * {@link GlobalExceptionHandler} and mapped to HTTP 422 Unprocessable Entity.</p>
 */
public class TranslationException extends RuntimeException {

    /**
     * Constructs a {@code TranslationException} with the given detail message.
     *
     * @param message human-readable error description surfaced in the API response
     */
    public TranslationException(final String message) {
        super(message);
    }

    /**
     * Constructs a {@code TranslationException} with a detail message and the
     * root cause.
     *
     * @param message human-readable error description
     * @param cause   the underlying exception that triggered this failure
     */
    public TranslationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

