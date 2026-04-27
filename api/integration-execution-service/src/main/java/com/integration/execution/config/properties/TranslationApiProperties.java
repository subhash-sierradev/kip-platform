package com.integration.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for the KIP-544 Translation API.
 * Injected into {@link com.integration.execution.client.TranslationApiClient}.
 *
 * <p>All values can be overridden via environment variables:</p>
 * <pre>
 *   TRANSLATION_API_BASE_URL    — base URL of the translation service (default: http://localhost:8083)
 *   TRANSLATION_API_ENABLED     — whether translation is active (default: true)
 *   TRANSLATION_API_TIMEOUT     — read timeout in seconds            (default: 30)
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "translation.api")
public class TranslationApiProperties {

    /**
     * Base URL of the Translation Service (KIP-544).
     * Example: {@code http://localhost:8083}
     */
    @NotBlank(message = "translation.api.base-url must not be blank")
    private String baseUrl = "http://localhost:8083";

    /**
     * Master switch. Set to {@code false} to skip all translation calls and
     * publish the original English content unchanged.
     */
    private boolean enabled = true;

    /**
     * Read / response timeout for a single translation HTTP call (seconds).
     * Should be generous: Ollama LLM inference can take 5-30 s.
     */
    @Min(value = 1, message = "translation.api.timeout-seconds must be >= 1")
    private int timeoutSeconds = 30;
}

