package com.integration.management.service;

import com.integration.management.ies.client.IesVersionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private BuildProperties buildProperties;

    @Mock
    private IesVersionClient iesVersionClient;

    private VersionService versionService;

    @BeforeEach
    void setUp() {
        versionService = new VersionService(
                buildProperties, iesVersionClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getVersionInfo_returnsImsAndIesVersions() {
        when(buildProperties.getVersion()).thenReturn("0.157.1");
        when(buildProperties.getName())
                .thenReturn("integration-management-service");
        when(buildProperties.getTime())
                .thenReturn(Instant.parse("2026-04-14T10:00:00Z"));

        Map<String, String> iesVersion = new LinkedHashMap<>();
        iesVersion.put("version", "0.157.1");
        iesVersion.put("name", "integration-execution-service");
        when(iesVersionClient.getVersion()).thenReturn(iesVersion);

        Map<String, Object> result = versionService.getVersionInfo();

        assertThat(result).containsKeys("ims", "ies");

        Map<String, String> ims =
                (Map<String, String>) result.get("ims");
        assertThat(ims.get("version")).isEqualTo("0.157.1");
        assertThat(ims.get("name"))
                .isEqualTo("integration-management-service");
        assertThat(ims.get("buildTime"))
                .isEqualTo("2026-04-14T10:00:00Z");

        Map<String, String> ies =
                (Map<String, String>) result.get("ies");
        assertThat(ies.get("version")).isEqualTo("0.157.1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchIesVersion_iesUnavailable_returnsFallback() {
        when(buildProperties.getVersion()).thenReturn("0.157.1");
        when(buildProperties.getName())
                .thenReturn("integration-management-service");
        when(buildProperties.getTime())
                .thenReturn(Instant.parse("2026-04-14T10:00:00Z"));
        when(iesVersionClient.getVersion())
                .thenThrow(new RuntimeException("Connection refused"));

        Map<String, Object> result = versionService.getVersionInfo();

        Map<String, String> ies =
                (Map<String, String>) result.get("ies");
        assertThat(ies.get("version")).isEqualTo("unavailable");
        assertThat(ies.get("name"))
                .isEqualTo("integration-execution-service");
    }
}
