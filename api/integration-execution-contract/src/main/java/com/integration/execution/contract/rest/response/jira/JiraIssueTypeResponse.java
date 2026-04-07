package com.integration.execution.contract.rest.response.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a Jira issue type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueTypeResponse {
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private Boolean subtask;
}
