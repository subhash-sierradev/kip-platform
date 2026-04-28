package com.integration.execution.service.processor;

import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.client.ConfluenceApiClient.ConfluencePublishRequest;
import com.integration.execution.client.ConfluenceApiClient.ConfluencePublishResult;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.service.KwGraphQLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KwToConfluenceOrchestratorTest {

    @Mock
    private KwGraphQLService kwGraphQLService;

    @Mock
    private ConfluencePageRenderer confluencePageRenderer;

    @Mock
    private ConfluenceApiClient confluenceApiClient;

    @Mock
    private ConfluenceTranslationStep confluenceTranslationStep;

    private KwToConfluenceOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new KwToConfluenceOrchestrator(
                kwGraphQLService, confluencePageRenderer, confluenceApiClient,
                confluenceTranslationStep);
        // Default: translation step is a pass-through.
        lenient().when(confluenceTranslationStep.translate(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void processExecution_withMonitoringData_publishesEnglishPageAndReturnsResult() {
        ConfluenceExecutionCommand cmd = buildCommand("DYNAMIC_TYPE", "2025-{date}", "UTC");
        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"), buildDocument("doc-2"));

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>content</p>");
        ArgumentCaptor<ConfluencePublishRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfluencePublishRequest.class);
        when(confluenceApiClient.createOrUpdatePage(requestCaptor.capture()))
                .thenReturn(new ConfluencePublishResult("https://example.com/page/42", "42"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.confluencePageUrl()).isEqualTo("https://example.com/page/42");
        assertThat(result.confluencePageId()).isEqualTo("42");

        String expectedDate = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .format(Instant.parse("2026-01-31T23:59:59Z").atZone(ZoneId.of("UTC")));
        assertThat(requestCaptor.getValue().pageTitle()).isEqualTo("2025-" + expectedDate);
    }

    @Test
    void processExecution_multipleLanguages_publishesOnePagePerLanguage() {
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .businessTimeZone("UTC")
                .sourceLanguage("en")
                .languageCodes(List.of("en", "ja", "de"))
                .tenantId("tenant-1")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>English</p>");
        when(confluenceTranslationStep.translate("<p>English</p>", "en", "ja"))
                .thenReturn("<p>日本語</p>");
        when(confluenceTranslationStep.translate("<p>English</p>", "en", "de"))
                .thenReturn("<p>Deutsch</p>");

        ArgumentCaptor<ConfluencePublishRequest> captor =
                ArgumentCaptor.forClass(ConfluencePublishRequest.class);
        when(confluenceApiClient.createOrUpdatePage(captor.capture()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // 3 pages: English + Japanese + German
        verify(confluenceApiClient, times(3)).createOrUpdatePage(any());
        List<ConfluencePublishRequest> requests = captor.getAllValues();

        assertThat(requests.get(0).pageTitle()).isEqualTo("2026/01/31");
        assertThat(requests.get(0).body()).isEqualTo("<p>English</p>");

        assertThat(requests.get(1).pageTitle()).isEqualTo("2026/01/31 [JA]");
        assertThat(requests.get(1).body()).isEqualTo("<p>日本語</p>");

        assertThat(requests.get(2).pageTitle()).isEqualTo("2026/01/31 [DE]");
        assertThat(requests.get(2).body()).isEqualTo("<p>Deutsch</p>");

        assertThat(result.publishedPages()).hasSize(3);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void processExecution_englishOnlyLanguages_publishesOnlyOneEnglishPage() {
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .businessTimeZone("UTC")
                .sourceLanguage("en")
                .languageCodes(List.of("en"))
                .tenantId("tenant-1")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>English</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        verify(confluenceApiClient, times(1)).createOrUpdatePage(any());
        verify(confluenceTranslationStep, never()).translate(anyString(), anyString(), anyString());
        assertThat(result.publishedPages()).hasSize(1);
        assertThat(result.publishedPages().get(0).languageCode()).isEqualTo("en");
    }

    @Test
    void processExecution_translatedPagePublishFails_continuesAndReturnsSuccess() {
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .businessTimeZone("UTC")
                .sourceLanguage("en")
                .languageCodes(List.of("en", "fr"))
                .tenantId("tenant-1")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>English</p>");

        // English page publishes OK, French throws
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url/en", "1"))
                .thenThrow(new RuntimeException("Confluence API timeout"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // Job succeeds; only the English page appears in publishedPages
        assertThat(result.errorMessage()).isNull();
        assertThat(result.publishedPages()).hasSize(1);
        assertThat(result.publishedPages().get(0).languageCode()).isEqualTo("en");
    }

    @Test
    void processExecution_emptyMonitoringData_returnsSuccessWithZeroRecords() {
        ConfluenceExecutionCommand cmd = buildCommand("DYNAMIC_TYPE", "{date}-Report", "Europe/London");

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.totalRecords()).isZero();
        assertThat(result.confluencePageUrl()).isNull();
        verify(confluenceApiClient, never()).createOrUpdatePage(any());
    }

    @Test
    void processExecution_confluenceClientThrowsOnEnglishPage_returnsFailedResult() {
        ConfluenceExecutionCommand cmd = buildCommand("TYPE", "{date}", "UTC");
        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenThrow(new RuntimeException("Confluence API unavailable"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).contains("Confluence API unavailable");
        assertThat(result.totalRecords()).isZero();
        assertThat(result.confluencePageUrl()).isNull();
    }

    @Test
    void processExecution_graphqlServiceThrows_returnsFailedResult() {
        ConfluenceExecutionCommand cmd = buildCommand("TYPE", "{date}", "UTC");

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("KW unavailable"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).contains("KW unavailable");
        assertThat(result.totalRecords()).isZero();
    }

    @Test
    void processExecution_pageTitleUsesWindowEndInConfluenceTimezone() {
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}-Report")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("folder-id")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-01T03:00:00Z"))
                .businessTimeZone("America/New_York")
                .tenantId("tenant-1")
                .build();
        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(ZoneId.of("America/New_York"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        ArgumentCaptor<ConfluencePublishRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfluencePublishRequest.class);
        when(confluenceApiClient.createOrUpdatePage(requestCaptor.capture()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "99"));

        orchestrator.processExecution(cmd);

        assertThat(requestCaptor.getValue().pageTitle()).isEqualTo("2026/01/31-Report");
    }

    @Test
    void processExecution_businessTimeZoneUsedAsFallback() {
        ConfluenceExecutionCommand cmd = buildCommand("TYPE", "{date}", "America/Chicago");
        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));

        ArgumentCaptor<ZoneId> fallbackCaptor = ArgumentCaptor.forClass(ZoneId.class);
        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), fallbackCaptor.capture()))
                .thenReturn(ZoneId.of("America/Chicago"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        orchestrator.processExecution(cmd);

        assertThat(fallbackCaptor.getValue()).isEqualTo(ZoneId.of("America/Chicago"));
    }

    @Test
    void processExecution_nullWindowStartAndEnd_usesNowAsDefault() {
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .windowStart(null)
                .windowEnd(null)
                .businessTimeZone("UTC")
                .build();

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.totalRecords()).isZero();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void processExecution_nullBusinessTimezoneAndNullBlankLanguageCodes_onlyPublishesSourcePage() {
        // null businessTimeZone   → covers `!= null` false branch in processExecution
        // null entry in list      → covers `langCode == null` true branch in the for-loop
        // blank entry in list     → covers `langCode.isBlank()` true branch in the for-loop
        List<String> langs = new ArrayList<>();
        langs.add("en");
        langs.add(null);
        langs.add("   ");

        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .businessTimeZone(null)
                .sourceLanguage("en")
                .languageCodes(langs)
                .tenantId("tenant-1")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        verify(confluenceApiClient, times(1)).createOrUpdatePage(any());
        assertThat(result.errorMessage()).isNull();
        assertThat(result.publishedPages()).hasSize(1);
    }

    @Test
    void processExecution_blankBusinessTimeZone_usesNullFallbackForTimezone() {
        // blank businessTimeZone → covers `!isBlank()` false branch
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .businessTimeZone("   ")
                .sourceLanguage("en")
                .languageCodes(List.of("en"))
                .tenantId("tenant-1")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.publishedPages()).hasSize(1);
    }

    @Test
    void processExecution_nullWindowEndAndBlankSourceLanguage_usesDefaultsWithoutError() {
        // null windowEnd     → covers `windowEnd != null` false branch in buildMonitoringPageTitle
        // blank sourceLanguage → covers `!isBlank()` false branch in resolveSource
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("ROOT")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(null)
                .businessTimeZone("UTC")
                .sourceLanguage("   ")
                .languageCodes(List.of("en"))
                .tenantId("tenant-1")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.publishedPages()).hasSize(1);
        // Blank sourceLanguage resolved to "en"
        assertThat(result.publishedPages().get(0).languageCode()).isEqualTo("en");
    }

    private ConfluenceExecutionCommand buildCommand(
            String dynamicType, String reportTemplate, String timezone) {
        return ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType(dynamicType)
                .reportNameTemplate(reportTemplate)
                .connectionSecretName("my-secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("folder-id")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .businessTimeZone(timezone)
                .tenantId("tenant-1")
                .build();
    }

    private KwMonitoringDocument buildDocument(String id) {
        return KwMonitoringDocument.builder()
                .id(id)
                .title("Report " + id)
                .attributes(Map.of("dynamicData", Map.of("Client", "ACME", "Priority", "HIGH")))
                .build();
    }
}
