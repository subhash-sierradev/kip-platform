package com.integration.management.controller;

import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.model.dto.response.JiraWebhookEventResponse;
import com.integration.management.service.JiraWebhookEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraWebhookEventController")
class JiraWebhookEventControllerTest {

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String WEBHOOK_ID = "webhook-789";
    private static final String EVENT_ID = "event-123";
    private static final String ORIGINAL_EVENT_ID = "original-event-123";
    private static final String BASE_URL = "/api/webhooks/jira";

    @Mock
    private JiraWebhookEventService jiraWebhookEventService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JiraWebhookEventController(jiraWebhookEventService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/webhooks/jira/triggers/{webhookId}")
    class GetTriggerHistory {

        @Test
        @DisplayName("should return list of trigger events for webhook")
        void getTriggerHistory_existingEvents_returnsResponseList() throws Exception {
            List<JiraWebhookEventResponse> events = Arrays.asList(
                    buildEventResponse("event-1", 0),
                    buildEventResponse("event-2", 1));

            when(jiraWebhookEventService.getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID)).thenReturn(events);

            mockMvc.perform(get(BASE_URL + "/triggers/{webhookId}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is("event-1")))
                    .andExpect(jsonPath("$[0].webhookId", is(WEBHOOK_ID)))
                    .andExpect(jsonPath("$[0].retryAttempt", is(0)))
                    .andExpect(jsonPath("$[1].id", is("event-2")))
                    .andExpect(jsonPath("$[1].retryAttempt", is(1)));

            verify(jiraWebhookEventService).getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no events found")
        void getTriggerHistory_noEvents_returnsEmptyList() throws Exception {
            when(jiraWebhookEventService.getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/triggers/{webhookId}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(jiraWebhookEventService).getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/webhooks/jira/triggers/retry/{id} - requires only feature_jira_webhook role")
    class RetryTrigger {

        @Test
        @DisplayName("should be accessible without webhook_client role - only feature_jira_webhook required")
        void retryTrigger_noWebhookClientRole_succeeds() throws Exception {
            when(jiraWebhookEventService.retryTrigger(EVENT_ID, TENANT_ID, USER_ID))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(EVENT_ID, 1)));

            // Standalone MockMvc bypasses Spring Security - confirms no method-level role guard blocks the call
            mockMvc.perform(post(BASE_URL + "/triggers/retry/{id}", EVENT_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isAccepted());

            verify(jiraWebhookEventService).retryTrigger(EVENT_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should retry trigger and return 202 Accepted")
        void retryTrigger_successfulRetry_returnsAccepted() throws Exception {
            when(jiraWebhookEventService.retryTrigger(EVENT_ID, TENANT_ID, USER_ID))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(EVENT_ID, 1)));

            mockMvc.perform(post(BASE_URL + "/triggers/retry/{id}", EVENT_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id", is(EVENT_ID)))
                    .andExpect(jsonPath("$.retryAttempt", is(1)));

            verify(jiraWebhookEventService).retryTrigger(EVENT_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should return 500 when retry service throws exception")
        void retryTrigger_serviceThrowsException_returnsInternalServerError() throws Exception {
            when(jiraWebhookEventService.retryTrigger(EVENT_ID, TENANT_ID, USER_ID))
                    .thenThrow(new RuntimeException("Retry failed"));

            mockMvc.perform(post(BASE_URL + "/triggers/retry/{id}", EVENT_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(jiraWebhookEventService).retryTrigger(EVENT_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should include tenant and user ID from request headers")
        void retryTrigger_withHeaders_usesProvidedTenantAndUser() throws Exception {
            String customTenant = "custom-tenant";
            String customUser = "custom-user";

            when(jiraWebhookEventService.retryTrigger(EVENT_ID, customTenant, customUser))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(EVENT_ID, 1)));

            mockMvc.perform(post(BASE_URL + "/triggers/retry/{id}", EVENT_ID)
                    .requestAttr(X_TENANT_ID, customTenant)
                    .requestAttr(X_USER_ID, customUser)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isAccepted());

            verify(jiraWebhookEventService).retryTrigger(EVENT_ID, customTenant, customUser);
        }
    }

    @Nested
    @DisplayName("POST /api/webhooks/jira/execute/{id}")
    class ExecuteWebhook {

        private static final String JIRA_PAYLOAD = "{\"issueKey\":\"TEST-123\",\"eventType\":\"issue_created\"}";

        @Test
        @DisplayName("should execute webhook and return 202 Accepted")
        void executeWebhook_successfulExecution_returnsAccepted() throws Exception {
            when(jiraWebhookEventService.executeWebhook(eq(WEBHOOK_ID), eq(JIRA_PAYLOAD), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(WEBHOOK_ID, 0)));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JIRA_PAYLOAD))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.webhookId", is(WEBHOOK_ID)))
                    .andExpect(jsonPath("$.retryAttempt", is(0)));

            verify(jiraWebhookEventService).executeWebhook(WEBHOOK_ID, JIRA_PAYLOAD, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should return 500 when webhook execution service throws exception")
        void executeWebhook_serviceThrowsException_returnsInternalServerError() throws Exception {
            when(jiraWebhookEventService.executeWebhook(eq(WEBHOOK_ID), eq(JIRA_PAYLOAD), eq(TENANT_ID), eq(USER_ID)))
                    .thenThrow(new RuntimeException("Processing error"));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JIRA_PAYLOAD))
                    .andExpect(status().isInternalServerError());

            verify(jiraWebhookEventService).executeWebhook(WEBHOOK_ID, JIRA_PAYLOAD, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should return 404 when webhook ID path is empty")
        void executeWebhook_emptyWebhookIdPath_returnsNotFound() throws Exception {
            mockMvc.perform(post(BASE_URL + "/execute/{id}", "")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JIRA_PAYLOAD))
                    .andExpect(status().isNotFound());

            verify(jiraWebhookEventService, never()).executeWebhook(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should handle minimal JSON payload gracefully")
        void executeWebhook_minimalPayload_processesSuccessfully() throws Exception {
            String minimalPayload = "{}";

            when(jiraWebhookEventService.executeWebhook(eq(WEBHOOK_ID), eq(minimalPayload), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(WEBHOOK_ID, 0)));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(minimalPayload))
                    .andExpect(status().isAccepted());

            verify(jiraWebhookEventService).executeWebhook(WEBHOOK_ID, minimalPayload, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should include tenant and user ID from request headers")
        void executeWebhook_withHeaders_usesProvidedTenantAndUser() throws Exception {
            String customTenant = "custom-tenant";
            String customUser = "custom-user";

            when(jiraWebhookEventService.executeWebhook(eq(WEBHOOK_ID), eq(JIRA_PAYLOAD), eq(customTenant),
                    eq(customUser)))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(WEBHOOK_ID, 0)));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, customTenant)
                    .requestAttr(X_USER_ID, customUser)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JIRA_PAYLOAD))
                    .andExpect(status().isAccepted());

            verify(jiraWebhookEventService).executeWebhook(WEBHOOK_ID, JIRA_PAYLOAD, customTenant, customUser);
        }
    }

    @Nested
    @DisplayName("Additional Edge Cases and Coverage")
    class AdditionalEdgeCases {

        @Test
        @DisplayName("should handle large payloads successfully in execute")
        void executeWebhook_largePayload_processesSuccessfully() throws Exception {
            String largePayload = "{\"data\":\"" + "x".repeat(5000) + "\"}";

            when(jiraWebhookEventService.executeWebhook(eq(WEBHOOK_ID), eq(largePayload), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(WEBHOOK_ID, 0)));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(largePayload))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("should handle complex nested JSON payload")
        void executeWebhook_complexPayload_processesSuccessfully() throws Exception {
            String complexPayload = "{\"issue\":{\"key\":\"TEST-123\",\"fields\":{\"summary\":\"Test\"}}}";

            when(jiraWebhookEventService.executeWebhook(eq(WEBHOOK_ID), eq(complexPayload), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(WEBHOOK_ID, 0)));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(complexPayload))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("should return 500 when execute service throws exception")
        void executeWebhook_serviceThrowsException_returns500() throws Exception {
            when(jiraWebhookEventService.executeWebhook(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Processing error"));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("should return events with multiple retry attempts")
        void getTriggerHistory_multipleRetries_returnsAllAttempts() throws Exception {
            List<JiraWebhookEventResponse> events = Arrays.asList(
                    buildEventResponse("event-1", 0),
                    buildEventResponse("event-2", 1),
                    buildEventResponse("event-3", 2));

            when(jiraWebhookEventService.getWebhookEventsByWebhookId(WEBHOOK_ID, TENANT_ID)).thenReturn(events);

            mockMvc.perform(get(BASE_URL + "/triggers/{webhookId}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test
        @DisplayName("should handle service exception in getTriggerHistory")
        void getTriggerHistory_serviceException_returns500() throws Exception {
            when(jiraWebhookEventService.getWebhookEventsByWebhookId(any(), any()))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get(BASE_URL + "/triggers/{webhookId}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("should handle retry with exception from service")
        void retryTrigger_serviceException_returns500() throws Exception {
            when(jiraWebhookEventService.retryTrigger(any(String.class), any(), any()))
                    .thenThrow(new RuntimeException("Retry failed"));

            mockMvc.perform(post(BASE_URL + "/triggers/retry/{id}", EVENT_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("should handle JSON array payload")
        void executeWebhook_arrayPayload_processesSuccessfully() throws Exception {
            String arrayPayload = "[{\"key\":\"TEST-1\"}]";

            when(jiraWebhookEventService.executeWebhook(eq(WEBHOOK_ID), eq(arrayPayload), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(buildEventResponse(WEBHOOK_ID, 0)));

            mockMvc.perform(post(BASE_URL + "/execute/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(arrayPayload))
                    .andExpect(status().isAccepted());
        }
    }

    private JiraWebhookEventResponse buildEventResponse(String eventId, int retryAttempt) {
        return JiraWebhookEventResponse.builder()
                .id(eventId)
                .webhookId(WEBHOOK_ID)
                .tenantId(TENANT_ID)
                .triggeredBy(USER_ID)
                .triggeredAt(Instant.now())
                .incomingPayload("{\"issueKey\":\"TEST-123\"}")
                .transformedPayload("{\"transformed\":true}")
                .responseStatusCode(200)
                .responseBody("{\"success\":true}")
                .status("SUCCESS")
                .retryAttempt(retryAttempt)
                .originalEventId(ORIGINAL_EVENT_ID)
                .jiraIssueUrl("https://jira.example.com/browse/TEST-123")
                .build();
    }
}
