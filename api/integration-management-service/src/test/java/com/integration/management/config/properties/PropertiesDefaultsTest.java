package com.integration.management.config.properties;

import com.integration.management.security.config.SecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfigurationProperties defaults")
class PropertiesDefaultsTest {

    @Test
    @DisplayName("JiraWebhookProperties should have default id length and retries")
    void jiraWebhookProperties_defaults() {
        JiraWebhookProperties props = new JiraWebhookProperties();

        assertThat(props.getIdLength()).isPositive();
        assertThat(props.getMaxRetries()).isPositive();
    }

    @Test
    @DisplayName("SecurityProperties should have stable defaults")
    void securityProperties_defaults() {
        SecurityProperties props = new SecurityProperties();

        assertThat(props.getJwt().getTenantIdClaim()).isEqualTo("tenant_id");
        assertThat(props.getJwt().getUserIdClaim()).isEqualTo("preferred_username");
        assertThat(props.getCsp().isEnabled()).isTrue();
        assertThat(props.getCsp().getDefaultSrc()).contains("self");
        assertThat(props.getWebhook().getClientRole()).isEqualTo("webhook_client");
    }
}
