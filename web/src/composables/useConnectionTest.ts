/**
 * Generic composable for testing new connections
 * Handles test and create workflow using Strategy Pattern
 */

import { computed, type ComputedRef, type Ref, ref } from 'vue';

import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import type { ConnectionStepData } from '@/types/ConnectionStepData';
import { buildIntegrationSecret, validateConnectionData } from '@/utils/connectionTestHelpers';
import { toPlainText } from '@/utils/stringFormatUtils';

/**
 * Test and create new connection
 * @param serviceType - 'JIRA' or 'ARCGIS'
 */
export function useConnectionTest(serviceType: string) {
  const isTesting = ref(false);
  const tested = ref(false);
  const testSuccess = ref(false);
  const testMessage = ref('');
  const createdConnectionId = ref<string | null>(null);

  /**
   * Computed: Button text for test action
   */
  const testButtonText = computed(() => {
    if (isTesting.value) return 'Testing...';
    return 'Test Connection';
  });

  /**
   * Test new connection and create if successful
   * @param connectionData - Connection data to test
   * @returns true if test successful, false otherwise
   */
  const testConnection = async (connectionData: ConnectionStepData): Promise<boolean> => {
    isTesting.value = true;
    tested.value = false;
    testSuccess.value = false;
    testMessage.value = '';
    createdConnectionId.value = null;

    try {
      // Build request payload
      const requestBody = {
        name: connectionData.connectionName || '',
        serviceType: serviceType,
        integrationSecret: buildIntegrationSecret(serviceType, connectionData),
      };

      // Call test and create API
      const response = await IntegrationConnectionService.testAndCreateConnection({
        requestBody,
      });

      // Check response status
      testSuccess.value = response.lastConnectionStatus === 'SUCCESS';
      const rawMessage =
        response.lastConnectionMessage ||
        (testSuccess.value ? 'Connection tested successfully' : 'Connection test failed');
      testMessage.value = toPlainText(rawMessage, 'Connection test failed');

      // Store created connection ID for later use
      if (testSuccess.value && response.id) {
        createdConnectionId.value = response.id;
      }

      tested.value = true;
      return testSuccess.value;
    } catch (err: unknown) {
      console.error('Error testing connection:', err);
      testSuccess.value = false;
      const error = err as { body?: { message?: string }; message?: string };
      const rawErrorMessage = error.body?.message || error.message || 'Failed to test connection';
      testMessage.value = toPlainText(rawErrorMessage, 'Failed to test connection');
      tested.value = true;
      return false;
    } finally {
      isTesting.value = false;
    }
  };

  /**
   * Reset test state
   */
  const resetTest = (): void => {
    tested.value = false;
    testSuccess.value = false;
    testMessage.value = '';
    createdConnectionId.value = null;
  };

  return {
    isTesting: isTesting as Ref<boolean>,
    tested: tested as Ref<boolean>,
    testSuccess: testSuccess as Ref<boolean>,
    testMessage: testMessage as Ref<string>,
    testButtonText: testButtonText as ComputedRef<string>,
    createdConnectionId: createdConnectionId as Ref<string | null>,
    testConnection,
    resetTest,
    validateForTest: (data: ConnectionStepData) => validateConnectionData(serviceType, data),
  };
}
