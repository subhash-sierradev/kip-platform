package com.integration.execution.service.processor;

import com.integration.execution.config.properties.TranslationApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Post-render, pre-publish translation step for Confluence reports.
 *
 * <p>After FreeMarker renders the monitoring report in English this component checks
 * whether translation is required (enabled, languages differ) and delegates the actual
 * XHTML text-node translation to {@link XhtmlTextTranslator}.  All failure modes are
 * handled gracefully: any error causes a {@code WARN} log and the original content
 * is returned so the job can continue uninterrupted.</p>
 *
 * <h3>Skip conditions (no API call made)</h3>
 * <ul>
 *   <li>{@link TranslationApiProperties#isEnabled()} is {@code false}.</li>
 *   <li>{@code content} is {@code null} or blank.</li>
 *   <li>{@code targetLanguage} is {@code null}/blank or equals {@code sourceLanguage}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceTranslationStep {

    private static final String DEFAULT_SOURCE_LANGUAGE = "en";

    private final TranslationApiProperties translationApiProperties;
    private final XhtmlTextTranslator xhtmlTextTranslator;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Translate {@code content} if the integration's language configuration requires it.
     *
     * <p>Picks the first language code from {@code languageCodes} that differs from
     * {@code sourceLanguage} as the single translation target.</p>
     *
     * @param content        rendered FreeMarker output (Confluence Storage Format XHTML)
     * @param sourceLanguage BCP-47 code of the template language; {@code null} defaults to {@code "en"}
     * @param languageCodes  target language codes configured on the integration
     * @return translated content, or {@code content} when translation is skipped / fails
     */
    public String translateIfNeeded(final String content,
                                    final String sourceLanguage,
                                    final List<String> languageCodes) {
        String effectiveSource = resolveSource(sourceLanguage);
        String targetLanguage = resolveTargetLanguage(languageCodes, effectiveSource);
        return translate(content, effectiveSource, targetLanguage);
    }

    /**
     * Translate {@code content} from {@code sourceLanguage} to {@code targetLanguage}.
     * Delegates all DOM parsing and chunked API calls to {@link XhtmlTextTranslator}.
     *
     * @param content        Confluence Storage Format XHTML
     * @param sourceLanguage BCP-47 source language code
     * @param targetLanguage BCP-47 target language code; {@code null}/blank or same as source → skip
     * @return translated (or original) content
     */
    public String translate(final String content,
                            final String sourceLanguage,
                            final String targetLanguage) {
        if (!translationApiProperties.isEnabled()) {
            log.debug("Translation disabled (translation.api.enabled=false); skipping.");
            return content;
        }

        if (content == null || content.isBlank()) {
            return content;
        }

        String src = resolveSource(sourceLanguage);

        if (targetLanguage == null || targetLanguage.isBlank()
                || targetLanguage.equalsIgnoreCase(src)) {
            log.debug("Target '{}' equals source '{}'; skipping translation.", targetLanguage, src);
            return content;
        }

        log.info("Translating Confluence page content from '{}' to '{}' ({} chars)",
                src, targetLanguage, content.length());

        return xhtmlTextTranslator.translateXhtml(content, src, targetLanguage);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String resolveSource(final String sourceLanguage) {
        return (sourceLanguage != null && !sourceLanguage.isBlank())
                ? sourceLanguage : DEFAULT_SOURCE_LANGUAGE;
    }

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

