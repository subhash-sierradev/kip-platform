package com.integration.management.util;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CronUtils")
class CronUtilsTest {

    @Test
    @DisplayName("normalize returns empty for null/blank")
    void normalize_nullOrBlank_returnsEmpty() {
        assertThat(CronUtils.normalize(null)).isEmpty();
        assertThat(CronUtils.normalize(" ")).isEmpty();
    }

    @Test
    @DisplayName("normalize keeps non-6-field expression unchanged")
    void normalize_non6Fields_returnsOriginal() {
        assertThat(CronUtils.normalize("0 0 12 * * ? *")).isEqualTo("0 0 12 * * ? *");
        assertThat(CronUtils.normalize("0 0 12 * *")).isEqualTo("0 0 12 * *");
    }

    @Test
    @DisplayName("normalize fixes day-of-month/day-of-week ambiguity")
    void normalize_fixesDayMonthAmbiguity() {
        assertThat(CronUtils.normalize("0 0 12 * * *")).isEqualTo("0 0 12 * * ?");
        assertThat(CronUtils.normalize("0 0 12 1 * MON")).isEqualTo("0 0 12 1 * ?");
        assertThat(CronUtils.normalize("0 0 12 * * ?")).isEqualTo("0 0 12 * * ?");
    }

    @Test
    @DisplayName("validate throws for null/blank and invalid cron")
    void validate_invalid_throws() {
        assertThatThrownBy(() -> CronUtils.validate(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CronUtils.validate(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CronUtils.validate("not-a-cron"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    @Test
    @DisplayName("generateCronExpression DAILY uses fixed hour when interval <=0 or 24")
    void generate_daily_interval24OrNonPositive() {
        String cron24 = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(12, 5),
                null,
                null,
                false,
                24,
                null);
        assertThat(cron24).isEqualTo("0 5 12 * * ?");

        String cron0 = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(12, 5),
                null,
                null,
                false,
                0,
                null);
        assertThat(cron0).isEqualTo("0 5 12 * * ?");
    }

    @Test
    @DisplayName("generateCronExpression DAILY uses hour step when interval provided")
    void generate_daily_intervalStep() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(1, 15),
                null,
                null,
                false,
                6,
                null);
        assertThat(cron).isEqualTo("0 15 1/6 * * ?");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY maps days and defaults to every day")
    void generate_weekly_dayMapping() {
        String withDays = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 0),
                List.of("monday", "WED"),
                null,
                false,
                0,
                null);
        assertThat(withDays).isEqualTo("0 0 9 ? * MON,WED");

        String defaultEveryDay = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 0),
                List.of(),
                null,
                false,
                0,
                null);
        assertThat(defaultEveryDay).isEqualTo("0 0 9 ? * *");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY defaults null/blank to MON and unknown to SUN")
    void generate_weekly_nullBlankAndUnknownDays() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 0),
                Arrays.asList(null, " ", "noday"),
                null,
                false,
                0,
                null);

        assertThat(cron).isEqualTo("0 0 9 ? * MON,MON,SUN");
    }

    @Test
    @DisplayName("generateCronExpression WEEKLY covers all day mapping cases")
    void generate_weekly_allDayMappings() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.WEEKLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 0),
                List.of("MON", "TUESDAY", "WEDNESDAY", "THU", "FRIDAY", "SAT", "SUNDAY"),
                null,
                false,
                0,
                null);

        assertThat(cron).isEqualTo("0 0 9 ? * MON,TUE,WED,THU,FRI,SAT,SUN");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY uses day-of-month or L and distinct months")
    void generate_monthly_distinctMonthsAndMonthEnd() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 30),
                null,
                List.of("JANUARY", "JANUARY", "MARCH"),
                false,
                0,
                null);
        assertThat(cron).isEqualTo("0 30 9 6 1,3 ?");

        String cronLast = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 30),
                null,
                List.of("JAN"),
                true,
                0,
                null);
        assertThat(cronLast).isEqualTo("0 30 9 L 1 ?");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY allows null date when executing on month end")
    void generate_monthly_monthEnd_allowsNullDate() {
        String cronLast = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                null,
                LocalTime.of(9, 30),
                null,
                List.of("JAN"),
                true,
                0,
                null);

        assertThat(cronLast).isEqualTo("0 30 9 L 1 ?");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY maps null/blank months to 1 and invalid to 1")
    void generate_monthly_nullBlankAndInvalidMonths() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 30),
                null,
                Arrays.asList(null, " ", "invalid"),
                false,
                0,
                null);

        assertThat(cron).isEqualTo("0 30 9 6 1,1,1 ?");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY covers all month mapping cases")
    void generate_monthly_allMonthMappings() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(9, 30),
                null,
                List.of("JAN", "FEBRUARY", "MAR", "APRIL", "MAY", "JUN", "JULY", "AUG", "SEP", "OCTOBER", "NOV", "DECEMBER"),
                false,
                0,
                null);

        assertThat(cron).isEqualTo("0 30 9 6 1,2,3,4,5,6,7,8,9,10,11,12 ?");
    }

    @Test
    @DisplayName("generateCronExpression MONTHLY throws when date/time null")
    void generate_monthly_nullDateTime_throws() {
        assertThatThrownBy(() -> CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                null,
                LocalTime.of(9, 30),
                null,
                null,
                false,
                0,
                null)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CronUtils.generateCronExpression(
                FrequencyPattern.MONTHLY,
                LocalDate.of(2026, 2, 6),
                null,
                null,
                null,
                false,
                0,
                null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("generateCronExpression CUSTOM returns normalized custom expression")
    void generate_custom_normalizes() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.CUSTOM,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(12, 0),
                null,
                null,
                false,
                0,
                "0 0 12 * * *");
        assertThat(cron).isEqualTo("0 0 12 * * ?");
    }

    @Test
    @DisplayName("UTC overload converts time to user timezone before generating")
    void generate_utcOverload_convertsToUserTimezone() {
        // 12:00 UTC on Feb 6 => 07:00 America/New_York
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(12, 0),
                null,
                null,
                false,
                24,
                null,
                "America/New_York");

        assertThat(cron).isEqualTo("0 0 7 * * ?");
    }

    @Test
    @DisplayName("UTC overload converts time when date is null using current UTC date")
    void generate_utcOverload_nullDate_convertsTime() {
        // 23:30 UTC => 05:00 Asia/Kolkata (next day). For DAILY, date shift doesn't matter.
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                null,
                LocalTime.of(23, 30),
                null,
                null,
                false,
                24,
                null,
                "Asia/Kolkata");

        assertThat(cron).isEqualTo("0 0 5 * * ?");
    }

    @Test
    @DisplayName("UTC overload throws for invalid timezone")
    void generate_utcOverload_invalidTimezone_throws() {
        assertThatThrownBy(() -> CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(12, 0),
                null,
                null,
                false,
                24,
                null,
                "Not/AZone"))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    @DisplayName("UTC overload does not convert when timezone is blank")
    void generate_utcOverload_blankTimezone_noConversion() {
        String cron = CronUtils.generateCronExpression(
                FrequencyPattern.DAILY,
                LocalDate.of(2026, 2, 6),
                LocalTime.of(12, 0),
                null,
                null,
                false,
                24,
                null,
                " ");

        assertThat(cron).isEqualTo("0 0 12 * * ?");
    }
}
