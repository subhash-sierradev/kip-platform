package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.enums.JobExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArcGISIntegrationSummaryResponse")
class ArcGISIntegrationSummaryResponseTest {

    @Test
    @DisplayName("builder populates integration summary and scheduling fields")
    void builderPopulatesIntegrationSummaryAndSchedulingFields() {
        UUID id = UUID.randomUUID();

        ArcGISIntegrationSummaryResponse response = ArcGISIntegrationSummaryResponse.builder()
                .id(id)
                .name("ArcGIS Sync")
                .itemType("DOCUMENT")
                .itemSubtype("incident")
                .itemSubtypeLabel("Incident")
                .dynamicDocumentType("typeA")
                .dynamicDocumentTypeLabel("Type A")
                .frequencyPattern("DAILY")
                .dailyExecutionInterval(1)
                .executionDate(LocalDate.of(2026, 3, 6))
                .executionTime(LocalTime.of(9, 30))
                .daySchedule("MONDAY,FRIDAY")
                .monthSchedule("1,15")
                .isExecuteOnMonthEnd(true)
                .cronExpression("0 30 9 * * ?")
                .businessTimeZone("America/New_York")
                .createdDate(Instant.parse("2026-03-01T00:00:00Z"))
                .createdBy("creator")
                .lastModifiedDate(Instant.parse("2026-03-05T00:00:00Z"))
                .lastModifiedBy("editor")
                .isEnabled(true)
                .lastAttemptTimeUtc(Instant.parse("2026-03-06T09:30:00Z"))
                .lastStatus(JobExecutionStatus.SUCCESS)
                .nextRunAtUtc(Instant.parse("2026-03-07T09:30:00Z"))
                .build();

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("ArcGIS Sync");
        assertThat(response.getFrequencyPattern()).isEqualTo("DAILY");
        assertThat(response.getLastStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(response.getIsEnabled()).isTrue();
        assertThat(response.getBusinessTimeZone()).isEqualTo("America/New_York");
    }
}
