package com.integration.management.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for ArcGIS Integration field mappings.
 * Ensures that the mandatory field mapping (id -> external_location_id) is present
 * and properly configured.
 *
 * @see ArcGISFieldMappingsValidator
 */
@Documented
@Constraint(validatedBy = ArcGISFieldMappingsValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidArcGISFieldMappings {
    String message() default "Field mappings must include mandatory mapping: id -> external_location_id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
