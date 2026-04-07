package com.integration.management.entity;

import com.integration.execution.contract.model.enums.JiraDataType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jira_field_mappings", schema = "integration_platform")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JiraFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_webhook_id", nullable = false)
    @NotNull(message = "Jira webhook is required")
    @ToString.Exclude
    private JiraWebhook jiraWebhook;

    @Column(name = "jira_field_id", nullable = false, length = 100)
    @NotBlank(message = "Jira field ID is required")
    private String jiraFieldId;

    @Column(name = "jira_field_name", length = 255)
    private String jiraFieldName;

    @Column(name = "display_label", length = 255)
    private String displayLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    @NotNull(message = "Data type is required")
    private JiraDataType dataType;

    @Column(name = "template", columnDefinition = "TEXT")
    private String template;

    @Column(name = "required", nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
