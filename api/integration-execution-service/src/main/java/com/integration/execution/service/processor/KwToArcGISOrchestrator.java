package com.integration.execution.service.processor;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.integration.execution.client.KwGraphqlClient;
import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.model.ArcGISJobExecutionResult;
import com.integration.execution.contract.model.ApplyEditsPartition;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.contract.model.PublishingResult;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.TransformationResult;
import com.integration.execution.mapper.KwLocationMapper;
import com.integration.execution.mapper.resolver.ArcGISMappingResolver;
import com.integration.execution.service.VaultService;
import com.integration.execution.service.publisher.ArcGISFeaturePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KwToArcGISOrchestrator {

    private final KwGraphqlClient kwGraphqlClient;
    private final KwLocationMapper locationTransformer;
    private final ArcGISMappingResolver mappingResolver;
    private final ArcGISFeaturePublisher featurePublisher;
    private final VaultService vaultService;

    /**
     * Orchestrates the full Kaseware → ArcGIS execution pipeline for a single command.
     */
    public ArcGISJobExecutionResult processExecution(final ArcGISExecutionCommand cmd) {
        log.info("Starting document processing for integration: {}", cmd.getIntegrationId());
        log.info("Processing window: {} to {}", cmd.getWindowStart(), cmd.getWindowEnd());

        // Step 1: Fetch and filter documents from Kaseware
        List<KwDocumentDto> documents = kwGraphqlClient.queryDocumentsWithLocations(cmd);
        documents = filterByWindowEnd(documents, cmd);

        if (documents.isEmpty()) {
            log.info("No document locations found for integration: {} in the time window {} - {}",
                    cmd.getIntegrationId(), cmd.getWindowStart(), cmd.getWindowEnd());
            return new ArcGISJobExecutionResult(0, 0, 0, 0,
                    List.of(), List.of(), List.of(), List.of());
        }

        log.info("Fetched {} documents with locations", documents.size());
        int totalLocationCount = countLocations(documents);
        log.info("Total location records across all documents (pre-transform): {}", totalLocationCount);

        // Step 2: Transform documents to ArcGIS features with metadata tracking
        TransformationResult transformationResult = locationTransformer
                .transformToArcGISFeaturesWithMetadata(documents, cmd.getFieldMappings());

        ArrayNode features = transformationResult.features();
        List<RecordMetadata> successfulTransformMetadata = transformationResult.successfulMetadata();
        List<FailedRecordMetadata> failedTransformMetadata = transformationResult.failedMetadata();
        List<String> transformationErrors = locationTransformer.getAndClearTransformationErrors();

        Optional<ArcGISJobExecutionResult> emptyFeaturesResult =
                tryBuildEmptyFeaturesResult(features, totalLocationCount,
                        failedTransformMetadata, transformationErrors);
        if (emptyFeaturesResult.isPresent()) {
            return emptyFeaturesResult.get();
        }

        log.info("Transformed to {} ArcGIS features", features.size());
        String combinedErrorMessage = buildTransformationWarning(
                totalLocationCount - features.size(), totalLocationCount, transformationErrors);

        // Step 3: Get ArcGIS endpoint URL from vault
        String secretName = cmd.getConnectionSecretName();
        String arcgisEndpointUrl = resolveArcGISEndpointUrl(secretName);

        // Step 4: Resolve mappings and partition features (URL-aware)
        ApplyEditsPartition partition = mappingResolver
                .partitionFeaturesForAddOrUpdate(features, secretName, arcgisEndpointUrl);

        log.info("After transform: features={}, transformFailures={}",
                features.size(), totalLocationCount - features.size());
        log.info("ArcGIS partition: adds={}, updates={}, total={}",
                partition.adds().size(), partition.updates().size(),
                partition.adds().size() + partition.updates().size());
        log.info("Partitioned into {} adds, {} updates",
                partition.adds().size(), partition.updates().size());

        // Step 5: Publish to ArcGIS with metadata tracking
        PublishingResult publishingResult = featurePublisher.publishFeaturesWithMetadata(
                partition, secretName, successfulTransformMetadata);

        // Step 6: Aggregate all metadata
        List<FailedRecordMetadata> allFailedMetadata = new ArrayList<>(failedTransformMetadata);
        allFailedMetadata.addAll(publishingResult.failedMetadata());
        combinedErrorMessage = appendPublishFailureError(combinedErrorMessage, publishingResult);

        List<RecordMetadata> allTotalMetadata = aggregateTotalMetadata(publishingResult, allFailedMetadata);

        return new ArcGISJobExecutionResult(
                publishingResult.addedCount(),
                publishingResult.updatedCount(),
                allFailedMetadata.size(),
                allTotalMetadata.size(),
                publishingResult.addedMetadata(),
                publishingResult.updatedMetadata(),
                allFailedMetadata,
                allTotalMetadata,
                combinedErrorMessage);
    }

    /**
     * Filters documents to exclude those at or beyond the exclusive window-end boundary.
     * The Kaseware GraphQL API uses inclusive epoch-second boundaries, so windowEnd
     * (exclusive, half-open interval) must be enforced in memory at second granularity:
     * exact boundary → strict less-than; sub-second component → less-than-or-equal.
     */
    private List<KwDocumentDto> filterByWindowEnd(
            final List<KwDocumentDto> documents,
            final ArcGISExecutionCommand cmd) {
        if (cmd.getWindowEnd() == null) {
            return documents;
        }
        long windowEndSeconds = cmd.getWindowEnd().getEpochSecond();
        boolean exactSecondBoundary = cmd.getWindowEnd().getNano() == 0;
        return documents.stream()
                .filter(doc -> exactSecondBoundary
                        ? doc.getUpdatedTimestamp() < windowEndSeconds
                        : doc.getUpdatedTimestamp() <= windowEndSeconds)
                .toList();
    }

    /**
     * Counts the total number of locations across all documents.
     */
    private int countLocations(final List<KwDocumentDto> documents) {
        return documents.stream()
                .mapToInt(doc -> doc.getLocations() != null ? doc.getLocations().size() : 0)
                .sum();
    }

    /**
     * Returns a completed result when the features array is empty; otherwise {@link Optional#empty()}.
     */
    private Optional<ArcGISJobExecutionResult> tryBuildEmptyFeaturesResult(
            final ArrayNode features,
            final int totalLocationCount,
            final List<FailedRecordMetadata> failedTransformMetadata,
            final List<String> transformationErrors) {
        if (!features.isEmpty()) {
            return Optional.empty();
        }
        if (totalLocationCount > 0) {
            String errorMsg = buildIndividualErrorMessage(
                    totalLocationCount, totalLocationCount, transformationErrors);
            log.error(errorMsg);
            List<RecordMetadata> failedAsTotal = failedTransformMetadata.stream()
                    .map(this::convert)
                    .toList();
            return Optional.of(new ArcGISJobExecutionResult(0, 0, totalLocationCount, totalLocationCount,
                    List.of(), List.of(), failedTransformMetadata, failedAsTotal, errorMsg));
        }
        log.error("No valid feature payload generated for ArcGIS from {} location(s)", totalLocationCount);
        return Optional.of(new ArcGISJobExecutionResult(0, 0, 0, 0,
                List.of(), List.of(), List.of(), List.of()));
    }

    /**
     * Builds and logs a warning message when some records failed transformation; returns {@code null} if none failed.
     */
    private String buildTransformationWarning(
            final int failures,
            final int total,
            final List<String> errors) {
        if (failures <= 0) {
            return null;
        }
        String warning = buildIndividualErrorMessage(failures, total, errors);
        log.warn(warning);
        return warning;
    }

    /**
     * Appends an ArcGIS publish-failure notice to an existing error message, if any records failed publishing.
     */
    private String appendPublishFailureError(
            final String existingError,
            final PublishingResult publishingResult) {
        if (publishingResult.failedMetadata().isEmpty()) {
            return existingError;
        }
        String arcGisErrors = "ArcGIS publish failed for " + publishingResult.failedCount() + " record(s).";
        return existingError != null ? existingError + " | ArcGIS Errors: " + arcGisErrors : arcGisErrors;
    }

    /**
     * Assembles the combined total-metadata list from added, updated, and failed records.
     */
    private List<RecordMetadata> aggregateTotalMetadata(
            final PublishingResult publishingResult,
            final List<FailedRecordMetadata> allFailedMetadata) {
        List<RecordMetadata> allTotalMetadata = new ArrayList<>();
        allTotalMetadata.addAll(publishingResult.addedMetadata());
        allTotalMetadata.addAll(publishingResult.updatedMetadata());
        allFailedMetadata.stream().map(this::convert).forEach(allTotalMetadata::add);
        return allTotalMetadata;
    }

    /**
     * Resolves and normalises the ArcGIS feature-layer base URL from vault.
     */
    private String resolveArcGISEndpointUrl(final String secretName) {
        IntegrationSecret secret = vaultService.getSecret(secretName);
        String baseUrl = secret.getBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.matches(".*/\\d+$")) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        }

        log.debug("Normalized ArcGIS endpoint URL: {}", baseUrl);
        return baseUrl;
    }

    /**
     * Formats a human-readable error message listing individual transformation failures.
     */
    private String buildIndividualErrorMessage(
            final int failedCount,
            final int totalCount,
            final List<String> errors) {

        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(String.format(
                "%d out of %d records failed transformation:\n",
                failedCount, totalCount));

        if (errors != null && !errors.isEmpty()) {
            for (int i = 0; i < errors.size(); i++) {
                errorMsg.append(String.format("%d. %s\n", i + 1, errors.get(i)));
            }
        } else {
            errorMsg.append("Check application logs for specific field validation errors.");
        }

        return errorMsg.toString();
    }

    /**
     * Converts a {@link FailedRecordMetadata} to a plain {@link RecordMetadata}.
     */
    private RecordMetadata convert(final FailedRecordMetadata failed) {
        return new RecordMetadata(
                failed.documentId(),
                failed.title(),
                failed.locationId(),
                failed.documentCreatedAt(),
                failed.documentUpdatedAt(),
                failed.locationCreatedAt(),
                failed.locationUpdatedAt());
    }
}
