package com.integration.management.entity;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "integration_schedules", schema = "integration_platform")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationSchedule {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id")
    private UUID id;

    @Column(name = "execution_date")
    private LocalDate executionDate;

    @Column(name = "execution_time", nullable = false)
    @NotNull(message = "Execution time is required")
    private LocalTime executionTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_pattern", nullable = false, length = 20)
    @NotNull(message = "Frequency pattern is required")
    private FrequencyPattern frequencyPattern;

    @Column(name = "daily_execution_interval")
    private Integer dailyExecutionInterval; // 1, 6, 12, 24 hours

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "day_schedule", columnDefinition = "jsonb")
    private List<String> daySchedule; // ["MONDAY", "WEDNESDAY", "FRIDAY"]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "month_schedule", columnDefinition = "jsonb")
    private List<String> monthSchedule; // ["JANUARY", "MARCH", "JUNE"]

    @Column(name = "is_execute_on_month_end", nullable = false)
    private Boolean isExecuteOnMonthEnd = false; // Whether to execute on month end if frequency selected as monthly

    @Enumerated(EnumType.STRING)
    @Column(name = "time_calculation_mode", nullable = false, length = 30)
    @Builder.Default
    private TimeCalculationMode timeCalculationMode = TimeCalculationMode.FLEXIBLE_INTERVAL;

    @Column(name = "processed_until")
    private Instant processedUntil;

    @NotNull(message = "Business time zone is required")
    @Column(name = "business_time_zone", nullable = false, length = 64)
    @Builder.Default
    private String businessTimeZone = "UTC";

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;
}
