package com.integration.management.repository.projection;

import com.integration.execution.contract.model.enums.JobExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Projection interface for ArcGIS Integration summary query results.
 * Provides optimized data retrieval with only necessary fields including execution metadata.
 */
public interface ArcGISIntegrationSummaryProjection {

    UUID getId();

    String getName();

    String getItemType();

    String getItemSubtype();

    String getDynamicDocumentType();

    String getFrequencyPattern();

    Integer getDailyExecutionInterval();

    LocalDate getExecutionDate();

    LocalTime getExecutionTime();

    String getDaySchedule();

    String getMonthSchedule();

    Boolean getIsExecuteOnMonthEnd();

    String getCronExpression();

    String getBusinessTimeZone();

    Instant getCreatedDate();

    String getCreatedBy();

    Instant getLastModifiedDate();

    String getLastModifiedBy();

    Boolean getIsEnabled();

    Instant getLastAttemptTimeUtc();

    JobExecutionStatus getLastStatus();
}
