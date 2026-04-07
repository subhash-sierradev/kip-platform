package com.integration.management.mapper;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntegrationSchedulerMapper")
class IntegrationSchedulerMapperTest {

    @Test
    @DisplayName("toEntity returns null for null")
    void toEntity_null_returnsNull() {
        IntegrationSchedulerMapper mapper = new IntegrationSchedulerMapperImpl();
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toResponse returns null for null")
    void toResponse_null_returnsNull() {
        IntegrationSchedulerMapper mapper = new IntegrationSchedulerMapperImpl();
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toEntity maps businessTimeZone from request to entity")
    void toEntity_convertsToUtc() {
        IntegrationSchedulerMapper mapper = new IntegrationSchedulerMapperImpl();

        IntegrationScheduleRequest request = IntegrationScheduleRequest.builder()
                .executionDate(LocalDate.of(2026, 2, 6))
                .executionTime(LocalTime.of(12, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .daySchedule(List.of("MONDAY"))
                .monthSchedule(List.of("JANUARY"))
                .isExecuteOnMonthEnd(false)
                .cronExpression("0 0 12 * * ?")
                .businessTimeZone("America/New_York")
                .build();

        IntegrationSchedule entity = mapper.toEntity(request);
        assertThat(entity).isNotNull();

        assertThat(entity.getExecutionTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(entity.getExecutionDate()).isEqualTo(LocalDate.of(2026, 2, 6));
        assertThat(entity.getDaySchedule()).containsExactly("MONDAY");
        assertThat(entity.getMonthSchedule()).containsExactly("JANUARY");
        assertThat(entity.getBusinessTimeZone()).isEqualTo("America/New_York");
    }

    @Test
    @DisplayName("updateEntity covers collection update branches")
    void updateEntity_collectionUpdateBranches() {
        IntegrationSchedulerMapper mapper = new IntegrationSchedulerMapperImpl();

        IntegrationSchedule existing = IntegrationSchedule.builder()
                .daySchedule(new ArrayList<>(List.of("A")))
                .monthSchedule(new ArrayList<>(List.of("B")))
                .build();

        IntegrationScheduleRequest update = IntegrationScheduleRequest.builder()
                .executionDate(LocalDate.of(2026, 2, 6))
                .executionTime(LocalTime.of(12, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .daySchedule(List.of("X", "Y"))
                .monthSchedule(null)
                .build();

        mapper.updateEntity(update, existing);
        assertThat(existing.getDaySchedule()).containsExactly("X", "Y");
        assertThat(existing.getMonthSchedule()).isNull();

        IntegrationSchedule existingNullLists = IntegrationSchedule.builder().build();
        IntegrationScheduleRequest updateWithLists = IntegrationScheduleRequest.builder()
                .executionDate(LocalDate.of(2026, 2, 6))
                .executionTime(LocalTime.of(12, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .daySchedule(List.of("D"))
                .monthSchedule(List.of("M"))
                .build();

        mapper.updateEntity(updateWithLists, existingNullLists);
        assertThat(existingNullLists.getDaySchedule()).containsExactly("D");
        assertThat(existingNullLists.getMonthSchedule()).containsExactly("M");
    }

    @Test
    @DisplayName("updateEntity returns early for null request")
    void updateEntity_nullRequest_returnsEarly() {
        IntegrationSchedulerMapper mapper = new IntegrationSchedulerMapperImpl();

        IntegrationSchedule existing = IntegrationSchedule.builder()
                .executionTime(LocalTime.of(1, 2))
                .daySchedule(new ArrayList<>(List.of("A")))
                .build();

        mapper.updateEntity(null, existing);

        assertThat(existing.getExecutionTime()).isEqualTo(LocalTime.of(1, 2));
        assertThat(existing.getDaySchedule()).containsExactly("A");
    }
}
