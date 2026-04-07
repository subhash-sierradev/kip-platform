package com.integration.execution.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * OAuth2 authentication response from Kaseware.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String refreshToken;
    private String scope;
    private Instant issuedAt;

    public boolean isValid() {
        return accessToken != null && !accessToken.isEmpty() && issuedAt != null;
    }

    public Instant getExpiryTime() {
        if (issuedAt == null || expiresIn == null) {
            return null;
        }
        return issuedAt.plusSeconds(expiresIn);
    }

    public boolean isExpired() {
        Instant expiryTime = getExpiryTime();
        return expiryTime != null && Instant.now().isAfter(expiryTime);
    }
}
