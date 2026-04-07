package com.integration.execution.contract.rest.response.kaseware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KwDocField {
    private int id;           // sequential id starting at 1
    private String fieldName; // GraphQL field name
    private String fieldType; // ofType.name or fallback to type.name
    private Boolean isMandatory; // true if non-nullable
}
