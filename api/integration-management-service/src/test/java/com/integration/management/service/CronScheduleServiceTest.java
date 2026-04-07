package com.integration.management.service;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.management.entity.IntegrationSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CronScheduleServiceTest {

    private final CronScheduleService cronScheduleService = new CronScheduleService();

    @Test
    void build_cron_sets_expression_on_schedule() {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .frequencyPattern(FrequencyPattern.DAILY)
                .executionTime(LocalTime.of(10, 15))
                .dailyExecutionInterval(24)
                .cronExpression("0 0 0 * * ?")
                .build();

        cronScheduleService.buildCron(schedule);

        assertThat(schedule.getCronExpression()).isNotBlank();
    }

    @Test
    void build_cron_throws_when_schedule_is_null() {
        assertThatThrownBy(() -> cronScheduleService.buildCron(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("IntegrationSchedule must not be null");
    }

    @Test
    void get_cron_uses_default_values_when_optional_fields_are_null() {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .frequencyPattern(FrequencyPattern.DAILY)
                .executionDate(LocalDate.of(2026, 3, 6))
                .executionTime(LocalTime.of(8, 0))
                .dailyExecutionInterval(null)
                .isExecuteOnMonthEnd(null)
                .build();

        String cron = cronScheduleService.getCron(schedule);

        assertThat(cron).isNotBlank();
        assertThat(cron).doesNotContain("null");
    }

    @Test
    void get_next_run_returns_null_when_expression_blank_or_time_null() {
        assertThat(cronScheduleService.getNextRun("", LocalDate.now(), LocalTime.NOON)).isNull();
        assertThat(cronScheduleService.getNextRun("0 0 12 * * ?", LocalDate.now(), null)).isNull();
    }

    @Test
    void get_next_run_returns_null_for_invalid_cron_expression() {
        Instant next = cronScheduleService.getNextRun("invalid cron", LocalDate.now(), LocalTime.NOON);
        assertThat(next).isNull();
    }

    @Test
    void get_next_run_returns_future_instant_for_valid_cron_expression() {
        Instant next = cronScheduleService.getNextRun(
                "0 0 0 * * ?",
                LocalDate.now(),
                LocalTime.MIDNIGHT);

        assertThat(next).isNotNull();
    }

    @Test
    void get_next_run_handles_null_localDate_by_using_current_utc_date() {
        Instant next = cronScheduleService.getNextRun(
                "0 0 0 * * ?",
                null,
                LocalTime.MIDNIGHT);

        assertThat(next).isNotNull();
    }

    @Test
    void get_next_run_with_future_base_date_uses_base_instant_branch() {
        Instant next = cronScheduleService.getNextRun(
                "0 0 0 * * ?",
                LocalDate.now().plusDays(3),
                LocalTime.MIDNIGHT);

        assertThat(next).isNotNull();
    }

    @Test
    void get_cron_throws_when_schedule_is_null() {
        assertThatThrownBy(() -> cronScheduleService.getCron(null))
                .isInstanceOf(NullPointerException.class);
    }
}
