package com.integration.management.entity;

import com.integration.execution.contract.model.enums.FieldTransformationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;


@Entity
@Table(name = "integration_field_mappings", schema = "integration_platform")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "source_field_path", nullable = false, length = 100)
    private String sourceFieldPath;

    @Column(name = "target_field_path", nullable = false, length = 100)
    private String targetFieldPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type", nullable = false, length = 50)
    private FieldTransformationType transformationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transformation_config", columnDefinition = "jsonb")
    private Map<String, Object> transformationConfig;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = false;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;

    @Column(name = "display_order")
    private Integer displayOrder;

}