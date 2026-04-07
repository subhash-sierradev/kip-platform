package com.integration.execution.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthClientCredential implements AuthCredential {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String tokenUrl;

    private String scope;

    @JsonIgnore
    @Override
    public CredentialAuthType getAuthType() {
        return CredentialAuthType.OAUTH2;
    }
}