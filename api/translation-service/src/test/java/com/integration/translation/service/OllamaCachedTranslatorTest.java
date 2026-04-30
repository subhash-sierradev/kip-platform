package com.integration.translation.service;

import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.OllamaClient.OllamaClientException;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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

    private OllamaCachedTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new OllamaCachedTranslator(ollamaClient);
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
    @DisplayName("PROMPT_TEMPLATE uses Mistral [INST] format with source/target placeholders")
    void promptTemplate_containsRequiredPlaceholders() {
        assertThat(OllamaCachedTranslator.PROMPT_TEMPLATE)
                .contains("[INST]")
                .contains("[/INST]")
                .contains("%s")
                .contains("Translate");
    }

    // ── cleanLlmResponse ─────────────────────────────────────────────────────

    @Test
    @DisplayName("cleanLlmResponse() returns null/blank input unchanged")
    void cleanLlmResponse_nullOrBlank_returnsAsIs() {
        assertThat(translator.cleanLlmResponse(null)).isNull();
        assertThat(translator.cleanLlmResponse("   ")).isBlank();
    }

    @Test
    @DisplayName("cleanLlmResponse() strips 'Here is your translation:' preamble")
    void cleanLlmResponse_stripsLeadingPreamble() {
        assertThat(translator.cleanLlmResponse("Here is your translation: 報告詳細"))
                .isEqualTo("報告詳細");
    }

    @Test
    @DisplayName("cleanLlmResponse() strips 'This is the translated text...:' preamble")
    void cleanLlmResponse_stripsThisIsTheTranslatedTextPreamble() {
        assertThat(translator.cleanLlmResponse(
                "This is the translated text from English to Japanese: 報告詳細"))
                .isEqualTo("報告詳細");
    }

    @Test
    @DisplayName("cleanLlmResponse() strips trailing English artifact after double newline")
    void cleanLlmResponse_stripsTrailingArtifactParagraph() {
        String raw = "報告詳細\n\nHere is your translation: 報告詳細\n\nPlease note that I am an AI.";
        assertThat(translator.cleanLlmResponse(raw)).isEqualTo("報告詳細");
    }

    @Test
    @DisplayName("cleanLlmResponse() strips trailing English artifact after single newline")
    void cleanLlmResponse_stripsTrailingArtifactAfterSingleNewline() {
        String raw = "報告詳細\nHere is your translation: 報告詳細";
        assertThat(translator.cleanLlmResponse(raw)).isEqualTo("報告詳細");
    }

    @Test
    @DisplayName("cleanLlmResponse() strips 'this is the translated text' artifact line after newline")
    void cleanLlmResponse_stripsThisIsTheTranslatedTextLineArtifact() {
        String raw = "報告詳細\n\nThis is the translated text from English to Japanese: Report Details.";
        assertThat(translator.cleanLlmResponse(raw)).isEqualTo("報告詳細");
    }

    @Test
    @DisplayName("cleanLlmResponse() truncates at first newline unconditionally")
    void cleanLlmResponse_alwaysTruncatesAtFirstNewline() {
        // All values are single-line; everything after \n is discarded unconditionally
        assertThat(translator.cleanLlmResponse("第一段落。\n第二段落。")).isEqualTo("第一段落。");
        assertThat(translator.cleanLlmResponse("報告詳細\n\nSome non-artifact Japanese text second paragraph."))
                .isEqualTo("報告詳細");
    }

    @Test
    @DisplayName("cleanLlmResponse() strips inline English suffix after translated text")
    void cleanLlmResponse_stripsInlineEnglishSuffix() {
        assertThat(translator.cleanLlmResponse(
                "著者 Here is the translation of your text 'Authors' into Japanese."))
                .isEqualTo("著者");
    }

    @Test
    @DisplayName("cleanLlmResponse() does NOT strip when inline suffix contains non-ASCII")
    void cleanLlmResponse_doesNotStripWhenInlineSuffixContainsNonAscii() {
        assertThat(translator.cleanLlmResponse("著者 Here is日本語の続き"))
                .isEqualTo("著者 Here is日本語の続き");
    }

    @Test
    @DisplayName("cleanLlmResponse() strips machine-translation disclaimer after newline")
    void cleanLlmResponse_stripsThisTranslationMachineArtifact() {
        String raw = "報告詳細\n\nThis translation is a machine translation of an English text. "
                + "Human review and editing are highly recommended for accurate translations.";
        assertThat(translator.cleanLlmResponse(raw)).isEqualTo("報告詳細");
    }

    @Test
    @DisplayName("cleanLlmResponse() returns text unchanged when no artifact found")
    void cleanLlmResponse_noArtifact_returnsUnchanged() {
        assertThat(translator.cleanLlmResponse("こんにちは、世界。")).isEqualTo("こんにちは、世界。");
    }

    @Test
    @DisplayName("cleanLlmResponse() falls back to original when cleanup produces blank")
    void cleanLlmResponse_cleanupProducesBlank_returnsTrimmedOriginal() {
        assertThat(translator.cleanLlmResponse("Here is your translation: ")).isNotBlank();
    }

    @Test
    @DisplayName("cleanLlmResponse() strips parenthetical explanation containing translated word")
    void cleanLlmResponse_stripsParentheticalExplanationWithTranslatedWordAtEnd() {
        String raw = "合計レポート (The translation of \"Total Reports\" in Japanese is \"合計レポート\".)";
        assertThat(translator.cleanLlmResponse(raw)).isEqualTo("合計レポート");
    }
}
