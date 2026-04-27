package com.integration.translation.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response body returned by the Ollama {@code POST /api/generate} endpoint
 * when {@code stream} is set to {@code false}.
 *
 * <p>Only fields relevant to the translation use-case are mapped; the full
 * Ollama response contains additional timing and token-count metadata that is
 * intentionally ignored via {@link JsonIgnoreProperties}.</p>
 *
 * <p>Reference: <a href="https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-completion">
 * Ollama API - Generate a Completion</a></p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaGenerateResponse {

    /**
     * The model tag that produced this response (echoed from the request).
     */
    @JsonProperty("model")
    private String model;

    /**
     * The generated text produced by the model.
     * For a translation prompt this contains only the translated text
     * (given the prompt instructs the model to return nothing else).
     */
    @JsonProperty("response")
    private String response;

    /**
     * {@code true} when the model has finished generating all tokens.
     * Always {@code true} when {@code stream=false}.
     */
    @JsonProperty("done")
    private boolean done;

    /**
     * ISO-8601 timestamp of when the response was created inside Ollama.
     */
    @JsonProperty("created_at")
    private String createdAt;
}

