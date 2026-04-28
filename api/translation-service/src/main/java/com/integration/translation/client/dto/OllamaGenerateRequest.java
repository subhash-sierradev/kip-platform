package com.integration.translation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request body sent to the Ollama {@code POST /api/generate} endpoint.
 *
 * <p>Only the fields required for synchronous, non-streaming generation are
 * populated. Ollama ignores any fields it does not recognise, so this class
 * does not need to mirror the full Ollama schema.</p>
 *
 * <p>Reference: <a href="https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-completion">
 * Ollama API - Generate a Completion</a></p>
 */
@Data
@Builder
public class OllamaGenerateRequest {

    /**
     * The Ollama model tag to use (e.g. {@code mistral}, {@code llama3}).
     * Must match a model that has been pulled into the Ollama instance.
     */
    @JsonProperty("model")
    private String model;

    /**
     * The full prompt string sent to the model.
     * Should contain explicit instructions to return only the translated text.
     */
    @JsonProperty("prompt")
    private String prompt;

    /**
     * When {@code false} (the default) Ollama returns a single JSON response
     * object containing the complete generated text, which simplifies parsing.
     * Streaming ({@code true}) would yield newline-delimited JSON objects and
     * is not used here.
     */
    @JsonProperty("stream")
    private boolean stream;

    /**
     * Optional model-level parameters forwarded verbatim to Ollama.
     *
     * <p>Supported keys include {@code num_predict} (max tokens to generate)
     * and {@code num_ctx} (context window size).  When {@code null} Ollama
     * uses its built-in defaults.  Setting {@code num_predict} to a reasonable
     * ceiling (e.g. {@code 2048}) prevents runaway generation on slow hardware
     * and keeps inference time predictable.</p>
     */
    @JsonProperty("options")
    private Map<String, Object> options;
}

