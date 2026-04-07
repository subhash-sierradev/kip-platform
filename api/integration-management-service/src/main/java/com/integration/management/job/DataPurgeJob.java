package com.integration.management.job;

import com.integration.management.notification.repository.AppNotificationRepository;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.repository.projection.ArcGISIntegrationPurgeProjection;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.IntegrationFieldMappingRepository;
import com.integration.management.repository.IntegrationJobExecutionRepository;
import com.integration.management.repository.IntegrationScheduleRepository;
import com.integration.management.repository.JiraFieldMappingRepository;
import com.integration.management.repository.JiraWebhookEventRepository;
import com.integration.management.repository.JiraWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DataPurgeJob implements Job {

    private static final int INTEGRATION_RETENTION_MONTHS = 6;
    private static final int NOTIFICATION_RETENTION_MONTHS = 3;
    private static final int BATCH_SIZE = 500;

    private final PlatformTransactionManager transactionManager;
    private final AuditLogRepository auditLogRepository;
    private final IntegrationConnectionRepository integrationConnectionRepository;
    private final ArcGISIntegrationRepository arcGISIntegrationRepository;
    private final IntegrationScheduleRepository integrationScheduleRepository;
    private final IntegrationJobExecutionRepository integrationJobExecutionRepository;
    private final IntegrationFieldMappingRepository integrationFieldMappingRepository;
    private final JiraWebhookRepository jiraWebhookRepository;
    private final JiraFieldMappingRepository jiraFieldMappingRepository;
    private final JiraWebhookEventRepository jiraWebhookEventRepository;
    private final AppNotificationRepository appNotificationRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        run();
    }

    void run() {
        Instant startedAt = Instant.now();
        Instant integrationCutoff = ZonedDateTime.now(ZoneOffset.UTC)
                .minusMonths(INTEGRATION_RETENTION_MONTHS)
                .toInstant();
        Instant notificationCutoff = ZonedDateTime.now(ZoneOffset.UTC)
                .minusMonths(NOTIFICATION_RETENTION_MONTHS)
                .toInstant();

        log.info("Starting data purge with integrationCutoff={} notificationCutoff={} batchSize={}",
                integrationCutoff, notificationCutoff, BATCH_SIZE);

        long deletedArcgisIntegrations = purgeArcgisIntegrations(integrationCutoff);
        long deletedJiraWebhooks = purgeJiraWebhooks(integrationCutoff);
        long deletedAuditLogs = purgeAuditLogs(integrationCutoff);
        long deletedWebhookEvents = purgeWebhookEvents(integrationCutoff);
        long deletedConnections = purgeConnections(integrationCutoff);
        long deletedNotifications = purgeNotifications(notificationCutoff);

        log.info("Data purge completed in {} ms. arcgisIntegrations={} connections={} "
                        + "jiraWebhooks={} auditLogs={} webhookEvents={} appNotifications={}",
                ChronoUnit.MILLIS.between(startedAt, Instant.now()),
                deletedArcgisIntegrations, deletedConnections, deletedJiraWebhooks,
                deletedAuditLogs, deletedWebhookEvents, deletedNotifications);
    }

    private long purgeAuditLogs(Instant cutoff) {
        return deleteInBatches("audit_logs",
                () -> auditLogRepository.findIdsByTimestampBefore(cutoff, PageRequest.of(0, BATCH_SIZE)),
                auditLogRepository::deleteByIdIn);
    }

    private long purgeConnections(Instant cutoff) {
        long deletedArcgis = deleteInBatches("integration_connections (arcgis)",
                () -> integrationConnectionRepository.findArcgisCleanupIds(cutoff, PageRequest.of(0, BATCH_SIZE)),
                integrationConnectionRepository::deleteByIdIn);
        long deletedJira = deleteInBatches("integration_connections (jira)",
                () -> integrationConnectionRepository.findJiraCleanupIds(cutoff, PageRequest.of(0, BATCH_SIZE)),
                integrationConnectionRepository::deleteByIdIn);
        return deletedArcgis + deletedJira;
    }

    private long purgeArcgisIntegrations(Instant cutoff) {
        return deleteInBatches("arcgis_integrations",
                () -> arcGISIntegrationRepository.findCleanupCandidates(cutoff, PageRequest.of(0, BATCH_SIZE)),
                projections -> {
                    List<UUID> ids = projections.stream()
                            .map(ArcGISIntegrationPurgeProjection::getId).toList();
                    List<UUID> scheduleIds = projections.stream()
                            .map(ArcGISIntegrationPurgeProjection::getScheduleId)
                            .filter(Objects::nonNull).distinct().toList();
                    integrationFieldMappingRepository.deleteByIntegrationIds(ids);
                    int deleted = arcGISIntegrationRepository.deleteByIdIn(ids);
                    if (!scheduleIds.isEmpty()) {
                        integrationJobExecutionRepository.deleteByScheduleIds(scheduleIds);
                        integrationScheduleRepository.deleteByIdIn(scheduleIds);
                    }
                    return deleted;
                });
    }

    private long purgeJiraWebhooks(Instant cutoff) {
        return deleteInBatches("jira_webhooks",
                () -> jiraWebhookRepository.findCleanupIds(cutoff, PageRequest.of(0, BATCH_SIZE)),
                ids -> {
                    jiraFieldMappingRepository.deleteByWebhookIds(ids);
                    return jiraWebhookRepository.deleteByIdIn(ids);
                });
    }

    private long purgeWebhookEvents(Instant cutoff) {
        return deleteInBatches("jira_webhook_events",
                () -> jiraWebhookEventRepository.findIdsByTriggeredAtBefore(cutoff, PageRequest.of(0, BATCH_SIZE)),
                jiraWebhookEventRepository::deleteByIdIn);
    }

    private long purgeNotifications(Instant cutoff) {
        return deleteInBatches("app_notifications",
                () -> appNotificationRepository.findIdsByCreatedDateBefore(cutoff, PageRequest.of(0, BATCH_SIZE)),
                appNotificationRepository::deleteByIdIn);
    }

    private <T> long deleteInBatches(String tableName, Supplier<List<T>> idSupplier, Deleter<T> deleter) {
        record BatchResult(int idsFound, int deleted) {
        }

        long totalDeleted = 0L;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        while (true) {
            BatchResult result = template.execute(status -> {
                List<T> ids = idSupplier.get();
                if (ids.isEmpty()) {
                    return new BatchResult(0, 0);
                }
                return new BatchResult(ids.size(), deleter.delete(ids));
            });

            if (result == null || result.deleted() == 0) {
                if (result != null && result.idsFound() > 0) {
                    log.warn("Partial purge detected on {}: {} ids fetched but 0 rows deleted. "
                                    + "Possible concurrent deletion or constraint mismatch. Stopping batch.",
                            tableName, result.idsFound());
                }
                break;
            }
            totalDeleted += result.deleted();
        }

        if (totalDeleted > 0) {
            log.info("Deleted {} rows from {}", totalDeleted, tableName);
        }
        return totalDeleted;
    }

    @FunctionalInterface
    private interface Deleter<T> {
        int delete(List<T> ids);
    }
}
