package com.integration.translation.service;

import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.config.TranslationCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Mistral-native {@code [INST]...[/INST]} prompt format.
     *
     * <p>Mistral instruction-tuned models are trained to produce <em>only</em> the
     * continuation after {@code [/INST]}.  Framing the request as a completion task
     * — rather than a rule list — suppresses preambles, disclaimers, and parenthetical
     * notes far more reliably than any post-processing heuristic.</p>
     */
    static final String PROMPT_TEMPLATE =
            "[INST] You are a translation function. "
            + "Output ONLY the translated text — nothing else. "
            + "No preamble, explanation, notes, annotations, or source text. "
            + "Do not write in English unless the target language is English.\n\n"
            + "Translate from %s to %s:\n\n%s [/INST]";

    /** Matches common LLM preamble patterns at the start of a response. */
    private static final Pattern LEADING_ARTIFACT_PATTERN = Pattern.compile(
            "(?i)^(here is (your |the )translation[^:\\n]*:\\s*"
            + "|translated text:\\s*"
            + "|translation:\\s*"
            + "|this is the translated text[^:\\n]*:\\s*)",
            Pattern.CASE_INSENSITIVE);

    /** Inline English markers after which target-language text should have ended. */
    private static final List<String> ARTIFACT_INLINE_MARKERS =
            List.of(" Here is", " Please note", " (Note:", " Note —", " Note:",
                    " (The translation", " (Translation", " (This translation",
                    " (This is a", " (This is the");



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
        keyGenerator = TranslationCacheConfig.KEY_GENERATOR,
        unless = "#result == #p0"   // Do not cache fallbacks: if result == source text, Ollama failed silently
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

            if (response.getResponse() == null) {
                log.warn("Ollama returned null response body for targetLang={}; "
                        + "falling back to source text", targetLang);
                return text;
            }

            String translated = response.getResponse().trim();

            if (translated.isEmpty()) {
                log.warn("Ollama returned empty translation for targetLang={}; "
                        + "falling back to source text", targetLang);
                return text;
            }

            String cleaned = cleanLlmResponse(translated);
            if (!cleaned.equals(translated)) {
                log.debug("Stripped LLM artifacts from {} translation (before={} chars, after={} chars)",
                        targetLang, translated.length(), cleaned.length());
            }

            log.debug("Ollama translation OK: targetLang={}, outputLength={}",
                    targetLang, cleaned.length());
            return cleaned;

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
     * Strips LLM verbosity artifacts from a raw Ollama response.
     *
     * <p>Strategy: since all translatable values in this system are short phrases
     * (labels, names, summaries), the translated text is always a single line.
     * Any content after the first {@code \n} is an LLM disclaimer/explanation
     * and is unconditionally discarded.</p>
     *
     * <p>After newline-truncation, inline English artifact suffixes on the same
     * line (e.g. {@code "著者 Here is the translation…"}) are stripped.</p>
     *
     * <p>Leading preambles such as {@code "Here is your translation: 報告詳細"} or
     * {@code "This is the translated text…: 報告詳細"} are also stripped.</p>
     *
     * <p>Falls back to the original trimmed value if cleanup produces a blank
     * result (safety guard).</p>
     */
    String cleanLlmResponse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String t = raw.trim();

        // 1. Strip leading preamble e.g. "Here is your translation: 報告詳細"
        t = stripLeadingArtifact(t);

        // 2. Truncate everything after the first \n — unconditionally.
        //    All translatable values in this system are single-line phrases.
        //    Any content after \n is always an LLM disclaimer/explanation.
        int nIdx = t.indexOf('\n');
        if (nIdx > 0) {
            t = t.substring(0, nIdx).strip();
        }

        // 3. Strip inline English artifact suffix on the same line,
        //    e.g. "著者 Here is the translation of 'Authors' into Japanese."
        //    Check only the first 40 chars of the suffix for ASCII to avoid stripping
        //    suffixes that happen to end with the translated word.
        for (String marker : ARTIFACT_INLINE_MARKERS) {
            int idx = t.indexOf(marker);
            if (idx > 0) {
                String suffix = t.substring(idx);
                String prefixToCheck = suffix.length() <= 40 ? suffix : suffix.substring(0, 40);
                if (isAsciiOnly(prefixToCheck)) {
                    t = t.substring(0, idx).strip();
                }
            }
        }

        return t.isBlank() ? raw.trim() : t;
    }

    /** Removes a leading preamble matched by {@link #LEADING_ARTIFACT_PATTERN}. */
    private String stripLeadingArtifact(final String text) {
        Matcher m = LEADING_ARTIFACT_PATTERN.matcher(text);
        if (m.lookingAt()) {
            String remainder = text.substring(m.end()).strip();
            return remainder.isBlank() ? text : remainder;
        }
        return text;
    }


    /** Returns {@code true} if every character in {@code text} is ASCII (≤ 127). */
    private boolean isAsciiOnly(final String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 127) {
                return false;
            }
        }
        return true;
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
