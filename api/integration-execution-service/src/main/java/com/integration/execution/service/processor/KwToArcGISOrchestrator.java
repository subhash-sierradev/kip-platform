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

@Slf4j
@Service
@RequiredArgsConstructor
public class KwToArcGISOrchestrator {

    private final KwGraphqlClient kwGraphqlClient;
    private final KwLocationMapper locationTransformer;
    private final ArcGISMappingResolver mappingResolver;
    private final ArcGISFeaturePublisher featurePublisher;
    private final VaultService vaultService;

    public ArcGISJobExecutionResult processExecution(final ArcGISExecutionCommand cmd) {
        log.info("Starting document processing for integration: {}", cmd.getIntegrationId());
        log.info("Processing window: {} to {}", cmd.getWindowStart(), cmd.getWindowEnd());

        // Step 1: Fetch documents from Kaseware
        List<KwDocumentDto> documents = kwGraphqlClient.queryDocumentsWithLocations(cmd);

        if (documents.isEmpty()) {
            log.info("No document locations found for integration: {} in the time window {} - {}",
                    cmd.getIntegrationId(), cmd.getWindowStart(), cmd.getWindowEnd());
            return new ArcGISJobExecutionResult(0, 0, 0, 0,
                    List.of(), List.of(), List.of(), List.of());
        }

        log.info("Fetched {} documents with locations", documents.size());
        int totalLocationsBeforeTransform = documents.stream()
                .mapToInt(doc -> doc.getLocations() != null ? doc.getLocations().size() : 0)
                .sum();
        log.info("Total location records across all documents (pre-transform): {}",
                totalLocationsBeforeTransform);

        // Step 2: Transform documents to ArcGIS features with metadata tracking
        TransformationResult transformationResult = locationTransformer
                .transformToArcGISFeaturesWithMetadata(documents, cmd.getFieldMappings());

        ArrayNode features = transformationResult.features();
        List<RecordMetadata> successfulTransformMetadata = transformationResult.successfulMetadata();
        List<FailedRecordMetadata> failedTransformMetadata = transformationResult.failedMetadata();

        int totalLocationCount = documents.stream()
                .mapToInt(doc -> doc.getLocations() != null ? doc.getLocations().size() : 0)
                .sum();

        List<String> transformationErrors = locationTransformer.getAndClearTransformationErrors();

        if (features.isEmpty()) {
            if (totalLocationCount > 0) {
                String errorMsg = buildIndividualErrorMessage(totalLocationCount,
                        totalLocationCount, transformationErrors);
                log.error(errorMsg);
                List<RecordMetadata> failedAsTotal = failedTransformMetadata.stream()
                        .map(this::convert)
                        .toList();
                return new ArcGISJobExecutionResult(0, 0, totalLocationCount, totalLocationCount,
                        List.of(), List.of(), failedTransformMetadata, failedAsTotal, errorMsg);
            }
            log.error("No valid feature payload generated for ArcGIS from {} documents", documents.size());
            return new ArcGISJobExecutionResult(0, 0, 0, 0,
                    List.of(), List.of(), List.of(), List.of());
        }

        log.info("Transformed to {} ArcGIS features", features.size());

        String combinedErrorMessage = null;
        int transformationFailures = totalLocationCount - features.size();
        if (transformationFailures > 0) {
            combinedErrorMessage = buildIndividualErrorMessage(transformationFailures,
                    totalLocationCount, transformationErrors);
            log.warn(combinedErrorMessage);
        }

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
        List<FailedRecordMetadata> allFailedMetadata = new ArrayList<>();
        allFailedMetadata.addAll(failedTransformMetadata);
        allFailedMetadata.addAll(publishingResult.failedMetadata());

        if (!publishingResult.failedMetadata().isEmpty()) {
            String arcGisErrors = "ArcGIS publish failed for " + publishingResult.failedCount() + " record(s).";
            combinedErrorMessage = (combinedErrorMessage != null)
                    ? combinedErrorMessage + " | ArcGIS Errors: " + arcGisErrors
                    : arcGisErrors;
        }

        List<RecordMetadata> allTotalMetadata = new ArrayList<>();
        allTotalMetadata.addAll(publishingResult.addedMetadata());
        allTotalMetadata.addAll(publishingResult.updatedMetadata());
        allFailedMetadata.stream().map(this::convert).forEach(allTotalMetadata::add);

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
