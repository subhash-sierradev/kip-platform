package com.integration.translation.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration properties for the Ollama LLM sidecar.
 *
 * <p>Bound from the {@code ollama.*} namespace in {@code application.yml}.
 * Example configuration:</p>
 * <pre>{@code
 * ollama:
 *   base-url: http://ollama:11434
 *   model: mistral
 *   timeout-seconds: 120
 *   connect-timeout-seconds: 10
 * }</pre>
 *
 * <p>Swap the {@link #model} to any model available in your Ollama installation
 * (e.g. {@code llama3}, {@code gemma2}) without any code changes.</p>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    /**
     * Base URL of the Ollama REST API server.
     * Defaults to {@code http://localhost:11434} for local developer machines.
     */
    @NotBlank
    private String baseUrl = "http://localhost:11434";

    /**
     * Ollama model tag to use for translation prompts.
     * The model must be pulled inside the Ollama container before use.
     */
    @NotBlank
    private String model = "mistral";

    /**
     * Maximum time (in seconds) to wait for Ollama to generate a full response.
     * Set this high enough to accommodate slow hardware or large texts.
     */
    @Min(5)
    private int timeoutSeconds = 120;

    /**
     * Maximum time (in seconds) to wait for a TCP connection to Ollama.
     */
    @Min(1)
    private int connectTimeoutSeconds = 10;
}

