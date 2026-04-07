import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { IntegrationJobExecutionDto } from '@/api/models/IntegrationJobExecutionDto';
import { useArcGISJobHistory } from '@/composables/useArcGISJobHistory';

// Mock ArcGISIntegrationService
const mockGetJobHistory = vi.fn();
vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getJobHistory: (id: string) => mockGetJobHistory(id),
  },
}));

describe('useArcGISJobHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('initial state', () => {
    it('starts with empty history array', () => {
      const { history, loading, error } = useArcGISJobHistory();

      expect(history.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
    });
  });

  describe('fetchJobHistory', () => {
    const integrationId = 'test-integration-id';

    it('fetches job history successfully', async () => {
      const mockHistory: IntegrationJobExecutionDto[] = [
        {
          id: 'job1',
          scheduleId: 'schedule1',
          triggeredBy: 'user',
          startedAt: '2024-01-01T10:00:00Z',
          lastAttemptTime: '2024-01-01T10:00:00Z',
          completedAt: '2024-01-01T10:05:00Z',
          status: 'SUCCESS',
          errorMessage: null,
          duration: 300,
        } as any,
        {
          id: 'job2',
          scheduleId: 'schedule1',
          triggeredBy: 'user',
          startedAt: '2024-01-01T11:00:00Z',
          lastAttemptTime: '2024-01-01T11:00:00Z',
          completedAt: '2024-01-01T11:05:00Z',
          status: 'SUCCESS',
          errorMessage: null,
          duration: 300,
        } as any,
      ];
      mockGetJobHistory.mockResolvedValue(mockHistory);

      const { history, loading, error, fetchJobHistory } = useArcGISJobHistory();

      const fetchPromise = fetchJobHistory(integrationId);
      expect(loading.value).toBe(true);

      await fetchPromise;

      expect(history.value).toEqual(mockHistory);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
      expect(mockGetJobHistory).toHaveBeenCalledWith(integrationId);
    });

    it('handles empty history response', async () => {
      mockGetJobHistory.mockResolvedValue([]);

      const { history, loading, error, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory(integrationId);

      expect(history.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
    });

    it('handles error with message property', async () => {
      const mockError = { message: 'Failed to fetch history' };
      mockGetJobHistory.mockRejectedValue(mockError);

      const { history, loading, error, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory(integrationId);

      expect(history.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBe('Failed to fetch history');
    });

    it('handles error without message property', async () => {
      mockGetJobHistory.mockRejectedValue('String error');

      const { history, loading, error, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory(integrationId);

      expect(history.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBe('Failed to load job history.');
    });

    it('handles null error', async () => {
      mockGetJobHistory.mockRejectedValue(null);

      const { history, loading, error, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory(integrationId);

      expect(history.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBe('Failed to load job history.');
    });

    it('handles undefined error', async () => {
      mockGetJobHistory.mockRejectedValue(undefined);

      const { history, loading, error, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory(integrationId);

      expect(history.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBe('Failed to load job history.');
    });

    it('clears previous history on error', async () => {
      const mockHistory: IntegrationJobExecutionDto[] = [
        { id: 'job1' } as IntegrationJobExecutionDto,
      ];
      mockGetJobHistory.mockResolvedValueOnce(mockHistory);

      const { history, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory(integrationId);
      expect(history.value).toEqual(mockHistory);

      // Second call fails
      mockGetJobHistory.mockRejectedValue(new Error('Network error'));
      await fetchJobHistory(integrationId);

      expect(history.value).toEqual([]);
    });

    it('clears previous error on successful fetch', async () => {
      mockGetJobHistory.mockRejectedValueOnce({ message: 'First error' });

      const { error, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory(integrationId);
      expect(error.value).toBe('First error');

      // Second call succeeds
      mockGetJobHistory.mockResolvedValue([{ id: 'job1' } as IntegrationJobExecutionDto]);
      await fetchJobHistory(integrationId);

      expect(error.value).toBeNull();
    });

    it('can fetch history for different integration IDs', async () => {
      const history1 = [{ id: 'job1' } as IntegrationJobExecutionDto];
      const history2 = [{ id: 'job2' } as IntegrationJobExecutionDto];

      mockGetJobHistory.mockResolvedValueOnce(history1).mockResolvedValueOnce(history2);

      const { history, fetchJobHistory } = useArcGISJobHistory();

      await fetchJobHistory('integration-1');
      expect(history.value).toEqual(history1);
      expect(mockGetJobHistory).toHaveBeenCalledWith('integration-1');

      await fetchJobHistory('integration-2');
      expect(history.value).toEqual(history2);
      expect(mockGetJobHistory).toHaveBeenCalledWith('integration-2');
    });
  });
});
