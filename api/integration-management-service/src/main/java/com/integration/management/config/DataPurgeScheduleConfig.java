package com.integration.management.config;

import com.integration.management.job.DataPurgeJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class DataPurgeScheduleConfig {

    static final String JOB_NAME = "data-purge-job";
    static final String JOB_GROUP = "maintenance";
    static final String CRON = "0 0 7 * * ?";   // 07:00 UTC = 02:00 EST / 03:00 EDT

    @Bean
    public JobDetail dataPurgeJobDetail() {
        return JobBuilder.newJob(DataPurgeJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Nightly purge of soft-deleted records past retention period")
                .storeDurably(true)
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger dataPurgeTrigger(JobDetail dataPurgeJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(dataPurgeJobDetail)
                .withIdentity(JOB_NAME + "-trigger", JOB_GROUP)
                .withDescription("Nightly trigger at 07:00 UTC (02:00 EST / 03:00 EDT)")
                .withSchedule(CronScheduleBuilder.cronSchedule(CRON)
                        .inTimeZone(TimeZone.getTimeZone("UTC"))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
