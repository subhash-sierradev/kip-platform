package com.integration.execution.service.processor;

import com.integration.execution.client.TranslationApiClient;
import com.integration.execution.config.properties.TranslationApiProperties;
import com.integration.execution.model.KwMonitoringDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KwMonitoringDataTranslator}.
 * Exercises every guard clause, collection path, batch edge case, and failure mode.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KwMonitoringDataTranslatorTest {

    private static final Pattern SEG_SPLIT =
            Pattern.compile(Pattern.quote("\n" + KwMonitoringDataTranslator.SEG + "\n"));

    @Mock
    private TranslationApiClient translationApiClient;

    @Mock
    private TranslationApiProperties translationApiProperties;

    private KwMonitoringDataTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new KwMonitoringDataTranslator(translationApiClient, translationApiProperties);
        when(translationApiProperties.isEnabled()).thenReturn(true);
        when(translationApiProperties.getMaxChars()).thenReturn(45_000);
    }

    // ── Guard clauses — no API call should be made ───────────────────────────

    @Test
    void translate_whenDisabled_returnsOriginalWithoutApiCall() {
        when(translationApiProperties.isEnabled()).thenReturn(false);
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Title", "Body"));

        List<KwMonitoringDocument> result = translator.translate(docs, "en", "ja");

        assertThat(result).isSameAs(docs);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenDocumentsIsNull_returnsNull() {
        List<KwMonitoringDocument> result = translator.translate(null, "en", "ja");

        assertThat(result).isNull();
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenDocumentsIsEmpty_returnsEmptyList() {
        List<KwMonitoringDocument> result = translator.translate(List.of(), "en", "ja");

        assertThat(result).isEmpty();
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenTargetLanguageIsNull_returnsOriginal() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Title", "Body"));

        List<KwMonitoringDocument> result = translator.translate(docs, "en", null);

        assertThat(result).isSameAs(docs);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenTargetLanguageIsBlank_returnsOriginal() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Title", "Body"));

        List<KwMonitoringDocument> result = translator.translate(docs, "en", "   ");

        assertThat(result).isSameAs(docs);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenTargetEqualsSourceCaseInsensitive_returnsOriginal() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Title", "Body"));

        List<KwMonitoringDocument> result = translator.translate(docs, "en", "EN");

        assertThat(result).isSameAs(docs);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenNoTranslatableFieldsExist_returnsOriginalWithoutApiCall() {
        // Null title, null body, null attributes → originals list stays empty
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").build();

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result).hasSize(1);
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_whenExceptionThrownDuringTranslation_returnsOriginalDocuments() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Hello", "World"));
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Network failure"));

        List<KwMonitoringDocument> result = translator.translate(docs, "en", "ja");

        // Must not propagate the exception; falls back to original
        assertThat(result.getFirst().getTitle()).isEqualTo("Hello");
        assertThat(result.getFirst().getBody()).isEqualTo("World");
    }

    // ── collectFields — attribute edge cases ─────────────────────────────────

    @Test
    void translate_whenAttributesIsNull_translatesOnlyTitleAndBody() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").body("World").attributes(null).build();
        stubEchoTranslation("ja");

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isEqualTo("[Hello]");
        assertThat(result.getFirst().getBody()).isEqualTo("[World]");
    }

    // ── collectSimpleField — null / blank value skipped ──────────────────────

    @Test
    void translate_whenTitleIsNull_titleNotIncludedInBatch() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title(null).body("Body text").build();
        when(translationApiClient.translate("Body text", "en", "ja"))
                .thenReturn(Optional.of("本文テキスト"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isNull();  // unchanged
        assertThat(result.getFirst().getBody()).isEqualTo("本文テキスト");
    }

    @Test
    void translate_whenTitleIsBlank_titleNotIncludedInBatch() {
        KwMonitoringDocument doc = buildDoc("id1", "   ", "Body text");
        when(translationApiClient.translate("Body text", "en", "de"))
                .thenReturn(Optional.of("Textinhalt"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");

        assertThat(result.getFirst().getTitle()).isEqualTo("   ");  // blank preserved
        assertThat(result.getFirst().getBody()).isEqualTo("Textinhalt");
    }

    // ── collectDynamicDataFields — edge cases ────────────────────────────────

    @Test
    void translate_whenDynamicDataIsNotAMap_dynamicDataIsSkipped() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello")
                .attributes(Map.of("dynamicData", "not-a-map"))
                .build();
        when(translationApiClient.translate("Hello", "en", "ja"))
                .thenReturn(Optional.of("こんにちは"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isEqualTo("こんにちは");
        // dynamicData preserved as-is since it was not a Map
        assertThat(result.getFirst().getAttributes().get("dynamicData")).isEqualTo("not-a-map");
    }

    @Test
    void translate_whenDynamicDataValueIsNotString_entryIsSkipped() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Count", 42);       // Integer — must be skipped
        dynData.put("Label", "Active"); // String — must be translated
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1")
                .attributes(Map.of("dynamicData", dynData))
                .build();
        when(translationApiClient.translate("Active", "en", "ja"))
                .thenReturn(Optional.of("アクティブ"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        Map<?, ?> resultDyn = (Map<?, ?>) result.getFirst().getAttributes().get("dynamicData");
        assertThat(resultDyn.get("Label")).isEqualTo("アクティブ");
        assertThat(resultDyn.get("Count")).isEqualTo(42); // integer unchanged
    }

    @Test
    void translate_whenDynamicDataValueIsBlankString_entryIsSkipped() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Status", "  ");    // blank — must be skipped
        dynData.put("Region", "EMEA");  // non-blank — translated
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1")
                .attributes(Map.of("dynamicData", dynData))
                .build();
        when(translationApiClient.translate("EMEA", "en", "de"))
                .thenReturn(Optional.of("EMEA-DE"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");

        Map<?, ?> resultDyn = (Map<?, ?>) result.getFirst().getAttributes().get("dynamicData");
        assertThat(resultDyn.get("Region")).isEqualTo("EMEA-DE");
        assertThat(resultDyn.get("Status")).isEqualTo("  "); // blank unchanged
    }

    // ── collectTagFields — tags collection and translation ───────────────────

    @Test
    void translate_whenTagsIsNotAList_tagsIsSkipped() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello")
                .attributes(Map.of("tags", "not-a-list"))
                .build();
        when(translationApiClient.translate("Hello", "en", "ja"))
                .thenReturn(Optional.of("こんにちは"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isEqualTo("こんにちは");
        assertThat(result.getFirst().getAttributes().get("tags")).isEqualTo("not-a-list");
    }

    @Test
    void translate_whenTagsListHasStrings_tagsAreTranslated() {
        List<Object> tags = new ArrayList<>(List.of("urgent", "monitoring"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tags", tags);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        stubEchoTranslation("de");

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");

        List<?> resultTags = (List<?>) result.getFirst().getAttributes().get("tags");
        assertThat(resultTags).hasSize(2);
        assertThat(resultTags.get(0)).asString().contains("urgent");
        assertThat(resultTags.get(1)).asString().contains("monitoring");
        // Original list is not mutated
        assertThat(tags.get(0)).isEqualTo("urgent");
    }

    @Test
    void translate_whenTagsListHasBlankEntry_blankEntryIsSkipped() {
        List<Object> tags = new ArrayList<>(List.of("  ", "real-tag"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tags", tags);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        when(translationApiClient.translate("real-tag", "en", "ja"))
                .thenReturn(Optional.of("リアルタグ"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        List<?> resultTags = (List<?>) result.getFirst().getAttributes().get("tags");
        assertThat(resultTags.get(0)).isEqualTo("  ");       // blank unchanged
        assertThat(resultTags.get(1)).isEqualTo("リアルタグ"); // translated
    }

    @Test
    void translate_whenTagsListHasNonStringEntry_nonStringEntryIsSkipped() {
        List<Object> tags = new ArrayList<>(Arrays.asList(99, "text-tag")); // Integer + String
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tags", tags);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        when(translationApiClient.translate("text-tag", "en", "ja"))
                .thenReturn(Optional.of("テキストタグ"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        List<?> resultTags = (List<?>) result.getFirst().getAttributes().get("tags");
        assertThat(resultTags.get(0)).isEqualTo(99);           // integer unchanged
        assertThat(resultTags.get(1)).isEqualTo("テキストタグ"); // translated
    }

    // ── deepCopy — attribute paths ───────────────────────────────────────────

    @Test
    void translate_whenDocHasOtherAttributes_otherAttributesAreCopiedAsIs() {
        // "authors" is not dynamicData/tags → hits the else branch in deepCopy
        Map<String, Object> attrs = new HashMap<>();
        Object authorsValue = List.of("Alice", "Bob");
        attrs.put("authors", authorsValue);
        attrs.put("title", "Hello");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").attributes(attrs).build();
        when(translationApiClient.translate("Hello", "en", "ja"))
                .thenReturn(Optional.of("こんにちは"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        // "authors" preserved by reference (not deep-copied)
        assertThat(result.getFirst().getAttributes().get("authors")).isSameAs(authorsValue);
    }

    @Test
    void translate_whenDocHasTagsAttribute_tagsAreDeepCopied() {
        // Covers "tags".equals(key) && val instanceof List<?> branch in deepCopy
        List<Object> tags = new ArrayList<>(List.of("tag1"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tags", tags);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        stubEchoTranslation("de");

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");

        List<?> resultTags = (List<?>) result.getFirst().getAttributes().get("tags");
        assertThat(resultTags).isNotSameAs(tags); // deep-copied, not same reference
    }

    @Test
    void translate_whenDocHasNullAttributes_deepCopyHasNullAttributes() {
        // Covers `if (src.getAttributes() != null)` false branch in deepCopy
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").attributes(null).build();
        when(translationApiClient.translate("Hello", "en", "ja"))
                .thenReturn(Optional.of("こんにちは"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getAttributes()).isNull();
        assertThat(result.getFirst().getTitle()).isEqualTo("こんにちは");
    }

    // ── translateInBatches — batching edge cases ─────────────────────────────

    @Test
    void translate_whenApiReturnsEmptyOptional_originalTextKeptForBatch() {
        KwMonitoringDocument doc = buildDoc("id1", "Hello", null);
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isEqualTo("Hello"); // fallback
    }

    @Test
    void translate_whenApiReturnsWrongSegmentCount_originalTextKeptForBatch() {
        KwMonitoringDocument doc = buildDoc("id1", "Hello", "World");
        // Two segments sent → only one returned → mismatch → keep originals
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("こんにちは")); // missing second segment

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isEqualTo("Hello");
        assertThat(result.getFirst().getBody()).isEqualTo("World");
    }

    @Test
    void translate_whenBatchExceedsMaxChars_stringsAreSplitAcrossMultipleBatches() {
        // Set maxChars very small so each string forms its own batch
        when(translationApiProperties.getMaxChars()).thenReturn(5);

        KwMonitoringDocument doc = buildDoc("id1", "Hello", "World");
        // "Hello" alone = 5 chars (fits), then "World" must be a new batch
        when(translationApiClient.translate("Hello", "en", "ja")).thenReturn(Optional.of("A"));
        when(translationApiClient.translate("World", "en", "ja")).thenReturn(Optional.of("B"));

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isEqualTo("A");
        assertThat(result.getFirst().getBody()).isEqualTo("B");
    }

    // ── Happy path — title + body + dynamic data ─────────────────────────────

    @Test
    void translate_titleBodyAndDynamicDataValues_allTranslated() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Status", "Active");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("dynamicData", dynData);
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").body("World").attributes(attrs).build();
        stubEchoTranslation("ja");

        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");

        assertThat(result.getFirst().getTitle()).isEqualTo("[Hello]");
        assertThat(result.getFirst().getBody()).isEqualTo("[World]");
        Map<?, ?> resultDyn = (Map<?, ?>) result.getFirst().getAttributes().get("dynamicData");
        assertThat(resultDyn.get("Status")).isEqualTo("[Active]");
        // Original doc must NOT be mutated
        assertThat(doc.getTitle()).isEqualTo("Hello");
    }

    @Test
    void translate_multipleDocuments_eachDocumentTranslatedIndependently() {
        KwMonitoringDocument doc1 = buildDoc("id1", "First", null);
        KwMonitoringDocument doc2 = buildDoc("id2", "Second", "Body2");
        stubEchoTranslation("de");

        List<KwMonitoringDocument> result = translator.translate(List.of(doc1, doc2), "en", "de");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("[First]");
        assertThat(result.get(1).getTitle()).isEqualTo("[Second]");
        assertThat(result.get(1).getBody()).isEqualTo("[Body2]");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private KwMonitoringDocument buildDoc(final String id, final String title, final String body) {
        return KwMonitoringDocument.builder().id(id).title(title).body(body).build();
    }

    /**
     * Stubs the translation client to wrap each segment in brackets {@code [segment]},
     * so assertions can verify the translated value came from the translator without
     * hard-coding exact text.
     */
    private void stubEchoTranslation(final String targetLang) {
        when(translationApiClient.translate(anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq(targetLang)))
                .thenAnswer(inv -> {
                    String batch = inv.getArgument(0);
                    String[] segments = SEG_SPLIT.split(batch, -1);
                    String joined = Arrays.stream(segments)
                            .map(s -> "[" + s + "]")
                            .collect(Collectors.joining("\n" + KwMonitoringDataTranslator.SEG + "\n"));
                    return Optional.of(joined);
                });
    }
}

