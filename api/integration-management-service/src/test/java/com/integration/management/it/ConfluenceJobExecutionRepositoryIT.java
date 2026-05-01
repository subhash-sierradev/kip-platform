package com.integration.management.it;

import com.integration.execution.contract.model.enums.FetchMode;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationConnection;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.repository.ConfluenceIntegrationRepository;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.IntegrationJobExecutionRepository;
import com.integration.management.repository.IntegrationScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Confluence Job Execution Repository — integration tests")
class ConfluenceJobExecutionRepositoryIT extends AbstractImsIT {

    private static final String TENANT = "GLOBAL";
    private static final String OTHER_TENANT = "OTHER";
    private static final String USER = "system";

    @Autowired
    private IntegrationJobExecutionRepository jobExecutionRepository;

    @Autowired
    private ConfluenceIntegrationRepository confluenceIntegrationRepository;

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Autowired
    private IntegrationScheduleRepository scheduleRepository;

    private final List<UUID> createdExecutionIds = new ArrayList<>();
    private final List<UUID> createdIntegrationIds = new ArrayList<>();
    private final List<UUID> createdConnectionIds = new ArrayList<>();

    private IntegrationConnection savedConnection;
    private IntegrationSchedule savedSchedule;
    private ConfluenceIntegration savedIntegration;

