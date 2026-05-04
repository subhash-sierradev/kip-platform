package com.integration.translation.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Inbound request body for the {@code POST /api/translate} endpoint.
 *
 * <p>Example JSON:</p>
 * <pre>{@code
 * {
 *   "textToTranslate": "Hello world. Please translate this document.",
 *   "sourceLanguage": "en",
 *   "languageCodes": ["ja", "ru"]
 * }
 * }</pre>
 *
 * <h3>Field constraints</h3>
 * <ul>
 *   <li>{@code textToTranslate} — must not be blank, max 50 000 characters (Ollama limit).</li>
 *   <li>{@code sourceLanguage} — BCP-47 language code for the input text (e.g. {@code en}).</li>
 *   <li>{@code languageCodes} — one or more BCP-47 target language codes
 *       (e.g. {@code ["ja", "ru"]}).</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequest {

    /**
     * The text content to be translated.
     * Accepts plain text, Confluence wiki markup, or any UTF-8 encoded string.
     */
    @NotBlank(message = "textToTranslate must not be blank")
    @Size(max = 50_000, message = "textToTranslate must not exceed 50 000 characters")
    private String textToTranslate;

    /**
     * BCP-47 language code of the source text (e.g. {@code "en"} for English).
     */
    @NotBlank(message = "sourceLanguage must not be blank")
    private String sourceLanguage;

    /**
     * One or more BCP-47 target language codes.
     * Each code results in a separate {@link com.integration.translation.model.response.TranslationResult}
     * in the response.
     */
    @NotEmpty(message = "languageCodes must contain at least one language code")
    private List<@NotBlank String> languageCodes;
}

