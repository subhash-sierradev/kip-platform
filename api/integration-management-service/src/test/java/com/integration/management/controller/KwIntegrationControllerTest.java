package com.integration.management.controller;

import com.integration.management.service.KwIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("KwIntegrationController")
class KwIntegrationControllerTest {

    @Mock
    private KwIntegrationService kwIntegrationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new KwIntegrationController(kwIntegrationService)).build();
    }

    @Test
    @DisplayName("GET /api/kw/dynamic-documents-types returns 200")
    void getDynamicDocuments_ok() throws Exception {
        when(kwIntegrationService.getDynamicDocuments(anyString(), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/kw/dynamic-documents-types")
                .param("type", "item")
                .param("subType", "sub"))
                .andExpect(status().isOk());

        verify(kwIntegrationService).getDynamicDocuments("item", "sub");
    }

    @Test
    @DisplayName("GET /api/kw/item-subtypes returns 200")
    void getItemSubtypes_ok() throws Exception {
        when(kwIntegrationService.getItemSubtypesList()).thenReturn(List.of());

        mockMvc.perform(get("/api/kw/item-subtypes"))
                .andExpect(status().isOk());

        verify(kwIntegrationService).getItemSubtypesList();
    }

    @Test
    @DisplayName("GET /api/kw/source-field-mappings returns 200")
    void getFieldMappingData_ok() throws Exception {
        when(kwIntegrationService.getFieldMappingForLocations()).thenReturn(List.of());

        mockMvc.perform(get("/api/kw/source-field-mappings"))
                .andExpect(status().isOk());

        verify(kwIntegrationService).getFieldMappingForLocations();
    }
}
