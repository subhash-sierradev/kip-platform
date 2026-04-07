package com.integration.execution.contract.rest.response.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing an Atlassian Team minimal payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraTeamResponse {
    private String id;
    private String name;
}
