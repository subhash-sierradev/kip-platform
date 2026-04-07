package com.integration.execution.contract.model;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;

public record TransformationResult(
        ArrayNode features,
        List<RecordMetadata> successfulMetadata,
        List<FailedRecordMetadata> failedMetadata
) {
}
