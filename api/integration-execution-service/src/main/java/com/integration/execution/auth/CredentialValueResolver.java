package com.integration.execution.auth;

import com.integration.execution.contract.model.AuthCredential;
import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CredentialValueResolver {

    public Map<String, String> resolve(final IntegrationSecret ref) {
        if (ref.getCredentials() == null) {
            throw new IllegalArgumentException("Credential data is required");
        }
        return convertCredentialToMap(ref.getCredentials());
    }

    private Map<String, String> convertCredentialToMap(final AuthCredential credential) {
        Map<String, String> map = new HashMap<>();
        switch (credential.getAuthType()) {
            case BASIC_AUTH -> {
                BasicAuthCredential basicAuth = (BasicAuthCredential) credential;
                putIfNotNull(map, "username", basicAuth.getUsername());
                putIfNotNull(map, "password", basicAuth.getPassword());
            }
            case OAUTH2 -> {
                OAuthClientCredential oauth = (OAuthClientCredential) credential;
                putIfNotNull(map, "clientId", oauth.getClientId());
                putIfNotNull(map, "clientSecret", oauth.getClientSecret());
                putIfNotNull(map, "tokenUrl", oauth.getTokenUrl());
                putIfNotNull(map, "scope", oauth.getScope());
            }
            default -> {
                log.warn("Unsupported credential type: {}", credential.getAuthType());
                throw new IllegalArgumentException(
                        "Unsupported credential type: " + credential.getAuthType());
            }
        }
        return map;
    }

    private void putIfNotNull(final Map<String, String> map, final String key, final String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
