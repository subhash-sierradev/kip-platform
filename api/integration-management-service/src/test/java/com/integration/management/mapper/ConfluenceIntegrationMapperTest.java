package com.integration.management.mapper;

import com.integration.execution.contract.message.ConfluenceExecutionCommand;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.model.enums.TriggerType;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationJobExecution;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.entity.Language;
import com.integration.management.model.dto.request.ConfluenceIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.response.ConfluenceIntegrationResponse;
import com.integration.management.model.dto.response.ConfluenceIntegrationSummaryResponse;
import com.integration.management.repository.projection.ConfluenceIntegrationSummaryProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfluenceIntegrationMapper")
class ConfluenceIntegrationMapperTest {

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Failed to set field '" + fieldName + "' on " + target.getClass(), e);
        }
    }

    private static ConfluenceIntegrationMapperImpl mapper() {
        ConfluenceIntegrationMapperImpl mapper = new ConfluenceIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", new IntegrationSchedulerMapperImpl());
        return mapper;
    }

    @Test
    @DisplayName("toEntity maps request and normalizes name")
    void toEntity_mapsRequestAndNormalizesName() {
        ConfluenceIntegrationMapperImpl mapper = mapper();

        ConfluenceIntegrationCreateUpdateRequest request = ConfluenceIntegrationCreateUpdateRequest.builder()
                .name("  Daily Report!  ")
                .description("desc")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("REPORT")
                .dynamicDocumentType("DYN")
                .languageCodes(List.of("en", "ja"))
                .reportNameTemplate("report-${date}")
                .confluenceSpaceKey("OPS")
                .confluenceSpaceKeyFolderKey("ROOT")
                .includeTableOfContents(null)
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
                .build();

        ConfluenceIntegration entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getNormalizedName()).isEqualTo("daily_report");
        assertThat(entity.getIncludeTableOfContents()).isFalse();
        assertThat(entity.getConnectionId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000011"));
    }

    @Test
    @DisplayName("updateEntity updates fields and handles null request")
    void updateEntity_updatesFieldsAndHandlesNullRequest() {
        ConfluenceIntegrationMapperImpl mapper = mapper();

        ConfluenceIntegration entity = ConfluenceIntegration.builder()
                .name("Old")
                .normalizedName("old")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("OLD")
                .reportNameTemplate("old")
                .confluenceSpaceKey("OLD")
                .confluenceSpaceKeyFolderKey("ROOT")
                .includeTableOfContents(true)
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000012"))
                .build();

        ConfluenceIntegrationCreateUpdateRequest request = ConfluenceIntegrationCreateUpdateRequest.builder()
                .name(" New Name ")
                .description("new")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("NEW")
                .dynamicDocumentType("DYN")
                .reportNameTemplate("tmpl")
                .confluenceSpaceKey("OPS")
                .confluenceSpaceKeyFolderKey("SPACE")
                .includeTableOfContents(Boolean.TRUE)
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000013"))
                .build();

        mapper.updateEntity(request, entity);
        assertThat(entity.getName()).isEqualTo(" New Name ");
        assertThat(entity.getNormalizedName()).isEqualTo("new_name");
        assertThat(entity.getIncludeTableOfContents()).isTrue();

        mapper.updateEntity(null, entity);
        assertThat(entity.getName()).isEqualTo(" New Name ");
    }

    @Test
    @DisplayName("toDetailsResponse maps nested fields and language codes")
    void toDetailsResponse_mapsNestedFieldsAndLanguageCodes() {
        ConfluenceIntegrationMapperImpl mapper = mapper();

        IntegrationSchedule schedule = IntegrationSchedule.builder()
                .frequencyPattern(FrequencyPattern.DAILY)
                .executionTime(LocalTime.NOON)
                .businessTimeZone("America/Chicago")
                .cronExpression("0 0 12 * * ?")
                .build();

        ConfluenceIntegration entity = ConfluenceIntegration.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000014"))
                .name("Confluence")
                .normalizedName("confluence")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("REPORT")
                .dynamicDocumentType("DYN")
                .languages(List.of(
                        Language.builder().code("en").name("English").nativeName("English").build(),
                        Language.builder().code("ja").name("Japanese").nativeName("日本語").build()))
                .reportNameTemplate("daily")
                .confluenceSpaceKey("OPS")
                .confluenceSpaceKeyFolderKey("ROOT")
                .includeTableOfContents(Boolean.TRUE)
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000015"))
                .schedule(schedule)
                .isEnabled(Boolean.TRUE)
                .tenantId("tenant-1")
                .createdBy("user")
                .createdDate(Instant.parse("2026-02-17T00:00:00Z"))
                .lastModifiedBy("user2")
                .lastModifiedDate(Instant.parse("2026-02-18T00:00:00Z"))
                .version(3L)
                .build();

        ConfluenceIntegrationResponse response = mapper.toDetailsResponse(entity);

        assertThat(response.getConnectionId()).isEqualTo("00000000-0000-0000-0000-000000000015");
        assertThat(response.getLanguageCodes()).containsExactly("en", "ja");
        assertThat(response.getSchedule()).isNotNull();
        assertThat(response.getSchedule().getBusinessTimeZone()).isEqualTo("America/Chicago");
        assertThat(response.getCreatedBy()).isEqualTo("user");
    }

    @Test
    @DisplayName("toCreationResponse and toLanguageCodes handle null branches")
    void toCreationResponse_andToLanguageCodes_handleNullBranches() {
        ConfluenceIntegrationMapperImpl mapper = mapper();

        assertThat(mapper.toCreationResponse(null)).isNull();
        assertThat(mapper.toLanguageCodes(null)).isEmpty();

        ConfluenceIntegration entity = ConfluenceIntegration.builder().id(null).name("name").build();
        CreationResponse response = mapper.toCreationResponse(entity);
        assertThat(response.getId()).isNull();
        assertThat(response.getName()).isEqualTo("name");
    }

    @Test
    @DisplayName("projectionToSummaryResponse maps projection fields")
    void projectionToSummaryResponse_mapsProjectionFields() {
        ConfluenceIntegrationMapperImpl mapper = mapper();

        ConfluenceIntegrationSummaryProjection projection = new ConfluenceIntegrationSummaryProjection() {
            @Override
            public UUID getId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000016");
            }

            @Override
            public String getName() {
                return "Confluence";
            }

            @Override
            public String getDocumentItemType() {
                return "DOCUMENT";
            }

            @Override
            public String getDocumentItemSubtype() {
                return "REPORT";
            }

            @Override
            public String getDynamicDocumentType() {
                return "DYN";
            }

            @Override
            public String getConfluenceSpaceKey() {
                return "OPS";
            }

            @Override
            public String getFrequencyPattern() {
                return "DAILY";
            }

            @Override
            public Integer getDailyExecutionInterval() {
                return 24;
            }

            @Override
            public LocalDate getExecutionDate() {
                return LocalDate.of(2026, 2, 20);
            }

            @Override
            public LocalTime getExecutionTime() {
                return LocalTime.of(9, 30);
            }

            @Override
            public String getDaySchedule() {
                return "MONDAY";
            }

            @Override
            public String getMonthSchedule() {
                return "JANUARY";
            }

            @Override
            public Boolean getIsExecuteOnMonthEnd() {
                return Boolean.FALSE;
            }

            @Override
            public String getCronExpression() {
                return "0 30 9 * * ?";
            }

            @Override
            public Instant getCreatedDate() {
                return Instant.parse("2026-02-01T00:00:00Z");
            }

            @Override
            public String getCreatedBy() {
                return "user";
            }

            @Override
            public Instant getLastModifiedDate() {
                return Instant.parse("2026-02-10T00:00:00Z");
            }

            @Override
            public String getLastModifiedBy() {
                return "user2";
            }

            @Override
            public Boolean getIsEnabled() {
                return Boolean.TRUE;
            }

            @Override
            public Instant getLastAttemptTimeUtc() {
                return Instant.parse("2026-02-15T00:00:00Z");
            }

            @Override
            public JobExecutionStatus getLastStatus() {
                return JobExecutionStatus.SUCCESS;
            }
        };

        ConfluenceIntegrationSummaryResponse response = mapper.projectionToSummaryResponse(projection);

        assertThat(response.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000016"));
        assertThat(response.getItemType()).isEqualTo("DOCUMENT");
        assertThat(response.getLastStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
    }

    @Test
    @DisplayName("toExecutionCommand maps integration and job execution")
    void toExecutionCommand_mapsIntegrationAndJobExecution() {
        ConfluenceIntegrationMapperImpl mapper = mapper();

        ConfluenceIntegration integration = ConfluenceIntegration.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000017"))
                .name("Confluence")
                .documentItemType("DOCUMENT")
                .documentItemSubtype("REPORT")
                .dynamicDocumentType("DYN")
                .languages(List.of(Language.builder().code("en").name("English").nativeName("English").build()))
                .confluenceSpaceKey("OPS")
                .confluenceSpaceKeyFolderKey("ROOT")
                .reportNameTemplate("daily")
                .includeTableOfContents(null)
                .schedule(IntegrationSchedule.builder().businessTimeZone("UTC").build())
                .tenantId("tenant-2")
                .build();

        IntegrationJobExecution execution = IntegrationJobExecution.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000018"))
                .triggeredBy(TriggerType.API)
                .triggeredByUser("system")
                .windowStart(Instant.parse("2026-02-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-02-02T00:00:00Z"))
                .build();

        ConfluenceExecutionCommand command = mapper.toExecutionCommand(
                integration,
                execution,
                "secret-name",
                "tenant-2",
                TriggerType.MANUAL,
                "ignored",
                Instant.parse("2026-02-03T00:00:00Z"),
                Instant.parse("2026-02-04T00:00:00Z"));

        assertThat(command.getIntegrationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000017"));
        assertThat(command.getJobExecutionId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000018"));
        assertThat(command.getConnectionSecretName()).isEqualTo("secret-name");
        assertThat(command.getLanguageCodes()).containsExactly("en");
        assertThat(command.isIncludeTableOfContents()).isFalse();
        assertThat(command.getBusinessTimezone()).isEqualTo("UTC");
        assertThat(command.getTriggeredBy()).isEqualTo(TriggerType.API);
    }
}


