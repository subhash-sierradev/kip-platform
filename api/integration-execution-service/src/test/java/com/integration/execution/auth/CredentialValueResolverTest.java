package com.integration.execution.auth;

import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialValueResolverTest {

    private final CredentialValueResolver resolver = new CredentialValueResolver();

    @Test
    void resolve_basicAuth_returnsUsernameAndPassword() {
        IntegrationSecret secret = IntegrationSecret.builder()
                .authType(CredentialAuthType.BASIC_AUTH)
                .credentials(BasicAuthCredential.builder().username("u").password("p").build())
                .build();

        Map<String, String> values = resolver.resolve(secret);

        assertThat(values).containsEntry("username", "u").containsEntry("password", "p");
    }

    @Test
    void resolve_oauth2_returnsOAuthFields() {
        IntegrationSecret secret = IntegrationSecret.builder()
                .authType(CredentialAuthType.OAUTH2)
                .credentials(OAuthClientCredential.builder()
                        .clientId("id")
                        .clientSecret("secret")
                        .tokenUrl("https://token")
                        .scope("read")
                        .build())
                .build();

        Map<String, String> values = resolver.resolve(secret);

        assertThat(values)
                .containsEntry("clientId", "id")
                .containsEntry("clientSecret", "secret")
                .containsEntry("tokenUrl", "https://token")
                .containsEntry("scope", "read");
    }

    @Test
    void resolve_nullCredential_throwsIllegalArgumentException() {
        IntegrationSecret secret = IntegrationSecret.builder().build();

        assertThatThrownBy(() -> resolver.resolve(secret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credential data is required");
    }
}
