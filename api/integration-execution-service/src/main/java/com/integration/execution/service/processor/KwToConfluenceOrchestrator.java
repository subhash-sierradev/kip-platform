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

            log.info("Confluence integration {} — monitoring records fetched={} startTs={} endTs={} start={} limit={}",
                    cmd.getIntegrationId(), monitoringData.size(), startTimestamp, endTimestamp, offset, limit);

            if (monitoringData.isEmpty()) {
                log.info("Confluence integration {} — no monitoring data found for requested window",
                        cmd.getIntegrationId());
                return ConfluenceJobExecutionResult.success(0, null, null);
            }

            List<KwMonitoringDocument> namedClientData = confluencePageRenderer.filterNamedClients(monitoringData);
            log.info("Confluence integration {} \u2014 records after unknown-client filter: fetched={} included={}",
                    cmd.getIntegrationId(), monitoringData.size(), namedClientData.size());

            if (namedClientData.isEmpty()) {
                log.info("Confluence integration {} \u2014 all fetched records have unknown client, no page published",
                        cmd.getIntegrationId());
                return ConfluenceJobExecutionResult.success(0, null, null);
            }

            // Fetch Confluence user timezone and convert monitoring data timestamps
            ZoneId fallbackTimezone = cmd.getBusinessTimezone() != null && !cmd.getBusinessTimezone().isBlank()
                    ? ZoneId.of(cmd.getBusinessTimezone())
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
                            buildMonitoringPageTitle(cmd),
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

    private String buildMonitoringPageTitle(final ConfluenceExecutionCommand cmd) {
        String formattedDate = MONITORING_TITLE_FORMATTER.format(
                Instant.now().atZone(ZoneId.systemDefault()));
        return cmd.getReportNameTemplate().replace("{date}", formattedDate);
    }

    private int toEpochSeconds(final Instant instant) {
        Instant value = instant != null ? instant : Instant.now();
        return Math.toIntExact(value.getEpochSecond());
    }
}
