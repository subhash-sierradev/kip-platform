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
    private KwMonitoringDataTranslator kwMonitoringDataTranslator;

    private KwToConfluenceOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new KwToConfluenceOrchestrator(
                kwGraphQLService, confluencePageRenderer, confluenceApiClient,
                kwMonitoringDataTranslator);
        // Default: data translator is a pass-through (returns original docs).
        lenient().when(kwMonitoringDataTranslator.translate(any(), anyString(), anyString()))
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
        assertThat(result.pageUrl()).isEqualTo("https://example.com/page/42");
        assertThat(result.pageId()).isEqualTo("42");

        String expectedDate = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .format(Instant.parse("2026-01-31T23:59:59Z").atZone(ZoneId.of("UTC")));
        assertThat(requestCaptor.getValue().pageTitle()).isEqualTo("2025-" + expectedDate);
    }

    @Test
    void processExecution_multipleLanguages_publishesSingleCombinedPage() {
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
        List<KwMonitoringDocument> jaDocs = List.of(buildDocument("doc-1-ja"));
        List<KwMonitoringDocument> deDocs = List.of(buildDocument("doc-1-de"));

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));

        // Translator returns per-language documents
        when(kwMonitoringDataTranslator.translateAll(docs, "en", List.of("ja", "de")))
                .thenReturn(Map.of("ja", jaDocs, "de", deDocs));

        // Renderer combines all languages into one page
        when(confluencePageRenderer.buildMultiLanguagePageContent(any(), any(), anyString(), any()))
                .thenReturn("<p>EN+JA+DE combined</p>");

        ArgumentCaptor<ConfluencePublishRequest> captor =
                ArgumentCaptor.forClass(ConfluencePublishRequest.class);
        when(confluenceApiClient.createOrUpdatePage(captor.capture()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // Only ONE combined page is published
        verify(confluenceApiClient, times(1)).createOrUpdatePage(any());
        assertThat(captor.getValue().pageTitle()).isEqualTo("2026/01/31");
        assertThat(captor.getValue().body()).isEqualTo("<p>EN+JA+DE combined</p>");

        assertThat(result.pageUrl()).isEqualTo("https://page.url");
        assertThat(result.pageId()).isEqualTo("1");
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
        when(confluencePageRenderer.buildMultiLanguagePageContent(any(), any(), anyString(), any()))
                .thenReturn("<p>English</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        verify(confluenceApiClient, times(1)).createOrUpdatePage(any());
        // Data translator must never be called when only the source language is requested
        verify(kwMonitoringDataTranslator, never()).translate(any(), anyString(), anyString());
        verify(kwMonitoringDataTranslator, never()).translateAll(any(), anyString(), any());
        assertThat(result.pageUrl()).isEqualTo("https://page.url");
        assertThat(result.pageId()).isEqualTo("1");
    }

    @Test
    void processExecution_multipleLanguages_translationFails_stillPublishesCombinedPage() {
        // When translateAll returns an empty map (e.g. translation API disabled),
        // the orchestrator should still publish the combined page using source-language content.
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

        // Translation returns empty (disabled / internal failure handled by translator)
        when(kwMonitoringDataTranslator.translateAll(any(), anyString(), any()))
                .thenReturn(Map.of());

        when(confluencePageRenderer.buildMultiLanguagePageContent(any(), any(), anyString(), any()))
                .thenReturn("<p>EN only</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url/combined", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // ONE combined page published; job succeeds
        verify(confluenceApiClient, times(1)).createOrUpdatePage(any());
        assertThat(result.errorMessage()).isNull();
        assertThat(result.pageUrl()).isEqualTo("https://page.url/combined");
        assertThat(result.pageId()).isEqualTo("1");
    }

    @Test
    void processExecution_emptyMonitoringData_returnsSuccessWithZeroRecords() {
        ConfluenceExecutionCommand cmd = buildCommand("DYNAMIC_TYPE", "{date}-Report", "Europe/London");

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.totalRecords()).isZero();
        assertThat(result.pageUrl()).isNull();
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
        assertThat(result.pageUrl()).isNull();
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
        assertThat(result.pageUrl()).isEqualTo("https://page.url");
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
        assertThat(result.pageUrl()).isEqualTo("https://page.url");
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
        assertThat(result.pageUrl()).isEqualTo("https://page.url");
        assertThat(result.pageId()).isEqualTo("1");
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
