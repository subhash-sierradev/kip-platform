package com.integration.management.notification.repository;

import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationEventCatalogRepository extends JpaRepository<NotificationEventCatalog, UUID> {

    List<NotificationEventCatalog> findByIsEnabledTrueAndEntityTypeInOrderByEventKeyAsc(Collection<NotificationEntityType> entityTypes);
}
