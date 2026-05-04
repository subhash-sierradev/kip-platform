package com.integration.translation.service;

import com.integration.translation.model.request.TranslationRequest;
import com.integration.translation.model.response.TranslationResponse;

/**
 * Contract for text translation services.
 *
 * <p>This interface is the single extension point for swapping the translation
 * backend.  The current implementation is {@link OllamaTranslationService}
 * which delegates to a locally hosted Ollama LLM.  Future implementations can
 * provide cloud-backed translation (e.g. Azure AI Translator, DeepL, Google
 * Translate) simply by creating a new {@code @Service} class that implements
 * this interface and activating it via Spring profiles or conditional beans —
 * no changes to the controller or model layer are required.</p>
 *
 * <h3>Contract guarantees</h3>
 * <ul>
 *   <li>Implementations must never return {@code null}.</li>
 *   <li>A {@link TranslationResponse} must contain exactly one
 *       {@link com.integration.translation.model.response.TranslationResult} per
 *       element in {@link TranslationRequest#getLanguageCodes()}.</li>
 *   <li>If translation fails for a given language code, the implementation
 *       <em>should</em> fall back to the original source text rather than
 *       propagating an exception, unless the failure is unrecoverable.</li>
 *   <li>Implementations must preserve UTF-8 encoding throughout.</li>
 * </ul>
 *
 * @see OllamaTranslationService
 */
public interface TranslationService {

    /**
     * Translates the text in the request into each of the requested target languages.
     *
     * @param request the validated translation request containing source text,
     *                source language, and one or more target language codes
     * @return a {@link TranslationResponse} containing one result per target language
     */
    TranslationResponse translate(TranslationRequest request);
}

