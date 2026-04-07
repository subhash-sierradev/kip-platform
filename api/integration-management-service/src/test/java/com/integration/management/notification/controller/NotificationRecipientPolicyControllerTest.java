package com.integration.management.notification.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.notification.model.dto.response.RecipientPolicyResponse;
import com.integration.management.notification.service.NotificationRecipientPolicyService;
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
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRecipientPolicyController")
class NotificationRecipientPolicyControllerTest {

    private static final String TENANT_ID = "tenant-xyz";
    private static final String USER_ID = "user-xyz";
    private static final String BASE_URL = "/api/management/notifications/policies";

    @Mock private NotificationRecipientPolicyService recipientPolicyService;

    @InjectMocks
    private NotificationRecipientPolicyController notificationRecipientPolicyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(notificationRecipientPolicyController)
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/management/notifications/policies")
    class GetPolicies {

        @Test
        @DisplayName("should return 200 with list of policies")
        void get_policies_returns_ok() throws Exception {
            RecipientPolicyResponse policy = buildResponse(UUID.randomUUID());

            when(recipientPolicyService.getPoliciesForTenant(TENANT_ID))
                    .thenReturn(List.of(policy));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("should return 200 with empty list when no policies")
        void get_policies_returns_empty() throws Exception {
            when(recipientPolicyService.getPoliciesForTenant(TENANT_ID))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/management/notifications/policies")
    class CreatePolicy {

        @Test
        @DisplayName("should return 201 when policy created successfully")
        void create_policy_returns_created() throws Exception {
            UUID ruleId = UUID.randomUUID();
            UUID policyId = UUID.randomUUID();
            RecipientPolicyResponse response = buildResponse(policyId);

            when(recipientPolicyService.create(eq(TENANT_ID), eq(USER_ID), any()))
                    .thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ruleId\":\"" + ruleId + "\",\"recipientType\":\"ALL_USERS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(policyId.toString()));
        }

        @Test
        @DisplayName("should return 400 when request body is invalid")
        void create_policy_returns_bad_request_when_invalid() throws Exception {
            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"recipientType\":\"ALL_USERS\"}")) // missing ruleId
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/management/notifications/policies/{id}")
    class UpdatePolicy {

        @Test
        @DisplayName("should return 200 when policy updated successfully")
        void update_policy_returns_ok() throws Exception {
            UUID ruleId = UUID.randomUUID();
            UUID policyId = UUID.randomUUID();
            RecipientPolicyResponse response = buildResponse(policyId);

            when(recipientPolicyService.update(eq(TENANT_ID), eq(USER_ID), eq(policyId), any()))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{id}", policyId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ruleId\":\"" + ruleId + "\",\"recipientType\":\"ADMINS_ONLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(policyId.toString()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/management/notifications/policies/{id}")
    class DeletePolicy {

        @Test
        @DisplayName("should return 204 when policy deleted successfully")
        void delete_policy_returns_no_content() throws Exception {
            UUID policyId = UUID.randomUUID();
            doNothing().when(recipientPolicyService).delete(TENANT_ID, policyId);

            mockMvc.perform(delete(BASE_URL + "/{id}", policyId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                .andExpect(status().isNoContent());

            verify(recipientPolicyService).delete(TENANT_ID, policyId);
        }
    }

    private RecipientPolicyResponse buildResponse(UUID id) {
        return RecipientPolicyResponse.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .recipientType("ALL_USERS")
                .build();
    }
}
