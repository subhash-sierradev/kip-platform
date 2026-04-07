package com.integration.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.execution.contract.rest.response.arcgis.ArcGISFieldDto;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.exception.IntegrationApiException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.exception.SchedulingException;
import com.integration.management.model.dto.request.ArcGISIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import com.integration.management.model.dto.response.ArcGISIntegrationResponse;
import com.integration.management.model.dto.response.ArcGISIntegrationSummaryResponse;
import com.integration.management.service.ArcGISIntegrationService;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcGISIntegrationController")
class ArcGISIntegrationControllerTest {

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final UUID INTEGRATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String BASE_URL = "/api/integrations/arcgis";

    @Mock
    private ArcGISIntegrationService arcGISIntegrationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ArcGISIntegrationController(arcGISIntegrationService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /api/integrations/arcgis")
    class Create {

        @Test
        @DisplayName("should create integration and return 201")
        void create_validRequest_returnsCreated() throws Exception {
            ArcGISIntegrationCreateUpdateRequest request = buildValidRequest();
            CreationResponse response = CreationResponse.builder()
                    .id(INTEGRATION_ID.toString())
                    .name(request.getName())
                    .build();

            when(arcGISIntegrationService.create(any(ArcGISIntegrationCreateUpdateRequest.class), eq(TENANT_ID),
                    eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(INTEGRATION_ID.toString()))
                    .andExpect(jsonPath("$.name").value(request.getName()));

            verify(arcGISIntegrationService).create(any(ArcGISIntegrationCreateUpdateRequest.class), eq(TENANT_ID),
                    eq(USER_ID));
        }

        @Test
        @DisplayName("should return 400 when request body is invalid")
        void create_invalidRequest_returnsBadRequest() throws Exception {
            String invalidJson = "{\"name\":\"\"}";

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());

            verify(arcGISIntegrationService, never()).create(any(), any(), any());
        }

        @Test
        @DisplayName("should propagate service exceptions")
        void create_serviceThrowsException_returns5xx() throws Exception {
            ArcGISIntegrationCreateUpdateRequest request = buildValidRequest();

            when(arcGISIntegrationService.create(any(), eq(TENANT_ID), eq(USER_ID)))
                    .thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(post(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/integrations/arcgis/{id}")
    class Update {

        @Test
        @DisplayName("should update integration and return 200")
        void update_validRequest_returnsOk() throws Exception {
            ArcGISIntegrationCreateUpdateRequest request = buildValidRequest();
            CreationResponse response = CreationResponse.builder()
                    .id(INTEGRATION_ID.toString())
                    .name(request.getName())
                    .build();

            when(arcGISIntegrationService.update(eq(INTEGRATION_ID), any(ArcGISIntegrationCreateUpdateRequest.class),
                    eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{id}", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(INTEGRATION_ID.toString()))
                    .andExpect(jsonPath("$.name").value(request.getName()));

            verify(arcGISIntegrationService).update(eq(INTEGRATION_ID), any(ArcGISIntegrationCreateUpdateRequest.class),
                    eq(TENANT_ID), eq(USER_ID));
        }

        @Test
        @DisplayName("should return 400 when schedule update throws SchedulingException")
        void update_schedulingException_returnsBadRequest() throws Exception {
            ArcGISIntegrationCreateUpdateRequest request = buildValidRequest();

            when(arcGISIntegrationService.update(eq(INTEGRATION_ID), any(), eq(TENANT_ID), eq(USER_ID)))
                    .thenThrow(new SchedulingException("Invalid cron"));

            mockMvc.perform(put(BASE_URL + "/{id}", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when integration not found")
        void update_notFound_returnsNotFound() throws Exception {
            ArcGISIntegrationCreateUpdateRequest request = buildValidRequest();

            when(arcGISIntegrationService.update(eq(INTEGRATION_ID), any(), eq(TENANT_ID), eq(USER_ID)))
                    .thenThrow(new IntegrationNotFoundException("not found"));

            mockMvc.perform(put(BASE_URL + "/{id}", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/arcgis")
    class GetAll {

        @Test
        @DisplayName("should return 200 with list")
        void getAll_returnsOk() throws Exception {
            ArcGISIntegrationSummaryResponse r1 = ArcGISIntegrationSummaryResponse.builder()
                    .id(INTEGRATION_ID)
                    .name("ArcGIS A")
                    .lastStatus(JobExecutionStatus.SUCCESS)
                    .lastAttemptTimeUtc(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            when(arcGISIntegrationService.getAllByTenant(TENANT_ID)).thenReturn(List.of(r1));

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("ArcGIS A"));

            verify(arcGISIntegrationService).getAllByTenant(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/arcgis/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 with details")
        void getById_returnsOk() throws Exception {
            ArcGISIntegrationResponse response = ArcGISIntegrationResponse.builder()
                    .id(INTEGRATION_ID)
                    .name("ArcGIS A")
                    .build();

            when(arcGISIntegrationService.getByIdAndTenantWithDetails(INTEGRATION_ID, TENANT_ID)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/{id}", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(INTEGRATION_ID.toString()))
                    .andExpect(jsonPath("$.name").value("ArcGIS A"));

            verify(arcGISIntegrationService).getByIdAndTenantWithDetails(INTEGRATION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return 404 when integration not found")
        void getById_notFound_returnsNotFound() throws Exception {
            when(arcGISIntegrationService.getByIdAndTenantWithDetails(INTEGRATION_ID, TENANT_ID))
                    .thenThrow(new IntegrationNotFoundException("not found"));

            mockMvc.perform(get(BASE_URL + "/{id}", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/integrations/arcgis/{id}")
    class Delete {

        @Test
        @DisplayName("should delete and return 204")
        void delete_returnsNoContent() throws Exception {
            doNothing().when(arcGISIntegrationService).delete(INTEGRATION_ID, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/{id}", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNoContent());

            verify(arcGISIntegrationService).delete(INTEGRATION_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should return 404 when integration not found")
        void delete_notFound_returnsNotFound() throws Exception {
            doThrow(new IntegrationNotFoundException("not found"))
                    .when(arcGISIntegrationService).delete(INTEGRATION_ID, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/{id}", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/arcgis/{id}/trigger")
    class Trigger {

        @Test
        @DisplayName("should trigger and return 200")
        void trigger_returnsOk() throws Exception {
            doNothing().when(arcGISIntegrationService).triggerJobExecution(INTEGRATION_ID, TENANT_ID, USER_ID);

            mockMvc.perform(post(BASE_URL + "/{id}/trigger", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk());

            verify(arcGISIntegrationService).triggerJobExecution(INTEGRATION_ID, TENANT_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/arcgis/{id}/toggle")
    class Toggle {

        @Test
        @DisplayName("should toggle and return 200")
        void toggle_returnsOk() throws Exception {
            when(arcGISIntegrationService.toggleActiveStatus(INTEGRATION_ID, TENANT_ID, USER_ID)).thenReturn(true);

            mockMvc.perform(post(BASE_URL + "/{id}/toggle", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(arcGISIntegrationService).toggleActiveStatus(INTEGRATION_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should toggle to disabled and return 200")
        void toggle_returnsOkFalse() throws Exception {
            when(arcGISIntegrationService.toggleActiveStatus(INTEGRATION_ID, TENANT_ID, USER_ID)).thenReturn(false);

            mockMvc.perform(post(BASE_URL + "/{id}/toggle", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(arcGISIntegrationService).toggleActiveStatus(INTEGRATION_ID, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should return 400 when toggling throws SchedulingException")
        void toggle_schedulingException_returnsBadRequest() throws Exception {
            when(arcGISIntegrationService.toggleActiveStatus(INTEGRATION_ID, TENANT_ID, USER_ID))
                    .thenThrow(new SchedulingException("scheduler down"));

            mockMvc.perform(post(BASE_URL + "/{id}/toggle", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/arcgis/{id}/field-mappings")
    class GetFieldMappings {

        @Test
        @DisplayName("should return 200 with field mappings")
        void getFieldMappings_returnsOk() throws Exception {
            IntegrationFieldMappingDto mapping1 = IntegrationFieldMappingDto.builder()
                    .sourceFieldPath("id")
                    .targetFieldPath("external_location_id")
                    .transformationType(FieldTransformationType.PASSTHROUGH)
                    .isMandatory(true)
                    .displayOrder(0)
                    .build();

            IntegrationFieldMappingDto mapping2 = IntegrationFieldMappingDto.builder()
                    .sourceFieldPath("case.caseNumber")
                    .targetFieldPath("CASE_NUMBER")
                    .transformationType(FieldTransformationType.PASSTHROUGH)
                    .isMandatory(false)
                    .displayOrder(1)
                    .build();

            when(arcGISIntegrationService.getFieldMappings(INTEGRATION_ID, TENANT_ID))
                    .thenReturn(List.of(mapping1, mapping2));

            mockMvc.perform(get(BASE_URL + "/{id}/field-mappings", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].sourceFieldPath").value("id"))
                    .andExpect(jsonPath("$[0].targetFieldPath").value("external_location_id"))
                    .andExpect(jsonPath("$[0].transformationType").value("PASSTHROUGH"))
                    .andExpect(jsonPath("$[0].isMandatory").value(true))
                    .andExpect(jsonPath("$[0].displayOrder").value(0))
                    .andExpect(jsonPath("$[1].sourceFieldPath").value("case.caseNumber"))
                    .andExpect(jsonPath("$[1].targetFieldPath").value("CASE_NUMBER"))
                    .andExpect(jsonPath("$[1].isMandatory").value(false));

            verify(arcGISIntegrationService).getFieldMappings(INTEGRATION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return 200 with empty list when no mappings exist")
        void getFieldMappings_noMappings_returnsEmptyList() throws Exception {
            when(arcGISIntegrationService.getFieldMappings(INTEGRATION_ID, TENANT_ID))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{id}/field-mappings", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(arcGISIntegrationService).getFieldMappings(INTEGRATION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should enforce tenant isolation")
        void getFieldMappings_differentTenant_returnsEmptyOrNotFound() throws Exception {
            when(arcGISIntegrationService.getFieldMappings(INTEGRATION_ID, TENANT_ID))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{id}/field-mappings", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(arcGISIntegrationService).getFieldMappings(INTEGRATION_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/arcgis/normalized/names")
    class GetNormalizedNames {

        @Test
        @DisplayName("should return 200 with list")
        void getNormalizedNames_returnsOk() throws Exception {
            when(arcGISIntegrationService.getAllArcGISNormalizedNamesByTenantId(TENANT_ID))
                    .thenReturn(List.of("arcgis_a"));

            mockMvc.perform(get(BASE_URL + "/normalized/names")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0]").value("arcgis_a"));

            verify(arcGISIntegrationService).getAllArcGISNormalizedNamesByTenantId(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/arcgis/connections/{connectionId}/features")
    class FetchArcGISFields {

        @Test
        @DisplayName("should return 200 with list of fields")
        void fetchArcGISFields_validRequest_returnsOkWithList() throws Exception {
            List<ArcGISFieldDto> fields = List.of(
                    ArcGISFieldDto.builder().name("objectid").type("esriFieldTypeOID").build(),
                    ArcGISFieldDto.builder().name("createdDate").type("esriFieldTypeDate").build());

            when(arcGISIntegrationService.fetchArcGISFields(INTEGRATION_ID, TENANT_ID)).thenReturn(fields);

            mockMvc.perform(get(BASE_URL + "/connections/{connectionId}/features", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("objectid"))
                    .andExpect(jsonPath("$[1].name").value("createdDate"));

            verify(arcGISIntegrationService).fetchArcGISFields(INTEGRATION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return 400 for invalid UUID connectionId")
        void fetchArcGISFields_invalidUuid_returnsBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL + "/connections/{connectionId}/features", "not-a-uuid")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("should map IntegrationApiException status code")
        void fetchArcGISFields_serviceThrowsIntegrationApiException_mapsStatus() throws Exception {
            when(arcGISIntegrationService.fetchArcGISFields(eq(INTEGRATION_ID), eq(TENANT_ID)))
                    .thenThrow(new IntegrationApiException("Upstream error", 502));

            mockMvc.perform(get(BASE_URL + "/connections/{connectionId}/features", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadGateway())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            verify(arcGISIntegrationService).fetchArcGISFields(INTEGRATION_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return 500 when service throws runtime exception")
        void fetchArcGISFields_serviceThrowsRuntimeException_returns5xx() throws Exception {
            when(arcGISIntegrationService.fetchArcGISFields(INTEGRATION_ID, TENANT_ID))
                    .thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get(BASE_URL + "/connections/{connectionId}/features", INTEGRATION_ID)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().is5xxServerError())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            verify(arcGISIntegrationService).fetchArcGISFields(INTEGRATION_ID, TENANT_ID);
        }
    }

    private static ArcGISIntegrationCreateUpdateRequest buildValidRequest() {
        IntegrationScheduleRequest schedule = IntegrationScheduleRequest.builder()
                .executionDate(LocalDate.now())
                .executionTime(LocalTime.of(12, 30))
                .frequencyPattern(FrequencyPattern.DAILY)
                .timeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY)
                .build();

        IntegrationFieldMappingDto mandatoryMapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("id")
                .targetFieldPath("external_location_id")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(true)
                .build();

        return ArcGISIntegrationCreateUpdateRequest.builder()
                .name("ArcGIS Integration")
                .description("desc")
                .itemType("DOCUMENT")
                .itemSubtype("LOCATION")
                .dynamicDocumentType("Case")
                .connectionId(UUID.randomUUID())
                .schedule(schedule)
                .fieldMappings(List.of(mandatoryMapping))
                .build();
    }
}
