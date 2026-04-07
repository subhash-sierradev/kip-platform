package com.integration.management.config.authorization;

import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.execution.contract.rest.response.IntegrationConnectionResponse;
import com.integration.management.service.IntegrationConnectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static com.integration.management.constants.ManagementSecurityConstants.ROLE_FEATURE_ARCGIS_INTEGRATION;
import static com.integration.management.constants.ManagementSecurityConstants.ROLE_FEATURE_JIRA_WEBHOOK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceTypeAuthorizationHelper")
class ServiceTypeAuthorizationHelperTest {

    @Mock
    private IntegrationConnectionService integrationConnectionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("hasAccessToServiceType returns false when serviceType is null")
    void hasAccessToServiceType_null_returnsFalse() {
        ServiceTypeAuthorizationHelper helper = new ServiceTypeAuthorizationHelper(integrationConnectionService);

        assertThat(helper.hasAccessToServiceType(null)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToServiceType delegates to role checks")
    void hasAccessToServiceType_delegatesToSecurityContextHelper() {
        ServiceTypeAuthorizationHelper helper = new ServiceTypeAuthorizationHelper(integrationConnectionService);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "u",
                "pw",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_FEATURE_JIRA_WEBHOOK))));
        assertThat(helper.hasAccessToServiceType(ServiceType.JIRA)).isTrue();
        assertThat(helper.hasAccessToServiceType(ServiceType.ARCGIS)).isFalse();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "u",
                "pw",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_FEATURE_ARCGIS_INTEGRATION))));
        assertThat(helper.hasAccessToServiceType(ServiceType.JIRA)).isFalse();
        assertThat(helper.hasAccessToServiceType(ServiceType.ARCGIS)).isTrue();
    }

    @Test
    @DisplayName("hasAccessToConnection returns false when connectionId or tenantId is null")
    void hasAccessToConnection_nullArgs_returnsFalse() {
        ServiceTypeAuthorizationHelper helper = new ServiceTypeAuthorizationHelper(integrationConnectionService);

        assertThat(helper.hasAccessToConnection(null, "t")).isFalse();
        assertThat(helper.hasAccessToConnection(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToConnection loads connection and checks access")
    void hasAccessToConnection_success_checksServiceTypeAccess() {
        ServiceTypeAuthorizationHelper helper = new ServiceTypeAuthorizationHelper(integrationConnectionService);

        UUID connectionId = UUID.randomUUID();
        IntegrationConnectionResponse connection = IntegrationConnectionResponse.builder()
                .id(connectionId.toString())
                .tenantId("t")
                .serviceType(ServiceType.ARCGIS)
                .build();

        when(integrationConnectionService.getConnectionById(eq(connectionId), eq("t"))).thenReturn(connection);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "u",
                "pw",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_FEATURE_ARCGIS_INTEGRATION))));
        assertThat(helper.hasAccessToConnection(connectionId, "t")).isTrue();
    }

    @Test
    @DisplayName("hasAccessToConnection returns false when service throws")
    void hasAccessToConnection_serviceThrows_returnsFalse() {
        ServiceTypeAuthorizationHelper helper = new ServiceTypeAuthorizationHelper(integrationConnectionService);

        when(integrationConnectionService.getConnectionById(any(), any())).thenThrow(new RuntimeException("boom"));

        assertThat(helper.hasAccessToConnection(UUID.randomUUID(), "t")).isFalse();
    }
}
