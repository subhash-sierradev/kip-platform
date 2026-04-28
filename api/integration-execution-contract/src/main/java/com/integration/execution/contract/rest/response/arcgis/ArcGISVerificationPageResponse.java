package com.integration.execution.contract.rest.response.arcgis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

// TODO KIP-547 REMOVE — Temporary ArcGIS verification response DTO
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArcGISVerificationPageResponse {

    /** Attribute maps extracted from ArcGIS feature records for this page. */
    private List<Map<String, Object>> features;

    /** Number of records returned in this page. */
    private int fetchedCount;

    /** Offset (record index) used for this request. */
    private int offset;

    /** True when ArcGIS indicates more records exist beyond this page. */
    private boolean exceededTransferLimit;
}
