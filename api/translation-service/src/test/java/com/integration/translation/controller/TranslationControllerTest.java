package com.integration.translation.controller;

import com.integration.translation.model.request.TranslationRequest;
import com.integration.translation.model.response.CognitiveServicesUsage;
import com.integration.translation.model.response.TranslationResponse;
import com.integration.translation.model.response.TranslationResult;
import com.integration.translation.service.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationControllerTest {

    @Mock
    private TranslationService translationService;

    private TranslationController controller;

    @BeforeEach
    void setUp() {
        controller = new TranslationController(translationService);
    }

    @Test
    @DisplayName("translate() returns 200 with translation results from service")
    void translate_validRequest_returns200WithResults() {
        TranslationRequest request = new TranslationRequest(
                "Hello world.", "en", List.of("ja"));

        TranslationResponse mockResponse = TranslationResponse.builder()
                .cognitiveServicesUsage(CognitiveServicesUsage.ofTranslation(12))
                .translationResults(List.of(
                        TranslationResult.builder()
                                .translatedTimestamp(Instant.now().getEpochSecond())
                                .languageCode("ja")
                                .value("こんにちは、世界。")
                                .build()
                ))
                .extractOnDisk(false)
                .build();

        when(translationService.translate(any(TranslationRequest.class)))
                .thenReturn(mockResponse);

        ResponseEntity<TranslationResponse> response = controller.translate(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTranslationResults()).hasSize(1);
        assertThat(response.getBody().getTranslationResults().get(0).getLanguageCode())
                .isEqualTo("ja");
        assertThat(response.getBody().getTranslationResults().get(0).getValue())
                .isEqualTo("こんにちは、世界。");
    }

    @Test
    @DisplayName("translate() returns results for multiple language codes")
    void translate_multipleTargets_returnsOneResultPerLanguage() {
        TranslationRequest request = new TranslationRequest(
                "Hello world.", "en", List.of("ja", "ru"));

        TranslationResponse mockResponse = TranslationResponse.builder()
                .cognitiveServicesUsage(CognitiveServicesUsage.ofTranslation(24))
                .translationResults(List.of(
                        TranslationResult.builder().languageCode("ja").value("こんにちは").build(),
                        TranslationResult.builder().languageCode("ru").value("Привет мир").build()
                ))
                .extractOnDisk(false)
                .build();

        when(translationService.translate(any(TranslationRequest.class)))
                .thenReturn(mockResponse);

        ResponseEntity<TranslationResponse> response = controller.translate(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTranslationResults()).hasSize(2);
    }

    @Test
    @DisplayName("translate() delegates correctly to the service")
    void translate_delegatesToService() {
        TranslationRequest request = new TranslationRequest(
                "Hello", "en", List.of("ja"));

        TranslationResponse mockResponse = TranslationResponse.builder()
                .translationResults(List.of(
                        TranslationResult.builder().languageCode("ja").value("こんにちは").build()))
                .extractOnDisk(false)
                .build();

        when(translationService.translate(request)).thenReturn(mockResponse);

        controller.translate(request);

        verify(translationService).translate(request);
    }

    @Test
    @DisplayName("translate() response has extractOnDisk=false")
    void translate_extractOnDiskIsFalse() {
        TranslationRequest request = new TranslationRequest(
                "Hello", "en", List.of("ja"));

        TranslationResponse mockResponse = TranslationResponse.builder()
                .translationResults(List.of(
                        TranslationResult.builder().languageCode("ja").value("こんにちは").build()))
                .extractOnDisk(false)
                .build();

        when(translationService.translate(any())).thenReturn(mockResponse);

        ResponseEntity<TranslationResponse> response = controller.translate(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isExtractOnDisk()).isFalse();
    }

    @Test
    @DisplayName("translate() populates cognitive services usage")
    void translate_cognitiveServicesUsageIsPopulated() {
        TranslationRequest request = new TranslationRequest(
                "Hello world", "en", List.of("ja"));

        TranslationResponse mockResponse = TranslationResponse.builder()
                .cognitiveServicesUsage(CognitiveServicesUsage.ofTranslation(11))
                .translationResults(List.of(
                        TranslationResult.builder().languageCode("ja").value("こんにちは").build()))
                .extractOnDisk(false)
                .build();

        when(translationService.translate(any())).thenReturn(mockResponse);

        ResponseEntity<TranslationResponse> response = controller.translate(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCognitiveServicesUsage()
                .getTranslatorTranslateTextCharacterCount())
                .isEqualTo(11);
    }
}
