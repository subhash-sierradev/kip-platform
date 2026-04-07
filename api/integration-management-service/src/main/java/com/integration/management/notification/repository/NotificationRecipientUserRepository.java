package com.integration.management.notification.repository;

import com.integration.management.notification.entity.NotificationRecipientUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRecipientUserRepository
        extends JpaRepository<NotificationRecipientUser, UUID> {

    List<NotificationRecipientUser> findByTenantId(String tenantId);

    List<NotificationRecipientUser> findByRecipientPolicyId(UUID recipientPolicyId);

    List<NotificationRecipientUser> findByRecipientPolicyIdIn(Collection<UUID> policyIds);
}
