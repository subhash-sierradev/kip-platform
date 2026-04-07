package com.integration.execution.client;

import com.azure.core.http.rest.PagedIterable;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.integration.execution.constants.HttpConstants.JSON_CONTENT_TYPE;

/**
 * Client for Azure Key Vault credential operations.
 * Wraps Azure's SecretClient with additional logging and credential-focused operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureKeyVaultSecretClient {

    private final SecretClient secretClient;

    public void createJsonCredential(String name, String value) {
        log.info("Creating/updating credential in Azure Key Vault: {}", name);
        KeyVaultSecret secret = new KeyVaultSecret(name, value);
        SecretProperties properties = secret.getProperties();
        properties.setContentType(JSON_CONTENT_TYPE);
        properties.setTags(Map.of("type", "json-credential"));
        secretClient.setSecret(secret);
        log.info("Successfully created/updated credential: {}", name);
    }

    public KeyVaultSecret get(String name) {
        log.info("Retrieving credential from Azure Key Vault: {}", name);
        KeyVaultSecret result = secretClient.getSecret(name);
        log.info("Successfully retrieved credential: {}", name);
        return result;
    }

    public void delete(String name) {
        log.info("Soft-deleting credential from Azure Key Vault: {}", name);
        secretClient.beginDeleteSecret(name).waitForCompletion();
        log.info("Successfully soft-deleted credential: {}", name);
    }

    /**
     * Permanently purges a deleted credential from Azure Key Vault.
     * This operation is irreversible.
     */
    public void purge(String name) {
        log.debug("Permanently purging credential from Azure Key Vault: {}", name);
        secretClient.purgeDeletedSecret(name);
        log.debug("Successfully purged credential: {}", name);
    }

    public List<String> listSecretNames() {
        log.info("Listing all credential names in Azure Key Vault");
        PagedIterable<SecretProperties> secrets = secretClient.listPropertiesOfSecrets();
        List<String> secretNames = secrets.stream()
                .map(SecretProperties::getName)
                .toList();
        log.info("Successfully listed {} credential names", secretNames.size());
        return secretNames;
    }
}