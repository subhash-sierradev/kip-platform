package com.integration.execution.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.config.properties.TranslationApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * HTTP client for the KIP-544 Translation Service.
 *
 * <p>Calls {@code POST {baseUrl}/api/translate} with the rendered page content and
 * returns the translated text for the requested target language.  The client is
 * intentionally <em>not</em> retryable — callers should treat any failure as a
 * soft error and fall back to the original content (see
 * {@link com.integration.execution.service.processor.ConfluenceTranslationStep}).</p>
 *
 * <h3>Contract (mirrors KIP-544 TranslationRequest / TranslationResponse)</h3>
 * <pre>
 * POST /api/translate
 * {
 *   "textToTranslate": "...",
 *   "sourceLanguage":  "en",
 *   "languageCodes":   ["ja"]
 * }
 * → 200
 * {
 *   "translationResults": [
 *     { "translatedTimestamp": 1776694594, "languageCode": "ja", "value": "..." }
 *   ]
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationApiClient {

    private static final String TRANSLATE_PATH = "/api/translate";

    private final TranslationApiProperties properties;
    private final ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Translates {@code text} from {@code sourceLanguage} to {@code targetLanguage}.
     *
     * @return the translated text, or {@link Optional#empty()} if the call fails or
     *         returns an empty result list.
     */
    public Optional<String> translate(final String text,
                                      final String sourceLanguage,
                                      final String targetLanguage) {
        try {
            TranslationRequest requestBody = new TranslationRequest(text, sourceLanguage,
                    List.of(targetLanguage));
            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + TRANSLATE_PATH))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                log.warn("Translation API returned HTTP {} for target language '{}': {}",
                        statusCode, targetLanguage, response.body());
                return Optional.empty();
            }

            TranslationResponse translationResponse =
                    objectMapper.readValue(response.body(), TranslationResponse.class);

            if (translationResponse.translationResults() == null
                    || translationResponse.translationResults().isEmpty()) {
                log.warn("Translation API returned empty translationResults for target language '{}'",
                        targetLanguage);
                return Optional.empty();
            }

            String translated = translationResponse.translationResults().get(0).value();
            if (translated == null || translated.isBlank()) {
                log.warn("Translation API returned blank value for target language '{}'", targetLanguage);
                return Optional.empty();
            }

            log.debug("Translation API successfully translated {} chars to '{}'",
                    text.length(), targetLanguage);
            return Optional.of(translated);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Translation API call failed for target language '{}': {}",
                    targetLanguage, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unexpected error calling Translation API for target language '{}': {}",
                    targetLanguage, e.getMessage(), e);
            return Optional.empty();
        }
    }

    // -----------------------------------------------------------------------
    // Inner DTOs (mirror KIP-544 contract — no compile-time dependency needed)
    // -----------------------------------------------------------------------

    /**
     * Request body for {@code POST /api/translate}.
     */
    public record TranslationRequest(
            @JsonProperty("textToTranslate") String textToTranslate,
            @JsonProperty("sourceLanguage") String sourceLanguage,
            @JsonProperty("languageCodes") List<String> languageCodes) {
    }

    /**
     * Top-level response from {@code POST /api/translate}.
     * Only {@code translationResults} is used; other fields are ignored.
     */
    public record TranslationResponse(
            @JsonProperty("translationResults") List<TranslationResult> translationResults) {
    }

    /**
     * A single translated result for one target language.
     */
    public record TranslationResult(
            @JsonProperty("translatedTimestamp") long translatedTimestamp,
            @JsonProperty("languageCode") String languageCode,
            @JsonProperty("value") String value) {
    }
}

