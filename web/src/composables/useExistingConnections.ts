/**
 * Generic composable for managing existing connections
 * Handles fetching, displaying, and verifying saved connections for any service type
 */

import { computed, type ComputedRef, type Ref, ref } from 'vue';

import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import type { SavedConnection } from '@/types/ConnectionStepData';
import {
  formatLastTested,
  getConnectionStatus,
  transformToSavedConnection,
} from '@/utils/connectionStatusHelpers';
import { verifyConnectionWithApi } from '@/utils/connectionVerificationHelpers';

/**
 * Fetch connections from API and transform to SavedConnection format
 */
async function fetchConnectionsFromApi(
  serviceType: string,
  loadingRef: Ref<boolean>,
  refreshingRef: Ref<boolean>,
  errorRef: Ref<string | null>
): Promise<SavedConnection[]> {
  loadingRef.value = true;
  refreshingRef.value = true;
  errorRef.value = null;

  try {
    const response = await IntegrationConnectionService.getAllConnections({
      serviceType,
    });
    return (response || []).map(transformToSavedConnection);
  } catch (err: unknown) {
    console.error('Error fetching existing connections:', err);
    const errorObj = err as { message?: string };
    errorRef.value = errorObj.message || 'Failed to load existing connections';
    return [];
  } finally {
    loadingRef.value = false;
    refreshingRef.value = false;
  }
}

/**
 * Verify connection and update state
 */
async function performConnectionVerification(
  connectionId: string,
  testingRef: Ref<boolean>,
  testedRef: Ref<boolean>,
  successRef: Ref<boolean>,
  messageRef: Ref<string>
): Promise<boolean> {
  testingRef.value = true;
  testedRef.value = false;
  messageRef.value = '';

  const result = await verifyConnectionWithApi(connectionId);

  successRef.value = result.success;
  messageRef.value = result.message;
  testedRef.value = true;
  testingRef.value = false;

  return result.success;
}

/**
 * Manage existing connections for a specific service type
 * @param serviceType - 'JIRA' or 'ARCGIS'
 */
export function useExistingConnections(serviceType: string) {
  const existingConnections = ref<SavedConnection[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const isRefreshing = ref(false);

  const isTestingExisting = ref(false);
  const existingTestSuccess = ref(false);
  const existingTestMessage = ref('');
  const existingTested = ref(false);

  const activeCount = computed(() => {
    return existingConnections.value.filter(c => c.lastConnectionStatus === 'SUCCESS').length;
  });

  const failedCount = computed(() => {
    return existingConnections.value.filter(c => c.lastConnectionStatus === 'FAILED').length;
  });

  const verifyButtonText = computed(() => {
    if (isTestingExisting.value) return 'Verifying...';
    if (existingTested.value && existingTestSuccess.value) return 'Verified ✓';
    if (existingTested.value && !existingTestSuccess.value) return 'Failed ✗';
    return 'Verify Connection';
  });

  const fetchExistingConnections = async (): Promise<void> => {
    if (isRefreshing.value || loading.value) {
      return;
    }

    existingConnections.value = await fetchConnectionsFromApi(
      serviceType,
      loading,
      isRefreshing,
      error
    );
  };

  const verifyConnection = async (connectionId: string): Promise<boolean> => {
    return performConnectionVerification(
      connectionId,
      isTestingExisting,
      existingTested,
      existingTestSuccess,
      existingTestMessage
    );
  };

  const resetVerification = (): void => {
    existingTested.value = false;
    existingTestSuccess.value = false;
    existingTestMessage.value = '';
  };

  return {
    existingConnections: existingConnections as Ref<SavedConnection[]>,
    loading: loading as Ref<boolean>,
    error: error as Ref<string | null>,
    isRefreshing: isRefreshing as Ref<boolean>,
    activeCount: activeCount as ComputedRef<number>,
    failedCount: failedCount as ComputedRef<number>,
    isTestingExisting: isTestingExisting as Ref<boolean>,
    existingTested: existingTested as Ref<boolean>,
    existingTestSuccess: existingTestSuccess as Ref<boolean>,
    existingTestMessage: existingTestMessage as Ref<string>,
    verifyButtonText: verifyButtonText as ComputedRef<string>,
    fetchExistingConnections,
    verifyConnection,
    resetVerification,
    getConnectionStatus,
    formatLastTested,
  };
}
