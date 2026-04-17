package com.integration.execution.service;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldTransformationServiceTest {

    private FieldTransformationService service;

    @BeforeEach
    void setUp() {
        service = new FieldTransformationService();
    }

    @Test
    void applyTransformation_nullSourceValue_returnsDefaultValue() {
        Object result = service.applyTransformation(null, FieldTransformationType.TO_STRING, "default", null);

        assertThat(result).isEqualTo("default");
    }

    @Test
    void applyTransformation_passthrough_returnsSourceValue() {
        Object result = service.applyTransformation("abc", FieldTransformationType.PASSTHROUGH, "default", null);

        assertThat(result).isEqualTo("abc");
    }

    @Test
    void applyTransformation_nullTransformationType_returnsSourceValue() {
        Object result = service.applyTransformation("abc", null, "default", null);

        assertThat(result).isEqualTo("abc");
    }

    @Test
    void applyTransformation_invalidInteger_returnsDefaultValue() {
        Object result = service.applyTransformation("abc", FieldTransformationType.TO_INTEGER, 99, null);

        assertThat(result).isEqualTo(99);
    }

    @Test
    void applyTransformation_toDateWithFormat_returnsInstant() {
        Object result = service.applyTransformation(
                "2026-02-27 10:15:30",
                FieldTransformationType.TO_DATE,
                null,
                Map.of("format", "yyyy-MM-dd HH:mm:ss")
        );

        assertThat(result).isInstanceOf(Instant.class);
    }

    @Test
    void applyTransformation_invalidDateWithFormat_returnsDefaultValue() {
        Object result = service.applyTransformation(
                "bad-date",
                FieldTransformationType.TO_DATE,
                Instant.EPOCH,
                Map.of("format", "yyyy-MM-dd")
        );

        assertThat(result).isEqualTo(Instant.EPOCH);
    }

    @Test
    void applyTransformation_mappingOverload_appliesConfiguredTransformation() {
        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .transformationType(FieldTransformationType.UPPERCASE)
                .defaultValue("default")
                .build();

        Object result = service.applyTransformation("jira", mapping);

        assertThat(result).isEqualTo("JIRA");
    }

    @Test
    void convertToEpochMillis_withInstant_returnsEpochMillis() {
        Instant instant = Instant.parse("2026-02-27T00:00:00Z");

        Object result = service.convertToEpochMillis(instant);

        assertThat(result).isEqualTo(instant.toEpochMilli());
    }

    @Test
    void convertToEpochMillis_secondsNumber_convertsToMillis() {
        Object result = service.convertToEpochMillis(12345L);

        assertThat(result).isEqualTo(12_345_000L);
    }

    @Test
    void convertToEpochMillis_invalidDateString_returnsOriginalValue() {
        Object result = service.convertToEpochMillis("not-a-date");

        assertThat(result).isEqualTo("not-a-date");
    }

    @Test
    void convertToEpochMillis_withNull_returnsNull() {
        assertThat(service.convertToEpochMillis(null)).isNull();
    }

    @Test
    void convertToEpochMillis_withIsoDateString_returnsEpochMillis() {
        String date = "2026-02-27T00:00:00Z";

        Object result = service.convertToEpochMillis(date);

        assertThat(result).isEqualTo(Instant.parse(date).toEpochMilli());
    }

    @Test
    void convertToEpochMillis_millisNumber_returnsSameNumber() {
        Object result = service.convertToEpochMillis(32_503_680_001L);

        assertThat(result).isEqualTo(32_503_680_001L);
    }

    @Test
    void convertToEpochMillis_unhandledType_returnsOriginalValue() {
        List<String> value = List.of("a");

        Object result = service.convertToEpochMillis(value);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void canApplyTransformation_numericStringToDouble_returnsTrue() {
        boolean result = service.canApplyTransformation("12.34", FieldTransformationType.TO_DOUBLE);

        assertThat(result).isTrue();
    }

    @Test
    void canApplyTransformation_nonBooleanStringToBoolean_returnsFalse() {
        boolean result = service.canApplyTransformation("maybe", FieldTransformationType.TO_BOOLEAN);

        assertThat(result).isFalse();
    }

    @Test
    void canApplyTransformation_stringTransformationWithNonString_returnsFalse() {
        boolean result = service.canApplyTransformation(100, FieldTransformationType.UPPERCASE);

        assertThat(result).isFalse();
    }

    @Test
    void canApplyTransformation_nullValueOrType_returnsFalse() {
        assertThat(service.canApplyTransformation(null, FieldTransformationType.TO_STRING)).isFalse();
        assertThat(service.canApplyTransformation("x", null)).isFalse();
    }

    @Test
    void canApplyTransformation_passthrough_returnsTrue() {
        assertThat(service.canApplyTransformation(100, FieldTransformationType.PASSTHROUGH)).isTrue();
    }

    @Test
    void canApplyTransformation_toInteger_withNonNumericString_returnsFalse() {
        boolean result = service.canApplyTransformation("not-a-number", FieldTransformationType.TO_INTEGER);

        assertThat(result).isFalse();
    }

    @Test
    void canApplyTransformation_toDate_withNonStringAndNonInstant_returnsFalse() {
        boolean result = service.canApplyTransformation(123, FieldTransformationType.TO_DATE);

        assertThat(result).isFalse();
    }

    @Test
    void directConverters_coverAdditionalBranches() {
        assertThat(service.convertToInteger(42.9)).isEqualTo(42);
        assertThat(service.convertToDouble(42)).isEqualTo(42.0);
        assertThat(service.convertToBoolean("No")).isFalse();
        assertThat(service.convertToUppercase("jira")).isEqualTo("JIRA");
        assertThat(service.convertToLowercase("JIRA")).isEqualTo("jira");
        assertThat(service.convertToTrim("  jira  ")).isEqualTo("jira");
        assertThat(service.convertToTitleCase("HELLO   WORLD")).isEqualTo("Hello World");
        assertThat(service.convertToString(123)).isEqualTo("123");
    }

    @Test
    void convertToInstant_withoutFormat_parsesIsoInstant() {
        Instant result = service.convertToInstant("2026-02-27T12:00:00Z", null);

        assertThat(result).isEqualTo(Instant.parse("2026-02-27T12:00:00Z"));
    }

    @Test
    void convertToInstant_withInstantInput_returnsSameInstant() {
        Instant instant = Instant.now();

        Instant result = service.convertToInstant(instant, null);

        assertThat(result).isEqualTo(instant);
    }

    @Test
    void convertToInstant_invalidInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.convertToInstant("bad-date", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot convert");
    }

    @Test
    void directConverters_nullAndInvalidBranches() {
        assertThat(service.convertToString(null)).isNull();
        assertThat(service.convertToInteger(null)).isNull();
        assertThat(service.convertToDouble(null)).isNull();
        assertThat(service.convertToBoolean(null)).isNull();
        assertThat(service.convertToTitleCase("")).isEmpty();

        assertThatThrownBy(() -> service.convertToInteger("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot convert 'abc' to Integer");
        assertThatThrownBy(() -> service.convertToDouble("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot convert 'abc' to Double");
    }

    @Test
    void convertToBoolean_andInstantAdditionalBranches() {
        assertThat(service.convertToBoolean(Boolean.TRUE)).isTrue();
        assertThat(service.convertToBoolean("1")).isTrue();
        assertThat(service.convertToBoolean("yes")).isTrue();
        assertThat(service.convertToBoolean("0")).isFalse();

        Instant instant = Instant.parse("2026-02-27T10:00:00Z");
        assertThat(service.convertToInstant(instant, Map.of("format", "yyyy-MM-dd"))).isEqualTo(instant);
        assertThat(service.convertToInstant("2026-02-27T10:00:00Z", Map.of("format", "")))
                .isEqualTo(instant);
    }

    @Test
    void canApplyTransformation_additionalDecisionPaths() {
        assertThat(service.canApplyTransformation(15, FieldTransformationType.TO_INTEGER)).isTrue();
        assertThat(service.canApplyTransformation(Boolean.TRUE, FieldTransformationType.TO_BOOLEAN)).isTrue();
        assertThat(service.canApplyTransformation(Instant.now(), FieldTransformationType.TO_DATE)).isTrue();
        assertThat(service.canApplyTransformation("abc", FieldTransformationType.UPPERCASE)).isTrue();
        assertThat(service.canApplyTransformation("abc", FieldTransformationType.TO_STRING)).isTrue();
    }

    @Test
    void applyTransformation_coversAllEnumBranches() {
        assertThat(service.applyTransformation("123", FieldTransformationType.TO_STRING, null, null)).isEqualTo("123");
        assertThat(service.applyTransformation("123", FieldTransformationType.TO_INTEGER, null, null)).isEqualTo(123);
        assertThat(service.applyTransformation("123.5", FieldTransformationType.TO_DOUBLE, null, null)).isEqualTo(123.5d);
        assertThat(service.applyTransformation("true", FieldTransformationType.TO_BOOLEAN, null, null)).isEqualTo(true);
        assertThat(service.applyTransformation("jira", FieldTransformationType.UPPERCASE, null, null)).isEqualTo("JIRA");
        assertThat(service.applyTransformation("JIRA", FieldTransformationType.LOWERCASE, null, null)).isEqualTo("jira");
        assertThat(service.applyTransformation("  jira  ", FieldTransformationType.TRIM, null, null)).isEqualTo("jira");
        assertThat(service.applyTransformation("hello world", FieldTransformationType.STRING_TITLE_CASE, null, null))
                .isEqualTo("Hello World");
    }

    @Test
    void canApplyTransformation_toBooleanSupportsAllBooleanStrings() {
        assertThat(service.canApplyTransformation("false", FieldTransformationType.TO_BOOLEAN)).isTrue();
        assertThat(service.canApplyTransformation("0", FieldTransformationType.TO_BOOLEAN)).isTrue();
        assertThat(service.canApplyTransformation("no", FieldTransformationType.TO_BOOLEAN)).isTrue();
        assertThat(service.canApplyTransformation("TRUE", FieldTransformationType.TO_BOOLEAN)).isTrue();
    }

    @Test
    void convertToStringAndCaseConverters_nullInputs_returnNull() {
        assertThat(service.convertToUppercase(null)).isNull();
        assertThat(service.convertToLowercase(null)).isNull();
        assertThat(service.convertToTrim(null)).isNull();
    }

    @Test
    void convertToTitleCase_singleCharacterWord_returnsUppercasedChar() {
        // Covers lambda$convertToTitleCase$0: word.length() > 1 is false (single-char word)
        assertThat(service.convertToTitleCase("a b")).isEqualTo("A B");
        assertThat(service.convertToTitleCase("i")).isEqualTo("I");
    }

    @Test
    void isBooleanString_yesAndNo_returnTrue() {
        // Covers isBooleanString: "yes" and "no" branches
        assertThat(service.canApplyTransformation("yes", FieldTransformationType.TO_BOOLEAN)).isTrue();
        assertThat(service.canApplyTransformation("YES", FieldTransformationType.TO_BOOLEAN)).isTrue();
    }
}
