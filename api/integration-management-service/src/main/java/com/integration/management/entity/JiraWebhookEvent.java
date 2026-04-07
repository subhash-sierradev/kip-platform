package com.integration.management.entity;

import com.integration.execution.contract.model.enums.TriggerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jira_webhook_events", schema = "integration_platform")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JiraWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "triggered_at", nullable = false)
    @Builder.Default
    private Instant triggeredAt = Instant.now();

    @Column(name = "webhook_id", nullable = false)
    @NotNull(message = "Webhook ID is required")
    private String webhookId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @Column(name = "incoming_payload", columnDefinition = "TEXT")
    private String incomingPayload;

    @Column(name = "transformed_payload", columnDefinition = "TEXT")
    private String transformedPayload;

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_status", nullable = false, length = 20)
    @NotNull(message = "Trigger status is required")
    @Builder.Default
    private TriggerStatus status = TriggerStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_attempt", nullable = false)
    @Builder.Default
    private Integer retryAttempt = 0;

    @Column(name = "original_event_id", length = 100)
    private String originalEventId;

    @Column(name = "jira_issue_url", length = 500)
    private String jiraIssueUrl;
}
