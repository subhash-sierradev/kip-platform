/**
 * Generic composable for connection step validation
 * Uses Strategy Pattern for credential-specific validation
 */

import { computed, type ComputedRef, type Ref } from 'vue';

import { CredentialStrategyRegistry } from '@/strategies';
import type { ConnectionStepData, CredentialTypeOption } from '@/types/ConnectionStepData';

interface ValidationParams {
  connectionData: Ref<ConnectionStepData>;
  credentialTypes: Ref<CredentialTypeOption[]>;
  testSuccess: Ref<boolean>;
  existingTestSuccess: Ref<boolean>;
  serviceType: string;
}

/**
 * Validate connection step for completion
 */
export function useConnectionValidation(params: ValidationParams) {
  const { connectionData, credentialTypes, testSuccess, existingTestSuccess, serviceType } = params;
  const registry = CredentialStrategyRegistry.getInstance();

  /**
   * Computed: Can the test button be enabled?
   * Uses strategy pattern to check credentials
   */
  const canTestConnection = computed(() => {
    const data = connectionData.value;

    // For existing connections, always allow verify
    if (data.connectionMethod === 'existing') {
      return !!data.existingConnectionId;
    }

    // For new connections, use strategy to validate
    const strategy = registry.getStrategy(serviceType, data.credentialType);
    if (!strategy) return false;

    // Check base fields and credentials
    return !!(data.baseUrl?.trim() && data.connectionName?.trim() && strategy.hasCredentials(data));
  });

  /**
   * Computed: Is the step valid and complete?
   * Step is valid when connection has been successfully tested/verified
   */
  const isStepValid = computed(() => {
    const data = connectionData.value;

    if (data.connectionMethod === 'existing') {
      // Existing connection must be selected and verified
      return !!data.existingConnectionId && existingTestSuccess.value;
    }

    // New connection must be tested successfully
    return testSuccess.value;
  });

  /**
   * Get validation message using strategy pattern
   */
  const getValidationMessage = (): string => {
    const data = connectionData.value;

    if (data.connectionMethod === 'existing') {
      if (!data.existingConnectionId) {
        return 'Please select a connection';
      }
      if (!existingTestSuccess.value) {
        return 'Please verify the selected connection';
      }
      return '';
    }

    // New connection validation using strategy
    const strategy = registry.getStrategy(serviceType, data.credentialType);
    if (!strategy) {
      return `Invalid credential type: ${data.credentialType}`;
    }

    const errors = strategy.validate(data);
    if (errors.length > 0) {
      return errors[0];
    }

    if (!testSuccess.value) {
      return 'Please test the connection';
    }

    return '';
  };

  /**
   * Get required fields for current credential type
   */
  const getRequiredFields = computed(() => {
    const credType = connectionData.value.credentialType;
    const credTypeOption = credentialTypes.value.find(ct => ct.value === credType);

    if (!credTypeOption) return [];

    return credTypeOption.fields.filter(field => field.required).map(field => field.key);
  });

  /**
   * Check if a specific field is filled
   */
  const isFieldFilled = (fieldKey: string): boolean => {
    const data = connectionData.value;
    const value = (data as unknown as Record<string, unknown>)[fieldKey];
    return !!(value && String(value).trim());
  };

  /**
   * Get missing required fields
   */
  const getMissingFields = computed(() => {
    const required = getRequiredFields.value;
    return required.filter(fieldKey => !isFieldFilled(fieldKey));
  });

  return {
    canTestConnection: canTestConnection as ComputedRef<boolean>,
    isStepValid: isStepValid as ComputedRef<boolean>,
    getRequiredFields: getRequiredFields as ComputedRef<string[]>,
    getMissingFields: getMissingFields as ComputedRef<string[]>,
    getValidationMessage,
    isFieldFilled,
  };
}
