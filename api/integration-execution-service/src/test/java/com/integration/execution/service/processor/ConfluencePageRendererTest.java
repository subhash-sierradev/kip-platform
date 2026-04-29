package com.integration.execution.service.processor;

import com.integration.execution.config.AppConfig;
import com.integration.execution.model.KwMonitoringDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfluencePageRendererTest {

    private ConfluencePageRenderer renderer;
    private KwMonitoringDataTranslator mockTranslator;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        mockTranslator = mock(KwMonitoringDataTranslator.class);
        // translateLabelMaps returns an empty map → renderer falls back to English via the ! operator.
        when(mockTranslator.translateLabelMaps(any(), anyString(), any())).thenReturn(Map.of());
        renderer = new ConfluencePageRenderer(config.freemarkerConfiguration(), mockTranslator);
    }

    @Test
    void buildPageContent_singleDocument_returnsNonEmptyHtml() {
        List<KwMonitoringDocument> docs = List.of(
                docWithClient("ACME", "HIGH", "Report Title"));

        String html = renderer.buildPageContent(docs, ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("ACME");
    }

    @Test
    void buildPageContent_emptyDocumentList_rendersWithZeroCounts() {
        String html = renderer.buildPageContent(List.of(), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_multiplePriorities_rendersAllGroups() {
        List<KwMonitoringDocument> docs = List.of(
                docWithClient("ClientA", "CRITICAL", "C-1"),
                docWithClient("ClientB", "LOW", "L-1"),
                docWithClient("ClientA", "HIGH", "H-1"),
                docWithClient("ClientA", "MEDIUM", "M-1"));

        String html = renderer.buildPageContent(docs, ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("ClientA");
        assertThat(html).contains("ClientB");
    }

    @Test
    void buildPageContent_documentWithNullPriority_defaultsToInfo() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("doc-null-priority")
                .title("No Priority Doc")
                .attributes(Map.of("dynamicData", new HashMap<>()))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_documentWithBlankPriority_defaultsToInfo() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Priority", "  ");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("doc-blank-priority")
                .title("Blank Priority")
                .attributes(Map.of("dynamicData", dynData))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_documentWithNullAttributes_usesUnknownClient() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("doc-no-attrs")
                .title("No Attributes")
                .attributes(null)
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("Unknown");
    }

    @Test
    void buildPageContent_documentWithAuthors_includesAuthorData() {
        List<Map<String, Object>> authors = List.of(
                Map.of("displayFullName", "Alice Smith", "id", "u1"),
                Map.of("displayFullName", "  ", "id", "u2"));  // blank name filtered
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("dynamicData", Map.of("Client", "BigCorp", "Priority", "LOW"));
        attrs.put("authors", authors);

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d1")
                .title("T1")
                .attributes(attrs)
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("Europe/London"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_documentWithSerials_includesSerialData() {
        List<Map<String, Object>> serials = List.of(
                Map.of("id", "s1", "filingNumberDisplay", "SN-001"));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("dynamicData", Map.of("Client", "Corp", "Priority", "MEDIUM"));
        attrs.put("serials", serials);

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d2").title("T2").attributes(attrs).build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_documentWithTags_includesTagData() {
        List<String> tags = List.of("tag1", "tag2");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("dynamicData", Map.of("Client", "Corp", "Priority", "INFO"));
        attrs.put("tags", tags);

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d3").title("T3").attributes(attrs).build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_dynamicFieldWithMapValue_rendersCorrectly() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "CustomCorp");
        dynData.put("Priority", "HIGH");
        dynData.put("Region", Map.of("Region/Country", "United States"));

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d4").title("Region Doc")
                .attributes(Map.of("dynamicData", dynData))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_documentWithNullTitle_usesEmptyString() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d5")
                .title(null)
                .attributes(Map.of("dynamicData", Map.of("Client", "Corp")))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_documentWithNullBody_usesEmptyString() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d6")
                .title("T")
                .body(null)
                .attributes(Map.of("dynamicData", Map.of("Client", "Corp")))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_clientWithBlankName_groupedUnderUnknown() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "  ");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d7").title("Blank Client")
                .attributes(Map.of("dynamicData", dynData))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("Unknown");
    }

    @Test
    void buildPageContent_invalidFreemarkerConfig_throwsRuntimeException() {
        freemarker.template.Configuration brokenCfg = mock(freemarker.template.Configuration.class);
        try {
            when(brokenCfg.getTemplate("monitoring_data_report.ftl"))
                    .thenThrow(new java.io.IOException("template missing"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ConfluencePageRenderer brokenRenderer =
                new ConfluencePageRenderer(brokenCfg, mock(KwMonitoringDataTranslator.class));

        assertThatThrownBy(() -> brokenRenderer.buildPageContent(List.of(), ZoneId.of("UTC")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to render monitoring data page template");
    }

    @Test
    void buildPageContent_documentWithNumericPriorityDefaultValue_rendersCorrectly() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "TestCorp");
        dynData.put("Priority", 42);  // non-String priority exercises resolveStringValue default branch
        dynData.put("ExtraField", "SomeValue");

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d8").title("Numeric Priority")
                .attributes(Map.of("dynamicData", dynData))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_multipleClientsAlphabeticallySorted() {
        List<KwMonitoringDocument> docs = List.of(
                docWithClient("Zeta Corp", "LOW", "Z"),
                docWithClient("Alpha Inc", "HIGH", "A"),
                docWithClient("Beta Ltd", "MEDIUM", "B"));

        String html = renderer.buildPageContent(docs, ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        // Alpha should appear before Beta, Beta before Zeta (TreeMap sorts alphabetically)
        int alphaIdx = html.indexOf("Alpha");
        int betaIdx = html.indexOf("Beta");
        int zetaIdx = html.indexOf("Zeta");
        assertThat(alphaIdx).isLessThan(betaIdx);
        assertThat(betaIdx).isLessThan(zetaIdx);
    }

    @Test
    void buildPageContent_withNonAmericaTimezone_rendersCorrectly() {
        List<KwMonitoringDocument> docs = List.of(docWithClient("Corp", "HIGH", "T"));

        String html = renderer.buildPageContent(docs, ZoneId.of("Asia/Tokyo"));

        assertThat(html).isNotBlank();
    }

    @Test
    void buildPageContent_differentTimezones_produceDifferentTimestampOutput() {
        List<KwMonitoringDocument> docs = List.of(docWithClient("Corp", "HIGH", "T"));

        String htmlUtc = renderer.buildPageContent(docs, ZoneId.of("UTC"));
        String htmlNewYork = renderer.buildPageContent(docs, ZoneId.of("America/New_York"));

        assertThat(htmlUtc).isNotBlank();
        assertThat(htmlNewYork).isNotBlank();
        assertThat(htmlUtc).isNotEqualTo(htmlNewYork);
    }

    @Test
    void buildMultiLanguagePageContent_withJapaneseTranslation_rendersBothLanguageSections() {
        List<KwMonitoringDocument> sourceDocs = List.of(docWithClient("ACME", "HIGH", "English Title"));

        Map<String, Object> jaDynData = new HashMap<>();
        jaDynData.put("Client", "ACME");
        jaDynData.put("Priority", "HIGH");
        List<KwMonitoringDocument> jaDocs = List.of(
                KwMonitoringDocument.builder()
                        .id("ja-doc").title("日本語タイトル").body("日本語本文")
                        .attributes(Map.of("dynamicData", jaDynData))
                        .build());

        String html = renderer.buildMultiLanguagePageContent(
                sourceDocs, Map.of("ja", jaDocs), "en", ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("English");         // English section heading
        assertThat(html).contains("日本語");            // Japanese section heading
        assertThat(html).contains("日本語タイトル");      // Japanese record title
    }

    @Test
    void buildMultiLanguagePageContent_noTranslations_rendersSingleEnglishSection() {
        List<KwMonitoringDocument> sourceDocs = List.of(docWithClient("Corp", "LOW", "Solo Title"));

        String html = renderer.buildMultiLanguagePageContent(
                sourceDocs, Map.of(), "en", ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("English");
        assertThat(html).contains("Solo Title");
    }

    @Test
    void buildMultiLanguagePageContent_multipleTargetLanguages_rendersAllSections() {
        List<KwMonitoringDocument> sourceDocs = List.of(docWithClient("Corp", "HIGH", "Report"));
        List<KwMonitoringDocument> deDocs = List.of(docWithClient("Corp", "HIGH", "Bericht"));
        List<KwMonitoringDocument> ruDocs = List.of(docWithClient("Corp", "HIGH", "Отчёт"));

        Map<String, List<KwMonitoringDocument>> translatedByLang = new java.util.LinkedHashMap<>();
        translatedByLang.put("de", deDocs);
        translatedByLang.put("ru", ruDocs);

        String html = renderer.buildMultiLanguagePageContent(
                sourceDocs, translatedByLang, "en", ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("English");    // en section
        assertThat(html).contains("Deutsch");    // de section
        assertThat(html).contains("Русский");    // ru section
        assertThat(html).contains("Bericht");    // German record content
        assertThat(html).contains("Отчёт");     // Russian record content
    }

    @ParameterizedTest(name = "langCode={0}")
    @ValueSource(strings = {"fr", "es", "zh", "zh-cn", "zh-tw", "ko", "hi", "jp", "UNKNOWN_LANG"})
    void buildMultiLanguagePageContent_variousLanguageCodes_rendersWithoutError(String lang) {
        List<KwMonitoringDocument> source = List.of(docWithClient("Corp", "HIGH", "T"));
        List<KwMonitoringDocument> translated = List.of(docWithClient("Corp", "HIGH", "T-" + lang));

        String html = renderer.buildMultiLanguagePageContent(
                source, Map.of(lang, translated), "en", ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
    }

    // ── Summary field resolution ─────────────────────────────────────────────

    @Test
    void buildPageContent_blankBodyWithDynamicSummary_usesDynamicSummaryText() {
        // Document has no body but dynamicData["Summary"] has content.
        // The renderer should fold it into the Summary row and NOT show it again as a dynamic field.
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "Corp");
        dynData.put("Priority", "INFO");
        dynData.put("Summary", "Dynamic summary content");

        List<KwMonitoringDocument> docs = List.of(
                KwMonitoringDocument.builder()
                        .id("doc-summary")
                        .title("Summary Test")
                        .body(null)              // blank body — should fall back to dynamicData
                        .createdTimestamp(1700000000L)
                        .updatedTimestamp(1700001000L)
                        .attributes(Map.of("dynamicData", dynData))
                        .build());

        String html = renderer.buildPageContent(docs, ZoneId.of("UTC"));

        assertThat(html).contains("Dynamic summary content");
        // "Dynamic summary content" should appear ONCE (as body), not twice as a dynamic field too.
        int occurrences = html.split("Dynamic summary content", -1).length - 1;
        assertThat(occurrences).isEqualTo(1);
    }

    @Test
    void buildPageContent_blankBodyNoSummaryField_rendersEmptySummaryRow() {
        // Document has neither body nor dynamicData["Summary"]: Summary row shows dash.
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "Corp");
        dynData.put("Priority", "INFO");

        List<KwMonitoringDocument> docs = List.of(
                KwMonitoringDocument.builder()
                        .id("doc-nosummary")
                        .title("No Summary")
                        .body("")
                        .createdTimestamp(1700000000L)
                        .updatedTimestamp(1700001000L)
                        .attributes(Map.of("dynamicData", dynData))
                        .build());

        String html = renderer.buildPageContent(docs, ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        // Template renders "-" for empty values — page should still be valid
        assertThat(html).contains("No Summary");
    }

    @Test
    void buildMultiLanguagePageContent_localeBasedTimestamps_differentMonthNamesPerLanguage() {
        // English section uses English month names; Japanese section uses Japanese month names.
        long may2025 = 1746057600L; // 2025-05-01 00:00:00 UTC
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "Corp");
        dynData.put("Priority", "INFO");

        List<KwMonitoringDocument> source = List.of(
                KwMonitoringDocument.builder()
                        .id("ts-en").title("TS EN").body("body")
                        .createdTimestamp(may2025).updatedTimestamp(may2025)
                        .attributes(Map.of("dynamicData", dynData))
                        .build());
        Map<String, Object> jaDynData = new HashMap<>(dynData);
        List<KwMonitoringDocument> jaDocs = List.of(
                KwMonitoringDocument.builder()
                        .id("ts-ja").title("TS JA").body("body-ja")
                        .createdTimestamp(may2025).updatedTimestamp(may2025)
                        .attributes(Map.of("dynamicData", jaDynData))
                        .build());

        String html = renderer.buildMultiLanguagePageContent(
                source, Map.of("ja", jaDocs), "en", ZoneId.of("UTC"));

        // English section contains English month name ("May")
        assertThat(html).contains("May");
        // Japanese section contains Japanese month name ("5月")
        assertThat(html).contains("5月");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private KwMonitoringDocument docWithClient(String client, String priority, String title) {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", client);
        dynData.put("Priority", priority);

        return KwMonitoringDocument.builder()
                .id("doc-" + title)
                .title(title)
                .body("Report body for " + title)
                .createdTimestamp(1700000000L)
                .updatedTimestamp(1700001000L)
                .attributes(Map.of("dynamicData", dynData))
                .build();
    }
}
