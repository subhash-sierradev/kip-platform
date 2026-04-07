import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/api/core/ApiError';
import { ServiceType } from '@/api/models/enums';
import { ConnectionStatus } from '@/api/models/IntegrationConnection';
import type { IntegrationConnectionResponse } from '@/api/models/IntegrationConnectionResponse';
import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import { useToastStore } from '@/store/toast';

vi.mock('@/api/services/IntegrationConnectionService');
vi.mock('@/api/OpenAPI', () => ({ api: { patch: vi.fn() } }));
vi.mock('@/store/toast');

const mockToast = {
  $id: 'toast',
  $state: {} as any,
  $patch: vi.fn(),
  $reset: vi.fn(),
  $subscribe: vi.fn(),
  $dispose: vi.fn(),
  $onAction: vi.fn(),
  toasts: { value: [] } as any,
  toastCount: { value: 0 } as any,
  showSuccess: vi.fn(),
  showError: vi.fn(),
  showWarning: vi.fn(),
  showInfo: vi.fn(),
  clearToast: vi.fn(),
  clearAllToasts: vi.fn(),
  updateToast: vi.fn(),
};

vi.mocked(useToastStore).mockReturnValue(mockToast as any);

// Import after mocking
const { useServiceConnectionsAdmin } = await import('@/composables/useServiceConnectionsAdmin');

