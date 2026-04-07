/**
 * Pure data configuration for ConnectionStep component.
 * Service-specific configurations with NO business logic.
 * All credential handling logic moved to strategy classes.
 */

import type { ConnectionStepConfig } from '@/types/ConnectionStepData';

/**
 * Jira connection step configuration - Pure data only.
 * Logic delegated to JiraBasicAuthStrategy.
 */
export const JIRA_CONNECTION_CONFIG: ConnectionStepConfig = {
  serviceType: 'JIRA',
  baseUrlLabel: 'Jira Base URL',
  baseUrlPlaceholder: 'https://your-domain.atlassian.net',
  supportsDynamicCredentials: false,
  showCredentialTypeSelector: false,
  defaultCredentialType: 'BASIC_AUTH',
  requiresConnectionName: true,
};

/**
 * ArcGIS connection step configuration - Pure data only.
 * Logic delegated to ArcGIS* strategy classes.
 * Note: ArcGIS integrations use OAuth2 authentication exclusively.
 */
export const ARCGIS_CONNECTION_CONFIG: ConnectionStepConfig = {
  serviceType: 'ARCGIS',
  baseUrlLabel: 'ArcGIS Endpoint',
  baseUrlPlaceholder: 'https://services.arcgis.com/...',
  supportsDynamicCredentials: false,
  showCredentialTypeSelector: false,
  defaultCredentialType: 'OAUTH2',
  requiresConnectionName: true,
};

/**
 * Confluence connection step configuration - Pure data only.
 * Logic delegated to ConfluenceBasicAuthStrategy.
 */
export const CONFLUENCE_CONNECTION_CONFIG: ConnectionStepConfig = {
  serviceType: 'CONFLUENCE',
  baseUrlLabel: 'Confluence Site URL',
  baseUrlPlaceholder: 'https://your-org.atlassian.net',
  supportsDynamicCredentials: false,
  showCredentialTypeSelector: false,
  defaultCredentialType: 'BASIC_AUTH',
  requiresConnectionName: true,
};
