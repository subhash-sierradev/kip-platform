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
public class BasicAuthCredential implements AuthCredential {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @JsonIgnore
    @Override
    public CredentialAuthType getAuthType() {
        return CredentialAuthType.BASIC_AUTH;
    }
}