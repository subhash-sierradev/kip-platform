package com.integration.execution.constants;

/**
 * ArcGIS-specific constants for Integration Execution Service.
 * Includes field names and identifiers used in ArcGIS integrations.
 */
public final class ArcGisConstants {

    // ArcGIS Field Names
    public static final String ARCGIS_FIELD_OBJECTID = "OBJECTID";
    public static final String ARCGIS_FIELD_EXTERNAL_LOCATION_ID = "external_location_id";

    private ArcGisConstants() {
        throw new IllegalStateException("ArcGisConstants is a utility class and cannot be instantiated");
    }
}
