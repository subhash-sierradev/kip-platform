package com.integration.management.model.dto.request;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntegrationScheduleRequest")
class IntegrationScheduleRequestTest {

    @Test
    @DisplayName("builder populates schedule request fields")
    void builderPopulatesScheduleRequestFields() {
        UUID id = UUID.randomUUID();

        IntegrationScheduleRequest request = IntegrationScheduleRequest.builder()
                .id(id)
                .executionDate(LocalDate.of(2026, 3, 6))
                .executionTime(LocalTime.of(10, 30))
                .frequencyPattern(FrequencyPattern.MONTHLY)
                .dailyExecutionInterval(2)
                .daySchedule(List.of("MONDAY", "FRIDAY"))
                .monthSchedule(List.of("1", "15"))
                .isExecuteOnMonthEnd(true)
                .cronExpression("0 30 10 * * ?")
                .build();

        assertThat(request.getId()).isEqualTo(id);
        assertThat(request.getExecutionTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(request.getFrequencyPattern()).isEqualTo(FrequencyPattern.MONTHLY);
        assertThat(request.getDaySchedule()).containsExactly("MONDAY", "FRIDAY");
        assertThat(request.getIsExecuteOnMonthEnd()).isTrue();
    }

    @Test
    @DisplayName("bean validation rejects null executionTime and frequencyPattern")
    void beanValidationRejectsNullExecutionTimeAndFrequencyPattern() {
        IntegrationScheduleRequest invalid = IntegrationScheduleRequest.builder()
                .executionDate(LocalDate.of(2026, 3, 6))
                .executionTime(null)
                .frequencyPattern(null)
                .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(invalid))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("executionTime", "frequencyPattern");
        }
    }
}
