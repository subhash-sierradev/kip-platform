package com.integration.execution.contract.rest.response.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a Jira Sprint from Agile API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraSprintResponse {
    private Long id;
    private String name;
    private String state; // active, future, closed
    private String startDate;
    private String endDate;
    private Long originBoardId;
}
