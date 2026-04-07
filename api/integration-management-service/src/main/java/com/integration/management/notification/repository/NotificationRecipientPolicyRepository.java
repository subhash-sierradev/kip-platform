package com.integration.management.notification.repository;

import com.integration.management.notification.entity.NotificationRecipientPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRecipientPolicyRepository
        extends JpaRepository<NotificationRecipientPolicy, UUID> {

    List<NotificationRecipientPolicy> findByTenantIdOrderByLastModifiedDateDesc(String tenantId);

    Optional<NotificationRecipientPolicy> findByRuleId(UUID ruleId);
}
