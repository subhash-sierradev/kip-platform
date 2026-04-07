package com.integration.management.entity;

import com.integration.management.entity.base.UuidBaseEntity;
import com.integration.execution.contract.model.enums.ConfigValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "site_configs", schema = "integration_platform")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SiteConfig extends UuidBaseEntity {

    @Column(name = "config_key", nullable = false, length = 100)
    @NotBlank(message = "Config key is required")
    @Size(max = 100, message = "Key must be at most {max} characters long")
    private String configKey;              // Unique key for the configuration

    @Column(name = "config_value", length = 500)
    @NotBlank(message = "Config value is required")
    @Size(max = 500, message = "Value must be at most {max} characters long")
    private String configValue;            // Raw value stored as text (string/number/boolean/json)

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    @NotNull(message = "Value type is required")
    private ConfigValueType type;    // string | number | boolean

    @Column(name = "description", length = 200)
    @Size(max = 200, message = "Description must be at most {max} characters long")
    private String description;
}
