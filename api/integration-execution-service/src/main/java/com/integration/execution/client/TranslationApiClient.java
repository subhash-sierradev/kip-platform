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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the KIP-544 Translation Service.
 *
 * <p>Calls {@code POST {baseUrl}/api/translate} with the rendered page content and
 * returns the translated text for the requested target language(s).  The client is
 * intentionally <em>not</em> retryable — callers should treat any failure as a
 * soft error and fall back to the original content (see
 * {@link com.integration.execution.service.processor.KwMonitoringDataTranslator}).</p>
 *
 * <h3>Contract (mirrors KIP-544 TranslationRequest / TranslationResponse)</h3>
 * <pre>
 * POST /api/translate
 * {
 *   "textToTranslate": "...",
 *   "sourceLanguage":  "en",
 *   "languageCodes":   ["ja", "fr"]
 * }
 * → 200
 * {
 *   "translationResults": [
 *     { "translatedTimestamp": 1776694594, "languageCode": "ja", "value": "..." },
 *     { "translatedTimestamp": 1776694595, "languageCode": "fr", "value": "..." }
 *   ]
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationApiClient {

    private static final String TRANSLATE_PATH = "/api/translate";

    /**
     * Lowercase line-start phrases that identify an English LLM artifact paragraph
     * (explanation / disclaimer appended after the real translation).
     */
    private static final List<String> ARTIFACT_LINE_PREFIXES = List.of(
            "here is", "please note", "note:", "translation:", "i am an ai", "(note:",
            "this translation", "this is a machine", "human review", "please be aware",
            "kindly note", "disclaimer:", "(translation", "(the translation",
            "the translation is", "machine translation", "i cannot", "i'm unable");

    /**
     * Inline markers after which everything is expected to be an English explanation.
     * Only stripped when the text that follows is ASCII-only (i.e. English).
     */
    private static final List<String> ARTIFACT_INLINE_MARKERS =
            List.of(" Here is", " Please note", " (Note:", " Note —", " Note:",
                    " (The translation", " (Translation", " (This translation",
                    " (This is a");

    private final TranslationApiProperties properties;
    private final ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Translates {@code text} from {@code sourceLanguage} to a single {@code targetLanguage}.
     * Convenience wrapper around {@link #translateMulti}.
     *
     * @return the translated text, or {@link Optional#empty()} if the call fails or
     *         returns an empty result list.
     */
    public Optional<String> translate(final String text,
                                      final String sourceLanguage,
                                      final String targetLanguage) {
        return translateMulti(text, sourceLanguage, List.of(targetLanguage))
                .getOrDefault(targetLanguage, Optional.empty());
    }

    /**
     * Translates {@code text} from {@code sourceLanguage} to <em>all</em>
     * {@code targetLanguages} in a single HTTP call.
     *
     * <p>The API already accepts an array of language codes, so this avoids
     * N × L round-trips (N strings × L languages) and reduces to N calls total.</p>
     *
     * @param text            the string to translate
     * @param sourceLanguage  BCP-47 source language code
     * @param targetLanguages list of BCP-47 target language codes
     * @return map of languageCode → translated text ({@link Optional#empty()} on per-language failure)
     */
    public Map<String, Optional<String>> translateMulti(final String text,
                                                        final String sourceLanguage,
                                                        final List<String> targetLanguages) {
        Map<String, Optional<String>> results = new LinkedHashMap<>();
        for (String lang : targetLanguages) {
            results.put(lang, Optional.empty());
        }

        try {
            TranslationResponse translationResponse = callApi(text, sourceLanguage, targetLanguages);
            if (translationResponse == null) {
                return results;
            }

            for (TranslationResult result : translationResponse.translationResults()) {
                if (result.languageCode() == null) {
                    continue;
                }
                if (result.value() == null || result.value().isBlank()) {
                    log.warn("Translation API returned blank value for language '{}'; keeping original.",
                            result.languageCode());
                } else if (isLikelyFallbackExplanation(text, result.value(), result.languageCode())) {
                    log.warn("Translation API returned a source-language explanation instead of a '{}' "
                            + "translation; keeping original.", result.languageCode());
                } else {
                    String cleaned = cleanTranslationResponse(result.value());
                    if (!cleaned.equals(result.value())) {
                        log.debug("Stripped English artifact from '{}' translation.", result.languageCode());
                    }
                    results.put(result.languageCode(), Optional.of(cleaned));
                }
            }

            log.debug("Translation API translated {} chars to {} language(s): {}",
                    text.length(), targetLanguages.size(), targetLanguages);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Translation API call failed for languages {}: {}", targetLanguages, e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error calling Translation API for languages {}: {}",
                    targetLanguages, e.getMessage(), e);
        }

        return results;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Executes the HTTP POST to {@code /api/translate} and parses the response.
     *
     * @return parsed {@link TranslationResponse}, or {@code null} on non-2xx / empty body
     */
    private TranslationResponse callApi(final String text,
                                        final String sourceLanguage,
                                        final List<String> targetLanguages)
            throws IOException, InterruptedException {

        TranslationRequest requestBody = new TranslationRequest(text, sourceLanguage, targetLanguages);
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
            log.warn("Translation API returned HTTP {} for languages {}: {}",
                    statusCode, targetLanguages, response.body());
            return null;
        }

        TranslationResponse parsed = objectMapper.readValue(response.body(), TranslationResponse.class);
        if (parsed.translationResults() == null || parsed.translationResults().isEmpty()) {
            log.warn("Translation API returned empty translationResults for languages '{}'", targetLanguages);
            return null;
        }

        return parsed;
    }

    // -----------------------------------------------------------------------
    // Response cleanup helpers
    // -----------------------------------------------------------------------

    /**
     * Cleans LLM verbosity artifacts from a translation response value.
     *
     * <p>This is a client-side defence layer that runs <em>regardless</em> of whether
     * the translation service already applied its own cleanup.  It is necessary because
     * the translation service may return a cached (pre-fix) dirty response.</p>
     *
     * <p>Three patterns are handled:</p>
     * <ol>
     *   <li><strong>Leading preamble</strong> — {@code "Here is your translation: 報告詳細"}
     *       → {@code "報告詳細"}.</li>
     *   <li><strong>Newline-separated content</strong> — everything after the first {@code \n}
     *       is unconditionally discarded.  All translatable values in this system are
     *       single-line phrases; any multi-line content is always an LLM
     *       disclaimer/explanation.</li>
     *   <li><strong>Inline English suffix</strong> — ASCII-only text that follows a
     *       known English marker on the same line is stripped.</li>
     * </ol>
     */
    String cleanTranslationResponse(final String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String t = value.trim();

        // 1. Strip leading artifact preamble ("Here is your translation: ...")
        t = stripLeadingArtifactPreamble(t);

        // 2. Unconditionally truncate at the first newline.
        //    All translatable values in this system are single-line phrases;
        //    any content after the first \n is always an LLM disclaimer/explanation.
        int nIdx = t.indexOf('\n');
        if (nIdx > 0) {
            String candidate = t.substring(0, nIdx).strip();
            if (!candidate.isBlank()) {
                t = candidate;
            }
        }

        // 3. Strip inline English suffix, e.g. "著者 Here is the translation of ..."
        //    Check only the first 40 characters of the suffix for ASCII so that
        //    "(The translation of 'X' in Japanese is '合計レポート'.)" is still stripped
        //    even though the full suffix contains non-ASCII at the end.
        for (String marker : ARTIFACT_INLINE_MARKERS) {
            int idx = t.indexOf(marker);
            if (idx > 0) {
                String suffix = t.substring(idx);
                String prefixToCheck = suffix.length() <= 40 ? suffix : suffix.substring(0, 40);
                if (isAsciiOnly(prefixToCheck)) {
                    String candidate = t.substring(0, idx).strip();
                    if (!candidate.isBlank()) {
                        t = candidate;
                    }
                }
            }
        }

        return t.isBlank() ? value.trim() : t;
    }

    /** Strips leading LLM preamble such as {@code "Here is your translation: "}. */
    private String stripLeadingArtifactPreamble(final String text) {
        String lower = text.toLowerCase(Locale.ENGLISH);
        for (String prefix : ARTIFACT_LINE_PREFIXES) {
            if (lower.startsWith(prefix)) {
                // Find where the actual translated content starts (after the first colon+space)
                int colonIdx = text.indexOf(':');
                if (colonIdx > 0 && colonIdx < text.length() - 1) {
                    String remainder = text.substring(colonIdx + 1).strip();
                    if (!remainder.isBlank()) {
                        return remainder;
                    }
                }
                // No colon — the entire text is the preamble; return as-is (safety guard)
                return text;
            }
        }
        return text;
    }

    /** Returns {@code true} when {@code text} starts with a known English artifact phrase. */
    private boolean isArtifactLine(final String text) {
        String lower = text.toLowerCase(Locale.ENGLISH);
        for (String prefix : ARTIFACT_LINE_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }


    // -----------------------------------------------------------------------
    // Fallback-detection helpers  (unchanged)
    // -----------------------------------------------------------------------

    /**
     * Detects when an LLM-based translation service has returned a source-language
     * "fallback explanation" instead of an actual translation.
     *
     * <p>Some LLM models (e.g. locally-hosted Ollama) respond in English (or the source
     * language) when they cannot translate a word — for example returning
     * {@code "This is the phonetic translation of 'xyz'..."} instead of target-language text.
     * Such responses are useless as translations and should be treated as failures so the
     * original text is kept instead.</p>
     *
     * <p>Two heuristics are applied:</p>
     * <ol>
     *   <li>The response starts with the original input text verbatim (the LLM echoed
     *       the input back before adding an explanation).</li>
     *   <li>The target language normally uses a non-Latin script, but every character in the
     *       response is ASCII — the LLM responded entirely in the source language.</li>
     * </ol>
     */
    private boolean isLikelyFallbackExplanation(final String originalText,
                                                 final String translatedText,
                                                 final String targetLang) {
        if (translatedText.startsWith(originalText)) {
            return true;
        }
        return isNonLatinScriptLanguage(targetLang) && isAsciiOnly(translatedText);
    }

    private boolean isNonLatinScriptLanguage(final String langCode) {
        if (langCode == null) {
            return false;
        }
        return switch (langCode.toLowerCase()) {
            case "ja", "jp"          -> true;
            case "zh", "zh-cn", "zh-tw" -> true;
            case "ko"                -> true;
            case "ru", "bg", "uk"    -> true;
            case "ar", "fa", "ur"    -> true;
            case "hi", "mr"          -> true;
            case "th"                -> true;
            case "el"                -> true;
            default                  -> false;
        };
    }

    private boolean isAsciiOnly(final String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }


    // -----------------------------------------------------------------------
    // Inner DTOs (mirror KIP-544 contract — no compile-time dependency needed)
    // -----------------------------------------------------------------------
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

