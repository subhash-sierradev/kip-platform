package com.integration.management.controller;

import com.integration.management.controller.advice.FeignClientExceptionHandler;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.service.IntegrationConnectionService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationConnectionController - Feign/IES error handling")
class IntegrationConnectionControllerErrorHandlingTest {

    private static final String BASE_URL = "/api/integrations/connections";

    @Mock
    private IntegrationConnectionService integrationConnectionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new IntegrationConnectionController(integrationConnectionService))
                .setControllerAdvice(new SpecificExceptionHandler(), new FeignClientExceptionHandler(),
                        new GenericExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("should return 503 with generic message and not leak IES URL when IES is down")
    void testConnection_iesDown_returns503WithoutUrlLeak() throws Exception {
        UUID connectionId = UUID.randomUUID();

        FeignException feignException = org.mockito.Mockito.mock(FeignException.class);
        when(feignException.status()).thenReturn(-1);
        when(feignException.getMessage()).thenReturn(
                "Connection refused: getsockopt executing POST http://localhost:8081/api/integrations/connections/"
                        + connectionId + "/test");

        when(integrationConnectionService.testExistingConnection(eq(connectionId), any()))
                .thenThrow(feignException);

        mockMvc.perform(post(BASE_URL + "/{connectionId}/test", connectionId)
                .requestAttr(X_TENANT_ID, "tenant-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("External service is temporarily unavailable. Please try again later."))
                .andExpect(jsonPath("$.errorCode").value("EXTERNAL_SERVICE_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("localhost:8081"))))
                .andExpect(content().string(not(containsString("http://"))));
    }
}
