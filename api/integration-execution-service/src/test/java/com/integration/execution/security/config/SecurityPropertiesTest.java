package com.integration.execution.security.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPropertiesTest {

    @Test
    void defaults_areInitialized() {
        SecurityProperties properties = new SecurityProperties();

        assertThat(properties.getJwt().getTenantIdClaim()).isEqualTo("tenant_id");
        assertThat(properties.getJwt().getUserIdClaim()).isEqualTo("preferred_username");
        assertThat(properties.getWebhook().getTenantId()).isEqualTo("GLOBAL");
        assertThat(properties.getWebhook().getUserId()).isEqualTo("system");
        assertThat(properties.getWebhook().getClientRole()).isEqualTo("webhook_client");
        assertThat(properties.getError().getMissingAuth()).isEqualTo("Missing or invalid Authorization header");
        assertThat(properties.getError().getMissingFields()).isEqualTo("Missing tenantId or userId in token");
        assertThat(properties.getError().getInvalidToken()).isEqualTo("Invalid JWT token");
    }

    @Test
    void nestedProperties_areMutable() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setTenantIdClaim("tenant");
        properties.getJwt().setUserIdClaim("user");
        properties.getWebhook().setTenantId("tenant-a");
        properties.getWebhook().setUserId("svc-user");
        properties.getWebhook().setClientRole("svc-role");
        properties.getError().setMissingAuth("no auth");
        properties.getError().setMissingFields("missing fields");
        properties.getError().setInvalidToken("bad token");

        assertThat(properties.getJwt().getTenantIdClaim()).isEqualTo("tenant");
        assertThat(properties.getJwt().getUserIdClaim()).isEqualTo("user");
        assertThat(properties.getWebhook().getTenantId()).isEqualTo("tenant-a");
        assertThat(properties.getWebhook().getUserId()).isEqualTo("svc-user");
        assertThat(properties.getWebhook().getClientRole()).isEqualTo("svc-role");
        assertThat(properties.getError().getMissingAuth()).isEqualTo("no auth");
        assertThat(properties.getError().getMissingFields()).isEqualTo("missing fields");
        assertThat(properties.getError().getInvalidToken()).isEqualTo("bad token");
    }

    @Test
    void rootNestedObjects_canBeReplaced() {
        SecurityProperties properties = new SecurityProperties();

        SecurityProperties.Jwt jwt = new SecurityProperties.Jwt();
        jwt.setTenantIdClaim("tenant");
        jwt.setUserIdClaim("user");

        SecurityProperties.Webhook webhook = new SecurityProperties.Webhook();
        webhook.setTenantId("tenant-x");
        webhook.setUserId("user-x");
        webhook.setClientRole("role-x");

        SecurityProperties.Error error = new SecurityProperties.Error();
        error.setMissingAuth("missing");
        error.setMissingFields("missing-fields");
        error.setInvalidToken("invalid");

        properties.setJwt(jwt);
        properties.setWebhook(webhook);
        properties.setError(error);

        assertThat(properties.getJwt().getTenantIdClaim()).isEqualTo("tenant");
        assertThat(properties.getJwt().getUserIdClaim()).isEqualTo("user");
        assertThat(properties.getWebhook().getTenantId()).isEqualTo("tenant-x");
        assertThat(properties.getWebhook().getUserId()).isEqualTo("user-x");
        assertThat(properties.getWebhook().getClientRole()).isEqualTo("role-x");
        assertThat(properties.getError().getMissingAuth()).isEqualTo("missing");
        assertThat(properties.getError().getMissingFields()).isEqualTo("missing-fields");
        assertThat(properties.getError().getInvalidToken()).isEqualTo("invalid");
    }
}
