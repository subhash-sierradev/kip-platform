package com.integration.execution.service;

import com.integration.execution.contract.model.IntegrationSecret;

import java.util.List;

/**
 * Vault service interface for credential storage and retrieval operations.
 * Provides a unified API for different vault implementations (Azure Key Vault, PostgreSQL).
 */
public interface VaultService {

    void saveSecret(String secretName, IntegrationSecret secret);

    IntegrationSecret getSecret(String secretName);

    void deleteSecret(String secretName);

    List<String> listSecrets();
}