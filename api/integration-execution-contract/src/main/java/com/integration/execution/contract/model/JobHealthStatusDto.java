package com.integration.execution.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for job scheduling health check responses.
 * Provides information about the consistency between database schedules and
 * Quartz jobs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobHealthStatusDto {

    /**
     * Number of integration schedules that should be active based on database
     */
    private int expectedJobCount;

    /**
     * Number of jobs actually scheduled in Quartz
     */
    private int actualJobCount;

    /**
     * Whether job scheduling is healthy (expected == actual)
     */
    private boolean healthy;

    /**
     * Number of jobs missing from Quartz but present in database
     */
    private int missingJobCount;

    /**
     * Number of orphaned jobs in Quartz (not in database)
     */
    private int orphanedJobCount;

    /**
     * Last time job reload was attempted
     */
    private String lastReloadAttempt;

    /**
     * Status message describing current state
     */
    private String statusMessage;

    /**
     * Overall health status
     */
    private HealthLevel healthLevel;

    public enum HealthLevel {
        HEALTHY, // All jobs properly synchronized
        WARNING, // Minor inconsistencies
        CRITICAL // Major inconsistencies or errors
    }

    /**
     * Create a healthy status
     */
    public static JobHealthStatusDto healthy(int jobCount, String lastReload) {
        return JobHealthStatusDto.builder()
                .expectedJobCount(jobCount)
                .actualJobCount(jobCount)
                .healthy(true)
                .missingJobCount(0)
                .orphanedJobCount(0)
                .lastReloadAttempt(lastReload)
                .healthLevel(HealthLevel.HEALTHY)
                .statusMessage("All scheduled jobs are properly synchronized")
                .build();
    }

    /**
     * Create an unhealthy status
     */
    public static JobHealthStatusDto unhealthy(int expected, int actual, int missing, int orphaned, String lastReload) {
        HealthLevel level = (missing > 0 || orphaned > 5) ? HealthLevel.CRITICAL : HealthLevel.WARNING;

        String message = String.format("Job synchronization issues detected: %d missing, %d orphaned",
                missing, orphaned);

        return JobHealthStatusDto.builder()
                .expectedJobCount(expected)
                .actualJobCount(actual)
                .healthy(false)
                .missingJobCount(missing)
                .orphanedJobCount(orphaned)
                .lastReloadAttempt(lastReload)
                .healthLevel(level)
                .statusMessage(message)
                .build();
    }

    /**
     * Create an error status when health check fails
     */
    public static JobHealthStatusDto error(String errorMessage) {
        return JobHealthStatusDto.builder()
                .expectedJobCount(-1)
                .actualJobCount(-1)
                .healthy(false)
                .healthLevel(HealthLevel.CRITICAL)
                .statusMessage("Health check failed: " + errorMessage)
                .build();
    }
}