package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditLogResponse")
class AuditLogResponseTest {

    @Test
    @DisplayName("setters and getters map all audit fields")
    void settersAndGettersMapAllAuditFields() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-03-06T05:00:00Z");

        AuditLogResponse response = new AuditLogResponse();
        response.setId(id);
        response.setEntityType(EntityType.JIRA_WEBHOOK);
        response.setEntityId("wh-1");
        response.setEntityName("Webhook One");
        response.setAction("UPDATE");
        response.setResult(AuditResult.SUCCESS);
        response.setPerformedBy("admin");
        response.setTenantId("tenant-1");
        response.setClientIpAddress("203.0.113.8");
        response.setTenantName("Tenant One");
        response.setTimestamp(timestamp);
        response.setDetails("updated mappings");

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEntityType()).isEqualTo(EntityType.JIRA_WEBHOOK);
        assertThat(response.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(response.getClientIpAddress()).isEqualTo("203.0.113.8");
        assertThat(response.getTenantName()).isEqualTo("Tenant One");
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
    }
}
