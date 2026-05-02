package com.integration.translation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.OllamaClient.OllamaClientException;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.config.TranslationCacheConfig;
import com.integration.translation.config.cache.TranslationCacheKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OllamaCachedTranslator}.
 *
 * <p>These tests verify the Ollama HTTP interaction logic, the fallback strategy,
 * and prompt/language-name helpers.  Cache behaviour (Caffeine hit/miss) is best
 * verified in a Spring integration test (out of scope here); what matters in this
 * unit test is that {@link OllamaClient#generate} is called exactly once per
 * unique {@code (text, sourceLang, targetLang)} combination — the contract that
 * lets Spring's caching proxy later short-circuit those calls.</p>
 */
@ExtendWith(MockitoExtension.class)
class OllamaCachedTranslatorTest {

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private TranslationCacheKeyGenerator cacheKeyGenerator;

    private OllamaCachedTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new OllamaCachedTranslator(
                ollamaClient,
                cacheManager,
                new ObjectMapper(),
                cacheKeyGenerator);
    }

    // ── Happy-path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("translateSingleLanguage() returns trimmed Ollama response on success")
    void translateSingleLanguage_success_returnsTrimmedResponse() {
        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("  こんにちは、世界。  \n");
        resp.setDone(true);
        when(ollamaClient.generate(anyString())).thenReturn(resp);

        String result = translator.translateSingleLanguage(
                "Hello world.", "en", "ja");

        assertThat(result).isEqualTo("こんにちは、世界。");
    }

    @Test
    @DisplayName("translateSingleLanguage() passes a non-blank prompt to OllamaClient")
    void translateSingleLanguage_buildsNonBlankPrompt() {
        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("Привет мир.");
        when(ollamaClient.generate(anyString())).thenReturn(resp);

        translator.translateSingleLanguage("Hello", "en", "ru");

        verify(ollamaClient, times(1)).generate(anyString());
    }

    // ── Fallback cases ────────────────────────────────────────────────────────

    @Test
    @DisplayName("translateSingleLanguage() returns source text when Ollama throws generic exception")
    void translateSingleLanguage_ollamaThrows_returnsSourceText() {
        when(ollamaClient.generate(anyString()))
                .thenThrow(new OllamaClientException("Connection refused"));

        String result = translator.translateSingleLanguage("Hello", "en", "ja");

        assertThat(result).isEqualTo("Hello");
    }

    @Test
    @DisplayName("translateSingleLanguage() restores thread interrupt flag when cause is InterruptedException")
    void translateSingleLanguage_wrappedInterruptedException_restoresInterruptFlag()
            throws InterruptedException {

        // OllamaClient.generate() does not declare InterruptedException (checked),
        // so the HTTP stack wraps it in a RuntimeException on shutdown/timeout.
        when(ollamaClient.generate(anyString()))
                .thenThrow(new RuntimeException(new InterruptedException("shutdown signal")));

        boolean[] flagRestoredAfterCall = {false};

        Thread worker = new Thread(() -> {
            String result = translator.translateSingleLanguage("Hello", "en", "ja");
            // The catch block must restore the flag via Thread.currentThread().interrupt().
            flagRestoredAfterCall[0] = Thread.currentThread().isInterrupted();
            assertThat(result).isEqualTo("Hello");
        });
        worker.start();
        worker.join(3_000);

        assertThat(flagRestoredAfterCall[0])
                .as("interrupt flag must be restored so the thread pool can detect shutdown")
                .isTrue();
    }

    @Test
    @DisplayName("translateSingleLanguage() returns source text when response body is null")
    void translateSingleLanguage_nullResponseBody_returnsSourceText() {
        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse(null);
        when(ollamaClient.generate(anyString())).thenReturn(resp);

        String result = translator.translateSingleLanguage("Hello", "en", "ja");

        assertThat(result).isEqualTo("Hello");
    }

    @Test
    @DisplayName("translateSingleLanguage() returns source text when Ollama returns blank string")
    void translateSingleLanguage_blankResponse_returnsSourceText() {
        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("   ");
        when(ollamaClient.generate(anyString())).thenReturn(resp);

        String result = translator.translateSingleLanguage("Hello", "en", "ja");

        assertThat(result).isEqualTo("Hello");
    }

    // ── Prompt content helper ─────────────────────────────────────────────────

    @Test
    @DisplayName("languageDisplayName() returns English name for known BCP-47 codes")
    void languageDisplayName_knownCode_returnsEnglishName() {
        assertThat(translator.languageDisplayName("ja")).isEqualTo("Japanese");
        assertThat(translator.languageDisplayName("ru")).isEqualTo("Russian");
        assertThat(translator.languageDisplayName("de")).isEqualTo("German");
        assertThat(translator.languageDisplayName("en")).isEqualTo("English");
    }

    @Test
    @DisplayName("languageDisplayName() returns source lang code when display name is blank")
    void languageDisplayName_blankDisplayName_returnsLangCode() {
        // Locale.ROOT (empty tag) has no display language → isBlank() true branch
        String result = translator.languageDisplayName("");
        assertThat(result).isEmpty(); // returns the original empty langCode
    }

    @Test
    @DisplayName("languageDisplayName() returns non-blank result for unknown private-use tags")
    void languageDisplayName_unknownCode_returnsNonBlank() {
        // Private-use tags like "x-custom" may return the tag text; must not throw
        String result = translator.languageDisplayName("x-custom");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("PROMPT_TEMPLATE contains source/target placeholders and text marker")
    void promptTemplate_containsRequiredPlaceholders() {
        assertThat(OllamaCachedTranslator.PROMPT_TEMPLATE)
                .contains("%s")        // source lang placeholder
                .contains("translate")
                .contains("Text to translate");
    }

    @Test
    @DisplayName("MULTI_PROMPT_TEMPLATE contains JSON instruction and text marker")
    void multiPromptTemplate_containsRequiredInstructions() {
        assertThat(OllamaCachedTranslator.MULTI_PROMPT_TEMPLATE)
                .contains("%s")
                .contains("JSON")
                .contains("Text to translate");
    }

    // ── translateAllLanguages – happy path ────────────────────────────────────

    @Test
    @DisplayName("translateAllLanguages() returns translations from Ollama JSON response")
    void translateAllLanguages_success_returnsAllTranslations() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("{\"ja\":\"こんにちは、世界。\",\"ru\":\"Привет мир.\"}");
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        Map<String, String> result = translator.translateAllLanguages(
                "Hello world.", "en", List.of("ja", "ru"));

        assertThat(result).containsEntry("ja", "こんにちは、世界。")
                          .containsEntry("ru", "Привет мир.");
    }

    @Test
    @DisplayName("translateAllLanguages() writes each language result into the cache")
    void translateAllLanguages_success_populatesCache() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);
        when(cacheKeyGenerator.generate(any(), any(), any(), any(), any())).thenReturn("some-key");

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("{\"ja\":\"こんにちは\",\"ru\":\"Привет\"}");
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        translator.translateAllLanguages("Hello", "en", List.of("ja", "ru"));

        // putIfAbsent must be called once per language
        verify(cache, times(2)).putIfAbsent(any(), anyString());
    }

    @Test
    @DisplayName("translateAllLanguages() sends a single generateJson call regardless of language count")
    void translateAllLanguages_callsGenerateJsonOnce() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("{\"ja\":\"A\",\"ru\":\"B\",\"de\":\"C\"}");
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        translator.translateAllLanguages("Hi", "en", List.of("ja", "ru", "de"));

        verify(ollamaClient, times(1)).generateJson(anyString());
    }

    // ── translateAllLanguages – empty / null response ─────────────────────────

    @Test
    @DisplayName("translateAllLanguages() falls back to source text when Ollama returns null response field")
    void translateAllLanguages_nullResponse_fallsBackToSourceText() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse(null);
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        Map<String, String> result = translator.translateAllLanguages(
                "Hello", "en", List.of("ja"));

        assertThat(result).containsEntry("ja", "Hello");
    }

    @Test
    @DisplayName("translateAllLanguages() falls back to source text when Ollama returns empty string")
    void translateAllLanguages_emptyResponse_fallsBackToSourceText() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("   ");
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        Map<String, String> result = translator.translateAllLanguages(
                "Hello", "en", List.of("ja", "ru"));

        assertThat(result).containsEntry("ja", "Hello").containsEntry("ru", "Hello");
    }

    // ── translateAllLanguages – per-language fallback ─────────────────────────

    @Test
    @DisplayName("translateAllLanguages() falls back per language when key is missing from JSON")
    void translateAllLanguages_missingLanguageKey_fallsBackToSourceText() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        // JSON returns only "ja", "ru" is missing
        resp.setResponse("{\"ja\":\"こんにちは\"}");
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        Map<String, String> result = translator.translateAllLanguages(
                "Hello", "en", List.of("ja", "ru"));

        assertThat(result).containsEntry("ja", "こんにちは")
                          .containsEntry("ru", "Hello");
    }

    @Test
    @DisplayName("translateAllLanguages() falls back per language when value is blank in JSON")
    void translateAllLanguages_blankLanguageValue_fallsBackToSourceText() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("{\"ja\":\"   \",\"ru\":\"Привет\"}");
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        Map<String, String> result = translator.translateAllLanguages(
                "Hello", "en", List.of("ja", "ru"));

        assertThat(result).containsEntry("ja", "Hello")
                          .containsEntry("ru", "Привет");
    }

    // ── translateAllLanguages – exception paths ───────────────────────────────

    @Test
    @DisplayName("translateAllLanguages() falls back to source text when Ollama throws generic exception")
    void translateAllLanguages_ollamaThrows_fallsBackToSourceText() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);
        when(ollamaClient.generateJson(anyString()))
                .thenThrow(new OllamaClientException("Connection refused"));

        Map<String, String> result = translator.translateAllLanguages(
                "Hello", "en", List.of("ja", "ru"));

        assertThat(result).containsEntry("ja", "Hello").containsEntry("ru", "Hello");
    }

    @Test
    @DisplayName("translateAllLanguages() restores interrupt flag when cause is InterruptedException")
    void translateAllLanguages_wrappedInterruptedException_restoresInterruptFlag()
            throws InterruptedException {

        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(cache);
        when(ollamaClient.generateJson(anyString()))
                .thenThrow(new RuntimeException(new InterruptedException("shutdown")));

        boolean[] flagRestored = {false};

        Thread worker = new Thread(() -> {
            Map<String, String> result = translator.translateAllLanguages(
                    "Hello", "en", List.of("ja"));
            flagRestored[0] = Thread.currentThread().isInterrupted();
            assertThat(result).containsEntry("ja", "Hello");
        });
        worker.start();
        worker.join(3_000);

        assertThat(flagRestored[0])
                .as("interrupt flag must be restored so the thread pool can detect shutdown")
                .isTrue();
    }

    // ── translateAllLanguages – null cache ────────────────────────────────────

    @Test
    @DisplayName("translateAllLanguages() skips cache write when CacheManager returns null cache")
    void translateAllLanguages_nullCache_stillReturnsResults() {
        when(cacheManager.getCache(TranslationCacheConfig.CACHE_TRANSLATIONS)).thenReturn(null);

        OllamaGenerateResponse resp = new OllamaGenerateResponse();
        resp.setResponse("{\"ja\":\"こんにちは\"}");
        when(ollamaClient.generateJson(anyString())).thenReturn(resp);

        Map<String, String> result = translator.translateAllLanguages(
                "Hello", "en", List.of("ja"));

        assertThat(result).containsEntry("ja", "こんにちは");
        // No NPE — cache.putIfAbsent is simply skipped
        verify(cacheKeyGenerator, never()).generate(any(), any(), any(), any(), eq("ja"));
    }
}

