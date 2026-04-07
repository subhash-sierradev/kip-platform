package com.integration.management.entity;

import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing the execution status of scheduled integration jobs
 */
@Entity
@Table(name = "integration_job_executions", schema = "integration_platform")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IntegrationJobExecution {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", insertable = false, updatable = false)
    private IntegrationSchedule schedule;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, length = 20)
    private TriggerType triggeredBy;

    // Processing window (UTC)
    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;

    // Retry lineage tracking (KIP-437)
    @Column(name = "original_job_id")
    private UUID originalJobId;

    @NotNull
    @Column(name = "retry_attempt", nullable = false)
    @Builder.Default
    private Integer retryAttempt = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobExecutionStatus status;

    @NotNull
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt =  Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @NotNull
    @Column(name = "added_records", nullable = false)
    @Builder.Default
    private Integer addedRecords = 0;

    @Column(name = "added_records_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<RecordMetadata> addedRecordsMetadata;

    @NotNull
    @Column(name = "updated_records", nullable = false)
    @Builder.Default
    private Integer updatedRecords = 0;

    @Column(name = "updated_records_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<RecordMetadata> updatedRecordsMetadata;

    @NotNull
    @Column(name = "failed_records", nullable = false)
    @Builder.Default
    private Integer failedRecords = 0;

    @Column(name = "failed_records_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<FailedRecordMetadata> failedRecordsMetadata;

    @NotNull
    @Column(name = "total_records", nullable = false)
    @Builder.Default
    private Integer totalRecords = 0;

    @Column(name = "total_records_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<RecordMetadata> totalRecordsMetadata;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> executionMetadata;

    @Column(name = "triggered_by_user", length = 100)
    private String triggeredByUser;
}
