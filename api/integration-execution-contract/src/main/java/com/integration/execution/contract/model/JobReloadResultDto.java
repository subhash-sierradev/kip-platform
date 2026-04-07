package com.integration.execution.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for tracking the result of job reload operations during application
 * startup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobReloadResultDto {

    /**
     * The integration ID that was attempted to be reloaded
     */
    private UUID integrationId;

    /**
     * The tenant ID associated with the integration
     */
    private String tenantId;

    /**
     * Whether the job reload was successful
     */
    private boolean success;

    /**
     * Error message if the reload failed
     */
    private String errorMessage;

    /**
     * The cron expression of the schedule that was reloaded
     */
    private String cronExpression;

    /**
     * The name/title of the integration for better identification
     */
    private String integrationName;

    /**
     * Creates a successful reload result
     */
    public static JobReloadResultDto success(UUID integrationId, String tenantId,
                                          String cronExpression, String integrationName) {
        return JobReloadResultDto.builder()
                .integrationId(integrationId)
                .tenantId(tenantId)
                .success(true)
                .cronExpression(cronExpression)
                .integrationName(integrationName)
                .build();
    }

    /**
     * Creates a failed reload result
     */
    public static JobReloadResultDto failure(UUID integrationId, String tenantId,
                                          String integrationName, String errorMessage) {
        return JobReloadResultDto.builder()
                .integrationId(integrationId)
                .tenantId(tenantId)
                .success(false)
                .integrationName(integrationName)
                .errorMessage(errorMessage)
                .build();
    }
}