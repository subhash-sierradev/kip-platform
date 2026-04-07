package com.integration.management.entity;

import com.integration.management.entity.base.StringBaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jira_webhooks", schema = "integration_platform")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JiraWebhook extends StringBaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Webhook name is required")
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(name = "description", columnDefinition = "TEXT", length = 500)
    private String description;

    @Column(name = "webhook_url", nullable = false, length = 255)
    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    @Column(name = "connection_id", nullable = false)
    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @Column(name = "sample_payload", nullable = false, columnDefinition = "TEXT")
    private String samplePayload;

    @OneToMany(mappedBy = "jiraWebhook", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<JiraFieldMapping> jiraFieldMappings = new ArrayList<>();

    @Column(name = "is_enabled", nullable = false)
    @NotNull(message = "Enabled status is required")
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * Touch method to force JPA to detect entity change and increment version.
     * Used when only child entities (field mappings) are modified.
     */
    public void touch() {
        // Force JPA to detect change and increment version
        this.setLastModifiedDate(Instant.now());
    }
}
