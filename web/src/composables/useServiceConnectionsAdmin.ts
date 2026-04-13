import { DxToast } from 'devextreme-vue/toast';
import { computed, type Ref, ref } from 'vue';

import { ApiError } from '@/api/core/ApiError';
import { ServiceType } from '@/api/models/enums';
import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import { useToastStore } from '@/store/toast';
import type { ConnectionStepData } from '@/types/ConnectionStepData';
import { buildIntegrationSecret } from '@/utils/connectionTestHelpers';
import { toPlainText } from '@/utils/stringFormatUtils';

import { ConnectionStatus } from '../api/models/IntegrationConnection';
import type { IntegrationConnectionResponse } from '../api/models/IntegrationConnectionResponse';

export type ConnectionQuickFilter = 'ALL' | 'FAILED';

// Internal helper builders extracted to reduce main function size (ESLint max-lines-per-function)
function buildToastOptions() {
  return ref({
    visible: false,
    message: '',
    type: 'info' as 'success' | 'error' | 'warning' | 'info',
  });
}

function buildDerivatives(
  connections: Ref<IntegrationConnectionResponse[]>,
  quickFilter: Ref<ConnectionQuickFilter>,
  sortAsc: Ref<boolean>,
  page: Ref<number>,
  rowsPerPage: Ref<number>
) {
  const filtered = computed(() => {
    let working = connections.value;
    if (quickFilter.value === 'FAILED')
      working = working.filter(c => c.lastConnectionStatus === ConnectionStatus.FAILED);
    return working;
  });
  const sorted = computed(() =>
    [...filtered.value].sort((a, b) => {
      const at = a.lastConnectionTest ? Date.parse(a.lastConnectionTest) : 0;
      const bt = b.lastConnectionTest ? Date.parse(b.lastConnectionTest) : 0;
      return sortAsc.value ? at - bt : bt - at;
    })
  );
  const total = computed(() => sorted.value.length);
  const paginated = computed(() =>
    sorted.value.slice(
      page.value * rowsPerPage.value,
      page.value * rowsPerPage.value + rowsPerPage.value
    )
  );
  return { filtered, sorted, total, paginated };
}

// Initialize toast store
const toast = useToastStore();

function createFetchConnections(
  connections: { value: IntegrationConnectionResponse[] },
  serviceType: ServiceType,
  loading: { value: boolean },
  error: { value: string | null }
) {
  return async function fetchConnections() {
    loading.value = true;
    error.value = null;
    try {
      // Fetch connections for specific service type using the unified integrations endpoint
      const data = await IntegrationConnectionService.getAllConnections({ serviceType });
      connections.value = data;
    } catch {
      error.value = 'Failed to load connections';
      connections.value = [];
    } finally {
      loading.value = false;
    }
  };
}

function createTestConnection(connectionsRef: { value: IntegrationConnectionResponse[] }) {
  return async function testConnection(id: string) {
    try {
      const result = await IntegrationConnectionService.testExistingConnection({
        connectionId: id,
      });

      const idx = connectionsRef.value.findIndex(c => c.id === id);
      if (idx !== -1) {
        const row = connectionsRef.value[idx];

        // 🔥 force update only if present in response
        if (result.connectionStatus !== undefined) {
          row.lastConnectionStatus = result.connectionStatus;
        }

        if (result.lastConnectionTest !== undefined) {
          row.lastConnectionTest = result.lastConnectionTest;
        }
      }

      if (result.success) {
        toast.showSuccess(`Connection verified successfully. Your integration is ready for use.`);
      } else {
        const errorMessage = result.message ?? 'Connection verification failed';
        toast.showError(
          `❌ Connection test failed: ${errorMessage}. Please check your credentials and server configuration.`
        );
      }
    } catch {
      toast.showError(
        '⚠️ Unable to test connection due to an unexpected error. Please check your network connection and try again.'
      );
    }
  };
}

