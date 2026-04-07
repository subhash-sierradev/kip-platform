package com.integration.execution.contract.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.integration.execution.contract.model.enums.CredentialAuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationSecret {
    @URL
    @NotBlank(message = "Base URL must be a valid URL")
    private String baseUrl;

    @NotNull(message = "Auth type is required")
    private CredentialAuthType authType;

    @NotNull
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "authType"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BasicAuthCredential.class, name = "BASIC_AUTH"),
        @JsonSubTypes.Type(value = OAuthClientCredential.class, name = "OAUTH2")
    })
    private AuthCredential credentials;
}