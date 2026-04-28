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
 *   TRANSLATION_API_MAX_CHARS   — max characters per batch           (default: 45000)
 * </pre>
 *
 * <p><strong>Ollama note</strong>: the default {@code timeout-seconds} of 30 s is
 * calibrated for a cloud translation API.  When using a locally-hosted Ollama model
 * override <em>both</em> {@code timeout-seconds} and {@code max-chars} in
 * {@code application-dev.yml} — Ollama needs more time and smaller batches.</p>
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
     *
     * <p>Must be <em>greater</em> than {@code ollama.timeout-seconds} in the
     * Translation Service so that Ollama's inference has time to finish before
     * the IES-side HTTP socket times out.  Set to {@code 150} in dev when
     * using Ollama (which has a default inference timeout of 120 s).</p>
     */
    @Min(value = 1, message = "translation.api.timeout-seconds must be >= 1")
    private int timeoutSeconds = 30;

    /**
     * Maximum characters per translation batch sent to the Translation Service.
     *
     * <p>For cloud APIs (50 000-char limit) the default of 45 000 provides safe
     * headroom.  For a locally-hosted Ollama model set a much smaller value
     * (e.g. {@code 2000}) via {@code TRANSLATION_API_MAX_CHARS} — Ollama needs
     * far more inference time per token than a cloud API.</p>
     */
    @Min(value = 1, message = "translation.api.max-chars must be >= 1")
    private int maxChars = 45_000;
}
