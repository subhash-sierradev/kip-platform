package com.integration.translation.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single translated text result for one target language.
 *
 * <p>Corresponds to one entry in the {@code translationResults} array of
 * {@link TranslationResponse}.</p>
 *
 * <p>Example JSON:</p>
 * <pre>{@code
 * {
 *   "translatedTimestamp": 1776694594,
 *   "languageCode": "ja",
 *   "value": "こんにちは、世界。この文書を翻訳してください。"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResult {

    /**
     * Unix epoch timestamp (seconds) recorded at the moment the translation
     * was produced.  Allows callers to track result freshness.
     */
    @JsonProperty("translatedTimestamp")
    private long translatedTimestamp;

    /**
     * BCP-47 language code of the translated text (e.g. {@code "ja"}, {@code "ru"}).
     */
    @JsonProperty("languageCode")
    private String languageCode;

    /**
     * The translated text in the target language.
     * This is the verbatim output from Ollama, trimmed of leading/trailing
     * whitespace.  Falls back to the original source text if translation fails.
     */
    @JsonProperty("value")
    private String value;
}

