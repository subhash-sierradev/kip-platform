package com.integration.management.controller;

import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.config.aspect.AuditLoggable;
import com.integration.management.controller.advice.FeignClientExceptionHandler;
import com.integration.management.controller.advice.GenericExceptionHandler;
import com.integration.management.controller.advice.SpecificExceptionHandler;
import com.integration.management.exception.IntegrationConnectionDeleteConflictException;
import com.integration.management.model.dto.response.ConnectionDependentsResponse;
import com.integration.management.service.IntegrationConnectionService;
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

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.integration.execution.contract.model.enums.AuditActivity.CREATE;
import static com.integration.execution.contract.model.enums.EntityType.INTEGRATION_CONNECTION;
import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;
import static com.integration.management.constants.ManagementSecurityConstants.X_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
@DisplayName("IntegrationConnectionController")
class IntegrationConnectionControllerTest {

    private static final String BASE_URL = "/api/integrations/connections";
    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";

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

    @Nested
    @DisplayName("POST /api/integrations/connections/test-connection")
    class TestAndCreateConnection {

        @Test
        @DisplayName("should be audit logged as connection creation")
        void testAndCreateConnection_hasAuditLogAnnotation() throws Exception {
            Method method = IntegrationConnectionController.class.getMethod(
                    "testAndCreateConnection",
                    IntegrationConnectionRequest.class,
                    String.class,
                    String.class);

            AuditLoggable auditLoggable = method.getAnnotation(AuditLoggable.class);

            assertThat(auditLoggable).isNotNull();
            assertThat(auditLoggable.entityType()).isEqualTo(INTEGRATION_CONNECTION);
            assertThat(auditLoggable.action()).isEqualTo(CREATE);
        }

        @Test
        @DisplayName("should return service response (created)")
        void testAndCreateConnection_serviceReturnsCreated_returnsCreated() throws Exception {
            IntegrationConnectionResponse response = IntegrationConnectionResponse.builder()
                    .id("conn-1")
                    .name("Conn A")
                    .secretName("secret-1")
                    .serviceType(ServiceType.JIRA)
                    .lastConnectionStatus(ConnectionStatus.SUCCESS)
                    .lastConnectionTest(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            ResponseEntity<?> serviceResponse = ResponseEntity.status(HttpStatus.CREATED).body(response);

            when(integrationConnectionService.testAndCreateConnection(any(IntegrationConnectionRequest.class),
                    eq(TENANT_ID), eq(USER_ID)))
                    .thenReturn((ResponseEntity) serviceResponse);

            mockMvc.perform(post(BASE_URL + "/test-connection")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validTestConnectionRequestJson()))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value("conn-1"))
                    .andExpect(jsonPath("$.name").value("Conn A"))
                    .andExpect(jsonPath("$.secretName").value("secret-1"))
                    .andExpect(jsonPath("$.serviceType").value("JIRA"));

            verify(integrationConnectionService).testAndCreateConnection(any(IntegrationConnectionRequest.class),
                    eq(TENANT_ID), eq(USER_ID));
        }

