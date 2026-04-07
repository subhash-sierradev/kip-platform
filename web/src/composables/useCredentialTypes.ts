/**
 * Generic composable for fetching credential types by service type
 * Uses Strategy Pattern to get field definitions from registered strategies
 */

import { type Ref, ref } from 'vue';

import { CredentialTypeService } from '@/api/services/CredentialTypeService';
import { CredentialStrategyRegistry } from '@/strategies';
import type { CredentialField, CredentialTypeOption } from '@/types/ConnectionStepData';

/**
 * Fetch credential types for a specific service type
 * @param serviceType - 'JIRA' or 'ARCGIS'
 * @param supportsDynamic - If false, returns fixed BASIC_AUTH only
 */
export function useCredentialTypes(serviceType: string, supportsDynamic: boolean = true) {
  const credentialTypes = ref<CredentialTypeOption[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const registry = CredentialStrategyRegistry.getInstance();

  /**
   * Fetch credential types using strategy pattern
   */
  const fetchCredentialTypes = async (): Promise<void> => {
    // For services with fixed credential types (e.g., Jira), get from registry
    if (!supportsDynamic) {
      const strategies = registry.getStrategiesForService(serviceType);
      credentialTypes.value = strategies.map(strategy => ({
        value: strategy.credentialType,
        label: strategy.displayName,
        fields: strategy.getFields() as CredentialField[],
      }));
      return;
    }

    // For services with dynamic credential types, fetch from API then map to strategies
    loading.value = true;
    error.value = null;

    try {
      const response = await CredentialTypeService.getCredentialTypes();

      // Map API response to strategies
      credentialTypes.value = (response || [])
        .map((ct: { value: string }) => {
          const strategy = registry.getStrategy(serviceType, ct.value);
          if (!strategy) {
            console.warn(`No strategy found for ${serviceType} ${ct.value}`);
            return null;
          }

          return {
            value: strategy.credentialType,
            label: strategy.displayName,
            fields: strategy.getFields() as CredentialField[],
          };
        })
        .filter(Boolean) as CredentialTypeOption[];
    } catch (err: unknown) {
      console.error('Error fetching credential types:', err);
      const errorObj = err as { message?: string };
      error.value = errorObj?.message || 'Failed to load credential types';
      credentialTypes.value = [];
    } finally {
      loading.value = false;
    }
  };

  return {
    credentialTypes: credentialTypes as Ref<CredentialTypeOption[]>,
    loading: loading as Ref<boolean>,
    error: error as Ref<string | null>,
    fetchCredentialTypes,
  };
}
