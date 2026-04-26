package com.integration.management.service;

import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.model.ExecutionWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.integration.management.constants.IntegrationManagementConstants.CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;
import static com.integration.management.constants.IntegrationManagementConstants.CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START;
import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_ARCGIS;
import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_CONFLUENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionWindowResolverService")
class ExecutionWindowResolverServiceTest {

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private ExecutionWindowResolverService service;

    private IntegrationSchedule schedule;
    private String tenantId;
    private Instant triggerTime;

    @BeforeEach
    void setUp() {
        schedule = new IntegrationSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setBusinessTimeZone("America/New_York");
        tenantId = "tenant1";
        triggerTime = Instant.parse("2024-03-15T14:30:00Z");
    }

    // -------------------------------------------------------------------------
    // FLEXIBLE_INTERVAL mode tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resolve - FLEXIBLE_INTERVAL uses processedUntil to triggerTime")
    void resolve_flexibleInterval_usesProcessedUntilToTriggerTime() {
        // Arrange
        Instant processedUntil = Instant.parse("2024-03-14T10:00:00Z");
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);
        schedule.setProcessedUntil(processedUntil);

        // Act
        ExecutionWindow result = service.resolve(schedule, triggerTime, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        assertThat(result.windowStart()).isEqualTo(processedUntil);
        assertThat(result.windowEnd()).isEqualTo(triggerTime);
        verifyNoInteractions(settingsService);
    }

    @Test
    @DisplayName("resolve - FLEXIBLE_INTERVAL with null processedUntil uses fallback for ArcGIS")
    void resolve_flexibleIntervalNullProcessedUntil_usesFallbackArcGIS() {
        // Arrange
        Instant fallbackStart = Instant.parse("2024-01-01T00:00:00Z");
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);
        schedule.setProcessedUntil(null);

        when(settingsService.getConfigValueAsTimestamp(CONFIG_KEY_ARCGIS_INITIAL_SYNC_START, tenantId))
                .thenReturn(Optional.of(fallbackStart));

        // Act
        ExecutionWindow result = service.resolve(schedule, triggerTime, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        assertThat(result.windowStart()).isEqualTo(fallbackStart);
        assertThat(result.windowEnd()).isEqualTo(triggerTime);
        verify(settingsService).getConfigValueAsTimestamp(CONFIG_KEY_ARCGIS_INITIAL_SYNC_START, tenantId);
    }

    @Test
    @DisplayName("resolve - FLEXIBLE_INTERVAL with null processedUntil uses fallback for Confluence")
    void resolve_flexibleIntervalNullProcessedUntil_usesFallbackConfluence() {
        // Arrange
        Instant fallbackStart = Instant.parse("2024-02-01T00:00:00Z");
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);
        schedule.setProcessedUntil(null);

        when(settingsService.getConfigValueAsTimestamp(CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START, tenantId))
                .thenReturn(Optional.of(fallbackStart));

        // Act
        ExecutionWindow result = service.resolve(schedule, triggerTime, tenantId, INTEGRATION_TYPE_CONFLUENCE);

