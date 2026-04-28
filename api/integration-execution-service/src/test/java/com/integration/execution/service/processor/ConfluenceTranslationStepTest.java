package com.integration.execution.service.processor;

import com.integration.execution.config.properties.TranslationApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfluenceTranslationStep}.
 * Covers the skip-logic layer; XHTML DOM manipulation is tested in XhtmlTextTranslatorTest.
 */
@ExtendWith(MockitoExtension.class)
class ConfluenceTranslationStepTest {

    private static final String ENGLISH_CONTENT = "<p>Hello World. This is a monitoring report.</p>";
    private static final String JAPANESE_TRANSLATED = "<p>こんにちは世界。これは監視レポートです。</p>";
    private static final String GERMAN_TRANSLATED = "<p>Hallo Welt. Dies ist ein Überwachungsbericht.</p>";

    @Mock
    private TranslationApiProperties translationApiProperties;

    @Mock
    private XhtmlTextTranslator xhtmlTextTranslator;

    private ConfluenceTranslationStep step;

    @BeforeEach
    void setUp() {
        step = new ConfluenceTranslationStep(translationApiProperties, xhtmlTextTranslator);
    }

    @Test
    void translate_whenLanguagesDiffer_delegatesToXhtmlTranslator() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "ja"))
                .thenReturn(JAPANESE_TRANSLATED);

        String result = step.translate(ENGLISH_CONTENT, "en", "ja");

        assertThat(result).isEqualTo(JAPANESE_TRANSLATED);
        verify(xhtmlTextTranslator).translateXhtml(ENGLISH_CONTENT, "en", "ja");
    }

    @Test
    void translate_whenTargetEqualsSource_skipsXhtmlTranslator() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", "en");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenTargetIsNull_skipsXhtmlTranslator() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", null);

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenXhtmlTranslatorReturnsFallback_resultIsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(ENGLISH_CONTENT);

        String result = step.translate(ENGLISH_CONTENT, "en", "de");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
    }

    @Test
    void translate_whenGloballyDisabled_skipsXhtmlTranslatorAndReturnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(false);

        String result = step.translate(ENGLISH_CONTENT, "en", "ja");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenContentIsBlank_returnsBlankWithoutDelegating() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate("   ", "en", "ja");

        assertThat(result).isEqualTo("   ");
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenContentIsNull_returnsNullWithoutDelegating() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(null, "en", "ja");

        assertThat(result).isNull();
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translate_caseInsensitiveLanguageCodeComparison_treatsEnAndENasSame() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", "EN");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenSourceLanguageIsNull_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(GERMAN_TRANSLATED);

        String result = step.translate(ENGLISH_CONTENT, null, "de");

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
        verify(xhtmlTextTranslator).translateXhtml(ENGLISH_CONTENT, "en", "de");
    }

    @Test
    void translate_whenSourceLanguageIsBlank_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(GERMAN_TRANSLATED);

        String result = step.translate(ENGLISH_CONTENT, "   ", "de");

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
        verify(xhtmlTextTranslator).translateXhtml(ENGLISH_CONTENT, "en", "de");
    }

    @Test
    void translate_whenTargetLanguageIsBlank_skipsXhtmlTranslator() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translate(ENGLISH_CONTENT, "en", "  ");

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translateIfNeeded_picksFirstNonSourceLanguageCode() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "ja"))
                .thenReturn(JAPANESE_TRANSLATED);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", List.of("en", "ja"));

        assertThat(result).isEqualTo(JAPANESE_TRANSLATED);
        verify(xhtmlTextTranslator).translateXhtml(ENGLISH_CONTENT, "en", "ja");
    }

    @Test
    void translateIfNeeded_whenAllLanguageCodesMatchSource_returnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", List.of("en"));

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translateIfNeeded_whenLanguageCodesIsEmpty_returnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", List.of());

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translateIfNeeded_whenLanguageCodesIsNull_returnsOriginal() {
        when(translationApiProperties.isEnabled()).thenReturn(true);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en", null);

        assertThat(result).isEqualTo(ENGLISH_CONTENT);
        verify(xhtmlTextTranslator, never()).translateXhtml(anyString(), anyString(), anyString());
    }

    @Test
    void translateIfNeeded_whenSourceLanguageIsNull_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(GERMAN_TRANSLATED);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, null, List.of("de"));

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
    }

    @Test
    void translateIfNeeded_whenSourceLanguageIsBlank_defaultsToEnglish() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "ja"))
                .thenReturn(JAPANESE_TRANSLATED);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "  ", List.of("ja"));

        assertThat(result).isEqualTo(JAPANESE_TRANSLATED);
    }

    @Test
    void translateIfNeeded_listWithNullAndBlankCodes_skipsInvalidEntries() {
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(xhtmlTextTranslator.translateXhtml(ENGLISH_CONTENT, "en", "de"))
                .thenReturn(GERMAN_TRANSLATED);

        String result = step.translateIfNeeded(ENGLISH_CONTENT, "en",
                Arrays.asList(null, "  ", "de"));

        assertThat(result).isEqualTo(GERMAN_TRANSLATED);
    }
}

