package com.integration.management.entity;

import com.integration.execution.contract.model.enums.ConnectionStatus;
import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.entity.base.UuidBaseEntity;
import com.integration.execution.contract.model.enums.FetchMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "integration_connections", schema = "integration_platform")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IntegrationConnection extends UuidBaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "secret_name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Connection key is required")
    private String secretName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fetch_mode", nullable = false, length = 20)
    @NotNull(message = "Fetch mode is required")
    @Builder.Default
    private FetchMode fetchMode = FetchMode.GET;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_connection_status", length = 50)
    private ConnectionStatus lastConnectionStatus;

    @Column(name = "last_connection_message", columnDefinition = "TEXT")
    private String lastConnectionMessage;

    @Column(name = "last_connection_test")
    private Instant lastConnectionTest;

    @Column(name = "connection_hash_key", nullable = false, length = 100)
    private String connectionHashKey;
}
