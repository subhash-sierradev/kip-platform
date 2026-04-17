package com.integration.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.IntegrationJobExecutionDto;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.confluence.ConfluencePageDto;
import com.integration.execution.contract.rest.response.confluence.ConfluenceSpaceDto;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.SchedulingException;
import com.integration.management.model.dto.request.ConfluenceIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import com.integration.management.model.dto.response.ConfluenceIntegrationResponse;
import com.integration.management.model.dto.response.ConfluenceIntegrationSummaryResponse;
import com.integration.management.service.ConfluenceIntegrationService;
import com.integration.management.service.ConfluenceLookupService;
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

import java.time.Instant;
import java.time.LocalTime;
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
@DisplayName("ConfluenceIntegrationController")
class ConfluenceIntegrationControllerTest {

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final UUID INTEGRATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONNECTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String BASE_URL = "/api/integrations/confluence";

    @Mock
    private ConfluenceIntegrationService confluenceIntegrationService;

    @Mock
    private ConfluenceLookupService confluenceLookupService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ConfluenceIntegrationController(
                        confluenceIntegrationService, confluenceLookupService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /api/integrations/confluence")
    class Create {

        @Test
        @DisplayName("should create integration and return 201")
        void create_validRequest_returnsCreated() throws Exception {
            ConfluenceIntegrationCreateUpdateRequest request = buildValidRequest();
            CreationResponse response = CreationResponse.builder()
                    .id(INTEGRATION_ID.toString())
                    .name(request.getName())
                    .build();

            when(confluenceIntegrationService.create(any(), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(INTEGRATION_ID.toString()));
        }

        @Test
        @DisplayName("should return 400 when request is invalid")
        void create_invalidRequest_returnsBadRequest() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/integrations/confluence/{id}")
    class Update {

        @Test
        @DisplayName("should update integration and return 200")
        void update_validRequest_returnsOk() throws Exception {
            ConfluenceIntegrationCreateUpdateRequest request = buildValidRequest();
            CreationResponse response = CreationResponse.builder()
                    .id(INTEGRATION_ID.toString())
                    .name(request.getName())
                    .build();

            when(confluenceIntegrationService.update(eq(INTEGRATION_ID), any(), eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/" + INTEGRATION_ID)
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(INTEGRATION_ID.toString()));
        }

        @Test
        @DisplayName("should return 400 when scheduling exception occurs during update")
        void update_schedulingException_returnsBadRequest() throws Exception {
            ConfluenceIntegrationCreateUpdateRequest request = buildValidRequest();

            when(confluenceIntegrationService.update(eq(INTEGRATION_ID), any(), eq(TENANT_ID), eq(USER_ID)))
                    .thenThrow(new SchedulingException("schedule error"));

            mockMvc.perform(put(BASE_URL + "/" + INTEGRATION_ID)
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/confluence")
    class GetAll {

        @Test
        @DisplayName("should return all integrations for tenant")
        void getAllByTenant_returnsIntegrationList() throws Exception {
            ConfluenceIntegrationSummaryResponse summary = ConfluenceIntegrationSummaryResponse.builder()
                    .id(INTEGRATION_ID)
                    .name("Test Integration")
                    .build();

            when(confluenceIntegrationService.getAllByTenant(TENANT_ID))
                    .thenReturn(List.of(summary));

            mockMvc.perform(get(BASE_URL)
                            .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Test Integration"));
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/confluence/{id}")
    class GetById {

        @Test
        @DisplayName("should return integration details")
        void getById_existingIntegration_returnsDetails() throws Exception {
            ConfluenceIntegrationResponse response = ConfluenceIntegrationResponse.builder()
                    .id(INTEGRATION_ID)
                    .name("Test Integration")
                    .build();

            when(confluenceIntegrationService.getByIdAndTenantWithDetails(INTEGRATION_ID, TENANT_ID))
                    .thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + INTEGRATION_ID)
                            .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(INTEGRATION_ID.toString()));
        }

        @Test
        @DisplayName("should return 404 when integration not found")
        void getById_notFound_returns404() throws Exception {
            when(confluenceIntegrationService.getByIdAndTenantWithDetails(INTEGRATION_ID, TENANT_ID))
                    .thenThrow(new IntegrationNotFoundException("Not found"));

            mockMvc.perform(get(BASE_URL + "/" + INTEGRATION_ID)
                            .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/integrations/confluence/{id}")
    class Delete {

        @Test
        @DisplayName("should delete integration and return 204")
        void delete_existingIntegration_returnsNoContent() throws Exception {
            doNothing().when(confluenceIntegrationService).delete(INTEGRATION_ID, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/" + INTEGRATION_ID)
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNoContent());

            verify(confluenceIntegrationService).delete(INTEGRATION_ID, TENANT_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/confluence/{id}/executions")
    class GetJobHistory {

        @Test
        @DisplayName("should return job execution history")
        void getJobHistory_returnsExecutions() throws Exception {
            IntegrationJobExecutionDto execution = IntegrationJobExecutionDto.builder()
                    .id(UUID.randomUUID())
                    .status(JobExecutionStatus.SUCCESS)
                    .startedAt(Instant.now())
                    .build();

            when(confluenceIntegrationService.getJobHistory(INTEGRATION_ID, TENANT_ID))
                    .thenReturn(List.of(execution));

            mockMvc.perform(get(BASE_URL + "/" + INTEGRATION_ID + "/executions")
                            .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/confluence/{id}/trigger")
    class Trigger {

        @Test
        @DisplayName("should trigger job execution and return 200")
        void trigger_existingIntegration_returnsOk() throws Exception {
            doNothing().when(confluenceIntegrationService)
                    .triggerJobExecution(INTEGRATION_ID, TENANT_ID, USER_ID);

            mockMvc.perform(post(BASE_URL + "/" + INTEGRATION_ID + "/trigger")
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/confluence/{integrationId}/executions/{originalJobId}/retry")
    class Retry {

        @Test
        @DisplayName("should retry execution and return 202")
        void retry_validRequest_returnsAccepted() throws Exception {
            UUID jobId = UUID.randomUUID();
            doNothing().when(confluenceIntegrationService)
                    .retryJobExecution(INTEGRATION_ID, jobId, TENANT_ID, USER_ID);

            mockMvc.perform(post(BASE_URL + "/" + INTEGRATION_ID + "/executions/" + jobId + "/retry")
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isAccepted());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/confluence/{id}/toggle")
    class Toggle {

        @Test
        @DisplayName("should toggle integration status and return new status")
        void toggle_returnsNewStatus() throws Exception {
            when(confluenceIntegrationService.toggleActiveStatus(INTEGRATION_ID, TENANT_ID, USER_ID))
                    .thenReturn(true);

            mockMvc.perform(post(BASE_URL + "/" + INTEGRATION_ID + "/toggle")
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("should return 400 when scheduling exception occurs during toggle")
        void toggle_schedulingException_returnsBadRequest() throws Exception {
            when(confluenceIntegrationService.toggleActiveStatus(INTEGRATION_ID, TENANT_ID, USER_ID))
                    .thenThrow(new SchedulingException("toggle failed"));

            mockMvc.perform(post(BASE_URL + "/" + INTEGRATION_ID + "/toggle")
                            .requestAttr(X_TENANT_ID, TENANT_ID)
                            .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/confluence/normalized/names")
    class GetNormalizedNames {

        @Test
        @DisplayName("should return normalized names for tenant")
        void getNormalizedNames_returnsNames() throws Exception {
            when(confluenceIntegrationService.getAllConfluenceNormalizedNamesByTenantId(TENANT_ID))
                    .thenReturn(List.of("integration-one", "integration-two"));

            mockMvc.perform(get(BASE_URL + "/normalized/names")
                            .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/confluence/connections/{connectionId}/spaces")
    class GetSpaces {

        @Test
        @DisplayName("should return spaces for connection")
        void getSpaces_returnsSpaceList() throws Exception {
            ConfluenceSpaceDto space = ConfluenceSpaceDto.builder()
                    .key("PROJ")
                    .name("Project Space")
                    .build();

            when(confluenceLookupService.getSpacesByConnectionId(CONNECTION_ID, TENANT_ID))
                    .thenReturn(List.of(space));

            mockMvc.perform(get(BASE_URL + "/connections/" + CONNECTION_ID + "/spaces")
                            .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].key").value("PROJ"));
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/confluence/connections/{connectionId}/spaces/{spaceKey}/pages")
    class GetPages {

        @Test
        @DisplayName("should return pages for connection and space key")
        void getPages_returnPageList() throws Exception {
            ConfluencePageDto page = ConfluencePageDto.builder()
                    .id("page-1")
                    .title("Home Page")
                    .build();

            when(confluenceLookupService.getPagesByConnectionIdAndSpaceKey(CONNECTION_ID, TENANT_ID, "PROJ"))
                    .thenReturn(List.of(page));

            mockMvc.perform(get(BASE_URL + "/connections/" + CONNECTION_ID + "/spaces/PROJ/pages")
                            .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Home Page"));
        }
    }

    private ConfluenceIntegrationCreateUpdateRequest buildValidRequest() {
        IntegrationScheduleRequest schedule = IntegrationScheduleRequest.builder()
                .frequencyPattern(FrequencyPattern.DAILY)
                .executionTime(LocalTime.of(6, 0))
                .timeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY)
                .build();

        return ConfluenceIntegrationCreateUpdateRequest.builder()
                .name("Test Confluence Integration")
                .documentItemType("INCIDENT")
                .documentItemSubtype("GENERAL")
                .languageCodes(List.of("en"))
                .reportNameTemplate("Report {date}")
                .confluenceSpaceKey("PROJ")
                .connectionId(CONNECTION_ID)
                .schedule(schedule)
                .build();
    }
}





