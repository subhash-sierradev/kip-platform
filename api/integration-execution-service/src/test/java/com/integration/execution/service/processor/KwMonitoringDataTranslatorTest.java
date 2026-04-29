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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * Unit tests for {@link KwMonitoringDataTranslator}.
 * Exercises every guard clause, collection path, and failure mode.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KwMonitoringDataTranslatorTest {
    @Mock
    private TranslationApiClient translationApiClient;
    @Mock
    private TranslationApiProperties translationApiProperties;
    private KwMonitoringDataTranslator translator;
    @BeforeEach
    void setUp() {
        translator = new KwMonitoringDataTranslator(translationApiClient, translationApiProperties);
        when(translationApiProperties.isEnabled()).thenReturn(true);
    }
    // -- Guard clauses - no API call should be made --
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
        assertThat(result.getFirst().getTitle()).isEqualTo("Hello");
        assertThat(result.getFirst().getBody()).isEqualTo("World");
    }
    // -- collectFields - attribute edge cases --
    @Test
    void translate_whenAttributesIsNull_translatesOnlyTitleAndBody() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").body("World").attributes(null).build();
        stubEchoTranslation("ja");
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");
        assertThat(result.getFirst().getTitle()).isEqualTo("[Hello]");
        assertThat(result.getFirst().getBody()).isEqualTo("[World]");
    }
    // -- collectSimpleField - null / blank value skipped --
    @Test
    void translate_whenTitleIsNull_titleNotIncludedInTranslation() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title(null).body("Body text").build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("B o d y - J A"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");
        assertThat(result.getFirst().getTitle()).isNull();
        assertThat(result.getFirst().getBody()).isEqualTo("B o d y - J A");
    }
    @Test
    void translate_whenTitleIsBlank_titleNotIncludedInTranslation() {
        KwMonitoringDocument doc = buildDoc("id1", "   ", "Body text");
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Textinhalt"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        assertThat(result.getFirst().getTitle()).isEqualTo("   ");
        assertThat(result.getFirst().getBody()).isEqualTo("Textinhalt");
    }
    // -- collectDynamicDataFields - edge cases --
    @Test
    void translate_whenDynamicDataIsNotAMap_dynamicDataIsSkipped() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello")
                .attributes(Map.of("dynamicData", "not-a-map"))
                .build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Hallo"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        assertThat(result.getFirst().getTitle()).isEqualTo("Hallo");
        assertThat(result.getFirst().getAttributes().get("dynamicData")).isEqualTo("not-a-map");
    }
    @Test
    void translate_whenDynamicDataValueIsNotString_entryIsSkipped() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Count", 42);
        dynData.put("Label", "Active");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1")
                .attributes(Map.of("dynamicData", dynData))
                .build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Aktiv"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        Map<?, ?> resultDyn = (Map<?, ?>) result.getFirst().getAttributes().get("dynamicData");
        assertThat(resultDyn.get("Label")).isEqualTo("Aktiv");
        assertThat(resultDyn.get("Count")).isEqualTo(42);
    }
    @Test
    void translate_whenDynamicDataValueIsBlankString_entryIsSkipped() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Status", "  ");
        dynData.put("Region", "EMEA");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1")
                .attributes(Map.of("dynamicData", dynData))
                .build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("EMEA-DE"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        Map<?, ?> resultDyn = (Map<?, ?>) result.getFirst().getAttributes().get("dynamicData");
        assertThat(resultDyn.get("Region")).isEqualTo("EMEA-DE");
        assertThat(resultDyn.get("Status")).isEqualTo("  ");
    }
    // -- collectTagFields - tags collection and translation --
    @Test
    void translate_whenTagsIsNotAList_tagsIsSkipped() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello")
                .attributes(Map.of("tags", "not-a-list"))
                .build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Hallo"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        assertThat(result.getFirst().getTitle()).isEqualTo("Hallo");
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
        assertThat(tags.getFirst()).isEqualTo("urgent");
    }
    @Test
    void translate_whenTagsListHasBlankEntry_blankEntryIsSkipped() {
        List<Object> tags = new ArrayList<>(List.of("  ", "real-tag"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tags", tags);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("echtes-Tag"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        List<?> resultTags = (List<?>) result.getFirst().getAttributes().get("tags");
        assertThat(resultTags.get(0)).isEqualTo("  ");
        assertThat(resultTags.get(1)).isEqualTo("echtes-Tag");
    }
    @Test
    void translate_whenTagsListHasNonStringEntry_nonStringEntryIsSkipped() {
        List<Object> tags = new ArrayList<>(Arrays.asList(99, "text-tag"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tags", tags);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Text-Tag"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        List<?> resultTags = (List<?>) result.getFirst().getAttributes().get("tags");
        assertThat(resultTags.get(0)).isEqualTo(99);
        assertThat(resultTags.get(1)).isEqualTo("Text-Tag");
    }
    // -- deepCopy - attribute paths --
    @Test
    void translate_whenDocHasOtherAttributes_otherAttributesAreCopiedAsIs() {
        // Non-special attribute keys (not "dynamicData", "tags", "authors") should be
        // kept as the same object reference in the deep copy.
        Map<String, Object> attrs = new HashMap<>();
        Object metadataValue = Map.of("source", "kw");
        attrs.put("metadata", metadataValue);
        attrs.put("title", "Hello");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").attributes(attrs).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Hallo"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        assertThat(result.getFirst().getAttributes().get("metadata")).isSameAs(metadataValue);
    }

    @Test
    void translate_whenDocHasAuthorsAttribute_authorsAreDeepCopied() {
        // "authors" is now deep-copied (like tags) so displayFullName can be mutated
        // during translation without affecting the original document.
        List<Map<String, Object>> authors = new ArrayList<>();
        authors.add(new HashMap<>(Map.of("displayFullName", "Alice Smith")));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("authors", authors);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        stubEchoTranslation("de");
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        List<?> resultAuthors = (List<?>) result.getFirst().getAttributes().get("authors");
        assertThat(resultAuthors).isNotSameAs(authors);
    }
    @Test
    void translate_whenDocHasTagsAttribute_tagsAreDeepCopied() {
        List<Object> tags = new ArrayList<>(List.of("tag1"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tags", tags);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        stubEchoTranslation("de");
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        List<?> resultTags = (List<?>) result.getFirst().getAttributes().get("tags");
        assertThat(resultTags).isNotSameAs(tags);
    }
    @Test
    void translate_whenDocHasNullAttributes_deepCopyHasNullAttributes() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").attributes(null).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Hallo"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        assertThat(result.getFirst().getAttributes()).isNull();
        assertThat(result.getFirst().getTitle()).isEqualTo("Hallo");
    }
    // -- translateStrings - per-string translation --
    @Test
    void translate_whenApiReturnsEmptyOptional_originalTextKept() {
        KwMonitoringDocument doc = buildDoc("id1", "Hello", null);
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");
        assertThat(result.getFirst().getTitle()).isEqualTo("Hello");
    }
    // -- Happy path - title + body + dynamic data --
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
    // -- translateAll - guard clauses --
    @Test
    void translateAll_whenAllTargetsEqualSourceLanguage_returnsEmptyMap() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Title", "Body"));
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", List.of("en", "EN"));
        assertThat(result).isEmpty();
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }
    @Test
    void translateAll_whenTargetListContainsNullAndBlankEntries_theyAreFilteredOut() {
        List<String> targets = new ArrayList<>();
        targets.add(null);
        targets.add("   ");
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Title", "Body"));
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", targets);
        assertThat(result).isEmpty();
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }
    @Test
    void translateAll_whenDisabled_returnsFallbackForAllTargets() {
        when(translationApiProperties.isEnabled()).thenReturn(false);
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Title", "Body"));
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", List.of("ja", "de"));
        assertThat(result).containsKeys("ja", "de");
        assertThat(result.get("ja")).isSameAs(docs);
        assertThat(result.get("de")).isSameAs(docs);
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }
    @Test
    void translateAll_whenDocumentsAreNull_returnsFallbackForAllTargets() {
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(null, "en", List.of("ja", "de"));
        assertThat(result).containsKeys("ja", "de");
        assertThat(result.get("ja")).isNull();
        assertThat(result.get("de")).isNull();
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }
    @Test
    void translateAll_whenDocumentsAreEmpty_returnsFallbackForAllTargets() {
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(List.of(), "en", List.of("ja", "de"));
        assertThat(result).containsKeys("ja", "de");
        assertThat(result.get("ja")).isEmpty();
        assertThat(result.get("de")).isEmpty();
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }
    @Test
    void translateAll_whenExceptionThrownDuringTranslation_returnsFallbackForAllTargets() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Hello", "World"));
        when(translationApiClient.translateMulti(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("API failure"));
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", List.of("ja", "de"));
        assertThat(result).containsKeys("ja", "de");
        assertThat(result.get("ja")).isSameAs(docs);
        assertThat(result.get("de")).isSameAs(docs);
    }
    @Test
    void translateAll_whenNoTranslatableFieldsExist_returnsFallbackForAllTargets() {
        KwMonitoringDocument emptyDoc = KwMonitoringDocument.builder().id("id1").build();
        List<KwMonitoringDocument> docs = List.of(emptyDoc);
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", List.of("ja", "de"));
        assertThat(result).containsKeys("ja", "de");
        assertThat(result.get("ja")).isSameAs(docs);
        assertThat(result.get("de")).isSameAs(docs);
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }
    // -- translateAll - happy path --
    @Test
    void translateAll_happyPath_returnsTranslatedDocumentsForEachLanguage() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Hello", "World"));
        stubEchoTranslationMulti("ja", "de");
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", List.of("ja", "de"));
        assertThat(result).containsKeys("ja", "de");
        assertThat(result.get("ja").getFirst().getTitle()).isEqualTo("[Hello]");
        assertThat(result.get("de").getFirst().getTitle()).isEqualTo("[Hello]");
        assertThat(docs.getFirst().getTitle()).isEqualTo("Hello");
    }
    // -- translateAll - per-string edge cases --
    @Test
    void translateAll_whenMultiLangApiReturnsEmptyOptional_keepsOriginalForThatLanguage() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Hello", null));
        when(translationApiClient.translateMulti(anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    List<String> langs = inv.getArgument(2);
                    Map<String, Optional<String>> res = new java.util.LinkedHashMap<>();
                    for (String lang : langs) {
                        res.put(lang, Optional.empty());
                    }
                    return res;
                });
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", List.of("ja"));
        assertThat(result.get("ja").getFirst().getTitle()).isEqualTo("Hello");
    }
    @Test
    void translateAll_whenApiReturnsResultForOnlyOneLanguage_otherLanguageKeepsOriginal() {
        List<KwMonitoringDocument> docs = List.of(buildDoc("id1", "Hello", null));
        when(translationApiClient.translateMulti(anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    List<String> langs = inv.getArgument(2);
                    Map<String, Optional<String>> res = new java.util.LinkedHashMap<>();
                    for (String lang : langs) {
                        res.put(lang, "ja".equals(lang)
                                ? Optional.of("Hallo auf Japanisch")
                                : Optional.empty());
                    }
                    return res;
                });
        Map<String, List<KwMonitoringDocument>> result =
                translator.translateAll(docs, "en", List.of("ja", "de"));
        assertThat(result.get("ja").getFirst().getTitle()).isEqualTo("Hallo auf Japanisch");
        assertThat(result.get("de").getFirst().getTitle()).isEqualTo("Hello");
    }
    // -- Helpers --
    private KwMonitoringDocument buildDoc(final String id, final String title, final String body) {
        return KwMonitoringDocument.builder().id(id).title(title).body(body).build();
    }
    /** Stubs single-language translation to wrap each string in brackets. */
    private void stubEchoTranslation(final String targetLang) {
        when(translationApiClient.translate(anyString(), anyString(),
                org.mockito.ArgumentMatchers.eq(targetLang)))
                .thenAnswer(inv -> {
                    String text = inv.getArgument(0);
                    return Optional.of("[" + text + "]");
                });
    }
    /** Stubs multi-language translation to wrap each string in brackets for all languages. */
    private void stubEchoTranslationMulti(final String... langs) {
        when(translationApiClient.translateMulti(anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    String text = inv.getArgument(0);
                    List<String> requestedLangs = inv.getArgument(2);
                    Map<String, Optional<String>> res = new java.util.LinkedHashMap<>();
                    for (String lang : requestedLangs) {
                        res.put(lang, Optional.of("[" + text + "]"));
                    }
                    return res;
                });
    }

    // ── translateLabelMaps ───────────────────────────────────────────────────

    @Test
    void translateLabelMaps_emptyTargetList_returnsEmptyResult() {
        Map<String, String> labels = Map.of("priority", "Priority");

        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(labels, "en", List.of());

        assertThat(result).isEmpty();
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }

    @Test
    void translateLabelMaps_sourceEqualsTarget_filteredOut() {
        Map<String, String> labels = Map.of("priority", "Priority");

        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(labels, "en", List.of("en"));

        assertThat(result).isEmpty();
    }

    @Test
    void translateLabelMaps_translationDisabled_returnsEnglishLabels() {
        when(translationApiProperties.isEnabled()).thenReturn(false);
        Map<String, String> labels = Map.of("priority", "Priority", "summary", "Summary");

        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(labels, "en", List.of("ja"));

        assertThat(result).containsKey("ja");
        assertThat(result.get("ja"))
                .containsEntry("priority", "Priority")
                .containsEntry("summary", "Summary");
    }

    @Test
    void translateLabelMaps_nullLabels_returnsEmptyInnerMaps() {
        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(null, "en", List.of("ja"));

        assertThat(result).containsKey("ja");
        assertThat(result.get("ja")).isEmpty();
    }

    @Test
    void translateLabelMaps_emptyLabels_returnsEmptyInnerMaps() {
        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(Map.of(), "en", List.of("ja"));

        assertThat(result).containsKey("ja");
        assertThat(result.get("ja")).isEmpty();
    }

    @Test
    void translateLabelMaps_successfulTranslation_storesTranslatedValues() {
        when(translationApiClient.translateMulti("Priority", "en", List.of("ja")))
                .thenReturn(Map.of("ja", Optional.of("優先度")));
        when(translationApiClient.translateMulti("Summary", "en", List.of("ja")))
                .thenReturn(Map.of("ja", Optional.of("概要")));

        Map<String, String> labels = new java.util.LinkedHashMap<>();
        labels.put("priority", "Priority");
        labels.put("summary", "Summary");

        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(labels, "en", List.of("ja"));

        assertThat(result.get("ja"))
                .containsEntry("priority", "優先度")
                .containsEntry("summary", "概要");
    }

    @Test
    void translateLabelMaps_apiReturnsEmpty_fallsBackToEnglishValue() {
        when(translationApiClient.translateMulti(anyString(), anyString(), any()))
                .thenReturn(Map.of("ja", Optional.empty()));

        Map<String, String> labels = Map.of("priority", "Priority");

        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(labels, "en", List.of("ja"));

        assertThat(result.get("ja")).containsEntry("priority", "Priority");
    }

    @Test
    void translateLabelMaps_blankLabelValue_storedWithoutApiCall() {
        Map<String, String> labels = new java.util.LinkedHashMap<>();
        labels.put("blank_key", "   ");

        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(labels, "en", List.of("de"));

        assertThat(result.get("de")).containsEntry("blank_key", "   ");
        verify(translationApiClient, never()).translateMulti(anyString(), anyString(), any());
    }

    @Test
    void translateLabelMaps_multipleTargetLanguages_translatesSeparately() {
        when(translationApiClient.translateMulti("Priority", "en", List.of("ja", "de")))
                .thenReturn(Map.of("ja", Optional.of("優先度"), "de", Optional.of("Priorität")));

        Map<String, String> labels = Map.of("priority", "Priority");

        Map<String, Map<String, String>> result =
                translator.translateLabelMaps(labels, "en", List.of("ja", "de"));

        assertThat(result.get("ja")).containsEntry("priority", "優先度");
        assertThat(result.get("de")).containsEntry("priority", "Priorität");
    }

    // ── Non-translatable dynamic keys ────────────────────────────────────────

    @Test
    void translateAll_dynamicDataDateField_isNotSentToTranslationApi() {
        // The "Date" dynamicData key should be skipped; translateMulti must NOT be called with it.
        when(translationApiClient.translateMulti(anyString(), anyString(), any()))
                .thenReturn(Map.of("ja", Optional.of("translated")));

        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "Corp");
        dynData.put("Priority", "HIGH");
        dynData.put("Date", "01 May 2025 00:00 UTC");   // should NOT be translated

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d1").title("Title").body("Body")
                .attributes(Map.of("dynamicData", dynData))
                .build();

        translator.translateAll(List.of(doc), "en", List.of("ja"));

        // translateMulti was called for Title and Body — NOT for the Date field.
        verify(translationApiClient, never()).translateMulti(
                org.mockito.ArgumentMatchers.eq("01 May 2025 00:00 UTC"), anyString(), any());
    }

    @Test
    void translateAll_dynamicDataSummaryField_isSentToTranslationApiAsBodyFallback() {
        // "Summary" is no longer excluded: when body is null, Summary is the rendered
        // body fallback — it must be translated so it appears in the target language,
        // not left in English inside a translated page section.
        when(translationApiClient.translateMulti(anyString(), anyString(), any()))
                .thenReturn(Map.of("ja", Optional.of("translated")));

        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "Corp");
        dynData.put("Priority", "INFO");
        dynData.put("Summary", "Summary content here");  // should be translated now

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d2").title("Title").body(null)
                .attributes(Map.of("dynamicData", dynData))
                .build();

        translator.translateAll(List.of(doc), "en", List.of("ja"));

        // translateMulti must be called with the Summary value so it gets translated.
        verify(translationApiClient).translateMulti(
                org.mockito.ArgumentMatchers.eq("Summary content here"), anyString(), any());
    }

    // -- collectAuthorFields / applyAuthorName --

    @Test
    void translate_whenAuthorsHaveDisplayFullName_authorNamesAreTranslated() {
        Map<String, Object> author1 = new HashMap<>();
        author1.put("displayFullName", "Alice Smith");
        Map<String, Object> author2 = new HashMap<>();
        author2.put("displayFullName", "Bob Jones");
        List<Object> authors = new ArrayList<>(List.of(author1, author2));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("authors", authors);
        KwMonitoringDocument doc = KwMonitoringDocument.builder().id("id1").attributes(attrs).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Translated Name"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "ja");
        List<?> resultAuthors = (List<?>) result.getFirst().getAttributes().get("authors");
        assertThat(((Map<?, ?>) resultAuthors.get(0)).get("displayFullName")).isEqualTo("Translated Name");
        assertThat(((Map<?, ?>) resultAuthors.get(1)).get("displayFullName")).isEqualTo("Translated Name");
    }

    @Test
    void translate_whenAuthorsAttributeIsNotAList_authorsAreSkipped() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("authors", "not-a-list");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").attributes(attrs).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Hallo"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        assertThat(result.getFirst().getAttributes().get("authors")).isEqualTo("not-a-list");
    }

    @Test
    void translate_whenAuthorEntryIsNotAMap_entryIsSkippedInCollection() {
        // An author entry that is a plain String (not a Map) should not be collected
        // for translation and should be preserved as-is in the deep copy.
        List<Object> authors = new ArrayList<>(List.of("plain-string-author"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("authors", authors);
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").attributes(attrs).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Hallo"));
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        List<?> resultAuthors = (List<?>) result.getFirst().getAttributes().get("authors");
        assertThat(resultAuthors.get(0)).isEqualTo("plain-string-author");
    }

    @Test
    void translate_whenAuthorDisplayFullNameIsBlank_authorNameIsNotTranslated() {
        Map<String, Object> author = new HashMap<>();
        author.put("displayFullName", "   ");
        List<Object> authors = new ArrayList<>(List.of(author));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("authors", authors);
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("id1").title("Hello").attributes(attrs).build();
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Hallo"));
        // Only title is translated (1 call); blank displayFullName is not sent to API
        List<KwMonitoringDocument> result = translator.translate(List.of(doc), "en", "de");
        verify(translationApiClient, times(1)).translate(anyString(), anyString(), anyString());
        List<?> resultAuthors = (List<?>) result.getFirst().getAttributes().get("authors");
        assertThat(((Map<?, ?>) resultAuthors.get(0)).get("displayFullName")).isEqualTo("   ");
    }
}
