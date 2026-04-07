/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock env
vi.mock('@/config/env', () => ({
  env: { apiBaseUrl: 'https://api.example.test' },
}));

// Mock keycloak helpers
vi.mock('@/config/keycloak', () => ({
  getToken: vi.fn(() => 'mock-token'),
  refreshToken: vi.fn(async () => {}),
}));

// Mock axios to capture interceptors and instance config
vi.mock('axios', () => {
  const create = vi.fn((cfg: any) => {
    const instance: any = {
      config: cfg,
      interceptors: {
        request: {
          use: (onFulfilled: any, _onRejected?: any) => {
            instance.__handlers.requestFulfilled = onFulfilled;
          },
        },
        response: {
          use: (onFulfilled: any, onRejected: any) => {
            instance.__handlers.responseFulfilled = onFulfilled;
            instance.__handlers.responseRejected = onRejected;
          },
        },
      },
      __handlers: {
        requestFulfilled: null,
        responseFulfilled: null,
        responseRejected: null,
      },
    };
    return instance;
  });
  // axios default export is a callable function with a .create method
  const axiosDefault: any = vi.fn();
  axiosDefault.create = create;
  return { default: axiosDefault, create };
});

// Import after mocks, so OpenAPI.ts wires interceptors into our mocked axios instance
import { api } from '@/api/OpenAPI';
const apiAny: any = api;

describe('OpenAPI axios instance', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('uses env.apiBaseUrl and 10s timeout', () => {
    expect(apiAny.config.baseURL).toBe('https://api.example.test');
    expect(apiAny.config.timeout).toBe(10000);
  });

  it('attaches Authorization header when token is present', async () => {
    const kc = await import('@/config/keycloak');
    (kc.getToken as any).mockReturnValue('abc123');

    const cfg: any = { headers: {} };
    const result = await apiAny.__handlers.requestFulfilled(cfg);
    expect(result.headers.Authorization).toBe('Bearer abc123');
  });

  it('does not attach Authorization header when token is missing', async () => {
    const kc = await import('@/config/keycloak');
    (kc.getToken as any).mockReturnValue(null);

    const cfg: any = { headers: {} };
    const result = await apiAny.__handlers.requestFulfilled(cfg);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it('passes through successful responses', async () => {
    const resp: any = { status: 200, data: { ok: true } };
    const result = await apiAny.__handlers.responseFulfilled(resp);
    expect(result).toBe(resp);
  });

  it('on 401: refreshes token once and rejects the error', async () => {
    const kc = await import('@/config/keycloak');
    const err: any = { response: { status: 401 } };
    await expect(apiAny.__handlers.responseRejected(err)).rejects.toBe(err);
    expect(kc.refreshToken).toHaveBeenCalledTimes(1);
  });

  it('on non-401: does not refresh and rejects the error', async () => {
    const kc = await import('@/config/keycloak');
    const err: any = { response: { status: 500 } };
    await expect(apiAny.__handlers.responseRejected(err)).rejects.toBe(err);
    expect(kc.refreshToken).not.toHaveBeenCalled();
  });
});
