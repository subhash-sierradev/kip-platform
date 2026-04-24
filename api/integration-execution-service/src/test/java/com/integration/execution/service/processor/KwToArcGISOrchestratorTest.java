package com.integration.execution.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.integration.execution.client.KwGraphqlClient;
import com.integration.execution.contract.message.ArcGISExecutionCommand;
import com.integration.execution.contract.model.ArcGISJobExecutionResult;
import com.integration.execution.contract.model.ApplyEditsPartition;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.KwDocumentDto;
import com.integration.execution.contract.model.KwLocationDto;
import com.integration.execution.contract.model.PublishingResult;
import com.integration.execution.contract.model.RecordMetadata;
import com.integration.execution.contract.model.TransformationResult;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.mapper.KwLocationMapper;
import com.integration.execution.mapper.resolver.ArcGISMappingResolver;
import com.integration.execution.service.VaultService;
import com.integration.execution.service.publisher.ArcGISFeaturePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KwToArcGISOrchestratorTest {

    @Mock
    private KwGraphqlClient kwGraphqlClient;

    @Mock
    private KwLocationMapper locationMapper;

    @Mock
    private ArcGISMappingResolver mappingResolver;

    @Mock
    private ArcGISFeaturePublisher featurePublisher;

    @Mock
    private VaultService vaultService;

    private KwToArcGISOrchestrator orchestrator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orchestrator = new KwToArcGISOrchestrator(
                kwGraphqlClient,
                locationMapper,
                mappingResolver,
                featurePublisher,
                vaultService
        );
    }

    @Test
    void processExecution_noDocuments_returnsZeroResult() {
        ArcGISExecutionCommand command = command("secret");
        when(kwGraphqlClient.queryDocumentsWithLocations(command)).thenReturn(List.of());

        ArcGISJobExecutionResult result = orchestrator.processExecution(command);

        assertThat(result.addedRecords()).isZero();
        assertThat(result.updatedRecords()).isZero();
        assertThat(result.failedRecords()).isZero();
        assertThat(result.totalRecords()).isZero();
        verify(locationMapper, never()).transformToArcGISFeaturesWithMetadata(any(), any());
    }

    @Test
    void processExecution_noTransformedFeatures_returnsTransformationFailureResult() {
        ArcGISExecutionCommand command = command("secret");
        List<KwDocumentDto> documents = List.of(documentWithLocations("doc-1", 2));

        ArrayNode emptyFeatures = objectMapper.createArrayNode();
        List<FailedRecordMetadata> failedMetadata = List.of(
                new FailedRecordMetadata("doc-1", "Doc", "loc-1", 1L, 2L, 3L, 4L, "invalid mandatory field"),
                new FailedRecordMetadata("doc-1", "Doc", "loc-2", 1L, 2L, 3L, 4L, "invalid coordinates")
        );

        when(kwGraphqlClient.queryDocumentsWithLocations(command)).thenReturn(documents);
        when(locationMapper.transformToArcGISFeaturesWithMetadata(eq(documents), any()))
                .thenReturn(new TransformationResult(emptyFeatures, List.of(), failedMetadata));
        when(locationMapper.getAndClearTransformationErrors())
                .thenReturn(List.of("loc-1 invalid", "loc-2 invalid"));

        ArcGISJobExecutionResult result = orchestrator.processExecution(command);

        assertThat(result.addedRecords()).isZero();
        assertThat(result.updatedRecords()).isZero();
        assertThat(result.failedRecords()).isEqualTo(2);
        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.failedRecordsMetadata()).hasSize(2);
        assertThat(result.errorMessage()).contains("2 out of 2 records failed transformation");
        verify(mappingResolver, never()).partitionFeaturesForAddOrUpdate(any(), anyString(), anyString());
    }

    @Test
    void processExecution_fullPipeline_aggregatesResultsAndNormalizesEndpointUrl() {
        ArcGISExecutionCommand command = command("secret");
        List<KwDocumentDto> documents = List.of(documentWithLocations("doc-1", 2));

        ArrayNode features = objectMapper.createArrayNode();
        features.add(objectMapper.createObjectNode());
        features.add(objectMapper.createObjectNode());

        List<RecordMetadata> successfulMetadata = List.of(
                new RecordMetadata("doc-1", "Doc", "loc-1", 1L, 2L, 3L, 4L),
                new RecordMetadata("doc-1", "Doc", "loc-2", 1L, 2L, 3L, 4L)
        );
        List<FailedRecordMetadata> failedTransform = List.of(
                new FailedRecordMetadata("doc-1", "Doc", "loc-3", 1L, 2L, 3L, 4L, "bad field")
        );

        ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
        ArrayNode updates = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
        ApplyEditsPartition partition = new ApplyEditsPartition(adds, updates);

        PublishingResult publishingResult = new PublishingResult(
                1,
                1,
                1,
                List.of(successfulMetadata.get(0)),
                List.of(successfulMetadata.get(1)),
                List.of(new FailedRecordMetadata("doc-1", "Doc", "loc-4", 1L, 2L, 3L, 4L, "arcgis failed"))
        );

        when(kwGraphqlClient.queryDocumentsWithLocations(command)).thenReturn(documents);
        when(locationMapper.transformToArcGISFeaturesWithMetadata(eq(documents), any()))
                .thenReturn(new TransformationResult(features, successfulMetadata, failedTransform));
        when(locationMapper.getAndClearTransformationErrors()).thenReturn(List.of("transform warn"));
        when(vaultService.getSecret("secret")).thenReturn(secret("https://example.com/FeatureServer/0/"));
        when(mappingResolver.partitionFeaturesForAddOrUpdate(features, "secret", "https://example.com/FeatureServer"))
                .thenReturn(partition);
        when(featurePublisher.publishFeaturesWithMetadata(partition, "secret", successfulMetadata))
                .thenReturn(publishingResult);

        ArcGISJobExecutionResult result = orchestrator.processExecution(command);

        assertThat(result.addedRecords()).isEqualTo(1);
        assertThat(result.updatedRecords()).isEqualTo(1);
        assertThat(result.failedRecords()).isEqualTo(2);
        assertThat(result.totalRecords()).isEqualTo(4);
        assertThat(result.failedRecordsMetadata()).hasSize(2);
        assertThat(result.errorMessage()).contains("ArcGIS publish failed for 1 record(s)");

        verify(mappingResolver).partitionFeaturesForAddOrUpdate(
                features,
                "secret",
                "https://example.com/FeatureServer"
        );
    }

    @Test
    void processExecution_documentAtExactWindowEnd_isExcludedByFilter() {
        // windowEnd epoch = 1000s; document at exactly 1000 must be excluded
        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .connectionSecretName("secret")
                .windowStart(Instant.ofEpochSecond(0))
                .windowEnd(Instant.ofEpochSecond(1000))
                .fieldMappings(List.of())
                .build();

        KwDocumentDto atBoundary = new KwDocumentDto("doc-boundary", "D", "TYPE", 1L, 1000L);
        KwDocumentDto beforeBoundary = documentWithLocations("doc-before", 1);
        beforeBoundary.setUpdatedTimestamp(999L);

        ArrayNode emptyFeatures = objectMapper.createArrayNode();
        when(kwGraphqlClient.queryDocumentsWithLocations(command))
                .thenReturn(List.of(atBoundary, beforeBoundary));
        when(locationMapper.transformToArcGISFeaturesWithMetadata(
                eq(List.of(beforeBoundary)), any()))
                .thenReturn(new TransformationResult(emptyFeatures, List.of(), List.of()));
        when(locationMapper.getAndClearTransformationErrors()).thenReturn(List.of());

        orchestrator.processExecution(command);

        // boundary doc was excluded; only beforeBoundary was passed to the mapper
        verify(locationMapper).transformToArcGISFeaturesWithMetadata(
                eq(List.of(beforeBoundary)), any());
        verify(locationMapper, never()).transformToArcGISFeaturesWithMetadata(
                eq(List.of(atBoundary, beforeBoundary)), any());
    }

    @Test
    void processExecution_nullWindowEnd_skipsFilteringAndProcessesAll() {
        // windowEnd == null → the filtering branch is skipped entirely
        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .connectionSecretName("secret")
                .windowStart(Instant.ofEpochSecond(0))
                .windowEnd(null)
                .fieldMappings(List.of())
                .build();

        KwDocumentDto doc = documentWithLocations("doc-1", 1);
        ArrayNode emptyFeatures = objectMapper.createArrayNode();

        when(kwGraphqlClient.queryDocumentsWithLocations(command)).thenReturn(List.of(doc));
        when(locationMapper.transformToArcGISFeaturesWithMetadata(eq(List.of(doc)), any()))
                .thenReturn(new TransformationResult(emptyFeatures, List.of(), List.of()));
        when(locationMapper.getAndClearTransformationErrors()).thenReturn(List.of());

        orchestrator.processExecution(command);

        // All docs passed through without timestamp filtering
        verify(locationMapper).transformToArcGISFeaturesWithMetadata(eq(List.of(doc)), any());
    }

    @Test
    void processExecution_documentWithNullLocations_treatedAsZeroLocations() {
        // doc.getLocations() == null → ternary false branch; total = 0 → features.isEmpty + count==0 branch
        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .connectionSecretName("secret")
                .windowStart(Instant.ofEpochSecond(0))
                .windowEnd(null)
                .fieldMappings(List.of())
                .build();

        KwDocumentDto docWithNullLocations = new KwDocumentDto("doc-null-loc", "D", "TYPE", 1L, 2L);
        // locations field left null by default 5-arg constructor

        ArrayNode emptyFeatures = objectMapper.createArrayNode();
        when(kwGraphqlClient.queryDocumentsWithLocations(command)).thenReturn(List.of(docWithNullLocations));
        when(locationMapper.transformToArcGISFeaturesWithMetadata(any(), any()))
                .thenReturn(new TransformationResult(emptyFeatures, List.of(), List.of()));
        when(locationMapper.getAndClearTransformationErrors()).thenReturn(List.of());

        ArcGISJobExecutionResult result = orchestrator.processExecution(command);

        // totalLocationCount = 0 (null locations treated as 0), so returns empty result directly
        assertThat(result.totalRecords()).isZero();
        assertThat(result.failedRecords()).isZero();
    }

    @Test
    void processExecution_urlWithoutTrailingSlashOrNumericSuffix_usesUrlAsIs() {
        // Covers false branches for both endsWith('/') and matches('.*/\\d+$')
        ArcGISExecutionCommand command = command("secret");
        List<KwDocumentDto> documents = List.of(documentWithLocations("doc-1", 1));

        ArrayNode features = objectMapper.createArrayNode();
        features.add(objectMapper.createObjectNode());

        List<RecordMetadata> successMeta = List.of(
                new RecordMetadata("doc-1", "Doc", "loc-0", 1L, 2L, 1L, 2L));
        ApplyEditsPartition partition = new ApplyEditsPartition(
                objectMapper.createArrayNode(), objectMapper.createArrayNode());
        PublishingResult publishingResult = new PublishingResult(0, 0, 0,
                List.of(), List.of(), List.of());

        when(kwGraphqlClient.queryDocumentsWithLocations(command)).thenReturn(documents);
        when(locationMapper.transformToArcGISFeaturesWithMetadata(eq(documents), any()))
                .thenReturn(new TransformationResult(features, successMeta, List.of()));
        when(locationMapper.getAndClearTransformationErrors()).thenReturn(List.of());
        // URL with no trailing slash and no numeric suffix → both branches false
        when(vaultService.getSecret("secret")).thenReturn(secret("https://example.com/FeatureServer"));
        when(mappingResolver.partitionFeaturesForAddOrUpdate(
                features, "secret", "https://example.com/FeatureServer"))
                .thenReturn(partition);
        when(featurePublisher.publishFeaturesWithMetadata(partition, "secret", successMeta))
                .thenReturn(publishingResult);

        ArcGISJobExecutionResult result = orchestrator.processExecution(command);

        assertThat(result.totalRecords()).isZero();
        verify(mappingResolver).partitionFeaturesForAddOrUpdate(
                features, "secret", "https://example.com/FeatureServer");
    }

    @Test
    void processExecution_transformationFailuresWithEmptyErrorList_usesGenericFallback() {
        // Covers buildIndividualErrorMessage when errors list is empty
        ArcGISExecutionCommand command = command("secret");
        List<KwDocumentDto> documents = List.of(documentWithLocations("doc-1", 2));

        ArrayNode emptyFeatures = objectMapper.createArrayNode();
        List<FailedRecordMetadata> failedMeta = List.of(
                new FailedRecordMetadata("doc-1", "Doc", "loc-0", 1L, 2L, 1L, 2L, "err"),
                new FailedRecordMetadata("doc-1", "Doc", "loc-1", 1L, 2L, 1L, 2L, "err"));

        when(kwGraphqlClient.queryDocumentsWithLocations(command)).thenReturn(documents);
        when(locationMapper.transformToArcGISFeaturesWithMetadata(eq(documents), any()))
                .thenReturn(new TransformationResult(emptyFeatures, List.of(), failedMeta));
        // Empty errors list → buildIndividualErrorMessage uses fallback text branch
        when(locationMapper.getAndClearTransformationErrors()).thenReturn(List.of());

        ArcGISJobExecutionResult result = orchestrator.processExecution(command);

        assertThat(result.errorMessage()).contains("Check application logs");
        assertThat(result.failedRecords()).isEqualTo(2);
    }

    @Test
    void processExecution_allDocumentsAtOrAfterWindowEnd_returnsEmptyResult() {
        ArcGISExecutionCommand command = ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .connectionSecretName("secret")
                .windowStart(Instant.ofEpochSecond(0))
                .windowEnd(Instant.ofEpochSecond(1000))
                .fieldMappings(List.of())
                .build();

        KwDocumentDto atBoundary = new KwDocumentDto("doc-1", "D", "TYPE", 1L, 1000L);
        KwDocumentDto afterBoundary = new KwDocumentDto("doc-2", "D", "TYPE", 1L, 1001L);
        when(kwGraphqlClient.queryDocumentsWithLocations(command))
                .thenReturn(List.of(atBoundary, afterBoundary));

        ArcGISJobExecutionResult result = orchestrator.processExecution(command);

        assertThat(result.totalRecords()).isZero();
        verify(locationMapper, never()).transformToArcGISFeaturesWithMetadata(any(), any());
    }

    private ArcGISExecutionCommand command(String secretName) {
        return ArcGISExecutionCommand.builder()
                .integrationId(UUID.randomUUID())
                .connectionSecretName(secretName)
                .windowStart(Instant.parse("2026-01-01T00:00:00Z"))
                .windowEnd(Instant.parse("2026-01-31T23:59:59Z"))
                .fieldMappings(List.of())
                .build();
    }

    private KwDocumentDto documentWithLocations(String documentId, int locationCount) {
        List<KwLocationDto> locations = java.util.stream.IntStream.range(0, locationCount)
                .mapToObj(index -> new KwLocationDto(
                        "loc-" + index,
                        1L,
                        2L,
                        "name",
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
                ))
                .toList();

        return new KwDocumentDto(documentId, "Doc", "DOCUMENT", 1L, 2L, locations);
    }

    private IntegrationSecret secret(String baseUrl) {
        return IntegrationSecret.builder()
                .baseUrl(baseUrl)
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u").password("p").build())
                .build();
    }
}
