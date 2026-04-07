package com.integration.management.notification.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.execution.contract.model.enums.NotificationSeverity;
import com.integration.management.notification.model.dto.response.NotificationRuleResponse;
import com.integration.management.notification.service.NotificationDefaultRulesService;
import com.integration.management.notification.service.NotificationRuleService;
import jakarta.persistence.EntityNotFoundException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRuleController")
class NotificationRuleControllerTest {

    private static final String TENANT_ID = "tenant-xyz";
    private static final String BASE_URL = "/api/management/notifications/rules";

    @Mock
    private NotificationRuleService notificationRuleService;

    @Mock
    private NotificationDefaultRulesService notificationDefaultRulesService;

    @InjectMocks
    private NotificationRuleController notificationRuleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(notificationRuleController)
            .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
            .build();
    }

    // ------------------------------------------------------------------------------------
    // GET /api/management/notifications/rules
    // ------------------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/management/notifications/rules")
    class GetRules {

        @Test
        @DisplayName("should return 200 with list of rules")
        void getRules_validTenant_returnsOkWithList() throws Exception {
            NotificationRuleResponse rule1 = buildResponse(UUID.randomUUID(), true);
            NotificationRuleResponse rule2 = buildResponse(UUID.randomUUID(), false);

            when(notificationRuleService.getRulesForTenant(TENANT_ID)).thenReturn(List.of(rule1, rule2));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    // ------------------------------------------------------------------------------------
    // DELETE /api/management/notifications/rules/{id}
    // ------------------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/management/notifications/rules/{id}")
    class DeleteRule {

        @Test
        @DisplayName("should return 204 when rule is deleted successfully")
        void delete_existingRule_returnsNoContent() throws Exception {
            UUID ruleId = UUID.randomUUID();

            doNothing().when(notificationRuleService).delete(TENANT_ID, ruleId);

            mockMvc.perform(delete(BASE_URL + "/{id}", ruleId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                .andExpect(status().isNoContent());

            verify(notificationRuleService).delete(TENANT_ID, ruleId);
        }

        @Test
        @DisplayName("should return 500 when rule not found")
        void delete_ruleNotFound_returnsServerError() throws Exception {
            UUID ruleId = UUID.randomUUID();

            doThrow(new EntityNotFoundException("Not found")).when(notificationRuleService).delete(TENANT_ID, ruleId);

            mockMvc.perform(delete(BASE_URL + "/{id}", ruleId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                .andExpect(status().isInternalServerError());
        }
    }

    // ------------------------------------------------------------------------------------
    // PATCH /api/management/notifications/rules/{id}/toggle
    // ------------------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/management/notifications/rules/{id}/toggle")
    class ToggleEnabled {

        @Test
        @DisplayName("should return 200 with updated rule when toggling enabled rule to disabled")
        void toggleEnabled_enabledRule_returnsOkWithDisabledResponse() throws Exception {
            UUID ruleId = UUID.randomUUID();
            NotificationRuleResponse response = buildResponse(ruleId, false);

            when(notificationRuleService.toggleEnabled(eq(TENANT_ID), eq(ruleId))).thenReturn(response);

            mockMvc.perform(patch(BASE_URL + "/{id}/toggle", ruleId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(false));

            verify(notificationRuleService).toggleEnabled(TENANT_ID, ruleId);
        }

        @Test
        @DisplayName("should return 200 with updated rule when toggling disabled rule to enabled")
        void toggleEnabled_disabledRule_returnsOkWithEnabledResponse() throws Exception {
            UUID ruleId = UUID.randomUUID();
            NotificationRuleResponse response = buildResponse(ruleId, true);

            when(notificationRuleService.toggleEnabled(eq(TENANT_ID), eq(ruleId))).thenReturn(response);

            mockMvc.perform(patch(BASE_URL + "/{id}/toggle", ruleId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnabled").value(true));
        }

        @Test
        @DisplayName("should return 500 when rule not found")
        void toggleEnabled_ruleNotFound_returnsServerError() throws Exception {
            UUID ruleId = UUID.randomUUID();

            when(notificationRuleService.toggleEnabled(eq(TENANT_ID), eq(ruleId)))
                .thenThrow(new EntityNotFoundException("Notification rule not found: " + ruleId));

            mockMvc.perform(patch(BASE_URL + "/{id}/toggle", ruleId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("should return 500 when tenant does not match")
        void toggleEnabled_wrongTenant_returnsServerError() throws Exception {
            UUID ruleId = UUID.randomUUID();

            when(notificationRuleService.toggleEnabled(eq(TENANT_ID), eq(ruleId)))
                .thenThrow(new EntityNotFoundException("Notification rule not found: " + ruleId));

            mockMvc.perform(patch(BASE_URL + "/{id}/toggle", ruleId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        }
    }

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    private NotificationRuleResponse buildResponse(UUID id, boolean isEnabled) {
        return NotificationRuleResponse.builder()
            .id(id)
            .tenantId(TENANT_ID)
            .severity(NotificationSeverity.INFO)
            .isEnabled(isEnabled)
            .build();
    }

    // ------------------------------------------------------------------------------------
    // POST /api/management/notifications/rules
    // ------------------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/management/notifications/rules")
    class CreateRule {

        @Test
        @DisplayName("should return 201 when rule created successfully")
        void create_rule_returns_created() throws Exception {
            UUID eventId = UUID.randomUUID();
            UUID ruleId = UUID.randomUUID();
            NotificationRuleResponse response = buildResponse(ruleId, true);

            when(notificationRuleService.create(eq(TENANT_ID), eq("user-1"), any()))
                    .thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, "user-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"eventId\":\"" + eventId + "\",\"severity\":\"INFO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ruleId.toString()));
        }
    }

    // ------------------------------------------------------------------------------------
    // PUT /api/management/notifications/rules/{id}
    // ------------------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/management/notifications/rules/{id}")
    class UpdateRule {

        @Test
        @DisplayName("should return 200 when rule updated successfully")
        void update_rule_returns_ok() throws Exception {
            UUID ruleId = UUID.randomUUID();
            NotificationRuleResponse response = buildResponse(ruleId, true);

            when(notificationRuleService.updateRule(eq(TENANT_ID), eq("user-1"),
                    eq(ruleId), any()))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{id}", ruleId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, "user-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"severity\":\"WARNING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ruleId.toString()));
        }
    }

    // ------------------------------------------------------------------------------------
    // POST /api/management/notifications/rules/batch
    // ------------------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/management/notifications/rules/batch")
    class CreateBatch {

        @Test
        @DisplayName("should return 201 when batch rules created")
        void create_batch_returns_created() throws Exception {
            NotificationRuleResponse r1 = buildResponse(UUID.randomUUID(), true);
            NotificationRuleResponse r2 = buildResponse(UUID.randomUUID(), true);

            when(notificationRuleService.createBatch(eq(TENANT_ID), eq("user-1"), any()))
                    .thenReturn(List.of(r1, r2));

            UUID eventId = UUID.randomUUID();
            String body = "{\"rules\":[{\"eventId\":\"" + eventId
                    + "\",\"severity\":\"INFO\"}],\"recipientType\":\"ALL_USERS\"}";

            mockMvc.perform(post(BASE_URL + "/batch")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, "user-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    // ------------------------------------------------------------------------------------
    // POST /api/management/notifications/rules/reset-defaults
    // ------------------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/management/notifications/rules/reset-defaults")
    class ResetDefaults {

        @Test
        @DisplayName("should return 200 with restored count")
        void reset_defaults_returns_ok() throws Exception {
            when(notificationDefaultRulesService.resetRulesToDefaults(TENANT_ID, "user-1"))
                    .thenReturn(8);

            mockMvc.perform(post(BASE_URL + "/reset-defaults")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rulesRestored").value(8));

            verify(notificationDefaultRulesService).resetRulesToDefaults(TENANT_ID, "user-1");
        }
    }
}
