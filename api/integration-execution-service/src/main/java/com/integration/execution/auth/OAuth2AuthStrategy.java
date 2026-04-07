package com.integration.execution.auth;

import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.integration.execution.constants.HttpConstants.AUTHORIZATION;
import static com.integration.execution.constants.HttpConstants.BEARER;

@Component
@RequiredArgsConstructor
public class OAuth2AuthStrategy implements AuthHeaderStrategy {

    private final CredentialValueResolver resolver;

    @Override
    public CredentialAuthType supports() {
        return CredentialAuthType.OAUTH2;
    }

    @Override
    public void apply(Map<String, String> headers, IntegrationSecret ref) {
        Map<String, String> values = resolver.resolve(ref);

        String token = values.get("accessToken");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("OAuth2 token missing");
        }

        headers.put(AUTHORIZATION, BEARER + token);
    }
}
