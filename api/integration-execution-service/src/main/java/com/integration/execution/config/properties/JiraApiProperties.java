package com.integration.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Jira API paths and settings.
 * Centralizes all Jira-related configuration in a type-safe manner.
 */
@Data
@Component
@ConfigurationProperties(prefix = "integration.jira.api")
public class JiraApiProperties {

    private Paths paths = new Paths();

    @Data
    public static class Paths {
        private String serverInfo = "/rest/api/2/serverInfo";
        private String projectSearch = "/rest/api/3/project/search";
        private String userAssignableSearch = "/rest/api/3/user/assignable/search";
        private String projectStatuses = "/rest/api/3/project/{projectKey}/statuses";
        private String fields = "/rest/api/3/field";
        private String createMetaFields =
                "/rest/api/3/issue/createmeta"
                        + "?projectKeys={projectKey}"
                        + "&expand=projects.issuetypes.fields";
        private String issueCreate = "/rest/api/3/issue";
        private String issueSearch = "/rest/api/3/search/jql";
        // Agile API
        private String boardsByProject = "/rest/agile/1.0/board?projectKeyOrId={projectKey}";
        private String sprintsByBoard = "/rest/agile/1.0/board/{boardId}/sprint";
        // Teams API
        private String teamsSearch = "/rest/teams/1.0/teams";
    }
}