        @Test
        @DisplayName("should return 400 when request body is invalid")
        void testAndCreateConnection_invalidBody_returnsBadRequest() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection")
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest());

            verify(integrationConnectionService, never())
                    .testAndCreateConnection(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/connections")
    class GetConnections {

        @Test
        @DisplayName("should return 200 with list")
        void getConnections_validRequest_returnsOkWithList() throws Exception {
            List<IntegrationConnectionResponse> connections = List.of(
                    IntegrationConnectionResponse.builder().id("c1").name("Conn 1").build(),
                    IntegrationConnectionResponse.builder().id("c2").name("Conn 2").build());

            when(integrationConnectionService.getConnectionsByTenantAndServiceType(TENANT_ID, ServiceType.JIRA))
                    .thenReturn(connections);

            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .param("serviceType", "JIRA"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value("c1"))
                    .andExpect(jsonPath("$[1].id").value("c2"));

            verify(integrationConnectionService)
                    .getConnectionsByTenantAndServiceType(TENANT_ID, ServiceType.JIRA);
        }

        @Test
        @DisplayName("should return 400 for invalid serviceType")
        void getConnections_invalidServiceType_returnsBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .param("serviceType", "NOT_A_TYPE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                    .andExpect(jsonPath("$.message", containsString("Invalid value")));

            verify(integrationConnectionService, never())
                    .getConnectionsByTenantAndServiceType(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/connections/{connectionId}")
    class GetConnectionById {

        @Test
        @DisplayName("should return 200 with connection")
        void getConnectionById_validId_returnsOk() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            IntegrationConnectionResponse response = IntegrationConnectionResponse.builder()
                    .id(connectionId.toString())
                    .name("Conn A")
                    .serviceType(ServiceType.JIRA)
                    .build();

            when(integrationConnectionService.getConnectionById(connectionId, TENANT_ID)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/{connectionId}", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(connectionId.toString()))
                    .andExpect(jsonPath("$.name").value("Conn A"));

            verify(integrationConnectionService).getConnectionById(connectionId, TENANT_ID);
        }

        @Test
        @DisplayName("should return 400 for invalid UUID")
        void getConnectionById_invalidUuid_returnsBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{connectionId}", "not-a-uuid")
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));

            verify(integrationConnectionService, never()).getConnectionById(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/integrations/connections/{connectionId}/test")
    class TestConnection {

        @Test
        @DisplayName("should return 200 with test response")
        void testConnection_validRequest_returnsOkWithResponse() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            ConnectionTestResponse response = ConnectionTestResponse.builder()
                    .success(true)
                    .statusCode(200)
                    .message("ok")
                    .secretName("secret-1")
                    .connectionStatus(ConnectionStatus.SUCCESS)
                    .build();

            when(integrationConnectionService.testExistingConnection(connectionId, TENANT_ID)).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/{connectionId}/test", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("ok"));

            verify(integrationConnectionService).testExistingConnection(connectionId, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/integrations/connections/{connectionId}/dependents")
    class GetDependents {

        @Test
        @DisplayName("should return 200 with dependents")
        void getDependents_validRequest_returnsOkWithResponse() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            ConnectionDependentsResponse response = ConnectionDependentsResponse.builder()
                    .serviceType(ServiceType.JIRA)
                    .build();

            when(integrationConnectionService.getDependents(connectionId, TENANT_ID)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/{connectionId}/dependents", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.serviceType").value("JIRA"));

            verify(integrationConnectionService).getDependents(connectionId, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("DELETE /api/integrations/connections/{connectionId}")
    class DeleteConnection {

        @Test
        @DisplayName("should return 204")
        void deleteConnection_validRequest_returnsNoContent() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            doNothing().when(integrationConnectionService)
                    .deleteConnection(connectionId, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/{connectionId}", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isNoContent());

            verify(integrationConnectionService)
                    .deleteConnection(connectionId, TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("should return 409 when dependents exist")
        void deleteConnection_conflict_returnsConflict() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Map<String, Object> details = Map.of("reason", "hasDependents");

            doThrow(new IntegrationConnectionDeleteConflictException("Conflict", details))
                    .when(integrationConnectionService)
                    .deleteConnection(connectionId, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/{connectionId}", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode").value("INTEGRATION_CONNECTION_DELETE_CONFLICT"))
                    .andExpect(jsonPath("$.details.reason").value("hasDependents"));
        }

        @Test
        @DisplayName("should return 400 when service throws IllegalArgumentException")
        void deleteConnection_serviceThrowsIllegalArgumentException_returnsBadRequest() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            doThrow(new IllegalArgumentException("bad"))
                    .when(integrationConnectionService)
                    .deleteConnection(connectionId, TENANT_ID, USER_ID);

            mockMvc.perform(delete(BASE_URL + "/{connectionId}", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message")
                            .value("Invalid connectionId format: " + connectionId));
        }
    }

    @Nested
    @DisplayName("PUT /api/integrations/connections/{connectionId}/secret")
    class RotateSecret {

        @Test
        @DisplayName("should return 204")
        void rotateSecret_validRequest_returnsNoContent() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            doNothing().when(integrationConnectionService)
                    .rotateConnectionSecret(eq(connectionId), eq(TENANT_ID), eq(USER_ID),
                            any(IntegrationConnectionSecretRotateRequest.class));

            mockMvc.perform(put(BASE_URL + "/{connectionId}/secret", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRotateSecretRequestJson()))
                    .andExpect(status().isNoContent());

            verify(integrationConnectionService)
                    .rotateConnectionSecret(eq(connectionId), eq(TENANT_ID), eq(USER_ID),
                            any(IntegrationConnectionSecretRotateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when newSecret is blank")
        void rotateSecret_blankNewSecret_returnsBadRequest() throws Exception {
            UUID connectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            mockMvc.perform(put(BASE_URL + "/{connectionId}/secret", connectionId)
                    .requestAttr(X_TENANT_ID, TENANT_ID)
                    .requestAttr(X_USER_ID, USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"secretName\":\"s\",\"serviceType\":\"JIRA\",\"newSecret\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

            verify(integrationConnectionService, never())
                    .rotateConnectionSecret(any(), any(), any(), any());
        }
    }

    private static String validTestConnectionRequestJson() {
        return "{"
                + "\"name\":\"Conn A\","
                + "\"serviceType\":\"JIRA\","
                + "\"integrationSecret\":{"
                + "\"baseUrl\":\"https://jira.example.com\","
                + "\"authType\":\"BASIC_AUTH\","
                + "\"credentials\":{"
                + "\"username\":\"user\","
                + "\"password\":\"pass\""
                + "}"
                + "}"
                + "}";
    }

    private static String validRotateSecretRequestJson() {
        return "{"
                + "\"secretName\":\"secret-1\","
                + "\"serviceType\":\"JIRA\","
                + "\"newSecret\":\"new-secret\""
                + "}";
    }
}
