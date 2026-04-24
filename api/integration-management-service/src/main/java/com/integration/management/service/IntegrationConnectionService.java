package com.integration.management.service;

import com.integration.execution.contract.message.NotificationEvent;
import com.integration.execution.contract.model.enums.NotificationEventKey;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.request.ConnectionReTestRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionRequest;
import com.integration.execution.contract.rest.request.IntegrationConnectionSecretRotateRequest;
import com.integration.execution.contract.rest.response.ConnectionTestResponse;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.entity.IntegrationConnection;
import com.integration.management.exception.IntegrationConnectionDeleteConflictException;
import com.integration.management.exception.IntegrationApiException;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.ies.client.IesConnectionClient;
import com.integration.management.mapper.IntegrationConnectionMapper;
import com.integration.management.model.dto.response.ConnectionDependentItemResponse;
import com.integration.management.model.dto.response.ConnectionDependentsResponse;
import com.integration.management.notification.messaging.NotificationEventPublisher;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.repository.ConfluenceIntegrationRepository;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.JiraWebhookRepository;
import com.integration.management.util.ConnectionHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConnectionService {

    private final IntegrationConnectionRepository connectionRepository;
    private final ArcGISIntegrationRepository arcGISIntegrationRepository;
    private final JiraWebhookRepository jiraWebhookRepository;
    private final ConfluenceIntegrationRepository confluenceIntegrationRepository;
    private final IntegrationConnectionMapper connectionMapper;
    private final IesConnectionClient iesConnectionClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public ResponseEntity<?> testAndCreateConnection(
            IntegrationConnectionRequest request, String tenantId, String userId) {

        String connectionHashKey = ConnectionHashUtil.compute(
                tenantId, request.serviceType(), request.integrationSecret());

        Optional<IntegrationConnection> existing = connectionRepository
                .findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(tenantId, connectionHashKey);
        if (existing.isPresent()) {
            String message = String.format(
                    "A connection named <strong>%s</strong> already exists for this service. "
                            + "Please delete it before creating a new one.",
                    existing.get().getName());
            throw new IntegrationNameAlreadyExistsException(message);
        }

        ResponseEntity<ConnectionTestResponse> iesResponse = iesConnectionClient
                .testAndCreateConnection(request);
        ConnectionTestResponse testResponse = iesResponse.getBody();

        if (testResponse == null || !testResponse.isSuccess()) {
            return ResponseEntity.ok(testResponse);
        }

        IntegrationConnection connection = buildNewConnection(
                request, tenantId, userId, connectionHashKey, testResponse);
        connectionRepository.save(connection);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .eventKey(NotificationEventKey.INTEGRATION_CONNECTION_CREATED.name())
                .tenantId(tenantId)
                .triggeredByUserId(userId)
                .metadata(Map.of(
                        "connectionName", connection.getName() != null ? connection.getName() : "",
                        "connectionId", connection.getId().toString(),
                        "serviceType", connection.getServiceType().getDisplayName(),
                        "createdBy", userId))
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(connectionMapper.toResponse(connection));
    }

    public List<IntegrationConnectionResponse> getConnectionsByTenantAndServiceType(
            String tenantId, ServiceType serviceType) {
        log.debug("Fetching connections for tenant={}, serviceType={}", tenantId, serviceType);
        return connectionRepository
                .findAllConnectionsByTenantAndServiceTypeAndIsDeletedFalse(tenantId, serviceType)
                .stream()
                .map(connectionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConnectionTestResponse testExistingConnection(UUID connectionId, String tenantId) {
        log.info("Delegating connection test to execution-service for connectionId={} tenantId={}",
                connectionId, tenantId);
        IntegrationConnection connection = findByIdAndTenantIdAndIsDeletedFalse(connectionId, tenantId);
        ConnectionReTestRequest reTestRequest = new ConnectionReTestRequest(
                connection.getSecretName(), connection.getServiceType());
        ConnectionTestResponse response = iesConnectionClient
                .testExistingConnection(connectionId, reTestRequest);
        if (response != null) {
            connection.setLastConnectionStatus(response.getConnectionStatus());
            connection.setLastConnectionTest(response.getLastConnectionTest());
            connection.setLastConnectionMessage(response.getMessage());
            connectionRepository.save(connection);
        }
        return response != null ? response : new ConnectionTestResponse();
    }

    public IntegrationConnection findByIdAndTenantIdAndIsDeletedFalse(UUID connectionId, String tenantId) {
        return connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(connectionId, tenantId)
                .orElseThrow(() -> new IntegrationNotFoundException(
                        "Integration connection not found for id=" + connectionId + " tenant=" + tenantId));
    }

    private IntegrationConnection buildNewConnection(
            IntegrationConnectionRequest request,
            String tenantId,
            String userId,
            String connectionHashKey,
            ConnectionTestResponse testResponse) {
        return IntegrationConnection.builder()
                .tenantId(tenantId)
                .name(request.name())
                .secretName(testResponse.getSecretName())
                .serviceType(request.serviceType())
                .connectionHashKey(connectionHashKey)
                .lastConnectionStatus(testResponse.getConnectionStatus())
                .lastConnectionTest(testResponse.getLastConnectionTest())
                .lastConnectionMessage(testResponse.getMessage())
                .createdBy(userId)
                .lastModifiedBy(userId)
                .build();
    }

    @Cacheable(value = "integrationConnectionNamesCache", key = "#id + '_' + #tenantId")
    public String getIntegrationConnectionNameById(String id, String tenantId) {
        log.info("Fetching Integration connection name for id: {}", id);
        try {
            UUID connectionId = UUID.fromString(id);
            return connectionRepository.findSecretNameByIdAndTenantId(connectionId, tenantId).orElseThrow(
                    () -> new IntegrationNotFoundException("Integration connection not found for id=" + id));
        } catch (Exception e) {
            log.error("Error fetching Integration connection name for id: {}", id, e);
            return null;
        }
    }

    @Transactional
    public void deleteConnection(UUID connectionId, String tenantId, String userId) {
        log.info("Soft deleting connection {} for tenant: {} by user: {}", connectionId, tenantId, userId);
        IntegrationConnection connection = findByIdAndTenantIdAndIsDeletedFalse(connectionId, tenantId);

        ConnectionDependentsResponse dependents = buildDependentsResponse(connection, connectionId, tenantId);
        if (dependents.hasAnyDependents()) {
            throw new IntegrationConnectionDeleteConflictException(
                    "Connection cannot be deleted because it has existing dependents",
                    dependents);
        }

        notificationEventPublisher.publish(NotificationEvent.builder()
                .eventKey(NotificationEventKey.INTEGRATION_CONNECTION_DELETED.name())
                .tenantId(tenantId)
                .triggeredByUserId(userId)
                .metadata(Map.of(
                        "connectionName", connection.getName() != null ? connection.getName() : "",
                        "connectionId", connectionId.toString(),
                        "serviceType", connection.getServiceType().getDisplayName(),
                        "deletedBy", userId))
                .build());

        connection.setIsDeleted(true);
        connection.setLastModifiedBy(userId);

        connectionRepository.save(connection);
        log.info("Successfully soft deleted connection {} for tenant: {}", connectionId, tenantId);

        evictConnectionNameCache(connectionId.toString(), tenantId);
        evictConnectionCache(connectionId.toString(), tenantId);
    }

    public ConnectionDependentsResponse getDependents(UUID connectionId, String tenantId) {
        IntegrationConnection connection = findByIdAndTenantIdAndIsDeletedFalse(connectionId, tenantId);
        return buildDependentsResponse(connection, connectionId, tenantId);
    }

    private ConnectionDependentsResponse buildDependentsResponse(
            IntegrationConnection connection, UUID connectionId, String tenantId) {
        ServiceType serviceType = connection.getServiceType();
        List<ConnectionDependentItemResponse> integrations = new ArrayList<>();

        if (serviceType == ServiceType.ARCGIS) {
            integrations = arcGISIntegrationRepository
                    .findNonDeletedSummariesByConnectionIdAndTenantId(connectionId, tenantId)
                    .stream()
                    .map(p -> ConnectionDependentItemResponse.builder()
                            .id(p.getId().toString())
                            .name(p.getName())
                            .isEnabled(p.getIsEnabled())
                            .description(p.getDescription())
                            .lastRunAt(p.getLastRunAt())
                            .build())
                    .collect(Collectors.toList());
        } else if (serviceType == ServiceType.JIRA) {
            integrations = jiraWebhookRepository
                    .findNonDeletedSummariesByConnectionIdAndTenantId(connectionId, tenantId)
                    .stream()
                    .map(p -> ConnectionDependentItemResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .isEnabled(p.getIsEnabled())
                            .description(p.getDescription())
                            .lastRunAt(p.getLastRunAt())
                            .build())
                    .collect(Collectors.toList());
        } else if (serviceType == ServiceType.CONFLUENCE) {
            integrations = confluenceIntegrationRepository
                    .findNonDeletedSummariesByConnectionIdAndTenantId(connectionId, tenantId)
                    .stream()
                    .map(p -> ConnectionDependentItemResponse.builder()
                            .id(p.getId().toString())
                            .name(p.getName())
                            .isEnabled(p.getIsEnabled())
                            .description(p.getDescription())
                            .lastRunAt(p.getLastRunAt())
                            .build())
                    .collect(Collectors.toList());
        }

        return ConnectionDependentsResponse.builder()
                .serviceType(serviceType)
                .integrations(integrations)
                .build();
    }

    @CacheEvict(value = "integrationConnectionNamesCache", key = "#connectionId + '_' + #tenantId")
    public void evictConnectionNameCache(String connectionId, String tenantId) {
        log.debug("Evicted cache for connection name with id: {}", connectionId);
    }

    @CacheEvict(value = "integrationConnectionCache", key = "#connectionId + '_' + #tenantId")
    public void evictConnectionCache(String connectionId, String tenantId) {
        log.debug("Evicted cache for connection with id: {}", connectionId);
    }

    @Cacheable(value = "integrationConnectionCache", key = "#connectionId + '_' + #tenantId")
    public IntegrationConnectionResponse getConnectionById(UUID connectionId, String tenantId) {
        if (connectionId == null || !StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("connectionId and tenantId must be provided");
        }
        return connectionRepository
                .findConnectionById(connectionId, tenantId)
                .stream()
                .map(connectionMapper::toResponse)
                .findFirst()
                .orElseThrow(() -> new IntegrationNotFoundException(
                        "Jira connection not found for id=" + connectionId + " tenant=" + tenantId));
    }

    @Transactional(noRollbackFor = IntegrationApiException.class)
    public void rotateConnectionSecret(
            UUID connectionId,
            String tenantId,
            String userId,
            IntegrationConnectionSecretRotateRequest request) {
        IntegrationConnection connection = findByIdAndTenantIdAndIsDeletedFalse(connectionId, tenantId);
        IntegrationConnectionSecretRotateRequest enrichedRequest = new IntegrationConnectionSecretRotateRequest(
                connection.getSecretName(), connection.getServiceType(), request.newSecret());
        iesConnectionClient.rotateConnectionSecret(connectionId, enrichedRequest);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .eventKey(NotificationEventKey.INTEGRATION_CONNECTION_SECRET_UPDATED.name())
                .tenantId(tenantId)
                .triggeredByUserId(userId)
                .metadata(java.util.Map.of(
                        "connectionName", connection.getName() != null ? connection.getName() : "",
                        "connectionId", connectionId.toString(),
                        "serviceType", connection.getServiceType().getDisplayName(),
                        "updatedBy", userId))
                .build());
    }
}
