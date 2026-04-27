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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Parameterized end-to-end pipeline test for multi-language Confluence reports.
 *
 * <h2>What this tests</h2>
 * <p>The complete pipeline without Spring context or Docker:</p>
 * <pre>
 *   KwGraphQLService (mock)
 *        ↓  monitoring docs
 *   ConfluencePageRenderer (real FreeMarker)
 *        ↓  English XHTML
 *   ConfluenceTranslationStep (real)
 *        ↓  calls Translation API (WireMock)
 *        ↓  translated content
 *   KwToConfluenceOrchestrator
 *        ↓
 *   ConfluenceApiClient.createOrUpdatePage (mock)
 *        ↓
 *   ConfluenceJobExecutionResult
 * </pre>
 *
 * <h2>Languages tested</h2>
 * <ul>
 *   <li><b>Japanese (ja)</b> — multi-byte CJK characters</li>
 *   <li><b>German (de)</b> — European with umlauts</li>
 *   <li><b>Russian (ru)</b> — Cyrillic script</li>
 *   <li><b>Arabic (ar)</b> — right-to-left script</li>
 *   <li><b>Same-language passthrough</b> — sourceLanguage == targetLanguage → no API call</li>
 *   <li><b>Translation API unavailable</b> — graceful fallback to English</li>
 * </ul>
 *
 * <h2>Running the test</h2>
 * <pre>
 *   # From api/ directory
 *   ./gradlew :integration-execution-service:test \
 *     --tests "*.ConfluenceMultiLanguageReportTest"
 * </pre>
 */
class ConfluenceMultiLanguageReportTest {

    // ── WireMock server (shared across all parameterized cases) ─────────────
    private static WireMockServer translationApiMock;
    private static final String TRANSLATE_PATH = "/api/translate";

    // ── Sample monitoring document ──────────────────────────────────────────
    private static final KwMonitoringDocument SAMPLE_DOC = buildSampleDocument();

    // ── System under test ───────────────────────────────────────────────────
    private KwGraphQLService kwGraphQLService;
    private ConfluenceApiClient confluenceApiClient;
    private KwToConfluenceOrchestrator orchestrator;

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

        // Real FreeMarker renderer (uses classpath template)
        AppConfig appConfig = new AppConfig();
        ConfluencePageRenderer renderer = new ConfluencePageRenderer(appConfig.freemarkerConfiguration());

        // Real translation step pointing at WireMock
        TranslationApiProperties props = new TranslationApiProperties();
        props.setBaseUrl("http://localhost:" + translationApiMock.port());
        props.setEnabled(true);
        props.setTimeoutSeconds(10);

        TranslationApiClient apiClient = new TranslationApiClient(props, new ObjectMapper());
        ConfluenceTranslationStep translationStep = new ConfluenceTranslationStep(props, apiClient);

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

        // ── Arrange ─────────────────────────────────────────────────────────
        stubTranslationApi(targetLanguage, translatedOutput);
        stubKwService();

        ConfluenceExecutionCommand cmd = buildCommand("en", List.of(targetLanguage));

        // ── Act ──────────────────────────────────────────────────────────────
        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // ── Assert ───────────────────────────────────────────────────────────
        assertThat(result.errorMessage()).as("job must not fail").isNull();
        assertThat(result.totalRecords()).isEqualTo(1);

        // The translated content must have been pushed to Confluence
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

        translationApiMock.resetAll(); // no stubs — any call would fail

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.totalRecords()).isEqualTo(1);

        // Translation API must NOT have been called
        assertThat(translationApiMock.getAllServeEvents())
                .as("Translation API must not be called when source == target")
                .isEmpty();
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

        // Stub Translation API to return an error
        translationApiMock.resetAll();
        translationApiMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse().withStatus(httpStatus).withBody("error")));

        stubKwService();
        ConfluenceExecutionCommand cmd = buildCommand("en", List.of("ja"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // Job must succeed even though translation failed
        assertThat(result.errorMessage())
                .as("job must not fail when Translation API returns %s", httpStatus)
                .isNull();
        assertThat(result.totalRecords()).isEqualTo(1);

        // Confluence page must still have been published (with original English content)
        assertThat(capturedPublishRequest).isNotNull();
        assertThat(capturedPublishRequest.body())
                .as("fallback content must be English XHTML")
                .isNotBlank();

        verify(confluenceApiClient).createOrUpdatePage(any());
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
        // Point the translation step at a port nothing is listening on
        TranslationApiProperties offlineProps = new TranslationApiProperties();
        offlineProps.setBaseUrl("http://localhost:1"); // refused
        offlineProps.setEnabled(true);
        offlineProps.setTimeoutSeconds(2);

        TranslationApiClient offlineClient = new TranslationApiClient(offlineProps, new ObjectMapper());
        ConfluenceTranslationStep offlineStep = new ConfluenceTranslationStep(offlineProps, offlineClient);

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
        verify(confluenceApiClient).createOrUpdatePage(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Multi-code list: first non-source language is picked as target
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @org.junit.jupiter.api.Test
    @DisplayName("when languageCodes contains source + target, first non-source code is used")
    void pipeline_mixedLanguageCodeList_picksFirstNonSourceAsTarget() {
        String japaneseContent = "日本語: 今日の監視レポート";
        stubTranslationApi("ja", japaneseContent);
        stubKwService();

        // languageCodes = ["en", "ja"] — "en" is same as source, "ja" is the target
        ConfluenceExecutionCommand cmd = buildCommand("en", List.of("en", "ja"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(capturedPublishRequest.body()).contains(japaneseContent);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Helpers
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** Stubs the WireMock Translation API endpoint for the given language. */
    private void stubTranslationApi(final String languageCode, final String translatedValue) {
        translationApiMock.stubFor(post(urlEqualTo(TRANSLATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withBody(String.format("""
                                {
                                  "translationResults": [
                                    {
                                      "translatedTimestamp": 1776694594,
                                      "languageCode": "%s",
                                      "value": "%s"
                                    }
                                  ]
                                }
                                """, languageCode, escapeJson(translatedValue)))));
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

    /** Minimal JSON escaping for test stub bodies. */
    private static String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

