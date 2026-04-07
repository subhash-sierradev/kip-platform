package com.integration.management.entity;

import com.integration.execution.contract.model.enums.AuditActivity;
import com.integration.execution.contract.model.enums.AuditResult;
import com.integration.execution.contract.model.enums.EntityType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", schema = "integration_platform")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    @NotBlank(message = "Entity ID is required")
    private String entityId;

    @Column(name = "entity_name", length = 100)
    @NotBlank(message = "Entity name is required")
    private String entityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    @NotNull(message = "Audit action is required")
    private AuditActivity action;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 20)
    private AuditResult result;

    @Column(name = "performed_by", nullable = false, length = 255)
    @NotBlank(message = "Performed by is required")
    private String performedBy;

    @Column(name = "tenant_id", nullable = false, length = 100)
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @Column(name = "client_ip_address", length = 45)
    private String clientIpAddress;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
