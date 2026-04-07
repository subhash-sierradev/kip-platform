package com.integration.management.repository.projection;

import com.integration.execution.contract.model.enums.JobExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Projection interface for Confluence Integration summary query results.
 * Provides optimized data retrieval with only necessary fields including execution metadata.
 */
public interface ConfluenceIntegrationSummaryProjection {

    UUID getId();

    String getName();

    String getDocumentItemType();

    String getDocumentItemSubtype();

    String getDynamicDocumentType();


    String getConfluenceSpaceKey();

    String getFrequencyPattern();

    Integer getDailyExecutionInterval();

    LocalDate getExecutionDate();

    LocalTime getExecutionTime();

    String getDaySchedule();

    String getMonthSchedule();

    Boolean getIsExecuteOnMonthEnd();

    String getCronExpression();

    Instant getCreatedDate();

    String getCreatedBy();

    Instant getLastModifiedDate();

    String getLastModifiedBy();

    Boolean getIsEnabled();

    Instant getLastAttemptTimeUtc();

    JobExecutionStatus getLastStatus();
}
