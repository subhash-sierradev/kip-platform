package com.integration.management.entity;

import java.util.UUID;

/**
 * Marker interface for integration entities that support Quartz-based scheduling.
 * Implemented by {@link ArcGISIntegration} and {@link ConfluenceIntegration}.
 */
public interface SchedulableIntegration {

    UUID getId();

    String getTenantId();

    String getName();

    IntegrationSchedule getSchedule();
}
