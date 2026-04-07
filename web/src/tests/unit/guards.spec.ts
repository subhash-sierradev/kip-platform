/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { NavigationGuardNext } from 'vue-router';

vi.mock('@/store/auth', () => {
  let mockStore: any = null;
  return {
    useAuthStore: vi.fn(() => mockStore),
    __setMockAuthStore: (s: any) => {
      mockStore = s;
    },
  };
});

import { authGuard, roleGuard } from '@/router/guards';

describe('router guards', () => {
  let next: NavigationGuardNext;

  beforeEach(() => {
    next = vi.fn() as unknown as NavigationGuardNext;
    vi.clearAllMocks();
  });

  describe('authGuard', () => {
    it('allows navigation when already initialized and authenticated', async () => {
      const authModule = await import('@/store/auth');
      (authModule as any).__setMockAuthStore({
        initialized: true,
        isAuthenticated: true,
        init: vi.fn(),
      });

      await authGuard({} as any, {} as any, next);
      expect(authModule.useAuthStore).toHaveBeenCalledTimes(1);
      // init should not be called if already initialized
      expect(authModule.useAuthStore().init).not.toHaveBeenCalled();
      expect(next).toHaveBeenCalledTimes(1);
      expect(next).toHaveBeenCalledWith();
    });

    it('initializes and allows when authenticated after init', async () => {
      const authModule = await import('@/store/auth');
      const init = vi.fn(async () => {});
      (authModule as any).__setMockAuthStore({
        initialized: false,
        isAuthenticated: true,
        init,
      });

      await authGuard({} as any, {} as any, next);
      expect(init).toHaveBeenCalledTimes(1);
      expect(next).toHaveBeenCalledWith();
    });

    it('redirects to /login when unauthenticated', async () => {
      const authModule = await import('@/store/auth');
      (authModule as any).__setMockAuthStore({
        initialized: true,
        isAuthenticated: false,
        init: vi.fn(),
      });

      await authGuard({} as any, {} as any, next);
      expect(next).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when init throws', async () => {
      const authModule = await import('@/store/auth');
      const init = vi.fn(async () => {
        throw new Error('init failed');
      });
      (authModule as any).__setMockAuthStore({
        initialized: false,
        isAuthenticated: false,
        init,
      });

      await authGuard({} as any, {} as any, next);
      expect(init).toHaveBeenCalledTimes(1);
      expect(next).toHaveBeenCalledWith('/login');
    });
  });

  describe('roleGuard', () => {
    it('redirects to /login when not authenticated after init', async () => {
      const authModule = await import('@/store/auth');
      const init = vi.fn(async () => {});
      (authModule as any).__setMockAuthStore({
        initialized: false,
        isAuthenticated: false,
        hasAnyRole: vi.fn(() => false),
        init,
      });

      const guard = roleGuard(['some_role']);
      await guard({} as any, {} as any, next);
      expect(init).toHaveBeenCalledTimes(1);
      expect(next).toHaveBeenCalledWith('/login');
    });

    it('redirects to /unauthorized when missing required roles', async () => {
      const authModule = await import('@/store/auth');
      (authModule as any).__setMockAuthStore({
        initialized: true,
        isAuthenticated: true,
        hasAnyRole: vi.fn(() => false),
        init: vi.fn(),
      });

      const guard = roleGuard(['admin']);
      await guard({} as any, {} as any, next);
      expect(authModule.useAuthStore().hasAnyRole).toHaveBeenCalledWith(['admin']);
      expect(next).toHaveBeenCalledWith('/unauthorized');
    });

    it('allows when has any required role', async () => {
      const authModule = await import('@/store/auth');
      (authModule as any).__setMockAuthStore({
        initialized: true,
        isAuthenticated: true,
        hasAnyRole: vi.fn(() => true),
        init: vi.fn(),
      });

      const guard = roleGuard(['tenant_admin', 'app_admin']);
      await guard({} as any, {} as any, next);
      expect(authModule.useAuthStore().hasAnyRole).toHaveBeenCalledWith([
        'tenant_admin',
        'app_admin',
      ]);
      expect(next).toHaveBeenCalledWith();
    });

    it('redirects to /login when init throws', async () => {
      const authModule = await import('@/store/auth');
      const init = vi.fn(async () => {
        throw new Error('boom');
      });
      (authModule as any).__setMockAuthStore({
        initialized: false,
        isAuthenticated: false,
        hasAnyRole: vi.fn(() => false),
        init,
      });

      const guard = roleGuard(['x']);
      await guard({} as any, {} as any, next);
      expect(init).toHaveBeenCalledTimes(1);
      expect(next).toHaveBeenCalledWith('/login');
    });
  });
});
