package com.integration.management.util;

import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.ServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConnectionHashUtil")
class ConnectionHashUtilTest {

    @Test
    @DisplayName("compute should be deterministic for BASIC_AUTH")
    void compute_basicAuth_deterministic() {
        IntegrationSecret secret = IntegrationSecret.builder()
                .baseUrl("https://example.test")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder()
                        .username("user")
                        .password("pw")
                        .build())
                .build();

        String h1 = ConnectionHashUtil.compute("t1", ServiceType.JIRA, secret);
        String h2 = ConnectionHashUtil.compute("t1", ServiceType.JIRA, secret);

        assertThat(h1).isNotBlank();
        assertThat(h1).hasSize(64);
        assertThat(h2).isEqualTo(h1);
    }

    @Test
    @DisplayName("compute should differ when username differs (BASIC_AUTH)")
    void compute_basicAuth_usernameChanges_hashChanges() {
        IntegrationSecret secret1 = IntegrationSecret.builder()
                .baseUrl("https://example.test")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u1").password("pw").build())
                .build();
        IntegrationSecret secret2 = IntegrationSecret.builder()
                .baseUrl("https://example.test")
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u2").password("pw").build())
                .build();

        assertThat(ConnectionHashUtil.compute("t1", ServiceType.JIRA, secret1))
                .isNotEqualTo(ConnectionHashUtil.compute("t1", ServiceType.JIRA, secret2));
    }

    @Test
    @DisplayName("compute should use OAuth clientId when authType=OAUTH2")
    void compute_oauth2_usesClientId() {
        IntegrationSecret secret1 = IntegrationSecret.builder()
                .baseUrl("https://example.test")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(OAuthClientCredential.builder().clientId("c1").clientSecret("s").build())
                .build();
        IntegrationSecret secret2 = IntegrationSecret.builder()
                .baseUrl("https://example.test")
                .authType(CredentialAuthType.OAUTH2)
                .credentials(OAuthClientCredential.builder().clientId("c2").clientSecret("s").build())
                .build();

        assertThat(ConnectionHashUtil.compute("t1", ServiceType.ARCGIS, secret1))
                .isNotEqualTo(ConnectionHashUtil.compute("t1", ServiceType.ARCGIS, secret2));
    }

    @Test
    @DisplayName("compute should throw when secret is null or authType is missing")
    void compute_nullSecretOrMissingAuthType_throws() {
        assertThatThrownBy(() -> ConnectionHashUtil.compute("t", ServiceType.JIRA, null))
                .isInstanceOf(IllegalArgumentException.class);

        IntegrationSecret missingAuthType = IntegrationSecret.builder()
                .baseUrl("https://example.test")
                .credentials(BasicAuthCredential.builder().username("u").password("p").build())
                .build();

        assertThatThrownBy(() -> ConnectionHashUtil.compute("t", ServiceType.JIRA, missingAuthType))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
