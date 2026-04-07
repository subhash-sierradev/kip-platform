/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('@/config/keycloak', () => {
  return {
    getToken: vi.fn(() => 'token-abc'),
    getUserInfo: vi.fn(() => ({
      userName: 'Test User',
      userMail: 'test@example.com',
      tenantId: 'tenant-xyz',
      userId: 'user-xyz',
      roles: ['admin', 'viewer'],
    })),
    isAuthenticated: vi.fn(() => true),
    loginIfNeeded: vi.fn(async () => ({ kc: true })),
    logout: vi.fn(async () => {}),
  };
});

import * as keycloak from '@/config/keycloak';
import { useAuthStore } from '@/store/auth';

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('init loads keycloak, token, and user info', async () => {
    const store = useAuthStore();

    expect(store.initialized).toBe(false);
    expect(store.loading).toBe(false);

    const kc = await store.init();
    expect(kc).toEqual({ kc: true });
    expect(store.initialized).toBe(true);
    expect(store.loading).toBe(false);
    expect(store.token).toBe('token-abc');
    expect(store.userInfo?.userName).toBe('Test User');

    // getter uses external isAuthenticated() + token/initialized
    expect(store.isAuthenticated).toBe(true);
    expect(keycloak.loginIfNeeded).toHaveBeenCalledTimes(1);
  });

  it('role helpers work (hasRole, hasAnyRole, hasAllRoles)', async () => {
    const store = useAuthStore();
    await store.init();

    expect(store.hasRole('admin')).toBe(true);
    expect(store.hasRole('missing')).toBe(false);
    expect(store.hasAnyRole(['x', 'viewer'])).toBe(true);
    expect(store.hasAnyRole(['x'])).toBe(false);
    expect(store.hasAllRoles(['admin', 'viewer'])).toBe(true);
    expect(store.hasAllRoles(['admin', 'x'])).toBe(false);
  });

  it('updateToken updates token and userInfo', async () => {
    const store = useAuthStore();
    await store.init();

    store.updateToken('new-token');
    expect(store.token).toBe('new-token');
    // userInfo refreshed via getUserInfo mock
    expect(keycloak.getUserInfo).toHaveBeenCalled();
  });

  it('logout triggers keycloak logout and resets state', async () => {
    const store = useAuthStore();
    await store.init();

    await store.logout();
    expect(keycloak.logout).toHaveBeenCalledTimes(1);
    expect(store.initialized).toBe(false);
    expect(store.token).toBeNull();
    expect(store.userInfo).toBeNull();
  });

  it('init only runs once if already initialized', async () => {
    const store = useAuthStore();

    const kc1 = await store.init();
    expect(keycloak.loginIfNeeded).toHaveBeenCalledTimes(1);

    const kc2 = await store.init();
    expect(keycloak.loginIfNeeded).toHaveBeenCalledTimes(1); // Should not call again
    expect(kc2).toEqual(kc1);
  });

  it('init handles auth errors without throwing', async () => {
    vi.mocked(keycloak.loginIfNeeded).mockRejectedValueOnce(new Error('Auth failed'));

    const store = useAuthStore();
    await expect(store.init()).resolves.toBeUndefined();
    expect(store.loading).toBe(false);
    expect(store.initialized).toBe(false);
    expect(store.token).toBeNull();
    expect(store.userInfo).toBeNull();
  });

  it('logout handles errors and logs to console', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.mocked(keycloak.logout).mockRejectedValueOnce(new Error('Logout failed'));

    const store = useAuthStore();
    await store.init();

    await store.logout();

    expect(consoleErrorSpy).toHaveBeenCalledWith('Logout failed');
    consoleErrorSpy.mockRestore();
  });

  it('getters return correct values', async () => {
    const store = useAuthStore();
    await store.init();

    expect(store.currentUser).toEqual({
      userName: 'Test User',
      userMail: 'test@example.com',
      tenantId: 'tenant-xyz',
      userId: 'user-xyz',
      roles: ['admin', 'viewer'],
    });

    expect(store.userRoles).toEqual(['admin', 'viewer']);
  });

  it('isAuthenticated getter returns false when not initialized', () => {
    const store = useAuthStore();
    expect(store.isAuthenticated).toBe(false);
  });

  it('init allows null token without throwing', async () => {
    vi.mocked(keycloak.getToken).mockReturnValueOnce(null as any);

    const store = useAuthStore();
    await expect(store.init()).resolves.toBeDefined();
    expect(store.initialized).toBe(true);
    expect(store.token).toBeNull();
  });

  it('isAuthenticated getter returns false when keycloak.isAuthenticated returns false', async () => {
    vi.mocked(keycloak.isAuthenticated).mockReturnValueOnce(false);

    const store = useAuthStore();
    await store.init();

    expect(store.isAuthenticated).toBe(false);
  });

  it('userRoles returns empty array when userInfo is null', () => {
    const store = useAuthStore();
    expect(store.userRoles).toEqual([]);
  });

  it('hasRole returns false when userInfo is null', () => {
    const store = useAuthStore();
    expect(store.hasRole('admin')).toBe(false);
  });

  it('hasAnyRole returns false when userInfo is null', () => {
    const store = useAuthStore();
    expect(store.hasAnyRole(['admin', 'viewer'])).toBe(false);
  });

  it('hasAllRoles returns false when userInfo is null', () => {
    const store = useAuthStore();
    expect(store.hasAllRoles(['admin', 'viewer'])).toBe(false);
  });

  it('$reset clears all state', async () => {
    const store = useAuthStore();
    await store.init();

    expect(store.initialized).toBe(true);
    expect(store.token).not.toBeNull();

    store.$reset();

    expect(store.token).toBeNull();
    expect(store.loading).toBe(false);
    expect(store.initialized).toBe(false);
    expect(store.userInfo).toBeNull();
    expect(store.keycloakInstance).toBeNull();
  });

  it('loading state is managed correctly during init', async () => {
    const store = useAuthStore();

    expect(store.loading).toBe(false);

    const initPromise = store.init();
    expect(store.loading).toBe(true);

    await initPromise;
    expect(store.loading).toBe(false);
  });
});
