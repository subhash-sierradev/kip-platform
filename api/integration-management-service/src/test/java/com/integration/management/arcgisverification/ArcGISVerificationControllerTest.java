package com.integration.management.arcgisverification;

import com.integration.execution.contract.rest.response.arcgis.ArcGISVerificationPageResponse;
import com.integration.management.controller.advice.FeignClientExceptionHandler;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.ies.client.IesArcGISVerificationClient;
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
@DisplayName("ArcGISVerificationController (IMS)")
class ArcGISVerificationControllerTest {

    private static final String BASE_URL = "/api/management/arcgis/verification/features";

    @Mock
    private IesArcGISVerificationClient iesArcGISVerificationClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArcGISVerificationController(iesArcGISVerificationClient))
                .setControllerAdvice(
                        new SpecificExceptionHandler(),
                        new FeignClientExceptionHandler(),
                        new GenericExceptionHandler())
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

            when(iesArcGISVerificationClient.getFeatures(0, null, null)).thenReturn(response);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fetchedCount").value(1))
                    .andExpect(jsonPath("$.offset").value(0))
                    .andExpect(jsonPath("$.exceededTransferLimit").value(false))
                    .andExpect(jsonPath("$.features").isArray());

            verify(iesArcGISVerificationClient).getFeatures(0, null, null);
        }

        @Test
        @DisplayName("passes objectId to Feign client")
        void getFeatures_withObjectId_passesParamToFeignClient() throws Exception {
            ArcGISVerificationPageResponse response = ArcGISVerificationPageResponse.builder()
                    .features(List.of(Map.of("OBJECTID", 42L)))
                    .fetchedCount(1)
                    .offset(0)
                    .exceededTransferLimit(false)
                    .build();

            when(iesArcGISVerificationClient.getFeatures(0, "42", null)).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .param("objectId", "42")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fetchedCount").value(1));

            verify(iesArcGISVerificationClient).getFeatures(0, "42", null);
        }

        @Test
        @DisplayName("passes locationId and offset to Feign client")
        void getFeatures_withLocationIdAndOffset_passesParamsToFeignClient() throws Exception {
            ArcGISVerificationPageResponse response = ArcGISVerificationPageResponse.builder()
                    .features(List.of())
                    .fetchedCount(0)
                    .offset(1000)
                    .exceededTransferLimit(true)
                    .build();

            when(iesArcGISVerificationClient.getFeatures(1000, null, "LOC-001"))
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
