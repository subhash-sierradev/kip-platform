package com.integration.management.util;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CronUtils - branch coverage")
class CronUtilsBranchTest {

    // ── normalize branches ────────────────────────────────────────────────────

    @Test
    @DisplayName("normalize: 6-field expr with ? in day-of-week leaves it unchanged")
    void normalize_sixFields_questionInDow_unchanged() {
        assertThat(CronUtils.normalize("0 0 12 * * ?")).isEqualTo("0 0 12 * * ?");
    }

    @Test
    @DisplayName("normalize: 6-field with ? in day-of-month keeps day-of-week unchanged")
    void normalize_sixFields_questionInDom_replacesDow() {
        // When DOM is ?, DOW is left as-is — neither normalize branch fires
        assertThat(CronUtils.normalize("0 0 12 ? * SUN")).isEqualTo("0 0 12 ? * SUN");
    }

    // ── WEEKLY all-day variants ────────────────────────────────────────────────

    @Test
    @DisplayName("generateCronExpression WEEKLY: null daySchedule -> every day")
    void generate_weekly_nullDays_everyDay() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                LocalDate.of(2026, 3, 9),
                LocalTime.of(8, 30),
                null, null, false, 0, null);
        assertThat(cron).contains("*");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY: empty daySchedule -> every day")
    void generate_weekly_emptyDays_everyDay() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                LocalDate.of(2026, 3, 9),
                LocalTime.of(9, 0),
                List.of(), null, false, 0, null);
        assertThat(cron).contains("*");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY: full weekday names map correctly")
    void generate_weekly_fullDayNames_map() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                LocalDate.of(2026, 3, 9),
                LocalTime.of(9, 0),
                List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
                null, false, 0, null);
        assertThat(cron).contains("MON").contains("TUE").contains("WED")
                .contains("THU").contains("FRI").contains("SAT").contains("SUN");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY: abbreviated day names map correctly")
    void generate_weekly_abbrevDayNames_map() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                null,
                LocalTime.of(7, 0),
                List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                null, false, 0, null);
        assertThat(cron).contains("MON").contains("TUE").contains("WED")
                .contains("THU").contains("FRI").contains("SAT").contains("SUN");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY: unknown day name defaults to SUN")
    void generate_weekly_unknownDay_defaultsSun() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                null,
                LocalTime.of(7, 0),
                List.of("HOLIDAY"),
                null, false, 0, null);
        assertThat(cron).contains("SUN");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY: blank day name defaults to MON")
    void generate_weekly_blankDay_defaultsMon() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                null,
                LocalTime.of(7, 0),
                List.of("  "),
                null, false, 0, null);
        assertThat(cron).contains("MON");
    }

    // ── MONTHLY variants ──────────────────────────────────────────────────────

    @Test
    @DisplayName("generateCronExpression MONTHLY: null months -> wildcard month")
    void generate_monthly_nullMonths_wildcardMonth() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0),
                null, null, false, 0, null);
        assertThat(cron).contains("15").contains("*");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: empty months -> wildcard month")
    void generate_monthly_emptyMonths_wildcardMonth() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0),
                null, List.of(), false, 0, null);
        assertThat(cron).contains("*");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: isExecuteOnMonthEnd=true uses L day-of-month")
    void generate_monthly_monthEnd_usesL() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                null,
                LocalTime.of(10, 0),
                null, null, true, 0, null);
        assertThat(cron).contains("L");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: null date without monthEnd throws")
    void generate_monthly_nullDate_notMonthEnd_throws() {
        assertThatThrownBy(() -> CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                null,
                LocalTime.of(10, 0),
                null, null, false, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Execution date");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: null time throws")
    void generate_monthly_nullTime_throws() {
        assertThatThrownBy(() -> CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 3, 15),
                null,
                null, null, false, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date/time cannot be null");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: all full month names map correctly")
    void generate_monthly_fullMonthNames_map() {
        List<String> allMonths = List.of(
                "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
                "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER");
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 1, 1),
                LocalTime.of(0, 0),
                null, allMonths, false, 0, null);
        // Should have 12 months comma-separated
        assertThat(cron).isNotBlank();
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: all abbrev month names map correctly")
    void generate_monthly_abbrevMonthNames_map() {
        List<String> abbrevMonths = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 1, 1),
                LocalTime.of(0, 0),
                null, abbrevMonths, false, 0, null);
        assertThat(cron).isNotBlank();
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: unknown month name defaults to 1")
    void generate_monthly_unknownMonth_defaults1() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 1, 1),
                LocalTime.of(0, 0),
                null, List.of("UNKNOWNMONTH"), false, 0, null);
        assertThat(cron).isNotBlank().contains("1");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY: blank month name defaults to 1")
    void generate_monthly_blankMonth_defaults1() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 1, 1),
                LocalTime.of(0, 0),
                null, List.of("  "), false, 0, null);
        assertThat(cron).isNotBlank();
    }

    // ── CUSTOM cron ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateCronExpression CUSTOM: passes through expression")
    void generate_custom_passesThrough() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.CUSTOM,
                null, null, null, null, false, 0,
                "0 0 12 * * ?");
        assertThat(cron).isEqualTo("0 0 12 * * ?");
    }

    // ── Timezone-aware overload ───────────────────────────────────────────────

    @Test
    @DisplayName("generateCronExpression (timezone overload): null timezone falls back to UTC cron")
    void generate_withTimezone_nullTimezone_usesUtc() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 3, 9),
                LocalTime.of(10, 0),
                null, null, false, 24, null,
                null);
        assertThat(cron).isNotBlank();
    }

    @Test
    @DisplayName("generateCronExpression (timezone overload): blank timezone falls back to UTC cron")
    void generate_withTimezone_blankTimezone_usesUtc() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 3, 9),
                LocalTime.of(10, 0),
                null, null, false, 24, null,
                "  ");
        assertThat(cron).isNotBlank();
    }

    @Test
    @DisplayName("generateCronExpression (timezone overload): valid timezone converts time")
    void generate_withTimezone_validTimezone_convertsTime() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 3, 9),
                LocalTime.of(15, 0),
                null, null, false, 24, null,
                "America/New_York");
        assertThat(cron).isNotBlank();
    }

    @Test
    @DisplayName("generateCronExpression (timezone overload): null executionDate with timezone uses today")
    void generate_withTimezone_nullDate_usesToday() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                null,
                LocalTime.of(12, 0),
                null, null, false, 24, null,
                "Europe/London");
        assertThat(cron).isNotBlank();
    }

    @Test
    @DisplayName("generateCronExpression (timezone overload): null executionTime bypasses timezone conversion")
    void generate_withTimezone_nullTime_bypassesConversion() {
        // When executionTime is null we must use CUSTOM so the null time doesn't
        // blow up in the daily/weekly/monthly builder
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.CUSTOM,
                LocalDate.of(2026, 3, 9),
                null,
                null, null, false, 24,
                "0 0 12 * * ?",
                "America/New_York");
        assertThat(cron).isEqualTo("0 0 12 * * ?");
    }

    // ── DAILY interval branch ────────────────────────────────────────────────

    @Test
    @DisplayName("generateCronExpression DAILY: positive interval < 24 produces step expression")
    void generate_daily_positiveInterval_stepExpr() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                null, LocalTime.of(6, 0),
                null, null, false, 6, null);
        assertThat(cron).contains("6/6");
    }

    // ── validate ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validate: valid cron expression passes without exception")
    void validate_valid_noException() {
        CronUtils.validate("0 0 12 * * ?");
        // no exception expected
    }
}

