package com.integration.management.controller;

import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.entity.CredentialType;
import com.integration.management.service.MasterDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("MasterDataController")
class MasterDataControllerTest {

    private static final String BASE_URL = "/api";

    @Mock
    private MasterDataService masterDataService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MasterDataController(masterDataService))
                .setControllerAdvice(new SpecificExceptionHandler(), new GenericExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/credential-types")
    class GetAllEnabledCredentialTypes {

        @Test
        @DisplayName("should return 200 with list of credential types")
        void getAllEnabledCredentialTypes_validRequest_returnsOkWithList() throws Exception {
            CredentialType basicAuth = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.BASIC_AUTH)
                    .displayName("Basic Authentication")
                    .isEnabled(true)
                    .requiredFields(List.of("username", "password"))
                    .build();

            CredentialType oauth2 = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.OAUTH2)
                    .displayName("OAuth 2.0")
                    .isEnabled(true)
                    .requiredFields(List.of("clientId", "clientSecret", "tokenUrl"))
                    .build();

            when(masterDataService.getAllCredentialTypes()).thenReturn(List.of(basicAuth, oauth2));

            mockMvc.perform(get(BASE_URL + "/credential-types"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].credentialAuthType").value("BASIC_AUTH"))
                    .andExpect(jsonPath("$[0].displayName").value("Basic Authentication"))
                    .andExpect(jsonPath("$[0].isEnabled").value(true))
                    .andExpect(jsonPath("$[0].requiredFields", hasSize(2)))
                    .andExpect(jsonPath("$[1].credentialAuthType").value("OAUTH2"))
                    .andExpect(jsonPath("$[1].displayName").value("OAuth 2.0"));

            verify(masterDataService).getAllCredentialTypes();
        }

        @Test
        @DisplayName("should return empty list when no credential types found")
        void getAllEnabledCredentialTypes_noTypes_returnsEmptyList() throws Exception {
            when(masterDataService.getAllCredentialTypes()).thenReturn(new ArrayList<>());

            mockMvc.perform(get(BASE_URL + "/credential-types"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(masterDataService).getAllCredentialTypes();
        }

        @Test
        @DisplayName("should return only enabled credential types")
        void getAllEnabledCredentialTypes_onlyEnabled_returnsEnabledList() throws Exception {
            CredentialType enabledType = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.BASIC_AUTH)
                    .displayName("Basic Authentication")
                    .isEnabled(true)
                    .requiredFields(List.of("username", "password"))
                    .build();

            when(masterDataService.getAllCredentialTypes()).thenReturn(List.of(enabledType));

            mockMvc.perform(get(BASE_URL + "/credential-types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].isEnabled").value(true));

            verify(masterDataService).getAllCredentialTypes();
        }

        @Test
        @DisplayName("should handle service exception")
        void getAllEnabledCredentialTypes_serviceThrowsException_returns500() throws Exception {
            when(masterDataService.getAllCredentialTypes())
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get(BASE_URL + "/credential-types"))
                    .andExpect(status().is5xxServerError());

            verify(masterDataService).getAllCredentialTypes();
        }

        @Test
        @DisplayName("should verify required fields structure in response")
        void getAllEnabledCredentialTypes_validRequest_returnsRequiredFieldsStructure() throws Exception {
            CredentialType credentialType = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.OAUTH2)
                    .displayName("OAuth 2.0")
                    .isEnabled(true)
                    .requiredFields(List.of("clientId", "clientSecret", "tokenUrl", "scope"))
                    .build();

            when(masterDataService.getAllCredentialTypes()).thenReturn(List.of(credentialType));

            mockMvc.perform(get(BASE_URL + "/credential-types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].requiredFields", hasSize(4)))
                    .andExpect(jsonPath("$[0].requiredFields[0]").value("clientId"))
                    .andExpect(jsonPath("$[0].requiredFields[1]").value("clientSecret"))
                    .andExpect(jsonPath("$[0].requiredFields[2]").value("tokenUrl"))
                    .andExpect(jsonPath("$[0].requiredFields[3]").value("scope"));

            verify(masterDataService).getAllCredentialTypes();
        }
    }

    @Nested
    @DisplayName("GET /api/credential-types/{authType}")
    class FindByCredentialAuthType {

        @ParameterizedTest
        @EnumSource(CredentialAuthType.class)
        @DisplayName("should return 200 with credential type for each auth type")
        void findByCredentialAuthType_validAuthType_returnsOkWithDetails(CredentialAuthType authType) throws Exception {
            CredentialType credentialType = CredentialType.builder()
                    .credentialAuthType(authType)
                    .displayName(authType.name().replace("_", " "))
                    .isEnabled(true)
                    .requiredFields(List.of("field1", "field2"))
                    .build();

            when(masterDataService.findByCredentialAuthType(authType)).thenReturn(credentialType);

            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", authType))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.credentialAuthType").value(authType.name()))
                    .andExpect(jsonPath("$.isEnabled").value(true))
                    .andExpect(jsonPath("$.requiredFields", hasSize(2)));

            verify(masterDataService).findByCredentialAuthType(authType);
        }

        @Test
        @DisplayName("should return 200 with BASIC_AUTH details")
        void findByCredentialAuthType_basicAuth_returnsOkWithDetails() throws Exception {
            CredentialType basicAuth = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.BASIC_AUTH)
                    .displayName("Basic Authentication")
                    .isEnabled(true)
                    .requiredFields(List.of("username", "password"))
                    .build();

            when(masterDataService.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                    .thenReturn(basicAuth);

            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "BASIC_AUTH"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.credentialAuthType").value("BASIC_AUTH"))
                    .andExpect(jsonPath("$.displayName").value("Basic Authentication"))
                    .andExpect(jsonPath("$.isEnabled").value(true))
                    .andExpect(jsonPath("$.requiredFields", hasSize(2)))
                    .andExpect(jsonPath("$.requiredFields[0]").value("username"))
                    .andExpect(jsonPath("$.requiredFields[1]").value("password"));

            verify(masterDataService).findByCredentialAuthType(CredentialAuthType.BASIC_AUTH);
        }

        @Test
        @DisplayName("should return 200 with OAUTH2 details")
        void findByCredentialAuthType_oauth2_returnsOkWithDetails() throws Exception {
            CredentialType oauth2 = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.OAUTH2)
                    .displayName("OAuth 2.0")
                    .isEnabled(true)
                    .requiredFields(List.of("clientId", "clientSecret", "tokenUrl"))
                    .build();

            when(masterDataService.findByCredentialAuthType(CredentialAuthType.OAUTH2))
                    .thenReturn(oauth2);

            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "OAUTH2"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.credentialAuthType").value("OAUTH2"))
                    .andExpect(jsonPath("$.displayName").value("OAuth 2.0"))
                    .andExpect(jsonPath("$.isEnabled").value(true))
                    .andExpect(jsonPath("$.requiredFields", hasSize(3)));

            verify(masterDataService).findByCredentialAuthType(CredentialAuthType.OAUTH2);
        }

        @Test
        @DisplayName("should handle invalid auth type")
        void findByCredentialAuthType_invalidAuthType_returnsBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "INVALID_TYPE"))
                    .andExpect(status().isBadRequest());

            verify(masterDataService, never()).findByCredentialAuthType(any());
        }

        @Test
        @DisplayName("should handle service exception when credential type not found")
        void findByCredentialAuthType_typeNotFound_returnsBadRequest() throws Exception {
            when(masterDataService.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                    .thenThrow(new IllegalArgumentException("Invalid credential auth type: BASIC_AUTH"));

            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "BASIC_AUTH"))
                    .andExpect(status().isBadRequest());

            verify(masterDataService).findByCredentialAuthType(CredentialAuthType.BASIC_AUTH);
        }

        @Test
        @DisplayName("should handle disabled credential type")
        void findByCredentialAuthType_disabledType_returnsOk() throws Exception {
            CredentialType disabledType = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.BASIC_AUTH)
                    .displayName("Basic Authentication")
                    .isEnabled(false)
                    .requiredFields(List.of("username", "password"))
                    .build();

            when(masterDataService.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                    .thenReturn(disabledType);

            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "BASIC_AUTH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isEnabled").value(false));

            verify(masterDataService).findByCredentialAuthType(CredentialAuthType.BASIC_AUTH);
        }

        @Test
        @DisplayName("should handle credential type with empty required fields")
        void findByCredentialAuthType_emptyRequiredFields_returnsOk() throws Exception {
            CredentialType credentialType = CredentialType.builder()
                    .credentialAuthType(CredentialAuthType.BASIC_AUTH)
                    .displayName("Basic Authentication")
                    .isEnabled(true)
                    .requiredFields(new ArrayList<>())
                    .build();

            when(masterDataService.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                    .thenReturn(credentialType);

            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "BASIC_AUTH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requiredFields", hasSize(0)));

            verify(masterDataService).findByCredentialAuthType(CredentialAuthType.BASIC_AUTH);
        }

        @Test
        @DisplayName("should verify case sensitivity for auth type")
        void findByCredentialAuthType_lowerCaseAuthType_returnsBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "basic_auth"))
                    .andExpect(status().isBadRequest());

            verify(masterDataService, never()).findByCredentialAuthType(any());
        }

        @Test
        @DisplayName("should handle null response from service")
        void findByCredentialAuthType_nullResponse_returnsOk() throws Exception {
            when(masterDataService.findByCredentialAuthType(CredentialAuthType.BASIC_AUTH))
                    .thenReturn(null);

            mockMvc.perform(get(BASE_URL + "/credential-types/{authType}", "BASIC_AUTH"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(masterDataService).findByCredentialAuthType(CredentialAuthType.BASIC_AUTH);
        }
    }
}
