package com.integration.management.repository;

import com.integration.management.entity.JiraFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JiraFieldMappingRepository extends JpaRepository<JiraFieldMapping, UUID> {

    @Modifying
    @Query("DELETE FROM JiraFieldMapping jfm WHERE jfm.jiraWebhook.id IN :webhookIds")
    int deleteByWebhookIds(@Param("webhookIds") List<String> webhookIds);
}
