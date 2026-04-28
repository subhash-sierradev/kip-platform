package com.integration.translation.client;

import com.integration.translation.client.OllamaClient.OllamaClientException;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.config.properties.OllamaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OllamaProperties ollamaProperties;

    @InjectMocks
    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        // Use lenient() so that isReachable() tests (which don't call getModel())
        // do not fail with UnnecessaryStubbingException.
        lenient().when(ollamaProperties.getBaseUrl()).thenReturn("http://localhost:11434");
        lenient().when(ollamaProperties.getModel()).thenReturn("mistral");
        lenient().when(ollamaProperties.getNumPredict()).thenReturn(2048);
    }

    @Test
    @DisplayName("generate() returns response on success")
    void generate_success_returnsResponse() {
        OllamaGenerateResponse body = new OllamaGenerateResponse();
        body.setResponse("こんにちは");
        body.setDone(true);

        when(restTemplate.postForEntity(anyString(), any(), eq(OllamaGenerateResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        OllamaGenerateResponse result = ollamaClient.generate("Translate hello");

        assertThat(result).isNotNull();
        assertThat(result.getResponse()).isEqualTo("こんにちは");
        assertThat(result.isDone()).isTrue();
    }

    @Test
    @DisplayName("generate() throws OllamaClientException when RestTemplate throws")
    void generate_restTemplateThrows_throwsOllamaClientException() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OllamaGenerateResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> ollamaClient.generate("prompt"))
                .isInstanceOf(OllamaClientException.class)
                .hasMessageContaining("Ollama request failed");
    }

    @Test
    @DisplayName("generate() throws OllamaClientException when response body is null")
    void generate_nullBody_throwsOllamaClientException() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OllamaGenerateResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> ollamaClient.generate("prompt"))
                .isInstanceOf(OllamaClientException.class)
                .hasMessageContaining("empty response body");
    }

    @Test
    @DisplayName("isReachable() returns true when server responds")
    void isReachable_serverResponds_returnsTrue() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Ollama is running");

        assertThat(ollamaClient.isReachable()).isTrue();
    }

    @Test
    @DisplayName("isReachable() returns false when server is unreachable")
    void isReachable_serverUnreachable_returnsFalse() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThat(ollamaClient.isReachable()).isFalse();
    }
}



