package com.integration.management.mapper;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import com.integration.execution.contract.rest.response.IntegrationScheduleResponse;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntegrationSchedulerMapper")
class IntegrationSchedulerMapperTest {

    private final IntegrationSchedulerMapper mapper = new IntegrationSchedulerMapperImpl();

    @Test
    @DisplayName("toEntity returns null for null")
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toResponse returns null for null")
    void toResponse_null_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toEntity maps all fields from request to entity")
    void toEntity_mapsAllFields() {
        IntegrationScheduleRequest request = IntegrationScheduleRequest.builder()
                .executionDate(LocalDate.of(2026, 2, 6))
                .executionTime(LocalTime.of(12, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .daySchedule(List.of("MONDAY"))
                .monthSchedule(List.of("JANUARY"))
                .isExecuteOnMonthEnd(false)
                .cronExpression("0 0 12 * * ?")
                .businessTimeZone("America/New_York")
                .timeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL)
                .build();

        IntegrationSchedule entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getExecutionDate()).isEqualTo(LocalDate.of(2026, 2, 6));
        assertThat(entity.getExecutionTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(entity.getFrequencyPattern()).isEqualTo(FrequencyPattern.DAILY);
        assertThat(entity.getDaySchedule()).containsExactly("MONDAY");
        assertThat(entity.getMonthSchedule()).containsExactly("JANUARY");
        assertThat(entity.getIsExecuteOnMonthEnd()).isFalse();
        assertThat(entity.getCronExpression()).isEqualTo("0 0 12 * * ?");
        assertThat(entity.getBusinessTimeZone()).isEqualTo("America/New_York");
        assertThat(entity.getTimeCalculationMode()).isEqualTo(TimeCalculationMode.FLEXIBLE_INTERVAL);
        // id and processedUntil are explicitly ignored by the mapper
        assertThat(entity.getId()).isNull();
        assertThat(entity.getProcessedUntil()).isNull();
    }

    @Test
    @DisplayName("toEntity with null daySchedule and monthSchedule sets them to null")
    void toEntity_nullLists_setsNullOnEntity() {
        IntegrationScheduleRequest request = IntegrationScheduleRequest.builder()
                .frequencyPattern(FrequencyPattern.DAILY)
                .daySchedule(null)
                .monthSchedule(null)
                .build();

        IntegrationSchedule entity = mapper.toEntity(request);

        assertThat(entity.getDaySchedule()).isNull();
        assertThat(entity.getMonthSchedule()).isNull();
    }

    @Test
    @DisplayName("toResponse maps all fields from entity to response")
    void toResponse_withNonNullScheduleLists_mapsCorrectly() {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .executionDate(LocalDate.of(2026, 3, 1))
                .executionTime(LocalTime.of(9, 0))
                .frequencyPattern(FrequencyPattern.WEEKLY)
                .daySchedule(List.of("MONDAY", "WEDNESDAY"))
                .monthSchedule(List.of("JANUARY", "MARCH"))
                .isExecuteOnMonthEnd(false)
                .cronExpression("0 0 9 ? * MON,WED")
                .businessTimeZone("UTC")
                .timeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL)
                .build();

        IntegrationScheduleResponse response = mapper.toResponse(schedule);

        assertThat(response).isNotNull();
        assertThat(response.getExecutionDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(response.getExecutionTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.getFrequencyPattern()).isEqualTo(FrequencyPattern.WEEKLY);
        assertThat(response.getDaySchedule()).containsExactly("MONDAY", "WEDNESDAY");
        assertThat(response.getMonthSchedule()).containsExactly("JANUARY", "MARCH");
        assertThat(response.getIsExecuteOnMonthEnd()).isFalse();
        assertThat(response.getCronExpression()).isEqualTo("0 0 9 ? * MON,WED");
        assertThat(response.getBusinessTimeZone()).isEqualTo("UTC");
        assertThat(response.getTimeCalculationMode()).isEqualTo(TimeCalculationMode.FLEXIBLE_INTERVAL);
    }

    @Test
    @DisplayName("toResponse with null day and month schedules sets them to null")
    void toResponse_withNullScheduleLists_setsNull() {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .frequencyPattern(FrequencyPattern.DAILY)
                .daySchedule(null)
                .monthSchedule(null)
                .build();

        IntegrationScheduleResponse response = mapper.toResponse(schedule);

        assertThat(response.getDaySchedule()).isNull();
        assertThat(response.getMonthSchedule()).isNull();
    }

    @Nested
    @DisplayName("updateEntity branch coverage")
    class UpdateEntityBranches {

        @Test
        @DisplayName("updateEntity returns early for null request")
        void nullRequest_returnsEarly() {
            IntegrationSchedule existing = IntegrationSchedule.builder()
                    .executionTime(LocalTime.of(1, 2))
                    .daySchedule(new ArrayList<>(List.of("A")))
                    .build();

            mapper.updateEntity(null, existing);

            assertThat(existing.getExecutionTime()).isEqualTo(LocalTime.of(1, 2));
            assertThat(existing.getDaySchedule()).containsExactly("A");
        }

        @Test
        @DisplayName("dto daySchedule non-null overwrites existing daySchedule and monthSchedule")
        void existingDayNonNull_dtoDayNonNull_overwritesBothLists() {
            IntegrationSchedule existing = IntegrationSchedule.builder()
                    .daySchedule(new ArrayList<>(List.of("A")))
                    .monthSchedule(new ArrayList<>(List.of("B")))
                    .build();

            IntegrationScheduleRequest dto = IntegrationScheduleRequest.builder()
                    .daySchedule(List.of("X", "Y"))
                    .monthSchedule(List.of("C", "D"))
                    .build();

            mapper.updateEntity(dto, existing);

            assertThat(existing.getDaySchedule()).containsExactly("X", "Y");
            assertThat(existing.getMonthSchedule()).containsExactly("C", "D");
        }

        @Test
        @DisplayName("dto daySchedule null sets existing daySchedule and monthSchedule to null")
        void existingDayNonNull_dtoDayNull_setsNull() {
            IntegrationSchedule existing = IntegrationSchedule.builder()
                    .daySchedule(new ArrayList<>(List.of("A")))
                    .monthSchedule(new ArrayList<>(List.of("B")))
                    .build();

            IntegrationScheduleRequest dto = IntegrationScheduleRequest.builder()
                    .daySchedule(null)
                    .monthSchedule(null)
                    .build();

            mapper.updateEntity(dto, existing);

            assertThat(existing.getDaySchedule()).isNull();
            assertThat(existing.getMonthSchedule()).isNull();
        }

        @Test
        @DisplayName("existing daySchedule null, dto daySchedule non-null -> sets new list")
        void existingDayNull_dtoDayNonNull_setsNewList() {
            IntegrationSchedule existing = IntegrationSchedule.builder()
                    .daySchedule(null)
                    .monthSchedule(null)
                    .build();

            IntegrationScheduleRequest dto = IntegrationScheduleRequest.builder()
                    .daySchedule(List.of("D"))
                    .monthSchedule(List.of("M"))
                    .build();

            mapper.updateEntity(dto, existing);

            assertThat(existing.getDaySchedule()).containsExactly("D");
            assertThat(existing.getMonthSchedule()).containsExactly("M");
        }

        @Test
        @DisplayName("existing daySchedule null, dto daySchedule null -> remains null")
        void existingDayNull_dtoDayNull_remainsNull() {
            IntegrationSchedule existing = IntegrationSchedule.builder()
                    .daySchedule(null)
                    .monthSchedule(null)
                    .build();

            IntegrationScheduleRequest dto = IntegrationScheduleRequest.builder()
                    .daySchedule(null)
                    .monthSchedule(null)
                    .build();

            mapper.updateEntity(dto, existing);

            assertThat(existing.getDaySchedule()).isNull();
            assertThat(existing.getMonthSchedule()).isNull();
        }

        @Test
        @DisplayName("updateEntity maps scalar fields from dto to existing entity")
        void updateEntity_scalarFields_areMapped() {
            IntegrationSchedule existing = IntegrationSchedule.builder()
                    .executionTime(LocalTime.of(1, 0))
                    .frequencyPattern(FrequencyPattern.DAILY)
                    .cronExpression("old-cron")
                    .businessTimeZone("UTC")
                    .build();

            IntegrationScheduleRequest dto = IntegrationScheduleRequest.builder()
                    .executionDate(LocalDate.of(2026, 6, 1))
                    .executionTime(LocalTime.of(8, 30))
                    .frequencyPattern(FrequencyPattern.WEEKLY)
                    .cronExpression("0 30 8 ? * MON")
                    .businessTimeZone("America/New_York")
                    .timeCalculationMode(TimeCalculationMode.FLEXIBLE_INTERVAL)
                    .build();

            mapper.updateEntity(dto, existing);

            assertThat(existing.getExecutionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(existing.getExecutionTime()).isEqualTo(LocalTime.of(8, 30));
            assertThat(existing.getFrequencyPattern()).isEqualTo(FrequencyPattern.WEEKLY);
            assertThat(existing.getCronExpression()).isEqualTo("0 30 8 ? * MON");
            assertThat(existing.getBusinessTimeZone()).isEqualTo("America/New_York");
            assertThat(existing.getTimeCalculationMode()).isEqualTo(TimeCalculationMode.FLEXIBLE_INTERVAL);
            // id and processedUntil must never be touched by updateEntity
            assertThat(existing.getId()).isNull();
            assertThat(existing.getProcessedUntil()).isNull();
        }
    }
}

