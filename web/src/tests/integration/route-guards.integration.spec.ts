import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import { allRolesGuard, authGuard, roleGuard } from '@/router/guards';

const keycloakMocks = vi.hoisted(() => ({
  loginIfNeededMock: vi.fn(),
  getTokenMock: vi.fn(),
  getUserInfoMock: vi.fn(),
  isAuthenticatedMock: vi.fn(),
  logoutMock: vi.fn(),
}));

vi.mock('@/config/keycloak', () => ({
  loginIfNeeded: keycloakMocks.loginIfNeededMock,
  getToken: keycloakMocks.getTokenMock,
  getUserInfo: keycloakMocks.getUserInfoMock,
  isAuthenticated: keycloakMocks.isAuthenticatedMock,
  logout: keycloakMocks.logoutMock,
}));

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>home</div>' } },
      { path: '/login', component: { template: '<div>login-page</div>' } },
      { path: '/unauthorized', component: { template: '<div>unauthorized-page</div>' } },
      {
        path: '/protected-auth',
        component: { template: '<div>protected-auth</div>' },
        beforeEnter: authGuard,
      },
      {
        path: '/protected-role',
        component: { template: '<div>protected-role</div>' },
        beforeEnter: roleGuard(['tenant_admin']),
      },
      {
        path: '/protected-all-roles',
        component: { template: '<div>protected-all-roles</div>' },
        beforeEnter: allRolesGuard(['tenant_admin', 'feature_arcgis_integration']),
      },
    ],
  });
}

let mountedWrapper: ReturnType<typeof mount> | null = null;

async function mountGuardHarness() {
  const pinia = createPinia();
  const router = createTestRouter();

  const wrapper = mount(
    {
      template: '<router-view />',
    },
    {
      global: {
        plugins: [pinia, router],
      },
    }
  );

  mountedWrapper = wrapper;

  await router.push('/');
  await router.isReady();

  return { wrapper, router };
}

describe('Route guards integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    keycloakMocks.loginIfNeededMock.mockResolvedValue({ token: 'token' });
    keycloakMocks.getTokenMock.mockReturnValue('token');
    keycloakMocks.getUserInfoMock.mockReturnValue({
      userName: 'UI Tester',
      userMail: 'ui@example.com',
      tenantId: 'tenant-1',
      userId: 'user-1',
      roles: ['tenant_admin'],
    });
    keycloakMocks.isAuthenticatedMock.mockReturnValue(true);
  });

  afterEach(() => {
    mountedWrapper?.unmount();
    mountedWrapper = null;
  });

  it('redirects unauthenticated users to login through authGuard', async () => {
    keycloakMocks.getTokenMock.mockReturnValue(undefined);
    keycloakMocks.getUserInfoMock.mockReturnValue(null);
    keycloakMocks.isAuthenticatedMock.mockReturnValue(false);

    const { router, wrapper } = await mountGuardHarness();

    await router.push('/protected-auth');
    await flushPromises();

    expect(keycloakMocks.loginIfNeededMock).toHaveBeenCalledTimes(1);
    expect(router.currentRoute.value.path).toBe('/login');
    expect(wrapper.text()).toContain('login-page');
  });

  it('allows authenticated users with the required role through roleGuard', async () => {
    const { router, wrapper } = await mountGuardHarness();

    await router.push('/protected-role');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/protected-role');
    expect(wrapper.text()).toContain('protected-role');
  });

  it('redirects authenticated users without the required role to unauthorized', async () => {
    keycloakMocks.getUserInfoMock.mockReturnValue({
      userName: 'UI Tester',
      userMail: 'ui@example.com',
      tenantId: 'tenant-1',
      userId: 'user-1',
      roles: ['feature_jira_webhook'],
    });

    const { router, wrapper } = await mountGuardHarness();

    await router.push('/protected-role');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/unauthorized');
    expect(wrapper.text()).toContain('unauthorized-page');
  });

  it('redirects to login when auth initialization throws', async () => {
    keycloakMocks.loginIfNeededMock.mockRejectedValueOnce(new Error('keycloak init failed'));

    const { router, wrapper } = await mountGuardHarness();

    await router.push('/protected-auth');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/login');
    expect(wrapper.text()).toContain('login-page');
  });

  it('allows access through allRolesGuard when user has all required roles', async () => {
    keycloakMocks.getUserInfoMock.mockReturnValue({
      userName: 'UI Tester',
      userMail: 'ui@example.com',
      tenantId: 'tenant-1',
      userId: 'user-1',
      roles: ['tenant_admin', 'feature_arcgis_integration'],
    });

    const { router, wrapper } = await mountGuardHarness();

    await router.push('/protected-all-roles');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/protected-all-roles');
    expect(wrapper.text()).toContain('protected-all-roles');
  });

  it('redirects to unauthorized through allRolesGuard when user has only tenant_admin', async () => {
    keycloakMocks.getUserInfoMock.mockReturnValue({
      userName: 'UI Tester',
      userMail: 'ui@example.com',
      tenantId: 'tenant-1',
      userId: 'user-1',
      roles: ['tenant_admin'],
    });

    const { router, wrapper } = await mountGuardHarness();

    await router.push('/protected-all-roles');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/unauthorized');
    expect(wrapper.text()).toContain('unauthorized-page');
  });

  it('redirects to unauthorized through allRolesGuard when user has only feature_arcgis_integration', async () => {
    keycloakMocks.getUserInfoMock.mockReturnValue({
      userName: 'UI Tester',
      userMail: 'ui@example.com',
      tenantId: 'tenant-1',
      userId: 'user-1',
      roles: ['feature_arcgis_integration'],
    });

    const { router, wrapper } = await mountGuardHarness();

    await router.push('/protected-all-roles');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/unauthorized');
    expect(wrapper.text()).toContain('unauthorized-page');
  });
});
