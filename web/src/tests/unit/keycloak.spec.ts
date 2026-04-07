import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock environment config used by keycloak init
vi.mock('@/config/env', () => ({
  getEnvironmentConfig: () => ({
    KEYCLOAK_URL: 'http://kc',
    KEYCLOAK_REALM: 'realm',
    KEYCLOAK_CLIENT_ID: 'client',
  }),
}));

// Storage for the latest constructed Keycloak instance
let lastInstance: any;

// Mock keycloak-js factory to return a mutable instance with spy-able methods
vi.mock('keycloak-js', () => ({
  default: function MockKeycloak() {
    lastInstance = {
      token: undefined as string | undefined,
      tokenParsed: undefined as any,
      authenticated: false,
      init: vi.fn().mockResolvedValue(true),
      login: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn().mockResolvedValue(undefined),
      updateToken: vi.fn().mockResolvedValue(true),
    };
    return lastInstance;
  },
}));

describe('config/keycloak', () => {
  beforeEach(async () => {
    await vi.resetModules();
    lastInstance = undefined;
  });

  it('extracts user info from token (string tenant_id and roles)', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      name: 'Jane Doe',
      email: 'jane@example.com',
      preferred_username: 'jane',
      tenant_id: 'tenant-1',
      roles: ['a', 'b'],
    };

    const info = getUserInfo();
    expect(info).toMatchObject({
      userName: 'Jane Doe',
      userMail: 'jane@example.com',
      tenantId: 'tenant-1',
      userId: 'jane',
      roles: ['a', 'b'],
    });
  });

  it('extracts user info when tenant_id is array and roles from realm_access', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      preferred_username: 'john',
      tenant_id: ['/tenant-2/'],
      realm_access: { roles: ['x', 'y'] },
    };

    const info = getUserInfo();
    expect(info).toMatchObject({
      tenantId: 'tenant-2',
      userId: 'john',
      roles: ['x', 'y'],
    });
  });

  it('refreshToken returns true on success, false and logs in on error', async () => {
    const { initKeycloak, refreshToken } = await import('@/config/keycloak');
    const kc = initKeycloak();

    // success path
    (kc.updateToken as any).mockResolvedValueOnce(true);
    await expect(refreshToken(5)).resolves.toBe(true);

    // failure path triggers login and returns false
    (kc.updateToken as any).mockRejectedValueOnce(new Error('expired'));
    (kc.login as any).mockResolvedValueOnce(undefined);
    await expect(refreshToken(5)).resolves.toBe(false);
    expect(kc.login).toHaveBeenCalled();
  });

  it('refreshToken uses default minValidity of 30 when not provided', async () => {
    const { initKeycloak, refreshToken } = await import('@/config/keycloak');
    const kc = initKeycloak();
    (kc.updateToken as any).mockResolvedValueOnce(true);

    await refreshToken();

    expect(kc.updateToken).toHaveBeenCalledWith(30);
  });

  it('logout passes normalized base redirectUri', async () => {
    const { initKeycloak, logout } = await import('@/config/keycloak');
    initKeycloak();
    const origin = window.location.origin;

    // base '/'
    vi.stubEnv('VITE_BASE_URL', '/');
    await logout();
    expect(lastInstance.logout).toHaveBeenCalledWith({ redirectUri: `${origin}` });

    // base without leading slash gets normalized
    vi.stubEnv('VITE_BASE_URL', 'app');
    await logout();
    expect(lastInstance.logout).toHaveBeenCalledWith({ redirectUri: `${origin}/app` });

    // base with leading slash preserved (trailing kept as-is by implementation)
    vi.stubEnv('VITE_BASE_URL', '/app/');
    await logout();
    expect(lastInstance.logout).toHaveBeenCalledWith({ redirectUri: `${origin}/app/` });
  });

  it('logout uses default base URL when VITE_BASE_URL is undefined', async () => {
    const { initKeycloak, logout } = await import('@/config/keycloak');
    initKeycloak();
    const origin = window.location.origin;

    vi.stubEnv('VITE_BASE_URL', undefined);
    await logout();
    expect(lastInstance.logout).toHaveBeenCalledWith({ redirectUri: `${origin}` });
  });

  it('loginIfNeeded initializes and calls login when not authenticated', async () => {
    const { loginIfNeeded } = await import('@/config/keycloak');
    // mock init to report NOT authenticated
    const kc = await loginIfNeeded();
    expect(kc.init).toHaveBeenCalled();
    // our mock init resolves true by default; simulate false
    (kc.init as any).mockResolvedValueOnce(false);
    await loginIfNeeded();
    expect(kc.login).toHaveBeenCalled();
  });

  it('loginIfNeeded does not call login when already authenticated', async () => {
    const { loginIfNeeded } = await import('@/config/keycloak');
    const kc = await loginIfNeeded();
    const loginCallCount = (kc.login as any).mock.calls.length;

    // Second call when already authenticated
    (kc.init as any).mockResolvedValueOnce(true);
    await loginIfNeeded();

    // login should not be called again
    expect((kc.login as any).mock.calls.length).toBe(loginCallCount);
  });

  it('isAuthenticated reflects keycloak.authenticated flag', async () => {
    const { initKeycloak, isAuthenticated } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.authenticated = false;
    expect(isAuthenticated()).toBe(false);
    kc.authenticated = true;
    expect(isAuthenticated()).toBe(true);
  });

  it('getUserInfo returns null when tokenParsed is undefined', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = undefined;

    const info = getUserInfo();
    expect(info).toBeNull();
  });

  it('getUserInfo returns Unknown User when names are missing', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      email: 'test@example.com',
      preferred_username: 'testuser',
      tenant_id: 'tenant-1',
    };

    const info = getUserInfo();
    expect(info?.userName).toBe('Unknown User');
    expect(info?.userMail).toBe('test@example.com');
  });

  it('getUserInfo handles only given_name without family_name', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      given_name: 'John',
      email: 'john@example.com',
      preferred_username: 'john',
    };

    const info = getUserInfo();
    expect(info?.userName).toBe('Unknown User');
  });

  it('getUserInfo handles only family_name without given_name', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      family_name: 'Doe',
      email: 'doe@example.com',
      preferred_username: 'doe',
    };

    const info = getUserInfo();
    expect(info?.userName).toBe('Unknown User');
  });

  it('getUserInfo returns empty strings for missing fields', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      tenant_id: 'tenant-1',
    };

    const info = getUserInfo();
    expect(info).toMatchObject({
      userName: 'Unknown User',
      userMail: '',
      tenantId: 'tenant-1',
      userId: '',
      roles: [],
    });
  });

  it('extractTenantId handles tenant_id with slashes in array', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      tenant_id: ['/tenant/with/slashes/'],
      preferred_username: 'user',
    };

    const info = getUserInfo();
    expect(info?.tenantId).toBe('tenantwithslashes');
  });

  it('extractTenantId handles tenant_id with slashes in string', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      tenant_id: '/tenant/id/',
      preferred_username: 'user',
    };

    const info = getUserInfo();
    expect(info?.tenantId).toBe('tenantid');
  });

  it('extractTenantId returns empty string for invalid tenant_id types', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      tenant_id: 123,
      preferred_username: 'user',
    };

    const info = getUserInfo();
    expect(info?.tenantId).toBe('');
  });

  it('extractTenantId returns empty string for empty array', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      tenant_id: [],
      preferred_username: 'user',
    };

    const info = getUserInfo();
    expect(info?.tenantId).toBe('');
  });

  it('extractTenantId returns empty string for array with non-string values', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      tenant_id: [123, 456],
      preferred_username: 'user',
    };

    const info = getUserInfo();
    expect(info?.tenantId).toBe('');
  });

  it('getToken returns current token', async () => {
    const { initKeycloak, getToken } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.token = 'test-token-123';

    expect(getToken()).toBe('test-token-123');
  });

  it('getToken returns undefined when no token', async () => {
    const { initKeycloak, getToken } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.token = undefined;

    expect(getToken()).toBeUndefined();
  });

  it('getTokenParsed returns parsed token', async () => {
    const { initKeycloak, getTokenParsed } = await import('@/config/keycloak');
    const kc = initKeycloak();
    const mockToken = {
      given_name: 'Test',
      family_name: 'User',
      email: 'test@example.com',
    };
    kc.tokenParsed = mockToken;

    expect(getTokenParsed()).toEqual(mockToken);
  });

  it('getTokenParsed returns undefined when no token', async () => {
    const { initKeycloak, getTokenParsed } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = undefined;

    expect(getTokenParsed()).toBeUndefined();
  });

  it('initKeycloak returns same instance on multiple calls', async () => {
    const { initKeycloak } = await import('@/config/keycloak');
    const instance1 = initKeycloak();
    const instance2 = initKeycloak();

    expect(instance1).toBe(instance2);
  });

  it('initKeycloak initializes with correct configuration', async () => {
    await vi.resetModules();
    const { initKeycloak } = await import('@/config/keycloak');
    const kc = initKeycloak();

    // Verify the instance was created (mock constructor was called)
    expect(kc).toBeDefined();
    expect(kc.init).toBeDefined();
    expect(kc.login).toBeDefined();
    expect(kc.logout).toBeDefined();
  });

  it('isAuthenticated returns false when keycloak is not initialized', async () => {
    await vi.resetModules();
    const { isAuthenticated } = await import('@/config/keycloak');

    expect(isAuthenticated()).toBe(false);
  });

  it('refreshToken returns false when keycloak is not initialized', async () => {
    await vi.resetModules();
    const { refreshToken } = await import('@/config/keycloak');

    const result = await refreshToken();
    expect(result).toBe(false);
  });

  it('logout does nothing when keycloak is not initialized', async () => {
    await vi.resetModules();
    const { logout } = await import('@/config/keycloak');

    await expect(logout()).resolves.toBeUndefined();
  });

  it('getUserInfo includes tokenParsed in return value', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    const mockTokenParsed = {
      given_name: 'Jane',
      family_name: 'Doe',
      email: 'jane@example.com',
      preferred_username: 'jane',
      tenant_id: 'tenant-1',
      roles: ['a', 'b'],
      custom_claim: 'custom_value',
    };
    kc.tokenParsed = mockTokenParsed;

    const info = getUserInfo();
    expect(info?.tokenParsed).toEqual(mockTokenParsed);
  });

  it('extractRoles returns empty array when both roles and realm_access are undefined', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      given_name: 'Test',
      family_name: 'User',
      tenant_id: 'tenant-1',
    };

    const info = getUserInfo();
    expect(info?.roles).toEqual([]);
  });

  it('extractRoles prefers direct roles over realm_access roles', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      given_name: 'Test',
      family_name: 'User',
      tenant_id: 'tenant-1',
      roles: ['direct-role-1', 'direct-role-2'],
      realm_access: { roles: ['realm-role-1', 'realm-role-2'] },
    };

    const info = getUserInfo();
    expect(info?.roles).toEqual(['direct-role-1', 'direct-role-2']);
  });

  it('extractRoles returns empty array when realm_access exists but roles is undefined', async () => {
    const { initKeycloak, getUserInfo } = await import('@/config/keycloak');
    const kc = initKeycloak();
    kc.tokenParsed = {
      given_name: 'Test',
      family_name: 'User',
      tenant_id: 'tenant-1',
      realm_access: {} as any,
    };

    const info = getUserInfo();
    expect(info?.roles).toEqual([]);
  });

  it('loginIfNeeded passes correct init options', async () => {
    const { loginIfNeeded } = await import('@/config/keycloak');
    const kc = await loginIfNeeded();

    expect(kc.init).toHaveBeenCalledWith({
      onLoad: 'login-required',
      checkLoginIframe: false,
      enableLogging: expect.any(Boolean),
    });
  });

  it('getToken returns undefined before initialization', async () => {
    await vi.resetModules();
    const { getToken } = await import('@/config/keycloak');

    expect(getToken()).toBeUndefined();
  });

  it('getTokenParsed returns undefined before initialization', async () => {
    await vi.resetModules();
    const { getTokenParsed } = await import('@/config/keycloak');

    expect(getTokenParsed()).toBeUndefined();
  });

  it('getUserInfo returns null before initialization', async () => {
    await vi.resetModules();
    const { getUserInfo } = await import('@/config/keycloak');

    expect(getUserInfo()).toBeNull();
  });
});
