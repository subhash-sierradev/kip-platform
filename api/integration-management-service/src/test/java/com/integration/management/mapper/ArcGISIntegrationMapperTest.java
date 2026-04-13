package com.integration.management.mapper;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.JobExecutionStatus;
import com.integration.execution.contract.rest.response.CreationResponse;
import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.model.dto.request.ArcGISIntegrationCreateUpdateRequest;
import com.integration.management.model.dto.request.IntegrationScheduleRequest;
import com.integration.management.model.dto.response.ArcGISIntegrationResponse;
import com.integration.management.model.dto.response.ArcGISIntegrationSummaryResponse;
import com.integration.management.repository.projection.ArcGISIntegrationSummaryProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArcGISIntegrationMapper")
class ArcGISIntegrationMapperTest {

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Failed to set field '" + fieldName + "' on " + target.getClass(), e);
        }
    }

    @Test
    @DisplayName("joinList returns empty for null")
    void joinList_null_returnsEmpty() {
        ArcGISIntegrationMapper mapper = new ArcGISIntegrationMapperImpl();
        assertThat(mapper.joinList(null)).isEqualTo("");
        assertThat(mapper.joinList(List.of("a", "b"))).isEqualTo("a,b");
    }

    @Test
    @DisplayName("toEntity returns null for null request")
    void toEntity_null_returnsNull() {
        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", new IntegrationSchedulerMapperImpl());
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toEntity uses IntegrationSchedulerMapper and normalizes name")
    void toEntity_setsScheduleAndNormalizedName() {
        IntegrationSchedulerMapperImpl scheduler = new IntegrationSchedulerMapperImpl();

        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", scheduler);

        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("  Hello, World!!  ")
                .description("d")
                .itemType("DOCUMENT")
                .itemSubtype("Subtype")
                .dynamicDocumentType("Dyn")
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .schedule(IntegrationScheduleRequest.builder()
                        .executionDate(LocalDate.of(2026, 2, 6))
                        .executionTime(LocalTime.of(12, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .daySchedule(List.of("MONDAY"))
                        .monthSchedule(List.of("JANUARY"))
                        .isExecuteOnMonthEnd(false)
                        .cronExpression("0 0 12 * * ?")
                        .build())
                .build();

        ArcGISIntegration entity = mapper.toEntity(request);
        assertThat(entity.getNormalizedName()).isEqualTo("hello_world");
        assertThat(entity.getSchedule()).isNotNull();
        assertThat(entity.getSchedule().getExecutionTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("updateEntity creates schedule when missing and updates it")
    void updateEntity_createsScheduleWhenNull() {
        IntegrationSchedulerMapperImpl scheduler = new IntegrationSchedulerMapperImpl();

        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", scheduler);

        ArcGISIntegration entity = ArcGISIntegration.builder()
                .name("Old")
                .schedule(null)
                .build();

        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("New")
                .description("d")
                .itemType("DOCUMENT")
                .itemSubtype("Subtype")
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .schedule(IntegrationScheduleRequest.builder()
                        .executionDate(LocalDate.of(2026, 2, 6))
                        .executionTime(LocalTime.of(12, 0))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .cronExpression("0 0 12 * * ?")
                        .build())
                .build();

        mapper.updateEntity(request, entity);

        assertThat(entity.getName()).isEqualTo("New");
        assertThat(entity.getNormalizedName()).isEqualTo("new");
        assertThat(entity.getSchedule()).isNotNull();
        assertThat(entity.getSchedule().getFrequencyPattern()).isEqualTo(FrequencyPattern.DAILY);
    }

    @Test
    @DisplayName("updateEntity updates existing schedule when present")
    void updateEntity_updatesExistingScheduleWhenPresent() {
        IntegrationSchedulerMapperImpl scheduler = new IntegrationSchedulerMapperImpl();

        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", scheduler);

        ArcGISIntegration entity = ArcGISIntegration.builder()
            .name("Old")
            .schedule(IntegrationSchedule.builder()
                .frequencyPattern(FrequencyPattern.DAILY)
                .build())
            .build();

        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
            .name("New")
            .schedule(IntegrationScheduleRequest.builder()
                .frequencyPattern(FrequencyPattern.MONTHLY)
                .build())
            .build();

        mapper.updateEntity(request, entity);

        assertThat(entity.getSchedule()).isNotNull();
        assertThat(entity.getSchedule().getFrequencyPattern()).isEqualTo(FrequencyPattern.MONTHLY);
    }

    @Test
    @DisplayName("updateEntity clears schedule when request schedule is null")
    void updateEntity_nullSchedule_clears() {
        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();

        ArcGISIntegration entity = ArcGISIntegration.builder()
                .schedule(IntegrationSchedule.builder().build())
                .build();

        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("New")
                .description("d")
                .itemType("DOCUMENT")
                .itemSubtype("Subtype")
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .schedule(null)
                .build();

        mapper.updateEntity(request, entity);
        assertThat(entity.getSchedule()).isNull();
    }

    @Test
    @DisplayName("updateEntity returns early for null request")
    void updateEntity_nullRequest_returnsEarly() {
        IntegrationSchedulerMapperImpl scheduler = new IntegrationSchedulerMapperImpl();

        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", scheduler);

        ArcGISIntegration entity = ArcGISIntegration.builder()
                .name("Old")
                .normalizedName("old")
                .build();

        mapper.updateEntity(null, entity);

        assertThat(entity.getName()).isEqualTo("Old");
        assertThat(entity.getNormalizedName()).isEqualTo("old");
    }

    @Test
    @DisplayName("toResponse maps connectionId to string")
    void toResponse_connectionIdToString() {
        IntegrationSchedulerMapperImpl scheduler = new IntegrationSchedulerMapperImpl();

        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", scheduler);

        ArcGISIntegration entity = ArcGISIntegration.builder()
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .schedule(IntegrationSchedule.builder().build())
                .build();

        ArcGISIntegrationResponse response = mapper.toResponse(entity);
        assertThat(response.getConnectionId()).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("toResponse handles null inputs and null connectionId")
    void toResponse_nullBranches() {
        IntegrationSchedulerMapperImpl scheduler = new IntegrationSchedulerMapperImpl();

        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", scheduler);

        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toDetailsResponse(null)).isNull();

        ArcGISIntegration entity = ArcGISIntegration.builder()
                .connectionId(null)
                .schedule(null)
                .build();

        ArcGISIntegrationResponse response = mapper.toResponse(entity);
        assertThat(response.getConnectionId()).isNull();
        assertThat(response.getSchedule()).isNull();
    }

    @Test
    @DisplayName("toCreationResponse handles null entity and null id")
    void toCreationResponse_nullBranches() {
        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", new IntegrationSchedulerMapperImpl());

        assertThat(mapper.toCreationResponse(null)).isNull();

        ArcGISIntegration entityWithNullId = ArcGISIntegration.builder()
                .id(null)
                .name("n")
                .build();

        CreationResponse response = mapper.toCreationResponse(entityWithNullId);
        assertThat(response.getId()).isNull();
        assertThat(response.getName()).isEqualTo("n");
    }

    @Test
    @DisplayName("projectionToResponse returns null for null")
    void projectionToResponse_null_returnsNull() {
        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", new IntegrationSchedulerMapperImpl());
        assertThat(mapper.projectionToResponse(null)).isNull();
    }

    @Test
    @DisplayName("toDetailsResponse maps non-null entity fields")
    void toDetailsResponse_mapsNonNullEntityFields() {
        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", new IntegrationSchedulerMapperImpl());

        ArcGISIntegration entity = ArcGISIntegration.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000101"))
                .name("ArcGIS")
                .normalizedName("arcgis")
                .description("desc")
                .itemType("DOCUMENT")
                .itemSubtype("REPORT")
                .dynamicDocumentType("DYN")
                .connectionId(UUID.fromString("00000000-0000-0000-0000-000000000102"))
                .tenantId("tenant-1")
                .isEnabled(true)
                .schedule(IntegrationSchedule.builder()
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .executionTime(LocalTime.NOON)
                        .cronExpression("0 0 12 * * ?")
                        .build())
                .build();

        ArcGISIntegrationResponse response = mapper.toDetailsResponse(entity);

        assertThat(response.getConnectionId()).isEqualTo("00000000-0000-0000-0000-000000000102");
        assertThat(response.getItemSubtype()).isEqualTo("REPORT");
        assertThat(response.getSchedule()).isNotNull();
    }

    @Test
    @DisplayName("projectionToResponse maps projection fields")
    void projectionToResponse_mapsProjectionFields() {
        ArcGISIntegrationMapperImpl mapper = new ArcGISIntegrationMapperImpl();
        setField(mapper, "integrationSchedulerMapper", new IntegrationSchedulerMapperImpl());

        ArcGISIntegrationSummaryProjection projection = new ArcGISIntegrationSummaryProjection() {
            @Override
            public UUID getId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000103");
            }

            @Override
            public String getName() {
                return "ArcGIS";
            }

            @Override
            public String getItemType() {
                return "DOCUMENT";
            }

            @Override
            public String getItemSubtype() {
                return "REPORT";
            }

            @Override
            public String getDynamicDocumentType() {
                return "DYN";
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
                return LocalDate.of(2026, 4, 10);
            }

            @Override
            public LocalTime getExecutionTime() {
                return LocalTime.of(9, 15);
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
                return false;
            }

            @Override
            public String getCronExpression() {
                return "0 15 9 * * ?";
            }

            @Override
            public String getBusinessTimeZone() {
                return "UTC";
            }

            @Override
            public java.time.Instant getCreatedDate() {
                return java.time.Instant.parse("2026-04-01T00:00:00Z");
            }

            @Override
            public String getCreatedBy() {
                return "user";
            }

            @Override
            public java.time.Instant getLastModifiedDate() {
                return java.time.Instant.parse("2026-04-02T00:00:00Z");
            }

            @Override
            public String getLastModifiedBy() {
                return "user2";
            }

            @Override
            public Boolean getIsEnabled() {
                return true;
            }

            @Override
            public java.time.Instant getLastAttemptTimeUtc() {
                return java.time.Instant.parse("2026-04-03T00:00:00Z");
            }

            @Override
            public JobExecutionStatus getLastStatus() {
                return JobExecutionStatus.SUCCESS;
            }
        };

        ArcGISIntegrationSummaryResponse response = mapper.projectionToResponse(projection);

        assertThat(response.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000103"));
        assertThat(response.getFrequencyPattern()).isEqualTo("DAILY");
        assertThat(response.getLastStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
    }
}
