package com.integration.management.util;

import com.integration.execution.contract.model.BasicAuthCredential;
import com.integration.execution.contract.model.IntegrationSecret;
import com.integration.execution.contract.model.OAuthClientCredential;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import com.integration.execution.contract.model.enums.ServiceType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for computing a stable, deterministic SHA-256 hash key that uniquely identifies
 * an {@code IntegrationConnection} for a given tenant, service type, auth type, and user
 * identifier (username / clientId).
 *
 * <p>Used by IMS to detect duplicate connections before delegating to IES.
 */
public final class ConnectionHashUtil {

    private ConnectionHashUtil() {
    }

    public static String compute(String tenantId, ServiceType serviceType, IntegrationSecret secret) {
        validateSecret(secret);
        String rawValue = buildRawValue(tenantId, serviceType, secret);
        return sha256Hex(rawValue);
    }

    private static void validateSecret(IntegrationSecret secret) {
        if (secret == null || secret.getAuthType() == null || secret.getCredentials() == null) {
            throw new IllegalArgumentException(
                    "Integration secret, auth type, and credentials must not be null");
        }
    }

    private static String buildRawValue(String tenantId, ServiceType serviceType, IntegrationSecret secret) {
        CredentialAuthType authType = secret.getAuthType();
        String userIdentifier = switch (authType) {
            case BASIC_AUTH -> ((BasicAuthCredential) secret.getCredentials()).getUsername();
            case OAUTH2 -> ((OAuthClientCredential) secret.getCredentials()).getClientId();
        };
        return tenantId + "|" + serviceType.name() + "|" + authType.name() + "|" + userIdentifier;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
