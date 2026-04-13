package com.integration.management.notification.repository;

import com.integration.management.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    @Query("SELECT t FROM NotificationTemplate t WHERE (t.tenantId = :tenantId OR t.tenantId = 'GLOBAL') "
            + " AND t.isDeleted = false ORDER BY t.event.eventKey ASC")
    List<NotificationTemplate> findByTenantId(String tenantId);
}
