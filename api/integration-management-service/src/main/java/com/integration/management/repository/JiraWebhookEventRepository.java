package com.integration.management.repository;

import com.integration.management.entity.JiraWebhookEvent;
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
public interface JiraWebhookEventRepository extends JpaRepository<JiraWebhookEvent, UUID> {

    // Find latest events per original trigger, filtered by tenantId on the event
    // itself
    @Query("SELECT jwe FROM JiraWebhookEvent jwe "
            + "WHERE jwe.webhookId = :webhookId "
            + "AND jwe.tenantId = :tenantId "
            + "AND NOT EXISTS ("
            + "    SELECT 1 FROM JiraWebhookEvent newer "
            + "    WHERE newer.webhookId = :webhookId "
            + "    AND newer.tenantId = :tenantId "
            + "    AND newer.originalEventId = jwe.originalEventId "
            + "    AND (newer.triggeredAt > jwe.triggeredAt "
            + "         OR (newer.triggeredAt = jwe.triggeredAt AND newer.id > jwe.id))"
            + ") ORDER BY jwe.triggeredAt DESC")
    List<JiraWebhookEvent> findLatestEventsPerOriginalTriggerByWebhook(@Param("webhookId") String webhookId,
                                                                       @Param("tenantId") String tenantId);

    Optional<JiraWebhookEvent> findTopByOriginalEventIdOrderByRetryAttemptDesc(String originalEventId);

    @Query("SELECT jwe FROM JiraWebhookEvent jwe "
            + "WHERE jwe.webhookId IN :webhookIds AND jwe.triggeredAt = ("
            + "   SELECT MAX(e2.triggeredAt) FROM JiraWebhookEvent e2 WHERE e2.webhookId = jwe.webhookId"
            + ")")
    List<JiraWebhookEvent> findLatestEventsForWebhookIds(@Param("webhookIds") List<String> webhookIds);

    @Query("SELECT jwe.id FROM JiraWebhookEvent jwe WHERE jwe.triggeredAt < :cutoff ORDER BY jwe.triggeredAt ASC")
    List<UUID> findIdsByTriggeredAtBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM JiraWebhookEvent jwe WHERE jwe.id IN :ids")
    int deleteByIdIn(@Param("ids") List<UUID> ids);
}
