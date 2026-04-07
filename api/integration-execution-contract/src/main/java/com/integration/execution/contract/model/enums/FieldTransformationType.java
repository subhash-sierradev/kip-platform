package com.integration.execution.contract.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum FieldTransformationType {

    // No transformation
    PASSTHROUGH("PASSTHROUGH", false),

    // String transformations
    UPPERCASE("UPPERCASE", false),
    LOWERCASE("LOWERCASE", false),
    STRING_TITLE_CASE("STRING_TITLE_CASE", false),
    TRIM("TRIM", false),

    // Type conversions
    TO_STRING("TO_STRING", false),
    TO_INTEGER("TO_INTEGER", false),
    TO_DOUBLE("TO_DOUBLE", false),
    TO_BOOLEAN("TO_BOOLEAN", false),
    TO_DATE("TO_DATE", true); // requires format configuration

    private final String code;
    private final boolean requiresConfiguration;

    public static FieldTransformationType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid transformation type: " + code));
    }

    public boolean isTypeConversion() {
        return this == TO_STRING
                || this == TO_INTEGER
                || this == TO_DOUBLE
                || this == TO_BOOLEAN
                || this == TO_DATE;
    }

    public boolean isStringTransformation() {
        return this == UPPERCASE
                || this == LOWERCASE
                || this == TRIM
                || this == STRING_TITLE_CASE;
    }
}
