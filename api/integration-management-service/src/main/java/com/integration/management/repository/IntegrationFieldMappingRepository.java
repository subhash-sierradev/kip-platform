package com.integration.management.repository;

import com.integration.management.entity.IntegrationFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface IntegrationFieldMappingRepository extends JpaRepository<IntegrationFieldMapping, UUID> {

    @Query("SELECT ifm FROM IntegrationFieldMapping ifm "
            + "JOIN ArcGISIntegration agi ON ifm.integrationId = agi.id "
            + "WHERE ifm.integrationId = :integrationId "
            + "AND agi.tenantId = :tenantId")
    List<IntegrationFieldMapping> findByIntegrationIdAndTenantId(UUID integrationId, String tenantId);

    @Modifying
    @Query("""
            DELETE FROM IntegrationFieldMapping m
                WHERE m.integrationId = :integrationId
                  AND m.id NOT IN :incomingIds
            """)
    void deleteByIntegrationIdAndIdNotIn(UUID integrationId, Set<UUID> incomingIds);

    @Modifying
    @Query("DELETE FROM IntegrationFieldMapping ifm WHERE ifm.integrationId IN :integrationIds")
    int deleteByIntegrationIds(@Param("integrationIds") List<UUID> integrationIds);
}
