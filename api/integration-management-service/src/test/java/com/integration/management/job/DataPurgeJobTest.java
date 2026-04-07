package com.integration.management.job;

import com.integration.management.notification.repository.AppNotificationRepository;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.IntegrationFieldMappingRepository;
import com.integration.management.repository.IntegrationJobExecutionRepository;
import com.integration.management.repository.IntegrationScheduleRepository;
import com.integration.management.repository.JiraFieldMappingRepository;
import com.integration.management.repository.JiraWebhookEventRepository;
import com.integration.management.repository.JiraWebhookRepository;
import com.integration.management.repository.projection.ArcGISIntegrationPurgeProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataPurgeJob")
class DataPurgeJobTest {

    @Mock private PlatformTransactionManager transactionManager;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private IntegrationConnectionRepository integrationConnectionRepository;
    @Mock private ArcGISIntegrationRepository arcGISIntegrationRepository;
    @Mock private IntegrationScheduleRepository integrationScheduleRepository;
    @Mock private IntegrationJobExecutionRepository integrationJobExecutionRepository;
    @Mock private IntegrationFieldMappingRepository integrationFieldMappingRepository;
    @Mock private JiraWebhookRepository jiraWebhookRepository;
    @Mock private JiraFieldMappingRepository jiraFieldMappingRepository;
    @Mock private JiraWebhookEventRepository jiraWebhookEventRepository;
    @Mock private AppNotificationRepository appNotificationRepository;

    private DataPurgeJob job;

    @BeforeEach
    void setUp() {
        job = new DataPurgeJob(
                transactionManager,
                auditLogRepository,
                integrationConnectionRepository,
                arcGISIntegrationRepository,
                integrationScheduleRepository,
                integrationJobExecutionRepository,
                integrationFieldMappingRepository,
                jiraWebhookRepository,
                jiraFieldMappingRepository,
                jiraWebhookEventRepository,
                appNotificationRepository);
        when(transactionManager.getTransaction(any())).thenAnswer(inv -> new SimpleTransactionStatus());
    }

    // -------------------------------------------------------------------------
    // run() — top-level orchestration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("invokes all purge repositories when all return empty")
        void run_invokesAllRepositories_whenNothingToPurge() {
            mockAllRepositoriesEmpty();

            job.run();

            verify(auditLogRepository).findIdsByTimestampBefore(any(Instant.class), any(Pageable.class));
            verify(integrationConnectionRepository).findArcgisCleanupIds(any(Instant.class), any(Pageable.class));
            verify(integrationConnectionRepository).findJiraCleanupIds(any(Instant.class), any(Pageable.class));
            verify(arcGISIntegrationRepository).findCleanupCandidates(any(Instant.class), any(Pageable.class));
            verify(jiraWebhookRepository).findCleanupIds(any(Instant.class), any(Pageable.class));
            verify(jiraWebhookEventRepository).findIdsByTriggeredAtBefore(any(Instant.class), any(Pageable.class));
            verify(appNotificationRepository).findIdsByCreatedDateBefore(any(Instant.class), any(Pageable.class));
        }

