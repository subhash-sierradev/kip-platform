package com.integration.management.model.validation;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * Validator implementation for {@link ValidArcGISFieldMappings} annotation.
 * Validates that ArcGIS Integration field mappings include the mandatory
 * id -> external_location_id mapping with mandatory flag set to true.
 *
 * <p>This validation ensures data integrity for ArcGIS integrations by
 * enforcing that every integration has the required location identifier mapping.
 */
public class ArcGISFieldMappingsValidator implements ConstraintValidator<ValidArcGISFieldMappings,
        List<IntegrationFieldMappingDto>> {

    private static final String MANDATORY_SOURCE_FIELD = "id";
    private static final String MANDATORY_TARGET_FIELD = "external_location_id";

    @Override
    public boolean isValid(List<IntegrationFieldMappingDto> fieldMappings, ConstraintValidatorContext context) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "At least one field mapping is required, including mandatory mapping: "
                            + MANDATORY_SOURCE_FIELD + " -> " + MANDATORY_TARGET_FIELD
            ).addConstraintViolation();
            return false;
        }

        // Check for mandatory id -> external_location_id mapping
        boolean hasMandatoryMapping = fieldMappings.stream()
                .anyMatch(mapping ->
                        MANDATORY_SOURCE_FIELD.equalsIgnoreCase(mapping.getSourceFieldPath())
                                && MANDATORY_TARGET_FIELD.equalsIgnoreCase(mapping.getTargetFieldPath())
                                && Boolean.TRUE.equals(mapping.getIsMandatory())
                );

        if (!hasMandatoryMapping) {
            context.disableDefaultConstraintViolation();

            String message =
                    "Mandatory field mapping required: "
                            + MANDATORY_SOURCE_FIELD + " (source) -> "
                            + MANDATORY_TARGET_FIELD + " (target) "
                            + "with mandatory flag set to true. "
                            + "This mapping is required for proper ArcGIS location identification.";

            context.buildConstraintViolationWithTemplate(message)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
