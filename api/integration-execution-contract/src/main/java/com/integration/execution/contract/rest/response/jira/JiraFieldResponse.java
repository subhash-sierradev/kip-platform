package com.integration.execution.contract.rest.response.jira;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a Jira field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraFieldResponse {
    private String id;
    private String name;
    private String key;
    private Boolean custom;
    private Boolean orderable;
    private Boolean navigable;
    private Boolean searchable;

    @JsonIgnore
    private String schema;

    // Optional richer schema metadata from Jira response
    private JiraFieldSchema schemaDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JiraFieldSchema {
        private String type;
        private String system;
        private String custom;
        private String customId;
    }
}
