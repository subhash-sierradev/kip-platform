package com.integration.management.constants;

/**
 * Reflection-related constants for Integration Management Service.
 * Includes method naming conventions used in reflection operations.
 */
public final class ReflectionConstants {

    // Method Prefixes for Reflection
    public static final String METHOD_PREFIX_GET = "getSecret";
    public static final String METHOD_PREFIX_IS = "is";

    private ReflectionConstants() {
        throw new IllegalStateException("ReflectionConstants is a utility class and cannot be instantiated");
    }
}
