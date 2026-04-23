package com.integration.management.service;

import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.request.ConnectionReTestRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.entity.IntegrationConnection;
import com.integration.management.exception.IntegrationConnectionDeleteConflictException;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.ies.client.IesConnectionClient;
import com.integration.management.mapper.IntegrationConnectionMapper;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.JiraWebhookRepository;
import com.integration.management.repository.projection.StringIdSummaryWithLastRunProjection;
import com.integration.management.repository.projection.UuidIdSummaryWithLastRunProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationConnectionService")
class IntegrationConnectionServiceTest {

    @Mock
    private IntegrationConnectionRepository connectionRepository;
    @Mock
    private ArcGISIntegrationRepository arcGISIntegrationRepository;
    @Mock
    private JiraWebhookRepository jiraWebhookRepository;
    @Mock
    private IntegrationConnectionMapper connectionMapper;
    @Mock
    private IesConnectionClient iesConnectionClient;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private IntegrationConnectionService service;

    private static IntegrationConnectionRequest jiraBasicRequest() {
        IntegrationSecret secret = IntegrationSecret.builder()
                .baseUrl("https://example.test")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u").password("pw").build())
                .build();
        return new IntegrationConnectionRequest("n", ServiceType.JIRA, secret);
    }

    @Test
    @DisplayName("testAndCreateConnection should throw when duplicate hash exists")
    void testAndCreateConnection_duplicate_throws() {
        IntegrationConnection existing = IntegrationConnection.builder().id(UUID.randomUUID()).name("Dup").build();
        when(connectionRepository.findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.testAndCreateConnection(jiraBasicRequest(), "t1", "u1"))
                .isInstanceOf(IntegrationNameAlreadyExistsException.class)
                .hasMessageContaining("already exists");
        verify(iesConnectionClient, never()).testAndCreateConnection(any());
    }

