package com.integration.management.controller;

import com.integration.management.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionControllerTest {

    @Mock
    private VersionService versionService;

    private VersionController controller;

    @BeforeEach
    void setUp() {
        controller = new VersionController(versionService);
    }

    @Test
    void getVersion_returnsVersionInfo() {
        Map<String, Object> versionInfo = new LinkedHashMap<>();
        Map<String, String> imsInfo = Map.of(
                "version", "0.157.1",
                "name", "integration-management-service"
        );
        Map<String, String> iesInfo = Map.of(
                "version", "0.157.1",
                "name", "integration-execution-service"
        );
        versionInfo.put("ims", imsInfo);
        versionInfo.put("ies", iesInfo);
        when(versionService.getVersionInfo()).thenReturn(versionInfo);

        ResponseEntity<Map<String, Object>> response =
                controller.getVersion();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKeys("ims", "ies");
        verify(versionService).getVersionInfo();
    }
}
