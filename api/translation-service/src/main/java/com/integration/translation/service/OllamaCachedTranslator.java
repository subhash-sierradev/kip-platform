package com.integration.translation.service;

import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.config.TranslationCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Spring-managed bean that performs a <em>single</em> text-to-language translation
 * and caches the result through Spring's AOP proxy.
 *
 * <h3>Why this class exists (proxy-safe caching)</h3>
 * <p>{@link OllamaTranslationService#translate} must iterate over multiple target
 * languages and call a cached helper for each one.  If that helper lived in the
 * same class, Spring's {@code @Cacheable} annotation would be silently ignored:
 * internal {@code this.method()} calls bypass the AOP proxy that intercepts and
 * applies the cache advice.</p>
 *
 * <p>Extracting the cacheable logic into this dedicated bean means every call from
 * {@link OllamaTranslationService} goes <em>through</em> the proxy, so the cache
 * is correctly consulted before — and populated after — each Ollama request.</p>
 *
 * <h3>Cache behaviour</h3>
 * <p>Results are stored in the {@value TranslationCacheConfig#CACHE_TRANSLATIONS}
 * Caffeine cache, keyed on {@code text + "::" + sourceLang + "::" + targetLang}.
 * The TTL is 24 hours (see {@link TranslationCacheConfig}).  Repeated calls with
 * identical parameters are served entirely from memory — no Ollama round-trip.</p>
 *
 * <h3>Fallback strategy</h3>
 * <p>If Ollama is unreachable or returns an empty/null body, the original source
 * text is returned unchanged so the translation pipeline can continue for the
 * remaining target languages without surfacing a 5xx to the caller.</p>
 *
 * @see OllamaTranslationService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaCachedTranslator {

    /**
     * Prompt template used for every Ollama request.
     * Placeholders (positional): source language name, target language name,
     * the text to translate.
     *
     * <p>Instructions are written in English regardless of the target language
     * because instruction-tuned models (e.g. Mistral) respond more reliably to
     * English directives.</p>
     */
    static final String PROMPT_TEMPLATE =
            "You are a professional translator. "
            + "Translate the following text from %s to %s. "
            + "Return ONLY the translated text — no explanations, no introduction, "
            + "no quotation marks around the result.\n\n"
            + "Text to translate:\n%s";

    private final OllamaClient ollamaClient;

    /**
     * Translates {@code text} from {@code sourceLang} into {@code targetLang} via
     * Ollama, caching the result for 24 hours.
     *
     * <p>This method <strong>must</strong> be called from another Spring bean (not
     * via {@code this}) so that the AOP caching proxy is active.  It is intentionally
     * {@code public} — Spring proxies only intercept public methods.</p>
     *
     * <p>Cache key: a {@link com.integration.translation.config.cache.TranslationCacheKey}
     * built by {@link com.integration.translation.config.cache.TranslationCacheKeyGenerator}
     * using a SHA-256 digest of the text plus the two language codes. Keys are
     * compact (constant size) regardless of input text length.</p>
     *
     * @param text       the UTF-8 source text (max 50 000 characters)
     * @param sourceLang BCP-47 source language code (e.g. {@code "en"})
     * @param targetLang BCP-47 target language code (e.g. {@code "ja"})
     * @return translated text trimmed of surrounding whitespace,
     *         or {@code text} unchanged if Ollama fails or returns empty output
     */
    @Cacheable(
        cacheNames = TranslationCacheConfig.CACHE_TRANSLATIONS,
        keyGenerator = TranslationCacheConfig.KEY_GENERATOR
    )
    public String translateSingleLanguage(
            final String text,
            final String sourceLang,
            final String targetLang) {

        String sourceName = languageDisplayName(sourceLang);
        String targetName = languageDisplayName(targetLang);
        String prompt = String.format(PROMPT_TEMPLATE, sourceName, targetName, text);

        log.debug("Invoking Ollama: {} → {}, promptLength={}",
                sourceLang, targetLang, prompt.length());

        try {
            OllamaGenerateResponse response = ollamaClient.generate(prompt);

            String translated = response.getResponse() == null
                    ? text
                    : response.getResponse().trim();

            if (translated.isEmpty()) {
                log.warn("Ollama returned empty translation for targetLang={}; "
                        + "falling back to source text", targetLang);
                return text;
            }

            log.debug("Ollama translation OK: targetLang={}, outputLength={}",
                    targetLang, translated.length());
            return translated;

        } catch (Exception ex) {
            // If the exception is (or wraps) an InterruptedException, restore the
            // interrupt flag so the thread pool / shutdown logic can detect it.
            // OllamaClient.generate() does not declare InterruptedException, but the
            // underlying HTTP stack may wrap one inside a RuntimeException on timeout
            // or when the application is shutting down.
            if (ex instanceof InterruptedException
                    || ex.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.warn("Translation interrupted for targetLang={}; "
                        + "falling back to source text.", targetLang);
            } else {
                log.warn("Ollama translation failed for targetLang={}: {}. "
                        + "Falling back to source text.", targetLang, ex.getMessage());
            }
            return text;
        }
    }

    /**
     * Converts a BCP-47 language code to a human-readable English name that
     * instruction-tuned LLMs understand more reliably than raw codes.
     *
     * <p>Examples: {@code "ja"} → {@code "Japanese"},
     * {@code "ru"} → {@code "Russian"}, {@code "zh"} → {@code "Chinese"}.</p>
     *
     * <p>{@link Locale#getDisplayLanguage(Locale)} is specified to always return a
     * non-null value, so only the blank check is required.</p>
     *
     * @param langCode BCP-47 language tag
     * @return display name in English, or {@code langCode} if no mapping exists
     */
    String languageDisplayName(final String langCode) {
        Locale locale = Locale.forLanguageTag(langCode);
        String name = locale.getDisplayLanguage(Locale.ENGLISH);
        return name.isBlank() ? langCode : name;
    }
}