    @BeforeEach
    void setUp() {
        createdExecutionIds.clear();
        createdIntegrationIds.clear();
        createdConnectionIds.clear();

        savedConnection = connectionRepository.save(
                IntegrationConnection.builder()
                        .id(UUID.randomUUID())
                        .name("Confluence IT Connection")
                        .secretName("confluence-it-secret-" + UUID.randomUUID())
                        .serviceType(ServiceType.CONFLUENCE)
                        .fetchMode(FetchMode.POST)
                        .connectionHashKey("hash-" + UUID.randomUUID())
                        .tenantId(TENANT)
                        .createdBy(USER)
                        .lastModifiedBy(USER)
                        .build());
        createdConnectionIds.add(savedConnection.getId());

        savedSchedule = scheduleRepository.save(
                IntegrationSchedule.builder()
                        .executionTime(LocalTime.of(8, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .dailyExecutionInterval(1)
                        .timeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL)
                        .businessTimeZone("UTC")
                        .cronExpression("0 0 8 * * ?")
                        .isExecuteOnMonthEnd(false)
                        .build());

        savedIntegration = confluenceIntegrationRepository.save(
                ConfluenceIntegration.builder()
                        .id(UUID.randomUUID())
                        .name("Confluence IT Integration")
                        .normalizedName("confluence-it-integration")
                        .documentItemSubtype("REPORT")
                        .reportNameTemplate("IT Report {date}")
                        .confluenceSpaceKey("IT")
                        .connectionId(savedConnection.getId())
                        .schedule(savedSchedule)
                        .isEnabled(true)
                        .tenantId(TENANT)
                        .createdBy(USER)
                        .lastModifiedBy(USER)
                        .build());
        createdIntegrationIds.add(savedIntegration.getId());
    }

    @AfterEach
    void tearDown() {
        jobExecutionRepository.deleteAllById(createdExecutionIds);
        createdIntegrationIds.forEach(id ->
                confluenceIntegrationRepository.findById(id).ifPresent(i -> {
                    i.setIsDeleted(true);
                    confluenceIntegrationRepository.save(i);
                }));
        createdConnectionIds.forEach(id ->
                connectionRepository.findById(id).ifPresent(c -> {
                    c.setIsDeleted(true);
                    connectionRepository.save(c);
                }));
    }

    private IntegrationJobExecution saveExecution(Instant startedAt, UUID originalJobId,
            int retryAttempt) {
        IntegrationJobExecution execution = jobExecutionRepository.save(
                IntegrationJobExecution.builder()
                        .scheduleId(savedSchedule.getId())
                        .triggeredBy(TriggerType.SCHEDULER)
                        .status(JobExecutionStatus.SUCCESS)
                        .startedAt(startedAt)
                        .originalJobId(originalJobId)
                        .retryAttempt(retryAttempt)
                        .build());
        createdExecutionIds.add(execution.getId());
        return execution;
    }

    private IntegrationJobExecution saveExecution(Instant startedAt) {
        UUID selfId = UUID.randomUUID();
        return saveExecution(startedAt, selfId, 0);
    }

    @Nested
    @DisplayName("Result ordering")
    class ResultOrdering {

        @Test
        @DisplayName("returns executions in descending started_at order (latest first)")
        void returnsExecutionsInDescendingStartedAtOrder() {
            Instant base = Instant.parse("2026-01-10T10:00:00Z");
            IntegrationJobExecution oldest = saveExecution(base);
            IntegrationJobExecution middle = saveExecution(base.plus(1, ChronoUnit.HOURS));
            IntegrationJobExecution newest = saveExecution(base.plus(2, ChronoUnit.HOURS));

            List<IntegrationJobExecution> result =
                    jobExecutionRepository.findByConfluenceIntegrationAndTenant(
                            savedIntegration.getId(), TENANT);

            assertThat(result)
                    .extracting(IntegrationJobExecution::getId)
                    .containsExactly(newest.getId(), middle.getId(), oldest.getId());
        }

        @Test
        @DisplayName("stable ordering by id DESC when two executions share the same started_at")
        void stableOrderByIdWhenStartedAtIsIdentical() {
            Instant sameTime = Instant.parse("2026-01-10T10:00:00Z");
            // PostgreSQL orders UUIDs lexicographically as text; '00000000...' < 'ffffffff...'
            // so 'ffffffff-...' wins in DESC order.
            UUID lowUuid   = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID highUuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

            IntegrationJobExecution execLow = jobExecutionRepository.save(
                    IntegrationJobExecution.builder()
                            .id(lowUuid)
                            .scheduleId(savedSchedule.getId())
                            .triggeredBy(TriggerType.SCHEDULER)
                            .status(JobExecutionStatus.SUCCESS)
                            .startedAt(sameTime)
                            .originalJobId(lowUuid)
                            .retryAttempt(0)
                            .build());
            createdExecutionIds.add(execLow.getId());

            IntegrationJobExecution execHigh = jobExecutionRepository.save(
                    IntegrationJobExecution.builder()
                            .id(highUuid)
                            .scheduleId(savedSchedule.getId())
                            .triggeredBy(TriggerType.SCHEDULER)
                            .status(JobExecutionStatus.SUCCESS)
                            .startedAt(sameTime)
                            .originalJobId(highUuid)
                            .retryAttempt(0)
                            .build());
            createdExecutionIds.add(execHigh.getId());

            List<IntegrationJobExecution> result =
                    jobExecutionRepository.findByConfluenceIntegrationAndTenant(
                            savedIntegration.getId(), TENANT);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(highUuid);
            assertThat(result.get(1).getId()).isEqualTo(lowUuid);
        }
    }

    @Nested
    @DisplayName("Retry deduplication")
    class RetryDeduplication {

        @Test
        @DisplayName("returns only the latest retry attempt for each original_job_id")
        void returnsOnlyLatestRetryPerOriginalJob() {
            Instant base = Instant.parse("2026-01-10T10:00:00Z");
            UUID originalJobId = UUID.randomUUID();

            saveExecution(base, originalJobId, 0);
            IntegrationJobExecution latestRetry = saveExecution(base.plus(30, ChronoUnit.MINUTES),
                    originalJobId, 1);

            List<IntegrationJobExecution> result =
                    jobExecutionRepository.findByConfluenceIntegrationAndTenant(
                            savedIntegration.getId(), TENANT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(latestRetry.getId());
            assertThat(result.get(0).getRetryAttempt()).isEqualTo(1);
        }

        @Test
        @DisplayName("multiple original jobs each deduplicated to their latest retry, sorted by started_at DESC")
        void multipleOriginalJobsDeduplicatedAndSorted() {
            Instant base = Instant.parse("2026-01-10T10:00:00Z");

            UUID job1 = UUID.randomUUID();
            saveExecution(base, job1, 0);
            IntegrationJobExecution job1Latest = saveExecution(
                    base.plus(10, ChronoUnit.MINUTES), job1, 1);

            UUID job2 = UUID.randomUUID();
            IntegrationJobExecution job2Only = saveExecution(
                    base.plus(2, ChronoUnit.HOURS), job2, 0);

            List<IntegrationJobExecution> result =
                    jobExecutionRepository.findByConfluenceIntegrationAndTenant(
                            savedIntegration.getId(), TENANT);

            assertThat(result)
                    .extracting(IntegrationJobExecution::getId)
                    .containsExactly(job2Only.getId(), job1Latest.getId());
        }
    }

    @Nested
    @DisplayName("Tenant and soft-delete isolation")
    class Isolation {

        @Test
        @DisplayName("excludes executions linked to a different tenant's integration")
        void excludesDifferentTenantExecutions() {
            IntegrationConnection otherConn = connectionRepository.save(
                    IntegrationConnection.builder()
                            .id(UUID.randomUUID())
                            .name("Other Tenant Connection")
                            .secretName("other-secret-" + UUID.randomUUID())
                            .serviceType(ServiceType.CONFLUENCE)
                            .fetchMode(FetchMode.POST)
                            .connectionHashKey("hash-other-" + UUID.randomUUID())
                            .tenantId(OTHER_TENANT)
                            .createdBy(USER)
                            .lastModifiedBy(USER)
                            .build());
            createdConnectionIds.add(otherConn.getId());

            IntegrationSchedule otherSchedule = scheduleRepository.save(
                    IntegrationSchedule.builder()
                            .executionTime(LocalTime.of(9, 0))
                            .frequencyPattern(FrequencyPattern.DAILY)
                            .dailyExecutionInterval(1)
                            .timeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL)
                            .businessTimeZone("UTC")
                            .cronExpression("0 0 9 * * ?")
                            .isExecuteOnMonthEnd(false)
                            .build());

            ConfluenceIntegration otherIntegration = confluenceIntegrationRepository.save(
                    ConfluenceIntegration.builder()
                            .id(UUID.randomUUID())
                            .name("Other Tenant Integration")
                            .normalizedName("other-tenant-integration")
                            .documentItemSubtype("REPORT")
                            .reportNameTemplate("Other Report {date}")
                            .confluenceSpaceKey("OTHER")
                            .connectionId(otherConn.getId())
                            .schedule(otherSchedule)
                            .isEnabled(true)
                            .tenantId(OTHER_TENANT)
                            .createdBy(USER)
                            .lastModifiedBy(USER)
                            .build());
            createdIntegrationIds.add(otherIntegration.getId());

            IntegrationJobExecution otherExec = jobExecutionRepository.save(
                    IntegrationJobExecution.builder()
                            .scheduleId(otherSchedule.getId())
                            .triggeredBy(TriggerType.SCHEDULER)
                            .status(JobExecutionStatus.SUCCESS)
                            .startedAt(Instant.parse("2026-01-10T10:00:00Z"))
                            .originalJobId(UUID.randomUUID())
                            .retryAttempt(0)
                            .build());
            createdExecutionIds.add(otherExec.getId());

            IntegrationJobExecution ownExec = saveExecution(Instant.parse("2026-01-10T11:00:00Z"));

            List<IntegrationJobExecution> result =
                    jobExecutionRepository.findByConfluenceIntegrationAndTenant(
                            savedIntegration.getId(), TENANT);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(ownExec.getId());

            List<IntegrationJobExecution> crossTenantResult =
                    jobExecutionRepository.findByConfluenceIntegrationAndTenant(
                            savedIntegration.getId(), OTHER_TENANT);
            assertThat(crossTenantResult).isEmpty();
        }

        @Test
        @DisplayName("excludes executions of a soft-deleted integration")
        void excludesSoftDeletedIntegrationExecutions() {
            saveExecution(Instant.parse("2026-01-10T10:00:00Z"));

            savedIntegration.setIsDeleted(true);
            confluenceIntegrationRepository.save(savedIntegration);

            List<IntegrationJobExecution> result =
                    jobExecutionRepository.findByConfluenceIntegrationAndTenant(
                            savedIntegration.getId(), TENANT);

            assertThat(result).isEmpty();
        }
    }
}
