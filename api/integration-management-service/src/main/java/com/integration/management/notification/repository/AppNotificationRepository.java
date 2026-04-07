package com.integration.management.notification.repository;

import com.integration.management.notification.entity.AppNotification;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.management.notification.model.dto.response.NotificationCountResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    @Query("SELECT n FROM AppNotification n "
            + "WHERE n.tenantId = :tenantId "
            + "AND n.userId = :userId "
            + "AND (:isRead IS NULL OR n.isRead = :isRead) "
            + "AND (:severity IS NULL OR n.severity = :severity) "
            + "ORDER BY n.createdDate DESC")
    Page<AppNotification> findByTenantIdAndUserIdWithFilters(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("isRead") Boolean isRead,
            @Param("severity") NotificationSeverity severity,
            Pageable pageable);

    @Query(
            "SELECT new com.integration.management.notification.model.dto.response.NotificationCountResponse("
                    + "COUNT(n), "
                    + "COALESCE(SUM(CASE WHEN n.isRead = false THEN 1 ELSE 0 END), 0), "
                    + "COALESCE(SUM(CASE WHEN n.isRead = true THEN 1 ELSE 0 END), 0), "
                    + "COALESCE(SUM(CASE WHEN n.severity = "
                    + "com.integration.execution.contract.model.enums.NotificationSeverity.ERROR "
                    + "THEN 1 ELSE 0 END), 0), "
                    + "COALESCE(SUM(CASE WHEN n.severity = "
                    + "com.integration.execution.contract.model.enums.NotificationSeverity.INFO "
                    + "THEN 1 ELSE 0 END), 0), "
                    + "COALESCE(SUM(CASE WHEN n.severity = "
                    + "com.integration.execution.contract.model.enums.NotificationSeverity.SUCCESS "
                    + "THEN 1 ELSE 0 END), 0), "
                    + "COALESCE(SUM(CASE WHEN n.severity = "
                    + "com.integration.execution.contract.model.enums.NotificationSeverity.WARNING "
                    + "THEN 1 ELSE 0 END), 0)"
                    + ") FROM AppNotification n "
                    + "WHERE n.tenantId = :tenantId "
                    + "AND n.userId = :userId"
    )
    NotificationCountResponse countAllGrouped(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId
    );

    @Modifying
    @Query("UPDATE AppNotification n SET n.isRead = true "
            + "WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.id IN :ids")
    int markAsReadByIds(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("ids") List<UUID> ids);

    @Modifying
    @Query("UPDATE AppNotification n SET n.isRead = true "
            + "WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUser(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM AppNotification n "
            + "WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.id = :id")
    int deleteByIdForUser(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("id") UUID id);

    @Modifying
    @Query("DELETE FROM AppNotification n WHERE n.tenantId = :tenantId AND n.userId = :userId")
    int deleteAllByTenantIdAndUserId(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId);

    @Query("SELECT n.id FROM AppNotification n WHERE n.createdDate < :cutoff ORDER BY n.createdDate ASC, n.id ASC")
    List<UUID> findIdsByCreatedDateBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AppNotification n WHERE n.id IN :ids")
    int deleteByIdIn(@Param("ids") List<UUID> ids);
}
