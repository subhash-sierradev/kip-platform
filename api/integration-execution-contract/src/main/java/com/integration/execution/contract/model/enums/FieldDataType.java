package com.integration.execution.contract.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum FieldDataType {
    STRING("STRING", String.class),
    INTEGER("INTEGER", Integer.class),
    LONG("LONG", Long.class),
    DOUBLE("DOUBLE", Double.class),
    BOOLEAN("BOOLEAN", Boolean.class),
    DATE("DATE", Date.class),
    DATETIME("DATETIME", Instant.class), // Always use Instant for datetime
    ARRAY("ARRAY", List.class),
    OBJECT("OBJECT", Map.class),
    JSON("JSON", Object.class);

    private final String code;
    private final Class<?> javaType;
}
