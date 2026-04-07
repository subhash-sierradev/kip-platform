package com.integration.management.notification.repository;

import com.integration.management.notification.entity.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRule, UUID> {

    List<NotificationRule> findByTenantIdOrderByLastModifiedDateDesc(String tenantId);

    Optional<NotificationRule> findByEventEventKeyAndTenantIdAndIsEnabledTrue(
            String eventKey, String tenantId);
}
