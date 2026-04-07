package com.integration.management.repository;

import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.repository.projection.ArcGISIntegrationPurgeProjection;
import com.integration.management.repository.projection.ArcGISIntegrationSummaryProjection;
import com.integration.management.repository.projection.UuidIdSummaryWithLastRunProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArcGISIntegrationRepository extends JpaRepository<ArcGISIntegration, UUID> {

    @Query("SELECT ai FROM ArcGISIntegration ai WHERE ai.id = :id "
            + "AND ai.tenantId = :tenantId AND ai.isDeleted = false")
    Optional<ArcGISIntegration> findByIdAndTenantIdAndIsDeletedFalse(UUID id, String tenantId);

    @Query("SELECT ai FROM ArcGISIntegration ai JOIN FETCH ai.schedule "
            + "WHERE ai.id = :id AND ai.tenantId = :tenantId AND ai.isDeleted = false")
    Optional<ArcGISIntegration> findByIdAndTenantIdAndIsDeletedFalseWithSchedule(
            @Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("SELECT ai FROM ArcGISIntegration ai JOIN FETCH ai.schedule "
            + "WHERE ai.isDeleted = false AND ai.isEnabled = true AND ai.schedule IS NOT NULL")
    List<ArcGISIntegration> findAllIntegrationsWithActiveSchedules();

    @Query("select ai.name from ArcGISIntegration ai where ai.id = :id")
    Optional<String> findIntegrationNameById(@Param("id") UUID id);

    @Query("SELECT ai.normalizedName FROM ArcGISIntegration ai WHERE ai.tenantId = :tenantId "
            + " AND ai.isDeleted = false")
    List<String> findAllNormalizedNamesByTenantId(@Param("tenantId") String tenantId);

    @Query(value = """
            SELECT ai.id AS id, ai.name AS name, ai.is_enabled AS isEnabled,
                   ai.description AS description, latest_exec.started_at AS lastRunAt
            FROM integration_platform.arcgis_integrations ai
            LEFT JOIN LATERAL (
                SELECT ije.started_at
                FROM integration_platform.integration_job_executions ije
                WHERE ije.schedule_id = ai.schedule_id
                ORDER BY ije.started_at DESC
                LIMIT 1
            ) latest_exec ON true
            WHERE ai.tenant_id = :tenantId AND ai.connection_id = :connectionId
              AND ai.is_deleted = false
            """, nativeQuery = true)
    List<UuidIdSummaryWithLastRunProjection> findNonDeletedSummariesByConnectionIdAndTenantId(
            @Param("connectionId") UUID connectionId,
            @Param("tenantId") String tenantId);

    @Query(value = """
            SELECT
                ai.id AS id,
                ai.name AS name,
                ai.item_type AS itemType,
                ai.item_subtype AS itemSubtype,
                ai.dynamic_document_type AS dynamicDocumentType,
                s.frequency_pattern AS frequencyPattern,
                s.daily_execution_interval AS dailyExecutionInterval,
                s.execution_date AS executionDate,
                s.execution_time AS executionTime,
                s.day_schedule::text AS daySchedule,
                s.month_schedule::text AS monthSchedule,
                s.is_execute_on_month_end AS isExecuteOnMonthEnd,
                s.cron_expression AS cronExpression,
                s.business_time_zone AS businessTimeZone,
                ai.created_date AS createdDate,
                ai.created_by AS createdBy,
                ai.last_modified_date AS lastModifiedDate,
                ai.last_modified_by AS lastModifiedBy,
                ai.is_enabled AS isEnabled,
                latest_exec.started_at AS lastAttemptTimeUtc,
                latest_exec.status AS lastStatus
            FROM integration_platform.arcgis_integrations ai
            LEFT JOIN integration_platform.integration_schedules s ON ai.schedule_id = s.id
            LEFT JOIN LATERAL (
                SELECT
                    ije.started_at,
                    ije.status
                FROM integration_platform.integration_job_executions ije
                WHERE ije.schedule_id = s.id
                ORDER BY ije.started_at DESC
                LIMIT 1
            ) latest_exec ON true
            WHERE ai.tenant_id = :tenantId
              AND ai.is_deleted = false
            ORDER BY ai.created_date DESC
            """, nativeQuery = true)
    List<ArcGISIntegrationSummaryProjection> findAllSummariesByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT ai.id AS id, ai.schedule.id AS scheduleId FROM ArcGISIntegration ai "
            + "WHERE ai.isDeleted = true AND ai.lastModifiedDate < :cutoff "
            + "ORDER BY ai.lastModifiedDate ASC, ai.id ASC")
    List<ArcGISIntegrationPurgeProjection> findCleanupCandidates(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ArcGISIntegration ai WHERE ai.id IN :ids")
    int deleteByIdIn(@Param("ids") List<UUID> ids);
}
