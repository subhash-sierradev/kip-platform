package com.integration.execution.exception;

/**
 * Custom exception for Azure Key Vault operations.
 * Extends IntegrationBaseException to maintain consistent error handling patterns.
 */
public class AzureKeyVaultException extends IntegrationBaseException {

    public AzureKeyVaultException(String message) {
        super(message);
    }

    public AzureKeyVaultException(String message, Throwable cause) {
        super(message, cause);
    }

    public static AzureKeyVaultException credentialNotFound(String secretKey) {
        return new AzureKeyVaultException("Credential not found for key: " + secretKey);
    }

    public static AzureKeyVaultException storeOperationFailed(String secretKey, Throwable cause) {
        return new AzureKeyVaultException("Failed to store credential with key: " + secretKey, cause);
    }

    public static AzureKeyVaultException retrieveOperationFailed(String secretKey, Throwable cause) {
        return new AzureKeyVaultException("Failed to retrieve credential with key: " + secretKey, cause);
    }

    public static AzureKeyVaultException deleteOperationFailed(String secretKey, Throwable cause) {
        return new AzureKeyVaultException("Failed to delete credential with key: " + secretKey, cause);
    }

    public static AzureKeyVaultException serializationFailed(Throwable cause) {
        return new AzureKeyVaultException("Failed to serialize credential data", cause);
    }

    public static AzureKeyVaultException deserializationFailed(String secretKey, Throwable cause) {
        return new AzureKeyVaultException("Failed to deserialize credential data for key: " + secretKey, cause);
    }

    public static AzureKeyVaultException listOperationFailed(Throwable cause) {
        return new AzureKeyVaultException("Failed to list credentials in Azure Key Vault", cause);
    }
}
