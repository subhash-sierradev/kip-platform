package com.integration.management.service;

import com.integration.management.entity.TenantProfile;
import com.integration.management.notification.service.NotificationDefaultRulesService;
import com.integration.management.repository.TenantProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.integration.management.constants.ManagementSecurityConstants.SYSTEM_USER;

@Service
@AllArgsConstructor
@Slf4j
public class TenantProfileService {

    private final TenantProfileRepository tenantProfileRepository;
    private final NotificationDefaultRulesService notificationDefaultRulesService;

    @Transactional
    @Cacheable(value = "tenantProfileCache", key = "#tenantId")
    public synchronized TenantProfile getOrCreateTenantProfile(String tenantId, String tenantName) {
        // Double-check pattern to handle race conditions
        return tenantProfileRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    try {
                        log.debug("Creating new tenant profile: {}", tenantId);
                        TenantProfile newTenant = TenantProfile.builder()
                                .tenantId(tenantId)
                                .tenantName(tenantName)
                                .build();
                        TenantProfile savedTenant = tenantProfileRepository.saveAndFlush(newTenant);
                        log.info("Created new tenant profile: {}", tenantId);

                        // Initialize default notification rules for the new tenant
                        try {
                            notificationDefaultRulesService.initializeDefaultRulesForTenant(tenantId, SYSTEM_USER);
                            log.info("Successfully initialized default notification rules for tenant: {}", tenantId);
                        } catch (Exception e) {
                            log.error("Failed to initialize default notification rules for tenant: {}", tenantId, e);
                            // Log error but don't fail the tenant creation
                        }

                        return savedTenant;
                    } catch (DataIntegrityViolationException e) {
                        // Handle race condition: another thread created it between check and save
                        log.warn("Tenant profile already exists (race condition handled): {}", tenantId);
                        return tenantProfileRepository.findByTenantId(tenantId)
                                .orElseThrow(() ->
                                        new RuntimeException("Failed to retrieve tenant profile after creation: "
                                                + tenantId));
                    }
                });
    }
}
