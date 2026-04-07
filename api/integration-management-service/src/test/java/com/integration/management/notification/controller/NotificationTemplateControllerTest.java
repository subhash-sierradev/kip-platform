package com.integration.management.notification.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.service.NotificationTemplateService;
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
@DisplayName("NotificationTemplateController")
class NotificationTemplateControllerTest {

    private static final String TENANT_ID = "tenant-xyz";
    private static final String BASE_URL = "/api/management/notifications/templates";

    @Mock private NotificationTemplateService notificationTemplateService;

    @InjectMocks
    private NotificationTemplateController notificationTemplateController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(notificationTemplateController)
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/management/notifications/templates")
    class GetTemplates {

        @Test
        @DisplayName("should return 200 with list of templates")
        void get_templates_returns_ok() throws Exception {
            NotificationTemplateResponse t1 = NotificationTemplateResponse.builder()
                    .id(UUID.randomUUID()).tenantId(TENANT_ID)
                    .eventKey("SITE_CONFIG_UPDATED")
                    .titleTemplate("Config updated").messageTemplate("Config {{configKey}} changed")
                    .build();

            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of(t1));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("should return 200 with empty list when no templates")
        void get_templates_returns_empty() throws Exception {
            when(notificationTemplateService.getTemplatesForTenant(TENANT_ID))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
