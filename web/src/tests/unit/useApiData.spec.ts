/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';
import { useApiData } from '@/composables/useApiData';

describe('useApiData', () => {
  it('initializes with default state', () => {
    const api = useApiData<number>();
    expect(api.data.value).toEqual([]);
    expect(api.loading.value).toBe(false);
    expect(api.error.value).toBeNull();
    expect(api.hasData.value).toBe(false);
    expect(api.hasError.value).toBe(false);
  });

  it('supports custom initialValue', () => {
    const api = useApiData<number>([1, 2, 3]);
    expect(api.data.value).toEqual([1, 2, 3]);
    expect(api.hasData.value).toBe(true);
    expect(api.hasError.value).toBe(false);
  });

  it('setData updates data and clears error', () => {
    const api = useApiData<string>();
    api.setError('x');
    api.setData(['a', 'b']);
    expect(api.data.value).toEqual(['a', 'b']);
    expect(api.error.value).toBeNull();
    expect(api.hasData.value).toBe(true);
    expect(api.hasError.value).toBe(false);
  });

  it('setError sets error and empties data', () => {
    const api = useApiData<string>(['z']);
    api.setError('boom');
    expect(api.error.value).toBe('boom');
    expect(api.data.value).toEqual([]);
    expect(api.hasError.value).toBe(true);
    expect(api.hasData.value).toBe(false);
  });

  it('setLoading toggles loading flag', () => {
    const api = useApiData<number>();
    api.setLoading(true);
    expect(api.loading.value).toBe(true);
    api.setLoading(false);
    expect(api.loading.value).toBe(false);
  });

  it('reset clears data, loading, and error', () => {
    const api = useApiData<number>();
    api.setLoading(true);
    api.setData([5]);
    api.setError('err');
    api.reset();

    expect(api.data.value).toEqual([]);
    expect(api.loading.value).toBe(false);
    expect(api.error.value).toBeNull();
    expect(api.hasData.value).toBe(false);
    expect(api.hasError.value).toBe(false);
  });
});
