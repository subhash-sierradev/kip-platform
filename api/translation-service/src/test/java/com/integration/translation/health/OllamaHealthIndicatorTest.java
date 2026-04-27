package com.integration.translation.health;

import com.integration.translation.client.OllamaClient;
import com.integration.translation.client.OllamaClient.OllamaClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaHealthIndicatorTest {

    @Mock
    private OllamaClient ollamaClient;

    private OllamaHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new OllamaHealthIndicator(ollamaClient);
    }

    @Test
    @DisplayName("health() returns UP when Ollama is reachable")
    void health_ollamaReachable_returnsUp() {
        when(ollamaClient.isReachable()).thenReturn(true);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("health() returns DOWN when Ollama is not reachable")
    void health_ollamaNotReachable_returnsDown() {
        when(ollamaClient.isReachable()).thenReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("health() returns DOWN when OllamaClient throws an exception")
    void health_ollamaThrows_returnsDown() {
        when(ollamaClient.isReachable())
                .thenThrow(new OllamaClientException("Connection refused"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}

