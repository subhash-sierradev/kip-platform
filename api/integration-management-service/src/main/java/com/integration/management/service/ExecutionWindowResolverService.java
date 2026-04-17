package com.integration.management.service;

import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.model.ExecutionWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.integration.management.constants.IntegrationManagementConstants.CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;
import static com.integration.management.constants.IntegrationManagementConstants.CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START;
import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_ARCGIS;
import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_CONFLUENCE;

/**
 * Resolves the processing window for an integration execution
 * based on the schedule's {@link TimeCalculationMode}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionWindowResolverService {

    private final SettingsService settingsService;

    public ExecutionWindow resolve(
            IntegrationSchedule schedule,
            Instant triggerTime,
            String tenantId,
            String integrationType) {

        Instant windowStart = schedule.getProcessedUntil();
        log.info("Resolving execution window for schedule {} with triggerTime={}, initial windowStart={}",
                schedule.getId(), triggerTime, windowStart);
        if (windowStart == null) {
            windowStart = resolveFallbackStart(tenantId, integrationType);
            log.info(
                    "First run for schedule {}: fallback windowStart={}", schedule.getId(), windowStart);
        }
        return switch (schedule.getTimeCalculationMode()) {
            case FLEXIBLE_INTERVAL -> new ExecutionWindow(windowStart, triggerTime);
            case FIXED_DAY_BOUNDARY -> resolveFixedDay(schedule, windowStart, triggerTime);
        };
    }

    private ExecutionWindow resolveFixedDay(
            IntegrationSchedule schedule,
            Instant windowStart,
            Instant triggerTime) {

        ZoneId zoneId = ZoneId.of(schedule.getBusinessTimeZone());

        // Align windowStart to midnight in business timezone
        Instant alignedStart = windowStart.atZone(zoneId).toLocalDate()
                .atStartOfDay(zoneId)
                .toInstant();

        // Align windowEnd to midnight of trigger day in business timezone (exclusive)
        Instant alignedEnd = triggerTime.atZone(zoneId).toLocalDate()
                .atStartOfDay(zoneId)
                .minus(1, ChronoUnit.MILLIS)
                .toInstant();

        // Validate window: throw error if start is after end
        if (alignedStart.isAfter(alignedEnd)) {
            log.warn("Skipping job run for schedule {}: alignedStart ({}) is after alignedEnd ({}). "
                    + "This typically means the job was triggered on the same day as the last execution. "
                    + "Timezone: {}", schedule.getId(), alignedStart, alignedEnd, zoneId);
            throw new IllegalStateException("Invalid execution window: start is after end");
        }

        log.info("Resolved FIXED_DAY_BOUNDARY window for schedule {}: original=[{} to {}], "
                        + "aligned=[{} to {}], timezone={}",
                schedule.getId(), windowStart, triggerTime, alignedStart, alignedEnd, zoneId);

        return new ExecutionWindow(alignedStart, alignedEnd);
    }

    private Instant resolveFallbackStart(String tenantId, String integrationType) {
        String configKey = switch (integrationType.toUpperCase()) {
            case INTEGRATION_TYPE_ARCGIS -> CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;
            case INTEGRATION_TYPE_CONFLUENCE -> CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START;
            default -> throw new IllegalArgumentException("Unknown integration type: " + integrationType);
        };
        return settingsService
                .getConfigValueAsTimestamp(configKey, tenantId)
                .orElse(Instant.EPOCH);
    }
}
