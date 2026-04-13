/**
 * Declarative configuration for the ConnectionStep component.
 * Contains service-specific configuration data and a small helper to select
 * the appropriate config by service type.
 * Credential handling logic remains delegated to strategy classes.
 */

import { ServiceType } from '@/api/models/enums';
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

export function getConnectionStepConfig(serviceType: ServiceType): ConnectionStepConfig {
  if (serviceType === ServiceType.ARCGIS) return ARCGIS_CONNECTION_CONFIG;
  if (serviceType === ServiceType.CONFLUENCE) return CONFLUENCE_CONNECTION_CONFIG;
  return JIRA_CONNECTION_CONFIG;
}
