/**
 * Integration Service Types
 * Matches backend com.integration.model.enums.ServiceType
 */
export enum ServiceType {
  /** Atlassian Jira integration */
  JIRA = 'JIRA',
  /** Esri ArcGIS integration */
  ARCGIS = 'ARCGIS',
  /** Atlassian Confluence integration */
  CONFLUENCE = 'CONFLUENCE',
}

/**
 * Service Type metadata for display purposes
 */
export const ServiceTypeMetadata = {
  [ServiceType.JIRA]: {
    displayName: 'Jira',
    description: 'Atlassian Jira integration',
  },
  [ServiceType.ARCGIS]: {
    displayName: 'ArcGIS',
    description: 'Esri ArcGIS integration',
  },
  [ServiceType.CONFLUENCE]: {
    displayName: 'Confluence',
    description: 'Atlassian Confluence integration',
  },
} as const;

/**
 * Connection status types
 */
export enum ConnectionStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  PENDING = 'PENDING',
  UNKNOWN = 'UNKNOWN',
}

/**
 * Authentication types for integrations
 */
export enum AuthenticationType {
  BASIC_AUTH = 'BASIC_AUTH',
  OAUTH2 = 'OAUTH2',
}

/**
 * Integration status types
 */
export enum IntegrationStatus {
  ENABLED = 'ENABLED',
  DISABLED = 'DISABLED',
  PENDING = 'PENDING',
  ERROR = 'ERROR',
}