    @Test
    @DisplayName("testAndCreateConnection should return ok when IES test fails")
    void testAndCreateConnection_testFail_returnsOk() {
        when(connectionRepository.findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());
        ConnectionTestResponse testResponse = ConnectionTestResponse.builder()
                .success(false)
                .statusCode(400)
                .message("bad")
                .build();
        when(iesConnectionClient.testAndCreateConnection(any()))
                .thenReturn(ResponseEntity.ok(testResponse));

        ResponseEntity<?> response = service.testAndCreateConnection(jiraBasicRequest(), "t1", "u1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(testResponse);
        verify(connectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("testAndCreateConnection should persist and return CREATED when IES test succeeds")
    void testAndCreateConnection_success_persistsAndReturnsCreated() {
        when(connectionRepository.findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        ConnectionTestResponse testResponse = ConnectionTestResponse.builder()
                .success(true)
                .secretName("sec")
                .connectionStatus(ConnectionStatus.SUCCESS)
                .lastConnectionTest(Instant.parse("2026-03-03T00:00:00Z"))
                .message("ok")
                .build();
        when(iesConnectionClient.testAndCreateConnection(any()))
                .thenReturn(ResponseEntity.ok(testResponse));
        when(connectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(inv -> {
                    IntegrationConnection c = inv.getArgument(0);
                    if (c.getId() == null) {
                        c.setId(UUID.randomUUID());
                    }
                    return c;
                });
        IntegrationConnectionResponse mapped = IntegrationConnectionResponse.builder().id("x").name("n").build();
        when(connectionMapper.toResponse(any(IntegrationConnection.class))).thenReturn(mapped);

        ResponseEntity<?> response = service.testAndCreateConnection(jiraBasicRequest(), "t1", "u1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(notificationEventPublisher).publish(any());
    }

    @Test
    @DisplayName("testExistingConnection should update connection fields when response returned")
    void testExistingConnection_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id)
                .tenantId("t")
                .secretName("sec")
                .serviceType(ServiceType.JIRA)
                .build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t")).thenReturn(Optional.of(connection));
        ConnectionTestResponse retest = ConnectionTestResponse.builder()
                .success(true)
                .connectionStatus(ConnectionStatus.SUCCESS)
                .message("ok")
                .lastConnectionTest(Instant.parse("2026-03-03T00:00:00Z"))
                .build();
        when(iesConnectionClient.testExistingConnection(eq(id), any(ConnectionReTestRequest.class))).thenReturn(retest);

        ConnectionTestResponse actual = service.testExistingConnection(id, "t");

        assertThat(actual).isSameAs(retest);
        verify(connectionRepository).save(connection);
        assertThat(connection.getLastConnectionStatus()).isEqualTo(ConnectionStatus.SUCCESS);
        assertThat(connection.getLastConnectionMessage()).isEqualTo("ok");
    }

    @Test
    @DisplayName("testExistingConnection should return empty response when IES returns null")
    void testExistingConnection_nullResponse_returnsEmpty() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id)
                .tenantId("t")
                .secretName("sec")
                .serviceType(ServiceType.JIRA)
                .build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t")).thenReturn(Optional.of(connection));
        when(iesConnectionClient.testExistingConnection(eq(id), any(ConnectionReTestRequest.class))).thenReturn(null);

        ConnectionTestResponse actual = service.testExistingConnection(id, "t");

        assertThat(actual).isNotNull();
        verify(connectionRepository, never()).save(any());
    }

    @Nested
    @DisplayName("getIntegrationConnectionNameById")
    class GetName {

        @Test
        @DisplayName("should return secretName when found")
        void returnsSecretName() {
            UUID id = UUID.randomUUID();
            when(connectionRepository.findSecretNameByIdAndTenantId(id, "t"))
                    .thenReturn(Optional.of("secret"));

            assertThat(service.getIntegrationConnectionNameById(id.toString(), "t")).isEqualTo("secret");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException on invalid UUID")
        void invalidUuid_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> service.getIntegrationConnectionNameById("not-a-uuid", "t"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when repository throws")
        void repositoryThrows_propagatesException() {
            UUID id = UUID.randomUUID();
            when(connectionRepository.findSecretNameByIdAndTenantId(id, "t"))
                    .thenThrow(new RuntimeException("boom"));

            assertThatThrownBy(() -> service.getIntegrationConnectionNameById(id.toString(), "t"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("boom");
        }
    }

    @Test
    @DisplayName("getConnectionById should validate inputs")
    void getConnectionById_invalidArgs_throws() {
        assertThatThrownBy(() -> service.getConnectionById(null, "t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getConnectionById(UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deleteConnection should throw conflict when dependents exist")
    void deleteConnection_withDependents_throwsConflict() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id)
                .tenantId("t")
                .serviceType(ServiceType.ARCGIS)
                .name("c")
                .build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));
        UuidIdSummaryWithLastRunProjection projection =
                org.mockito.Mockito.mock(UuidIdSummaryWithLastRunProjection.class);
        when(projection.getId()).thenReturn(UUID.randomUUID());
        when(projection.getName()).thenReturn("i1");
        when(projection.getIsEnabled()).thenReturn(true);
        when(projection.getDescription()).thenReturn("d");
        when(projection.getLastRunAt()).thenReturn(Instant.parse("2025-12-19T14:45:00Z"));
        when(arcGISIntegrationRepository.findNonDeletedSummariesByConnectionIdAndTenantId(id, "t"))
                .thenReturn(List.of(projection));

        assertThatThrownBy(() -> service.deleteConnection(id, "t", "u"))
                .isInstanceOf(IntegrationConnectionDeleteConflictException.class);
        verify(connectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteConnection should soft delete when no dependents")
    void deleteConnection_noDependents_softDeletesAndEvicts() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id)
                .tenantId("t")
                .serviceType(ServiceType.JIRA)
                .name("c")
                .isDeleted(false)
                .build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));
        when(jiraWebhookRepository.findNonDeletedSummariesByConnectionIdAndTenantId(id, "t"))
                .thenReturn(List.of());

        service.deleteConnection(id, "t", "u");

        ArgumentCaptor<IntegrationConnection> captor = ArgumentCaptor.forClass(IntegrationConnection.class);
        verify(connectionRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        verify(notificationEventPublisher).publish(any());
    }

    @Test
    @DisplayName("getDependents should return JIRA dependents")
    void getDependents_jira_buildsResponse() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id)
                .tenantId("t")
                .serviceType(ServiceType.JIRA)
                .name("c")
                .build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));
        StringIdSummaryWithLastRunProjection projection =
                org.mockito.Mockito.mock(StringIdSummaryWithLastRunProjection.class);
        when(projection.getId()).thenReturn("w1");
        when(projection.getName()).thenReturn("Webhook");
        when(projection.getIsEnabled()).thenReturn(true);
        when(projection.getDescription()).thenReturn("d");
        when(projection.getLastRunAt()).thenReturn(Instant.parse("2025-12-19T14:45:00Z"));
        when(jiraWebhookRepository.findNonDeletedSummariesByConnectionIdAndTenantId(id, "t"))
                .thenReturn(List.of(projection));

