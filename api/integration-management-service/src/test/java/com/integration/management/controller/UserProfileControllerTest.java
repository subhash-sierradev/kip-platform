package com.integration.management.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileController")
class UserProfileControllerTest {

    private static final String TENANT_ID = "tenant-123";
    private static final String BASE_URL = "/api/management/users";

    @Mock
    private UserProfileService userProfileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/management/users")
    class GetUsers {

        @Test
        @DisplayName("should return 200 with list of users")
        void getUsers_validTenant_returnsOkWithList() throws Exception {
            UserProfileResponse u1 = UserProfileResponse.builder()
                    .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                    .keycloakUserId("kc-1")
                    .email("user1@example.com")
                    .displayName("User One")
                    .isTenantAdmin(true)
                    .build();
            UserProfileResponse u2 = UserProfileResponse.builder()
                    .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .keycloakUserId("kc-2")
                    .email("user2@example.com")
                    .displayName("User Two")
                    .isTenantAdmin(false)
                    .build();

            when(userProfileService.getAllUsersByTenant(TENANT_ID)).thenReturn(List.of(u1, u2));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].keycloakUserId").value("kc-1"))
                    .andExpect(jsonPath("$[1].keycloakUserId").value("kc-2"));

            verify(userProfileService).getAllUsersByTenant(TENANT_ID);
        }

        @Test
        @DisplayName("should return 500 when service throws")
        void getUsers_serviceThrows_returns5xx() throws Exception {
            when(userProfileService.getAllUsersByTenant(TENANT_ID))
                    .thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().is5xxServerError())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            verify(userProfileService).getAllUsersByTenant(TENANT_ID);
        }
    }
}
