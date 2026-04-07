package com.integration.management.entity;

import com.integration.execution.contract.model.enums.CredentialAuthType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credential_types", schema = "integration_platform")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialType {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "credential_auth_type", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Credential type code is required")
    private CredentialAuthType credentialAuthType;

    @Column(name = "display_name", nullable = false, length = 50)
    @NotBlank(message = "Credential type name is required")
    private String displayName; // BASIC_AUTH, API_KEY, etc.

    @Column(name = "is_enabled", nullable = false)
    @NotNull(message = "Enabled status is required")
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * Fields required to build secrets (stored in Azure Key Vault as JSON)
     * Example: ["clientId", "clientSecret", "tenantId"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_fields", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> requiredFields = new ArrayList<>();
}
