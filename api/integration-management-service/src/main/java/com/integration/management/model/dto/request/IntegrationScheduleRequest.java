package com.integration.management.model.dto.request;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationScheduleRequest {

    private UUID id;
    private LocalDate executionDate;
    @NotNull(message = "Execution time is required")
    private LocalTime executionTime;
    @NotNull
    private FrequencyPattern frequencyPattern;
    private Integer dailyExecutionInterval;
    private List<String> daySchedule;
    private List<String> monthSchedule;
    private Boolean isExecuteOnMonthEnd;
    private String cronExpression;
    @NotNull(message = "Business time zone is required")
    @NotBlank(message = "Business time zone cannot be blank")
    @Builder.Default
    private String businessTimeZone = "UTC";
    @NotNull
    private TimeCalculationMode timeCalculationMode;
}
