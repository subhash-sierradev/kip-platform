package com.integration.management.repository;

import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.entity.IntegrationConnection;
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
public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, UUID> {

    Optional<IntegrationConnection> findByIdAndTenantIdAndIsDeletedFalse(UUID connectionId, String tenantId);

    @Query("SELECT c FROM IntegrationConnection c WHERE c.tenantId = :tenantId AND c.serviceType = :serviceType "
            + "AND c.isDeleted = false ORDER BY c.lastModifiedDate DESC")
    List<IntegrationConnection> findAllConnectionsByTenantAndServiceTypeAndIsDeletedFalse(
            @Param("tenantId") String tenantId,
            @Param("serviceType") ServiceType serviceType);

    @Query("SELECT ic.secretName FROM IntegrationConnection ic WHERE ic.id = :id"
            + " AND ic.tenantId = :tenantId AND ic.isDeleted = false")
    Optional<String> findSecretNameByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("SELECT c FROM IntegrationConnection c WHERE c.tenantId = :tenantId AND c.id = :id "
            + "AND c.isDeleted = false")
    Optional<IntegrationConnection> findConnectionById(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId);

    Optional<IntegrationConnection> findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(
            String tenantId, String connectionHashKey);

    @Query("SELECT c.id FROM IntegrationConnection c WHERE c.isDeleted = true AND c.lastModifiedDate < :cutoff "
            + "AND c.serviceType = com.integration.execution.contract.model.enums.ServiceType.ARCGIS "
            + "AND NOT EXISTS (SELECT 1 FROM ArcGISIntegration ai WHERE ai.connectionId = c.id) "
            + "ORDER BY c.lastModifiedDate ASC")
    List<UUID> findArcgisCleanupIds(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("SELECT c.id FROM IntegrationConnection c WHERE c.isDeleted = true AND c.lastModifiedDate < :cutoff "
            + "AND c.serviceType = com.integration.execution.contract.model.enums.ServiceType.JIRA "
            + "AND NOT EXISTS (SELECT 1 FROM JiraWebhook jw WHERE jw.connectionId = c.id) "
            + "ORDER BY c.lastModifiedDate ASC")
    List<UUID> findJiraCleanupIds(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM IntegrationConnection c WHERE c.id IN :ids")
    int deleteByIdIn(@Param("ids") List<UUID> ids);
}
