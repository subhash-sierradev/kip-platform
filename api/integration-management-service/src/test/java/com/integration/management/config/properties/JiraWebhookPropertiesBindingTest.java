package com.integration.management.config.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JiraWebhookProperties binding")
class JiraWebhookPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "integration-platform.jira.webhook.id-length=12",
                    "integration-platform.jira.webhook.max-retries=5",
                    "integration-platform.jira.webhook.url-template=https://kaseware.sierradev.com/backend/api/webhooks/jira/execute/{webhookId}"
            );

    private final ApplicationContextRunner missingUrlTemplateRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "integration-platform.jira.webhook.id-length=12",
                    "integration-platform.jira.webhook.max-retries=5"
            );

    @Test
    @DisplayName("binds urlTemplate from configuration")
    void bindsUrlTemplateFromConfiguration() {
        contextRunner.run(context -> {
            JiraWebhookProperties props = context.getBean(JiraWebhookProperties.class);

            assertThat(props.getIdLength()).isEqualTo(12);
            assertThat(props.getMaxRetries()).isEqualTo(5);
            assertThat(props.getUrlTemplate())
                    .isEqualTo("https://kaseware.sierradev.com/backend/api/webhooks/jira/execute/{webhookId}");
        });
    }

    @Configuration
    @EnableConfigurationProperties(JiraWebhookProperties.class)
    static class TestConfig {

        @Bean
        static LocalValidatorFactoryBean configurationPropertiesValidator() {
            return new LocalValidatorFactoryBean();
        }
    }
}





