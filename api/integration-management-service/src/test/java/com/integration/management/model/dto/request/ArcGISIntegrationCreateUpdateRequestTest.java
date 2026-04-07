package com.integration.management.model.dto.request;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import com.integration.execution.contract.model.enums.FrequencyPattern;
import com.integration.execution.contract.model.enums.TimeCalculationMode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArcGISIntegrationCreateUpdateRequest")
class ArcGISIntegrationCreateUpdateRequestTest {

    @Test
    @DisplayName("builder initializes fieldMappings to empty list")
    void builderInitializesFieldMappingsToEmptyList() {
        ArcGISIntegrationCreateUpdateRequest request = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("Integration")
                .itemType("DOCUMENT")
                .itemSubtype("incident")
                .connectionId(UUID.randomUUID())
                .schedule(IntegrationScheduleRequest.builder()
                        .executionTime(LocalTime.of(10, 15))
                        .frequencyPattern(FrequencyPattern.DAILY)
                        .timeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY)
                        .build())
                .build();

        assertThat(request.getFieldMappings()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("bean validation accepts valid request and rejects invalid itemType")
    void beanValidationAcceptsValidRequestAndRejectsInvalidItemType() {
        List<IntegrationFieldMappingDto> requiredMappings = List.of(
                IntegrationFieldMappingDto.builder()
                        .sourceFieldPath("id")
                        .targetFieldPath("external_location_id")
                        .transformationType(FieldTransformationType.PASSTHROUGH)
                        .isMandatory(true)
                        .build());

        ArcGISIntegrationCreateUpdateRequest valid = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("Integration")
                .itemType("DOCUMENT")
                .itemSubtype("incident")
                .connectionId(UUID.randomUUID())
                .fieldMappings(requiredMappings)
                .schedule(IntegrationScheduleRequest.builder()
                        .executionTime(LocalTime.of(10, 15))
                        .frequencyPattern(FrequencyPattern.WEEKLY)
                        .timeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY)
                        .build())
                .build();

        ArcGISIntegrationCreateUpdateRequest invalid = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("Integration")
                .itemType("CASE")
                .itemSubtype("incident")
                .connectionId(UUID.randomUUID())
                .fieldMappings(requiredMappings)
                .schedule(IntegrationScheduleRequest.builder()
                        .executionTime(LocalTime.of(10, 15))
                        .frequencyPattern(FrequencyPattern.WEEKLY)
                        .timeCalculationMode(TimeCalculationMode.FIXED_DAY_BOUNDARY)
                        .build())
                .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(valid)).isEmpty();
            assertThat(validator.validate(invalid))
                    .anyMatch(v -> "itemType".equals(v.getPropertyPath().toString()));
        }
    }

    @Test
    @DisplayName("bean validation rejects missing required fields")
    void beanValidationRejectsMissingRequiredFields() {
        ArcGISIntegrationCreateUpdateRequest invalid = ArcGISIntegrationCreateUpdateRequest.builder()
                .name("Integration")
                .itemType("DOCUMENT")
                .itemSubtype("incident")
                .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(invalid))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("connectionId", "schedule", "fieldMappings");
        }
    }
}
