package com.integration.translation.service;

import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.config.TranslationCacheConfig;
import com.integration.translation.model.request.TranslationRequest;
import com.integration.translation.model.response.CognitiveServicesUsage;
import com.integration.translation.model.response.TranslationResponse;
import com.integration.translation.model.response.TranslationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link TranslationService} implementation backed by a locally hosted
 * <a href="https://ollama.com">Ollama</a> LLM.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>For each target language code in the request, a structured translation
 *       prompt is constructed and sent to Ollama via {@link OllamaClient}.</li>
 *   <li>Ollama's response text is trimmed and stored as a
 *       {@link TranslationResult}.</li>
 *   <li>If Ollama is unreachable or returns an error, the original source text
 *       is used as a fallback so the caller always receives a usable response.</li>
 *   <li>Results for identical {@code (text, sourceLang, targetLang)} triples are
 *       cached in Caffeine for 24 hours to avoid redundant inference calls.</li>
 * </ol>
 *
 * <h3>Prompt design</h3>
 * <p>The prompt explicitly instructs the model to return <em>only</em> the
 * translated text — no preamble, no explanation.  This keeps post-processing
 * simple: trim whitespace and use the result directly.  The prompt is assembled
 * in English regardless of the target language because instruction-tuned models
 * generally follow English instructions more reliably.</p>
 *
 * <h3>Replacing this implementation</h3>
 * <p>Create a new {@code @Service} class annotated with
 * {@code @Primary} (or activated via a Spring profile) that implements
 * {@link TranslationService}.  No other changes are needed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaTranslationService implements TranslationService {

    /**
     * Prompt template.  Placeholders (in order): source language name,
     * target language name, the text to translate.
     */
    private static final String PROMPT_TEMPLATE =
            "You are a professional translator. "
            + "Translate the following text from %s to %s. "
            + "Return ONLY the translated text — no explanations, no introduction, "
            + "no quotation marks around the result.\n\n"
            + "Text to translate:\n%s";

    private final OllamaClient ollamaClient;

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over every requested target language, invoking
     * {@link #translateSingleLanguage(String, String, String)} for each one.
     * The overall {@link CognitiveServicesUsage#getTranslatorTranslateTextCharacterCount()}
     * is set to {@code len(text) * numberOfLanguages} to give callers a rough
     * cost indicator equivalent to what a cloud provider would report.</p>
     */
    @Override
    public TranslationResponse translate(final TranslationRequest request) {
        String text = request.getTextToTranslate();
        String sourceLang = request.getSourceLanguage();
        List<String> targets = request.getLanguageCodes();

        log.info("Translation request: sourceLang={}, targets={}, textLength={}",
                sourceLang, targets, text.length());

        List<TranslationResult> results = new ArrayList<>(targets.size());

        for (String targetLang : targets) {
            String translated = translateSingleLanguage(text, sourceLang, targetLang);
            results.add(TranslationResult.builder()
                    .translatedTimestamp(Instant.now().getEpochSecond())
                    .languageCode(targetLang)
                    .value(translated)
                    .build());
        }

        int charCount = text.length() * targets.size();
        log.info("Translation complete: {} result(s), {} characters billed",
                results.size(), charCount);

        return TranslationResponse.builder()
                .cognitiveServicesUsage(CognitiveServicesUsage.ofTranslation(charCount))
                .translationResults(results)
                .extractOnDisk(false)
                .build();
    }

    /**
     * Translates a single piece of text into one target language.
     *
     * <p>Results are cached by Caffeine using the composite key
     * {@code text + "::" + sourceLang + "::" + targetLang}, so repeated calls
     * with identical parameters are served from memory without hitting Ollama.</p>
     *
     * <p>If the Ollama call fails for any reason the original {@code text} is
     * returned so the translation pipeline can continue for remaining languages.</p>
     *
     * @param text       the UTF-8 text to translate
     * @param sourceLang BCP-47 source language code (e.g. {@code "en"})
     * @param targetLang BCP-47 target language code (e.g. {@code "ja"})
     * @return translated text, or {@code text} on fallback
     */
    @Cacheable(
        cacheNames = TranslationCacheConfig.CACHE_TRANSLATIONS,
        key = "#text + '::' + #sourceLang + '::' + #targetLang"
    )
    public String translateSingleLanguage(
            final String text,
            final String sourceLang,
            final String targetLang) {

        String sourceName = languageDisplayName(sourceLang);
        String targetName = languageDisplayName(targetLang);
        String prompt = String.format(PROMPT_TEMPLATE, sourceName, targetName, text);

        log.debug("Invoking Ollama: {} → {}, promptLength={}", sourceLang, targetLang, prompt.length());

        try {
            OllamaGenerateResponse response = ollamaClient.generate(prompt);
            String translated = response.getResponse() == null
                    ? text
                    : response.getResponse().trim();

            if (translated.isEmpty()) {
                log.warn("Ollama returned empty translation for targetLang={}; using source text", targetLang);
                return text;
            }

            log.debug("Ollama translation OK: targetLang={}, outputLength={}", targetLang, translated.length());
            return translated;

        } catch (Exception ex) {
            log.warn("Ollama translation failed for targetLang={}: {}. Falling back to source text.",
                    targetLang, ex.getMessage());
            return text;
        }
    }

    /**
     * Converts a BCP-47 language code to a human-readable display name in English,
     * which models understand more reliably than raw codes.
     *
     * <p>For example, {@code "ja"} → {@code "Japanese"}, {@code "ru"} → {@code "Russian"}.</p>
     *
     * @param langCode BCP-47 code
     * @return display name, or the original code if no display name is available
     */
    private String languageDisplayName(final String langCode) {
        Locale locale = Locale.forLanguageTag(langCode);
        String name = locale.getDisplayLanguage(Locale.ENGLISH);
        return (name == null || name.isBlank()) ? langCode : name;
    }
}

