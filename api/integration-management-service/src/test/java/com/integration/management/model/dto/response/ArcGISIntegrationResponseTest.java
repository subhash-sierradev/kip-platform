package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.rest.response.IntegrationScheduleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArcGISIntegrationResponse")
class ArcGISIntegrationResponseTest {

    @Test
    @DisplayName("builder maps nested schedule and audit fields")
    void builderMapsNestedScheduleAndAuditFields() {
        UUID id = UUID.randomUUID();
        IntegrationScheduleResponse schedule = IntegrationScheduleResponse.builder()
                .id(UUID.randomUUID())
                .executionDate(LocalDate.of(2026, 3, 6))
                .executionTime(LocalTime.of(10, 0))
                .frequencyPattern(FrequencyPattern.WEEKLY)
                .cronExpression("0 0 10 ? * MON")
                .build();

        ArcGISIntegrationResponse response = ArcGISIntegrationResponse.builder()
                .id(id)
                .name("ArcGIS Sync")
                .normalizedName("arcgis-sync")
                .description("Sync incidents")
                .itemType("DOCUMENT")
                .itemSubtype("incident")
                .itemSubtypeLabel("Incident")
                .dynamicDocumentType("typeA")
                .dynamicDocumentTypeLabel("Type A")
                .connectionId("conn-1")
                .tenantId("tenant-1")
                .isEnabled(true)
                .schedule(schedule)
                .createdBy("creator")
                .createdDate(Instant.parse("2026-03-01T00:00:00Z"))
                .lastModifiedBy("editor")
                .lastModifiedDate(Instant.parse("2026-03-05T00:00:00Z"))
                .nextRunAtUtc(Instant.parse("2026-03-07T10:00:00Z"))
                .version(3L)
                .build();

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getSchedule()).isNotNull();
        assertThat(response.getSchedule().getFrequencyPattern()).isEqualTo(FrequencyPattern.WEEKLY);
        assertThat(response.getConnectionId()).isEqualTo("conn-1");
        assertThat(response.getVersion()).isEqualTo(3L);
    }
}
