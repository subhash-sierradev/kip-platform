/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi } from 'vitest';
import { ref, nextTick } from 'vue';

vi.mock('@/config/env', () => ({
  getApiBaseUrl: vi.fn(),
}));

vi.mock('@/store', () => ({
  useAuthStore: vi.fn(() => ({
    currentUser: { tenantId: 't-1', userId: 'u-1' },
    getToken: () => 'tok-1',
  })),
}));

vi.mock('@/config/keycloak', () => ({
  getToken: vi.fn(),
}));

import { getApiBaseUrl } from '@/config/env';
import { getToken } from '@/config/keycloak';
import { useAuthStore } from '@/store';
import { OpenAPI } from '@/api/core/OpenAPI';
import type { ApiRequestOptions } from '@/api/core/ApiRequestOptions';
import { useConfigureOpenAPI } from '@/composables/useConfigureOpenAPI';

// Helpers to safely resolve union types (string | Resolver<T>) to Promises
const defaultOptions: ApiRequestOptions = {
  method: 'GET',
  url: '',
};

const resolveToken = async (): Promise<string> => {
  const t = OpenAPI.TOKEN;
  if (typeof t === 'function') {
    return await t(defaultOptions);
  }
  return t ?? '';
};

const resolveHeaders = async (): Promise<Record<string, string>> => {
  const h = OpenAPI.HEADERS;
  if (typeof h === 'function') {
    return await h(defaultOptions);
  }
  return h ?? {};
};

describe('useConfigureOpenAPI', () => {
  it('uses reactive ref token value', async () => {
    (getApiBaseUrl as any).mockReturnValue('https://env.example/api');

    const token = ref<string | undefined>('ref-token');
    useConfigureOpenAPI(token);

    await expect(resolveToken()).resolves.toBe('ref-token');
  });

  it('sets BASE from env, TOKEN from ref, and HEADERS from auth store', async () => {
    (getApiBaseUrl as any).mockReturnValue('https://env.example/api');
    (useAuthStore as any).mockReturnValue({
      currentUser: { tenantId: 'tenant-1', userId: 'user-1' },
    });
    vi.mocked(getToken).mockReturnValue(undefined);

    const token = ref<string | undefined>('tok-1');
    useConfigureOpenAPI(token);

    expect(OpenAPI.BASE).toBe('https://env.example/api');
    await expect(resolveToken()).resolves.toBe('tok-1');
    await expect(resolveHeaders()).resolves.toEqual({
      'X-TENANT-ID': 'tenant-1',
      'X-USER-ID': 'user-1',
    });

    token.value = 'tok-2';
    await nextTick();
    await expect(resolveToken()).resolves.toBe('tok-2');
  });

  it('defaults BASE to localhost in dev when env missing', async () => {
    (getApiBaseUrl as any).mockReturnValue(undefined);
    (useAuthStore as any).mockReturnValue({ currentUser: { tenantId: 't', userId: 'u' } });
    vi.mocked(getToken).mockReturnValue(undefined);

    const token = ref<string | undefined>(undefined);
    useConfigureOpenAPI(token);

    expect(OpenAPI.BASE).toBe('http://localhost:8085/api');
    await expect(resolveToken()).resolves.toBe('');
    await expect(resolveHeaders()).resolves.toEqual({ 'X-TENANT-ID': 't', 'X-USER-ID': 'u' });
  });

  it('falls back to production BASE when not localhost and env missing', async () => {
    (getApiBaseUrl as any).mockReturnValue(undefined);
    (useAuthStore as any).mockReturnValue({ currentUser: {} });
    vi.mocked(getToken).mockReturnValue(undefined);

    const originalLocation = window.location;
    // Override hostname to simulate non-dev environment
    Object.defineProperty(window, 'location', {
      value: { hostname: 'example.com' } as Location,
      configurable: true,
    });

    const token = ref<string | undefined>('tok-x');
    useConfigureOpenAPI(token);

    expect(OpenAPI.BASE).toBe('https://kaseware.sierradev.com/backend/api');
    await expect(resolveHeaders()).resolves.toEqual({});

    // Restore original location
    Object.defineProperty(window, 'location', { value: originalLocation, configurable: true });
  });
});
