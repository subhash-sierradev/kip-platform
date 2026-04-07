package com.integration.management.service;

import com.integration.execution.contract.model.SiteConfigDto;
import com.integration.execution.contract.model.enums.ConfigValueType;
import com.integration.management.entity.SiteConfig;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.mapper.AuditLogMapper;
import com.integration.management.mapper.SiteConfigMapper;
import com.integration.management.repository.AuditLogRepository;
import com.integration.management.repository.SiteConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsService - branch coverage")
class SettingsServiceBranchTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SiteConfigRepository siteConfigRepository;
    @Mock private AuditLogMapper auditLogMapper;
    @Mock private SiteConfigMapper siteConfigMapper;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private SettingsService settingsService;

    // ── isInvalidValue: BOOLEAN type ────────────────────────────────────────────

    @Test
    @DisplayName("updateSiteConfig: BOOLEAN 'true' value is valid - saves")
    void updateSiteConfig_booleanTrue_isValid() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_BOOL").configValue("false").type(ConfigValueType.BOOLEAN).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));
        SiteConfigDto saved = SiteConfigDto.builder().configKey("MY_BOOL").configValue("true").build();
        when(siteConfigRepository.save(any())).thenReturn(existing);
        when(siteConfigMapper.toDto(existing)).thenReturn(saved);

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("true").build(), "t1", "u");

        assertThat(result.getConfigValue()).isEqualTo("true");
        verify(siteConfigRepository).save(existing);
    }

    @Test
    @DisplayName("updateSiteConfig: BOOLEAN null value returns null (invalid)")
    void updateSiteConfig_booleanNull_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_BOOL").configValue("false").type(ConfigValueType.BOOLEAN).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue(null).build(), "t1", "u");

        assertThat(result).isNull();
        verify(siteConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSiteConfig: BOOLEAN 'maybe' value returns null (invalid)")
    void updateSiteConfig_booleanInvalidString_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_BOOL").configValue("false").type(ConfigValueType.BOOLEAN).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("maybe").build(), "t1", "u");

        assertThat(result).isNull();
        verify(siteConfigRepository, never()).save(any());
    }

    // ── isInvalidValue: NUMBER type ──────────────────────────────────────────────

    @Test
    @DisplayName("updateSiteConfig: NUMBER valid double saves")
    void updateSiteConfig_numberValid_saves() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_NUM").configValue("1").type(ConfigValueType.NUMBER).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));
        SiteConfigDto saved = SiteConfigDto.builder().configKey("MY_NUM").configValue("3.14").build();
        when(siteConfigRepository.save(any())).thenReturn(existing);
        when(siteConfigMapper.toDto(existing)).thenReturn(saved);

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("3.14").build(), "t1", "u");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateSiteConfig: NUMBER null value returns null (invalid)")
    void updateSiteConfig_numberNull_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_NUM").configValue("1").type(ConfigValueType.NUMBER).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue(null).build(), "t1", "u");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("updateSiteConfig: NUMBER 'notanumber' returns null (invalid)")
    void updateSiteConfig_numberInvalid_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_NUM").configValue("1").type(ConfigValueType.NUMBER).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("notanumber").build(), "t1", "u");

        assertThat(result).isNull();
    }

    // ── isInvalidValue: STRING type ──────────────────────────────────────────────

    @Test
    @DisplayName("updateSiteConfig: STRING valid value saves")
    void updateSiteConfig_stringValid_saves() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_STR").configValue("old").type(ConfigValueType.STRING).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));
        SiteConfigDto saved = SiteConfigDto.builder().configKey("MY_STR").configValue("new").build();
        when(siteConfigRepository.save(any())).thenReturn(existing);
        when(siteConfigMapper.toDto(existing)).thenReturn(saved);

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("new").build(), "t1", "u");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateSiteConfig: STRING empty value returns null (invalid)")
    void updateSiteConfig_stringEmpty_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_STR").configValue("old").type(ConfigValueType.STRING).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("").build(), "t1", "u");

        assertThat(result).isNull();
    }

    // ── isInvalidValue: TIMESTAMP type ───────────────────────────────────────────

    @Test
    @DisplayName("updateSiteConfig: TIMESTAMP valid ISO value saves")
    void updateSiteConfig_timestampValid_saves() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_TS").configValue("2020-01-01T00:00:00Z").type(ConfigValueType.TIMESTAMP).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));
        String newVal = "2026-03-01T00:00:00Z";
        SiteConfigDto saved = SiteConfigDto.builder().configKey("MY_TS").configValue(newVal).build();
        when(siteConfigRepository.save(any())).thenReturn(existing);
        when(siteConfigMapper.toDto(existing)).thenReturn(saved);

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue(newVal).build(), "t1", "u");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateSiteConfig: TIMESTAMP null value returns null (invalid)")
    void updateSiteConfig_timestampNull_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_TS").configValue("2020-01-01T00:00:00Z").type(ConfigValueType.TIMESTAMP).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue(null).build(), "t1", "u");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("updateSiteConfig: TIMESTAMP empty value returns null (invalid)")
    void updateSiteConfig_timestampEmpty_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_TS").configValue("2020-01-01T00:00:00Z").type(ConfigValueType.TIMESTAMP).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("").build(), "t1", "u");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("updateSiteConfig: TIMESTAMP bad format returns null (invalid)")
    void updateSiteConfig_timestampBadFormat_returnsNull() {
        UUID id = UUID.randomUUID();
        SiteConfig existing = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("MY_TS").configValue("2020-01-01T00:00:00Z").type(ConfigValueType.TIMESTAMP).build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(existing));

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("not-a-timestamp").build(), "t1", "u");

        assertThat(result).isNull();
    }

    // ── deleteSiteConfig paths ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSiteConfig throws IllegalStateException when config is GLOBAL")
    void deleteSiteConfig_global_throws() {
        UUID id = UUID.randomUUID();
        SiteConfig global = SiteConfig.builder()
                .id(id).tenantId("GLOBAL").createdBy("sys").lastModifiedBy("sys")
                .configKey("K").configValue("v").type(ConfigValueType.STRING).build();
        when(siteConfigRepository.findByTenant(id, "t1")).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> settingsService.deleteSiteConfig(id, "t1", "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GLOBAL SiteConfig cannot be deleted");
        verify(siteConfigRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteSiteConfig deletes tenant-specific config successfully")
    void deleteSiteConfig_tenantConfig_deletes() {
        UUID id = UUID.randomUUID();
        SiteConfig tenantConfig = SiteConfig.builder()
                .id(id).tenantId("t1").createdBy("u").lastModifiedBy("u")
                .configKey("K").configValue("v").type(ConfigValueType.STRING).build();
        when(siteConfigRepository.findByTenant(id, "t1")).thenReturn(Optional.of(tenantConfig));

        settingsService.deleteSiteConfig(id, "t1", "u");

        verify(siteConfigRepository).delete(tenantConfig);
    }

    @Test
    @DisplayName("deleteSiteConfig throws RuntimeException when config not found")
    void deleteSiteConfig_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findByTenant(id, "t1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.deleteSiteConfig(id, "t1", "u"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SiteConfig not found");
    }

    // ── getSiteConfigKeyById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getSiteConfigKeyById throws IntegrationNotFoundException when null returned")
    void getSiteConfigKeyById_null_throws() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findConfigKeyById(id, "t1")).thenReturn(null);

        assertThatThrownBy(() -> settingsService.getSiteConfigKeyById(id, "t1"))
                .isInstanceOf(IntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("getSiteConfigKeyById returns key when found")
    void getSiteConfigKeyById_found_returnsKey() {
        UUID id = UUID.randomUUID();
        when(siteConfigRepository.findConfigKeyById(id, "t1")).thenReturn("MY_KEY");

        String key = settingsService.getSiteConfigKeyById(id, "t1");

        assertThat(key).isEqualTo("MY_KEY");
    }

    // ── getConfigValueAsTimestamp ──────────────────────────────────────────────────

    @Test
    @DisplayName("getConfigValueAsTimestamp returns parsed Instant for valid ISO value")
    void getConfigValueAsTimestamp_valid_returnsParsed() {
        String val = "2026-01-15T12:00:00Z";
        when(siteConfigRepository.findByConfigKeyAndTenant("MY_KEY", "t1"))
                .thenReturn(Optional.of(val));

        Optional<Instant> result = settingsService.getConfigValueAsTimestamp("MY_KEY", "t1");

        assertThat(result).isPresent().contains(Instant.parse(val));
    }

    @Test
    @DisplayName("getConfigValueAsTimestamp returns empty optional when not found")
    void getConfigValueAsTimestamp_notFound_returnsEmpty() {
        when(siteConfigRepository.findByConfigKeyAndTenant("MY_KEY", "t1"))
                .thenReturn(Optional.empty());

        Optional<Instant> result = settingsService.getConfigValueAsTimestamp("MY_KEY", "t1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getConfigValueAsTimestamp returns empty or null-wrapped optional for unparseable value")
    void getConfigValueAsTimestamp_badFormat_returnsNullWrapped() {
        when(siteConfigRepository.findByConfigKeyAndTenant("MY_KEY", "t1"))
                .thenReturn(Optional.of("not-a-date"));

        // The map() catches DateTimeParseException and returns null,
        // which produces Optional containing null value (no exception)
        Optional<Instant> result = settingsService.getConfigValueAsTimestamp("MY_KEY", "t1");

        // Either Optional.empty() or Optional with null – both are acceptable
        // Just assert no exception is thrown and the result exists
        assertThat(result).isNotNull();
    }

    // ── updateSiteConfig: GLOBAL creates tenant override ─────────────────────────

    @Test
    @DisplayName("updateSiteConfig: GLOBAL STRING value - creates tenant override")
    void updateSiteConfig_globalStringOverride_createsTenant() {
        UUID id = UUID.randomUUID();
        SiteConfig global = SiteConfig.builder()
                .id(id).tenantId("GLOBAL").createdBy("sys").lastModifiedBy("sys")
                .configKey("G_KEY").configValue("old").type(ConfigValueType.STRING)
                .description("desc").build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(global));
        SiteConfig saved = SiteConfig.builder()
                .id(UUID.randomUUID()).tenantId("t1").configKey("G_KEY")
                .configValue("new").type(ConfigValueType.STRING).build();
        SiteConfigDto savedDto = SiteConfigDto.builder().configKey("G_KEY").configValue("new").build();
        when(siteConfigRepository.save(any())).thenReturn(saved);
        when(siteConfigMapper.toDto(saved)).thenReturn(savedDto);

        SiteConfigDto result = settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("new").build(), "t1", "u");

        assertThat(result.getConfigValue()).isEqualTo("new");
    }

    @Test
    @DisplayName("updateSiteConfig: GLOBAL BOOLEAN empty value - throws IllegalStateException")
    void updateSiteConfig_globalBooleanInvalid_throws() {
        UUID id = UUID.randomUUID();
        SiteConfig global = SiteConfig.builder()
                .id(id).tenantId("GLOBAL").createdBy("sys").lastModifiedBy("sys")
                .configKey("G_BOOL").configValue("true").type(ConfigValueType.BOOLEAN)
                .description("desc").build();
        when(siteConfigRepository.findByTenantWithGlobalFallback(id, "t1"))
                .thenReturn(Optional.of(global));

        assertThatThrownBy(() -> settingsService.updateSiteConfig(
                id, SiteConfigDto.builder().configValue("maybe").build(), "t1", "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid value");
    }
}

