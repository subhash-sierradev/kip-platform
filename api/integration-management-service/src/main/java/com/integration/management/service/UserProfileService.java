package com.integration.management.service;

import com.integration.management.entity.UserProfile;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final TenantProfileService tenantProfileService;
    private final UserProfileRepository userProfileRepository;

    @Cacheable(value = "allUserProfileCache", key = "#tenantId")
    public List<UserProfileResponse> getAllUsersByTenant(String tenantId) {
        log.debug("Fetching all user profiles for tenant: {}", tenantId);
        return userProfileRepository.findByTenantId(tenantId).stream()
                .map(u -> UserProfileResponse.builder()
                        .id(u.getId())
                        .keycloakUserId(u.getKeycloakUserId())
                        .email(u.getEmail())
                        .displayName(u.getDisplayName())
                        .isTenantAdmin(u.isTenantAdmin())
                        .createdDate(u.getCreatedDate())
                        .build())
                .toList();
    }

    @Cacheable(value = "userProfileMapByTenantCache", key = "#tenantId")
    public Map<String, UserProfileResponse> getUserProfileMapByTenant(String tenantId) {
        return getAllUsersByTenant(tenantId).stream()
                .collect(Collectors.toMap(UserProfileResponse::getKeycloakUserId, p -> p));
    }

    @Cacheable(value = "userProfileCache", key = "#keycloakUserId + ':' + #tenantId")
    public UserProfile getUserByKeycloakId(String keycloakUserId, String tenantId) {
        log.debug("Fetching user profile for keycloakUserId: {} tenantId: {}", keycloakUserId, tenantId);
        return userProfileRepository.findByKeycloakUserIdAndTenantId(keycloakUserId, tenantId)
                .orElseThrow(() -> new IntegrationNotFoundException(
                        "User profile not found for keycloakUserId=" + keycloakUserId + " tenantId=" + tenantId));
    }

    @Transactional
    @Caching(
        put = @CachePut(value = "userProfileCache", key = "#keycloakUserId + ':' + #tenantId"),
        evict = {
            @CacheEvict(value = "allUserProfileCache", key = "#tenantId"),
            @CacheEvict(value = "userProfileMapByTenantCache", key = "#tenantId")
        }
    )
    public UserProfile syncUser(String keycloakUserId, String email, String displayName,
                                String tenantId, String tenantName, boolean isTenantAdmin) {
        log.debug("Syncing user: {} for tenant: {}", keycloakUserId, tenantId);
        Optional<UserProfile> existingOpt = userProfileRepository
                .findByKeycloakUserIdAndTenantId(keycloakUserId, tenantId);
        return existingOpt.map(userProfile ->
                syncExistingUser(userProfile, email, displayName, isTenantAdmin))
                .orElseGet(() ->
                        createNewUser(keycloakUserId, email, displayName, tenantId, tenantName, isTenantAdmin));
    }

    private UserProfile syncExistingUser(final UserProfile existing, final String email,
                                          final String displayName, final boolean isTenantAdmin) {
        existing.setTenantAdmin(isTenantAdmin);
        existing.setEmail(email);
        existing.setDisplayName(displayName);
        UserProfile updated = userProfileRepository.save(existing);
        log.info("Updated user profile for: {} (isTenantAdmin={}, email={})",
                updated.getKeycloakUserId(), isTenantAdmin, email);
        return updated;
    }

    private UserProfile createNewUser(String keycloakUserId, String email, String displayName,
                                      String tenantId, String tenantName, boolean isTenantAdmin) {
        log.debug("Creating new user: {} for tenant: {}", keycloakUserId, tenantId);
        // Ensure tenant exists (handles race conditions internally)
        var tenantProfile = tenantProfileService.getOrCreateTenantProfile(tenantId, tenantName);
        try {
            UserProfile user = UserProfile.builder()
                    .keycloakUserId(keycloakUserId)
                    .email(email)
                    .displayName(displayName)
                    .tenantId(tenantProfile.getTenantId())
                    .isTenantAdmin(isTenantAdmin)
                    .build();
            user = userProfileRepository.save(user);
            log.info("Created new user: {} for tenant: {}", keycloakUserId, tenantProfile.getTenantId());
            return user;
        } catch (DataIntegrityViolationException e) {
            // Handle race condition: another thread created the user between check and save
            log.warn("User profile already exists (race condition handled): {} for tenant: {}",
                    keycloakUserId, tenantId);
            return userProfileRepository.findByKeycloakUserIdAndTenantId(keycloakUserId, tenantId)
                    .orElseThrow(() ->
                            new RuntimeException("Failed to retrieve user profile after creation: " + keycloakUserId));
        }
    }
}
