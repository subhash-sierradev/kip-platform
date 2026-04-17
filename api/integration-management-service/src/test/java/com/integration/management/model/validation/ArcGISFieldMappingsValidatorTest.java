package com.integration.management.model.validation;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArcGISFieldMappingsValidator")
class ArcGISFieldMappingsValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private ArcGISFieldMappingsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ArcGISFieldMappingsValidator();
        // default stub for all test cases that trigger constraint violation building
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    private IntegrationFieldMappingDto mandatoryMapping() {
        return IntegrationFieldMappingDto.builder()
                .sourceFieldPath("id")
                .targetFieldPath("external_location_id")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(Boolean.TRUE)
                .build();
    }

    private IntegrationFieldMappingDto nonMandatoryMapping() {
        return IntegrationFieldMappingDto.builder()
                .sourceFieldPath("title")
                .targetFieldPath("Name")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(Boolean.FALSE)
                .build();
    }

    @Nested
    @DisplayName("null/empty list")
    class NullEmptyList {

        @Test
        @DisplayName("null list returns false")
        void nullList_returnsFalse() {
            assertThat(validator.isValid(null, context)).isFalse();
        }

        @Test
        @DisplayName("empty list returns false")
        void emptyList_returnsFalse() {
            assertThat(validator.isValid(List.of(), context)).isFalse();
        }
    }

    @Nested
    @DisplayName("mandatory mapping present")
    class MandatoryMappingPresent {

        @Test
        @DisplayName("list with mandatory id->external_location_id returns true")
        void withMandatoryMapping_returnsTrue() {
            boolean result = validator.isValid(
                    List.of(mandatoryMapping(), nonMandatoryMapping()), context);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("case-insensitive source and target field matching returns true")
        void caseInsensitiveMatch_returnsTrue() {
            IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                    .sourceFieldPath("ID")
                    .targetFieldPath("EXTERNAL_LOCATION_ID")
                    .transformationType(FieldTransformationType.PASSTHROUGH)
                    .isMandatory(Boolean.TRUE)
                    .build();
            assertThat(validator.isValid(List.of(mapping), context)).isTrue();
        }
    }

    @Nested
    @DisplayName("mandatory mapping missing")
    class MandatoryMappingMissing {

        @Test
        @DisplayName("list without id->external_location_id returns false")
        void withoutMandatoryMapping_returnsFalse() {
            assertThat(validator.isValid(List.of(nonMandatoryMapping()), context)).isFalse();
        }

        @Test
        @DisplayName("mapping with correct paths but isMandatory=false returns false")
        void correctPaths_butMandatoryFalse_returnsFalse() {
            IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                    .sourceFieldPath("id")
                    .targetFieldPath("external_location_id")
                    .transformationType(FieldTransformationType.PASSTHROUGH)
                    .isMandatory(Boolean.FALSE)
                    .build();
            assertThat(validator.isValid(List.of(mapping), context)).isFalse();
        }

        @Test
        @DisplayName("mapping with correct paths but isMandatory=null returns false")
        void correctPaths_butMandatoryNull_returnsFalse() {
            IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                    .sourceFieldPath("id")
                    .targetFieldPath("external_location_id")
                    .transformationType(FieldTransformationType.PASSTHROUGH)
                    .isMandatory(null)
                    .build();
            assertThat(validator.isValid(List.of(mapping), context)).isFalse();
        }

        @Test
        @DisplayName("mapping with correct source but wrong target returns false")
        void correctSource_wrongTarget_returnsFalse() {
            IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                    .sourceFieldPath("id")
                    .targetFieldPath("some_other_field")
                    .transformationType(FieldTransformationType.PASSTHROUGH)
                    .isMandatory(Boolean.TRUE)
                    .build();
            assertThat(validator.isValid(List.of(mapping), context)).isFalse();
        }

        @Test
        @DisplayName("mapping with wrong source but correct target returns false")
        void wrongSource_correctTarget_returnsFalse() {
            IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                    .sourceFieldPath("other_field")
                    .targetFieldPath("external_location_id")
                    .transformationType(FieldTransformationType.PASSTHROUGH)
                    .isMandatory(Boolean.TRUE)
                    .build();
            assertThat(validator.isValid(List.of(mapping), context)).isFalse();
        }
    }
}

