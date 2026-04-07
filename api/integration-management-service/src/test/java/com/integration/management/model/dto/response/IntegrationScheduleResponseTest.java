package com.integration.management.model.dto.response;

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

@DisplayName("IntegrationScheduleResponse")
class IntegrationScheduleResponseTest {

    @Test
    @DisplayName("builder populates scheduling fields")
    void builderPopulatesSchedulingFields() {
        UUID id = UUID.randomUUID();

        IntegrationScheduleResponse response = IntegrationScheduleResponse.builder()
                .id(id)
                .executionDate(LocalDate.of(2026, 3, 6))
                .executionTime(LocalTime.of(14, 30))
                .frequencyPattern(FrequencyPattern.MONTHLY)
                .dailyExecutionInterval(2)
                .daySchedule(List.of("MONDAY", "FRIDAY"))
                .monthSchedule(List.of("1", "15"))
                .isExecuteOnMonthEnd(true)
                .cronExpression("0 30 14 * * ?")
                .businessTimeZone("America/New_York")
                .build();

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getFrequencyPattern()).isEqualTo(FrequencyPattern.MONTHLY);
        assertThat(response.getDaySchedule()).containsExactly("MONDAY", "FRIDAY");
        assertThat(response.getIsExecuteOnMonthEnd()).isTrue();
        assertThat(response.getBusinessTimeZone()).isEqualTo("America/New_York");
    }

    @Test
    @DisplayName("bean validation rejects null required schedule fields")
    void beanValidationRejectsNullRequiredScheduleFields() {
        IntegrationScheduleResponse invalid = IntegrationScheduleResponse.builder()
                .executionDate(null)
                .executionTime(null)
                .frequencyPattern(null)
                .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(invalid))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("executionDate", "executionTime", "frequencyPattern");
        }
    }
}
