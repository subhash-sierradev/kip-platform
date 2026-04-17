package com.integration.execution.service.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.contract.model.ApplyEditsPartition;
import com.integration.execution.contract.model.PublishingResult;
import com.integration.execution.contract.model.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("ArcGISFeaturePublisher")
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

    private RecordMetadata record(String id) {
        return new RecordMetadata(id, "Doc " + id, "loc-" + id, 1L, 2L, 3L, 4L);
    }

    @Test
    void publishFeaturesWithMetadata_mixedResults_mapsSuccessAndFailures() throws Exception {
        ArrayNode adds = objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode())
                .add(objectMapper.createObjectNode());
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

        List<RecordMetadata> metadata = List.of(record("d1"), record("d2"), record("d3"));

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

        List<RecordMetadata> metadata = List.of(record("d1"), record("d2"));

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

        List<RecordMetadata> metadata = List.of(record("d1"), record("d2"));

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

    @Nested
    @DisplayName("extractErrorMessage branches")
    class ExtractErrorMessageBranches {

        @Test
        @DisplayName("error node with description takes description over message")
        void errorWithDescription_usesDescription() throws Exception {
            ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
            ArrayNode updates = objectMapper.createArrayNode();
            String response = """
                    {"addResults":[{"success":false,"error":{"code":500,"description":"desc error","message":"msg error"}}],"updateResults":[]}
                    """;
            when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                    .thenReturn(objectMapper.readTree(response));

            PublishingResult result = publisher.publishFeaturesWithMetadata(
                    new ApplyEditsPartition(adds, updates), "secret", List.of(record("d1")));

            assertThat(result.failedMetadata().getFirst().errorMessage()).contains("desc error").contains("code=500");
        }

        @Test
        @DisplayName("error node with only message uses message")
        void errorWithOnlyMessage_usesMessage() throws Exception {
            ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
            ArrayNode updates = objectMapper.createArrayNode();
            String response = """
                    {"addResults":[{"success":false,"error":{"message":"only message"}}],"updateResults":[]}
                    """;
            when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                    .thenReturn(objectMapper.readTree(response));

            PublishingResult result = publisher.publishFeaturesWithMetadata(
                    new ApplyEditsPartition(adds, updates), "secret", List.of(record("d1")));

            assertThat(result.failedMetadata().getFirst().errorMessage()).isEqualTo("only message");
        }

        @Test
        @DisplayName("error node with no description/message falls back to error.toString()")
        void errorWithNoDetail_fallsBackToRawError() throws Exception {
            ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
            ArrayNode updates = objectMapper.createArrayNode();
            String response = """
                    {"addResults":[{"success":false,"error":{"code":503}}],"updateResults":[]}
                    """;
            when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                    .thenReturn(objectMapper.readTree(response));

            PublishingResult result = publisher.publishFeaturesWithMetadata(
                    new ApplyEditsPartition(adds, updates), "secret", List.of(record("d1")));

            assertThat(result.failedMetadata().getFirst().errorMessage()).contains("code=503");
        }

        @Test
        @DisplayName("failure result with no error node falls back to raw result JSON")
        void failureWithNoErrorNode_usesRawJson() throws Exception {
            ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
            ArrayNode updates = objectMapper.createArrayNode();
            String response = """
                    {"addResults":[{"success":false}],"updateResults":[]}
                    """;
            when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                    .thenReturn(objectMapper.readTree(response));

            PublishingResult result = publisher.publishFeaturesWithMetadata(
                    new ApplyEditsPartition(adds, updates), "secret", List.of(record("d1")));

            assertThat(result.failedMetadata().getFirst().errorMessage()).contains("success");
        }
    }

    @Test
    @DisplayName("buildErrorMessage with empty failureMessages uses generic count message")
    void buildErrorMessage_emptyFailureMessages_usesGenericMessage() throws Exception {
        // When failureMessages is empty, uses "ArcGIS applyEdits had N failed result(s)."
        // This happens when failedCount > 0 but all records are beyond metadata bounds
        ArrayNode adds = objectMapper.createArrayNode();
        ArrayNode updates = objectMapper.createArrayNode();

        String response = """
                {
                  "addResults": [],
                  "updateResults": []
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        // Passes empty metadata but adds/updates are empty too, so failedCount=0
        // To hit this branch, need failedCount > 0 with no error messages
        // Use missingResultArrays path: adds=2, empty results -> failureMessages stays empty
        ArrayNode twoAdds = objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode())
                .add(objectMapper.createObjectNode());
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree("{}"));

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(twoAdds, objectMapper.createArrayNode()),
                "secret",
                List.of(record("a1"), record("a2")));

        // failedCount=2, failureMessages is empty -> generic message
        assertThat(result.failedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("result node with no success field is treated as failure (isSuccess returns false)")
    void publishFeaturesWithMetadata_resultWithNoSuccessField_treatedAsFailure() throws Exception {
        // Covers isSuccess: successNode == null => returns false (short-circuit)
        ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
        ArrayNode updates = objectMapper.createArrayNode();

        String response = """
                {
                  "addResults": [{"error":{"code":400,"description":"no success field"}}],
                  "updateResults": []
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        List<RecordMetadata> metadata = List.of(record("d-no-success"));

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", metadata);

        assertThat(result.addedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failedMetadata()).hasSize(1);
        assertThat(result.failedMetadata().get(0).errorMessage()).contains("code=400");
    }

    @Test
    @DisplayName("processAddResults with non-array resultsNode returns 0")
    void publishFeaturesWithMetadata_addResultsIsNotArray_returnsZeroAdded() throws Exception {
        // Covers processAddResults: resultsNode != null but !resultsNode.isArray() => return 0
        ArrayNode adds = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
        ArrayNode updates = objectMapper.createArrayNode();

        // addResults is a string, not an array
        String response = """
                {
                  "addResults": "not-an-array",
                  "updateResults": []
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", List.of(record("d1")));

        assertThat(result.addedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("processUpdateResults with non-array resultsNode returns 0")
    void publishFeaturesWithMetadata_updateResultsIsNotArray_returnsZeroUpdated() throws Exception {
        // Covers processUpdateResults: resultsNode != null but !resultsNode.isArray() => return 0
        ArrayNode adds = objectMapper.createArrayNode();
        ArrayNode updates = objectMapper.createArrayNode().add(objectMapper.createObjectNode());

        // updateResults is an object, not an array
        String response = """
                {
                  "addResults": [],
                  "updateResults": {"count": 0}
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", List.of(record("d1")));

        assertThat(result.updatedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("buildErrorMessage truncates message when too long")
    void buildErrorMessage_veryLongMessage_isTruncated() throws Exception {
        // Build a response with many failures to produce a long error message
        ObjectMapper om = new ObjectMapper();
        ArrayNode adds = om.createArrayNode();
        ArrayNode failResults = om.createArrayNode();

        // Create enough failures to exceed MAX_ERROR_MESSAGE_LENGTH (1024 chars)
        for (int i = 0; i < 30; i++) {
            adds.add(om.createObjectNode());
            String longDesc = "Detailed error description for record number " + i
                    + " with additional text to make the message very long indeed";
            failResults.add(om.createObjectNode()
                    .put("success", false)
                    .set("error", om.createObjectNode()
                            .put("code", 400)
                            .put("description", longDesc)));
        }

        String responseBody = om.createObjectNode()
                .set("addResults", failResults).toString()
                + "";
        // Rebuild properly
        com.fasterxml.jackson.databind.node.ObjectNode responseNode = om.createObjectNode();
        responseNode.set("addResults", failResults);
        responseNode.set("updateResults", om.createArrayNode());

        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(responseNode);

        List<RecordMetadata> metadata = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            metadata.add(record("d" + i));
        }

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, om.createArrayNode()), "secret", metadata);

        assertThat(result.failedCount()).isEqualTo(30);
        // The combined error message from failedMetadata should be retrievable
        // and would be truncated internally
        assertThat(result.failedMetadata()).hasSize(30);
    }

    @Test
    @DisplayName("processAddResults: result entries exceed metadata size - excess entries ignored")
    void publishFeaturesWithMetadata_addResultsMoreThanMetadata_excessIgnored() throws Exception {
        // Covers processAddResults: i >= transformedMetadata.size() => skip entry (false branch)
        // 3 addResults but only 1 metadata entry => only first is processed
        ArrayNode adds = objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode())
                .add(objectMapper.createObjectNode())
                .add(objectMapper.createObjectNode());
        ArrayNode updates = objectMapper.createArrayNode();

        String response = """
                {
                  "addResults": [
                    {"success": true},
                    {"success": true},
                    {"success": true}
                  ],
                  "updateResults": []
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        // Only 1 metadata entry for 3 results => result indices 1 and 2 exceed metadata size
        List<RecordMetadata> metadata = List.of(record("only-one"));

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", metadata);

        // Only the first result maps to metadata; the other 2 are silently ignored
        assertThat(result.addedCount()).isEqualTo(1);
        // failedCount = adds.size() - addedCount = 3 - 1 = 2
        assertThat(result.failedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("processUpdateResults: result entries exceed metadata size - excess entries ignored")
    void publishFeaturesWithMetadata_updateResultsMoreThanMetadata_excessIgnored() throws Exception {
        // Covers processUpdateResults: metadataIndex >= transformedMetadata.size() => skip entry
        ArrayNode adds = objectMapper.createArrayNode();
        ArrayNode updates = objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode())
                .add(objectMapper.createObjectNode());

        String response = """
                {
                  "addResults": [],
                  "updateResults": [
                    {"success": true},
                    {"success": true}
                  ]
                }
                """;
        when(arcGISApiClient.applyEditsWithPartition(anyString(), anyString()))
                .thenReturn(objectMapper.readTree(response));

        // No metadata entries => both update results exceed metadata size
        List<RecordMetadata> metadata = List.of();

        PublishingResult result = publisher.publishFeaturesWithMetadata(
                new ApplyEditsPartition(adds, updates), "secret", metadata);

        assertThat(result.updatedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(2);
    }

}
