package com.integration.execution.contract.message;

import com.integration.execution.contract.model.IntegrationFieldMappingDto;
import com.integration.execution.contract.model.enums.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message sent from IMS to IES via RabbitMQ to trigger an ArcGIS integration execution.
 * Contains all data IES needs to run the full pipeline without database access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArcGISExecutionCommand {

    private UUID integrationId;
    private String integrationName;
    private String connectionSecretName;
    private String arcgisEndpointUrl;
    private String itemType;
    private String itemSubtype;
    private String dynamicDocumentType;
    private List<IntegrationFieldMappingDto> fieldMappings;

    /**
     * Tracking ID of the IntegrationJobExecution record created by IMS before publishing.
     */
    private UUID jobExecutionId;
    private String tenantId;
    private String triggeredByUser;
    private TriggerType triggeredBy;

    /**
     * Execution window pre-computed by IMS.
     */
    private Instant windowStart;
    private Instant windowEnd;

    /**
     * True when the command should build a monitoring-data-based Confluence page.
     */
    private boolean monitoringReport;

    /**
     * Monitoring data paging inputs computed by IMS for manual runs.
     */
    private Integer monitoringOffset;
    private Integer monitoringLimit;
}
