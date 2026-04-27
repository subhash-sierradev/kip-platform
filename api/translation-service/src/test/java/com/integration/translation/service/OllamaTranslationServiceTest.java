package com.integration.translation.service;

import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.OllamaClient.OllamaClientException;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.model.request.TranslationRequest;
import com.integration.translation.model.response.TranslationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaTranslationServiceTest {

    @Mock
    private OllamaClient ollamaClient;

    @InjectMocks
    private OllamaTranslationService service;

    private TranslationRequest request;

    @BeforeEach
    void setUp() {
        request = new TranslationRequest(
                "Hello world. Please translate this document.",
                "en",
                List.of("ja", "ru")
        );
    }

    @Test
    @DisplayName("translate() returns one result per target language on success")
    void translate_successfulTranslation_returnsResultPerLanguage() {
        OllamaGenerateResponse jaResponse = new OllamaGenerateResponse();
        jaResponse.setResponse("こんにちは、世界。この文書を翻訳してください。");
        jaResponse.setDone(true);

        OllamaGenerateResponse ruResponse = new OllamaGenerateResponse();
        ruResponse.setResponse("Привет мир. Пожалуйста, переведите этот документ.");
        ruResponse.setDone(true);

        when(ollamaClient.generate(anyString()))
                .thenReturn(jaResponse)
                .thenReturn(ruResponse);

        TranslationResponse response = service.translate(request);

        assertThat(response).isNotNull();
        assertThat(response.getTranslationResults()).hasSize(2);
        assertThat(response.getTranslationResults().get(0).getLanguageCode()).isEqualTo("ja");
        assertThat(response.getTranslationResults().get(0).getValue())
                .isEqualTo("こんにちは、世界。この文書を翻訳してください。");
        assertThat(response.getTranslationResults().get(1).getLanguageCode()).isEqualTo("ru");
        assertThat(response.getTranslationResults().get(1).getValue())
                .isEqualTo("Привет мир. Пожалуйста, переведите этот документ.");
    }

    @Test
    @DisplayName("translate() populates cognitiveServicesUsage with character count")
    void translate_populatesCharacterCount() {
        OllamaGenerateResponse response = new OllamaGenerateResponse();
        response.setResponse("Translated");
        response.setDone(true);
        when(ollamaClient.generate(anyString())).thenReturn(response);

        int expectedCharCount = "Hello world. Please translate this document.".length() * 2;

        TranslationResponse result = service.translate(request);

        assertThat(result.getCognitiveServicesUsage()
                .getTranslatorTranslateTextCharacterCount())
                .isEqualTo(expectedCharCount);
    }

    @Test
    @DisplayName("translate() falls back to source text when Ollama throws")
    void translate_ollamaThrows_fallsBackToSourceText() {
        when(ollamaClient.generate(anyString()))
                .thenThrow(new OllamaClientException("Connection refused"));

        TranslationResponse response = service.translate(request);

        // Both results should fall back to the original text
        assertThat(response.getTranslationResults()).hasSize(2);
        response.getTranslationResults().forEach(result ->
                assertThat(result.getValue())
                        .isEqualTo("Hello world. Please translate this document."));
    }

    @Test
    @DisplayName("translate() falls back when Ollama returns empty string")
    void translate_ollamaReturnsEmpty_fallsBackToSourceText() {
        OllamaGenerateResponse emptyResponse = new OllamaGenerateResponse();
        emptyResponse.setResponse("  ");
        emptyResponse.setDone(true);
        when(ollamaClient.generate(anyString())).thenReturn(emptyResponse);

        TranslationResponse response = service.translate(request);

        response.getTranslationResults().forEach(result ->
                assertThat(result.getValue())
                        .isEqualTo("Hello world. Please translate this document."));
    }

    @Test
    @DisplayName("translate() falls back when Ollama returns null body response")
    void translate_ollamaReturnsNullResponse_fallsBackToSourceText() {
        OllamaGenerateResponse nullBodyResponse = new OllamaGenerateResponse();
        nullBodyResponse.setResponse(null);
        nullBodyResponse.setDone(true);
        when(ollamaClient.generate(anyString())).thenReturn(nullBodyResponse);

        TranslationResponse response = service.translate(request);

        response.getTranslationResults().forEach(result ->
                assertThat(result.getValue())
                        .isEqualTo("Hello world. Please translate this document."));
    }

    @Test
    @DisplayName("translate() trims whitespace from Ollama response")
    void translate_ollamaResponseHasWhitespace_isTrimmed() {
        OllamaGenerateResponse trimResponse = new OllamaGenerateResponse();
        trimResponse.setResponse("  こんにちは  \n");
        trimResponse.setDone(true);
        when(ollamaClient.generate(anyString())).thenReturn(trimResponse);

        TranslationRequest singleTarget = new TranslationRequest(
                "Hello", "en", List.of("ja"));

        TranslationResponse response = service.translate(singleTarget);

        assertThat(response.getTranslationResults().get(0).getValue()).isEqualTo("こんにちは");
    }

    @Test
    @DisplayName("translate() sets translatedTimestamp to a recent epoch second")
    void translate_setsTimestamp() {
        long before = System.currentTimeMillis() / 1000L;

        OllamaGenerateResponse r = new OllamaGenerateResponse();
        r.setResponse("OK");
        when(ollamaClient.generate(anyString())).thenReturn(r);

        TranslationRequest singleTarget = new TranslationRequest("Hi", "en", List.of("ja"));
        TranslationResponse response = service.translate(singleTarget);

        long after = System.currentTimeMillis() / 1000L + 1;
        long ts = response.getTranslationResults().get(0).getTranslatedTimestamp();
        assertThat(ts).isBetween(before, after);
    }

    @Test
    @DisplayName("translate() calls Ollama once per language code")
    void translate_callsOllamaOncePerLanguage() {
        OllamaGenerateResponse r = new OllamaGenerateResponse();
        r.setResponse("Translated");
        when(ollamaClient.generate(anyString())).thenReturn(r);

        service.translate(request); // 2 target languages

        verify(ollamaClient, times(2)).generate(anyString());
    }

    @Test
    @DisplayName("translate() returns extractOnDisk=false")
    void translate_extractOnDiskIsFalse() {
        OllamaGenerateResponse r = new OllamaGenerateResponse();
        r.setResponse("Translated");
        when(ollamaClient.generate(anyString())).thenReturn(r);

        TranslationResponse response = service.translate(request);

        assertThat(response.isExtractOnDisk()).isFalse();
    }
}

