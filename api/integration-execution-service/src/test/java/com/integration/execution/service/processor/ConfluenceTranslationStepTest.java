package com.integration.execution.service.processor;

import com.integration.execution.client.TranslationApiClient;
import com.integration.execution.config.properties.TranslationApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfluenceTranslationStep}.
 *
 * <p>Covers the four acceptance-criteria scenarios:
 * <ol>
 *   <li>Translation called and result used when source ≠ target language.</li>
 *   <li>No API call when source language equals target language.</li>
 *   <li>Fallback to original content when the Translation API throws / returns empty.</li>
 *   <li>Translation globally disabled via {@code translation.api.enabled=false}.</li>
 * </ol>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ConfluenceTranslationStepTest {

    private static final String ENGLISH_CONTENT = "<p>Hello World. This is a monitoring report.</p>";
    private static final String JAPANESE_TRANSLATED = "<p>こんにちは世界。これは監視レポートです。</p>";
    private static final String GERMAN_TRANSLATED = "<p>Hallo Welt. Dies ist ein Überwachungsbericht.</p>";

    @Mock
    private TranslationApiProperties translationApiProperties;

    @Mock
    private TranslationApiClient translationApiClient;

    private ConfluenceTranslationStep step;

    @BeforeEach
    void setUp() {
        step = new ConfluenceTranslationStep(translationApiProperties, translationApiClient);
    }

    // -----------------------------------------------------------------------
    // translate() — direct method
    // -----------------------------------------------------------------------

    @Test
    void translate_whenLanguagesDiffer_callsApiAndReturnsTranslatedContent() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "ja"))
                .thenReturn(Optional.of(JAPANESE_TRANSLATED));

        String result = step.translate(ENGLISH_CONTENT, "en", "ja");

        assertThat(result).isEqualTo(JAPANESE_TRANSLATED);
        verify(translationApiClient).translate(ENGLISH_CONTENT, "en", "ja");
    }

    @Test
    void translate_whenTargetEqualsSource_skipsApiCall() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", "en");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenTargetIsNull_skipsApiCall() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", null);

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenApiReturnsEmpty_fallsBackToOriginalContent() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(Optional.empty());

        String result = step.translate(ENGLISH_CONTENT, "en", "de");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
    }

    @Test
    void translate_whenGloballyDisabled_skipsApiCallAndReturnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(false);

        String result = step.translate(ENGLISH_CONTENT, "en", "ja");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenContentIsBlank_returnsBlankWithoutApiCall() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate("   ", "en", "ja");

        assertThat(result).isEqualTo("   ");
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenContentIsNull_returnsNullWithoutApiCall() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(null, "en", "ja");

        assertThat(result).isNull();
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_caseInsensitiveLanguageCodeComparison_treatsEnAndENasSame() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", "EN");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenSourceLanguageIsNull_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(Optional.of(GERMAN_TRANSLATED));

        // null sourceLanguage must default to "en"
        String result = step.translate(ENGLISH_CONTENT, null, "de");

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
        verify(translationApiClient).translate(ENGLISH_CONTENT, "en", "de");
    }

    @Test
    void translate_whenSourceLanguageIsBlank_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(Optional.of(GERMAN_TRANSLATED));

        // blank sourceLanguage must default to "en"
        String result = step.translate(ENGLISH_CONTENT, "   ", "de");

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
        verify(translationApiClient).translate(ENGLISH_CONTENT, "en", "de");
    }

    @Test
    void translate_whenTargetLanguageIsBlank_skipsApiCall() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", "  ");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // translateIfNeeded() — entry point from the orchestrator
    // -----------------------------------------------------------------------

    @Test
    void translateIfNeeded_picksFirstNonSourceLanguageCode() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "ja"))
                .thenReturn(Optional.of(JAPANESE_TRANSLATED));

        // languageCodes contains "en" (same as source) and "ja" (target)
        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", List.of("en", "ja"));

        assertThat(result).isEqualTo(JAPANESE_TRANSLATED);
        verify(translationApiClient).translate(ENGLISH_CONTENT, "en", "ja");
    }

    @Test
    void translateIfNeeded_whenAllLanguageCodesMatchSource_returnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", List.of("en"));

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translateIfNeeded_whenLanguageCodesIsEmpty_returnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", List.of());

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translateIfNeeded_whenLanguageCodesIsNull_returnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", null);

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translateIfNeeded_whenSourceLanguageIsNull_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(Optional.of(GERMAN_TRANSLATED));

        String result = step.translateIfNeeded(ENGLISH_CONTENT, null, List.of("de"));

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
    }

    @Test
    void translateIfNeeded_whenSourceLanguageIsBlank_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "ja"))
                .thenReturn(Optional.of(JAPANESE_TRANSLATED));

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "  ", List.of("ja"));

        assertThat(result).isEqualTo(JAPANESE_TRANSLATED);
    }

    @Test
    void translateIfNeeded_listWithNullAndBlankCodes_skipsInvalidEntries() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiClient.translate(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(Optional.of(GERMAN_TRANSLATED));

        // null and blank codes should be skipped; "de" should be picked as the target
        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en",
                java.util.Arrays.asList(null, "  ", "de"));

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
    }
}

