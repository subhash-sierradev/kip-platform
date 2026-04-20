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
        when(confluenceApiClient.getUserTimezone(anyString(), any()))
                .thenReturn(ZoneId.of("America/New_York"));
        when(confluencePageRenderer.buildPageContent(any(), any())).thenReturn("<p>page</p>");
        ArgumentCaptor<ConfluencePublishRequest> requestCaptor =
                ArgumentCaptor.forClass(ConfluencePublishRequest.class);
        when(confluenceApiClient.createOrUpdatePage(requestCaptor.capture()))
                .thenReturn(new ConfluencePublishResult("https://page.url", "99"));

        orchestrator.processExecution(cmd);

        // 2026-02-01T03:00:00Z = 2026-01-31T22:00:00 in America/New_York
        assertThat(requestCaptor.getValue().pageTitle()).isEqualTo("2026/01/31-Report");
    }

    @Test
    void processExecution_businessTimeZoneUsedAsFallback_whenConfluenceTimezoneUnavailable() {
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

        // fallback ZoneId passed to getUserTimezone must match businessTimeZone on the command
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
