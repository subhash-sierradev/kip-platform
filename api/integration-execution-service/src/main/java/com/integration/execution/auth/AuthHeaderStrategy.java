package com.integration.execution.auth;

import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.enums.CredentialAuthType;

import java.util.Map;

public interface AuthHeaderStrategy {
    CredentialAuthType supports();
    void apply(Map<String, String> headers, IntegrationSecret integrationSecret);
}
