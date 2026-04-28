package com.integration.execution.service.processor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.client.ConfluenceApiClient.ConfluencePublishRequest;
import com.integration.execution.client.ConfluenceApiClient.ConfluencePublishResult;
import com.integration.execution.client.TranslationApiClient;
import com.integration.execution.config.AppConfig;
import com.integration.execution.config.properties.TranslationApiProperties;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.service.KwGraphQLService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Parameterized end-to-end pipeline test for multi-language Confluence reports.
 *
 * <p>Tests the full pipeline, using real FreeMarker and jsoup-based XHTML translation.
 * Content-verification tests use a mock {@link TranslationApiClient} to return
 * properly segmented responses. WireMock is still used for HTTP-error fallback tests.</p>
 */
class ConfluenceMultiLanguageReportTest {

    // ── WireMock server (shared across all parameterized cases) ─────────────
    private static WireMockServer translationApiMock;
    private static final String TRANSLATE_PATH = "/api/translate";
    private static final Pattern SEG_SPLIT =
            Pattern.compile(Pattern.quote("\n" + XhtmlTextTranslator.SEG + "\n"));

    // ── Sample monitoring document ──────────────────────────────────────────
    private static final KwMonitoringDocument SAMPLE_DOC = buildSampleDocument();

    // ── System under test ───────────────────────────────────────────────────
    private KwGraphQLService kwGraphQLService;
    private ConfluenceApiClient confluenceApiClient;
    private KwToConfluenceOrchestrator orchestrator;
    private TranslationApiClient mockTranslationApiClient;

    // ── Captured publish requests ───────────────────────────────────────────
    private ConfluencePublishRequest capturedPublishRequest;


    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Test infrastructure
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @BeforeAll
    static void startWireMock() {
        translationApiMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        translationApiMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        translationApiMock.stop();
    }

