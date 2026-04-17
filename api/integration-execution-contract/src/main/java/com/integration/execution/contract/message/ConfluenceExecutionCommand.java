package com.integration.execution.contract.message;

import com.integration.execution.contract.model.enums.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message sent from IMS to IES via RabbitMQ to trigger a Confluence integration execution.
 * Contains all data IES needs to run the full pipeline without database access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfluenceExecutionCommand {

    private UUID integrationId;
    private String integrationName;
    private String connectionSecretName;

    // Document selector
    private String itemType;
    private String itemSubtype;
    private String dynamicDocumentType;

    // Language codes for translation blocks (e.g. ["en", "ja"])
    private List<String> languageCodes;

    // Confluence page config
    private String confluenceSpaceKey;
    private String confluenceSpaceKeyFolderKey;
    private String reportNameTemplate;
    private boolean includeTableOfContents;

    /** Business timezone for timestamp conversion (fallback if Confluence user timezone unavailable). */
    private String businessTimeZone;

    /** Tracking ID of the IntegrationJobExecution record created by IMS before publishing. */
    private UUID jobExecutionId;
    private String tenantId;
    private String triggeredByUser;
    private TriggerType triggeredBy;

    /** Execution window pre-computed by IMS (previous calendar day UTC). */
    private Instant windowStart;
    private Instant windowEnd;
}
