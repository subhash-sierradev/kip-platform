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
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the Confluence monitoring report generation pipeline:
 * Kw fetch → page render → publish English page → translate + publish per target language.
 *
 * <h3>Multi-language strategy</h3>
 * <ol>
 *   <li>The English (source) page is <em>always</em> published first with the unmodified title.</li>
 *   <li>For each target language code in {@code languageCodes} that differs from
 *       {@code sourceLanguage}, the English XHTML is translated at the text-node level
 *       and published as a separate page titled {@code "<baseTitle> [<LANG>]"}.</li>
 *   <li>If a per-language publish fails, a warning is logged and the pipeline continues
 *       with the remaining languages — the job is not failed.</li>
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
    private final ConfluenceTranslationStep confluenceTranslationStep;

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

            // FreeMarker renders always in English (source language)
            String englishContent = confluencePageRenderer.buildPageContent(
                    monitoringData, confluenceTimezone);

            String baseTitle = buildMonitoringPageTitle(cmd, confluenceTimezone);
            String sourceLanguage = resolveSource(cmd.getSourceLanguage());
            List<String> languageCodes = cmd.getLanguageCodes() != null
                    ? cmd.getLanguageCodes() : List.of(sourceLanguage);

            List<PublishedPage> publishedPages = new ArrayList<>();

            // ── 1. Always publish the English (source) page ──────────────────
            ConfluenceApiClient.ConfluencePublishResult englishResult =
                    confluenceApiClient.createOrUpdatePage(new ConfluencePublishRequest(
                            cmd.getConnectionSecretName(),
                            cmd.getConfluenceSpaceKey(),
                            cmd.getConfluenceSpaceKeyFolderKey(),
                            baseTitle,
                            englishContent));
            publishedPages.add(new PublishedPage(sourceLanguage,
                    englishResult.confluencePageUrl(), englishResult.confluencePageId()));
            log.info("Confluence integration {} — published [{}] page: {}",
                    cmd.getIntegrationId(), sourceLanguage.toUpperCase(), baseTitle);

            // ── 2. Publish one translated page per non-source language ────────
            for (String langCode : languageCodes) {
                if (langCode == null || langCode.isBlank()
                        || langCode.equalsIgnoreCase(sourceLanguage)) {
                    continue; // English page already published above
                }
                publishTranslatedPage(cmd, englishContent, sourceLanguage,
                        langCode, baseTitle, publishedPages);
            }

            log.info("Confluence integration {} — published {} page(s): {}",
                    cmd.getIntegrationId(), publishedPages.size(),
                    publishedPages.stream().map(PublishedPage::languageCode).toList());

            return ConfluenceJobExecutionResult.success(monitoringData.size(), publishedPages);

        } catch (Exception ex) {
            log.error("Confluence execution failed for integration {}", cmd.getIntegrationId(), ex);
            return ConfluenceJobExecutionResult.failed(ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void publishTranslatedPage(final ConfluenceExecutionCommand cmd,
                                        final String englishContent,
                                        final String sourceLanguage,
                                        final String langCode,
                                        final String baseTitle,
                                        final List<PublishedPage> publishedPages) {
        try {
            String translatedContent = confluenceTranslationStep.translate(
                    englishContent, sourceLanguage, langCode);

            String langTitle = baseTitle + " [" + langCode.toUpperCase() + "]";

            ConfluenceApiClient.ConfluencePublishResult langResult =
                    confluenceApiClient.createOrUpdatePage(new ConfluencePublishRequest(
                            cmd.getConnectionSecretName(),
                            cmd.getConfluenceSpaceKey(),
                            cmd.getConfluenceSpaceKeyFolderKey(),
                            langTitle,
                            translatedContent));

            publishedPages.add(new PublishedPage(langCode,
                    langResult.confluencePageUrl(), langResult.confluencePageId()));
            log.info("Confluence integration {} — published [{}] page: {}",
                    cmd.getIntegrationId(), langCode.toUpperCase(), langTitle);

        } catch (Exception ex) {
            log.warn("Confluence integration {} — failed to publish [{}] page; skipping. Error: {}",
                    cmd.getIntegrationId(), langCode.toUpperCase(), ex.getMessage());
        }
    }

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
