import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { KwDocField } from '@/api/models/KwDocField';
import { useSourceFields } from '@/composables/useSourceFields';

// Mock KwDocService
const mockGetSourceFieldMappings = vi.fn();
vi.mock('@/api/services/KwIntegrationService', () => ({
  KwDocService: {
    getSourceFieldMappings: () => mockGetSourceFieldMappings(),
  },
}));

describe('useSourceFields', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  describe('initial state', () => {
    it('starts with empty fields array', () => {
      const { fields, loading, error, isReady } = useSourceFields();

      expect(fields.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
      expect(isReady.value).toBe(false);
    });
  });

  describe('load', () => {
    it('loads fields successfully', async () => {
      const mockFields: KwDocField[] = [
        { id: 1, fieldName: 'field1', fieldType: 'string' },
        { id: 2, fieldName: 'field2', fieldType: 'string' },
      ];
      mockGetSourceFieldMappings.mockResolvedValue(mockFields);

      const { fields, loading, error, load, isReady } = useSourceFields();

      const loadPromise = load();
      expect(loading.value).toBe(true);

      await loadPromise;

      expect(fields.value).toEqual(mockFields);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
      expect(isReady.value).toBe(true);
    });

    it('handles null response', async () => {
      mockGetSourceFieldMappings.mockResolvedValue(null);

      const { fields, loading, error, load, isReady } = useSourceFields();

      await load();

      expect(fields.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
      expect(isReady.value).toBe(false);
    });

    it('handles empty array response', async () => {
      mockGetSourceFieldMappings.mockResolvedValue([]);

      const { fields, loading, error, load, isReady } = useSourceFields();

      await load();

      expect(fields.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
      expect(isReady.value).toBe(false);
    });

    it('handles error with Error instance', async () => {
      const mockError = new Error('Network error');
      mockGetSourceFieldMappings.mockRejectedValue(mockError);

      const { fields, loading, error, load, isReady } = useSourceFields();

      await load();

      expect(fields.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBe('Network error');
      expect(isReady.value).toBe(false);
      expect(console.error).toHaveBeenCalledWith('Failed to fetch source fields:', mockError);
    });

    it('handles error with non-Error object', async () => {
      mockGetSourceFieldMappings.mockRejectedValue('String error');

      const { fields, loading, error, load, isReady } = useSourceFields();

      await load();

      expect(fields.value).toEqual([]);
      expect(loading.value).toBe(false);
      expect(error.value).toBe('Failed to load source fields');
      expect(isReady.value).toBe(false);
    });

    it('prevents concurrent loads', async () => {
      mockGetSourceFieldMappings.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve([]), 100))
      );

      const { load } = useSourceFields();

      const firstLoad = load();
      const secondLoad = load(); // Should be ignored

      await firstLoad;
      await secondLoad;

      expect(mockGetSourceFieldMappings).toHaveBeenCalledTimes(1);
    });

    it('clears previous error on new load attempt', async () => {
      const mockError = new Error('First error');
      mockGetSourceFieldMappings.mockRejectedValueOnce(mockError);

      const { error, load } = useSourceFields();

      await load();
      expect(error.value).toBe('First error');

      // Second load succeeds
      mockGetSourceFieldMappings.mockResolvedValue([{ fieldName: 'field1' } as KwDocField]);
      await load();

      expect(error.value).toBeNull();
    });

    it('allows load after previous load completes', async () => {
      mockGetSourceFieldMappings
        .mockResolvedValueOnce([{ fieldName: 'field1' } as KwDocField])
        .mockResolvedValueOnce([{ fieldName: 'field2' } as KwDocField]);

      const { fields, load } = useSourceFields();

      await load();
      expect(fields.value).toHaveLength(1);
      expect(fields.value[0].fieldName).toBe('field1');

      await load();
      expect(fields.value).toHaveLength(1);
      expect(fields.value[0].fieldName).toBe('field2');
      expect(mockGetSourceFieldMappings).toHaveBeenCalledTimes(2);
    });
  });

  describe('isReady computed', () => {
    it('returns false when loading', async () => {
      mockGetSourceFieldMappings.mockImplementation(
        () =>
          new Promise(resolve =>
            setTimeout(() => resolve([{ fieldName: 'field1' } as KwDocField]), 100)
          )
      );

      const { isReady, load } = useSourceFields();

      const loadPromise = load();
      expect(isReady.value).toBe(false);

      await loadPromise;
      expect(isReady.value).toBe(true);
    });

    it('returns false when fields array is empty', async () => {
      mockGetSourceFieldMappings.mockResolvedValue([]);

      const { isReady, load } = useSourceFields();

      await load();
      expect(isReady.value).toBe(false);
    });

    it('returns false when there is an error', async () => {
      mockGetSourceFieldMappings.mockRejectedValue(new Error('Failed'));

      const { isReady, load } = useSourceFields();

      await load();
      expect(isReady.value).toBe(false);
    });

    it('returns true only when loaded with fields', async () => {
      mockGetSourceFieldMappings.mockResolvedValue([{ fieldName: 'field1' } as KwDocField]);

      const { isReady, load } = useSourceFields();

      await load();
      expect(isReady.value).toBe(true);
    });
  });
});
