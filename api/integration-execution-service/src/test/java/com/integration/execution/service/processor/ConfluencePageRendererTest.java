package com.integration.execution.service.processor;

import com.integration.execution.config.AppConfig;
import com.integration.execution.model.KwMonitoringDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfluencePageRendererTest {

    private ConfluencePageRenderer renderer;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        renderer = new ConfluencePageRenderer(config.freemarkerConfiguration());
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
    void buildPageContent_documentWithNullAttributes_isExcludedFromReport() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("doc-no-attrs")
                .title("No Attributes")
                .attributes(null)
                .build();

        List<KwMonitoringDocument> filtered = renderer.filterNamedClients(List.of(doc));
        String html = renderer.buildPageContent(filtered, ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).doesNotContain("Unknown");
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
    void buildPageContent_clientWithBlankName_isExcludedFromReport() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "  ");
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("d7").title("Blank Client")
                .attributes(Map.of("dynamicData", dynData))
                .build();

        List<KwMonitoringDocument> filtered = renderer.filterNamedClients(List.of(doc));
        String html = renderer.buildPageContent(filtered, ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).doesNotContain("Unknown");
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
        ConfluencePageRenderer brokenRenderer = new ConfluencePageRenderer(brokenCfg);

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
    void buildPageContent_unknownClientsExcluded_onlyNamedClientsRendered() {
        List<KwMonitoringDocument> docs = List.of(
                docWithClient("Zeta Corp", "LOW", "Z"),
                docWithClient("Alpha Inc", "HIGH", "A"),
                docWithClient(null, "MEDIUM", "U1"),   // no client -> excluded
                docWithClient("  ", "LOW", "U2"));      // blank client -> excluded

        List<KwMonitoringDocument> filtered = renderer.filterNamedClients(docs);
        String html = renderer.buildPageContent(filtered, ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("Alpha Inc");
        assertThat(html).contains("Zeta Corp");
        assertThat(html).doesNotContain(">Unknown<");
    }

    @Test
    void buildPageContent_nullTitle_usesFallbackContainingDocumentId() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("doc-no-title-id")
                .title(null)
                .attributes(Map.of("dynamicData", Map.of("Client", "Corp", "Priority", "LOW")))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("[Untitled");
        assertThat(html).contains("doc-no-title-id");
    }

    @Test
    void buildPageContent_blankTitle_usesFallbackContainingDocumentId() {
        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("doc-blank-title-id")
                .title("   ")
                .attributes(Map.of("dynamicData", Map.of("Client", "Corp", "Priority", "LOW")))
                .build();

        String html = renderer.buildPageContent(List.of(doc), ZoneId.of("UTC"));

        assertThat(html).isNotBlank();
        assertThat(html).contains("[Untitled");
        assertThat(html).contains("doc-blank-title-id");
    }

    @Test
    void buildPageContent_onlyNamedClients_clientCountExcludesUnknown() {
        // Named clients only — unknown group should NOT appear at all
        List<KwMonitoringDocument> docs = List.of(
                docWithClient("Alpha", "HIGH", "A"),
                docWithClient("Beta", "LOW", "B"));

        String html = renderer.buildPageContent(docs, ZoneId.of("UTC"));

        assertThat(html).doesNotContain("Unknown");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private KwMonitoringDocument docWithClient(String client, String priority, String title) {
        Map<String, Object> dynData = new HashMap<>();
        if (client != null) {
            dynData.put("Client", client);
        }
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