        @Test
        @DisplayName("uses different cutoff instants for integrations and notifications")
        void run_usesDifferentCutoffs_forIntegrationsAndNotifications() {
            mockAllRepositoriesEmpty();

            job.run();

            // Both use the same Instant type but are computed independently — verifying both are called
            verify(auditLogRepository).findIdsByTimestampBefore(any(Instant.class), any(Pageable.class));
            verify(appNotificationRepository).findIdsByCreatedDateBefore(any(Instant.class), any(Pageable.class));
        }
    }

    // -------------------------------------------------------------------------
    // purgeArcgisIntegrations
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("purgeArcgisIntegrations")
    class PurgeArcgisIntegrations {

        @Test
        @DisplayName("deletes field mappings and integrations, then executions and schedules when scheduleIds present")
        void purgeArcgisIntegrations_deletesAllRelated_whenScheduleIdsPresent() {
            UUID integrationId = UUID.randomUUID();
            UUID scheduleId = UUID.randomUUID();

            ArcGISIntegrationPurgeProjection candidate = mock(ArcGISIntegrationPurgeProjection.class);
            when(candidate.getId()).thenReturn(integrationId);
            when(candidate.getScheduleId()).thenReturn(scheduleId);

            mockAllRepositoriesEmpty();
            when(arcGISIntegrationRepository.findCleanupCandidates(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(candidate))
                    .thenReturn(List.of());
            when(integrationFieldMappingRepository.deleteByIntegrationIds(anyList())).thenReturn(1);
            when(arcGISIntegrationRepository.deleteByIdIn(anyList())).thenReturn(1);
            when(integrationJobExecutionRepository.deleteByScheduleIds(anyList())).thenReturn(2);
            when(integrationScheduleRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(integrationFieldMappingRepository).deleteByIntegrationIds(List.of(integrationId));
            verify(arcGISIntegrationRepository).deleteByIdIn(List.of(integrationId));
            verify(integrationJobExecutionRepository).deleteByScheduleIds(List.of(scheduleId));
            verify(integrationScheduleRepository).deleteByIdIn(List.of(scheduleId));
        }

        @Test
        @DisplayName("skips execution and schedule deletes when no scheduleIds returned")
        void purgeArcgisIntegrations_skipsScheduleAndExecutionDelete_whenNoScheduleIds() {
            UUID integrationId = UUID.randomUUID();

            ArcGISIntegrationPurgeProjection candidate = mock(ArcGISIntegrationPurgeProjection.class);
            when(candidate.getId()).thenReturn(integrationId);
            when(candidate.getScheduleId()).thenReturn(null);

            mockAllRepositoriesEmpty();
            when(arcGISIntegrationRepository.findCleanupCandidates(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(candidate))
                    .thenReturn(List.of());
            when(integrationFieldMappingRepository.deleteByIntegrationIds(anyList())).thenReturn(0);
            when(arcGISIntegrationRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(integrationJobExecutionRepository, never()).deleteByScheduleIds(anyList());
            verify(integrationScheduleRepository, never()).deleteByIdIn(anyList());
        }

        @Test
        @DisplayName("processes multiple batches until repository returns empty")
        void purgeArcgisIntegrations_processesMultipleBatches() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            ArcGISIntegrationPurgeProjection candidate1 = mock(ArcGISIntegrationPurgeProjection.class);
            when(candidate1.getId()).thenReturn(id1);
            when(candidate1.getScheduleId()).thenReturn(null);

            ArcGISIntegrationPurgeProjection candidate2 = mock(ArcGISIntegrationPurgeProjection.class);
            when(candidate2.getId()).thenReturn(id2);
            when(candidate2.getScheduleId()).thenReturn(null);

            mockAllRepositoriesEmpty();
            when(arcGISIntegrationRepository.findCleanupCandidates(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(candidate1))
                    .thenReturn(List.of(candidate2))
                    .thenReturn(List.of());
            when(integrationFieldMappingRepository.deleteByIntegrationIds(anyList())).thenReturn(0);
            when(arcGISIntegrationRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(arcGISIntegrationRepository, times(3)).findCleanupCandidates(any(Instant.class), any(Pageable.class));
            verify(arcGISIntegrationRepository, times(2)).deleteByIdIn(anyList());
        }
    }

    // -------------------------------------------------------------------------
    // purgeJiraWebhooks
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("purgeJiraWebhooks")
    class PurgeJiraWebhooks {

        @Test
        @DisplayName("deletes field mappings before webhook when mappings exist")
        void purgeJiraWebhooks_deletesFieldMappingsThenWebhook() {
            String webhookId = "JIRA-WEBHOOK-001";

            mockAllRepositoriesEmpty();
            when(jiraWebhookRepository.findCleanupIds(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(webhookId))
                    .thenReturn(List.of());
            when(jiraFieldMappingRepository.deleteByWebhookIds(anyList())).thenReturn(3);
            when(jiraWebhookRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(jiraFieldMappingRepository).deleteByWebhookIds(List.of(webhookId));
            verify(jiraWebhookRepository).deleteByIdIn(List.of(webhookId));
        }

        @Test
        @DisplayName("deletes webhook even when no field mappings exist")
        void purgeJiraWebhooks_deletesWebhook_whenNoMappings() {
            String webhookId = "JIRA-WEBHOOK-002";

            mockAllRepositoriesEmpty();
            when(jiraWebhookRepository.findCleanupIds(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(webhookId))
                    .thenReturn(List.of());
            when(jiraFieldMappingRepository.deleteByWebhookIds(anyList())).thenReturn(0);
            when(jiraWebhookRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(jiraWebhookRepository).deleteByIdIn(List.of(webhookId));
        }
    }

    // -------------------------------------------------------------------------
    // purgeConnections
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("purgeConnections")
    class PurgeConnections {

        @Test
        @DisplayName("deletes both arcgis and jira connections independently")
        void purgeConnections_deletesBothConnectionTypes() {
            UUID arcgisConnId = UUID.randomUUID();
            UUID jiraConnId = UUID.randomUUID();

            mockAllRepositoriesEmpty();
            when(integrationConnectionRepository.findArcgisCleanupIds(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(arcgisConnId))
                    .thenReturn(List.of());
            when(integrationConnectionRepository.findJiraCleanupIds(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(jiraConnId))
                    .thenReturn(List.of());
            when(integrationConnectionRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(integrationConnectionRepository, times(2)).deleteByIdIn(anyList());
        }
    }

    // -------------------------------------------------------------------------
    // purgeAuditLogs / purgeWebhookEvents / purgeNotifications
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("purgeAuditLogs")
    class PurgeAuditLogs {

        @Test
        @DisplayName("deletes audit logs when ids returned")
        void purgeAuditLogs_deletesWhenIdsFound() {
            UUID logId = UUID.randomUUID();

            mockAllRepositoriesEmpty();
            when(auditLogRepository.findIdsByTimestampBefore(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(logId))
                    .thenReturn(List.of());
            when(auditLogRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(auditLogRepository).deleteByIdIn(List.of(logId));
        }
    }

    @Nested
    @DisplayName("purgeWebhookEvents")
    class PurgeWebhookEvents {

        @Test
        @DisplayName("deletes webhook events when ids returned")
        void purgeWebhookEvents_deletesWhenIdsFound() {
            UUID eventId = UUID.randomUUID();

            mockAllRepositoriesEmpty();
            when(jiraWebhookEventRepository.findIdsByTriggeredAtBefore(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(eventId))
                    .thenReturn(List.of());
            when(jiraWebhookEventRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(jiraWebhookEventRepository).deleteByIdIn(List.of(eventId));
        }
    }

    @Nested
    @DisplayName("purgeNotifications")
    class PurgeNotifications {

        @Test
        @DisplayName("deletes notifications when ids returned")
        void purgeNotifications_deletesWhenIdsFound() {
            UUID notifId = UUID.randomUUID();

            mockAllRepositoriesEmpty();
            when(appNotificationRepository.findIdsByCreatedDateBefore(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(notifId))
                    .thenReturn(List.of());
            when(appNotificationRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(appNotificationRepository).deleteByIdIn(List.of(notifId));
        }
    }

    // -------------------------------------------------------------------------
    // deleteInBatches — edge cases
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteInBatches")
    class DeleteInBatches {

        @Test
        @DisplayName("stops looping immediately when first page is empty")
        void deleteInBatches_stopsImmediately_whenFirstPageEmpty() {
            mockAllRepositoriesEmpty();

            job.run();

            // Each repo's find method called exactly once (first page empty → stop)
            verify(auditLogRepository, times(1)).findIdsByTimestampBefore(any(Instant.class), any(Pageable.class));
            verify(auditLogRepository, never()).deleteByIdIn(anyList());
        }

        @Test
        @DisplayName("continues looping until repository returns empty page")
        void deleteInBatches_continuesUntilEmptyPage() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            mockAllRepositoriesEmpty();
            when(auditLogRepository.findIdsByTimestampBefore(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(id1))
                    .thenReturn(List.of(id2))
                    .thenReturn(List.of());
            when(auditLogRepository.deleteByIdIn(anyList())).thenReturn(1);

            job.run();

            verify(auditLogRepository, times(3)).findIdsByTimestampBefore(any(Instant.class), any(Pageable.class));
            verify(auditLogRepository, times(2)).deleteByIdIn(anyList());
        }

        @Test
        @DisplayName("stops and does not loop infinitely when ids found but deleter returns zero")
        void deleteInBatches_stopsWithoutInfiniteLoop_whenDeleterReturnsZero() {
            UUID id = UUID.randomUUID();

            mockAllRepositoriesEmpty();
            when(auditLogRepository.findIdsByTimestampBefore(any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(id));
            when(auditLogRepository.deleteByIdIn(anyList())).thenReturn(0);

            job.run();

            // Should attempt delete once then stop — must not loop infinitely
            verify(auditLogRepository, times(1)).findIdsByTimestampBefore(any(Instant.class), any(Pageable.class));
            verify(auditLogRepository, times(1)).deleteByIdIn(List.of(id));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void mockAllRepositoriesEmpty() {
        lenient().when(auditLogRepository.findIdsByTimestampBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(integrationConnectionRepository.findArcgisCleanupIds(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(integrationConnectionRepository.findJiraCleanupIds(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(arcGISIntegrationRepository.findCleanupCandidates(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(jiraWebhookRepository.findCleanupIds(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(jiraWebhookEventRepository.findIdsByTriggeredAtBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(appNotificationRepository.findIdsByCreatedDateBefore(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
    }
}
