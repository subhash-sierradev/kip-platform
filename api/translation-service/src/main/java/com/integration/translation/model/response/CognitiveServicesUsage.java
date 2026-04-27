package com.integration.translation.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks usage counters across all cognitive service operations performed during
 * a single request.
 *
 * <p>This structure mirrors the Azure Cognitive Services usage envelope so that
 * future implementations can report real billing metrics without breaking the
 * API contract.  For the local Ollama implementation, all vision, speech, and
 * entity fields remain {@code 0}; only {@link #translatorTranslateTextCharacterCount}
 * is populated (set to the character length of the source text &times; the number
 * of target languages).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitiveServicesUsage {

    /** Number of image type-detection transactions (unused). */
    @JsonProperty("visionImageTypeTransactionCount")
    private int visionImageTypeTransactionCount;

    /** Number of image description transactions (unused). */
    @JsonProperty("visionImageDescTransactionCount")
    private int visionImageDescTransactionCount;

    /** Number of image tagging transactions (unused). */
    @JsonProperty("visionImageTagTransactionCount")
    private int visionImageTagTransactionCount;

    /** Number of image object-detection transactions (unused). */
    @JsonProperty("visionImageObjectTransactionCount")
    private int visionImageObjectTransactionCount;

    /** Number of image OCR transactions (unused). */
    @JsonProperty("visionImageOcrTransactionCount")
    private int visionImageOcrTransactionCount;

    /** Number of PDF pages read (unused). */
    @JsonProperty("visionPdfReadPageCount")
    private int visionPdfReadPageCount;

    /** Number of entity-recognition records processed (unused). */
    @JsonProperty("languageEntityRecognitionRecordCount")
    private int languageEntityRecognitionRecordCount;

    /** Number of summarization records processed (unused). */
    @JsonProperty("languageSummarizationRecordCount")
    private int languageSummarizationRecordCount;

    /** Number of speech-to-text seconds consumed (unused). */
    @JsonProperty("speechSpeechToTextSecondCount")
    private int speechSpeechToTextSecondCount;

    /** Number of characters submitted for language detection (unused for Ollama). */
    @JsonProperty("translatorLangDetectCharacterCount")
    private int translatorLangDetectCharacterCount;

    /**
     * Total characters submitted for translation.
     * Set to {@code len(textToTranslate) * numberOfTargetLanguages} by the
     * Ollama implementation to give callers a rough cost estimate.
     */
    @JsonProperty("translatorTranslateTextCharacterCount")
    private int translatorTranslateTextCharacterCount;

    /**
     * Factory method that builds a usage object populated with only the
     * translation character count (all other counters are zero).
     *
     * @param charCount total characters translated
     * @return populated {@code CognitiveServicesUsage}
     */
    public static CognitiveServicesUsage ofTranslation(final int charCount) {
        return CognitiveServicesUsage.builder()
                .translatorTranslateTextCharacterCount(charCount)
                .build();
    }
}

