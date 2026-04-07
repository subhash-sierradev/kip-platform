package com.integration.management.notification.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.notification.model.dto.response.NotificationEventCatalogResponse;
import com.integration.management.notification.service.NotificationEventCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventCatalogController")
class NotificationEventCatalogControllerTest {

    private static final String TENANT_ID = "tenant-xyz";
    private static final String BASE_URL = "/api/management/notifications/events";

    @Mock private NotificationEventCatalogService notificationEventCatalogService;

    @InjectMocks
    private NotificationEventCatalogController notificationEventCatalogController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(notificationEventCatalogController)
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/management/notifications/events")
    class GetAllEnabled {

        @Test
        @DisplayName("should return 200 with list of catalog entries")
        void get_all_enabled_returns_ok() throws Exception {
            NotificationEventCatalogResponse entry = NotificationEventCatalogResponse.builder()
                    .id(UUID.randomUUID()).eventKey("SITE_CONFIG_UPDATED")
                    .entityType("SITE_CONFIG").isEnabled(true).build();

            when(notificationEventCatalogService.getAllEnabled(TENANT_ID))
                    .thenReturn(List.of(entry));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventKey").value("SITE_CONFIG_UPDATED"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no events")
        void get_all_enabled_returns_empty() throws Exception {
            when(notificationEventCatalogService.getAllEnabled(TENANT_ID))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
