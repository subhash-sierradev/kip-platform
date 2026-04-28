package com.integration.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// TODO KIP-547 REMOVE — Temporary ArcGIS feature service verification configuration
@Data
@Component
@ConfigurationProperties(prefix = "arcgis.verification")
public class ArcGISVerificationProperties {

    /** Base URL of the ArcGIS feature service (without trailing /0/query). */
    private String featureServiceUrl;

    /** OAuth2 client ID for the ArcGIS application. */
    private String clientId;

    /** OAuth2 client secret for the ArcGIS application. */
    private String clientSecret;

    /** OAuth2 token endpoint URL (e.g. https://www.arcgis.com/sharing/rest/oauth2/token). */
    private String tokenUrl;
}
