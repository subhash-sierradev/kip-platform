package com.integration.execution.contract.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceType {
    JIRA("Jira", "Atlassian Jira integration"),
    ARCGIS("ArcGIS", "Esri ArcGIS integration"),
    CONFLUENCE("Confluence", "Atlassian Confluence integration");

    private final String displayName;
    private final String description;

    public static ServiceType fromCode(String code) {
        for (ServiceType type : ServiceType.values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ServiceType code: " + code);
    }
}