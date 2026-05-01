package com.integration.management.repository;

import com.integration.management.entity.IntegrationJobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for IntegrationJobExecution entities
 */
@Repository
public interface IntegrationJobExecutionRepository
        extends JpaRepository<IntegrationJobExecution, UUID> {
    IntegrationJobExecution findTopByScheduleIdOrderByStartedAtDesc(
            UUID scheduleId);

    IntegrationJobExecution
            findTopByScheduleIdAndIdNotOrderByStartedAtDesc(
                    UUID scheduleId, UUID id);

    Optional<IntegrationJobExecution> findTopByOriginalJobIdOrderByRetryAttemptDesc(
            UUID originalJobId);

    @Query("SELECT e FROM IntegrationJobExecution e "
            + "JOIN ConfluenceIntegration ci ON ci.schedule.id = e.scheduleId "
            + "WHERE e.id = :executionId "
            + "  AND ci.id = :integrationId "
            + "  AND ci.tenantId = :tenantId "
            + "  AND ci.isDeleted = false")
    Optional<IntegrationJobExecution> findConfluenceExecutionByIdAndIntegrationAndTenant(
            @Param("executionId") UUID executionId,
            @Param("integrationId") UUID integrationId,
            @Param("tenantId") String tenantId);

    @Query("SELECT e FROM IntegrationJobExecution e "
            + "JOIN ConfluenceIntegration ci ON ci.schedule.id = e.scheduleId "
            + "WHERE e.originalJobId = :originalJobId "
            + "  AND ci.id = :integrationId "
            + "  AND ci.tenantId = :tenantId "
            + "  AND ci.isDeleted = false "
            + "  AND e.status IN ('FAILED', 'ABORTED') "
            + "ORDER BY e.retryAttempt DESC LIMIT 1")
    Optional<IntegrationJobExecution> findConfluenceLatestRetriableExecutionByOriginalJobId(
            @Param("originalJobId") UUID originalJobId,
            @Param("integrationId") UUID integrationId,
            @Param("tenantId") String tenantId);

    @Query("SELECT e FROM IntegrationJobExecution e "
            + "JOIN ArcGISIntegration ai ON ai.schedule.id = e.scheduleId "
            + "WHERE ai.id = :integrationId "
            + "  AND ai.tenantId = :tenantId "
            + "  AND ai.isDeleted = false")
    List<IntegrationJobExecution> findByIntegrationAndTenant(
            @Param("integrationId") UUID integrationId,
            @Param("tenantId") String tenantId,
            Sort sort);

    @Query(value = "SELECT * FROM ("
            + "SELECT DISTINCT ON (e.original_job_id) e.* "
            + "FROM integration_platform.integration_job_executions e "
            + "JOIN integration_platform.confluence_integrations ci ON ci.schedule_id = e.schedule_id "
            + "WHERE ci.id = :integrationId "
            + "  AND ci.tenant_id = :tenantId "
            + "  AND ci.is_deleted = false "
            + "ORDER BY e.original_job_id, e.retry_attempt DESC, e.started_at DESC"
            + ") latest_executions "
            + "ORDER BY latest_executions.started_at DESC, latest_executions.id DESC",
            nativeQuery = true)
    List<IntegrationJobExecution> findByConfluenceIntegrationAndTenant(
            @Param("integrationId") UUID integrationId,
            @Param("tenantId") String tenantId);

    @Modifying
    @Query("DELETE FROM IntegrationJobExecution e WHERE e.scheduleId IN :scheduleIds")
    int deleteByScheduleIds(@Param("scheduleIds") List<UUID> scheduleIds);
}
