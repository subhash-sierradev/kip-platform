import type { IntegrationSecret } from './IntegrationSecret';

/**
 * Request DTO for integration connection operations like testing connection,
 * fetching projects, users, and fields.
 */
export interface IntegrationConnectionRequest {
  /**
   * User-friendly connection name
   */
  name?: string;
  /**
   * Service type (JIRA, ARCGIS, etc.)
   */
  serviceType: string;

  /**
   * Integration secret containing authentication details
   */
  integrationSecret: IntegrationSecret;
}
