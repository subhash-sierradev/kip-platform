package com.integration.translation.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Top-level response envelope returned by {@code POST /api/translate}.
 *
 * <p>The response structure mirrors the Azure Cognitive Services multi-modal
 * analysis envelope so that callers do not need to change their parsing logic
 * when the implementation is upgraded to a cloud provider.  Fields that are
 * only relevant to vision, speech, or NLP operations are set to {@code null}
 * by the Ollama implementation.</p>
 *
 * <p>Example JSON:</p>
 * <pre>{@code
 * {
 *   "cognitiveServicesUsage": { "translatorTranslateTextCharacterCount": 44, ... },
 *   "imageAnalysisResults": null,
 *   "translationResults": [
 *     { "translatedTimestamp": 1776694594, "languageCode": "ja",
 *       "value": "こんにちは、世界。この文書を翻訳してください。" }
 *   ],
 *   "recognizedText": null,
 *   "summarizedText": null,
 *   "tenantId": null,
 *   "attachmentId": null,
 *   "extractOnDisk": false
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {

    /**
     * Aggregate usage counters for billing / observability.
     * Populated by the translation implementation; all zero except
     * {@code translatorTranslateTextCharacterCount} for the Ollama backend.
     */
    @JsonProperty("cognitiveServicesUsage")
    private CognitiveServicesUsage cognitiveServicesUsage;

    /**
     * Reserved for future vision pipeline results.  Always {@code null} for the
     * translation endpoint.
     */
    @JsonProperty("imageAnalysisResults")
    private Object imageAnalysisResults;

    /**
     * One {@link TranslationResult} per requested target language code.
     */
    @JsonProperty("translationResults")
    private List<TranslationResult> translationResults;

    /**
     * Reserved for OCR / speech-to-text output.  Always {@code null} here.
     */
    @JsonProperty("recognizedText")
    private String recognizedText;

    /**
     * Reserved for abstractive summarisation output.  Always {@code null} here.
     */
    @JsonProperty("summarizedText")
    private String summarizedText;

    /**
     * Tenant identifier for multi-tenant deployments.  {@code null} in dev.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Attachment / document identifier from the caller's system. {@code null} in dev.
     */
    @JsonProperty("attachmentId")
    private String attachmentId;

    /**
     * Indicates whether the source artefact was extracted to a temporary disk
     * location during processing.  Always {@code false} for text-only requests.
     */
    @JsonProperty("extractOnDisk")
    private boolean extractOnDisk;
}

