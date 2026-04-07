import { beforeEach, describe, expect, it, vi } from 'vitest';

const { listFeatureFields } = vi.hoisted(() => ({
  listFeatureFields: vi.fn(),
}));

vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    listFeatureFields,
  },
}));

import { useArcgisFeatures } from '@/composables/useArcgisFeatures';

describe('useArcgisFeatures', () => {
  const originalNodeEnv = process.env.NODE_ENV;

  beforeEach(() => {
    vi.clearAllMocks();
    process.env.NODE_ENV = originalNodeEnv;
  });

  it('loads fields successfully when response is array', async () => {
    const field = { sourceFieldName: 'name', targetFieldName: 'name' } as any;
    listFeatureFields.mockResolvedValueOnce([field]);

    const { fields, loading, error, load } = useArcgisFeatures();
    await load('connection-1');

    expect(listFeatureFields).toHaveBeenCalledWith('connection-1');
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
    expect(fields.value).toEqual([field]);
  });

  it('falls back to empty array when API response is not array', async () => {
    listFeatureFields.mockResolvedValueOnce({ unexpected: true });

    const { fields, error, load } = useArcgisFeatures();
    await load('connection-2');

    expect(error.value).toBeNull();
    expect(fields.value).toEqual([]);
  });

  it('handles errors and resets loading/fields', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    listFeatureFields.mockRejectedValueOnce(new Error('Network down'));

    const { fields, loading, error, load } = useArcgisFeatures();
    await load('connection-3');

    expect(loading.value).toBe(false);
    expect(fields.value).toEqual([]);
    expect(error.value).toBe('Network down');
    errorSpy.mockRestore();
  });

  it('uses fallback error message when rejection has no message', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    listFeatureFields.mockRejectedValueOnce({});

    const { error, fields, load } = useArcgisFeatures();
    await load('connection-4');

    expect(error.value).toBe('Failed to load ArcGIS fields');
    expect(fields.value).toEqual([]);
    errorSpy.mockRestore();
  });

  it('executes development logging branches for array responses', async () => {
    process.env.NODE_ENV = 'development';
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    listFeatureFields.mockResolvedValueOnce([{ sourceFieldName: 'fieldA' } as any]);

    const { load } = useArcgisFeatures();
    await load('connection-dev');

    expect(warnSpy).toHaveBeenCalled();
    expect(warnSpy.mock.calls.length).toBeGreaterThanOrEqual(4);
    warnSpy.mockRestore();
  });
});
