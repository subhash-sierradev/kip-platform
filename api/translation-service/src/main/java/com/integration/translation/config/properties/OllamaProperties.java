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
 *   timeout-seconds: 300
 *   connect-timeout-seconds: 10
 *   num-predict: 2048
 * }</pre>
 *
 * <p>Swap the {@link #model} to any model available in your Ollama installation
 * (e.g. {@code llama3}, {@code gemma2}) without any code changes.</p>
 *
 * <p><b>Tuning guidance for slow hardware:</b> increase {@link #timeoutSeconds}
 * to 300+ seconds and set {@link #numPredict} to cap output tokens (e.g. 2048)
 * so the model cannot run indefinitely on large inputs. The
 * {@code OLLAMA_TIMEOUT_SECONDS} and {@code OLLAMA_NUM_PREDICT} environment
 * variables override these at runtime without rebuilding the image.</p>
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
     *
     * <p>Set this high enough to accommodate slow hardware or large texts.
     * On consumer hardware without a GPU, Mistral 7B generates approximately
     * 2–5 tokens per second; a 2 000-character batch may require 150–300 s.
     * Setting this to {@code 300} gives a comfortable 5-minute ceiling.</p>
     *
     * <p>Override via the {@code OLLAMA_TIMEOUT_SECONDS} environment variable.</p>
     */
    @Min(5)
    private int timeoutSeconds = 300;

    /**
     * Maximum time (in seconds) to wait for a TCP connection to Ollama.
     */
    @Min(1)
    private int connectTimeoutSeconds = 10;

    /**
     * Maximum number of tokens Ollama is allowed to generate per request
     * ({@code num_predict} Ollama option).
     *
     * <ul>
     *   <li>{@code -1} — unlimited (Ollama default; may run indefinitely)</li>
     *   <li>{@code -2} — fill the model's context window</li>
     *   <li>{@code 2048} — safe ceiling for batches up to ~1 500 words; prevents
     *       runaway generation without truncating realistic translations</li>
     * </ul>
     *
     * <p>Override via the {@code OLLAMA_NUM_PREDICT} environment variable.</p>
     */
    @Min(-2)
    private int numPredict = 2048;
}

