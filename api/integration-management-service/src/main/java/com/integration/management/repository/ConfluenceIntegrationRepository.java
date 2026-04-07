package com.integration.management.repository;

import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.repository.projection.ConfluenceIntegrationSummaryProjection;
import com.integration.management.repository.projection.UuidIdSummaryWithLastRunProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfluenceIntegrationRepository extends JpaRepository<ConfluenceIntegration, UUID> {

    @Query("SELECT ci FROM ConfluenceIntegration ci "
            + "WHERE ci.id = :id AND ci.tenantId = :tenantId AND ci.isDeleted = false")
    Optional<ConfluenceIntegration> findByIdAndTenantIdAndIsDeletedFalse(
            @Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("SELECT ci FROM ConfluenceIntegration ci JOIN FETCH ci.schedule "
            + "WHERE ci.id = :id AND ci.tenantId = :tenantId AND ci.isDeleted = false")
    Optional<ConfluenceIntegration> findByIdAndTenantIdAndIsDeletedFalseWithSchedule(
            @Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("SELECT ci FROM ConfluenceIntegration ci JOIN FETCH ci.schedule "
            + "WHERE ci.id = :id AND ci.tenantId = :tenantId "
            + "AND ci.isDeleted = false AND ci.isEnabled = true")
    Optional<ConfluenceIntegration> findEnabledByIdAndTenantIdWithSchedule(
            @Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("SELECT ci FROM ConfluenceIntegration ci JOIN FETCH ci.schedule "
            + "WHERE ci.isDeleted = false AND ci.isEnabled = true AND ci.schedule IS NOT NULL")
    List<ConfluenceIntegration> findAllIntegrationsWithActiveSchedules();

    @Query("SELECT ci.name FROM ConfluenceIntegration ci WHERE ci.id = :id")
    Optional<String> findIntegrationNameById(@Param("id") UUID id);

    @Query("SELECT ci.normalizedName FROM ConfluenceIntegration ci "
            + "WHERE ci.tenantId = :tenantId AND ci.isDeleted = false")
    List<String> findAllNormalizedNamesByTenantId(@Param("tenantId") String tenantId);

    @Query(value = """
            SELECT ci.id AS id, ci.name AS name, ci.is_enabled AS isEnabled,
                   ci.description AS description, latest_exec.started_at AS lastRunAt
            FROM integration_platform.confluence_integrations ci
            LEFT JOIN LATERAL (
                SELECT ije.started_at
                FROM integration_platform.integration_job_executions ije
                WHERE ije.schedule_id = ci.schedule_id
                ORDER BY ije.started_at DESC
                LIMIT 1
            ) latest_exec ON true
            WHERE ci.tenant_id = :tenantId AND ci.connection_id = :connectionId
              AND ci.is_deleted = false
            """, nativeQuery = true)
    List<UuidIdSummaryWithLastRunProjection> findNonDeletedSummariesByConnectionIdAndTenantId(
            @Param("connectionId") UUID connectionId,
            @Param("tenantId") String tenantId);

    @Query(value = """
            SELECT
                ci.id AS id,
                ci.name AS name,
                ci.document_item_type AS documentItemType,
                ci.document_item_subtype AS documentItemSubtype,
                ci.dynamic_document_type AS dynamicDocumentType,
                ci.confluence_space_key AS confluenceSpaceKey,
                s.frequency_pattern AS frequencyPattern,
                s.daily_execution_interval AS dailyExecutionInterval,
                s.execution_date AS executionDate,
                s.execution_time AS executionTime,
                s.day_schedule::text AS daySchedule,
                s.month_schedule::text AS monthSchedule,
                s.is_execute_on_month_end AS isExecuteOnMonthEnd,
                s.cron_expression AS cronExpression,
                ci.created_date AS createdDate,
                ci.created_by AS createdBy,
                ci.last_modified_date AS lastModifiedDate,
                ci.last_modified_by AS lastModifiedBy,
                ci.is_enabled AS isEnabled,
                latest_exec.started_at AS lastAttemptTimeUtc,
                latest_exec.status AS lastStatus
            FROM integration_platform.confluence_integrations ci
            LEFT JOIN integration_platform.integration_schedules s ON ci.schedule_id = s.id
            LEFT JOIN LATERAL (
                SELECT
                    ije.started_at,
                    ije.status
                FROM integration_platform.integration_job_executions ije
                WHERE ije.schedule_id = s.id
                ORDER BY ije.started_at DESC
                LIMIT 1
            ) latest_exec ON true
            WHERE ci.tenant_id = :tenantId
              AND ci.is_deleted = false
            ORDER BY ci.created_date DESC
            """, nativeQuery = true)
    List<ConfluenceIntegrationSummaryProjection> findAllSummariesByTenantId(@Param("tenantId") String tenantId);
}
