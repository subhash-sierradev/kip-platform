/**
 * Interface for credential strategies.
 * Each integration service + credential type combination implements this interface.
 */
export interface ICredentialStrategy {
  /** Unique identifier: {service}_{credentialType} (e.g., 'JIRA_BASIC_AUTH') */
  readonly id: string;

  /** Service type this strategy applies to (e.g., 'JIRA', 'ARCGIS', 'SALESFORCE') */
  readonly serviceType: string;

  /** Credential type this strategy handles (e.g., 'BASIC_AUTH', 'OAUTH2', 'API_KEY') */
  readonly credentialType: string;

  /** Display name for this authentication method */
  readonly displayName: string;

  /**
   * Get field definitions for this credential type.
   * Each field defines the form structure (key, label, type, required).
   */
  getFields(): Array<{
    key: string;
    label: string;
    type: string;
    required: boolean;
    placeholder?: string;
    helpText?: string;
  }>;

  /**
   * Build test connection payload from connection data.
   * Transforms UI form data into API-compatible structure.
   */
  buildTestPayload(data: {
    baseUrl: string;
    connectionName?: string;
    credentialType: string;
    username?: string;
    password?: string;
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
    scope?: string;
    apiKey?: string;
  }): unknown;

  /**
   * Validate connection data completeness.
   * Returns validation errors or empty array if valid.
   */
  validate(data: {
    baseUrl: string;
    connectionName?: string;
    credentialType: string;
    username?: string;
    password?: string;
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
    scope?: string;
    apiKey?: string;
  }): string[];

  /**
   * Check if credentials are configured (all required fields filled).
   */
  hasCredentials(data: {
    username?: string;
    password?: string;
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
    scope?: string;
    apiKey?: string;
  }): boolean;
}
