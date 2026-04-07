package com.integration.execution.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.contract.model.KwLocationDto;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.TransformationResult;
import com.integration.execution.service.FieldTransformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KwLocationMapper {

    // ThreadLocal to store individual error messages during transformation
    private static final ThreadLocal<List<String>> TRANSFORMATION_ERRORS =
            ThreadLocal.withInitial(ArrayList::new);

    private final ObjectMapper objectMapper;
    private final FieldTransformationService fieldTransformationService;

    public TransformationResult transformToArcGISFeaturesWithMetadata(
            final List<KwDocumentDto> documents,
            final List<IntegrationFieldMappingDto> fieldMappings) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            throw new IllegalStateException(
                    "No field mappings provided for transformation");
        }

        // Clear any previous errors
        TRANSFORMATION_ERRORS.get().clear();

        ArrayNode features = objectMapper.createArrayNode();
        List<RecordMetadata> successfulMetadata = new ArrayList<>();
        List<FailedRecordMetadata> failedMetadata = new ArrayList<>();

        for (KwDocumentDto document : documents) {
            List<KwLocationDto> locations = document.getLocations();
            for (KwLocationDto location : locations) {
                try {
                    ObjectNode locationNode = convertLocationToObjectNode(location, document);
                    ObjectNode feature = transformToArcGISFeature(locationNode, fieldMappings);
                    features.add(feature);

                    // Track successful transformation
                    RecordMetadata metadata = new RecordMetadata(
                            document.getId(),
                            document.getTitle(),
                            location.getId(),
                            toNullableTimestamp(document.getCreatedTimestamp()),
                            toNullableTimestamp(document.getUpdatedTimestamp()),
                            toNullableTimestamp(location.getCreatedTimestamp()),
                            toNullableTimestamp(location.getUpdatedTimestamp())
                    );
                    successfulMetadata.add(metadata);
                } catch (Exception e) {
                    // Track failed transformation
                    FailedRecordMetadata failedRecord = new FailedRecordMetadata(
                            document.getId(),
                            document.getTitle(),
                            location.getId(),
                            toNullableTimestamp(document.getCreatedTimestamp()),
                            toNullableTimestamp(document.getUpdatedTimestamp()),
                            toNullableTimestamp(location.getCreatedTimestamp()),
                            toNullableTimestamp(location.getUpdatedTimestamp()),
                            e.getMessage()
                    );
                    failedMetadata.add(failedRecord);

                    String errorDetail = String.format(
                            "Doc: %s, Location: %s: %s",
                            document.getId() != null ? document.getId() : "unknown",
                            location.getId() != null ? location.getId() : "unknown",
                            e.getMessage());
                    TRANSFORMATION_ERRORS.get().add(errorDetail);

                    log.error("Error creating feature for location in document id={}: {}",
                            document.getId(), e.getMessage());
                    // Continue processing other locations
                }
            }
        }

        log.info("Transformed {} documents into {} ArcGIS features (successful: {}, failed: {})",
                documents.size(), features.size(), successfulMetadata.size(), failedMetadata.size());

        return new TransformationResult(features, successfulMetadata, failedMetadata);
    }

    public ObjectNode convertLocationToObjectNode(
            final KwLocationDto location,
            final KwDocumentDto document) {

        ObjectNode locationNode = objectMapper.createObjectNode();

        addLocationFields(locationNode, location);
        addDocumentContext(locationNode, document);
        locationNode.put("entityType", "ENTITY_LOCATION");

        return locationNode;
    }

    private void addLocationFields(ObjectNode locationNode, KwLocationDto location) {
        putIfNotNull(locationNode, "id", location.getId());
        putIfNotNull(locationNode, "locationName", location.getLocationName());
        putIfNotNull(locationNode, "locationType", location.getLocationType());
        putIfNotNull(locationNode, "street1", location.getStreet1());
        putIfNotNull(locationNode, "street2", location.getStreet2());
        putIfNotNull(locationNode, "district", location.getDistrict());
        putIfNotNull(locationNode, "city", location.getCity());
        putIfNotNull(locationNode, "county", location.getCounty());
        putIfNotNull(locationNode, "state", location.getState());
        putIfNotNull(locationNode, "zipCode", location.getZipCode());
        putIfNotNull(locationNode, "country", location.getCountry());

        if (location.getLatitude() != null) {
            locationNode.put("latitude", location.getLatitude());
        }
        if (location.getLongitude() != null) {
            locationNode.put("longitude", location.getLongitude());
        }

        locationNode.put("createdTimestamp", location.getCreatedTimestamp() * 1000);
        locationNode.put("updatedTimestamp", location.getUpdatedTimestamp() * 1000);
    }

    private void addDocumentContext(ObjectNode locationNode, KwDocumentDto document) {
        ObjectNode documentContext = objectMapper.createObjectNode();
        putIfNotNull(documentContext, "id", document.getId());
        putIfNotNull(documentContext, "title", document.getTitle());
        putIfNotNull(documentContext, "type", document.getDocumentType());

        documentContext.put("createdTimestamp", document.getCreatedTimestamp() * 1000);
        documentContext.put("updatedTimestamp", document.getUpdatedTimestamp() * 1000);

        locationNode.set("document", documentContext);
    }

    private void putIfNotNull(ObjectNode node, String fieldName, String value) {
        if (value != null) {
            node.put(fieldName, value);
        }
    }

    public ObjectNode transformToArcGISFeature(
            final ObjectNode locationNode,
            final List<IntegrationFieldMappingDto> fieldMappings) {

        ObjectNode feature = objectMapper.createObjectNode();
        ObjectNode attributes = objectMapper.createObjectNode();

        applyFieldMappings(attributes, locationNode, fieldMappings);

        feature.set("attributes", attributes);
        return feature;
    }

    private void applyFieldMappings(
            final ObjectNode attributes,
            final ObjectNode locationNode,
            final List<IntegrationFieldMappingDto> fieldMappings) {

        for (IntegrationFieldMappingDto mapping : fieldMappings) {
            try {
                Object sourceValue = extractFieldValue(locationNode, mapping.getSourceFieldPath());

                Object transformedValue = fieldTransformationService.applyTransformation(
                        sourceValue, mapping);

                String targetField = mapping.getTargetFieldPath();
                boolean isDateTimeField = targetField.toLowerCase().contains("timestamp")
                        || targetField.toLowerCase().contains("date")
                        || targetField.toLowerCase().contains("time");

                // Handle timestamp/date fields specially
                if (isDateTimeField && transformedValue != null) {
                    transformedValue = fieldTransformationService.convertToEpochMillis(transformedValue);

                    // Check if timestamp is invalid (0, negative, or unparseable string)
                    if (isInvalidTimestamp(transformedValue)) {
                        log.warn("Invalid timestamp value '{}' for field mapping: {} -> {}",
                                transformedValue, mapping.getSourceFieldPath(), mapping.getTargetFieldPath());
                        transformedValue = null; // Treat as null for validation
                    }
                }

                // Validate mandatory fields
                if (transformedValue == null && Boolean.TRUE.equals(mapping.getIsMandatory())) {
                    log.warn("Mandatory field mapping resulted in null value: {} -> {} | sourceValue: {}",
                            mapping.getSourceFieldPath(), mapping.getTargetFieldPath(), sourceValue);
                    throw new IllegalStateException(
                            "Mandatory field mapping resulted in null value: "
                                    + mapping.getTargetFieldPath());
                }

                // Only add non-null values to attributes
                if (transformedValue != null) {
                    putAttribute(attributes, targetField, transformedValue);
                }

            } catch (Exception e) {
                log.error("Error processing field mapping: {} -> {} for location record: {}",
                        mapping.getSourceFieldPath(), mapping.getTargetFieldPath(), e.getMessage());

                if (Boolean.TRUE.equals(mapping.getIsMandatory())) {
                    throw new IllegalStateException(
                            "Failed to process mandatory field mapping: "
                                    + mapping.getTargetFieldPath(), e);
                }
            }
        }
    }

    private boolean isInvalidTimestamp(Object value) {
        switch (value) {
            case null -> {
                return true;
            }
            // Check numeric values
            case Number numValue -> {
                long longValue = numValue.longValue();
                return longValue <= 0;
            }
            // Check string values that represent numbers
            case String strValue -> {
                try {
                    long longValue = Long.parseLong(strValue.trim());
                    return longValue <= 0;
                } catch (NumberFormatException e) {
                    // Not a valid number string, consider invalid
                    return true;
                }
            }
            default -> {
            }
        }
        return false;
    }

    public Object extractFieldValue(final ObjectNode locationNode, final String fieldPath) {
        if (locationNode == null || fieldPath == null || fieldPath.trim().isEmpty()) {
            log.error("Invalid location or field path for value extraction");
            return null;
        }

        try {
            String[] parts = fieldPath.split("\\.");
            JsonNode current = locationNode;

            for (String part : parts) {
                current = current.path(part);
                if (current.isMissingNode() || current.isNull()) {
                    log.debug("Field part '{}' in path '{}' not found in location",
                            part, fieldPath);
                    return null;
                }
            }

            if (current.isValueNode()) {
                return current.asText();
            }

            return current.toString();

        } catch (Exception e) {
            log.warn("Could not extract field '{}' from location: {}", fieldPath, e.getMessage());
            return null;
        }
    }

    private void putAttribute(
            final ObjectNode attributes,
            final String targetField,
            final Object value) {

        switch (value) {
            case String s -> attributes.put(targetField, s);
            case Boolean b -> attributes.put(targetField, b);
            case Integer i -> attributes.put(targetField, i);
            case Long l -> attributes.put(targetField, l);
            case Number n -> attributes.put(targetField, n.doubleValue());
            default -> {
                log.info("Unsupported attribute type for field {}: {}, storing as string",
                        targetField, value.getClass().getSimpleName());
                attributes.put(targetField, value.toString());
            }
        }
    }

    public List<KwLocationDto> extractLocations(final JsonNode relatedEntities) {
        if (relatedEntities == null || !relatedEntities.isArray() || relatedEntities.isEmpty()) {
            return List.of();
        }

        List<KwLocationDto> locations = new java.util.ArrayList<>();
        for (JsonNode entity : relatedEntities) {
            String entityType = entity.path("entityType").asText(null);

            if ("ENTITY_LOCATION".equals(entityType)) {
                KwLocationDto location = convertToLocationDto(entity);
                if (location != null) {
                    locations.add(location);
                }
            }
        }
        return locations;
    }

    public KwLocationDto convertToLocationDto(final JsonNode locationNode) {
        if (locationNode == null || locationNode.isNull()) {
            return null;
        }

        KwLocationDto location = new KwLocationDto();

        location.setId(locationNode.path("id").asText(null));
        location.setLocationName(locationNode.path("locationName").asText(null));
        location.setLocationType(locationNode.path("locationType").asText(null));
        location.setStreet1(locationNode.path("street1").asText(null));
        location.setStreet2(locationNode.path("street2").asText(null));
        location.setDistrict(locationNode.path("district").asText(null));
        location.setCity(locationNode.path("city").asText(null));
        location.setCounty(locationNode.path("county").asText(null));
        location.setState(locationNode.path("state").asText(null));
        location.setZipCode(locationNode.path("zipCode").asText(null));
        location.setCountry(locationNode.path("country").asText(null));

        JsonNode latNode = locationNode.path("latitude");
        if (!latNode.isMissingNode() && !latNode.isNull() && latNode.isNumber()) {
            location.setLatitude(latNode.asDouble());
        }

        JsonNode lonNode = locationNode.path("longitude");
        if (!lonNode.isMissingNode() && !lonNode.isNull() && lonNode.isNumber()) {
            location.setLongitude(lonNode.asDouble());
        }

        location.setCreatedTimestamp(locationNode.path("createdTimestamp").asLong(0L));
        location.setUpdatedTimestamp(locationNode.path("updatedTimestamp").asLong(0L));

        return location;
    }

    public List<String> getAndClearTransformationErrors() {
        List<String> errors = new java.util.ArrayList<>(TRANSFORMATION_ERRORS.get());
        TRANSFORMATION_ERRORS.remove();
        return errors;
    }

    private Long toNullableTimestamp(long epochSeconds) {
        return epochSeconds == 0 ? null : epochSeconds * 1000;
    }
}

