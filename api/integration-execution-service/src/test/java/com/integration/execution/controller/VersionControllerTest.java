package com.integration.execution.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionControllerTest {

    @Mock
    private BuildProperties buildProperties;

    private VersionController controller;

    @BeforeEach
    void setUp() {
        controller = new VersionController(buildProperties);
    }

    @Test
    void getVersion_returnsBuildInfo() {
        when(buildProperties.getVersion()).thenReturn("0.157.1");
        when(buildProperties.getName())
                .thenReturn("integration-execution-service");
        when(buildProperties.getTime())
                .thenReturn(Instant.parse("2026-04-14T10:00:00Z"));

        ResponseEntity<Map<String, String>> response =
                controller.getVersion();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("version")).isEqualTo("0.157.1");
        assertThat(body.get("name"))
                .isEqualTo("integration-execution-service");
        assertThat(body.get("buildTime"))
                .isEqualTo("2026-04-14T10:00:00Z");
    }
}
