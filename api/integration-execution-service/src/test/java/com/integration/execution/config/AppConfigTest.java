package com.integration.execution.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final AppConfig appConfig = new AppConfig();

    @Test
    void taskExecutor_hasExpectedThreadPoolConfiguration() {
        Executor executor = appConfig.taskExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(5);
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(100);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("IntegrationAsync-");
    }

    @Test
    void objectMapper_isConfiguredForDatesAndUnknownProperties() {
        ObjectMapper mapper = appConfig.objectMapper();

        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
    }

    @Test
    void objectMapper_registersJavaTimeModule_serializesLocalDateTimeAsIsoString() throws Exception {
        ObjectMapper mapper = appConfig.objectMapper();
        LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 0);

        String json = mapper.writeValueAsString(dateTime);

        // JavaTimeModule registered: ISO-8601 string, not a numeric array
        assertThat(json).startsWith("\"2025-06-15");
        assertThat(json).doesNotContain("[2025,");
    }

    @Test
    void objectMapper_returnsNonNullInstance() {
        assertThat(appConfig.objectMapper()).isNotNull();
    }

    @Test
    void freemarkerConfiguration_isNotNull_andCanLoadMonitoringTemplate() throws Exception {
        freemarker.template.Configuration cfg = appConfig.freemarkerConfiguration();

        assertThat(cfg).isNotNull();
        assertThat(cfg.getDefaultEncoding()).isEqualTo("UTF-8");
        // Verify the template can be loaded from the classpath
        assertThat(cfg.getTemplate("monitoring_data_report.ftl")).isNotNull();
    }
}
