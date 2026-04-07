package com.integration.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Generic DTO for Kaseware monitoring data.
 * Stable fields are typed; all variable/dynamic data goes into {@code attributes}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KwMonitoringDocument {
    private String id;
    private String title;
    private String body;
    private long createdTimestamp;
    private long updatedTimestamp;
    private String dynamicFormDefinitionId;
    private String dynamicFormDefinitionName;
    private int dynamicFormVersionNumber;
    private String tenantId;
    private Map<String, Object> attributes;
}
