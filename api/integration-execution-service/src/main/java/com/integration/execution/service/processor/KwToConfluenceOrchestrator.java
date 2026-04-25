package com.integration.execution.service.processor;

import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.client.ConfluenceApiClient.ConfluencePublishRequest;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import com.integration.execution.service.KwGraphQLService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates the Confluence monitoring report generation pipeline:
 * Kw fetch → page build → Confluence publish → result.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KwToConfluenceOrchestrator {

    private static final int DEFAULT_MONITORING_OFFSET = 0;
    private static final int DEFAULT_MONITORING_LIMIT = 500;
    private static final DateTimeFormatter MONITORING_TITLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final KwGraphQLService kwGraphQLService;
    private final ConfluencePageRenderer confluencePageRenderer;
    private final ConfluenceApiClient confluenceApiClient;

    public ConfluenceJobExecutionResult processExecution(final ConfluenceExecutionCommand cmd) {
        try {
            int startTimestamp = toEpochSeconds(cmd.getWindowStart());
            int endTimestamp = toEpochSeconds(cmd.getWindowEnd());
            int offset = DEFAULT_MONITORING_OFFSET;
            int limit = DEFAULT_MONITORING_LIMIT;

            List<KwMonitoringDocument> monitoringData = kwGraphQLService.fetchMonitoringData(
                    cmd.getDynamicDocumentType(),
                    startTimestamp,
                    endTimestamp,
                    offset,
                    limit);

            // The Kaseware GraphQL API uses inclusive epoch-second boundaries.
            // ExecutionWindow.windowEnd is exclusive (half-open interval), so filter
            // out any record whose updatedTimestamp falls on the boundary second.
            Instant windowEnd = cmd.getWindowEnd();
            if (windowEnd != null) {
                long exclusiveEndSeconds = windowEnd.getEpochSecond();
                monitoringData = monitoringData.stream()
                        .filter(doc -> doc.getUpdatedTimestamp() < exclusiveEndSeconds)
                        .toList();
            }

            log.info("Confluence integration {} — monitoring records fetched={} startTs={} endTs={} start={} limit={}",
                    cmd.getIntegrationId(), monitoringData.size(), startTimestamp, endTimestamp, offset, limit);

            if (monitoringData.isEmpty()) {
                log.info("Confluence integration {} — no monitoring data found for requested window",
                        cmd.getIntegrationId());
                return ConfluenceJobExecutionResult.success(0, null, null);
            }

            List<KwMonitoringDocument> namedClientData = confluencePageRenderer.filterNamedClients(monitoringData);
            log.info("Confluence integration {} — records after unknown-client filter: fetched={} included={}",
                    cmd.getIntegrationId(), monitoringData.size(), namedClientData.size());

            if (namedClientData.isEmpty()) {
                log.info("Confluence integration {} — all fetched records have unknown client, no page published",
                        cmd.getIntegrationId());
                return ConfluenceJobExecutionResult.success(0, null, null);
            }

            // Fetch Confluence user timezone and convert monitoring data timestamps
            ZoneId fallbackTimezone = cmd.getBusinessTimeZone() != null && !cmd.getBusinessTimeZone().isBlank()
                    ? ZoneId.of(cmd.getBusinessTimeZone())
                    : null;
            ZoneId confluenceTimezone = confluenceApiClient.getUserTimezone(
                    cmd.getConnectionSecretName(), fallbackTimezone);
            log.info("Confluence integration {} — using timezone: {}",
                    cmd.getIntegrationId(), confluenceTimezone.getId());

            String pageContent = confluencePageRenderer.buildPageContent(namedClientData, confluenceTimezone);

            ConfluenceApiClient.ConfluencePublishResult publishResult =
                    confluenceApiClient.createOrUpdatePage(new ConfluencePublishRequest(
                            cmd.getConnectionSecretName(),
                            cmd.getConfluenceSpaceKey(),
                            cmd.getConfluenceSpaceKeyFolderKey(),
                            buildMonitoringPageTitle(cmd, confluenceTimezone),
                            pageContent));

            log.info("Confluence integration {} — published monitoring page", cmd.getIntegrationId());

            return ConfluenceJobExecutionResult.success(
                    namedClientData.size(),
                    publishResult.confluencePageUrl(),
                    publishResult.confluencePageId());

        } catch (Exception ex) {
            log.error("Confluence execution failed for integration {}", cmd.getIntegrationId(), ex);
            return ConfluenceJobExecutionResult.failed(ex.getMessage());
        }
    }

    private String buildMonitoringPageTitle(final ConfluenceExecutionCommand cmd, final ZoneId timezone) {
        Instant windowEnd = cmd.getWindowEnd() != null ? cmd.getWindowEnd() : Instant.now();
        String formattedDate = MONITORING_TITLE_FORMATTER.format(windowEnd.atZone(timezone));
        return cmd.getReportNameTemplate().replace("{date}", formattedDate);
    }

    private int toEpochSeconds(final Instant instant) {
        Instant value = instant != null ? instant : Instant.now();
        return Math.toIntExact(value.getEpochSecond());
    }
}