function createDeleteConnection(connectionsRef: { value: IntegrationConnectionResponse[] }) {
  return async function deleteConnection(id: string) {
    try {
      await IntegrationConnectionService.deleteConnection({ connectionId: id });

      const idx = connectionsRef.value.findIndex(c => c.id === id);
      if (idx !== -1) connectionsRef.value.splice(idx, 1);

      toast.showWarning('Connection deleted');
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        throw e;
      }
      toast.showError('Failed to delete connection');
    }
  };
}

function createRotateSecret() {
  return async function rotateSecret(id: string, newSecret: string): Promise<boolean> {
    try {
      if (!id || !newSecret) return false;
      await IntegrationConnectionService.rotateConnectionSecret({
        connectionId: id,
        requestBody: { newSecret },
      });
      toast.showSuccess('Secret updated');
      return true;
    } catch {
      toast.showError('Failed to update secret');
      return false;
    }
  };
}

function createCreateConnection(serviceType: ServiceType, fetchConnections: () => Promise<void>) {
  return async function createConnection(connectionData: ConnectionStepData) {
    const requestBody = {
      name: connectionData.connectionName || '',
      serviceType,
      integrationSecret: buildIntegrationSecret(serviceType, connectionData),
    };

    try {
      const response = await IntegrationConnectionService.testAndCreateConnection({ requestBody });
      const isSuccess = response.lastConnectionStatus === ConnectionStatus.SUCCESS;

      if (isSuccess) {
        await fetchConnections();
        toast.showSuccess('Connection created successfully.');
      } else {
        const rawMessage = response.lastConnectionMessage || 'Connection test failed';
        toast.showError(toPlainText(rawMessage, 'Connection test failed'));
      }

      return response;
    } catch (error: unknown) {
      const apiError = error as { body?: { message?: string }; message?: string };
      const rawMessage =
        apiError.body?.message || apiError.message || 'Failed to test and create connection';
      toast.showError(toPlainText(rawMessage, 'Failed to test and create connection'));
      throw error;
    }
  };
}

function buildActions(
  connections: Ref<IntegrationConnectionResponse[]>,
  loading: Ref<boolean>,
  error: Ref<string | null>,
  serviceType: ServiceType
) {
  const fetchConnections = createFetchConnections(connections, serviceType, loading, error);
  const testConnection = createTestConnection(connections);
  const deleteConnection = createDeleteConnection(connections);
  const rotateSecret = createRotateSecret();
  const createConnection = createCreateConnection(serviceType, fetchConnections);

  return { fetchConnections, testConnection, deleteConnection, rotateSecret, createConnection };
}

export function useServiceConnectionsAdmin(serviceType: ServiceType) {
  // Base refs declared inside function to ensure isolated state per invocation
  const connectionsRef = ref<IntegrationConnectionResponse[]>([]);
  const loadingRef = ref(false);
  const errorRef = ref<string | null>(null);
  const pageRef = ref(0);
  const rowsPerPageRef = ref(10);
  const sortAscRef = ref(false);
  const quickFilterRef = ref<ConnectionQuickFilter>('ALL');

  const toastOptions = buildToastOptions();
  const ToastComponent = DxToast;
  const { sorted, total, paginated } = buildDerivatives(
    connectionsRef,
    quickFilterRef,
    sortAscRef,
    pageRef,
    rowsPerPageRef
  );
  const { fetchConnections, testConnection, deleteConnection, rotateSecret, createConnection } =
    buildActions(connectionsRef, loadingRef, errorRef, serviceType);

  function toggleSort() {
    sortAscRef.value = !sortAscRef.value;
  }

  return {
    loading: loadingRef,
    error: errorRef,
    page: pageRef,
    rowsPerPage: rowsPerPageRef,
    sortAsc: sortAscRef,
    quickFilter: quickFilterRef,
    toastOptions,
    ToastComponent,
    connections: paginated,
    allConnections: sorted,
    total,
    toggleSort,
    setPage: (p: number) => {
      pageRef.value = p;
    },
    setRowsPerPage: (r: number) => {
      rowsPerPageRef.value = r;
    },
    setQuickFilter: (f: ConnectionQuickFilter) => {
      quickFilterRef.value = f;
    },
    testConnection,
    deleteConnection,
    rotateSecret,
    createConnection,
    fetchConnections,
  };
}
