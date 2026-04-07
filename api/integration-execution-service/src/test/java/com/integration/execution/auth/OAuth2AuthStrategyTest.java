package com.integration.execution.auth;

import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthStrategyTest {

    @Mock
    private CredentialValueResolver resolver;

    @Test
    void apply_withToken_setsBearerHeader() {
        OAuth2AuthStrategy strategy = new OAuth2AuthStrategy(resolver);
        IntegrationSecret secret = IntegrationSecret.builder().build();
        Map<String, String> headers = new HashMap<>();
        when(resolver.resolve(secret)).thenReturn(Map.of("accessToken", "abc"));

        strategy.apply(headers, secret);

        assertThat(headers.get("Authorization")).isEqualTo("Bearer abc");
        assertThat(strategy.supports()).isEqualTo(CredentialAuthType.OAUTH2);
    }

    @Test
    void apply_withoutToken_throwsIllegalArgumentException() {
        OAuth2AuthStrategy strategy = new OAuth2AuthStrategy(resolver);
        IntegrationSecret secret = IntegrationSecret.builder().build();
        when(resolver.resolve(secret)).thenReturn(Map.of());

        assertThatThrownBy(() -> strategy.apply(new HashMap<>(), secret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OAuth2 token missing");
    }
}
