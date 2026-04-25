package com.integration.execution.service.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.ApplyEditsPartition;
import com.integration.execution.contract.model.PublishingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArcGISFeaturePublisher {

    private final ArcGISApiClient arcgisApiClient;
    private final ObjectMapper objectMapper;

    public PublishingResult publishFeaturesWithMetadata(
            final ApplyEditsPartition partition,
            final String secretName,
            final List<RecordMetadata> transformedMetadata) {

        ArrayNode adds = partition.adds();
        ArrayNode updates = partition.updates();

        log.info("Publishing to ArcGIS - Adds: {}, Updates: {}", adds.size(), updates.size());

        try {
            // Build payload with separate adds and updates arrays
            ObjectNode payload = objectMapper.createObjectNode();
            payload.set("adds", adds);
            payload.set("updates", updates);

            String payloadJson = objectMapper.writeValueAsString(payload);

            log.info("ArcGIS applyEdits payload: {}", payloadJson);

            // Send to ArcGIS
            JsonNode response = arcgisApiClient.applyEditsWithPartition(secretName, payloadJson);

            // Parse response and collect metrics with metadata
            return parseApplyEditsResponseWithMetadata(
                    response,
                    transformedMetadata,
                    adds.size(),
                    updates.size());

        } catch (Exception e) {
            log.error("Error publishing features to ArcGIS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish features to ArcGIS", e);
        }
    }

    private PublishingResult parseApplyEditsResponseWithMetadata(
            final JsonNode response,
            final List<RecordMetadata> transformedMetadata,
            final int addFeatures,
            final int updateFeatures) {

        List<RecordMetadata> addedMetadata = new ArrayList<>();
        List<RecordMetadata> updatedMetadata = new ArrayList<>();
        List<FailedRecordMetadata> failedMetadata = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        // Process add results
        int addedCount = processAddResults(
                response.get("addResults"),
                transformedMetadata,
                // Start index for adds
                addedMetadata,
                failedMetadata,
                failureMessages);

        // Process update results
        int updatedCount = processUpdateResults(
                response.get("updateResults"),
                transformedMetadata,
                addFeatures, // Start index for updates (after adds)
                updatedMetadata,
                failedMetadata,
                failureMessages);

        int addFailed = addFeatures - addedCount;
        int updateFailed = updateFeatures - updatedCount;
        int totalFailed = addFailed + updateFailed;

        log.info("ArcGIS completed - Added: {}, Updated: {}, Failed: {}",
                addedCount, updatedCount, totalFailed);

        return new PublishingResult(
                addedCount,
                updatedCount,
                totalFailed,
                addedMetadata,
                updatedMetadata,
                failedMetadata);
    }

    private int processAddResults(
            final JsonNode resultsNode,
            final List<RecordMetadata> transformedMetadata,
            final List<RecordMetadata> addedMetadata,
            final List<FailedRecordMetadata> failedMetadata,
            final List<String> failureMessages) {

        if (resultsNode == null || !resultsNode.isArray()) {
            return 0;
        }

        int successCount = 0;
        for (int i = 0; i < resultsNode.size(); i++) {
            JsonNode result = resultsNode.get(i);
            if (i < transformedMetadata.size()) {
                RecordMetadata originalMetadata = transformedMetadata.get(i);
                if (isSuccess(result)) {
                    addedMetadata.add(originalMetadata);
                    successCount++;
                } else {
                    // Track failure
                    String errorMessage = extractErrorMessage(result);
                    failedMetadata.add(clone(originalMetadata, errorMessage));
                    collectFailureMessage("ADD", result, failureMessages);
                }
            }
        }
        return successCount;
    }

    private int processUpdateResults(
            final JsonNode resultsNode,
            final List<RecordMetadata> transformedMetadata,
            final int startIndex,
            final List<RecordMetadata> updatedMetadata,
            final List<FailedRecordMetadata> failedMetadata,
            final List<String> failureMessages) {

        if (resultsNode == null || !resultsNode.isArray()) {
            return 0;
        }

        int successCount = 0;
        for (int i = 0; i < resultsNode.size(); i++) {
            JsonNode result = resultsNode.get(i);
            int metadataIndex = startIndex + i;

            if (metadataIndex < transformedMetadata.size()) {
                RecordMetadata originalMetadata = transformedMetadata.get(metadataIndex);
                if (isSuccess(result)) {
                    updatedMetadata.add(originalMetadata);
                    successCount++;
                } else {
                    // Track failure
                    String errorMessage = extractErrorMessage(result);
                    failedMetadata.add(clone(originalMetadata, errorMessage));
                    collectFailureMessage("UPDATE", result, failureMessages);
                }
            }
        }
        return successCount;
    }

    private boolean isSuccess(final JsonNode result) {
        JsonNode successNode = result.get("success");
        return successNode != null && successNode.booleanValue();
    }

    private String extractErrorMessage(final JsonNode result) {
        JsonNode error = result.get("error");
        if (error == null) {
            return result.toString();
        }

        String description = error.path("description").asText(null);
        String message = error.path("message").asText(null);
        int code = error.path("code").asInt(-1);

        String detail = description != null ? description : message;
        if (detail == null) {
            detail = error.toString();
        }

        return code >= 0 ? ("code=" + code + ": " + detail) : detail;
    }

    private void collectFailureMessage(
            final String operation,
            final JsonNode result,
            final List<String> failureMessages) {

        JsonNode error = result.get("error");
        log.warn("Feature {} failed: {}", operation, error);

        String description = error != null ? error.path("description").asText(null) : null;
        String message = error != null ? error.path("message").asText(null) : null;
        int code = error != null ? error.path("code").asInt(-1) : -1;

        String detail = description != null ? description : message;
        if (detail == null) {
            detail = error != null ? error.toString() : "Unknown error";
        }

        String prefix = code >= 0 ? (operation + "(code=" + code + "): ") : (operation + ": ");
        failureMessages.add(prefix + detail);
    }

    private FailedRecordMetadata clone(RecordMetadata original, String errorMessage) {
        return new FailedRecordMetadata(
                original.documentId(),
                original.title(),
                original.locationId(),
                original.documentCreatedAt(),
                original.documentUpdatedAt(),
                original.locationCreatedAt(),
                original.locationUpdatedAt(),
                errorMessage
        );
    }
}

