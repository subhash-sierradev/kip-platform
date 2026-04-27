package com.integration.management.entity;

import com.integration.management.entity.base.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuration entity for a scheduled Confluence daily report integration.
 * Persists all settings needed to produce and publish an Aggregated Daily Report page.
 */
@Entity
@Table(name = "confluence_integrations", schema = "integration_platform")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfluenceIntegration extends UuidBaseEntity implements SchedulableIntegration {

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Confluence integration name is required")
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "document_item_type", nullable = false, length = 100)
    @NotBlank(message = "Document item type is required")
    @Builder.Default
    private String documentItemType = "DOCUMENT";

    @Column(name = "document_item_subtype", nullable = false, length = 100)
    @NotBlank(message = "Document item subtype is required")
    private String documentItemSubtype;

    @Column(name = "dynamic_document_type", length = 100)
    private String dynamicDocumentType;

    @Column(name = "report_name_template", nullable = false, length = 255)
    @NotBlank(message = "Report name template is required")
    private String reportNameTemplate;

    @Column(name = "confluence_space_key", nullable = false, length = 100)
    @NotBlank(message = "Confluence space key is required")
    private String confluenceSpaceKey;

    @Column(name = "confluence_space_folder_key", length = 100, nullable = false)
    @NotBlank(message = "Confluence space folder key is required")
    @Builder.Default
    private String confluenceSpaceKeyFolderKey = "ROOT";

    @Column(name = "include_table_of_contents", nullable = false)
    @Builder.Default
    private Boolean includeTableOfContents = true;

    /**
     * BCP-47 language code of the FreeMarker template language (default "en").
     * Combined with {@code languages} to determine whether the Translation API
     * should be called before publishing the Confluence page.
     */
    @Column(name = "source_language", nullable = false, length = 10)
    @Builder.Default
    private String sourceLanguage = "en";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "confluence_integration_languages",
        schema = "integration_platform",
        joinColumns = @JoinColumn(name = "integration_id"),
        inverseJoinColumns = @JoinColumn(name = "language_code")
    )
    @Builder.Default
    private List<Language> languages = new ArrayList<>();

    @Column(name = "connection_id", nullable = false)
    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "schedule_id")
    private IntegrationSchedule schedule;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;
}
