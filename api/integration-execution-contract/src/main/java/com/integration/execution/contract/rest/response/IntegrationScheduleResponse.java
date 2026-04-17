package com.integration.execution.contract.rest.response;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
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
public class IntegrationScheduleResponse {

    private UUID id;
    private LocalDate executionDate;
    private LocalTime executionTime;
    private FrequencyPattern frequencyPattern;
    private Integer dailyExecutionInterval;
    private List<String> daySchedule;
    private List<String> monthSchedule;
    private Boolean isExecuteOnMonthEnd;
    private String cronExpression;
    private String businessTimeZone;
    private TimeCalculationMode timeCalculationMode;
}