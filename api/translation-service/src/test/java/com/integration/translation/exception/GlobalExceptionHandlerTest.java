package com.integration.translation.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleValidationException returns 400 with field error details")
    void handleValidationException_returns400WithFieldErrors() {
        FieldError fieldError = new FieldError(
                "translationRequest", "textToTranslate", "must not be blank");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problem = handler.handleValidationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Request Validation Failed");
        assertThat(problem.getDetail()).contains("textToTranslate");
        assertThat(problem.getDetail()).contains("must not be blank");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleValidationException concatenates multiple field errors")
    void handleValidationException_multipleErrors_concatenatesThem() {
        FieldError error1 = new FieldError("req", "textToTranslate", "must not be blank");
        FieldError error2 = new FieldError("req", "sourceLanguage", "must not be blank");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problem = handler.handleValidationException(ex);

        assertThat(problem.getDetail()).contains("textToTranslate");
        assertThat(problem.getDetail()).contains("sourceLanguage");
    }

    @Test
    @DisplayName("handleTranslationException returns 422 with exception message")
    void handleTranslationException_returns422() {
        TranslationException ex = new TranslationException("Translation provider not available");

        ProblemDetail problem = handler.handleTranslationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Translation Failed");
        assertThat(problem.getDetail()).isEqualTo("Translation provider not available");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleTranslationException with cause still returns 422")
    void handleTranslationException_withCause_returns422() {
        TranslationException ex = new TranslationException(
                "upstream failure", new RuntimeException("root cause"));

        ProblemDetail problem = handler.handleTranslationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getDetail()).isEqualTo("upstream failure");
    }

    @Test
    @DisplayName("handleGenericException returns 500 with generic message")
    void handleGenericException_returns500() {
        Exception ex = new Exception("Something unexpected happened");

        ProblemDetail problem = handler.handleGenericException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).contains("unexpected error");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleGenericException type URI ends with internal-error")
    void handleGenericException_typeUriEndsWithInternalError() {
        ProblemDetail problem = handler.handleGenericException(new Exception("x"));

        assertThat(problem.getType().toString()).endsWith("internal-error");
    }
}

