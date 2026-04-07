package com.integration.execution.util;

import com.integration.execution.contract.model.enums.ServiceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecretKeyUtilTest {

    @Test
    void generate_validInputs_returnsNormalizedSecretName() {
        UUID connectionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String secretName = SecretKeyUtil.generate(ServiceType.JIRA, "Tenant Alpha", connectionId);

        assertThat(secretName).isEqualTo("jira-tenant-alpha-123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    void generate_specialCharactersAndDiacritics_stripsUnsafeCharacters() {
        UUID connectionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String secretName = SecretKeyUtil.generate(ServiceType.ARCGIS, "Ténant_# 01", connectionId);

        assertThat(secretName).startsWith("arcgis-tenant-01-");
        assertThat(secretName).doesNotContain("_");
        assertThat(secretName).doesNotContain("#");
    }

    @Test
    void generate_longTenant_truncatesToMaxLength() {
        UUID connectionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String longTenant = "tenant-" + "x".repeat(120);

        String secretName = SecretKeyUtil.generate(ServiceType.JIRA, longTenant, connectionId);

        assertThat(secretName).hasSizeLessThanOrEqualTo(100);
        assertThat(secretName).endsWith(connectionId.toString());
    }

    @Test
    void generate_differentTenants_produceDifferentSecretNames() {
        UUID connectionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String tenantA = SecretKeyUtil.generate(ServiceType.JIRA, "tenant-a", connectionId);
        String tenantB = SecretKeyUtil.generate(ServiceType.JIRA, "tenant-b", connectionId);

        assertThat(tenantA).isNotEqualTo(tenantB);
    }
}
