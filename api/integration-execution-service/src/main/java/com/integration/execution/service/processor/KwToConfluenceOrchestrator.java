package com.integration.execution.service.processor;

import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.client.ConfluenceApiClient.ConfluencePublishRequest;
import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.model.ConfluenceJobExecutionResult;
import com.integration.execution.model.ConfluenceJobExecutionResult.PublishedPage;
import com.integration.execution.model.KwMonitoringDocument;
import com.integration.execution.service.KwGraphQLService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Orchestrates the Confluence monitoring report generation pipeline:
 * Kw fetch → translate data → render combined multi-language page → publish.
 *
 * <h3>Multi-language strategy</h3>
 * <ol>
 *   <li>The English (source) records are always rendered first in the page.</li>
 *   <li>For each target language code in {@code languageCodes} that differs from
 *       {@code sourceLanguage}, the raw monitoring data is translated at the field level
 *       by {@link KwMonitoringDataTranslator} and added as an additional section in the
 *       <em>same</em> Confluence page — so a single page contains all language variants.</li>
 *   <li>If translation fails for a language the original English content is used as a
 *       fallback for that section; the job never fails due to translation errors.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KwToConfluenceOrchestrator {

    private static final int DEFAULT_MONITORING_OFFSET = 0;
    private static final int DEFAULT_MONITORING_LIMIT = 500;
    private static final String DEFAULT_SOURCE_LANGUAGE = "en";
    private static final DateTimeFormatter MONITORING_TITLE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final KwGraphQLService kwGraphQLService;
    private final ConfluencePageRenderer confluencePageRenderer;
    private final ConfluenceApiClient confluenceApiClient;
    private final KwMonitoringDataTranslator kwMonitoringDataTranslator;

    public ConfluenceJobExecutionResult processExecution(final ConfluenceExecutionCommand cmd) {
        try {
            int startTimestamp = toEpochSeconds(cmd.getWindowStart());
            int endTimestamp = toEpochSeconds(cmd.getWindowEnd());

            List<KwMonitoringDocument> monitoringData = kwGraphQLService.fetchMonitoringData(
                    cmd.getDynamicDocumentType(),
                    startTimestamp, endTimestamp,
                    DEFAULT_MONITORING_OFFSET, DEFAULT_MONITORING_LIMIT);

            log.info("Confluence integration {} — records fetched={} startTs={} endTs={}",
                    cmd.getIntegrationId(), monitoringData.size(), startTimestamp, endTimestamp);

            if (monitoringData.isEmpty()) {
                log.info("Confluence integration {} — no monitoring data found for window",
                        cmd.getIntegrationId());
                return ConfluenceJobExecutionResult.successEmpty();
            }

            ZoneId fallbackTimezone = cmd.getBusinessTimeZone() != null
                    && !cmd.getBusinessTimeZone().isBlank()
                    ? ZoneId.of(cmd.getBusinessTimeZone()) : null;
            ZoneId confluenceTimezone = confluenceApiClient.getUserTimezone(
                    cmd.getConnectionSecretName(), fallbackTimezone);
            log.info("Confluence integration {} — using timezone: {}",
                    cmd.getIntegrationId(), confluenceTimezone.getId());

            String sourceLanguage = resolveSource(cmd.getSourceLanguage());
            List<String> languageCodes = cmd.getLanguageCodes() != null
                    ? cmd.getLanguageCodes() : List.of(sourceLanguage);

            // ── Translate to all non-source languages in one pass ────────────
            List<String> targetLanguages = languageCodes.stream()
                    .filter(l -> l != null && !l.isBlank() && !l.equalsIgnoreCase(sourceLanguage))
                    .distinct()
                    .toList();

            Map<String, List<KwMonitoringDocument>> translatedByLang = targetLanguages.isEmpty()
                    ? Map.of()
                    : kwMonitoringDataTranslator.translateAll(monitoringData, sourceLanguage, targetLanguages);

            // ── Build & publish ONE combined page (English + translated sections) ──
            String baseTitle = buildMonitoringPageTitle(cmd, confluenceTimezone);
            String combinedContent = confluencePageRenderer.buildMultiLanguagePageContent(
                    monitoringData, translatedByLang, sourceLanguage, confluenceTimezone);

            ConfluenceApiClient.ConfluencePublishResult pageResult =
                    confluenceApiClient.createOrUpdatePage(new ConfluencePublishRequest(
                            cmd.getConnectionSecretName(),
                            cmd.getConfluenceSpaceKey(),
                            cmd.getConfluenceSpaceKeyFolderKey(),
                            baseTitle,
                            combinedContent));

            List<PublishedPage> publishedPages = List.of(
                    new PublishedPage(sourceLanguage,
                            pageResult.confluencePageUrl(), pageResult.confluencePageId()));

            log.info("Confluence integration {} — published combined page '{}' with languages: {}",
                    cmd.getIntegrationId(), baseTitle,
                    targetLanguages.isEmpty() ? List.of(sourceLanguage)
                            : Stream.concat(Stream.of(sourceLanguage), targetLanguages.stream()).toList());

            return ConfluenceJobExecutionResult.success(monitoringData.size(), publishedPages);

        } catch (Exception ex) {
            log.error("Confluence execution failed for integration {}", cmd.getIntegrationId(), ex);
            return ConfluenceJobExecutionResult.failed(ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String buildMonitoringPageTitle(final ConfluenceExecutionCommand cmd,
                                            final ZoneId timezone) {
        Instant windowEnd = cmd.getWindowEnd() != null ? cmd.getWindowEnd() : Instant.now();
        String formattedDate = MONITORING_TITLE_FORMATTER.format(windowEnd.atZone(timezone));
        return cmd.getReportNameTemplate().replace("{date}", formattedDate);
    }

    private String resolveSource(final String sourceLanguage) {
        return (sourceLanguage != null && !sourceLanguage.isBlank())
                ? sourceLanguage : DEFAULT_SOURCE_LANGUAGE;
    }

    private int toEpochSeconds(final Instant instant) {
        Instant value = instant != null ? instant : Instant.now();
        return Math.toIntExact(value.getEpochSecond());
    }
}
