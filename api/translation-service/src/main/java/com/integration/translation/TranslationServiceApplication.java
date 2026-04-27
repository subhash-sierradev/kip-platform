package com.integration.translation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Translation Service.
 *
 * <p>This Spring Boot application exposes a single REST endpoint
 * ({@code POST /api/translate}) that delegates to a locally running
 * <a href="https://ollama.com">Ollama</a> LLM to produce translations.
 * It is intended for <em>development and testing only</em> — in production
 * environments this bean should be replaced with a certified cloud translation
 * provider (e.g. Azure AI Translator, DeepL, Google Translate).</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * # Via Docker Compose (recommended for dev)
 * docker compose -f docker-compose.dev.yml up
 *
 * # Direct Gradle run (Ollama must be reachable at localhost:11434)
 * ./gradlew :translation-service:bootRun
 * }</pre>
 *
 * @see com.integration.translation.controller.TranslationController
 * @see com.integration.translation.service.TranslationService
 */
@EnableCaching
@SpringBootApplication
public class TranslationServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments forwarded to Spring Boot
     */
    public static void main(final String[] args) {
        SpringApplication.run(TranslationServiceApplication.class, args);
    }
}

