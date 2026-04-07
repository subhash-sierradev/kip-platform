import type { ICredentialStrategy } from './ICredentialStrategy';

/**
 * Abstract base class for credential strategies.
 * Provides common validation logic and requires subclasses to implement specifics.
 */
export abstract class BaseCredentialStrategy implements ICredentialStrategy {
  abstract readonly id: string;
  abstract readonly serviceType: string;
  abstract readonly credentialType: string;
  abstract readonly displayName: string;

  abstract getFields(): Array<{
    key: string;
    label: string;
    type: string;
    required: boolean;
    placeholder?: string;
    helpText?: string;
  }>;

  abstract buildTestPayload(data: {
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
   * Common validation logic for all strategies.
   * Validates base URL, connection name, and credential type.
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
  }): string[] {
    const errors: string[] = [];

    // Base URL validation
    if (!data.baseUrl?.trim()) {
      errors.push('Base URL is required');
    }

    // Connection name validation
    if (!data.connectionName?.trim()) {
      errors.push('Connection name is required');
    }

    // Credential type validation
    if (!data.credentialType) {
      errors.push('Credential type is required');
    }

    // Credential-specific validation (delegated to subclasses)
    errors.push(...this.validateCredentials(data));

    return errors;
  }

  /**
   * Check if credentials are configured based on required fields.
   * Subclasses override to implement specific credential checks.
   */
  abstract hasCredentials(data: {
    username?: string;
    password?: string;
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
    scope?: string;
    apiKey?: string;
  }): boolean;

  /**
   * Protected method for subclasses to implement credential-specific validation.
   */
  protected abstract validateCredentials(data: {
    username?: string;
    password?: string;
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
    scope?: string;
    apiKey?: string;
  }): string[];
}
