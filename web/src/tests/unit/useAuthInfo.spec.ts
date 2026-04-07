/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';

let getTokenMock = vi.fn(() => 'tok-123');
let getTokenParsedMock = vi.fn(() => ({
  preferred_username: 'alice',
  roles: ['admin'],
  realm_access: { roles: ['admin'] },
}));
let getUserInfoMock = vi.fn(() => ({
  userName: 'Alice',
  userMail: 'alice@example.com',
  tenantId: 'tenant-1',
  userId: 'user-1',
  roles: ['admin', 'viewer'],
}));
let isAuthenticatedMock = vi.fn(() => true);
let loginIfNeededMock = vi.fn(async () => ({ fake: 'kc' }));
let logoutMock = vi.fn(async () => {});

vi.mock('@/config/keycloak', () => {
  return {
    getToken: () => getTokenMock(),
    getTokenParsed: () => getTokenParsedMock(),
    getUserInfo: () => getUserInfoMock(),
    isAuthenticated: () => isAuthenticatedMock(),
    loginIfNeeded: () => loginIfNeededMock(),
    logout: () => logoutMock(),
  };
});

import { useAuthInfo } from '@/composables/useAuthInfo';

describe('useAuthInfo', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getTokenMock = vi.fn(() => 'tok-123');
    getTokenParsedMock = vi.fn(() => ({
      preferred_username: 'alice',
      roles: ['admin'],
      realm_access: { roles: ['admin'] },
    }));
    getUserInfoMock = vi.fn(() => ({
      userName: 'Alice',
      userMail: 'alice@example.com',
      tenantId: 'tenant-1',
      userId: 'user-1',
      roles: ['admin', 'viewer'],
    }));
    isAuthenticatedMock = vi.fn(() => true);
    loginIfNeededMock = vi.fn(async () => ({ fake: 'kc' }));
    logoutMock = vi.fn(async () => {});
  });

  it('init loads keycloak and populates authInfo', async () => {
    const { authInfo, initialized, init } = useAuthInfo();

    expect(initialized.value).toBe(false);

    const kc = await init();
    expect(kc).toEqual({ fake: 'kc' });
    expect(initialized.value).toBe(true);

    const info = authInfo.value;
    expect(info.isAuth).toBe(true);
    expect(info.token).toBe('tok-123');
    expect(info.userName).toBe('Alice');
    expect(info.userMail).toBe('alice@example.com');
    expect(info.tenantId).toBe('tenant-1');
    expect(info.userId).toBe('user-1');
    expect(info.roles).toEqual(['admin', 'viewer']);
    expect(info.tokenParsed.preferred_username).toBe('alice');
  });

  it('init returns keycloak instance if already initialized', async () => {
    const { init } = useAuthInfo();

    // First call - should initialize
    const kc1 = await init();
    expect(kc1).toEqual({ fake: 'kc' });

    // Second call - should return cached instance
    const kc2 = await init();
    expect(kc1).toEqual(kc2);
    expect(kc1).toBe(kc2); // Same reference
  });

  it('logout resets initialized and calls keycloak logout', async () => {
    const { initialized, init, logout } = useAuthInfo();

    await init();
    expect(initialized.value).toBe(true);

    await logout();
    expect(initialized.value).toBe(false);
    expect(logoutMock).toHaveBeenCalledTimes(1);
  });

  it('role helpers evaluate roles correctly', async () => {
    const { init, hasRole, hasAnyRole, hasAllRoles } = useAuthInfo();
    await init();

    expect(hasRole('admin')).toBe(true);
    expect(hasRole('unknown')).toBe(false);

    expect(hasAnyRole(['unknown', 'viewer'])).toBe(true);
    expect(hasAnyRole(['unknown'])).toBe(false);

    expect(hasAllRoles(['admin', 'viewer'])).toBe(true);
    expect(hasAllRoles(['admin', 'unknown'])).toBe(false);
  });

  it('handles missing user info gracefully with defaults', async () => {
    getUserInfoMock = vi.fn(() => ({
      userName: '',
      userMail: '',
      tenantId: '',
      userId: '',
      roles: [],
    })) as any;

    const { authInfo, init } = useAuthInfo();
    await init();

    const info = authInfo.value;
    expect(info.userName).toBe('Unknown User');
    expect(info.userMail).toBe('');
    expect(info.tenantId).toBe('');
    expect(info.userId).toBe('');
    expect(info.roles).toEqual([]);
  });

  it('handles missing token gracefully', async () => {
    getTokenMock = vi.fn(() => undefined) as any;

    const { authInfo, init } = useAuthInfo();
    await init();

    const info = authInfo.value;
    expect(info.token).toBeUndefined();
  });

  it('handles null token parsed gracefully', async () => {
    getTokenParsedMock = vi.fn(() => null) as any;

    const { authInfo, init } = useAuthInfo();
    await init();

    const info = authInfo.value;
    expect(info.tokenParsed).toEqual({});
  });

  it('handles null user info gracefully', async () => {
    getUserInfoMock = vi.fn(() => null) as any;

    const { authInfo, init } = useAuthInfo();
    await init();

    const info = authInfo.value;
    expect(info.userName).toBe('Unknown User');
    expect(info.userMail).toBe('');
  });

  it('reflects unauthenticated state when not authenticated', async () => {
    isAuthenticatedMock = vi.fn(() => false);

    const { authInfo, init } = useAuthInfo();
    await init();

    const info = authInfo.value;
    expect(info.isAuth).toBe(false);
  });

  it('reflects unauthenticated state when no token', async () => {
    getTokenMock = vi.fn(() => null) as any;

    const { authInfo, init } = useAuthInfo();
    await init();

    const info = authInfo.value;
    expect(info.isAuth).toBe(false);
  });

  it('reflects unauthenticated state when not initialized', () => {
    isAuthenticatedMock = vi.fn(() => false);
    getTokenMock = vi.fn(() => undefined) as any;

    const { authInfo } = useAuthInfo();

    const info = authInfo.value;
    expect(info.isAuth).toBe(false);
  });

  it('exposes loading state correctly', async () => {
    const { loading, init } = useAuthInfo();

    expect(loading.value).toBe(false);

    const initPromise = init();
    // During init, loading might be true briefly

    await initPromise;
    expect(loading.value).toBe(false);
  });

  it('exposes keycloak instance in authInfo', async () => {
    const { authInfo, init } = useAuthInfo();

    await init();

    const info = authInfo.value;
    expect(info.keycloak).toBeDefined();
    expect(info.keycloak).toEqual({ fake: 'kc' });
  });

  it('role helpers work with empty roles', async () => {
    getUserInfoMock = vi.fn(() => ({
      userName: 'Bob',
      userMail: 'bob@example.com',
      tenantId: 'tenant-2',
      userId: 'user-2',
      roles: [],
    }));

    const { init, hasRole, hasAnyRole, hasAllRoles } = useAuthInfo();
    await init();

    expect(hasRole('admin')).toBe(false);
    expect(hasAnyRole(['admin', 'viewer'])).toBe(false);
    expect(hasAllRoles([])).toBe(true); // All of no roles = true
  });
});