        // Assert
        assertThat(result.windowStart()).isEqualTo(fallbackStart);
        assertThat(result.windowEnd()).isEqualTo(triggerTime);
        verify(settingsService).getConfigValueAsTimestamp(CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START, tenantId);
    }

    @Test
    @DisplayName("resolve - FLEXIBLE_INTERVAL with null processedUntil defaults to EPOCH when fallback not found")
    void resolve_flexibleIntervalNullProcessedUntil_defaultsToEpoch() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);
        schedule.setProcessedUntil(null);

        when(settingsService.getConfigValueAsTimestamp(CONFIG_KEY_ARCGIS_INITIAL_SYNC_START, tenantId))
                .thenReturn(Optional.empty());

        // Act
        ExecutionWindow result = service.resolve(schedule, triggerTime, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        assertThat(result.windowStart()).isEqualTo(Instant.EPOCH);
        assertThat(result.windowEnd()).isEqualTo(triggerTime);
    }

    // -------------------------------------------------------------------------
    // FIXED_DAY_BOUNDARY mode tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resolve - FIXED_DAY_BOUNDARY aligns to day boundaries in business timezone")
    void resolve_fixedDayBoundary_alignsToDayBoundaries() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("America/New_York");

        // processedUntil: 2024-03-14 10:00 UTC (06:00 EDT)
        Instant processedUntil = Instant.parse("2024-03-14T10:00:00Z");
        schedule.setProcessedUntil(processedUntil);

        // triggerTime: 2024-03-15 14:30 UTC (10:30 EDT)
        Instant trigger = Instant.parse("2024-03-15T14:30:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        // windowStart should be 2024-03-14 00:00 EDT (04:00 UTC)
        Instant expectedStart = ZonedDateTime.of(2024, 3, 14, 0, 0, 0, 0,
                ZoneId.of("America/New_York")).toInstant();

        // windowEnd should be 2024-03-15 00:00 EDT (04:00 UTC) — exclusive midnight
        Instant expectedEnd = ZonedDateTime.of(2024, 3, 15, 0, 0, 0, 0,
                ZoneId.of("America/New_York")).toInstant();

        assertThat(result.windowStart()).isEqualTo(expectedStart);
        assertThat(result.windowEnd()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("resolve - FIXED_DAY_BOUNDARY with multiple days processes full days")
    void resolve_fixedDayBoundary_multipleDays_processesFullDays() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("UTC");

        // processedUntil: 2024-03-10 15:30 UTC
        Instant processedUntil = Instant.parse("2024-03-10T15:30:00Z");
        schedule.setProcessedUntil(processedUntil);

        // triggerTime: 2024-03-13 08:45 UTC
        Instant trigger = Instant.parse("2024-03-13T08:45:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        // windowStart: 2024-03-10 00:00 UTC
        Instant expectedStart = Instant.parse("2024-03-10T00:00:00Z");

        // windowEnd: 2024-03-13 00:00 UTC — exclusive midnight
        Instant expectedEnd = Instant.parse("2024-03-13T00:00:00Z");

        assertThat(result.windowStart()).isEqualTo(expectedStart);
        assertThat(result.windowEnd()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("resolve - FIXED_DAY_BOUNDARY same day trigger throws IllegalStateException")
    void resolve_fixedDayBoundarySameDay_throwsIllegalState() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("America/New_York");

        // Both on same day: 2024-03-15
        Instant processedUntil = Instant.parse("2024-03-15T10:00:00Z");  // 06:00 EDT
        schedule.setProcessedUntil(processedUntil);

        Instant trigger = Instant.parse("2024-03-15T14:30:00Z");  // 10:30 EDT (same day)

        // Act & Assert
        assertThatThrownBy(() -> service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_ARCGIS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid execution window: start is after end");
    }

    @Test
    @DisplayName("resolve - FIXED_DAY_BOUNDARY with Pacific timezone handles DST correctly")
    void resolve_fixedDayBoundary_pacificTimezone_handlesDST() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("America/Los_Angeles");

        // 2024-03-10 is the DST transition day (spring forward on Mar 10, 2024)
        Instant processedUntil = Instant.parse("2024-03-09T10:00:00Z");
        schedule.setProcessedUntil(processedUntil);

        Instant trigger = Instant.parse("2024-03-11T10:00:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        // Should align to midnight Pacific time on respective days
        Instant expectedStart = ZonedDateTime.of(2024, 3, 9, 0, 0, 0, 0,
                ZoneId.of("America/Los_Angeles")).toInstant();

        Instant expectedEnd = ZonedDateTime.of(2024, 3, 11, 0, 0, 0, 0,
                ZoneId.of("America/Los_Angeles")).toInstant();

        assertThat(result.windowStart()).isEqualTo(expectedStart);
        assertThat(result.windowEnd()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("resolve - FIXED_DAY_BOUNDARY with Europe/London timezone works correctly")
    void resolve_fixedDayBoundary_londonTimezone_worksCorrectly() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("Europe/London");

        Instant processedUntil = Instant.parse("2024-03-14T12:30:00Z");
        schedule.setProcessedUntil(processedUntil);

        Instant trigger = Instant.parse("2024-03-15T09:15:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_CONFLUENCE);

        // Assert
        Instant expectedStart = ZonedDateTime.of(2024, 3, 14, 0, 0, 0, 0,
                ZoneId.of("Europe/London")).toInstant();

        Instant expectedEnd = ZonedDateTime.of(2024, 3, 15, 0, 0, 0, 0,
                ZoneId.of("Europe/London")).toInstant();

        assertThat(result.windowStart()).isEqualTo(expectedStart);
        assertThat(result.windowEnd()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("resolve - FIXED_DAY_BOUNDARY with null processedUntil uses fallback and aligns")
    void resolve_fixedDayBoundaryNullProcessedUntil_usesFallbackAndAligns() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("UTC");
        schedule.setProcessedUntil(null);

        Instant fallbackStart = Instant.parse("2024-03-10T15:30:00Z");
        when(settingsService.getConfigValueAsTimestamp(CONFIG_KEY_ARCGIS_INITIAL_SYNC_START, tenantId))
                .thenReturn(Optional.of(fallbackStart));

        Instant trigger = Instant.parse("2024-03-12T10:00:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        // Fallback 2024-03-10 15:30 should align to 2024-03-10 00:00 UTC
        Instant expectedStart = Instant.parse("2024-03-10T00:00:00Z");

        // Trigger 2024-03-12 10:00 should align to 2024-03-12 00:00 UTC — exclusive midnight
        Instant expectedEnd = Instant.parse("2024-03-12T00:00:00Z");

        assertThat(result.windowStart()).isEqualTo(expectedStart);
        assertThat(result.windowEnd()).isEqualTo(expectedEnd);
    }

    // -------------------------------------------------------------------------
    // Unknown integration type tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resolve - unknown integration type throws IllegalArgumentException")
    void resolve_unknownIntegrationType_throwsIllegalArgument() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);
        schedule.setProcessedUntil(null);

        // Act & Assert
        assertThatThrownBy(() -> service.resolve(schedule, triggerTime, tenantId, "UNKNOWN_TYPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown integration type");
    }

    @Test
    @DisplayName("resolve - integration type is case insensitive")
    void resolve_integrationTypeCaseInsensitive_works() {
        // Arrange
        Instant fallbackStart = Instant.parse("2024-01-01T00:00:00Z");
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);
        schedule.setProcessedUntil(null);

        when(settingsService.getConfigValueAsTimestamp(CONFIG_KEY_ARCGIS_INITIAL_SYNC_START, tenantId))
                .thenReturn(Optional.of(fallbackStart));

        // Act - using lowercase
        ExecutionWindow result = service.resolve(schedule, triggerTime, tenantId, "arcgis");

        // Assert
        assertThat(result.windowStart()).isEqualTo(fallbackStart);
        verify(settingsService).getConfigValueAsTimestamp(CONFIG_KEY_ARCGIS_INITIAL_SYNC_START, tenantId);
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resolve - processedUntil at exact midnight with FIXED_DAY_BOUNDARY")
    void resolve_processedUntilAtMidnight_fixedDayBoundary() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("UTC");

        // processedUntil already at midnight
        Instant processedUntil = Instant.parse("2024-03-14T00:00:00Z");
        schedule.setProcessedUntil(processedUntil);

        Instant trigger = Instant.parse("2024-03-15T12:00:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert
        // Should stay at midnight
        Instant expectedStart = Instant.parse("2024-03-14T00:00:00Z");
        Instant expectedEnd = Instant.parse("2024-03-15T00:00:00Z");

        assertThat(result.windowStart()).isEqualTo(expectedStart);
        assertThat(result.windowEnd()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("resolve - triggerTime at exact midnight with FIXED_DAY_BOUNDARY")
    void resolve_triggerTimeAtMidnight_fixedDayBoundary() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY);
        schedule.setBusinessTimeZone("UTC");

        Instant processedUntil = Instant.parse("2024-03-14T10:00:00Z");
        schedule.setProcessedUntil(processedUntil);

        // triggerTime at exactly midnight
        Instant trigger = Instant.parse("2024-03-16T00:00:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_CONFLUENCE);

        // Assert
        Instant expectedStart = Instant.parse("2024-03-14T00:00:00Z");
        Instant expectedEnd = Instant.parse("2024-03-16T00:00:00Z");

        assertThat(result.windowStart()).isEqualTo(expectedStart);
        assertThat(result.windowEnd()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("resolve - FLEXIBLE_INTERVAL with processedUntil after triggerTime still uses both")
    void resolve_flexibleIntervalProcessedAfterTrigger_usesBoth() {
        // Arrange
        schedule.setTimeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL);

        // This is an unusual case but testing the behavior
        Instant processedUntil = Instant.parse("2024-03-16T10:00:00Z");
        schedule.setProcessedUntil(processedUntil);

        Instant trigger = Instant.parse("2024-03-15T10:00:00Z");

        // Act
        ExecutionWindow result = service.resolve(schedule, trigger, tenantId, INTEGRATION_TYPE_ARCGIS);

        // Assert - should just use the values as-is, window might be invalid but that's checked elsewhere
        assertThat(result.windowStart()).isEqualTo(processedUntil);
        assertThat(result.windowEnd()).isEqualTo(trigger);
    }
}
