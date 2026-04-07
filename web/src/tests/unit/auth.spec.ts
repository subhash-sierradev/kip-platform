import { createPinia, setActivePinia } from 'pinia';
import { describe, expect, it, vi } from 'vitest';

import { useAuthStore } from '@/store/auth';

vi.mock('@/config/keycloak', () => ({
  loginIfNeeded: vi.fn().mockResolvedValue({
    token: 'abc123',
    init: vi.fn(),
    authenticated: true,
  }),
  getToken: vi.fn().mockReturnValue('abc123'),
  getUserInfo: vi.fn().mockReturnValue({
    userName: 'Test User',
    userMail: 'test@example.com',
    tenantId: 'test-tenant',
    userId: 'test-user',
    roles: ['user'],
  }),
  isAuthenticated: vi.fn().mockReturnValue(true),
  logout: vi.fn().mockResolvedValue(undefined),
}));

describe('auth store', () => {
  it('initializes and sets token', async () => {
    setActivePinia(createPinia());
    const store = useAuthStore();
    await store.init();
    expect(store.token).toBe('abc123');
  });
});
