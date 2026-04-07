import type { ConnectionStatus } from './IntegrationConnection';

/**
 * Response returned by IES for both test-and-create and re-test existing connection operations.
 * Replaces ServiceConnectionResponse for the /{connectionId}/test endpoint.
 */
export interface ConnectionTestResponse {
  /** Whether the connection test was successful */
  success?: boolean;
  /** HTTP status code of the underlying connectivity test */
  statusCode?: number;
  /** Human-readable result message */
  message?: string;
  /**
   * Azure Key Vault secret name.
   * Populated only on a successful new-connection test; null for failures or re-tests of existing connections.
   */
  secretName?: string;
  /** Enum result of the connection test */
  connectionStatus?: ConnectionStatus;
  /** ISO 8601 timestamp of when the test was performed */
  lastConnectionTest?: string;
}
