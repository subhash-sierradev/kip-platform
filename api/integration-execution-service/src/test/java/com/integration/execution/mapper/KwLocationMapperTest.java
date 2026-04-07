package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.contract.model.KwLocationDto;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.TransformationResult;
import com.integration.execution.contract.model.enums.FieldTransformationType;
import com.integration.execution.service.FieldTransformationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KwLocationMapperTest {

    @Mock
    private FieldTransformationService fieldTransformationService;

    private KwLocationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KwLocationMapper(new ObjectMapper(), fieldTransformationService);
    }

    @Test
    void transformToArcGISFeaturesWithMetadata_noMappings_throwsIllegalStateException() {
        assertThatThrownBy(() -> mapper.transformToArcGISFeaturesWithMetadata(List.of(), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No field mappings provided");
    }

    @Test
    void transformToArcGISFeaturesWithMetadata_mixedLocations_tracksSuccessAndFailure() {
        when(fieldTransformationService.applyTransformation(any(), any(IntegrationFieldMappingDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KwDocumentDto document = new KwDocumentDto(
                "doc-1",
                "Doc",
                "DOCUMENT",
                1L,
                2L,
                List.of(location("loc-1", "HQ"), location("loc-2", null))
        );

        IntegrationFieldMappingDto mandatoryName = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("locationName")
                .targetFieldPath("name")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(true)
                .build();

        TransformationResult result = mapper.transformToArcGISFeaturesWithMetadata(
                List.of(document),
                List.of(mandatoryName)
        );

        assertThat(result.features()).hasSize(1);
        assertThat(result.successfulMetadata()).hasSize(1);
        assertThat(result.failedMetadata()).hasSize(1);
        assertThat(result.failedMetadata().getFirst().errorMessage())
                .contains("Failed to process mandatory field mapping");

        List<String> errors = mapper.getAndClearTransformationErrors();
        assertThat(errors).hasSize(1);
    }

    @Test
    void extractFieldValue_nestedField_returnsValue() {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.putObject("document").put("id", "doc-123");

        Object value = mapper.extractFieldValue(node, "document.id");

        assertThat(value).isEqualTo("doc-123");
    }

    @Test
    void extractFieldValue_missingPath_returnsNull() {
        ObjectNode node = new ObjectMapper().createObjectNode();

        Object value = mapper.extractFieldValue(node, "document.id");

        assertThat(value).isNull();
    }

    @Test
    void extractLocations_filtersOnlyEntityLocation() throws Exception {
        String payload = """
                [
                  {"entityType":"ENTITY_LOCATION","id":"loc-1","locationName":"HQ"},
                  {"entityType":"ENTITY_PERSON","id":"p-1"}
                ]
                """;

        List<KwLocationDto> locations = mapper.extractLocations(new ObjectMapper().readTree(payload));

        assertThat(locations).hasSize(1);
        assertThat(locations.getFirst().getId()).isEqualTo("loc-1");
    }

    @Test
    void transformToArcGISFeature_mapsAttributesFromFieldMappings() {
        when(fieldTransformationService.applyTransformation(any(), any(IntegrationFieldMappingDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("locationName", "HQ");
        locationNode.put("count", 5);

        IntegrationFieldMappingDto nameMapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("locationName")
                .targetFieldPath("name")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(true)
                .build();
        IntegrationFieldMappingDto countMapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("count")
                .targetFieldPath("count")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(nameMapping, countMapping));

        assertThat(feature.path("attributes").path("name").asText()).isEqualTo("HQ");
        assertThat(feature.path("attributes").path("count").asText()).isEqualTo("5");
    }

    @Test
    void convertToLocationDto_parsesNumericAndTextFields() throws Exception {
        String json = """
                {
                  "id":"loc-1",
                  "locationName":"HQ",
                  "city":"Denver",
                  "latitude":39.73,
                  "longitude":-104.99,
                  "createdTimestamp":100,
                  "updatedTimestamp":200
                }
                """;

        KwLocationDto location = mapper.convertToLocationDto(new ObjectMapper().readTree(json));

        assertThat(location.getId()).isEqualTo("loc-1");
        assertThat(location.getCity()).isEqualTo("Denver");
        assertThat(location.getLatitude()).isEqualTo(39.73);
        assertThat(location.getCreatedTimestamp()).isEqualTo(100L);
    }

    @Test
    void getAndClearTransformationErrors_returnsAndClearsThreadLocalState() {
        when(fieldTransformationService.applyTransformation(any(), any(IntegrationFieldMappingDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KwDocumentDto document = new KwDocumentDto(
                "doc-2",
                "Doc",
                "DOCUMENT",
                1L,
                2L,
                List.of(location("loc-1", null))
        );

        IntegrationFieldMappingDto mandatoryName = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("locationName")
                .targetFieldPath("name")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(true)
                .build();

        mapper.transformToArcGISFeaturesWithMetadata(List.of(document), List.of(mandatoryName));

        List<String> first = mapper.getAndClearTransformationErrors();
        List<String> second = mapper.getAndClearTransformationErrors();

        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();
    }

    @Test
    void transformToArcGISFeaturesWithMetadata_zeroTimestamps_storeNullMetadataTimestamps() {
        when(fieldTransformationService.applyTransformation(any(), any(IntegrationFieldMappingDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KwLocationDto location = new KwLocationDto(
                "loc-1",
                0L,
                0L,
                "HQ",
                "type",
                "s1",
                "s2",
                "district",
                "city",
                "county",
                "state",
                "zip",
                "country",
                1.0,
                2.0);

        KwDocumentDto document = new KwDocumentDto(
                "doc-1",
                "Doc",
                "DOCUMENT",
                0L,
                0L,
                List.of(location));

        IntegrationFieldMappingDto nameMapping = mapping("locationName", "name", true);

        TransformationResult result = mapper.transformToArcGISFeaturesWithMetadata(
                List.of(document),
                List.of(nameMapping));

        assertThat(result.successfulMetadata()).hasSize(1);
        RecordMetadata metadata = result.successfulMetadata().getFirst();
        assertThat(metadata.documentCreatedAt()).isNull();
        assertThat(metadata.documentUpdatedAt()).isNull();
        assertThat(metadata.locationCreatedAt()).isNull();
        assertThat(metadata.locationUpdatedAt()).isNull();
    }

    @Test
    void transformToArcGISFeaturesWithMetadata_errorDetailUsesUnknownWhenIdsAreNull() {
        when(fieldTransformationService.applyTransformation(any(), any(IntegrationFieldMappingDto.class)))
                .thenReturn(null);

        KwLocationDto location = new KwLocationDto(
                null,
                0L,
                0L,
                null,
                "type",
                "s1",
                "s2",
                "district",
                "city",
                "county",
                "state",
                "zip",
                "country",
                1.0,
                2.0);

        KwDocumentDto document = new KwDocumentDto(
                null,
                "Doc",
                "DOCUMENT",
                0L,
                0L,
                List.of(location));

        IntegrationFieldMappingDto mandatoryMapping = mapping("locationName", "name", true);

        mapper.transformToArcGISFeaturesWithMetadata(List.of(document), List.of(mandatoryMapping));

        List<String> errors = mapper.getAndClearTransformationErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).contains("Doc: unknown");
        assertThat(errors.getFirst()).contains("Location: unknown");
    }

    @Test
    void transformToArcGISFeature_dateFieldInvalidAndMandatory_throwsIllegalStateException() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("createdTimestamp", 123L);

        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("createdTimestamp")
                .targetFieldPath("createdDate")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(true)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(mapping))).thenReturn("invalid-date");
        when(fieldTransformationService.convertToEpochMillis("invalid-date")).thenReturn("0");

        assertThatThrownBy(() -> mapper.transformToArcGISFeature(locationNode, List.of(mapping)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to process mandatory field mapping");
    }

    @Test
    void transformToArcGISFeature_dateFieldInvalidAndOptional_skipsAttribute() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("createdTimestamp", 123L);

        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("createdTimestamp")
                .targetFieldPath("updatedTime")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(mapping))).thenReturn("bad");
        when(fieldTransformationService.convertToEpochMillis("bad")).thenReturn("-5");

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(mapping));

        assertThat(feature.path("attributes").isEmpty()).isTrue();
    }

    @Test
    void transformToArcGISFeature_transformationThrowsOnOptional_mappingContinues() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("locationName", "HQ");
        locationNode.put("city", "Denver");

        IntegrationFieldMappingDto badMapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("locationName")
                .targetFieldPath("name")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        IntegrationFieldMappingDto goodMapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("city")
                .targetFieldPath("city")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(badMapping)))
                .thenThrow(new RuntimeException("boom"));
        when(fieldTransformationService.applyTransformation(any(), eq(goodMapping))).thenReturn("Denver");

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(badMapping, goodMapping));

        assertThat(feature.path("attributes").path("city").asText()).isEqualTo("Denver");
        assertThat(feature.path("attributes").has("name")).isFalse();
    }

    @Test
    void transformToArcGISFeature_putAttributeBranches_handlesPrimitiveAndObjectValues() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("flag", "true");
        locationNode.put("integerVal", "7");
        locationNode.put("longVal", "11");
        locationNode.put("doubleVal", "3.5");
        locationNode.put("mapVal", "x");

        IntegrationFieldMappingDto boolMapping = mapping("flag", "boolField", false);
        IntegrationFieldMappingDto intMapping = mapping("integerVal", "intField", false);
        IntegrationFieldMappingDto longMapping = mapping("longVal", "longField", false);
        IntegrationFieldMappingDto numberMapping = mapping("doubleVal", "numberField", false);
        IntegrationFieldMappingDto objectMapping = mapping("mapVal", "objectField", false);

        when(fieldTransformationService.applyTransformation(any(), eq(boolMapping))).thenReturn(Boolean.TRUE);
        when(fieldTransformationService.applyTransformation(any(), eq(intMapping))).thenReturn(7);
        when(fieldTransformationService.applyTransformation(any(), eq(longMapping))).thenReturn(11L);
        when(fieldTransformationService.applyTransformation(any(), eq(numberMapping))).thenReturn(3.5f);
        when(fieldTransformationService.applyTransformation(any(), eq(objectMapping))).thenReturn(Map.of("k", "v"));

        ObjectNode feature = mapper.transformToArcGISFeature(
                locationNode,
                List.of(boolMapping, intMapping, longMapping, numberMapping, objectMapping));

        assertThat(feature.path("attributes").path("boolField").asBoolean()).isTrue();
        assertThat(feature.path("attributes").path("intField").asInt()).isEqualTo(7);
        assertThat(feature.path("attributes").path("longField").asLong()).isEqualTo(11L);
        assertThat(feature.path("attributes").path("numberField").asDouble()).isEqualTo(3.5);
        assertThat(feature.path("attributes").path("objectField").asText()).isEqualTo("{k=v}");
    }

    @Test
    void extractFieldValue_invalidInputs_returnNull() {
        assertThat(mapper.extractFieldValue(null, "a.b")).isNull();
        assertThat(mapper.extractFieldValue(new ObjectMapper().createObjectNode(), "  ")).isNull();
        assertThat(mapper.extractFieldValue(new ObjectMapper().createObjectNode(), null)).isNull();
    }

    @Test
    void extractFieldValue_objectNode_returnsJsonString() {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.putObject("document").put("id", "doc-1");

        Object value = mapper.extractFieldValue(node, "document");

        assertThat(value).isEqualTo("{\"id\":\"doc-1\"}");
    }

    @Test
    void extractLocations_nullOrNonArrayOrEmpty_returnsEmptyList() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThat(mapper.extractLocations(null)).isEmpty();
        assertThat(mapper.extractLocations(objectMapper.readTree("{}"))).isEmpty();
        assertThat(mapper.extractLocations(objectMapper.readTree("[]"))).isEmpty();
    }

    @Test
    void convertToLocationDto_nullAndNonNumericLatLon_handlesGracefully() throws Exception {
        assertThat(mapper.convertToLocationDto(null)).isNull();

        String json = "{\"id\":\"loc-1\",\"latitude\":\"n/a\",\"longitude\":null}";
        KwLocationDto dto = mapper.convertToLocationDto(new ObjectMapper().readTree(json));

        assertThat(dto.getId()).isEqualTo("loc-1");
        assertThat(dto.getLatitude()).isNull();
        assertThat(dto.getLongitude()).isNull();
    }

    @Test
    void transformToArcGISFeature_dateFieldWithValidStringTimestamp_isMapped() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("createdTimestamp", 123L);

        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("createdTimestamp")
                .targetFieldPath("createdDate")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(mapping))).thenReturn("123");
        when(fieldTransformationService.convertToEpochMillis("123")).thenReturn("1700000000000");

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(mapping));

        assertThat(feature.path("attributes").path("createdDate").asText()).isEqualTo("1700000000000");
    }

    @Test
    void transformToArcGISFeature_dateFieldWithNonNumericObjectTimestamp_storesAsString() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("createdTimestamp", 123L);

        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("createdTimestamp")
                .targetFieldPath("createdDate")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(mapping))).thenReturn("abc");
        when(fieldTransformationService.convertToEpochMillis("abc")).thenReturn(Map.of("ts", "abc"));

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(mapping));

        assertThat(feature.path("attributes").path("createdDate").asText()).isEqualTo("{ts=abc}");
    }

    @Test
    void convertLocationToObjectNode_withNullLatitudeAndLongitude_omitsCoordinateFields() {
        KwLocationDto location = new KwLocationDto(
            "loc-1", 100L, 200L, "HQ", "type",
            "s1", "s2", "district", "city", "county",
            "state", "zip", "country",
            null, null);
        KwDocumentDto document = new KwDocumentDto("doc-1", "Doc", "DOCUMENT", 1L, 2L, List.of(location));

        ObjectNode node = mapper.convertLocationToObjectNode(location, document);

        assertThat(node.has("latitude")).isFalse();
        assertThat(node.has("longitude")).isFalse();
    }

    @Test
    void transformToArcGISFeature_dateFieldWithPositiveLongTimestamp_isMapped() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("createdTimestamp", 123L);

        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("createdTimestamp")
                .targetFieldPath("createdDate")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(mapping))).thenReturn("val");
        when(fieldTransformationService.convertToEpochMillis("val")).thenReturn(1700000000000L);

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(mapping));

        assertThat(feature.path("attributes").path("createdDate").asLong()).isEqualTo(1700000000000L);
    }

    @Test
    void transformToArcGISFeature_dateFieldWithZeroLongTimestamp_treatedAsInvalid() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("createdTimestamp", 123L);

        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("createdTimestamp")
                .targetFieldPath("createdDate")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(mapping))).thenReturn("val");
        when(fieldTransformationService.convertToEpochMillis("val")).thenReturn(0L);

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(mapping));

        assertThat(feature.path("attributes").has("createdDate")).isFalse();
    }

    @Test
    void transformToArcGISFeature_dateFieldWithNegativeLongTimestamp_treatedAsInvalid() {
        ObjectNode locationNode = new ObjectMapper().createObjectNode();
        locationNode.put("createdTimestamp", 123L);

        IntegrationFieldMappingDto mapping = IntegrationFieldMappingDto.builder()
                .sourceFieldPath("createdTimestamp")
                .targetFieldPath("createdDate")
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(false)
                .build();

        when(fieldTransformationService.applyTransformation(any(), eq(mapping))).thenReturn("val");
        when(fieldTransformationService.convertToEpochMillis("val")).thenReturn(-1L);

        ObjectNode feature = mapper.transformToArcGISFeature(locationNode, List.of(mapping));

        assertThat(feature.path("attributes").has("createdDate")).isFalse();
    }

    private IntegrationFieldMappingDto mapping(String source, String target, boolean mandatory) {
        return IntegrationFieldMappingDto.builder()
                .sourceFieldPath(source)
                .targetFieldPath(target)
                .transformationType(FieldTransformationType.PASSTHROUGH)
                .isMandatory(mandatory)
                .build();
    }

    private KwLocationDto location(String id, String locationName) {
        return new KwLocationDto(
                id,
                1L,
                2L,
                locationName,
                "type",
                "s1",
                "s2",
                "district",
                "city",
                "county",
                "state",
                "zip",
                "country",
                1.0,
                2.0
        );
    }
}
