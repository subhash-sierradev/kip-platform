package com.integration.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.JiraFieldMappingDto;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.entity.JiraWebhook;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.model.dto.request.JiraWebhookCreateUpdateRequest;
import com.integration.management.model.dto.response.JiraWebhookDetailResponse;
import com.integration.management.model.dto.response.JiraWebhookSummaryResponse;
import com.integration.management.service.JiraWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraWebhookController")
class JiraWebhookControllerTest {

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String WEBHOOK_ID = "webhook-789";
    private static final String BASE_URL = "/api/webhooks/jira";

    @Mock
    private JiraWebhookService jiraWebhookService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JiraWebhookController(jiraWebhookService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/webhooks/jira")
    class CreateWebhook {

        @Test
        @DisplayName("should create webhook and return 201 with creation response")
        void create_validRequest_returnsCreatedWithResponse() throws Exception {
            JiraWebhookCreateUpdateRequest request = buildValidCreateRequest();
            JiraWebhook createdWebhook = buildMockWebhook();

            when(jiraWebhookService.create(any(JiraWebhookCreateUpdateRequest.class), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(createdWebhook);

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(WEBHOOK_ID))
                    .andExpect(jsonPath("$.name").value("Incident Webhook"))
                    .andExpect(jsonPath("$.webhookUrl").exists());

            verify(jiraWebhookService).create(any(JiraWebhookCreateUpdateRequest.class), eq(TENANT_ID), eq(USER_ID));
        }

        @Test
        @DisplayName("should return 400 when request body is invalid")
        void create_invalidRequest_returnsBadRequest() throws Exception {
            String invalidJson = "{\"name\": \"\"}";

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());

            verify(jiraWebhookService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("should propagate service exceptions")
        void create_serviceThrowsException_propagatesException() throws Exception {
            JiraWebhookCreateUpdateRequest request = buildValidCreateRequest();

            when(jiraWebhookService.create(any(), eq(TENANT_ID), eq(USER_ID)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/webhooks/jira/{webhookId}")
    class GetWebhookById {

        @Test
        @DisplayName("should return 200 with webhook details")
        void getById_validId_returnsOkWithDetails() throws Exception {
            JiraWebhookDetailResponse response = buildMockDetailResponse();

            when(jiraWebhookService.getById(WEBHOOK_ID, TENANT_ID)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/{webhookId}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(WEBHOOK_ID))
                    .andExpect(jsonPath("$.name").value("Incident Webhook"))
                    .andExpect(jsonPath("$.webhookUrl").exists())
                    .andExpect(jsonPath("$.jiraFieldMappings", hasSize(3)));

            verify(jiraWebhookService).getById(WEBHOOK_ID, TENANT_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = "")
        @DisplayName("should return 404 for empty webhook ID due to path mismatch")
        void getById_emptyId_returnsNotFound(String webhookId) throws Exception {
            mockMvc.perform(get(BASE_URL + "/{webhookId}", webhookId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isNotFound());

            verify(jiraWebhookService, never()).getById(any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = { "  ", " " })
        @DisplayName("should return 400 for blank webhook ID")
        void getById_blankId_returnsBadRequest(String webhookId) throws Exception {
            mockMvc.perform(get(BASE_URL + "/{webhookId}", webhookId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadRequest());

            verify(jiraWebhookService, never()).getById(any(), any());
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void getById_notFound_returnsNotFound() throws Exception {
            when(jiraWebhookService.getById(WEBHOOK_ID, TENANT_ID))
                    .thenThrow(new IntegrationNotFoundException("Webhook not found"));

            mockMvc.perform(get(BASE_URL + "/{webhookId}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/webhooks/jira/{id}")
    class UpdateWebhook {

        @Test
        @DisplayName("should update webhook and return 204")
        void update_validRequest_returnsNoContent() throws Exception {
            JiraWebhookCreateUpdateRequest request = buildValidCreateRequest();

            doNothing().when(jiraWebhookService).update(eq(WEBHOOK_ID), any(), eq(TENANT_ID), eq(USER_ID));

            mockMvc.perform(put(BASE_URL + "/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(jiraWebhookService).update(eq(WEBHOOK_ID), any(JiraWebhookCreateUpdateRequest.class), eq(TENANT_ID),
                    eq(USER_ID));
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void update_notFound_returnsNotFound() throws Exception {
            JiraWebhookCreateUpdateRequest request = buildValidCreateRequest();

            doThrow(new IntegrationNotFoundException("Webhook not found"))
                    .when(jiraWebhookService).update(eq(WEBHOOK_ID), any(), eq(TENANT_ID), eq(USER_ID));

            mockMvc.perform(put(BASE_URL + "/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/webhooks/jira/{id}")
    class DeleteWebhook {

        @Test
        @DisplayName("should delete webhook and return 204")
        void delete_validId_returnsNoContent() throws Exception {
            doNothing().when(jiraWebhookService).delete(WEBHOOK_ID, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNoContent());

            verify(jiraWebhookService).delete(WEBHOOK_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void delete_notFound_returnsNotFound() throws Exception {
            doThrow(new IntegrationNotFoundException("Webhook not found"))
                    .when(jiraWebhookService).delete(WEBHOOK_ID, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/{id}", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/webhooks/jira")
    class GetAllWebhooks {

        @Test
        @DisplayName("should return 200 with list of webhooks")
        void getAll_validTenantId_returnsOkWithList() throws Exception {
            List<JiraWebhookSummaryResponse> responses = List.of(
                    buildMockSummaryResponse("webhook-1", "Webhook 1"),
                    buildMockSummaryResponse("webhook-2", "Webhook 2"));

            when(jiraWebhookService.getAllByTenantId(TENANT_ID)).thenReturn(responses);

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value("webhook-1"))
                    .andExpect(jsonPath("$[1].id").value("webhook-2"));

            verify(jiraWebhookService).getAllByTenantId(TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no webhooks exist")
        void getAll_noWebhooks_returnsEmptyList() throws Exception {
            when(jiraWebhookService.getAllByTenantId(TENANT_ID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/webhooks/jira/connections/{connectionId}")
    class GetWebhooksByConnectionId {

        @Test
        @DisplayName("should return 200 with list of webhooks for connection")
        void getByConnectionId_validConnectionId_returnsOkWithList() throws Exception {
            UUID connectionId = UUID.randomUUID();
            List<JiraWebhookDetailResponse> responses = List.of(buildMockDetailResponse());

            when(jiraWebhookService.getByConnectionId(connectionId)).thenReturn(responses);

            mockMvc.perform(get(BASE_URL + "/connections/{connectionId}", connectionId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(WEBHOOK_ID));

            verify(jiraWebhookService).getByConnectionId(connectionId);
        }

        @Test
        @DisplayName("should return empty list when no webhooks for connection")
        void getByConnectionId_noWebhooks_returnsEmptyList() throws Exception {
            UUID connectionId = UUID.randomUUID();

            when(jiraWebhookService.getByConnectionId(connectionId)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/connections/{connectionId}", connectionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/webhooks/jira/{id}/active")
    class ToggleWebhookActive {

        @Test
        @DisplayName("should toggle webhook to enabled and return true")
        void toggleWebhookActive_toEnabled_returnsTrue() throws Exception {
            when(jiraWebhookService.toggleActiveStatus(WEBHOOK_ID, TENANT_ID, USER_ID)).thenReturn(true);

            mockMvc.perform(post(BASE_URL + "/{id}/active", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(jiraWebhookService).toggleActiveStatus(WEBHOOK_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should toggle webhook to disabled and return false")
        void toggleWebhookActive_toDisabled_returnsFalse() throws Exception {
            when(jiraWebhookService.toggleActiveStatus(WEBHOOK_ID, TENANT_ID, USER_ID)).thenReturn(false);

            mockMvc.perform(post(BASE_URL + "/{id}/active", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        @DisplayName("should handle both toggle states")
        void toggleWebhookActive_bothStates_returnsCorrectStatus(boolean newStatus) throws Exception {
            when(jiraWebhookService.toggleActiveStatus(WEBHOOK_ID, TENANT_ID, USER_ID)).thenReturn(newStatus);

            mockMvc.perform(post(BASE_URL + "/{id}/active", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().string(String.valueOf(newStatus)));
        }

        @Test
        @DisplayName("should return 404 when webhook not found")
        void toggleWebhookActive_notFound_returnsNotFound() throws Exception {
            when(jiraWebhookService.toggleActiveStatus(WEBHOOK_ID, TENANT_ID, USER_ID))
                    .thenThrow(new IntegrationNotFoundException("Webhook not found"));

            mockMvc.perform(post(BASE_URL + "/{id}/active", WEBHOOK_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    private static JiraWebhookCreateUpdateRequest buildValidCreateRequest() {
        return JiraWebhookCreateUpdateRequest.builder()
                .name("Incident Webhook")
                .description("Creates incidents from Jira issues")
                .connectionId(UUID.randomUUID())
                .fieldsMapping(List.of(
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("summary")
                                .jiraFieldName("Summary")
                                .displayLabel("Summary")
                                .dataType(JiraDataType.STRING)
                                .required(true)
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("description")
                                .jiraFieldName("Description")
                                .displayLabel("Description")
                                .dataType(JiraDataType.STRING)
                                .required(false)
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("assignee")
                                .jiraFieldName("Assignee")
                                .displayLabel("Assignee")
                                .dataType(JiraDataType.USER)
                                .required(false)
                                .build()))
                .samplePayload("{\"issue\":{\"key\":\"TEST-123\"}}")
                .build();
    }

    private static JiraWebhook buildMockWebhook() {
        return JiraWebhook.builder()
                .id(WEBHOOK_ID)
                .name("Incident Webhook")
                .description("Creates incidents from Jira issues")
                .webhookUrl("https://example.com/webhooks/" + WEBHOOK_ID)
                .connectionId(UUID.randomUUID())
                .isEnabled(true)
                .isDeleted(false)
                .normalizedName("incident webhook")
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .lastModifiedBy(USER_ID)
                .createdDate(Instant.now())
                .lastModifiedDate(Instant.now())
                .version(0L)
                .build();
    }

    private static JiraWebhookDetailResponse buildMockDetailResponse() {
        return JiraWebhookDetailResponse.builder()
                .id(WEBHOOK_ID)
                .name("Incident Webhook")
                .description("Creates incidents from Jira issues")
                .webhookUrl("https://example.com/webhooks/" + WEBHOOK_ID)
                .connectionId(UUID.randomUUID().toString())
                .jiraFieldMappings(List.of(
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("summary")
                                .jiraFieldName("Summary")
                                .displayLabel("Summary")
                                .dataType(JiraDataType.STRING)
                                .required(true)
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("description")
                                .jiraFieldName("Description")
                                .displayLabel("Description")
                                .dataType(JiraDataType.STRING)
                                .required(false)
                                .build(),
                        JiraFieldMappingDto.builder()
                                .jiraFieldId("assignee")
                                .jiraFieldName("Assignee")
                                .displayLabel("Assignee")
                                .dataType(JiraDataType.USER)
                                .required(false)
                                .build()))
                .samplePayload("{\"issue\":{\"key\":\"TEST-123\"}}")
                .isEnabled(true)
                .isDeleted(false)
                .normalizedName("incident webhook")
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .createdDate(Instant.now())
                .version(0L)
                .build();
    }

    private static JiraWebhookSummaryResponse buildMockSummaryResponse(String id, String name) {
        return JiraWebhookSummaryResponse.builder()
                .id(id)
                .name(name)
                .webhookUrl("https://example.com/webhooks/" + id)
                .isEnabled(true)
                .createdDate(Instant.now())
                .lastModifiedDate(Instant.now())
                .build();
    }
}
