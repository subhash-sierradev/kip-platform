package com.integration.execution.contract.rest.response.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a Jira issue reference used for subtask parent selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueReferenceResponse {
    private String key;
    private String summary;
    private String issueType;
}
