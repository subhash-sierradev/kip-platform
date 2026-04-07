package com.integration.execution.contract.rest.response.arcgis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a field in an ArcGIS feature layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArcGISFieldDto {
    private String name;
    private String type;
    private String alias;
    private String sqlType;
    private Boolean nullable;
    private Boolean editable;
    private Object domain;
    private Object defaultValue;
    private Integer length;
    private String description;
    private Integer precision;
}