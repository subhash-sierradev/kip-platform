/**
 * Helper utilities for connection testing
 */

import type { AuthCredential } from '@/api/models/AuthCredential';
import type { IntegrationSecret } from '@/api/models/IntegrationSecret';
import { CredentialStrategyRegistry } from '@/strategies';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

/**
 * Build integration secret object for connection testing
 */
export function buildIntegrationSecret(
  serviceType: string,
  connectionData: ConnectionStepData
): IntegrationSecret {
  const registry = CredentialStrategyRegistry.getInstance();
  const strategy = registry.getStrategy(serviceType, connectionData.credentialType);

  if (!strategy) {
    throw new Error(`No strategy found for ${serviceType} with ${connectionData.credentialType}`);
  }

  const payload = strategy.buildTestPayload(connectionData);

  return {
    baseUrl: connectionData.baseUrl,
    authType: connectionData.credentialType,
    credentials: (payload as { credentials: AuthCredential }).credentials,
  };
}

/**
 * Validate connection data using strategy pattern
 */
export function validateConnectionData(
  serviceType: string,
  connectionData: ConnectionStepData
): { valid: boolean; message: string } {
  const registry = CredentialStrategyRegistry.getInstance();
  const strategy = registry.getStrategy(serviceType, connectionData.credentialType);

  if (!strategy) {
    return {
      valid: false,
      message: `No strategy found for ${serviceType} with ${connectionData.credentialType}`,
    };
  }

  const errors = strategy.validate(connectionData);

  if (errors.length > 0) {
    return { valid: false, message: errors[0] };
  }

  return { valid: true, message: '' };
}
