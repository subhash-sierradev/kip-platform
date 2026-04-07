package com.integration.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.SiteConfigDto;
import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.ConfigValueType;
import com.integration.execution.contract.model.enums.EntityType;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.model.dto.response.AuditLogResponse;
import com.integration.management.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsController")
class SettingsControllerTest {

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final UUID SITE_CONFIG_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/management";

    @Mock
    private SettingsService settingsService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SettingsController(settingsService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("DELETE /api/management/caches")
    class ClearAllCaches {

        @Test
        @DisplayName("should return 204 when clearing all caches")
        void clearAllCaches_validRequest_returnsNoContent() throws Exception {
            doNothing().when(settingsService).clearAllCaches();

            mockMvc.perform(delete(BASE_URL + "/caches"))
                    .andExpect(status().isNoContent());

            verify(settingsService).clearAllCaches();
        }

        @Test
        @DisplayName("should handle service exception")
        void clearAllCaches_serviceThrowsException_returns500() throws Exception {
            doThrow(new RuntimeException("Cache clear error"))
                    .when(settingsService).clearAllCaches();

            mockMvc.perform(delete(BASE_URL + "/caches"))
                    .andExpect(status().is5xxServerError());

            verify(settingsService).clearAllCaches();
        }
    }

    @Nested
    @DisplayName("DELETE /api/management/caches/{cacheName}")
    class ClearCacheByName {

        @Test
        @DisplayName("should return 204 when clearing specific cache")
        void clearCacheByName_validRequest_returnsNoContent() throws Exception {
            String cacheName = "testCache";
            doNothing().when(settingsService).clearCacheByName(cacheName);

            mockMvc.perform(delete(BASE_URL + "/caches/{cacheName}", cacheName))
                    .andExpect(status().isNoContent());

            verify(settingsService).clearCacheByName(cacheName);
        }

        @Test
        @DisplayName("should handle non-existent cache")
        void clearCacheByName_cacheNotFound_returnsNoContent() throws Exception {
            String cacheName = "nonExistentCache";
            doNothing().when(settingsService).clearCacheByName(cacheName);

            mockMvc.perform(delete(BASE_URL + "/caches/{cacheName}", cacheName))
                    .andExpect(status().isNoContent());

            verify(settingsService).clearCacheByName(cacheName);
        }

        @Test
        @DisplayName("should handle service exception")
        void clearCacheByName_serviceThrowsException_returns500() throws Exception {
            String cacheName = "testCache";
            doThrow(new RuntimeException("Cache clear error"))
                    .when(settingsService).clearCacheByName(cacheName);

            mockMvc.perform(delete(BASE_URL + "/caches/{cacheName}", cacheName))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/management/caches/stats")
    class GetAllCacheStats {

        @Test
        @DisplayName("should return 200 with all cache statistics")
        void getAllCacheStats_validRequest_returnsOkWithStats() throws Exception {
            Map<String, Object> stats = new HashMap<>();
            stats.put("cache1", Map.of("size", 100, "hitRate", 0.85));
            stats.put("cache2", Map.of("size", 50, "hitRate", 0.92));

            when(settingsService.getCacheStatsForAllCaches()).thenReturn(stats);

            mockMvc.perform(get(BASE_URL + "/caches/stats"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.cache1.size").value(100))
                    .andExpect(jsonPath("$.cache2.size").value(50));

            verify(settingsService).getCacheStatsForAllCaches();
        }

        @Test
        @DisplayName("should return empty map when no caches exist")
        void getAllCacheStats_noCaches_returnsEmptyMap() throws Exception {
            when(settingsService.getCacheStatsForAllCaches()).thenReturn(new HashMap<>());

            mockMvc.perform(get(BASE_URL + "/caches/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", anEmptyMap()));

            verify(settingsService).getCacheStatsForAllCaches();
        }
    }

    @Nested
    @DisplayName("GET /api/management/caches/stats/{cacheName}")
    class GetCacheStats {

        @Test
        @DisplayName("should return 200 with specific cache statistics")
        void getCacheStats_validRequest_returnsOkWithStats() throws Exception {
            String cacheName = "testCache";
            Map<String, Object> stats = Map.of("size", 100, "hitRate", 0.85);

            when(settingsService.getCacheStats(cacheName)).thenReturn(stats);

            mockMvc.perform(get(BASE_URL + "/caches/stats/{cacheName}", cacheName))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.size").value(100))
                    .andExpect(jsonPath("$.hitRate").value(0.85));

            verify(settingsService).getCacheStats(cacheName);
        }

        @Test
        @DisplayName("should return 404 when cache not found")
        void getCacheStats_cacheNotFound_returns404() throws Exception {
            String cacheName = "nonExistentCache";

            when(settingsService.getCacheStats(cacheName)).thenReturn(null);

            mockMvc.perform(get(BASE_URL + "/caches/stats/{cacheName}", cacheName))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("Cache 'nonExistentCache' not found")));

            verify(settingsService).getCacheStats(cacheName);
        }

        @Test
        @DisplayName("should handle service exception")
        void getCacheStats_serviceThrowsException_returns500() throws Exception {
            String cacheName = "testCache";

            when(settingsService.getCacheStats(cacheName))
                    .thenThrow(new RuntimeException("Stats retrieval error"));

            mockMvc.perform(get(BASE_URL + "/caches/stats/{cacheName}", cacheName))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/management/audits/logs")
    class GetAllAuditLogs {

        @Test
        @DisplayName("should return 200 with list of audit logs")
        void getAllAuditLogs_validRequest_returnsOkWithList() throws Exception {
            AuditLogResponse auditLog = new AuditLogResponse();
            auditLog.setId(UUID.randomUUID());
            auditLog.setEntityType(EntityType.INTEGRATION);
            auditLog.setAction("CREATE");
            auditLog.setResult(AuditResult.SUCCESS);
            auditLog.setPerformedBy(USER_ID);
            auditLog.setTenantId(TENANT_ID);
            auditLog.setClientIpAddress("203.0.113.22");
            auditLog.setTimestamp(Instant.now());

            when(settingsService.getAllAuditLogs(TENANT_ID)).thenReturn(List.of(auditLog));

            mockMvc.perform(get(BASE_URL + "/audits/logs")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].entityType").value("INTEGRATION"))
                    .andExpect(jsonPath("$[0].action").value("CREATE"))
                    .andExpect(jsonPath("$[0].clientIpAddress").value("203.0.113.22"));

            verify(settingsService).getAllAuditLogs(TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no audit logs found")
        void getAllAuditLogs_noLogs_returnsEmptyList() throws Exception {
            when(settingsService.getAllAuditLogs(TENANT_ID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/audits/logs")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(settingsService).getAllAuditLogs(TENANT_ID);
        }

        @Test
        @DisplayName("should handle service exception")
        void getAllAuditLogs_serviceThrowsException_returns500() throws Exception {
            when(settingsService.getAllAuditLogs(TENANT_ID))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get(BASE_URL + "/audits/logs")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/management/site-configs")
    class GetAllSiteConfigs {

        @Test
        @DisplayName("should return 200 with list of site configs")
        void getAllSiteConfigs_validRequest_returnsOkWithList() throws Exception {
            SiteConfigDto siteConfig = SiteConfigDto.builder()
                    .id(SITE_CONFIG_ID)
                    .configKey("app.timezone")
                    .configValue("UTC")
                    .type(ConfigValueType.STRING)
                    .description("Application timezone")
                    .tenantId(TENANT_ID)
                    .build();

            when(settingsService.getAllSiteConfigs(TENANT_ID)).thenReturn(List.of(siteConfig));

            mockMvc.perform(get(BASE_URL + "/site-configs")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].configKey").value("app.timezone"))
                    .andExpect(jsonPath("$[0].configValue").value("UTC"));

            verify(settingsService).getAllSiteConfigs(TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no site configs found")
        void getAllSiteConfigs_noConfigs_returnsEmptyList() throws Exception {
            when(settingsService.getAllSiteConfigs(TENANT_ID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/site-configs")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(settingsService).getAllSiteConfigs(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/management/site-configs/{id}")
    class GetSiteConfig {

        @Test
        @DisplayName("should return 200 with site config details")
        void getSiteConfig_validRequest_returnsOkWithDetails() throws Exception {
            SiteConfigDto siteConfig = SiteConfigDto.builder()
                    .id(SITE_CONFIG_ID)
                    .configKey("app.timezone")
                    .configValue("UTC")
                    .type(ConfigValueType.STRING)
                    .description("Application timezone")
                    .tenantId(TENANT_ID)
                    .createdDate(Instant.now())
                    .build();

            when(settingsService.getSiteConfig(SITE_CONFIG_ID, TENANT_ID)).thenReturn(siteConfig);

            mockMvc.perform(get(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(SITE_CONFIG_ID.toString()))
                    .andExpect(jsonPath("$.configKey").value("app.timezone"))
                    .andExpect(jsonPath("$.configValue").value("UTC"))
                    .andExpect(jsonPath("$.type").value("STRING"));

            verify(settingsService).getSiteConfig(SITE_CONFIG_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should handle service exception for non-existent config")
        void getSiteConfig_configNotFound_returns500() throws Exception {
            when(settingsService.getSiteConfig(SITE_CONFIG_ID, TENANT_ID))
                    .thenThrow(new RuntimeException("Site config not found"));

            mockMvc.perform(get(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/management/site-configs/{id}")
    class UpdateSiteConfig {

        @Test
        @DisplayName("should return 200 with updated site config")
        void updateSiteConfig_validRequest_returnsOkWithUpdatedConfig() throws Exception {
            SiteConfigDto requestDto = SiteConfigDto.builder()
                    .configKey("app.timezone")
                    .configValue("America/New_York")
                    .type(ConfigValueType.STRING)
                    .description("Updated timezone")
                    .build();

            SiteConfigDto responseDto = SiteConfigDto.builder()
                    .id(SITE_CONFIG_ID)
                    .configKey("app.timezone")
                    .configValue("America/New_York")
                    .type(ConfigValueType.STRING)
                    .description("Updated timezone")
                    .tenantId(TENANT_ID)
                    .lastModifiedBy(USER_ID)
                    .build();

            when(settingsService.updateSiteConfig(eq(SITE_CONFIG_ID), any(SiteConfigDto.class), eq(TENANT_ID),
                    eq(USER_ID)))
                    .thenReturn(responseDto);

            mockMvc.perform(put(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(SITE_CONFIG_ID.toString()))
                    .andExpect(jsonPath("$.configValue").value("America/New_York"))
                    .andExpect(jsonPath("$.description").value("Updated timezone"));

            verify(settingsService).updateSiteConfig(eq(SITE_CONFIG_ID), any(SiteConfigDto.class), eq(TENANT_ID),
                    eq(USER_ID));
        }

        @Test
        @DisplayName("should return 400 for invalid request body")
        void updateSiteConfig_invalidRequest_returnsBadRequest() throws Exception {
            SiteConfigDto invalidDto = SiteConfigDto.builder()
                    .configKey("")
                    .configValue("value")
                    .type(ConfigValueType.STRING)
                    .build();

            mockMvc.perform(put(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());

            verify(settingsService, never()).updateSiteConfig(any(), any(), any(), any());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "  ")
        @DisplayName("should return 400 for blank config key")
        void updateSiteConfig_blankConfigKey_returnsBadRequest(String configKey) throws Exception {
            SiteConfigDto invalidDto = SiteConfigDto.builder()
                    .configKey(configKey)
                    .configValue("value")
                    .type(ConfigValueType.STRING)
                    .build();

            mockMvc.perform(put(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());

            verify(settingsService, never()).updateSiteConfig(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should return 400 when type is null")
        void updateSiteConfig_nullType_returnsBadRequest() throws Exception {
            SiteConfigDto invalidDto = SiteConfigDto.builder()
                    .configKey("app.key")
                    .configValue("value")
                    .type(null)
                    .build();

            mockMvc.perform(put(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());

            verify(settingsService, never()).updateSiteConfig(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should handle service exception")
        void updateSiteConfig_serviceThrowsException_returns500() throws Exception {
            SiteConfigDto requestDto = SiteConfigDto.builder()
                    .configKey("app.key")
                    .configValue("value")
                    .type(ConfigValueType.STRING)
                    .build();

            when(settingsService.updateSiteConfig(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Update error"));

            mockMvc.perform(put(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("DELETE /api/management/site-configs/{id}")
    class DeleteSiteConfig {

        @Test
        @DisplayName("should return 204 when deleting site config")
        void deleteSiteConfig_validRequest_returnsNoContent() throws Exception {
            doNothing().when(settingsService).deleteSiteConfig(SITE_CONFIG_ID, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNoContent());

            verify(settingsService).deleteSiteConfig(SITE_CONFIG_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should handle service exception for non-existent config")
        void deleteSiteConfig_configNotFound_returns500() throws Exception {
            doThrow(new RuntimeException("Site config not found"))
                    .when(settingsService).deleteSiteConfig(SITE_CONFIG_ID, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/site-configs/{id}", SITE_CONFIG_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should handle UUID parsing for invalid ID format")
        void deleteSiteConfig_invalidUuidFormat_returnsBadRequest() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/site-configs/{id}", "invalid-uuid")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isBadRequest());

            verify(settingsService, never()).deleteSiteConfig(any(), any(), any());
        }
    }
}
