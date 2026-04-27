package com.integration.execution.service.processor;

import com.integration.execution.client.TranslationApiClient;
import com.integration.execution.config.properties.TranslationApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Post-render, pre-publish translation step for Confluence reports.
 *
 * <p>After FreeMarker renders the monitoring report in English, this component
 * determines whether the configured integration requires translation, calls the
 * KIP-544 Translation API, and returns the translated content.  All failure
 * modes are handled gracefully: any API error causes a {@code WARN} log and
 * the original content is returned so the job can continue uninterrupted.</p>
 *
 * <h3>Skip conditions (no API call made)</h3>
 * <ul>
 *   <li>{@link TranslationApiProperties#isEnabled()} is {@code false}.</li>
 *   <li>{@code content} is {@code null} or blank.</li>
 *   <li>No {@code targetLanguage} can be resolved (empty list or all codes equal
 *       {@code sourceLanguage}).</li>
 *   <li>The resolved {@code targetLanguage} equals {@code sourceLanguage}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceTranslationStep {

    private static final String DEFAULT_SOURCE_LANGUAGE = "en";

    private final TranslationApiProperties translationApiProperties;
    private final TranslationApiClient translationApiClient;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Translate {@code content} if the integration's language configuration requires it.
     *
     * <p>Picks the first language code from {@code languageCodes} that differs from
     * {@code sourceLanguage} as the single translation target.  If all codes match the
     * source language (or the list is empty), the original content is returned unchanged.</p>
     *
     * @param content        rendered FreeMarker output (UTF-8 XHTML / plain text)
     * @param sourceLanguage BCP-47 code of the template language (e.g. {@code "en"});
     *                       {@code null} defaults to {@code "en"}
     * @param languageCodes  target language codes configured on the integration
     * @return translated content, or {@code content} when translation is skipped / fails
     */
    public String translateIfNeeded(final String content,
                                    final String sourceLanguage,
                                    final List<String> languageCodes) {
        String effectiveSource = (sourceLanguage != null && !sourceLanguage.isBlank())
                ? sourceLanguage : DEFAULT_SOURCE_LANGUAGE;

        String targetLanguage = resolveTargetLanguage(languageCodes, effectiveSource);
        return translate(content, effectiveSource, targetLanguage);
    }

    /**
     * Translate {@code content} from {@code sourceLanguage} to {@code targetLanguage}.
     * Returns the original {@code content} unchanged when translation is skipped or fails.
     *
     * @param content        page content to translate
     * @param sourceLanguage BCP-47 source language code
     * @param targetLanguage BCP-47 target language code; if {@code null}/blank or equal
     *                       to {@code sourceLanguage}, the content is returned as-is
     * @return translated (or original) content, never {@code null}
     */
    public String translate(final String content,
                            final String sourceLanguage,
                            final String targetLanguage) {
        if (!translationApiProperties.isEnabled()) {
            log.debug("Translation is disabled (translation.api.enabled=false); skipping.");
            return content;
        }

        if (content == null || content.isBlank()) {
            return content;
        }

        String src = (sourceLanguage != null && !sourceLanguage.isBlank())
                ? sourceLanguage : DEFAULT_SOURCE_LANGUAGE;

        if (targetLanguage == null || targetLanguage.isBlank()
                || targetLanguage.equalsIgnoreCase(src)) {
            log.debug("Target language '{}' equals source '{}'; skipping translation.",
                    targetLanguage, src);
            return content;
        }

        log.info("Translating Confluence page content from '{}' to '{}' ({} chars)",
                src, targetLanguage, content.length());

        Optional<String> translated = translationApiClient.translate(content, src, targetLanguage);

        if (translated.isEmpty()) {
            log.warn("Translation API did not return a result for '{}' → '{}'; "
                    + "falling back to original content.", src, targetLanguage);
            return content;
        }

        log.info("Translation succeeded: {} chars → {} chars ('{}')",
                content.length(), translated.get().length(), targetLanguage);
        return translated.get();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the first language code in {@code languageCodes} that differs
     * (case-insensitive) from {@code sourceLanguage}.
     * Returns {@code sourceLanguage} itself if no such code exists.
     */
    private String resolveTargetLanguage(final List<String> languageCodes,
                                         final String sourceLanguage) {
        if (languageCodes == null || languageCodes.isEmpty()) {
            return sourceLanguage;
        }
        return languageCodes.stream()
                .filter(code -> code != null && !code.isBlank()
                        && !code.equalsIgnoreCase(sourceLanguage))
                .findFirst()
                .orElse(sourceLanguage);
    }
}

