import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// Mock the API request module
vi.mock('@/api/core/request', () => ({
  request: vi.fn(),
}));

vi.mock('@/api/core/OpenAPI', () => ({
  OpenAPI: { BASE: '' },
}));

import { request } from '@/api/core/request';
import { useArcGISVerification } from '@/composables/useArcGISVerification';

const mockRequest = request as unknown as ReturnType<typeof vi.fn>;

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('useArcGISVerification', () => {
  describe('fetchRecords', () => {
    it('populates records and resets error on successful fetch', async () => {
      const features = [
        { OBJECTID: 1, external_location_id: 'LOC-1' },
        { OBJECTID: 2, external_location_id: 'LOC-2' },
      ];
      mockRequest.mockResolvedValue({
        features,
        fetchedCount: 2,
        offset: 0,
        exceededTransferLimit: false,
      });

      const { records, loading, error, hasMore, fetchRecords } = useArcGISVerification();

      await fetchRecords(0);

      expect(records.value).toEqual(features);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
      expect(hasMore.value).toBe(false);
    });

    it('replaces records when offset is 0 (new search)', async () => {
      mockRequest.mockResolvedValueOnce({
        features: [{ OBJECTID: 1 }],
        fetchedCount: 1,
        offset: 0,
        exceededTransferLimit: false,
      });
      mockRequest.mockResolvedValueOnce({
        features: [{ OBJECTID: 99 }],
        fetchedCount: 1,
        offset: 0,
        exceededTransferLimit: false,
      });

      const { records, fetchRecords } = useArcGISVerification();

      await fetchRecords(0);
      expect(records.value).toHaveLength(1);
      expect(records.value[0].OBJECTID).toBe(1);

      await fetchRecords(0); // new search — replace
      expect(records.value).toHaveLength(1);
      expect(records.value[0].OBJECTID).toBe(99);
    });

    it('appends records when offset is greater than 0 (load more)', async () => {
      mockRequest.mockResolvedValueOnce({
        features: [{ OBJECTID: 1 }],
        fetchedCount: 1,
        offset: 0,
        exceededTransferLimit: true,
      });
      mockRequest.mockResolvedValueOnce({
        features: [{ OBJECTID: 2 }],
        fetchedCount: 1,
        offset: 1000,
        exceededTransferLimit: false,
      });

      const { records, fetchRecords } = useArcGISVerification();

      await fetchRecords(0);
      await fetchRecords(1000); // load more — append

      expect(records.value).toHaveLength(2);
      expect(records.value[1].OBJECTID).toBe(2);
    });

    it('sets hasMore when exceededTransferLimit is true', async () => {
      mockRequest.mockResolvedValue({
        features: [],
        fetchedCount: 0,
        offset: 0,
        exceededTransferLimit: true,
      });

      const { hasMore, fetchRecords } = useArcGISVerification();

      await fetchRecords(0);

      expect(hasMore.value).toBe(true);
    });

    it('sets error and leaves records unchanged on API failure', async () => {
      mockRequest.mockRejectedValue(new Error('Network error'));

      const { records, error, loading, fetchRecords } = useArcGISVerification();

      await fetchRecords(0);

      expect(error.value).toBe('Network error');
      expect(records.value).toEqual([]);
      expect(loading.value).toBe(false);
    });

    it('sets loading=true during fetch and false after', async () => {
      let resolveRequest!: (value: unknown) => void;
      const pending = new Promise(resolve => {
        resolveRequest = resolve;
      });
      mockRequest.mockReturnValue(pending);

      const { loading, fetchRecords } = useArcGISVerification();

      const fetchPromise = fetchRecords(0);
      expect(loading.value).toBe(true);

      resolveRequest({
        features: [],
        fetchedCount: 0,
        offset: 0,
        exceededTransferLimit: false,
      });
      await fetchPromise;

      expect(loading.value).toBe(false);
    });

    it('passes objectId query param when provided', async () => {
      mockRequest.mockResolvedValue({
        features: [],
        fetchedCount: 0,
        offset: 0,
        exceededTransferLimit: false,
      });

      const { fetchRecords } = useArcGISVerification();
      await fetchRecords(0, '42');

      expect(mockRequest).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          query: expect.objectContaining({ objectId: '42', offset: 0 }),
        })
      );
    });

    it('passes locationId query param when provided', async () => {
      mockRequest.mockResolvedValue({
        features: [],
        fetchedCount: 0,
        offset: 0,
        exceededTransferLimit: false,
      });

      const { fetchRecords } = useArcGISVerification();
      await fetchRecords(0, '', 'LOC-001');

      expect(mockRequest).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          query: expect.objectContaining({ locationId: 'LOC-001' }),
        })
      );
    });

    it('does not include blank objectId in query params', async () => {
      mockRequest.mockResolvedValue({
        features: [],
        fetchedCount: 0,
        offset: 0,
        exceededTransferLimit: false,
      });

      const { fetchRecords } = useArcGISVerification();
      await fetchRecords(0, '   ');

      const callArg = mockRequest.mock.calls[0][1] as { query: Record<string, unknown> };
      expect(callArg.query).not.toHaveProperty('objectId');
    });
  });
});
