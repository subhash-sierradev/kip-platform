package com.integration.translation.service;

import com.integration.translation.model.request.TranslationRequest;
import com.integration.translation.model.response.CognitiveServicesUsage;
import com.integration.translation.model.response.TranslationResponse;
import com.integration.translation.model.response.TranslationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@link TranslationService} implementation backed by a locally hosted
 * <a href="https://ollama.com">Ollama</a> LLM.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>For each target language code in the request,
 *       {@link OllamaCachedTranslator#translateSingleLanguage} is called.
 *       That call travels through Spring's AOP proxy, so the Caffeine cache
 *       is consulted before — and populated after — every Ollama round-trip.</li>
 *   <li>Ollama's response text is trimmed and stored as a
 *       {@link TranslationResult}.</li>
 *   <li>If Ollama is unreachable or returns an error, the original source text
 *       is used as a fallback (handled inside {@link OllamaCachedTranslator})
 *       so the caller always receives a usable response.</li>
 * </ol>
 *
 * <h3>Caching architecture</h3>
 * <p>{@code @Cacheable} annotations only intercept calls that arrive
 * <em>through</em> a Spring AOP proxy.  Calling a {@code @Cacheable} method
 * on {@code this} would bypass the proxy entirely, silently disabling the cache.
 * This class therefore delegates all Ollama interactions to the separate
 * {@link OllamaCachedTranslator} bean, which is wire-injected and always called
 * via its proxy reference.</p>
 *
 * <h3>Replacing this implementation</h3>
 * <p>Create a new {@code @Service @Primary} class that implements
 * {@link TranslationService}.  No controller or model changes are required.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaTranslationService implements TranslationService {

    /**
     * Proxy-safe cached translator.  Every call to
     * {@link OllamaCachedTranslator#translateSingleLanguage} passes through
     * Spring's AOP proxy so the {@code @Cacheable} cache advice is applied.
     */
    private final OllamaCachedTranslator cachedTranslator;

    /**
     * {@inheritDoc}
     *
     * <p>Sends a <strong>single</strong> Ollama request for all target languages
     * using {@code format=json}, then parses the JSON response to assemble each
     * {@link TranslationResult}.  Each per-language result is also written into
     * the Caffeine cache by {@link OllamaCachedTranslator#translateAllLanguages},
     * so subsequent requests for the same text and any of those languages are
     * served entirely from memory without an Ollama round-trip.</p>
     *
     * <p>{@link CognitiveServicesUsage#getTranslatorTranslateTextCharacterCount()}
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

        // Single Ollama call for all languages; results are also written into cache
        Map<String, String> translations =
                cachedTranslator.translateAllLanguages(text, sourceLang, targets);

        List<TranslationResult> results = targets.stream()
                .map(targetLang -> TranslationResult.builder()
                        .translatedTimestamp(Instant.now().getEpochSecond())
                        .languageCode(targetLang)
                        .value(translations.get(targetLang))
                        .build())
                .toList();

        int charCount = text.length() * targets.size();
        log.info("Translation complete: {} result(s), {} characters billed",
                results.size(), charCount);

        return TranslationResponse.builder()
                .cognitiveServicesUsage(CognitiveServicesUsage.ofTranslation(charCount))
                .translationResults(results)
                .extractOnDisk(false)
                .build();
    }
}
