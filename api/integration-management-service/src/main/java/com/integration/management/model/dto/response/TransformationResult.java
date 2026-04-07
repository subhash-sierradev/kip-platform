package com.integration.management.model.dto.response;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.integration.execution.contract.model.FailedRecordMetadata;
import com.integration.execution.contract.model.RecordMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformationResult {

    private ArrayNode features;
    private List<RecordMetadata> successfulMetadata;
    private List<FailedRecordMetadata> failedMetadata;
}
