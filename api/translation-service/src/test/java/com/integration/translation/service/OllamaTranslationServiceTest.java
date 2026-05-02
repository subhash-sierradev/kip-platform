package com.integration.translation.service;

import com.integration.translation.model.request.TranslationRequest;
import com.integration.translation.model.response.TranslationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OllamaTranslationService}.
 *
 * <p>These tests verify the <em>orchestration</em> logic of the service:
 * delegating to {@link OllamaCachedTranslator#translateAllLanguages} in a single
 * call, assembling the response envelope, and billing character counts.
 * Actual Ollama HTTP calls and cache behaviour are exercised separately in
 * {@link OllamaCachedTranslatorTest}.</p>
 *
 * <p>{@link OllamaCachedTranslator} is mocked here, which also confirms that the
 * service correctly delegates through the injected bean (rather than via
 * {@code this}, which would bypass Spring's caching proxy).</p>
 */
@ExtendWith(MockitoExtension.class)
class OllamaTranslationServiceTest {

    @Mock
    private OllamaCachedTranslator cachedTranslator;

    private OllamaTranslationService service;

    @BeforeEach
    void setUp() {
        service = new OllamaTranslationService(cachedTranslator);
    }

    @Test
    @DisplayName("translate() returns one result per target language on success")
    void translate_successfulTranslation_returnsResultPerLanguage() {
        TranslationRequest request = new TranslationRequest(
                "Hello world.", "en", List.of("ja", "ru"));

        when(cachedTranslator.translateAllLanguages("Hello world.", "en", List.of("ja", "ru")))
                .thenReturn(Map.of("ja", "こんにちは、世界。", "ru", "Привет мир."));

        TranslationResponse response = service.translate(request);

        assertThat(response.getTranslationResults()).hasSize(2);
        assertThat(response.getTranslationResults().get(0).getLanguageCode()).isEqualTo("ja");
        assertThat(response.getTranslationResults().get(0).getValue())
                .isEqualTo("こんにちは、世界。");
        assertThat(response.getTranslationResults().get(1).getLanguageCode()).isEqualTo("ru");
        assertThat(response.getTranslationResults().get(1).getValue())
                .isEqualTo("Привет мир.");
    }

    @Test
    @DisplayName("translate() populates cognitiveServicesUsage with character count")
    void translate_populatesCharacterCount() {
        TranslationRequest request = new TranslationRequest(
                "Hello world.", "en", List.of("ja", "ru"));

        when(cachedTranslator.translateAllLanguages("Hello world.", "en", List.of("ja", "ru")))
                .thenReturn(Map.of("ja", "こんにちは", "ru", "Привет"));

        TranslationResponse result = service.translate(request);

        // "Hello world." = 12 chars × 2 languages = 24
        assertThat(result.getCognitiveServicesUsage()
                .getTranslatorTranslateTextCharacterCount())
                .isEqualTo(24);
    }

    @Test
    @DisplayName("translate() delegates to cachedTranslator once for all languages — not this.method()")
    void translate_delegatesToCachedTranslatorPerLanguage() {
        TranslationRequest request = new TranslationRequest(
                "Hi", "en", List.of("ja", "ru"));

        when(cachedTranslator.translateAllLanguages("Hi", "en", List.of("ja", "ru")))
                .thenReturn(Map.of("ja", "こんにちは", "ru", "Привет"));

        service.translate(request);

        verify(cachedTranslator, times(1))
                .translateAllLanguages("Hi", "en", List.of("ja", "ru"));
    }

    @Test
    @DisplayName("translate() propagates fallback text from cachedTranslator")
    void translate_cachedTranslatorReturnsFallback_propagatesIt() {
        TranslationRequest request = new TranslationRequest(
                "Hello", "en", List.of("ja"));
        // Simulate fallback: OllamaCachedTranslator returns the source text
        when(cachedTranslator.translateAllLanguages("Hello", "en", List.of("ja")))
                .thenReturn(Map.of("ja", "Hello"));

        TranslationResponse response = service.translate(request);

        assertThat(response.getTranslationResults().get(0).getValue()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("translate() sets translatedTimestamp to a recent epoch second")
    void translate_setsTimestamp() {
        long before = System.currentTimeMillis() / 1000L;
        when(cachedTranslator.translateAllLanguages("Hi", "en", List.of("ja")))
                .thenReturn(Map.of("ja", "こんにちは"));

        TranslationResponse response = service.translate(
                new TranslationRequest("Hi", "en", List.of("ja")));

        long after = System.currentTimeMillis() / 1000L + 1;
        long ts = response.getTranslationResults().get(0).getTranslatedTimestamp();
        assertThat(ts).isBetween(before, after);
    }

    @Test
    @DisplayName("translate() returns extractOnDisk=false")
    void translate_extractOnDiskIsFalse() {
        when(cachedTranslator.translateAllLanguages("Hi", "en", List.of("ja")))
                .thenReturn(Map.of("ja", "こんにちは"));

        TranslationResponse response = service.translate(
                new TranslationRequest("Hi", "en", List.of("ja")));

        assertThat(response.isExtractOnDisk()).isFalse();
    }
}
