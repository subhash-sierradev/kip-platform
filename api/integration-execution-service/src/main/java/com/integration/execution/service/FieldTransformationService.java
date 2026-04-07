package com.integration.execution.service;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FieldTransformationService {

    public Object applyTransformation(
            final Object sourceValue,
            final FieldTransformationType transformationType,
            final Object defaultValue,
            final Map<String, Object> transformationConfig) {

        if (sourceValue == null) {
            return defaultValue;
        }

        if (transformationType == null || transformationType == FieldTransformationType.PASSTHROUGH) {
            return sourceValue;
        }

        try {
            return switch (transformationType) {
                case TO_STRING -> convertToString(sourceValue);
                case TO_INTEGER -> convertToInteger(sourceValue);
                case TO_DOUBLE -> convertToDouble(sourceValue);
                case TO_BOOLEAN -> convertToBoolean(sourceValue);
                case UPPERCASE -> convertToUppercase(sourceValue);
                case LOWERCASE -> convertToLowercase(sourceValue);
                case TRIM -> convertToTrim(sourceValue);
                case STRING_TITLE_CASE -> convertToTitleCase(sourceValue);
                case TO_DATE -> convertToInstant(sourceValue, transformationConfig);
                default -> throw new IllegalStateException("Unexpected value: " + transformationType);
            };
        } catch (Exception e) {
            log.error("Error applying transformation {} to value: {}", transformationType, sourceValue, e);
            return defaultValue;
        }
    }

    public Object applyTransformation(
            final Object sourceValue,
            final IntegrationFieldMappingDto mapping) {
        return applyTransformation(
                sourceValue,
                mapping.getTransformationType(),
                mapping.getDefaultValue(),
                mapping.getTransformationConfig()
        );
    }

    public String convertToString(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        return sourceValue.toString();
    }

    public Integer convertToInteger(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        if (sourceValue instanceof Number) {
            return ((Number) sourceValue).intValue();
        }
        try {
            return Integer.parseInt(sourceValue.toString().trim());
        } catch (NumberFormatException e) {
            log.error("Failed to convert value '{}' to Integer", sourceValue, e);
            throw new IllegalArgumentException(
                    "Cannot convert '" + sourceValue + "' to Integer", e);
        }
    }

    public Double convertToDouble(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        if (sourceValue instanceof Number) {
            return ((Number) sourceValue).doubleValue();
        }
        try {
            return Double.parseDouble(sourceValue.toString().trim());
        } catch (NumberFormatException e) {
            log.error("Failed to convert value '{}' to Double", sourceValue, e);
            throw new IllegalArgumentException(
                    "Cannot convert '" + sourceValue + "' to Double", e);
        }
    }

    public Boolean convertToBoolean(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        if (sourceValue instanceof Boolean) {
            return (Boolean) sourceValue;
        }
        String strValue = sourceValue.toString().trim().toLowerCase();
        return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue);
    }

    public String convertToUppercase(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        return sourceValue.toString().toUpperCase();
    }

    public String convertToLowercase(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        return sourceValue.toString().toLowerCase();
    }

    public String convertToTrim(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        return sourceValue.toString().trim();
    }

    public String convertToTitleCase(final Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }

        String str = sourceValue.toString().toLowerCase();
        if (str.isEmpty()) {
            return str;
        }

        return Arrays.stream(str.split("\\s+"))
                .map(word -> {
                    if (word.isEmpty()) {
                        return word;
                    }
                    return word.substring(0, 1).toUpperCase()
                            + (word.length() > 1 ? word.substring(1) : "");
                })
                .collect(Collectors.joining(" "));
    }

    public Instant convertToInstant(
            final Object sourceValue,
            final Map<String, Object> transformationConfig) {

        if (sourceValue == null) {
            return null;
        }

        if (sourceValue instanceof Instant) {
            return (Instant) sourceValue;
        }

        String dateStr = sourceValue.toString().trim();
        String format = transformationConfig != null
                ? (String) transformationConfig.get("format")
                : null;

        try {
            if (format != null && !format.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(dateStr, formatter)
                        .atZone(ZoneOffset.UTC)
                        .toInstant();
            } else {
                return Instant.parse(dateStr);
            }
        } catch (Exception e) {
            log.error("Failed to convert '{}' to Instant with format '{}'", dateStr, format, e);
            throw new IllegalArgumentException(
                    "Cannot convert '" + dateStr + "' to date with format: " + format, e);
        }
    }

    public Object convertToEpochMillis(final Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case Instant instantValue -> {
                return instantValue.toEpochMilli();
            }
            case String strValue -> {
                try {
                    Instant parsedInstant = Instant.parse(strValue);
                    return parsedInstant.toEpochMilli();
                } catch (Exception e) {
                    log.warn("Failed to parse date string to Instant: {}", strValue);
                    return value;
                }
            }
            case Number numValue -> {
                long numLong = numValue.longValue();
                if (numLong < 32503680000L) {
                    return numLong * 1000;
                }
                return numLong;
            }
            default -> {
            }
        }

        return value;
    }

    public boolean canApplyTransformation(
            final Object value,
            final FieldTransformationType transformationType) {

        if (value == null || transformationType == null) {
            return false;
        }

        if (transformationType == FieldTransformationType.PASSTHROUGH) {
            return true;
        }

        if (transformationType.isTypeConversion()) {
            return switch (transformationType) {
                case TO_INTEGER, TO_DOUBLE -> value instanceof Number
                        || isNumericString(value.toString());
                case TO_BOOLEAN -> value instanceof Boolean
                        || isBooleanString(value.toString());
                case TO_DATE -> value instanceof Instant
                        || value instanceof String;
                default -> true;
            };
        }

        if (transformationType.isStringTransformation()) {
            return value instanceof CharSequence;
        }

        return true;
    }

    private boolean isNumericString(final String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBooleanString(final String value) {
        String normalized = value.trim().toLowerCase();
        return "true".equals(normalized) || "false".equals(normalized)
                || "1".equals(normalized) || "0".equals(normalized)
                || "yes".equals(normalized) || "no".equals(normalized);
    }
}
