import { ArcGISAPIKeyStrategy } from './ArcGISAPIKeyStrategy';
import { ArcGISBasicAuthStrategy } from './ArcGISBasicAuthStrategy';
import { ArcGISOAuth2Strategy } from './ArcGISOAuth2Strategy';
import { ConfluenceBasicAuthStrategy } from './ConfluenceBasicAuthStrategy';
import type { ICredentialStrategy } from './ICredentialStrategy';
import { JiraBasicAuthStrategy } from './JiraBasicAuthStrategy';

/**
 * Singleton registry for credential strategies.
 * Manages strategy registration and lookup for connection handling.
 */
export class CredentialStrategyRegistry {
  private static instance: CredentialStrategyRegistry;
  private strategies: Map<string, ICredentialStrategy> = new Map();

  private constructor() {
    // Register all strategies
    this.register(new JiraBasicAuthStrategy());
    this.register(new ArcGISBasicAuthStrategy());
    this.register(new ArcGISOAuth2Strategy());
    this.register(new ArcGISAPIKeyStrategy());
    this.register(new ConfluenceBasicAuthStrategy());
  }

  /**
   * Get singleton instance of the registry.
   */
  static getInstance(): CredentialStrategyRegistry {
    if (!CredentialStrategyRegistry.instance) {
      CredentialStrategyRegistry.instance = new CredentialStrategyRegistry();
    }
    return CredentialStrategyRegistry.instance;
  }

  /**
   * Register a new credential strategy.
   * @param strategy The strategy to register
   */
  register(strategy: ICredentialStrategy): void {
    this.strategies.set(strategy.id, strategy);
  }

  /**
   * Get strategy by service type and credential type.
   * @param serviceType Service type (e.g., 'JIRA', 'ARCGIS')
   * @param credentialType Credential type (e.g., 'BASIC_AUTH', 'OAUTH2', 'API_KEY')
   * @returns Strategy instance or undefined if not found
   */
  getStrategy(serviceType: string, credentialType: string): ICredentialStrategy | undefined {
    const key = `${serviceType}_${credentialType}`;
    return this.strategies.get(key);
  }

  /**
   * Get all registered strategies for a service type.
   * @param serviceType Service type to filter by (e.g., 'JIRA', 'ARCGIS')
   * @returns Array of strategies for the service type
   */
  getStrategiesForService(serviceType: string): ICredentialStrategy[] {
    return Array.from(this.strategies.values()).filter(
      strategy => strategy.serviceType === serviceType
    );
  }

  /**
   * Get all credential types available for a service.
   * @param serviceType Service type (e.g., 'JIRA', 'ARCGIS')
   * @returns Array of credential type identifiers
   */
  getCredentialTypes(serviceType: string): string[] {
    return this.getStrategiesForService(serviceType).map(strategy => strategy.credentialType);
  }

  /**
   * Check if a strategy exists for the given service and credential type.
   * @param serviceType Service type
   * @param credentialType Credential type
   * @returns True if strategy exists, false otherwise
   */
  hasStrategy(serviceType: string, credentialType: string): boolean {
    return this.getStrategy(serviceType, credentialType) !== undefined;
  }

  /**
   * Get all registered strategy IDs.
   * @returns Array of all strategy identifiers
   */
  getAllStrategyIds(): string[] {
    return Array.from(this.strategies.keys());
  }

  /**
   * Clear all registered strategies (primarily for testing).
   */
  clear(): void {
    this.strategies.clear();
  }
}
