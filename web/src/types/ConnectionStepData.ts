/**
 * Generic connection step data types for multi-integration support
 * Used by ConnectionStep component for Jira, ArcGIS, and future integrations
 */

/**
 * Connection step data interface - supports both new and existing connections
 * with dynamic credential types (BASIC_AUTH, OAUTH2, etc.)
 */
export interface ConnectionStepData {
  // Connection method selection
  connectionMethod: 'new' | 'existing';
  existingConnectionId?: string;
  createdConnectionId?: string;

  // New connection fields
  connectionName?: string;
  baseUrl: string; // jiraBaseUrl or arcgisEndpoint or other service base URL
  credentialType: string; // BASIC_AUTH, OAUTH2, API_KEY, etc.

  // BASIC_AUTH credential fields
  username?: string;
  password?: string;

  // OAUTH2 credential fields
  clientId?: string;
  clientSecret?: string;
  tokenUrl?: string;
  scope?: string;

  // API_KEY credential fields
  apiKey?: string;

  // Connection status
  connected: boolean;
}

/**
 * Configuration interface for service-specific behavior
 */
export interface ConnectionStepConfig {
  serviceType: 'JIRA' | 'ARCGIS' | 'CONFLUENCE';
  baseUrlLabel: string;
  baseUrlPlaceholder: string;
  supportsDynamicCredentials: boolean; // true = load from API, false = fixed BASIC_AUTH
  showCredentialTypeSelector: boolean;
  defaultCredentialType?: string;
  requiresConnectionName: boolean;
}

/**
 * Saved connection interface from backend API
 */
export interface SavedConnection {
  id: string;
  name: string;
  secretName?: string;
  baseUrl: string;
  lastConnectionStatus: 'SUCCESS' | 'FAILED' | string;
  lastConnectionTest?: string;
}

/**
 * Credential type option with field schema
 */
export interface CredentialTypeOption {
  value: string; // BASIC_AUTH, OAUTH2, etc.
  label: string; // Display name
  fields: CredentialField[];
}

/**
 * Credential field definition for dynamic form rendering
 */
export interface CredentialField {
  key: string; // username, password, clientId, etc.
  label: string; // Display label
  type: 'text' | 'password' | 'url' | 'email';
  required: boolean;
  placeholder?: string;
  helpText?: string;
}

/**
 * Connection status details for UI display
 */
export interface ConnectionStatusDetails {
  label: string;
  severity: 'success' | 'error' | 'info' | 'neutral';
}
