package com.integration.translation.client;

import com.integration.translation.client.dto.OllamaGenerateRequest;
import com.integration.translation.client.dto.OllamaGenerateResponse;
import com.integration.translation.config.properties.OllamaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Low-level HTTP client for the <a href="https://ollama.com">Ollama</a> REST API.
 *
 * <p>This component is responsible only for transport — building the HTTP request,
 * sending it, and returning the raw Ollama response object. All business logic
 * (prompt construction, fallback handling, result assembly) lives in the service
 * layer above it.</p>
 *
 * <h3>Endpoint used</h3>
 * <pre>POST /api/generate</pre>
 *
 * <h3>Error handling</h3>
 * <p>Any {@link RestClientException} (connection refused, timeout, non-2xx
 * status) is caught here, logged at WARN level, and re-thrown as an unchecked
 * {@link OllamaClientException} so the service layer can apply its fallback
 * strategy without dealing with Spring's exception hierarchy.</p>
 *
 * <h3>UTF-8 safety</h3>
 * <p>Request and response headers explicitly declare {@code charset=UTF-8} to
 * guarantee correct handling of Japanese, Russian, Arabic, and other multi-byte
 * scripts.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaClient {

    /** Path to the Ollama text-generation endpoint. */
    private static final String GENERATE_PATH = "/api/generate";

    /** Path used for the lightweight availability ping. */
    private static final String HEALTH_PATH = "/";

    private final RestTemplate restTemplate;
    private final OllamaProperties ollamaProperties;

    /**
     * Sends a generation request to Ollama and returns the raw response.
     *
     * <p>The request is always built with {@code stream=false} so the entire
     * generated text arrives in one JSON payload.</p>
     *
     * @param prompt the full prompt to send to the model
     * @return Ollama's {@link OllamaGenerateResponse}; never {@code null}
     * @throws OllamaClientException if the HTTP call fails for any reason
     */
    public OllamaGenerateResponse generate(final String prompt) {
        String url = ollamaProperties.getBaseUrl() + GENERATE_PATH;

        OllamaGenerateRequest request = OllamaGenerateRequest.builder()
                .model(ollamaProperties.getModel())
                .prompt(prompt)
                .stream(false)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

        HttpEntity<OllamaGenerateRequest> entity = new HttpEntity<>(request, headers);

        log.debug("Calling Ollama generate: url={}, model={}, promptLength={}",
                url, ollamaProperties.getModel(), prompt.length());

        try {
            ResponseEntity<OllamaGenerateResponse> response =
                    restTemplate.postForEntity(url, entity, OllamaGenerateResponse.class);

            OllamaGenerateResponse body = response.getBody();
            if (body == null) {
                throw new OllamaClientException("Ollama returned an empty response body");
            }

            log.debug("Ollama generate succeeded: model={}, done={}",
                    body.getModel(), body.isDone());
            return body;

        } catch (RestClientException ex) {
            log.warn("Ollama generate request failed [url={}]: {}", url, ex.getMessage());
            throw new OllamaClientException(
                    "Ollama request failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Pings the Ollama server to verify it is reachable.
     *
     * <p>A successful ping returns {@code true}; any I/O or HTTP error returns
     * {@code false}. This method never throws.</p>
     *
     * @return {@code true} if Ollama responded with HTTP 2xx
     */
    public boolean isReachable() {
        String url = ollamaProperties.getBaseUrl() + HEALTH_PATH;
        try {
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception ex) {
            log.debug("Ollama health ping failed [url={}]: {}", url, ex.getMessage());
            return false;
        }
    }

    /**
     * Unchecked exception thrown when Ollama communication fails.
     */
    public static final class OllamaClientException extends RuntimeException {

        /**
         * Constructs an exception with the supplied detail message.
         *
         * @param message human-readable failure description
         */
        public OllamaClientException(final String message) {
            super(message);
        }

        /**
         * Constructs an exception with the supplied detail message and root cause.
         *
         * @param message human-readable failure description
         * @param cause   original exception that triggered this failure
         */
        public OllamaClientException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}

