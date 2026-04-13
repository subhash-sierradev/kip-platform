import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import { useExistingConnections } from '@/composables/useExistingConnections';

// Mock the IntegrationConnectionService
vi.mock('@/api/services/IntegrationConnectionService', () => ({
  IntegrationConnectionService: {
    getAllConnections: vi.fn(),
    testExistingConnection: vi.fn(),
  },
}));

describe('useExistingConnections', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('fetchExistingConnections', () => {
    it('should fetch connections from API', async () => {
      const mockConnections = [
        {
          id: 'conn-1',
          name: 'Test Connection',
          integrationSecret: { secretName: 'secret-1', baseUrl: 'https://test.com' },
          lastConnectionStatus: 'SUCCESS',
          lastConnectionTest: '2024-01-30T10:00:00Z',
        },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(
        mockConnections as any
      );

      const { existingConnections, loading, fetchExistingConnections } =
        useExistingConnections('JIRA');

      expect(loading.value).toBe(false);

      const promise = fetchExistingConnections();
      expect(loading.value).toBe(true);

      await promise;

      expect(loading.value).toBe(false);
      expect(existingConnections.value).toHaveLength(1);
      expect(existingConnections.value[0].id).toBe('conn-1');
      expect(existingConnections.value[0].name).toBe('Test Connection');
      expect(existingConnections.value[0].baseUrl).toBe(''); // Base URL not available in API response
    });

    it('should handle API errors', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockRejectedValue(
        new Error('Failed to fetch')
      );

      const { existingConnections, error, fetchExistingConnections } =
        useExistingConnections('JIRA');

      await fetchExistingConnections();

      expect(error.value).toBe('Failed to fetch');
      expect(existingConnections.value).toHaveLength(0);
    });

    it('should pass correct service type to API', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue([]);

      const { fetchExistingConnections } = useExistingConnections('ARCGIS');

      await fetchExistingConnections();

      expect(IntegrationConnectionService.getAllConnections).toHaveBeenCalledWith({
        serviceType: 'ARCGIS',
      });
    });
  });

  describe('activeCount and failedCount', () => {
    it('should count active connections correctly', async () => {
      const mockConnections = [
        { id: '1', lastConnectionStatus: 'SUCCESS' },
        { id: '2', lastConnectionStatus: 'SUCCESS' },
        { id: '3', lastConnectionStatus: 'FAILED' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(
        mockConnections as any
      );

      const { activeCount, fetchExistingConnections } = useExistingConnections('JIRA');

      await fetchExistingConnections();

      expect(activeCount.value).toBe(2);
    });

    it('should count failed connections correctly', async () => {
      const mockConnections = [
        { id: '1', lastConnectionStatus: 'SUCCESS' },
        { id: '2', lastConnectionStatus: 'FAILED' },
        { id: '3', lastConnectionStatus: 'FAILED' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections).mockResolvedValue(
        mockConnections as any
      );

      const { failedCount, fetchExistingConnections } = useExistingConnections('JIRA');

      await fetchExistingConnections();

      expect(failedCount.value).toBe(2);
    });
  });

  describe('verifyConnection', () => {
    it('should verify existing connection successfully', async () => {
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        success: true,
        connectionStatus: 'SUCCESS',
        message: 'Connection verified',
      } as any);

      const { verifyConnection, existingTestSuccess, existingTestMessage } =
        useExistingConnections('JIRA');

      const result = await verifyConnection('conn-1');

      expect(result).toBe(true);
      expect(existingTestSuccess.value).toBe(true);
      expect(existingTestMessage.value).toBe('Connection verified');
      expect(IntegrationConnectionService.testExistingConnection).toHaveBeenCalledWith({
        connectionId: 'conn-1',
      });
    });

    it('should handle verification failure', async () => {
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        lastConnectionStatus: 'FAILED',
        message: 'Connection failed',
      } as any);

      const { verifyConnection, existingTestSuccess, existingTestMessage } =
        useExistingConnections('JIRA');

      const result = await verifyConnection('conn-1');

      expect(result).toBe(false);
      expect(existingTestSuccess.value).toBe(false);
      expect(existingTestMessage.value).toBe('Connection failed');
    });

    it('should handle verification API error', async () => {
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockRejectedValue(
        new Error('Network error')
      );

      const { verifyConnection, existingTestSuccess, existingTestMessage } =
        useExistingConnections('JIRA');

      const result = await verifyConnection('conn-1');

      expect(result).toBe(false);
      expect(existingTestSuccess.value).toBe(false);
      expect(existingTestMessage.value).toBe('Network error');
    });

    it('should handle empty connection ID', async () => {
      const { verifyConnection, existingTestMessage } = useExistingConnections('JIRA');

      const result = await verifyConnection('');

      expect(result).toBe(false);
      expect(existingTestMessage.value).toBe('Please select a connection');
    });
  });

  describe('getConnectionStatus', () => {
    it('should return correct status for SUCCESS', () => {
      const { getConnectionStatus } = useExistingConnections('JIRA');

      const status = getConnectionStatus({
        id: '1',
        name: 'Test',
        baseUrl: 'https://test.com',
        lastConnectionStatus: 'SUCCESS',
      });

      expect(status.label).toBe('Active');
      expect(status.severity).toBe('success');
    });

    it('should return correct status for FAILED', () => {
      const { getConnectionStatus } = useExistingConnections('JIRA');

      const status = getConnectionStatus({
        id: '1',
        name: 'Test',
        baseUrl: 'https://test.com',
        lastConnectionStatus: 'FAILED',
      });

      expect(status.label).toBe('Failed');
      expect(status.severity).toBe('error');
    });

    it('should return neutral status for unknown', () => {
      const { getConnectionStatus } = useExistingConnections('JIRA');

      const status = getConnectionStatus({
        id: '1',
        name: 'Test',
        baseUrl: 'https://test.com',
        lastConnectionStatus: '',
      });

      expect(status.label).toBe('Not Tested');
      expect(status.severity).toBe('neutral');
    });
  });

  describe('formatLastTested', () => {
    it('should format recent time as "Just now"', () => {
      const { formatLastTested } = useExistingConnections('JIRA');

      const now = new Date().toISOString();
      const result = formatLastTested(now);

      expect(result).toBe('Just now');
    });

    it('should format minutes ago', () => {
      const { formatLastTested } = useExistingConnections('JIRA');

      const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString();
      const result = formatLastTested(fiveMinutesAgo);

      expect(result).toBe('5m ago');
    });

    it('should return "Never tested" for undefined', () => {
      const { formatLastTested } = useExistingConnections('JIRA');

      const result = formatLastTested(undefined);

      expect(result).toBe('Never tested');
    });
  });

  describe('verifyButtonText', () => {
    it('should show "Testing..." during test', async () => {
      let resolveTest: any;
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockReturnValue(
        new Promise(resolve => {
          resolveTest = resolve;
        }) as any
      );

      const { verifyConnection, verifyButtonText } = useExistingConnections('JIRA');

      const promise = verifyConnection('conn-1');

      await new Promise(resolve => setTimeout(resolve, 0));
      expect(verifyButtonText.value).toBe('Testing...');

      resolveTest({ success: true, connectionStatus: 'SUCCESS' });
      await promise;
    });

    it('should show "Connection Verified" after successful test', async () => {
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        success: true,
        connectionStatus: 'SUCCESS',
      } as any);

      const { verifyConnection, verifyButtonText } = useExistingConnections('JIRA');

      await verifyConnection('conn-1');

      expect(verifyButtonText.value).toBe('Connection Verified');
    });

    it('should show "Verification Failed" after failed test', async () => {
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue({
        lastConnectionStatus: 'FAILED',
      } as any);

      const { verifyConnection, verifyButtonText } = useExistingConnections('JIRA');

      await verifyConnection('conn-1');

      expect(verifyButtonText.value).toBe('Verification Failed');
    });
  });

  describe('refresh functionality', () => {
    it('should prevent concurrent fetch calls with isRefreshing flag', async () => {
      let resolveFirstCall: any;
      let callCount = 0;

      vi.mocked(IntegrationConnectionService.getAllConnections).mockImplementation(() => {
        callCount++;
        if (callCount === 1) {
          return new Promise(resolve => {
            resolveFirstCall = () => resolve([]);
          }) as any;
        }
        return Promise.resolve([]);
      });

      const { fetchExistingConnections, isRefreshing, loading } = useExistingConnections('JIRA');

      // Start first fetch
      const firstFetch = fetchExistingConnections();
      await new Promise(resolve => setTimeout(resolve, 0));

      expect(loading.value).toBe(true);
      expect(isRefreshing.value).toBe(true);

      // Try to start second fetch while first is still running
      await fetchExistingConnections();

      // Should not have called API twice
      expect(callCount).toBe(1);

      // Complete first fetch
      resolveFirstCall();
      await firstFetch;

      expect(loading.value).toBe(false);
      expect(isRefreshing.value).toBe(false);
    });

    it('should allow fetch after previous fetch completes', async () => {
      const mockConnections1 = [{ id: 'conn-1', name: 'Connection 1' }];
      const mockConnections2 = [
        { id: 'conn-1', name: 'Connection 1' },
        { id: 'conn-2', name: 'Connection 2' },
      ];

      vi.mocked(IntegrationConnectionService.getAllConnections)
        .mockResolvedValueOnce(mockConnections1 as any)
        .mockResolvedValueOnce(mockConnections2 as any);

      const { fetchExistingConnections, existingConnections, isRefreshing } =
        useExistingConnections('JIRA');

      // First fetch
      await fetchExistingConnections();
      expect(existingConnections.value).toHaveLength(1);
      expect(isRefreshing.value).toBe(false);

      // Second fetch should work
      await fetchExistingConnections();
      expect(existingConnections.value).toHaveLength(2);
      expect(isRefreshing.value).toBe(false);
      expect(IntegrationConnectionService.getAllConnections).toHaveBeenCalledTimes(2);
    });

    it('should reset isRefreshing flag on error', async () => {
      vi.mocked(IntegrationConnectionService.getAllConnections).mockRejectedValue(
        new Error('Network error')
      );

      const { fetchExistingConnections, isRefreshing, loading } = useExistingConnections('JIRA');

      await fetchExistingConnections();

      expect(loading.value).toBe(false);
      expect(isRefreshing.value).toBe(false);
    });
  });
});