        var resp = service.getDependents(id, "t");
        assertThat(resp.getServiceType()).isEqualTo(ServiceType.JIRA);
        assertThat(resp.getIntegrations()).hasSize(1);
        assertThat(resp.getIntegrations().get(0).getLastRunAt())
                .isEqualTo(Instant.parse("2025-12-19T14:45:00Z"));
        assertThat(resp.hasAnyDependents()).isTrue();
    }

    @Test
    @DisplayName("rotateConnectionSecret should enrich request and publish notification")
    void rotateConnectionSecret_enrichesRequestAndPublishes() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id)
                .tenantId("t")
                .serviceType(ServiceType.JIRA)
                .secretName("secret")
                .name("c")
                .build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));

        IntegrationConnectionSecretRotateRequest request = new IntegrationConnectionSecretRotateRequest(
                null, null, "newSecret");

        service.rotateConnectionSecret(id, "t", "u", request);

        verify(iesConnectionClient).rotateConnectionSecret(eq(id), any(IntegrationConnectionSecretRotateRequest.class));
        verify(notificationEventPublisher).publish(any());
    }

    @Test
    @DisplayName("findByIdAndTenantIdAndIsDeletedFalse should throw when missing")
    void find_missing_throws() {
        UUID id = UUID.randomUUID();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .isInstanceOf(IntegrationNotFoundException.class)
                .hasMessageContaining("Integration connection not found");
    }

    @Test
    @DisplayName("getDependents should return ARCGIS dependents")
    void getDependents_arcgis_buildsResponse() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id).tenantId("t").serviceType(ServiceType.ARCGIS).name("c").build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));
        UuidIdSummaryWithLastRunProjection projection =
                org.mockito.Mockito.mock(UuidIdSummaryWithLastRunProjection.class);
        when(projection.getId()).thenReturn(UUID.randomUUID());
        when(projection.getName()).thenReturn("Integration A");
        when(projection.getIsEnabled()).thenReturn(true);
        when(projection.getDescription()).thenReturn("desc");
        when(projection.getLastRunAt()).thenReturn(Instant.parse("2025-12-19T14:45:00Z"));
        when(arcGISIntegrationRepository.findNonDeletedSummariesByConnectionIdAndTenantId(id, "t"))
                .thenReturn(List.of(projection));

        var resp = service.getDependents(id, "t");

        assertThat(resp.getServiceType()).isEqualTo(ServiceType.ARCGIS);
        assertThat(resp.getIntegrations()).hasSize(1);
        assertThat(resp.getIntegrations().get(0).getLastRunAt())
                .isEqualTo(Instant.parse("2025-12-19T14:45:00Z"));
        assertThat(resp.hasAnyDependents()).isTrue();
    }

    @Test
    @DisplayName("getDependents for unknown serviceType returns empty lists")
    void getDependents_unknownServiceType_returnsEmpty() {
        UUID id = UUID.randomUUID();
        // Use ARCGIS serviceType but mock to return empty so we fall through to default
        // Actually test the default branch by using null serviceType indirectly
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id).tenantId("t").serviceType(null).name("c").build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));

        var resp = service.getDependents(id, "t");

        assertThat(resp.getIntegrations()).isEmpty();
        assertThat(resp.hasAnyDependents()).isFalse();
    }

    @Test
    @DisplayName("getConnectionsByTenantAndServiceType returns mapped list")
    void getConnectionsByTenantAndServiceType_returnsMapped() {
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(UUID.randomUUID()).tenantId("t").serviceType(ServiceType.JIRA).name("c").build();
        IntegrationConnectionResponse mapped = IntegrationConnectionResponse.builder().id("x").name("c").build();
        when(connectionRepository.findAllConnectionsByTenantAndServiceTypeAndIsDeletedFalse("t", ServiceType.JIRA))
                .thenReturn(List.of(conn));
        when(connectionMapper.toResponse(conn)).thenReturn(mapped);

        List<IntegrationConnectionResponse> result = service.getConnectionsByTenantAndServiceType("t", ServiceType.JIRA);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo("x");
    }

    @Test
    @DisplayName("getConnectionById throws when not found")
    void getConnectionById_missing_throws() {
        UUID id = UUID.randomUUID();
        when(connectionRepository.findConnectionById(id, "t")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConnectionById(id, "t"))
                .isInstanceOf(IntegrationNotFoundException.class);
    }

    @Test
    @DisplayName("getConnectionById returns mapped when found")
    void getConnectionById_found_returnsMapped() {
        UUID id = UUID.randomUUID();
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(id).tenantId("t").serviceType(ServiceType.JIRA).name("c").build();
        IntegrationConnectionResponse mapped = IntegrationConnectionResponse.builder().id(id.toString()).name("c").build();
        when(connectionRepository.findConnectionById(id, "t")).thenReturn(Optional.of(conn));
        when(connectionMapper.toResponse(conn)).thenReturn(mapped);

        IntegrationConnectionResponse result = service.getConnectionById(id, "t");

        assertThat(result.getId()).isEqualTo(id.toString());
    }

    @Test
    @DisplayName("testAndCreateConnection returns null body response when IES body is null")
    void testAndCreateConnection_nullBody_returnsOk() {
        when(connectionRepository.findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(iesConnectionClient.testAndCreateConnection(any()))
                .thenReturn(ResponseEntity.ok(null));

        ResponseEntity<?> response = service.testAndCreateConnection(jiraBasicRequest(), "t1", "u1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
        verify(connectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getConnectionById throws IllegalArgumentException when connectionId is null")
    void getConnectionById_nullConnectionId_throws() {
        assertThatThrownBy(() -> service.getConnectionById(null, "t1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectionId and tenantId must be provided");
    }

    @Test
    @DisplayName("getConnectionById throws IllegalArgumentException when tenantId is blank")
    void getConnectionById_blankTenantId_throws() {
        assertThatThrownBy(() -> service.getConnectionById(UUID.randomUUID(), ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectionId and tenantId must be provided");
    }

    @Test
    @DisplayName("rotateConnectionSecret delegates to IES client and publishes notification")
    void rotateConnectionSecret_delegatesAndPublishes() {
        UUID id = UUID.randomUUID();
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(id).tenantId("t1").name("My Conn")
                .secretName("secret-key").serviceType(ServiceType.JIRA)
                .isDeleted(false).build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(conn));

        IntegrationConnectionSecretRotateRequest req =
                new IntegrationConnectionSecretRotateRequest(null, ServiceType.JIRA, "new-secret-value");

        service.rotateConnectionSecret(id, "t1", "u1", req);

        verify(iesConnectionClient).rotateConnectionSecret(eq(id), any());
        verify(notificationEventPublisher).publish(any());
    }

    @Test
    @DisplayName("evictConnectionNameCache does not throw")
    void evictConnectionNameCache_doesNotThrow() {
        service.evictConnectionNameCache(UUID.randomUUID().toString(), "t1");
        // No exception expected – just verifying the method runs
    }

    @Test
    @DisplayName("evictConnectionCache does not throw")
    void evictConnectionCache_doesNotThrow() {
        service.evictConnectionCache(UUID.randomUUID().toString(), "t1");
        // No exception expected – just verifying the method runs
    }

    @Test
    @DisplayName("getDependents with JIRA ServiceType returns jira webhooks list")
    void getDependents_jiraServiceType_returnsJiraWebhooks() {
        UUID id = UUID.randomUUID();
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(id).tenantId("t1").serviceType(ServiceType.JIRA).name("c")
                .isDeleted(false).build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(conn));

        com.integration.management.repository.projection.StringIdSummaryWithLastRunProjection proj =
                org.mockito.Mockito.mock(
                        com.integration.management.repository.projection.StringIdSummaryWithLastRunProjection.class);
        when(proj.getId()).thenReturn("w1");
        when(proj.getName()).thenReturn("Webhook 1");
        when(proj.getIsEnabled()).thenReturn(true);
        when(proj.getDescription()).thenReturn("desc");
        when(proj.getLastRunAt()).thenReturn(Instant.parse("2025-12-19T14:45:00Z"));

        when(jiraWebhookRepository.findNonDeletedSummariesByConnectionIdAndTenantId(id, "t1"))
                .thenReturn(List.of(proj));

        com.integration.management.model.dto.response.ConnectionDependentsResponse result =
                service.getDependents(id, "t1");

        assertThat(result.getIntegrations()).hasSize(1);
        assertThat(result.getServiceType()).isEqualTo(ServiceType.JIRA);
    }

    @Test
    @DisplayName("testExistingConnection updates connection when IES returns non-null response")
    void testExistingConnection_nonNullResponse_updatesConnection() {
        UUID id = UUID.randomUUID();
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(id).tenantId("t1").serviceType(ServiceType.JIRA)
                .secretName("sec").isDeleted(false).build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(conn));
        ConnectionTestResponse iesResp = new ConnectionTestResponse();
        iesResp.setConnectionStatus(
                com.integration.execution.contract.model.enums.ConnectionStatus.SUCCESS);
        when(iesConnectionClient.testExistingConnection(eq(id), any())).thenReturn(iesResp);

        ConnectionTestResponse result = service.testExistingConnection(id, "t1");

        assertThat(result).isSameAs(iesResp);
        verify(connectionRepository).save(conn);
    }

    @Test
    @DisplayName("testExistingConnection returns empty ConnectionTestResponse when IES returns null")
    void testExistingConnection_nullIesResponse_returnsEmpty() {
        UUID id = UUID.randomUUID();
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(id).tenantId("t1").serviceType(ServiceType.JIRA)
                .secretName("sec").isDeleted(false).build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t1"))
                .thenReturn(Optional.of(conn));
        when(iesConnectionClient.testExistingConnection(eq(id), any())).thenReturn(null);

        ConnectionTestResponse result = service.testExistingConnection(id, "t1");

        assertThat(result).isNotNull();
        verify(connectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getIntegrationConnectionNameById throws for invalid UUID")
    void getIntegrationConnectionNameById_invalidUuid_returnsNull() {
        assertThatThrownBy(() -> service.getIntegrationConnectionNameById("not-a-uuid", "t1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getDependents maps null lastRunAt for ArcGIS integrations")
    void getDependents_arcgis_nullLastRunAt() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id).tenantId("t").serviceType(ServiceType.ARCGIS).name("c").build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));
        UuidIdSummaryWithLastRunProjection projection =
                org.mockito.Mockito.mock(UuidIdSummaryWithLastRunProjection.class);
        when(projection.getId()).thenReturn(UUID.randomUUID());
        when(projection.getName()).thenReturn("Never run integration");
        when(projection.getIsEnabled()).thenReturn(true);
        when(projection.getDescription()).thenReturn("desc");
        when(projection.getLastRunAt()).thenReturn(null);
        when(arcGISIntegrationRepository.findNonDeletedSummariesByConnectionIdAndTenantId(id, "t"))
                .thenReturn(List.of(projection));

        var resp = service.getDependents(id, "t");

        assertThat(resp.getIntegrations()).hasSize(1);
        assertThat(resp.getIntegrations().get(0).getLastRunAt()).isNull();
        assertThat(resp.getIntegrations().get(0).getName()).isEqualTo("Never run integration");
    }

    @Test
    @DisplayName("getDependents maps null lastRunAt for Jira webhooks")
    void getDependents_jira_nullLastRunAt() {
        UUID id = UUID.randomUUID();
        IntegrationConnection connection = IntegrationConnection.builder()
                .id(id).tenantId("t").serviceType(ServiceType.JIRA).name("c").build();
        when(connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(id, "t"))
                .thenReturn(Optional.of(connection));
        StringIdSummaryWithLastRunProjection projection =
                org.mockito.Mockito.mock(StringIdSummaryWithLastRunProjection.class);
        when(projection.getId()).thenReturn("w1");
        when(projection.getName()).thenReturn("Never triggered webhook");
        when(projection.getIsEnabled()).thenReturn(false);
        when(projection.getDescription()).thenReturn("desc");
        when(projection.getLastRunAt()).thenReturn(null);
        when(jiraWebhookRepository.findNonDeletedSummariesByConnectionIdAndTenantId(id, "t"))
                .thenReturn(List.of(projection));

        var resp = service.getDependents(id, "t");

        assertThat(resp.getIntegrations()).hasSize(1);
        assertThat(resp.getIntegrations().get(0).getLastRunAt()).isNull();
        assertThat(resp.getIntegrations().get(0).getName()).isEqualTo("Never triggered webhook");
    }
}
