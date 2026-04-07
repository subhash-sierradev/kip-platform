package com.integration.execution.service.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.contract.model.ApplyEditsPartition;
import com.integration.execution.contract.model.PublishingResult;
import com.integration.execution.contract.model.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArcGISFeaturePublisherTest {

    @Mock
    private ArcGISApiClient arcGISApiClient;

    private ObjectMapper objectMapper;
    private ArcGISFeaturePublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new ArcGISFeaturePublisher(arcGISApiClient, objectMapper);
    }

    @Test
    void publishFeaturesWithMetadata_mixedResults_mapsSuccessAndFailures() throws Exception {
        ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode()).add(objectMapper.createObjectNode());
        ArrayNode updates = objectMapper.createArrayNode().add(objectMapper.createObjectNode());

        String response = """
                {
                  "addResults": [
                    {"success": true},
                    {"success": false, "error": {"code": 400, "description": "invalid geometry"}}
                  ],
                  "updateResults": [
                    {"success": true}
                  ]
                }
                """;
        JsonNode responseNode = objectMapper.readTree(response);
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString())).thenReturn(responseNode);

        List<RecordMetadata> metadata = List.of(
                new RecordMetadata("d1", "Doc 1", "l1", 1L, 2L, 3L, 4L),
                new RecordMetadata("d2", "Doc 2", "l2", 1L, 2L, 3L, 4L),
                new RecordMetadata("d3", "Doc 3", "l3", 1L, 2L, 3L, 4L)
        );

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", metadata);

        assertThat(result.addedCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.addedMetadata()).containsExactly(metadata.get(0));
        assertThat(result.updatedMetadata()).containsExactly(metadata.get(2));
        assertThat(result.failedMetadata()).hasSize(1);
        assertThat(result.failedMetadata().getFirst().errorMessage()).contains("code=400");
    }

    @Test
    void publishFeaturesWithMetadata_missingResultArrays_countsAsFailures() throws Exception {
        ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
        ArrayNode updates = objectMapper.createArrayNode().add(objectMapper.createObjectNode());

        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("{}"));

        List<RecordMetadata> metadata = List.of(
                new RecordMetadata("d1", "Doc 1", "l1", 1L, 2L, 3L, 4L),
                new RecordMetadata("d2", "Doc 2", "l2", 1L, 2L, 3L, 4L)
        );

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", metadata);

        assertThat(result.addedCount()).isZero();
        assertThat(result.updatedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(2);
        assertThat(result.failedMetadata()).isEmpty();
    }

    @Test
    void publishFeaturesWithMetadata_clientFailure_throwsRuntimeException() {
        ArrayNode adds = objectMapper.createArrayNode();
        ArrayNode updates = objectMapper.createArrayNode();

        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenThrow(new RuntimeException("ArcGIS unavailable"));

        assertThatThrownBy(() -> publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish features to ArcGIS");
    }

    @Test
    void publishFeaturesWithMetadata_errorMessageFallbacks_handleMessageAndRawResult() throws Exception {
        ArrayNode adds = objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode())
                .add(objectMapper.createObjectNode());
        ArrayNode updates = objectMapper.createArrayNode();

        String response = """
                {
                  "addResults": [
                    {"success": false, "error": {"message": "bad field"}},
                    {"success": false}
                  ],
                  "updateResults": []
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        List<RecordMetadata> metadata = List.of(
                new RecordMetadata("d1", "Doc 1", "l1", 1L, 2L, 3L, 4L),
                new RecordMetadata("d2", "Doc 2", "l2", 1L, 2L, 3L, 4L)
        );

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", metadata);

        assertThat(result.failedCount()).isEqualTo(2);
        assertThat(result.failedMetadata()).hasSize(2);
        assertThat(result.failedMetadata().get(0).errorMessage()).isEqualTo("bad field");
        assertThat(result.failedMetadata().get(1).errorMessage()).contains("success");
    }

    @Test
    void publishFeaturesWithMetadata_updateResultsBeyondMetadata_areIgnored() throws Exception {
        ArrayNode adds = objectMapper.createArrayNode();
        ArrayNode updates = objectMapper.createArrayNode().add(objectMapper.createObjectNode());

        String response = """
                {
                  "addResults": [],
                  "updateResults": [
                    {"success": true}
                  ]
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", List.of());

        assertThat(result.addedCount()).isZero();
        assertThat(result.updatedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.updatedMetadata()).isEmpty();
    }
}
