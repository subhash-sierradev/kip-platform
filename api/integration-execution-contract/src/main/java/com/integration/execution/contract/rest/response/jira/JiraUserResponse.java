package com.integration.execution.contract.rest.response.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a Jira user/assignee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraUserResponse {
    private String accountId;
    private String emailAddress;
    private String displayName;
    private Boolean active;
    private String accountType;
}
