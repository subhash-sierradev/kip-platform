package com.integration.management.it;

import com.integration.execution.contract.model.enums.FetchMode;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.entity.IntegrationConnection;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.repository.ArcGISIntegrationRepository;
import com.integration.management.repository.IntegrationConnectionRepository;
import com.integration.management.repository.IntegrationScheduleRepository;
import com.integration.management.service.ArcGISScheduleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ArcGIS scheduling — verifies that enabled integrations
 * with active schedules are discoverable by the schedule service and that
 * health-stats correctly reflect in-database state.
 *
 * <p>Uses the pre-seeded GLOBAL tenant and system user (V2 migration) to satisfy FK constraints.
 */
@DisplayName("ArcGIS Schedule Service — integration tests")
class ArcGISScheduleServiceIT extends AbstractImsIT {

    // Pre-seeded by V2 migration — satisfies FK constraints
    private static final String TENANT = "GLOBAL";
    private static final String USER = "system";

    @Autowired
    private ArcGISScheduleService arcGISScheduleService;

    @Autowired
    private ArcGISIntegrationRepository arcGISIntegrationRepository;

    @Autowired
    private IntegrationConnectionRepository integrationConnectionRepository;

    @Autowired
    private IntegrationScheduleRepository integrationScheduleRepository;

    private final List<UUID> createdConnectionIds = new ArrayList<>();
    private final List<UUID> createdIntegrationIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdConnectionIds.clear();
        createdIntegrationIds.clear();
    }

    @AfterEach
    void tearDown() {
        arcGISIntegrationRepository.findAllById(createdIntegrationIds)
                .forEach(i -> {
                    i.setIsDeleted(true);
                    arcGISIntegrationRepository.save(i);
                });
        integrationConnectionRepository.findAllById(createdConnectionIds)
                .forEach(c -> {
                    c.setIsDeleted(true);
                    integrationConnectionRepository.save(c);
                });
    }

    private IntegrationConnection saveConnection(String suffix) {
        IntegrationConnection connection = integrationConnectionRepository.save(
                IntegrationConnection.builder()
                        .id(UUID.randomUUID())
                        .name("ArcGIS IT Connection " + suffix)
                        .secretName("arcgis-it-secret-" + suffix + "-" + UUID.randomUUID())
                        .serviceType(ServiceType.ARCGIS)
                        .fetchMode(FetchMode.GET)
                        .connectionHashKey("hash-" + UUID.randomUUID())
                        .tenantId(TENANT)
                        .createdBy(USER)
                        .lastModifiedBy(USER)
                        .build());
        createdConnectionIds.add(connection.getId());
        return connection;
    }

    private IntegrationSchedule saveSchedule(int hour) {
        return integrationScheduleRepository.save(
                IntegrationSchedule.builder()
                        .executionTime(LocalTime.of(hour, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .dailyExecutionInterval(24)
                        .timeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL)
                        .businessTimeZone("UTC")
                        .cronExpression("0 0 " + hour + " * * ?")
                        .isExecuteOnMonthEnd(false)
                        .build());
    }

    private ArcGISIntegration saveIntegration(String name, IntegrationConnection conn,
            IntegrationSchedule schedule, boolean enabled) {
        ArcGISIntegration integration = arcGISIntegrationRepository.save(
                ArcGISIntegration.builder()
                        .id(UUID.randomUUID())
                        .name(name)
                        .normalizedName(name.toLowerCase().replace(" ", "-"))
                        .itemType("Feature Layer")
                        .itemSubtype("subtype")
                        .connectionId(conn.getId())
                        .schedule(schedule)
                        .isEnabled(enabled)
                        .tenantId(TENANT)
                        .createdBy(USER)
                        .lastModifiedBy(USER)
                        .build());
        createdIntegrationIds.add(integration.getId());
        return integration;
    }

    @Test
    @DisplayName("findAllIntegrationsWithActiveSchedules returns enabled integrations only")
    void findAllIntegrationsWithActiveSchedules_returnsOnlyEnabledIntegrations() {
        IntegrationConnection conn = saveConnection("enabled-test");
        IntegrationSchedule enabledSchedule = saveSchedule(8);
        IntegrationSchedule disabledSchedule = saveSchedule(9);

        ArcGISIntegration enabled = saveIntegration("Enabled Integration IT", conn, enabledSchedule, true);
        saveIntegration("Disabled Integration IT", conn, disabledSchedule, false);

        List<ArcGISIntegration> activeIntegrations =
                arcGISIntegrationRepository.findAllIntegrationsWithActiveSchedules();

        assertThat(activeIntegrations).isNotEmpty();
        assertThat(activeIntegrations).allMatch(ArcGISIntegration::getIsEnabled);

        List<UUID> activeIds = activeIntegrations.stream().map(ArcGISIntegration::getId).toList();
        assertThat(activeIds).contains(enabled.getId());
    }

    @Test
    @DisplayName("getJobSchedulingHealthStats returns non-negative counts")
    void getJobSchedulingHealthStats_returnsNonNegativeCounts() {
        int[] stats = arcGISScheduleService.getJobSchedulingHealthStats();

        assertThat(stats).hasSize(2);
        assertThat(stats[0]).isGreaterThanOrEqualTo(0);
        assertThat(stats[1]).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("findByIdAndTenantIdAndIsDeletedFalseWithSchedule fetches schedule eagerly")
    void findByIdAndTenantId_withSchedule_fetchesScheduleEagerly() {
        IntegrationConnection conn = saveConnection("schedule-fetch");
        IntegrationSchedule schedule = saveSchedule(10);
        ArcGISIntegration integration = saveIntegration("Schedule Fetch Test IT", conn, schedule, true);

        var found = arcGISIntegrationRepository
                .findByIdAndTenantIdAndIsDeletedFalseWithSchedule(integration.getId(), TENANT);

        assertThat(found).isPresent();
        assertThat(found.get().getSchedule()).isNotNull();
        assertThat(found.get().getSchedule().getCronExpression()).isEqualTo("0 0 10 * * ?");
    }

    @Test
    @DisplayName("soft-deleted integration is not returned by active query")
    void softDeletedIntegration_notReturnedByActiveQuery() {
        IntegrationConnection conn = saveConnection("soft-delete");
        IntegrationSchedule schedule = saveSchedule(11);
        ArcGISIntegration integration = saveIntegration("To Be Deleted IT", conn, schedule, true);

        // soft-delete
        integration.setIsDeleted(true);
        arcGISIntegrationRepository.save(integration);

        var result = arcGISIntegrationRepository
                .findByIdAndTenantIdAndIsDeletedFalse(integration.getId(), TENANT);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllNormalizedNamesByTenantId returns names for active integrations")
    void findAllNormalizedNamesByTenantId_returnsNamesForActiveIntegrations() {
        IntegrationConnection conn = saveConnection("names-test");
        IntegrationSchedule schedule = saveSchedule(12);
        ArcGISIntegration integration = saveIntegration("Names Test Integration IT", conn, schedule, true);

        List<String> names = arcGISIntegrationRepository.findAllNormalizedNamesByTenantId(TENANT);

        assertThat(names).contains(integration.getNormalizedName());
    }
}

