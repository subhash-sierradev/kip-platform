package com.integration.management.util;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.StringJoiner;

public final class CronUtils {

    private CronUtils() {
    }

    public static String generateCronExpression(
            FrequencyPattern frequencyType,
            LocalDate executionDate,
            LocalTime executionTime,
            List<String> daySchedule,
            List<String> monthSchedule,
            boolean isExecuteOnMonthEnd,
            int dailyExecutionInterval,
            String cronExpression
    ) {
        String cron = switch (frequencyType) {
            case DAILY -> daily(executionTime, dailyExecutionInterval);
            case WEEKLY -> weekly(executionTime, daySchedule);
            case MONTHLY -> monthly(executionDate, executionTime, monthSchedule, isExecuteOnMonthEnd);
            case CUSTOM -> cronExpression;
        };

        cron = normalize(cron);
        validate(cron);
        return cron;
    }

    // New overload: converts UTC inputs to user's local time before building cron
    public static String generateCronExpression(
            FrequencyPattern frequencyType,
            LocalDate executionDateUtc,
            LocalTime executionTimeUtc,
            List<String> daySchedule,
            List<String> monthSchedule,
            boolean isExecuteOnMonthEnd,
            int dailyExecutionInterval,
            String cronExpression,
            String userTimezone
    ) {
        LocalDate localDate = executionDateUtc;
        LocalTime localTime = executionTimeUtc;

        if (userTimezone != null && !userTimezone.isBlank() && executionTimeUtc != null) {
            ZoneId userZone = ZoneId.of(userTimezone);
            // Use the provided execution date when available for accurate timezone (including DST) conversion;
            // otherwise fall back to a stable UTC-based date
            LocalDate dateForConversion = (executionDateUtc != null) ? executionDateUtc : LocalDate.now(ZoneOffset.UTC);
            ZonedDateTime utcZdt = ZonedDateTime.of(dateForConversion, executionTimeUtc, ZoneOffset.UTC);
            ZonedDateTime localZdt = utcZdt.withZoneSameInstant(userZone);

            localTime = localZdt.toLocalTime();
            if (executionDateUtc != null) {
                localDate = localZdt.toLocalDate();
            }
        }

        return generateCronExpression(
                frequencyType,
                localDate,
                localTime,
                daySchedule,
                monthSchedule,
                isExecuteOnMonthEnd,
                dailyExecutionInterval,
                cronExpression
        );
    }


    private static String daily(LocalTime time, int interval) {
        return interval <= 0 || interval == 24
                ? String.format("0 %d %d * * ?", time.getMinute(), time.getHour())
                : String.format("0 %d %d/%d * * ?",
                time.getMinute(),
                time.getHour(),   // start hour
                interval);
    }


    private static String weekly(LocalTime time, List<String> days) {
        StringJoiner joiner = new StringJoiner(",");

        if (days == null || days.isEmpty()) {
            joiner.add("*"); // every day
        } else {
            days.forEach(d -> joiner.add(toCronDay(d)));
        }

        return String.format("0 %d %d ? * %s",
                time.getMinute(),
                time.getHour(),
                joiner);
    }


    private static String monthly(LocalDate date, LocalTime time,
                                  List<String> months,
                                  boolean isExecuteOnMonthEnd) {

        if (time == null) {
            throw new IllegalArgumentException("Date/time cannot be null");
        }

        if (!isExecuteOnMonthEnd && date == null) {
            throw new IllegalArgumentException("Execution date must be provided when not executing on month end");
        }

        StringJoiner joiner = new StringJoiner(",");

        if (months == null || months.isEmpty()) {
            joiner.add("*");
        } else {
            months.stream()
                    .distinct()
                    .forEach(m -> joiner.add(
                            String.valueOf(toCronMonth(m))));
        }

        String dayOfMonth = isExecuteOnMonthEnd
                ? "L"
                : String.valueOf(date.getDayOfMonth());

        return String.format(
                "0 %d %d %s %s ?",
                time.getMinute(),
                time.getHour(),
                dayOfMonth,
                joiner
        );
    }


    public static String normalize(String cron) {
        if (cron == null || cron.isBlank()) {
            return "";
        }

        String[] fields = cron.trim().split("\\s+");
        if (fields.length != 6) {
            return cron;
        }

        if ("*".equals(fields[3]) && "*".equals(fields[5])) {
            fields[5] = "?";
        } else if (!"?".equals(fields[3]) && !"?".equals(fields[5])) {
            fields[5] = "?";
        }

        return String.join(" ", fields);
    }

    public static void validate(String cron) {
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException("Cron expression must be provided");
        }

        try {
            new CronExpression(normalize(cron));
        } catch (RuntimeException | ParseException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron, e);
        }
    }

    private static String toCronDay(String day) {
        if (day == null || day.isBlank()) {
            return "MON";
        }

        return switch (day.trim().toUpperCase()) {
            case "MONDAY", "MON" -> "MON";
            case "TUESDAY", "TUE" -> "TUE";
            case "WEDNESDAY", "WED" -> "WED";
            case "THURSDAY", "THU" -> "THU";
            case "FRIDAY", "FRI" -> "FRI";
            case "SATURDAY", "SAT" -> "SAT";
            case "SUNDAY", "SUN" -> "SUN";
            default -> "SUN";
        };
    }

    private static int toCronMonth(String month) {
        if (month == null || month.isBlank()) {
            return 1;
        }

        return switch (month.trim().toUpperCase()) {
            case "JANUARY", "JAN" -> 1;
            case "FEBRUARY", "FEB" -> 2;
            case "MARCH", "MAR" -> 3;
            case "APRIL", "APR" -> 4;
            case "MAY" -> 5;
            case "JUNE", "JUN" -> 6;
            case "JULY", "JUL" -> 7;
            case "AUGUST", "AUG" -> 8;
            case "SEPTEMBER", "SEP" -> 9;
            case "OCTOBER", "OCT" -> 10;
            case "NOVEMBER", "NOV" -> 11;
            case "DECEMBER", "DEC" -> 12;
            default -> 1;
        };
    }
}
