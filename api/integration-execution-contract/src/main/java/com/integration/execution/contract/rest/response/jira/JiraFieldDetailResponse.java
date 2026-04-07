package com.integration.execution.contract.rest.response.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Detailed metadata for a Jira field to support dynamic UI rendering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraFieldDetailResponse {
    private String id;
    private String name;
    private String key;
    private String schema; // raw schema JSON string for flexibility
    private Boolean custom;
    private Boolean orderable;
    private Boolean navigable;
    private Boolean searchable;
    private JiraFieldResponse.JiraFieldSchema schemaDetails;
    private Boolean required;
    private String dataType; // normalized type (e.g., string, number, user, date, option)
    private List<Map<String, Object>> allowedValues; // for option/select fields
    private List<Map<String, Object>> users; // for reporter/assignee fields
    private Map<String, Object> validations; // constraints like formats, min/max, patterns
}
