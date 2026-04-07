package com.integration.execution.auth;

import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static com.integration.execution.constants.HttpConstants.AUTHORIZATION;
import static com.integration.execution.constants.HttpConstants.BASIC;

@Component
@RequiredArgsConstructor
public class BasicAuthStrategy implements AuthHeaderStrategy {

    private final CredentialValueResolver resolver;

    @Override
    public CredentialAuthType supports() {
        return CredentialAuthType.BASIC_AUTH;
    }

    @Override
    public void apply(Map<String, String> headers, IntegrationSecret ref) {
        Map<String, String> values = resolver.resolve(ref);

        String username = values.get("username");
        String password = values.get("password");

        if (username == null || password == null) {
            throw new IllegalArgumentException("BASIC_AUTH requires username & password");
        }

        headers.put(
                AUTHORIZATION,
                BASIC + encode(username, password)
        );
    }

    private String encode(String u, String p) {
        return Base64.getEncoder()
                .encodeToString((u + ":" + p).getBytes(StandardCharsets.UTF_8));
    }
}
