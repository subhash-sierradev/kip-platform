package com.integration.execution.controller;

import com.integration.execution.arcgisverification.ArcGISVerificationService;
import com.integration.execution.contract.rest.response.arcgis.ArcGISVerificationPageResponse;
import com.integration.execution.controller.advice.GenericExceptionHandler;
import com.integration.execution.controller.advice.SpecificExceptionHandler;
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
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcGISVerificationController (IES)")
class ArcGISVerificationControllerTest {

    private static final String BASE_URL = "/api/execution/arcgis/verification/features";

    @Mock
    private ArcGISVerificationService arcGISVerificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArcGISVerificationController(arcGISVerificationService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /features — success cases")
    class SuccessCases {

        @Test
        @DisplayName("returns 200 with features when no filters provided")
        void getFeatures_noFilters_returnsOk() throws Exception {
            ArcGISVerificationPageResponse response = ArcGISVerificationPageResponse.builder()
                    .features(List.of(Map.of("OBJECTID", 1L, "external_location_id", "LOC-1")))
                    .fetchedCount(1)
                    .offset(0)
                    .exceededTransferLimit(false)
                    .build();

            when(arcGISVerificationService.queryFeatures(0, null, null)).thenReturn(response);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fetchedCount").value(1))
                    .andExpect(jsonPath("$.offset").value(0))
                    .andExpect(jsonPath("$.exceededTransferLimit").value(false))
                    .andExpect(jsonPath("$.features").isArray());

            verify(arcGISVerificationService).queryFeatures(0, null, null);
        }

        @Test
        @DisplayName("passes objectId filter to service")
        void getFeatures_withObjectId_passesFilterToService() throws Exception {
            ArcGISVerificationPageResponse response = ArcGISVerificationPageResponse.builder()
                    .features(List.of(Map.of("OBJECTID", 42L)))
                    .fetchedCount(1)
                    .offset(0)
                    .exceededTransferLimit(false)
                    .build();

            when(arcGISVerificationService.queryFeatures(0, "42", null)).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .param("objectId", "42")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fetchedCount").value(1));

            verify(arcGISVerificationService).queryFeatures(0, "42", null);
        }

        @Test
        @DisplayName("passes locationId and offset to service")
        void getFeatures_withLocationIdAndOffset_passesParamsToService() throws Exception {
            ArcGISVerificationPageResponse response = ArcGISVerificationPageResponse.builder()
                    .features(List.of())
                    .fetchedCount(0)
                    .offset(1000)
                    .exceededTransferLimit(true)
                    .build();

            when(arcGISVerificationService.queryFeatures(1000, null, "LOC-001"))
                    .thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .param("offset", "1000")
                            .param("locationId", "LOC-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exceededTransferLimit").value(true))
                    .andExpect(jsonPath("$.offset").value(1000));
        }
    }
}
