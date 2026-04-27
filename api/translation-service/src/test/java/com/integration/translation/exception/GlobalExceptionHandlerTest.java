package com.integration.translation.exception;

import com.integration.translation.controller.TranslationController;
import com.integration.translation.service.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Validation tests ({@link org.springframework.web.bind.MethodArgumentNotValidException})
 * use Spring MVC's {@code standaloneSetup} MockMvc to produce a <em>real</em>
 * exception through the bean-validation machinery.  This is necessary in
 * Spring Framework 7 because {@link org.springframework.validation.FieldError#getField()}
 * is now {@code final} and cannot be stubbed with Mockito — mocking it would
 * silently return {@code null} and make all field-name assertions false.</p>
 *
 * <p>Domain- and server-error handlers ({@link TranslationException}, generic
 * {@link Exception}) are still called directly because they do not depend on
 * Spring-internal types.</p>
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        // standaloneSetup wires our controller + ControllerAdvice with a real
        // DispatcherServlet and Hibernate Validator — no Spring context needed.
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TranslationController(mock(TranslationService.class)))
                .setControllerAdvice(handler)
                .build();
    }

    // ── Validation (400) — tested via real Spring MVC binding ─────────────────

    @Test
    @DisplayName("POST with blank textToTranslate returns 400 containing the field name")
    void handleValidationException_blankText_returns400WithFieldName() throws Exception {
        String body = "{\"textToTranslate\":\"\","
                + "\"sourceLanguage\":\"en\","
                + "\"languageCodes\":[\"ja\"]}";

        mockMvc.perform(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Request Validation Failed"))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST with missing sourceLanguage returns 400 containing the field name")
    void handleValidationException_missingSourceLang_returns400WithFieldName() throws Exception {
        String body = "{\"textToTranslate\":\"Hello\","
                + "\"sourceLanguage\":\"\","
                + "\"languageCodes\":[\"ja\"]}";

        mockMvc.perform(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Request Validation Failed"));
    }

    @Test
    @DisplayName("POST with empty languageCodes array returns 400")
    void handleValidationException_emptyLanguageCodes_returns400() throws Exception {
        String body = "{\"textToTranslate\":\"Hello\","
                + "\"sourceLanguage\":\"en\","
                + "\"languageCodes\":[]}";

        mockMvc.perform(post("/api/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Request Validation Failed"));
    }

    // ── Translation failure (422) ─────────────────────────────────────────────

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

    // ── Unexpected error (500) ────────────────────────────────────────────────

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
