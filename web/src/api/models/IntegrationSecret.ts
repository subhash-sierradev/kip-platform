import type { AuthCredential } from './AuthCredential';

/**
 * Integration secret containing base URL and authentication details
 */
export interface IntegrationSecret {
  /**
   * Base URL for the service (must be valid URL)
   */
  baseUrl: string;

  /**
   * Authentication type
   */
  authType: string;

  /**
   * Authentication credentials
   */
  credentials: AuthCredential;
}
