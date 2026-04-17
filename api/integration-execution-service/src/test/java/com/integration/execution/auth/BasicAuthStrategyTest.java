package com.integration.execution.auth;

import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BasicAuthStrategy")
class BasicAuthStrategyTest {

    @Mock
    private CredentialValueResolver resolver;

    @Test
    void apply_withValidCredentials_setsBasicAuthorizationHeader() {
        BasicAuthStrategy strategy = new BasicAuthStrategy(resolver);
        IntegrationSecret secret = IntegrationSecret.builder().build();
        Map<String, String> headers = new HashMap<>();
        when(resolver.resolve(secret)).thenReturn(Map.of("username", "user", "password", "pass"));

        strategy.apply(headers, secret);

        assertThat(headers.get("Authorization")).startsWith("Basic ");
        assertThat(strategy.supports()).isEqualTo(CredentialAuthType.BASIC_AUTH);
    }

    @Test
    @DisplayName("encodes username:password correctly in Base64")
    void apply_encodesCredentialsCorrectly() {
        BasicAuthStrategy strategy = new BasicAuthStrategy(resolver);
        IntegrationSecret secret = IntegrationSecret.builder().build();
        when(resolver.resolve(secret)).thenReturn(Map.of("username", "alice", "password", "s3cr3t"));

        Map<String, String> headers = new HashMap<>();
        strategy.apply(headers, secret);

        String expected = "Basic " + Base64.getEncoder().encodeToString("alice:s3cr3t".getBytes());
        assertThat(headers.get("Authorization")).isEqualTo(expected);
    }

    @Test
    void apply_missingUsernameOrPassword_throwsIllegalArgumentException() {
        BasicAuthStrategy strategy = new BasicAuthStrategy(resolver);
        IntegrationSecret secret = IntegrationSecret.builder().build();
        when(resolver.resolve(secret)).thenReturn(Map.of("username", "user"));

        assertThatThrownBy(() -> strategy.apply(new HashMap<>(), secret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BASIC_AUTH requires username & password");
    }

    @Test
    @DisplayName("missing username alone also throws")
    void apply_missingUsername_throwsIllegalArgumentException() {
        BasicAuthStrategy strategy = new BasicAuthStrategy(resolver);
        IntegrationSecret secret = IntegrationSecret.builder().build();
        when(resolver.resolve(secret)).thenReturn(Map.of("password", "pass"));

        assertThatThrownBy(() -> strategy.apply(new HashMap<>(), secret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BASIC_AUTH requires username & password");
    }

    @Test
    @DisplayName("empty credentials map throws")
    void apply_emptyCredentials_throwsIllegalArgumentException() {
        BasicAuthStrategy strategy = new BasicAuthStrategy(resolver);
        IntegrationSecret secret = IntegrationSecret.builder().build();
        when(resolver.resolve(secret)).thenReturn(Map.of());

        assertThatThrownBy(() -> strategy.apply(new HashMap<>(), secret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BASIC_AUTH requires username & password");
    }
}
