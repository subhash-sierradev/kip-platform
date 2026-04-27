/**
 * Translation Service — root package.
 *
 * <p>This module provides a temporary, development-only translation API backed by a locally
 * hosted <a href="https://ollama.com">Ollama</a> LLM runtime. The API surface is intentionally
 * shaped to mirror the Azure Cognitive Services translation contract so that the implementation
 * can be swapped for a cloud provider in the future with minimal changes to callers.</p>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@code config}   — Spring {@code @Configuration} beans, cache setup, properties.</li>
 *   <li>{@code client}   — Low-level Ollama HTTP client and its request/response DTOs.</li>
 *   <li>{@code service}  — {@link com.integration.translation.service.TranslationService}
 *                          interface + Ollama-based implementation.</li>
 *   <li>{@code controller} — REST controller exposing {@code POST /api/translate}.</li>
 *   <li>{@code health}   — Spring Boot {@code HealthIndicator} for the Ollama sidecar.</li>
 *   <li>{@code model}    — Public API request/response model classes.</li>
 *   <li>{@code exception} — Domain exceptions and a global exception handler.</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li>Interface-based: swap the Ollama implementation for any cloud provider by providing
 *       an alternative {@link com.integration.translation.service.TranslationService} bean.</li>
 *   <li>UTF-8 everywhere: all HTTP traffic and file I/O is explicitly UTF-8 encoded.</li>
 *   <li>Fallback safety: if Ollama is unavailable, the original text is returned with a
 *       warning rather than propagating a 5xx to the caller.</li>
 * </ul>
 */
package com.integration.translation;

