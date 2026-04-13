package com.integration.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.integration.execution.client.ArcGISApiClient;
import com.integration.execution.client.ConfluenceApiClient;
import com.integration.execution.client.JiraApiClient;
import com.integration.execution.config.properties.JiraApiProperties;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.response.ApiResponse;
import com.integration.execution.exception.IntegrationApiException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConnectionTestService {

    private final ArcGISApiClient arcGisApiClient;
    private final JiraApiClient jiraApiClient;
    private final JiraApiProperties jiraApiProperties;
    private final ConfluenceApiClient confluenceApiClient;

    public ApiResponse testConnection(ServiceType serviceType, IntegrationSecret secret) {
        try {
            return switch (serviceType) {
                case ARCGIS -> testArcGis(() -> arcGisApiClient.getAccessToken("", secret));
                case JIRA -> testJiraConnectionAndValidateProjectAccess(secret);
                case CONFLUENCE -> testConfluence(secret);
            };
        } catch (IntegrationApiException ex) {
            log.error("Connection test failed", ex);
            return new ApiResponse(ex.getStatusCode(), false,
                    "Connection test failed: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Connection test failed", ex);
            return new ApiResponse(SC_INTERNAL_SERVER_ERROR, false,
                    "Connection test failed: " + ex.getMessage());
        }
    }

    public ApiResponse testConnection(ServiceType serviceType, String secretName) {
        try {
            return switch (serviceType) {
                case ARCGIS -> testArcGis(() -> arcGisApiClient.getAccessToken(secretName));
                case JIRA -> testJiraConnectionAndValidateProjectAccessBySecretName(secretName);
                case CONFLUENCE -> testConfluenceBySecretName(secretName);
            };
        } catch (IntegrationApiException ex) {
            log.error("Connection test failed", ex);
            return new ApiResponse(ex.getStatusCode(), false,
                    "Connection test failed: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Connection test failed", ex);
            return new ApiResponse(SC_INTERNAL_SERVER_ERROR, false,
                    "Connection test failed: " + ex.getMessage());
        }
    }

    private ApiResponse testConfluence(IntegrationSecret secret) {
        return validateConfluenceCurrentUser(
                () -> confluenceApiClient.getCurrentUser(secret), "secret");
    }

    private ApiResponse testConfluenceBySecretName(String secretName) {
        return validateConfluenceCurrentUser(
                () -> confluenceApiClient.getCurrentUser(secretName), "secretName");
    }

    private ApiResponse validateConfluenceCurrentUser(
            Supplier<JsonNode> currentUserFetcher, String logContext) {
        try {
            JsonNode userResponse = currentUserFetcher.get();

            if (userResponse == null) {
                return new ApiResponse(SC_INTERNAL_SERVER_ERROR, false,
                        "Unable to connect - no response from Confluence server");
            }

            // Circuit breaker fallback
            JsonNode errorField = userResponse.get("error");
            if (errorField != null && "Service temporarily unavailable".equals(errorField.asText())) {
                return new ApiResponse(HttpServletResponse.SC_SERVICE_UNAVAILABLE, false,
                        "Confluence service temporarily unavailable - please try again later");
            }

            // Verify user account returned
            JsonNode accountId = userResponse.get("accountId");
            if (accountId == null || accountId.asText().isBlank()) {
                return new ApiResponse(SC_INTERNAL_SERVER_ERROR, false,
                        "Unable to connect - no user information returned");
            }

            String displayName = userResponse.path("displayName").asText("Unknown");
            return new ApiResponse(SC_OK, true,
                    "Confluence connection successful - verified as " + displayName);

        } catch (Exception ex) {
            log.error("Confluence connection test failed: {}", logContext, ex);
            int statusCode = extractStatusCode(ex);
            String errorMessage = extractConfluenceErrorMessage(ex);
            return new ApiResponse(statusCode, false, errorMessage);
        }
    }

    private String extractConfluenceErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("401") || message.contains("Unauthorized")) {
                return "Invalid credentials - authentication failed";
            }
            if (message.contains("403") || message.contains("Forbidden")) {
                return "Invalid credentials - access denied";
            }
            if (message.contains("404")) {
                return "Confluence server not found - verify the base URL";
            }
            if (message.contains("timeout") || message.contains("timed out")) {
                return "Connection timeout - unable to reach Confluence server";
            }
        }
        return "Confluence connection test failed: " + (message != null ? message : "Unknown error");
    }

    private ApiResponse testJiraConnectionAndValidateProjectAccess(IntegrationSecret secret) {
        return validateJiraProjects(
                () -> jiraApiClient.get(jiraApiProperties.getPaths().getProjectSearch(), secret),
                "secret"
        );
    }

    private ApiResponse testJiraConnectionAndValidateProjectAccessBySecretName(String secretName) {
        return validateJiraProjects(
                () -> jiraApiClient.searchProjects(secretName),
                "secretName"
        );
    }

    private ApiResponse testArcGis(ResponseSupplier tokenSupplier) {
        try {
            String token = tokenSupplier.get();
            if (token != null && !token.isBlank()) {
                return new ApiResponse(SC_OK, true, "ArcGIS connection successful");
            }
            return new ApiResponse(SC_INTERNAL_SERVER_ERROR, false,
                    "ArcGIS connection failed - token not retrieved");
        } catch (IntegrationApiException ex) {
            log.error("ArcGIS connection test failed", ex);
            return new ApiResponse(ex.getStatusCode(), false,
                    "ArcGIS connection test failed: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("ArcGIS connection test failed", ex);
            return new ApiResponse(SC_INTERNAL_SERVER_ERROR, false,
                    "ArcGIS connection test failed: " + ex.getMessage());
        }
    }

    private ApiResponse validateJiraProjects(Supplier<JsonNode> projectFetcher, String logContext) {
        try {
            JsonNode projectsResponse = projectFetcher.get();

            if (projectsResponse == null) {
                return new ApiResponse(SC_INTERNAL_SERVER_ERROR, false,
                        "Unable to connect - no response from Jira server");
            }

            // Circuit breaker fallback
            JsonNode errorField = projectsResponse.get("error");
            if (errorField != null && "Service temporarily unavailable".equals(errorField.asText())) {
                return new ApiResponse(HttpServletResponse.SC_SERVICE_UNAVAILABLE, false,
                        "Jira service temporarily unavailable - please try again later");
            }

            // Validate projects
            JsonNode values = projectsResponse.get("values");
            if (values == null || !values.isArray() || values.isEmpty()) {
                return new ApiResponse(HttpServletResponse.SC_FORBIDDEN, false,
                    "Jira credentials are valid, but no projects are accessible. "
                        + "Ensure the account has Jira product access and Browse Projects permission.");
            }

            return new ApiResponse(SC_OK, true,
                    "Jira connection successful - verified access to " + values.size() + " project(s)");

        } catch (Exception ex) {
            log.error("Jira connection test failed: {}", logContext, ex);
            int statusCode = extractStatusCode(ex);
            String errorMessage = extractErrorMessage(ex);
            return new ApiResponse(statusCode, false, errorMessage);
        }
    }

    private int extractStatusCode(Exception ex) {
        if (ex instanceof IntegrationApiException apiEx) {
            return apiEx.getStatusCode();
        }

        // Fallback to parsing message for status indicators
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("401") || message.contains("Unauthorized")) {
                return SC_UNAUTHORIZED;
            }
            if (message.contains("403") || message.contains("Forbidden")) {
                return HttpServletResponse.SC_FORBIDDEN;
            }
            if (message.contains("404")) {
                return HttpServletResponse.SC_NOT_FOUND;
            }
            if (message.contains("timeout") || message.contains("timed out")) {
                return HttpServletResponse.SC_GATEWAY_TIMEOUT;
            }
        }
        return SC_INTERNAL_SERVER_ERROR;
    }

    /**
     * Extract meaningful error message from exceptions.
     */
    private String extractErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message != null) {
            // Check for common authentication errors
            if (message.contains("401") || message.contains("Unauthorized")) {
                return "Invalid credentials - authentication failed";
            }
            if (message.contains("403") || message.contains("Forbidden")) {
                return "Invalid credentials - access denied";
            }
            if (message.contains("404")) {
                return "Jira server not found - verify the base URL";
            }
            if (message.contains("timeout") || message.contains("timed out")) {
                return "Connection timeout - unable to reach Jira server";
            }
        }
        return "Jira connection test failed: " + (message != null ? message : "Unknown error");
    }

    @FunctionalInterface
    private interface ResponseSupplier {
        String get();
    }
}