describe('useServiceConnectionsAdmin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('initialization', () => {
    it('should initialize with default state', () => {
      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);

      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe(null);
      expect(composable.page.value).toBe(0);
      expect(composable.rowsPerPage.value).toBe(10);
      expect(composable.sortAsc.value).toBe(false);
      expect(composable.quickFilter.value).toBe('ALL');
      expect(composable.connections.value).toEqual([]);
      expect(composable.total.value).toBe(0);
    });

    it('should expose toast options and component', () => {
      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);

      expect(composable.toastOptions).toBeDefined();
      expect(composable.toastOptions.value.visible).toBe(false);
      expect(composable.ToastComponent).toBeDefined();
    });

    it('should create isolated state for different service types', () => {
      const arcgisComposable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      const jiraComposable = useServiceConnectionsAdmin(ServiceType.JIRA);

      arcgisComposable.page.value = 5;
      expect(jiraComposable.page.value).toBe(0);
    });
  });

  describe('fetchConnections', () => {
    it('should fetch connections successfully', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: '1', secretName: 'conn1', lastConnectionStatus: ConnectionStatus.SUCCESS },
        { id: '2', secretName: 'conn2', lastConnectionStatus: ConnectionStatus.FAILED },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe(null);
      expect(composable.allConnections.value).toEqual(mockConnections);
      expect(IntegrationConnectionService.getAllConnections).toHaveBeenCalledWith({
        serviceType: ServiceType.ARCGIS,
      });
    });

    it('should set loading state during fetch', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve([]), 100)) as any
      );

      const composable = useServiceConnectionsAdmin(ServiceType.JIRA);
      const fetchPromise = composable.fetchConnections();

      expect(composable.loading.value).toBe(true);

      await fetchPromise;
      expect(composable.loading.value).toBe(false);
    });

    it('should handle fetch error and set error message', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockRejectedValue(
        new Error('Network error')
      );

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe('Failed to load connections');
      expect(composable.connections.value).toEqual([]);
    });

    it('should clear previous error on successful fetch', async () => {
      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);

      // First fetch fails
      vi.mocked(IntegrationConnectionService.getAllConnections).mockRejectedValue(
        new Error('Error')
      );
      await composable.fetchConnections();
      expect(composable.error.value).toBe('Failed to load connections');

      // Second fetch succeeds
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue([]);
      await composable.fetchConnections();
      expect(composable.error.value).toBe(null);
    });
  });

  describe('testConnection', () => {
    it('should test connection successfully and show success toast', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test Connection' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        id: 'conn1',
        success: true,
        connectionStatus: ConnectionStatus.SUCCESS,
        lastConnectionTest: '2026-02-05T10:00:00Z',
      } as any);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.testConnection('conn1');

      expect(IntegrationConnectionService.testExistingConnection).toHaveBeenCalledWith({
        connectionId: 'conn1',
      });
      expect(mockToast.showSuccess).toHaveBeenCalledWith(
        'Connection verified successfully. Your integration is ready for use.'
      );
      expect(composable.allConnections.value[0].lastConnectionStatus).toBe(
        ConnectionStatus.SUCCESS
      );
      expect(composable.allConnections.value[0].lastConnectionTest).toBe('2026-02-05T10:00:00Z');
    });

    it('should update connection status only if present in response', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test', lastConnectionStatus: ConnectionStatus.FAILED },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        id: 'conn1',
        success: true,
        // lastConnectionStatus not provided
        lastConnectionTest: '2026-02-05T10:00:00Z',
      } as any);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.testConnection('conn1');

      // Status should remain unchanged
      expect(composable.allConnections.value[0].lastConnectionStatus).toBe(ConnectionStatus.FAILED);
      expect(composable.allConnections.value[0].lastConnectionTest).toBe('2026-02-05T10:00:00Z');
    });

    it('should show error toast when test fails with message', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        id: 'conn1',
        statusCode: 400,
        success: false,
        message: 'Invalid credentials',
      } as any);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.testConnection('conn1');

      expect(mockToast.showError).toHaveBeenCalledWith(
        '❌ Connection test failed: Invalid credentials. Please check your credentials and server configuration.'
      );
    });

    it('should show default error message when test fails without message', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        id: 'conn1',
        statusCode: 500,
        success: false,
        message: '',
      } as any);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.testConnection('conn1');

      expect(mockToast.showError).toHaveBeenCalledWith(
        '❌ Connection test failed: . Please check your credentials and server configuration.'
      );
    });

    it('should handle test connection API error', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockRejectedValue(
        new Error('Network error')
      );

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.testConnection('conn1');

      expect(mockToast.showError).toHaveBeenCalledWith(
        '⚠️ Unable to test connection due to an unexpected error. Please check your network connection and try again.'
      );
    });

    it('should not update connection if not found in list', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        id: 'nonexistent',
        statusCode: 200,
        success: true,
        message: 'Success',
        lastConnectionStatus: ConnectionStatus.SUCCESS,
      } as any);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.testConnection('nonexistent');

      // Should not throw, just skip update
      expect(composable.allConnections.value).toEqual(mockConnections);
    });
  });

  describe('deleteConnection', () => {
    it('should delete connection successfully', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test1' },
        { id: 'conn2', secretName: 'Test2' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.deleteConnection).mockResolvedValue(undefined);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.deleteConnection('conn1');

      expect(IntegrationConnectionService.deleteConnection).toHaveBeenCalledWith({
        connectionId: 'conn1',
      });
      expect(composable.allConnections.value).toHaveLength(1);
      expect(composable.allConnections.value[0].id).toBe('conn2');
      expect(mockToast.showWarning).toHaveBeenCalledWith('Connection deleted');
    });

    it('should handle delete API error', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.deleteConnection).mockRejectedValue(
        new Error('Delete failed')
      );

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.deleteConnection('conn1');

      expect(mockToast.showError).toHaveBeenCalledWith('Failed to delete connection');
      expect(composable.allConnections.value).toHaveLength(1); // Not deleted from local state
    });

    it('should rethrow 409 conflict to allow UI handling', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test' },
      ];

      const conflict = new ApiError(
        { method: 'DELETE', url: '/integrations/connections/conn1' } as any,
        {
          url: '/integrations/connections/conn1',
          ok: false,
          status: 409,
          statusText: 'Conflict',
          body: { details: { jiraWebhooks: [{ id: 'w1', name: 'Webhook 1', isEnabled: true }] } },
        } as any,
        'Conflict'
      );

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.deleteConnection).mockRejectedValue(conflict);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      await expect(composable.deleteConnection('conn1')).rejects.toBe(conflict);
      expect(mockToast.showError).not.toHaveBeenCalledWith('Failed to delete connection');
      expect(composable.allConnections.value).toHaveLength(1);
    });

    it('should not throw if deleting nonexistent connection', async () => {
      const mockConnections: IntegrationConnectionResponse[] = [
        { id: 'conn1', secretName: 'Test' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);
      vi.mocked(IntegrationConnectionService.deleteConnection).mockResolvedValue(undefined);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      await composable.deleteConnection('nonexistent');

      // Should not throw, list remains unchanged
      expect(composable.allConnections.value).toHaveLength(1);
    });
  });

  describe('filtering and sorting', () => {
    const mockConnections: IntegrationConnectionResponse[] = [
      {
        id: '1',
        secretName: 'conn1',
        lastConnectionStatus: ConnectionStatus.SUCCESS,
        lastConnectionTest: '2026-02-01T10:00:00Z',
      },
      {
        id: '2',
        secretName: 'conn2',
        lastConnectionStatus: ConnectionStatus.FAILED,
        lastConnectionTest: '2026-02-03T10:00:00Z',
      },
      {
        id: '3',
        secretName: 'conn3',
        lastConnectionStatus: ConnectionStatus.FAILED,
        lastConnectionTest: '2026-02-02T10:00:00Z',
      },
    ];

    it('should filter by FAILED status', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      composable.setQuickFilter('FAILED');

      expect(composable.connections.value).toHaveLength(2);
      expect(composable.connections.value.map(c => c.id)).toContain('2');
      expect(composable.connections.value.map(c => c.id)).toContain('3');
    });

    it('should show all connections with ALL filter', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      composable.setQuickFilter('ALL');

      expect(composable.connections.value).toHaveLength(3);
    });

    it('should sort connections by lastConnectionTest descending by default', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      // Default sort is descending (newest first)
      expect(composable.allConnections.value[0].id).toBe('2'); // 2026-02-03
      expect(composable.allConnections.value[1].id).toBe('3'); // 2026-02-02
      expect(composable.allConnections.value[2].id).toBe('1'); // 2026-02-01
    });

    it('should toggle sort to ascending', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();
      composable.toggleSort();

      expect(composable.sortAsc.value).toBe(true);
      expect(composable.allConnections.value[0].id).toBe('1'); // 2026-02-01
      expect(composable.allConnections.value[1].id).toBe('3'); // 2026-02-02
      expect(composable.allConnections.value[2].id).toBe('2'); // 2026-02-03
    });

    it('should handle connections without lastConnectionTest', async () => {
      const connectionsWithNull: IntegrationConnectionResponse[] = [
        { id: '1', secretName: 'conn1', lastConnectionTest: '2026-02-01T10:00:00Z' },
        { id: '2', secretName: 'conn2' }, // no lastConnectionTest
        { id: '3', secretName: 'conn3', lastConnectionTest: '2026-02-03T10:00:00Z' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(
        connectionsWithNull
      );

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      // Connections without lastConnectionTest should sort to the end (treated as 0)
      expect(composable.allConnections.value[2].id).toBe('2');
    });
  });

  describe('pagination', () => {
    const mockConnections: IntegrationConnectionResponse[] = Array.from({ length: 25 }, (_, i) => ({
      id: `conn${i}`,
      secretName: `Connection ${i}`,
      lastConnectionTest: `2026-02-0${Math.min((i % 9) + 1, 9)}T10:00:00Z`,
    }));

    it('should paginate connections with default page size', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      expect(composable.rowsPerPage.value).toBe(10);
      expect(composable.connections.value).toHaveLength(10);
      expect(composable.total.value).toBe(25);
    });

    it('should change page', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      composable.setPage(1);
      expect(composable.page.value).toBe(1);
      expect(composable.connections.value).toHaveLength(10);
    });

    it('should change rows per page', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      composable.setRowsPerPage(20);
      expect(composable.rowsPerPage.value).toBe(20);
      expect(composable.connections.value).toHaveLength(20);
    });

    it('should handle last page with fewer items', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      composable.setPage(2); // Page 3 (0-indexed)
      expect(composable.connections.value).toHaveLength(5); // Only 5 items on last page
    });
  });

  describe('combined filtering, sorting, and pagination', () => {
    const mockConnections: IntegrationConnectionResponse[] = [
      {
        id: '1',
        secretName: 'conn1',
        lastConnectionStatus: ConnectionStatus.SUCCESS,
        lastConnectionTest: '2026-02-01T10:00:00Z',
      },
      {
        id: '2',
        secretName: 'conn2',
        lastConnectionStatus: ConnectionStatus.FAILED,
        lastConnectionTest: '2026-02-05T10:00:00Z',
      },
      {
        id: '3',
        secretName: 'conn3',
        lastConnectionStatus: ConnectionStatus.FAILED,
        lastConnectionTest: '2026-02-03T10:00:00Z',
      },
      {
        id: '4',
        secretName: 'conn4',
        lastConnectionStatus: ConnectionStatus.SUCCESS,
        lastConnectionTest: '2026-02-04T10:00:00Z',
      },
    ];

    it('should apply filter, sort, and pagination together', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(mockConnections);

      const composable = useServiceConnectionsAdmin(ServiceType.ARCGIS);
      await composable.fetchConnections();

      // Filter to failed connections only
      composable.setQuickFilter('FAILED');
      expect(composable.total.value).toBe(2);

      // Toggle sort to ascending
      composable.toggleSort();

      // Check first page with 1 item per page
      composable.setRowsPerPage(1);
      expect(composable.connections.value).toHaveLength(1);
      expect(composable.connections.value[0].id).toBe('3'); // Oldest failed

      // Check second page
      composable.setPage(1);
      expect(composable.connections.value).toHaveLength(1);
      expect(composable.connections.value[0].id).toBe('2'); // Newest failed
    });
  });

  describe('rotateSecret', () => {
    it('should call rotate secret endpoint and show success toast', async () => {
      vi.spyOn(IntegrationConnectionService, 'rotateConnectionSecret').mockResolvedValue(
        undefined as any
      );

      const composable = useServiceConnectionsAdmin(ServiceType.JIRA);
      await composable.rotateSecret('conn1', 'new-secret');

      expect(IntegrationConnectionService.rotateConnectionSecret).toHaveBeenCalledWith({
        connectionId: 'conn1',
        requestBody: { newSecret: 'new-secret' },
      });
      expect(mockToast.showSuccess).toHaveBeenCalledWith('Secret updated');
    });
  });
});
