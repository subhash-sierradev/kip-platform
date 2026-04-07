package com.integration.management.repository;

import com.integration.management.entity.IntegrationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntegrationScheduleRepository extends JpaRepository<IntegrationSchedule, UUID> {

    @Modifying
    @Query("DELETE FROM IntegrationSchedule s WHERE s.id IN :ids")
    int deleteByIdIn(@Param("ids") List<UUID> ids);
}
