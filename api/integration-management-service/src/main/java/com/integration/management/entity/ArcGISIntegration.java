package com.integration.management.entity;

import com.integration.management.entity.base.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "arcgis_integrations", schema = "integration_platform")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArcGISIntegration extends UuidBaseEntity implements SchedulableIntegration {

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "ArcGIS Integration name is required")
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(name = "description", columnDefinition = "TEXT", length = 500)
    private String description;

    @Column(name = "item_type", nullable = false, length = 100)
    @NotBlank(message = "Item type is required")
    private String itemType;

    @Column(name = "item_subtype", nullable = false, length = 100)
    private String itemSubtype;

    @Column(name = "dynamic_document_type", length = 100)
    private String dynamicDocumentType;

    @Column(name = "connection_id", nullable = false)
    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private IntegrationSchedule schedule;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;
}
