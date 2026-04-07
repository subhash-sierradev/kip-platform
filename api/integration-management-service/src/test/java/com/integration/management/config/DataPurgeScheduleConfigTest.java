package com.integration.management.config;

import com.integration.management.job.DataPurgeJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataPurgeScheduleConfig")
class DataPurgeScheduleConfigTest {

    private final DataPurgeScheduleConfig config = new DataPurgeScheduleConfig();

    @Test
    @DisplayName("creates durable recovery-enabled data purge job")
    void dataPurgeJobDetail_createsExpectedJobDetail() {
        JobDetail jobDetail = config.dataPurgeJobDetail();

        assertThat(jobDetail.getKey().getName()).isEqualTo(DataPurgeScheduleConfig.JOB_NAME);
        assertThat(jobDetail.getKey().getGroup()).isEqualTo(DataPurgeScheduleConfig.JOB_GROUP);
        assertThat(jobDetail.getDescription()).contains("Nightly purge");
        assertThat(jobDetail.isDurable()).isTrue();
        assertThat(jobDetail.requestsRecovery()).isTrue();
        assertThat(jobDetail.getJobClass()).isEqualTo(DataPurgeJob.class);
    }

    @Test
    @DisplayName("creates UTC cron trigger with expected identity")
    void dataPurgeTrigger_createsExpectedCronTrigger() {
        JobDetail jobDetail = config.dataPurgeJobDetail();
        Trigger trigger = config.dataPurgeTrigger(jobDetail);

        assertThat(trigger.getKey().getName()).isEqualTo(DataPurgeScheduleConfig.JOB_NAME + "-trigger");
        assertThat(trigger.getKey().getGroup()).isEqualTo(DataPurgeScheduleConfig.JOB_GROUP);
        assertThat(trigger.getDescription()).contains("07:00 UTC");

        assertThat(trigger).isInstanceOf(CronTrigger.class);
        CronTrigger cronTrigger = (CronTrigger) trigger;
        assertThat(cronTrigger.getCronExpression()).isEqualTo(DataPurgeScheduleConfig.CRON);
        assertThat(cronTrigger.getTimeZone().getID()).isEqualTo("UTC");
        assertThat(cronTrigger.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
    }
}
