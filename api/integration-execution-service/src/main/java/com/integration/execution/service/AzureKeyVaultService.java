package com.integration.execution.service;

import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.execution.client.AzureKeyVaultSecretClient;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.exception.AzureKeyVaultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AzureKeyVaultService implements VaultService {

    private final AzureKeyVaultSecretClient azureKeyVaultSecretClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @CacheEvict(value = "credentialsCache", key = "#secretName")
    public void saveSecret(String secretName, IntegrationSecret secret) {
        log.info("Saving secret in Azure Key Vault [secretName={}]", secretName);
        try {
            String jsonValue = objectMapper.writeValueAsString(secret);
            azureKeyVaultSecretClient.createJsonCredential(secretName, jsonValue);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize IntegrationSecret [secretName={}]", secretName, e);
            throw AzureKeyVaultException.serializationFailed(e);
        } catch (Exception e) {
            log.error("Failed to saveSecret secret in Azure Key Vault [secretName={}]", secretName, e);
            throw AzureKeyVaultException.storeOperationFailed(secretName, e);
        }
    }

    @Override
    @Cacheable(value = "credentialsCache", key = "#secretName", unless = "#result == null")
    public IntegrationSecret getSecret(String secretName) {
        log.info("Fetching secret from Azure Key Vault [secretName={}]", secretName);
        try {
            KeyVaultSecret keyVaultSecret = azureKeyVaultSecretClient.get(secretName);
            if (keyVaultSecret == null || keyVaultSecret.getValue() == null) {
                log.error("Secret not found in Azure Key Vault [secretName={}]", secretName);
                throw AzureKeyVaultException.credentialNotFound(secretName);
            }
            return objectMapper.readValue(keyVaultSecret.getValue(), IntegrationSecret.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize secret value [secretName={}]", secretName, e);
            throw AzureKeyVaultException.deserializationFailed(secretName, e);
        } catch (AzureKeyVaultException e) {
            throw e; // rethrow domain exception
        } catch (Exception e) {
            log.error("Failed to retrieve secret from Azure Key Vault [secretName={}]", secretName, e);
            throw AzureKeyVaultException.retrieveOperationFailed(secretName, e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "credentialsCache", key = "#secretName")
    public void deleteSecret(String secretName) {
        log.info("Deleting secret from Azure Key Vault [secretName={}]", secretName);
        try {
            azureKeyVaultSecretClient.delete(secretName); // Soft deleteSecret
        } catch (Exception e) {
            log.error("Failed to deleteSecret secret from Azure Key Vault [secretName={}]", secretName, e);
            throw AzureKeyVaultException.deleteOperationFailed(secretName, e);
        }
    }

    @Override
    public List<String> listSecrets() {
        log.info("Listing secrets from Azure Key Vault");
        try {
            return azureKeyVaultSecretClient.listSecretNames();
        } catch (Exception e) {
            log.error("Failed to list secrets from Azure Key Vault", e);
            throw AzureKeyVaultException.listOperationFailed(e);
        }
    }
}
