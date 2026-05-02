package com.integration.translation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.config.TranslationCacheConfig;
import com.integration.translation.config.cache.TranslationCacheKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring-managed bean that performs text-to-language translation via Ollama
 * and caches results through Spring's AOP proxy.
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
 * Caffeine cache, keyed on a SHA-256 digest of the text plus the two language codes.
 * The TTL is 24 hours (see {@link TranslationCacheConfig}).  Repeated calls with
 * identical parameters are served entirely from memory — no Ollama round-trip.</p>
 *
 * <h3>Multi-language translation</h3>
 * <p>{@link #translateAllLanguages} sends a single Ollama request for all target
 * languages at once using {@code format=json}, then parses the JSON response and
 * writes each per-language result directly into the Caffeine cache.  Subsequent
 * calls to {@link #translateSingleLanguage} for any of those languages are served
 * from cache without an Ollama round-trip.</p>
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
     * Prompt template for single-language translation.
     * Placeholders: source language name, target language name, text to translate.
     */
    static final String PROMPT_TEMPLATE =
            "You are a professional translator. "
            + "Translate the following text from %s to %s. "
            + "Return ONLY the translated text — no explanations, no introduction, "
            + "no quotation marks around the result.\n\n"
            + "Text to translate:\n%s";

    /**
     * Prompt template for multi-language translation (single Ollama call).
     * Placeholders: source language name, comma-separated target language names
     * with BCP-47 codes in parentheses, text to translate.
     *
     * <p>The model is instructed to return a JSON object keyed by BCP-47 code
     * (e.g. {@code {"ja": "...", "ru": "..."}}).  The request uses
     * {@code format=json} so Ollama guarantees valid JSON output.</p>
     */
    static final String MULTI_PROMPT_TEMPLATE =
            "You are a professional translator. "
            + "Translate the following text from %s into these languages: %s. "
            + "Return ONLY a valid JSON object where each key is the BCP-47 language code "
            + "and the value is the translated text. "
            + "No explanations. No markdown. No extra fields. Just the JSON object.\n\n"
            + "Text to translate:\n%s";

    private final OllamaClient ollamaClient;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final TranslationCacheKeyGenerator cacheKeyGenerator;

    /**
     * Translates {@code text} from {@code sourceLang} into all {@code targetLangs}
     * in a <strong>single</strong> Ollama call using structured JSON output mode.
     *
     * <p>After a successful Ollama response each per-language result is written
     * directly into the {@value TranslationCacheConfig#CACHE_TRANSLATIONS} Caffeine
     * cache using the same {@link com.integration.translation.config.cache.TranslationCacheKey}
     * that {@link #translateSingleLanguage}'s {@code @Cacheable} reads.  This means
     * any subsequent call to {@code translateSingleLanguage} for the same text and
     * language will be a cache hit and will not call Ollama.</p>
     *
     * <p>Languages whose translation is missing from, or blank in, the Ollama
     * response fall back to the source text individually — other languages are
     * not affected.</p>
     *
     * @param text        the UTF-8 source text (max 50 000 characters)
     * @param sourceLang  BCP-47 source language code (e.g. {@code "en"})
     * @param targetLangs BCP-47 target language codes (e.g. {@code ["ja", "ru"]})
     * @return map of targetLang → translated text (falls back to {@code text} per language on failure)
     */
    public Map<String, String> translateAllLanguages(
            final String text,
            final String sourceLang,
            final List<String> targetLangs) {

        String sourceName = languageDisplayName(sourceLang);

        // Build a human-readable list like "Japanese (ja), Russian (ru)"
        String targetDescriptions = targetLangs.stream()
                .map(code -> languageDisplayName(code) + " (" + code + ")")
                .collect(Collectors.joining(", "));

        String prompt = String.format(MULTI_PROMPT_TEMPLATE, sourceName, targetDescriptions, text);

        log.debug("Invoking Ollama multi-language: {} → [{}], promptLength={}",
                sourceLang, targetLangs, prompt.length());

        Map<String, String> parsed = new HashMap<>();

        try {
            OllamaGenerateResponse response = ollamaClient.generateJson(prompt);
            String raw = response.getResponse() == null ? "" : response.getResponse().trim();

            if (!raw.isEmpty()) {
                parsed = objectMapper.readValue(raw, new TypeReference<Map<String, String>>() { });
                log.debug("Ollama multi-language response parsed: keys={}", parsed.keySet());
            } else {
                log.warn("Ollama returned empty response for multi-language request; "
                        + "falling back to source text for all languages");
            }
        } catch (Exception ex) {
            if (ex instanceof InterruptedException || ex.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.warn("Multi-language translation interrupted; falling back to source text.");
            } else {
                log.warn("Ollama multi-language translation failed: {}. "
                        + "Falling back to source text for all languages.", ex.getMessage());
            }
        }

        // Resolve final values, apply fallback, and populate the cache for each language
        Map<String, String> results = new HashMap<>(targetLangs.size());
        Cache cache = cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS);

        for (String targetLang : targetLangs) {
            String value = parsed.get(targetLang);
            if (value == null || value.isBlank()) {
                log.warn("No translation found for targetLang={} in Ollama response; "
                        + "falling back to source text.", targetLang);
                value = text;
            }
            results.put(targetLang, value);

            // Write into the same Caffeine cache that @Cacheable on translateSingleLanguage reads.
            // putIfAbsent avoids overwriting a concurrent cache population.
            if (cache != null) {
                Object key = cacheKeyGenerator.generate(this, null, text, sourceLang, targetLang);
                cache.putIfAbsent(key, value);
                log.trace("Cache populated for targetLang={}", targetLang);
            }
        }

        log.debug("Multi-language translation complete: {} language(s) resolved", results.size());
        return results;
    }

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
     * @param langCode BCP-47 language tag
     * @return display name in English, or {@code langCode} if no mapping exists
     */
    String languageDisplayName(final String langCode) {
        Locale locale = Locale.forLanguageTag(langCode);
        String name = locale.getDisplayLanguage(Locale.ENGLISH);
        return name.isBlank() ? langCode : name;
    }
}

