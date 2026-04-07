package com.integration.management.repository;

import com.integration.management.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByTenantIdAndTimestampAfterOrderByTimestampDesc(String tenantId, Instant cutoff);

    @Query("SELECT a.id FROM AuditLog a WHERE a.timestamp < :cutoff ORDER BY a.timestamp ASC, a.id ASC")
    List<UUID> findIdsByTimestampBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.id IN :ids")
    int deleteByIdIn(@Param("ids") List<UUID> ids);
}