    @BeforeEach
    void setUp() {
        kwGraphQLService = mock(KwGraphQLService.class);
        confluenceApiClient = mock(ConfluenceApiClient.class);
        mockTranslationApiClient = mock(TranslationApiClient.class);

        AppConfig appConfig = new AppConfig();
        ConfluencePageRenderer renderer = new ConfluencePageRenderer(appConfig.freemarkerConfiguration());

        TranslationApiProperties props = new TranslationApiProperties();
        props.setBaseUrl("http://localhost:" + translationApiMock.port());
        props.setEnabled(true);
        props.setTimeoutSeconds(10);

        // Real XhtmlTextTranslator backed by a mock TranslationApiClient
        XhtmlTextTranslator xhtmlTextTranslator = new XhtmlTextTranslator(mockTranslationApiClient, props);
        ConfluenceTranslationStep translationStep = new ConfluenceTranslationStep(props, xhtmlTextTranslator);

        orchestrator = new KwToConfluenceOrchestrator(
                kwGraphQLService, renderer, confluenceApiClient, translationStep);

        // Default: Confluence mock returns a success publish result
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(java.time.ZoneId.of("UTC"));
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenAnswer(invocation -> {
                    capturedPublishRequest = invocation.getArgument(0);
                    return new ConfluencePublishResult("https://confluence.example.com/page/1", "1");
                });
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Parameterized translation scenarios
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Provides all language test cases.
     *
     * <p>Each argument set is: (displayName, languageCode, translatedSnippet)</p>
     * <ul>
     *   <li>{@code languageCode} — BCP-47 target language (e.g. "ja")</li>
     *   <li>{@code translatedSnippet} — a substring visible in the expected translated page</li>
     * </ul>
     */
    static Stream<Arguments> multiLanguageScenarios() {
        return Stream.of(
                Arguments.of("Japanese (ja)", "ja",
                        "日本語テキスト: 今日の監視レポートです。全てのシステムは正常に動作しています。"),
                Arguments.of("German (de)", "de",
                        "Deutsch Text: Der heutige Überwachungsbericht. Alle Systeme funktionieren normal."),
                Arguments.of("Russian (ru)", "ru",
                        "Русский текст: Ежедневный отчёт мониторинга. Все системы работают нормально."),
                Arguments.of("Arabic (ar)", "ar",
                        "النص العربي: تقرير المراقبة اليومي. جميع الأنظمة تعمل بشكل طبيعي.")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multiLanguageScenarios")
    @DisplayName("pipeline renders FreeMarker in English then translates to target language")
    void pipeline_rendersAndTranslatesToTargetLanguage(
            final String displayName,
            final String targetLanguage,
            final String translatedOutput) {

        // Stub: for each batch, return the translatedOutput repeated for each segment
        stubContentTranslation(targetLanguage, translatedOutput);
        stubKwService();

        ConfluenceExecutionCommand cmd = buildCommand("en", List.of(targetLanguage));

        // ── Act ──────────────────────────────────────────────────────────────
        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // ── Assert ───────────────────────────────────────────────────────────
        assertThat(result.errorMessage()).as("job must not fail").isNull();
        assertThat(result.totalRecords()).isEqualTo(1);

        // Two pages published: English + target language
        verify(confluenceApiClient, times(2)).createOrUpdatePage(any());

        // capturedPublishRequest is the last call = translated page
        assertThat(capturedPublishRequest).isNotNull();
        assertThat(capturedPublishRequest.body())
                .as("page body must contain the translated snippet for %s", displayName)
                .contains(translatedOutput);

        // The page body must be UTF-8 encodable (no data loss for multi-byte chars)
        byte[] encoded = capturedPublishRequest.body().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String reDecoded = new String(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(reDecoded).isEqualTo(capturedPublishRequest.body());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Same-language passthrough — no Translation API call
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @ParameterizedTest(name = "sourceLanguage={0}, languageCodes={1}")
    @MethodSource("sameLanguageScenarios")
    @DisplayName("pipeline skips Translation API when target equals source language")
    void pipeline_sameLanguage_skipsTranslationApiCall(
            final String sourceLanguage,
            final List<String> languageCodes) {

        stubKwService();
        ConfluenceExecutionCommand cmd = buildCommand(sourceLanguage, languageCodes);

        // ── Act ──────────────────────────────────────────────────────────────
        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // ── Assert ───────────────────────────────────────────────────────────
        assertThat(result.errorMessage()).isNull();
        assertThat(result.totalRecords()).isEqualTo(1);

        // No translation should have been requested (mock verifies 0 interactions)
        verify(mockTranslationApiClient, org.mockito.Mockito.never())
                .translate(anyString(), anyString(), anyString());
    }

    static Stream<Arguments> sameLanguageScenarios() {
        return Stream.of(
                Arguments.of("en", List.of("en")),
                Arguments.of("en", List.of("en", "en")), // all codes same as source
                Arguments.of("en", List.of()),            // no target codes
                Arguments.of("ja", List.of("ja"))         // non-English same source and target
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Graceful fallback when Translation API is unavailable
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @ParameterizedTest(name = "HTTP {0} → job succeeds with English content")
    @MethodSource("translationApiErrorScenarios")
    @DisplayName("pipeline falls back to English when Translation API returns error")
    void pipeline_translationApiFails_fallsBackToEnglishAndJobSucceeds(
            final int httpStatus, final String desc) {

        // Use a real TranslationApiClient pointing at WireMock for HTTP error testing
        TranslationApiProperties wireMockProps = new TranslationApiProperties();
        wireMockProps.setBaseUrl("http://localhost:" + translationApiMock.port());
        wireMockProps.setEnabled(true);
        wireMockProps.setTimeoutSeconds(10);

        translationApiMock.resetAll();
        translationApiMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse().withStatus(httpStatus).withBody("error")));

        TranslationApiClient realApiClient = new TranslationApiClient(wireMockProps, new ObjectMapper());
        XhtmlTextTranslator realTranslator = new XhtmlTextTranslator(realApiClient, wireMockProps);
        ConfluenceTranslationStep errorStep = new ConfluenceTranslationStep(wireMockProps, realTranslator);

        AppConfig appConfig = new AppConfig();
        KwToConfluenceOrchestrator errorOrchestrator = new KwToConfluenceOrchestrator(
                kwGraphQLService,
                new ConfluencePageRenderer(appConfig.freemarkerConfiguration()),
                confluenceApiClient,
                errorStep);

        stubKwService();
        ConfluenceExecutionCommand cmd = buildCommand("en", List.of("ja"));

        // ── Act ──────────────────────────────────────────────────────────────
        ConfluenceJobExecutionResult result = errorOrchestrator.processExecution(cmd);

        // ── Assert ───────────────────────────────────────────────────────────
        assertThat(result.errorMessage())
                .as("job must not fail when Translation API returns %s", httpStatus)
                .isNull();
        assertThat(result.totalRecords()).isEqualTo(1);

        // English page + Japanese page (fallback) = 2 calls
        verify(confluenceApiClient, times(2)).createOrUpdatePage(any());
        assertThat(capturedPublishRequest).isNotNull();
        assertThat(capturedPublishRequest.body()).isNotBlank();
    }

    static Stream<Arguments> translationApiErrorScenarios() {
        return Stream.of(
                Arguments.of(500, "Internal Server Error"),
                Arguments.of(503, "Service Unavailable"),
                Arguments.of(422, "Unprocessable Entity"),
                Arguments.of(400, "Bad Request")
        );
    }

    @org.junit.jupiter.api.Test
    @DisplayName("pipeline falls back to English when Translation Service is unreachable")
    void pipeline_translationServiceUnreachable_fallsBackToEnglishAndJobSucceeds() {
        TranslationApiProperties offlineProps = new TranslationApiProperties();
        offlineProps.setBaseUrl("http://localhost:1"); // refused
        offlineProps.setEnabled(true);
        offlineProps.setTimeoutSeconds(2);

        TranslationApiClient offlineClient = new TranslationApiClient(offlineProps, new ObjectMapper());
        XhtmlTextTranslator offlineTranslator = new XhtmlTextTranslator(offlineClient, offlineProps);
        ConfluenceTranslationStep offlineStep = new ConfluenceTranslationStep(offlineProps, offlineTranslator);

        AppConfig appConfig = new AppConfig();
        KwToConfluenceOrchestrator offlineOrchestrator = new KwToConfluenceOrchestrator(
                kwGraphQLService,
                new ConfluencePageRenderer(appConfig.freemarkerConfiguration()),
                confluenceApiClient,
                offlineStep);

        stubKwService();
        ConfluenceExecutionCommand cmd = buildCommand("en", List.of("de"));

        ConfluenceJobExecutionResult result = offlineOrchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.totalRecords()).isEqualTo(1);
        // English page + German fallback page = 2 calls
        verify(confluenceApiClient, times(2)).createOrUpdatePage(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Multi-code list
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @org.junit.jupiter.api.Test
    @DisplayName("when languageCodes contains source + target, separate pages are published")
    void pipeline_mixedLanguageCodeList_publishesBothPages() {
        String japaneseContent = "日本語: 今日の監視レポート";
        stubContentTranslation("ja", japaneseContent);
        stubKwService();

        ConfluenceExecutionCommand cmd = buildCommand("en", List.of("en", "ja"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.publishedPages()).hasSize(2);
        // Last captured is the Japanese page
        assertThat(capturedPublishRequest.body()).contains(japaneseContent);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Helpers
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Stubs mockTranslationApiClient so that for any batch request for {@code targetLanguage},
     * the response contains the correct number of {@link XhtmlTextTranslator#SEG}-separated
     * segments, each filled with {@code translatedValue}. This lets the XhtmlTextTranslator
     * successfully reassemble the translated XHTML.
     */
    private void stubContentTranslation(final String targetLanguage, final String translatedValue) {
        when(mockTranslationApiClient.translate(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(targetLanguage)))
                .thenAnswer(inv -> {
                    String batch = (String) inv.getArgument(0);
                    String[] segments = SEG_SPLIT.split(batch, -1);
                    // Return translatedValue for every segment, joined with <<<SEG>>>
                    String joinedResponse = Arrays.stream(segments)
                            .map(ignored -> translatedValue)
                            .collect(Collectors.joining("\n" + XhtmlTextTranslator.SEG + "\n"));
                    return Optional.of(joinedResponse);
                });
    }

    private void stubKwService() {
        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(SAMPLE_DOC));
    }

    private ConfluenceExecutionCommand buildCommand(
            final String sourceLanguage, final List<String> languageCodes) {
        return ConfluenceExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .integrationName("Multi-Language Report")
                .jobExecutionId(UUID.randomUUID())
                .tenantId("tenant-test")
                .connectionSecretName("test-secret")
                .confluenceSpaceKey("MONITOR")
                .confluenceSpaceKeyFolderKey("ROOT")
                .reportNameTemplate("Daily Report {date}")
                .dynamicDocumentType("MONITORING_DOC")
                .businessTimeZone("UTC")
                .triggeredBy(TriggerType.API)
                .triggeredByUser("test-user")
                .windowStart(Instant.parse("2026-04-26T00:00:00Z"))
                .windowEnd(Instant.parse("2026-04-27T00:00:00Z"))
                .sourceLanguage(sourceLanguage)
                .languageCodes(languageCodes)
                .build();
    }

    private static KwMonitoringDocument buildSampleDocument() {
        Map<String, Object> dynData = new HashMap<>();
        dynData.put("Client", "ACME Corporation");
        dynData.put("Priority", "HIGH");
        dynData.put("Region", "North America");
        dynData.put("Status", "Active");
        return KwMonitoringDocument.builder()
                .id("doc-001")
                .title("Security Incident Report — Q2 2026")
                .body("All monitoring systems are operating within normal parameters.")
                .createdTimestamp(1_700_000_000L)
                .updatedTimestamp(1_700_001_000L)
                .attributes(Map.of("dynamicData", dynData))
                .build();
    }
}

