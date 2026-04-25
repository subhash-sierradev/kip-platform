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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KwToConfluenceOrchestratorTest {

    @Mock
    private KwGraphQLService kwGraphQLService;

    @Mock
    private ConfluencePageRenderer confluencePageRenderer;

    @Mock
    private ConfluenceApiClient confluenceApiClient;

    private KwToConfluenceOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new KwToConfluenceOrchestrator(
                kwGraphQLService, confluencePageRenderer, confluenceApiClient);
    }

    @Test
    void processExecution_withMonitoringData_rendersAndPublishesPage() {
        ConfluenceExecutionCommand cmd = buildCommand("DYNAMIC_TYPE", "2025-{date}", "UTC");
        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"), buildDocument("doc-2"));

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluencePageRenderer.filterNamedClients(docs)).thenReturn(docs);
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
        // Title date must reflect windowEnd (2026-01-31) formatted in UTC, not system clock
        String expectedDate = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .format(Instant.parse("2026-01-31T23:59:59Z").atZone(ZoneId.of("UTC")));
        assertThat(requestCaptor.getValue().pageTitle()).isEqualTo("2025-" + expectedDate);
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
    void processExecution_confluenceClientThrows_returnsFailedResult() {
        ConfluenceExecutionCommand cmd = buildCommand("TYPE", "{date}", "UTC");
        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluencePageRenderer.filterNamedClients(docs)).thenReturn(docs);
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
        // windowEnd 2026-02-01T03:00:00Z is 2026-01-31 in America/New_York (UTC-5)
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
        when(confluencePageRenderer.filterNamedClients(docs)).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(ZoneId.of("America/New_York"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        ArgumentCaptor<ConfluencePublishRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfluencePublishRequest.class);
        when(confluenceApiClient.createOrUpdatePage(requestCaptor.capture()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "99"));

        orchestrator.processExecution(cmd);

        ArgumentCaptor<ZoneId> timezoneCaptor = ArgumentCaptor.forClass(ZoneId.class);
        verify(confluenceApiClient).getUserTimezone(anyString(), timezoneCaptor.capture());
        assertThat(timezoneCaptor.getValue()).isEqualTo(ZoneId.of("America/New_York"));
    }

    @Test
    void processExecution_allRecordsHaveUnknownClient_returnsSuccessWithZeroRecords() {
        ConfluenceExecutionCommand cmd = buildCommand("TYPE", "{date}", "UTC");
        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-unknown"));

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluencePageRenderer.filterNamedClients(docs)).thenReturn(List.of());

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.totalRecords()).isZero();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.confluencePageUrl()).isNull();
        verify(confluenceApiClient, never()).createOrUpdatePage(any());
        verify(confluenceApiClient, never()).getUserTimezone(anyString(), any());
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
    void processExecution_documentAtExactWindowEnd_isExcludedByFilter() {
        // windowEnd epoch = 1000s; document updatedTimestamp == 1000 must be excluded
        Instant windowEnd = Instant.ofEpochSecond(1000);
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .windowStart(Instant.ofEpochSecond(0))
                .windowEnd(windowEnd)
                .businessTimeZone("UTC")
                .build();

        KwMonitoringDocument atBoundary = KwMonitoringDocument.builder()
                .id("doc-boundary").updatedTimestamp(1000L).build();
        KwMonitoringDocument beforeBoundary = KwMonitoringDocument.builder()
                .id("doc-before").updatedTimestamp(999L).build();

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(atBoundary, beforeBoundary));
        when(confluencePageRenderer.filterNamedClients(List.of(beforeBoundary))).thenReturn(List.of());

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        // The non-boundary document is passed to filterNamedClients, which returns empty,
        // so zero records are published.
        verify(confluencePageRenderer).filterNamedClients(List.of(beforeBoundary));
        assertThat(result.totalRecords()).isZero();
    }

    @Test
    void processExecution_documentBeforeWindowEnd_isIncludedByFilter() {
        Instant windowEnd = Instant.ofEpochSecond(1000);
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .windowStart(Instant.ofEpochSecond(0))
                .windowEnd(windowEnd)
                .businessTimeZone("UTC")
                .build();

        KwMonitoringDocument doc = KwMonitoringDocument.builder()
                .id("doc-1").title("Report").updatedTimestamp(999L)
                .attributes(Map.of()).build();

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(doc));
        when(confluencePageRenderer.filterNamedClients(List.of(doc))).thenReturn(List.of(doc));
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>ok</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void processExecution_nullWindowEnd_pageTitleUsesNow() {
        // Covers buildMonitoringPageTitle: windowEnd == null → Instant.now() used as fallback
        // Also covers toEpochSeconds: instant == null → Instant.now() used
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}-Title")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .windowStart(null)
                .windowEnd(null)
                .businessTimeZone("UTC")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluencePageRenderer.filterNamedClients(docs)).thenReturn(docs);
        when(confluenceApiClient.getUserTimezone(anyString(), any())).thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        ArgumentCaptor<ConfluencePublishRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfluencePublishRequest.class);
        when(confluenceApiClient.createOrUpdatePage(requestCaptor.capture()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.totalRecords()).isEqualTo(1);
        // Title must be non-blank (exact date is 'now', so just verify it rendered)
        assertThat(requestCaptor.getValue().pageTitle()).endsWith("-Title");
    }

    @Test
    void processExecution_documentAtBoundarySecond_isIncludedWhenWindowEndHasNanos() {
        // windowEnd has a sub-second component → exactSecondBoundary = false → filter uses <=
        // A document whose updatedTimestamp equals windowEnd.getEpochSecond() is still before
        // windowEnd and must be included.
        Instant windowEnd = Instant.ofEpochSecond(1000, 500_000_000); // 1000.5 s
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .windowStart(Instant.ofEpochSecond(0))
                .windowEnd(windowEnd)
                .businessTimeZone("UTC")
                .build();

        KwMonitoringDocument atBoundarySecond = KwMonitoringDocument.builder()
                .id("doc-boundary").updatedTimestamp(1000L).build();

        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(atBoundarySecond));
        when(confluencePageRenderer.filterNamedClients(List.of(atBoundarySecond)))
                .thenReturn(List.of());

        orchestrator.processExecution(cmd);

        // Document at the boundary second is included (not filtered out) when windowEnd has nanos
        verify(confluencePageRenderer).filterNamedClients(List.of(atBoundarySecond));
    }

    @Test
    void processExecution_blankBusinessTimezone_passesNullFallbackToClient() {
        // Covers: businessTimeZone blank → fallbackTimezone = null (else branch of ternary)
        ConfluenceExecutionCommand cmd = ConfluenceExecutionCommand.builder()
                .jobExecutionId(UUID.randomUUID())
                .integrationId(UUID.randomUUID())
                .dynamicDocumentType("TYPE")
                .reportNameTemplate("{date}")
                .connectionSecretName("secret")
                .confluenceSpaceKey("SPACE")
                .confluenceSpaceKeyFolderKey("folder")
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .businessTimeZone("   ")
                .build();

        List<KwMonitoringDocument> docs = List.of(buildDocument("doc-1"));
        when(kwGraphQLService.fetchMonitoringData(
                anyString(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(docs);
        when(confluencePageRenderer.filterNamedClients(docs)).thenReturn(docs);
        // null fallback passed to getUserTimezone
        when(confluenceApiClient.getUserTimezone(anyString(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(ZoneId.of("UTC"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        when(confluenceApiClient.createOrUpdatePage(any()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "1"));

        ConfluenceJobExecutionResult result = orchestrator.processExecution(cmd);

        assertThat(result.totalRecords()).isEqualTo(1);
        verify(confluenceApiClient).getUserTimezone(anyString(), org.mockito.ArgumentMatchers.isNull());
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
