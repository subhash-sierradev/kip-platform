package com.integration.management.model.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransformationResult")
class TransformationResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("stores model metadata and exposes it in response")
    void storesModelMetadataAndExposesItInResponse() {
        ArrayNode features = objectMapper.createArrayNode();
        features.addObject().put("id", 101);

        List<RecordMetadata> successful = List.of(
                new RecordMetadata("doc-1", "Title", "loc-1", 11L, 12L, 13L, 14L));
        List<FailedRecordMetadata> failed = List.of(
                new FailedRecordMetadata("doc-2", "Title2", "loc-2", null, null, null, null, "bad field"));

        TransformationResult result = new TransformationResult(features, successful, failed);

        assertThat(result.getFeatures()).hasSize(1);
        assertThat(result.getSuccessfulMetadata()).hasSize(1);
        assertThat(result.getSuccessfulMetadata().getFirst().documentId()).isEqualTo("doc-1");
        assertThat(result.getFailedMetadata()).hasSize(1);
        assertThat(result.getFailedMetadata().getFirst().errorMessage()).isEqualTo("bad field");
    }

    @Test
    @DisplayName("serializes and deserializes response payload with metadata lists")
    void serializesAndDeserializesResponsePayloadWithMetadataLists() throws Exception {
        ArrayNode features = objectMapper.createArrayNode();
        features.addObject().put("id", 1);

        TransformationResult original = new TransformationResult(
                features,
                List.of(new RecordMetadata("doc-1", "Title", "loc-1", 1L, 2L, 3L, 4L)),
                List.of(new FailedRecordMetadata("doc-2", "Title2", "loc-2", null, null, null, null, "boom")));

        String json = objectMapper.writeValueAsString(original);
        JsonNode tree = objectMapper.readTree(json);
        TransformationResult copy = objectMapper.readValue(json, TransformationResult.class);

        assertThat(tree.get("successfulMetadata").get(0).get("documentId").asText()).isEqualTo("doc-1");
        assertThat(tree.get("failedMetadata").get(0).get("errorMessage").asText()).isEqualTo("boom");

        assertThat(copy.getFeatures()).hasSize(1);
        assertThat(copy.getSuccessfulMetadata().getFirst().locationId()).isEqualTo("loc-1");
        assertThat(copy.getFailedMetadata().getFirst().documentId()).isEqualTo("doc-2");
        assertThat(copy.getFailedMetadata().getFirst().errorMessage()).isEqualTo("boom");
    }
}
