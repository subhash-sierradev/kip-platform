package com.integration.execution.auth;

import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectionAuthHeaderBuilderTest {

    @Test
    void buildAuthHeaders_nullSecret_returnsEmptyMap() {
        ConnectionAuthHeaderBuilder builder = new ConnectionAuthHeaderBuilder(List.of());

        assertThat(builder.buildAuthHeaders(null)).isEmpty();
    }

    @Test
    void buildAuthHeaders_matchingStrategy_appliesHeaders() {
        AuthHeaderStrategy strategy = new AuthHeaderStrategy() {
            @Override
            public CredentialAuthType supports() {
                return CredentialAuthType.BASIC_AUTH;
            }

            @Override
            public void apply(Map<String, String> headers, IntegrationSecret ref) {
                headers.put("Authorization", "Basic token");
            }
        };

        ConnectionAuthHeaderBuilder builder = new ConnectionAuthHeaderBuilder(List.of(strategy));
        IntegrationSecret secret = IntegrationSecret.builder().authType(CredentialAuthType.BASIC_AUTH).build();

        Map<String, String> headers = builder.buildAuthHeaders(secret);

        assertThat(headers).containsEntry("Authorization", "Basic token");
    }

    @Test
    void buildAuthHeaders_missingStrategy_throwsIllegalStateException() {
        ConnectionAuthHeaderBuilder builder = new ConnectionAuthHeaderBuilder(List.of());
        IntegrationSecret secret = IntegrationSecret.builder().authType(CredentialAuthType.OAUTH2).build();

        assertThatThrownBy(() -> builder.buildAuthHeaders(secret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No auth strategy for type");
    }
}
