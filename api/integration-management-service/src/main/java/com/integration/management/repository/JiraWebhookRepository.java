package com.integration.management.repository;

import com.integration.management.entity.JiraWebhook;
import com.integration.management.repository.projection.StringIdSummaryWithLastRunProjection;
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
public interface JiraWebhookRepository extends JpaRepository<JiraWebhook, String> {

    @Query("SELECT w FROM JiraWebhook w LEFT JOIN FETCH w.jiraFieldMappings WHERE w.id = :id "
            + "AND w.tenantId = :tenantId AND w.isDeleted = false")
    Optional<JiraWebhook> findByIdAndTenantIdAndIsDeletedFalse(String id, String tenantId);

    @Query("SELECT w FROM JiraWebhook w LEFT JOIN FETCH w.jiraFieldMappings WHERE w.id = :id "
            + "AND w.isDeleted = false")
    Optional<JiraWebhook> findByIdAndIsDeletedFalse(@Param("id") String id);

    /**
     * Retrieves a non-deleted webhook by id without tenant scoping.
     * Intended for GLOBAL/system contexts only.
     */
    @Query("SELECT w FROM JiraWebhook w LEFT JOIN FETCH w.jiraFieldMappings WHERE w.id = :id "
            + "AND w.isDeleted = false")
    Optional<JiraWebhook> findByIdIgnoringTenantAndIsDeletedFalse(@Param("id") String id);

    @Query("SELECT w.name FROM JiraWebhook w WHERE w.id = :id")
    String findJiraWebhookNameById(@Param("id") String id);

    List<JiraWebhook> findByConnectionIdAndIsDeletedFalse(UUID connectionId);

    @Query(value = """
            SELECT w.id AS id, w.name AS name, w.is_enabled AS isEnabled,
                   w.description AS description, latest_evt.triggered_at AS lastRunAt
            FROM integration_platform.jira_webhooks w
            LEFT JOIN LATERAL (
                SELECT jwe.triggered_at
                FROM integration_platform.jira_webhook_events jwe
                WHERE jwe.webhook_id = w.id
                  AND jwe.tenant_id = w.tenant_id
                ORDER BY jwe.triggered_at DESC
                LIMIT 1
            ) latest_evt ON true
            WHERE w.tenant_id = :tenantId AND w.connection_id = :connectionId
              AND w.is_deleted = false
            """, nativeQuery = true)
    List<StringIdSummaryWithLastRunProjection> findNonDeletedSummariesByConnectionIdAndTenantId(
            @Param("connectionId") UUID connectionId,
            @Param("tenantId") String tenantId);

    @Query("SELECT w.normalizedName FROM JiraWebhook w WHERE w.tenantId = :tenantId "
            + "AND w.normalizedName IS NOT NULL AND w.normalizedName != '' AND w.isDeleted = false")
    List<String> findAllNormalizedNamesByTenantId(@Param("tenantId") String tenantId);

    // Optimized query for webhook summary with last event history
    @Query("SELECT w FROM JiraWebhook w LEFT JOIN FETCH w.jiraFieldMappings "
            + "WHERE w.tenantId = :tenantId AND w.isDeleted = false ORDER BY w.lastModifiedDate DESC")
    List<JiraWebhook> findAllSummaryByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT w.id FROM JiraWebhook w WHERE w.isDeleted = true AND w.lastModifiedDate < :cutoff "
            + "ORDER BY w.lastModifiedDate ASC, w.id ASC")
    List<String> findCleanupIds(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM JiraWebhook w WHERE w.id IN :ids")
    int deleteByIdIn(@Param("ids") List<String> ids);
}
