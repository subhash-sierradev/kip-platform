package com.integration.execution.contract.rest.response.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a Jira project.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraProjectResponse {
    private String key;
    private String name;
    private String description;
    private String projectTypeKey;
    private String lead;
    private String url;
    private Boolean archived;
}
