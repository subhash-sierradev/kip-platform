package com.integration.management.service;

import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.util.CronUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

@Slf4j
@Service
public class CronScheduleService {

    public void buildCron(IntegrationSchedule schedule) {
        Objects.requireNonNull(schedule, "IntegrationSchedule must not be null");
        schedule.setCronExpression(getCron(schedule));
        log.info("Generated cron expression: {}", schedule.getCronExpression());
    }

    public String getCron(IntegrationSchedule schedule) {
        Objects.requireNonNull(schedule, "IntegrationSchedule must not be null");
        return CronUtils.generateCronExpression(
                schedule.getFrequencyPattern(),
                schedule.getExecutionDate(),
                schedule.getExecutionTime(),
                schedule.getDaySchedule(),
                schedule.getMonthSchedule(),
                schedule.getIsExecuteOnMonthEnd() != null ? schedule.getIsExecuteOnMonthEnd() : false,
                schedule.getDailyExecutionInterval() != null ? schedule.getDailyExecutionInterval() : 24,
                schedule.getCronExpression());
    }

    public Instant getNextRun(String cronExpression, LocalDate localDate, LocalTime localTime) {
        if (cronExpression == null || cronExpression.isBlank() || localTime == null) {
            return null;
        }
        try {
            CronExpression cron = new CronExpression(CronUtils.normalize(cronExpression));
            cron.setTimeZone(TimeZone.getTimeZone("UTC"));
            LocalDate baseDate = (localDate != null)
                    ? localDate
                    : LocalDate.now(ZoneOffset.UTC);
            ZonedDateTime baseDateTime =
                    ZonedDateTime.of(baseDate, localTime, ZoneOffset.UTC);
            Instant baseInstant = baseDateTime.toInstant();
            Instant now = Instant.now();
            // getNextValidTimeAfter is exclusive — subtract 1ms so the schedule start time
            // itself is returned as the next fire time when the job hasn't started yet
            Instant effectiveBase = baseInstant.isBefore(now) ? now : baseInstant.minusMillis(1);
            Date next = cron.getNextValidTimeAfter(Date.from(effectiveBase));
            return next != null ? next.toInstant() : null;
        } catch (Exception e) {
            log.error("Error calculating next run time for cron expression: {}", cronExpression, e);
            return null;
        }
    }
}
