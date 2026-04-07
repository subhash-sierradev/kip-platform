package com.integration.management.repository;

import com.integration.management.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query("SELECT u FROM UserProfile u "
            + "WHERE u.keycloakUserId = :keycloakUserId "
            + "AND u.tenantId = :tenantId")
    Optional<UserProfile> findByKeycloakUserIdAndTenantId(
            @Param("keycloakUserId") String keycloakUserId,
            @Param("tenantId") String tenantId);

    List<UserProfile> findByTenantId(String tenantId);
}
