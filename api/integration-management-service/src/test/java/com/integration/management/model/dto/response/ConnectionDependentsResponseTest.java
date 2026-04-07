package com.integration.management.model.dto.response;

import com.integration.execution.contract.model.enums.ServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConnectionDependentsResponse")
class ConnectionDependentsResponseTest {

    @Test
    @DisplayName("builder initializes dependent lists by default")
    void builderInitializesDependentListsByDefault() {
        ConnectionDependentsResponse response = ConnectionDependentsResponse.builder()
                .serviceType(ServiceType.ARCGIS)
                .build();

        assertThat(response.getIntegrations()).isNotNull().isEmpty();
        assertThat(response.hasAnyDependents()).isFalse();
    }

    @Test
    @DisplayName("hasAnyDependents is true when integrations list has items")
    void hasAnyDependentsIsTrueWhenIntegrationsListHasItems() {
        ConnectionDependentItemResponse item = ConnectionDependentItemResponse.builder()
                .id("id-1")
                .name("Name")
                .isEnabled(true)
                .description("desc")
                .build();

        ConnectionDependentsResponse arcgisResponse = ConnectionDependentsResponse.builder()
                .serviceType(ServiceType.ARCGIS)
                .integrations(List.of(item))
                .build();

        ConnectionDependentsResponse jiraResponse = ConnectionDependentsResponse.builder()
                .serviceType(ServiceType.JIRA)
                .integrations(List.of(item))
                .build();

        ConnectionDependentsResponse confluenceResponse = ConnectionDependentsResponse.builder()
                .serviceType(ServiceType.CONFLUENCE)
                .integrations(List.of(item))
                .build();

        assertThat(arcgisResponse.hasAnyDependents()).isTrue();
        assertThat(jiraResponse.hasAnyDependents()).isTrue();
        assertThat(confluenceResponse.hasAnyDependents()).isTrue();
    }

    @Test
    @DisplayName("hasAnyDependents handles null lists safely")
    void hasAnyDependentsHandlesNullListsSafely() {
        ConnectionDependentsResponse response = ConnectionDependentsResponse.builder()
                .serviceType(ServiceType.JIRA)
                .integrations(null)
                .build();

        assertThat(response.hasAnyDependents()).isFalse();
    }
}
