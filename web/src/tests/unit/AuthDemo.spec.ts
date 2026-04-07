/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';

vi.mock('@/store/auth', () => {
  let mockStore: any = null;
  return {
    useAuthStore: vi.fn(() => mockStore),
    __setMockAuthStore: (s: any) => {
      mockStore = s;
    },
  };
});

import AuthDemo from '@/components/AuthDemo.vue';

describe('AuthDemo.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state', async () => {
    const authModule = await import('@/store/auth');
    (authModule as any).__setMockAuthStore({
      loading: true,
      isAuthenticated: false,
      currentUser: null,
      hasAnyRole: vi.fn(() => false),
      logout: vi.fn(async () => {}),
    });

    const wrapper = mount(AuthDemo);
    expect(wrapper.find('.loading').text()).toContain('Loading authentication');
  });

  it('renders authenticated user details and supports logout', async () => {
    const authModule = await import('@/store/auth');
    const logout = vi.fn(async () => {});
    (authModule as any).__setMockAuthStore({
      loading: false,
      isAuthenticated: true,
      currentUser: {
        userName: 'Alice',
        userMail: 'alice@example.com',
        userId: 'user-1',
        tenantId: 'tenant-1',
        roles: ['admin'],
      },
      hasAnyRole: vi.fn(() => true),
      logout,
    });

    const wrapper = mount(AuthDemo);
    expect(wrapper.find('.authenticated h3').text()).toContain('Welcome, Alice');
    const details = wrapper.find('.user-details');
    expect(details.text()).toContain('alice@example.com');
    expect(details.text()).toContain('user-1');
    expect(details.text()).toContain('tenant-1');
    expect(details.text()).toContain('admin');

    // Role-based indicator
    expect(wrapper.text()).toContain('You have admin access');

    // Logout action
    await wrapper.find('.actions button').trigger('click');
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('renders unauthenticated state', async () => {
    const authModule = await import('@/store/auth');
    (authModule as any).__setMockAuthStore({
      loading: false,
      isAuthenticated: false,
      currentUser: null,
      hasAnyRole: vi.fn(() => false),
      logout: vi.fn(async () => {}),
    });

    const wrapper = mount(AuthDemo);
    expect(wrapper.find('.not-authenticated h3').text()).toContain('Not Authenticated');
    expect(wrapper.text()).toContain('Please log in');
  });
});
