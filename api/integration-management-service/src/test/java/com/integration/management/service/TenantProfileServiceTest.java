package com.integration.management.service;

import com.integration.management.entity.TenantProfile;
import com.integration.management.notification.service.NotificationDefaultRulesService;
import com.integration.management.repository.TenantProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProfileServiceTest {

    @Mock
    private TenantProfileRepository tenantProfileRepository;

    @Mock
    private NotificationDefaultRulesService notificationDefaultRulesService;

    @InjectMocks
    private TenantProfileService tenantProfileService;

    @Test
    void returns_existing_tenant_profile_when_found() {
        String tenantId = "tenant-1";
        String tenantName = "Tenant One";

        TenantProfile existing = TenantProfile.builder()
                .tenantId(tenantId)
                .tenantName("Existing")
                .build();

        when(tenantProfileRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));

        TenantProfile result = tenantProfileService.getOrCreateTenantProfile(tenantId, tenantName);

        assertThat(result).isSameAs(existing);
        verify(tenantProfileRepository).findByTenantId(tenantId);
        verify(tenantProfileRepository, never()).save(any(TenantProfile.class));
    }

    @Test
    void creates_and_saves_new_tenant_profile_when_not_found() {
        String tenantId = "tenant-2";
        String tenantName = "Tenant Two";

        when(tenantProfileRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tenantProfileRepository.saveAndFlush(any(TenantProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, TenantProfile.class));

        TenantProfile result = tenantProfileService.getOrCreateTenantProfile(tenantId, tenantName);

        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getTenantName()).isEqualTo(tenantName);
        verify(tenantProfileRepository).findByTenantId(tenantId);
        verify(tenantProfileRepository).saveAndFlush(any(TenantProfile.class));
        verify(notificationDefaultRulesService).initializeDefaultRulesForTenant(eq(tenantId), anyString());
    }

    @Test
    void handles_race_condition_on_duplicate_creation() {
        String tenantId = "tenant-3";
        TenantProfile existing = TenantProfile.builder().tenantId(tenantId).tenantName("T3").build();

        when(tenantProfileRepository.findByTenantId(tenantId))
                .thenReturn(Optional.empty())         // first check: not found -> proceed to create
                .thenReturn(Optional.of(existing));   // second check after DataIntegrityViolationException

        when(tenantProfileRepository.saveAndFlush(any(TenantProfile.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        TenantProfile result = tenantProfileService.getOrCreateTenantProfile(tenantId, "T3");

        assertThat(result).isSameAs(existing);
    }

    @Test
    void throws_when_second_lookup_also_fails_after_race_condition() {
        String tenantId = "tenant-4";

        when(tenantProfileRepository.findByTenantId(tenantId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty()); // even second lookup returns empty

        when(tenantProfileRepository.saveAndFlush(any(TenantProfile.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> tenantProfileService.getOrCreateTenantProfile(tenantId, "T4"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to retrieve tenant profile");
    }

    @Test
    void notification_init_failure_does_not_prevent_tenant_profile_creation() {
        String tenantId = "tenant-5";
        when(tenantProfileRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tenantProfileRepository.saveAndFlush(any(TenantProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0, TenantProfile.class));
        org.mockito.Mockito.doThrow(new RuntimeException("notification failed"))
                .when(notificationDefaultRulesService)
                .initializeDefaultRulesForTenant(eq(tenantId), anyString());

        TenantProfile result = tenantProfileService.getOrCreateTenantProfile(tenantId, "T5");

        assertThat(result.getTenantId()).isEqualTo(tenantId);
    }
}
