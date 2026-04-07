package com.integration.execution.auth;

import com.integration.execution.contract.model.IntegrationSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConnectionAuthHeaderBuilder {

    private final List<AuthHeaderStrategy> strategies;

    public Map<String, String> buildAuthHeaders(IntegrationSecret integrationSecret) {
        Map<String, String> headers = new HashMap<>();
        if (integrationSecret == null) {
            return headers;
        }

        AuthHeaderStrategy strategy = strategies.stream()
                .filter(s -> s.supports() == integrationSecret.getAuthType())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No auth strategy for type: " + integrationSecret.getAuthType()));

        strategy.apply(headers, integrationSecret);
        return headers;
    }
}
