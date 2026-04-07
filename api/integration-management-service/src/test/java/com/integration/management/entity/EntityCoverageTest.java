package com.integration.management.entity;

import com.integration.execution.contract.model.enums.AuditActivity;
import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.ConfigValueType;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.FetchMode;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.JiraDataType;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.model.enums.TriggerStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.management.entity.base.UuidBaseEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityCoverageTest {

    static class TestUuidEntity extends UuidBaseEntity {
    }

    @Test
    void uuidBaseEntity_generateId_setsIdAndDefaultsIsDeleted() {
        TestUuidEntity entity = new TestUuidEntity();
        entity.setId(null);
        entity.setIsDeleted(null);

        entity.generateId();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getIsDeleted()).isFalse();

        UUID existingId = UUID.randomUUID();
        entity.setId(existingId);
        entity.setIsDeleted(Boolean.TRUE);

        entity.generateId();

        assertThat(entity.getId()).isEqualTo(existingId);
        assertThat(entity.getIsDeleted()).isTrue();
    }

    @Test
    void jiraWebhook_touch_updatesLastModifiedDate() {
        JiraWebhook webhook = JiraWebhook.builder()
                .id("w1")
                .tenantId("tenant")
                .createdBy("creator")
                .lastModifiedBy("editor")
                .name("Webhook")
                .webhookUrl("https://host/webhook")
                .connectionId(UUID.randomUUID())
                .samplePayload("{}")
                .build();

        Instant before = Instant.parse("2026-01-01T00:00:00Z");
        webhook.setLastModifiedDate(before);

        webhook.touch();

        assertThat(webhook.getLastModifiedDate()).isAfter(before);
    }

    @Test
    void entity_buildersAndDefaults_smokeTest() {
        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .executionDate(LocalDate.of(2026, 3, 5))
                .executionTime(LocalTime.of(9, 0))
                .frequencyPattern(FrequencyPattern.DAILY)
                .dailyExecutionInterval(24)
                .daySchedule(List.of("MONDAY"))
                .monthSchedule(List.of("JANUARY"))
                .isExecuteOnMonthEnd(Boolean.TRUE)
                .cronExpression("0 0 9 * * ?")
                .build();
        assertThat(schedule.getExecutionTime()).isEqualTo(LocalTime.of(9, 0));

        ArcGISIntegration arcgis = ArcGISIntegration.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .createdBy("creator")
                .lastModifiedBy("editor")
                .name("ArcGIS")
                .normalizedName("arcgis")
                .itemType("Feature")
                .itemSubtype("Subtype")
                .connectionId(UUID.randomUUID())
                .schedule(schedule)
                .build();
        assertThat(arcgis.getIsEnabled()).isTrue();

        IntegrationConnection connection = IntegrationConnection.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .createdBy("creator")
                .lastModifiedBy("editor")
                .name("Conn")
                .secretName("secret")
                .serviceType(ServiceType.ARCGIS)
                .connectionHashKey("hash")
                .build();
        assertThat(connection.getFetchMode()).isEqualTo(FetchMode.GET);
        assertThat(connection.getIsDeleted()).isFalse();

        CredentialType credentialType = CredentialType.builder()
                .credentialAuthType(CredentialAuthType.BASIC_AUTH)
                .displayName("Basic")
                .build();
        assertThat(credentialType.getIsEnabled()).isTrue();
        assertThat(credentialType.getRequiredFields()).isNotNull().isEmpty();

        IntegrationFieldMapping mapping = IntegrationFieldMapping.builder()
                .sourceFieldPath("src")
                .targetFieldPath("tgt")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .transformationConfig(Map.of("k", "v"))
                .integrationId(UUID.randomUUID())
                .displayOrder(1)
                .build();
        assertThat(mapping.getIsMandatory()).isFalse();

        JiraWebhookEvent event = JiraWebhookEvent.builder()
                .webhookId("wh")
                .tenantId("tenant")
                .triggeredBy("user")
                .build();
        assertThat(event.getStatus()).isEqualTo(TriggerStatus.PENDING);
        assertThat(event.getRetryAttempt()).isEqualTo(0);

        JiraFieldMapping fieldMapping = JiraFieldMapping.builder()
                .jiraWebhook(JiraWebhook.builder()
                        .id("wh")
                        .tenantId("tenant")
                        .createdBy("creator")
                        .lastModifiedBy("editor")
                        .name("Webhook")
                        .webhookUrl("https://host/webhook")
                        .connectionId(UUID.randomUUID())
                        .samplePayload("{}")
                        .build())
                .jiraFieldId("fid")
                .jiraFieldName("Field")
                .displayLabel("Label")
                .dataType(JiraDataType.STRING)
                .template("tmpl")
                .metadata(Map.of("m", "v"))
                .build();
        assertThat(fieldMapping.getRequired()).isFalse();

        SiteConfig config = SiteConfig.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant")
                .createdBy("creator")
                .lastModifiedBy("editor")
                .configKey("key")
                .configValue("value")
                .type(ConfigValueType.STRING)
                .description("desc")
                .build();
        assertThat(config.getConfigKey()).isEqualTo("key");

        AuditLog auditLog = AuditLog.builder()
                .entityType(com.integration.execution.contract.model.enums.EntityType.INTEGRATION_CONNECTION)
                .entityId("id")
                .entityName("name")
                .action(AuditActivity.CREATE)
                .result(AuditResult.SUCCESS)
                .performedBy("user")
                .tenantId("tenant")
                .metadata(Map.of("a", "b"))
                .build();
        assertThat(auditLog.toBuilder().build()).isEqualTo(auditLog);

        TenantProfile tenantProfile = TenantProfile.builder()
                .tenantId("tenant")
                .tenantName("Tenant Name")
                .build();
        assertThat(tenantProfile.getTenantName()).isEqualTo("Tenant Name");

        UserProfile userProfile = UserProfile.builder()
                .keycloakUserId("kc")
                .email("a@b.com")
                .tenantId("tenant")
                .isTenantAdmin(true)
                .build();
        assertThat(userProfile.isTenantAdmin()).isTrue();

        IntegrationJobExecution exec = IntegrationJobExecution.builder()
                .scheduleId(UUID.randomUUID())
                .triggeredBy(TriggerType.SCHEDULER)
                .status(JobExecutionStatus.RUNNING)
                .startedAt(Instant.parse("2026-03-05T00:00:00Z"))
                .build();
        assertThat(exec.getAddedRecords()).isZero();
        assertThat(exec.getUpdatedRecords()).isZero();
        assertThat(exec.getFailedRecords()).isZero();
        assertThat(exec.getTotalRecords()).isZero();
    }
}
