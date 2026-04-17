package com.integration.management.it;

import com.integration.execution.contract.model.enums.FetchMode;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.entity.IntegrationConnection;
import com.integration.management.repository.IntegrationConnectionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link IntegrationConnectionRepository} persistence layer.
 *
 * <p>Covers the full CRUD lifecycle against a real PostgreSQL container:
 * save → find → soft-delete → purge queries.
 */
@DisplayName("IntegrationConnection Repository — integration tests")
class IntegrationConnectionRepositoryIT extends AbstractImsIT {

    private static final String TENANT = "GLOBAL";
    private static final String USER = "system";

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    private final List<UUID> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdIds.clear();
    }

    @AfterEach
    void tearDown() {
        if (createdIds.isEmpty()) {
            return;
        }
        connectionRepository.deleteAllById(createdIds);
        createdIds.clear();
    }

    private IntegrationConnection saveConnection(final String suffix, final ServiceType serviceType) {
        IntegrationConnection connection = connectionRepository.save(
                IntegrationConnection.builder()
                        .id(UUID.randomUUID())
                        .name("IT Connection " + suffix)
                        .secretName("it-secret-" + suffix + "-" + UUID.randomUUID())
                        .serviceType(serviceType)
                        .fetchMode(FetchMode.GET)
                        .connectionHashKey("hash-" + UUID.randomUUID())
                        .tenantId(TENANT)
                        .createdBy(USER)
                        .lastModifiedBy(USER)
                        .build());
        createdIds.add(connection.getId());
        return connection;
    }

    @Test
    @DisplayName("save and findByIdAndTenantIdAndIsDeletedFalse returns persisted connection")
    void saveAndFind_returnsPersisted() {
        IntegrationConnection saved = saveConnection("find-test", ServiceType.ARCGIS);

        Optional<IntegrationConnection> found =
                connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(saved.getId(), TENANT);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(saved.getName());
        assertThat(found.get().getServiceType()).isEqualTo(ServiceType.ARCGIS);
        assertThat(found.get().getTenantId()).isEqualTo(TENANT);
        assertThat(found.get().getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("findByIdAndTenantIdAndIsDeletedFalse returns empty after soft-delete")
    void softDelete_hiddenFromActiveQuery() {
        IntegrationConnection connection = saveConnection("soft-delete", ServiceType.JIRA);

        connection.setIsDeleted(true);
        connectionRepository.save(connection);

        Optional<IntegrationConnection> result =
                connectionRepository.findByIdAndTenantIdAndIsDeletedFalse(connection.getId(), TENANT);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllConnectionsByTenantAndServiceTypeAndIsDeletedFalse returns only active connections")
    void findAllByTenantAndType_returnsOnlyActive() {
        IntegrationConnection active1 = saveConnection("active-1", ServiceType.ARCGIS);
        IntegrationConnection active2 = saveConnection("active-2", ServiceType.ARCGIS);
        IntegrationConnection deleted = saveConnection("deleted", ServiceType.ARCGIS);

        deleted.setIsDeleted(true);
        connectionRepository.save(deleted);

        List<IntegrationConnection> results =
                connectionRepository.findAllConnectionsByTenantAndServiceTypeAndIsDeletedFalse(
                        TENANT, ServiceType.ARCGIS);

        List<UUID> resultIds = results.stream().map(IntegrationConnection::getId).toList();
        assertThat(resultIds).contains(active1.getId(), active2.getId());
        assertThat(resultIds).doesNotContain(deleted.getId());
    }

    @Test
    @DisplayName("findSecretNameByIdAndTenantId returns the stored secret name")
    void findSecretName_returnsStoredValue() {
        IntegrationConnection connection = saveConnection("secret-query", ServiceType.JIRA);

        Optional<String> secretName =
                connectionRepository.findSecretNameByIdAndTenantId(connection.getId(), TENANT);

        assertThat(secretName).isPresent();
        assertThat(secretName.get()).isEqualTo(connection.getSecretName());
    }

    @Test
    @DisplayName("findByTenantIdAndConnectionHashKeyAndIsDeletedFalse detects duplicate hash")
    void findByHashKey_detectsDuplicate() {
        IntegrationConnection connection = saveConnection("hash-query", ServiceType.ARCGIS);

        Optional<IntegrationConnection> found =
                connectionRepository.findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(
                        TENANT, connection.getConnectionHashKey());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(connection.getId());
    }

    @Test
    @DisplayName("findByTenantIdAndConnectionHashKeyAndIsDeletedFalse returns empty for soft-deleted duplicate")
    void findByHashKey_emptyForSoftDeleted() {
        IntegrationConnection connection = saveConnection("hash-deleted", ServiceType.ARCGIS);
        connection.setIsDeleted(true);
        connectionRepository.save(connection);

        Optional<IntegrationConnection> found =
                connectionRepository.findByTenantIdAndConnectionHashKeyAndIsDeletedFalse(
                        TENANT, connection.getConnectionHashKey());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findConnectionById returns connection for valid tenant and id")
    void findConnectionById_returnsForValidTenant() {
        IntegrationConnection connection = saveConnection("by-id", ServiceType.ARCGIS);

        Optional<IntegrationConnection> found =
                connectionRepository.findConnectionById(connection.getId(), TENANT);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(connection.getId());
    }

    @Test
    @DisplayName("findConnectionById returns empty for different tenant")
    void findConnectionById_emptyForWrongTenant() {
        IntegrationConnection connection = saveConnection("wrong-tenant", ServiceType.ARCGIS);

        Optional<IntegrationConnection> found =
                connectionRepository.findConnectionById(connection.getId(), "OTHER-TENANT");

        assertThat(found).isEmpty();
    }
}

