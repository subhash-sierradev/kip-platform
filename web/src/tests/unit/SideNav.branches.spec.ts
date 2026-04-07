/* eslint-disable simple-import-sort/imports */
import { describe, beforeEach, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';

// Mock router
const pushMock = vi.fn();
let currentPath = '/';
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: (route: string) => {
      currentPath = route;
      return Promise.resolve();
    },
    currentRoute: { value: { path: currentPath } },
  }),
}));

// Mock auth store for role-based visibility
let hasSuper = true;
let hasTenantAdmin = true;
let featureSet: Record<string, boolean> = {
  feature_integration: true,
  feature_jira_webhook: true,
  feature_arcgis_integration: true,
};
vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    hasRole: (role: string) => {
      if (role === 'app_admin') return hasSuper;
      if (role === 'tenant_admin') return hasTenantAdmin;
      return featureSet[role] ?? false;
    },
  }),
}));

import SideNav from '@/components/layout/SideNav.vue';

describe('SideNav branches', () => {
  beforeEach(() => {
    hasSuper = true;
    hasTenantAdmin = true;
    featureSet = {
      feature_integration: true,
      feature_jira_webhook: true,
      feature_arcgis_integration: true,
    };
    currentPath = '/';
    pushMock.mockReset();
  });

  it('shows admin-only items for super admin and hides when not super admin', async () => {
    const wrapper = mount(SideNav, { props: { collapsed: false } });
    // Open admin submenu
    await wrapper.find('[data-menu-key="admin"]').trigger('click');
    expect(wrapper.find('.submenu-flyout-admin').exists()).toBe(true);
    // Super admin shows Clear Cache, Cache Statistics, and Site Config
    const adminItems = wrapper
      .findAll('.submenu-item')
      .map(n => n.text())
      .join('|');
    expect(adminItems).toContain('Clear Cache');
    expect(adminItems).toContain('Cache Statistics');
    expect(adminItems).toContain('Site Config');

    // Re-mount as non-super admin (but still tenant_admin to see admin menu)
    hasSuper = false;
    hasTenantAdmin = true; // Keep tenant_admin so admin menu is still visible
    const wrapper2 = mount(SideNav, { props: { collapsed: false } });
    await wrapper2.find('[data-menu-key="admin"]').trigger('click');
    const nonSuperAdminItems = wrapper2
      .findAll('.submenu-item')
      .map(n => n.text())
      .join('|');
    expect(nonSuperAdminItems).not.toContain('Clear Cache');
    expect(nonSuperAdminItems).not.toContain('Cache Statistics');
    expect(nonSuperAdminItems).toContain('Site Config'); // tenant_admin can see this
  });

  it('hover expand/collapse toggles and closes submenus on collapse', async () => {
    const wrapper = mount(SideNav, { props: { collapsed: true }, attachTo: document.body });
    // Hover to expand
    await wrapper.trigger('mouseenter');
    // Open outbound submenu
    await wrapper.find('[data-menu-key="outbound"]').trigger('click');
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(true);
    // Mouseleave collapses and closes submenu
    await wrapper.trigger('mouseleave');
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(false);
  });

  it('navigateToRoute pushes on different route and suppresses when same (super admin only)', async () => {
    currentPath = '/admin/audit-log';
    const wrapper = mount(SideNav, { props: { collapsed: false } });
    // Open admin submenu and click Site Config (different route) - only visible to super admin
    await wrapper.find('[data-menu-key="admin"]').trigger('click');
    const siteConfig = wrapper
      .findAll('.submenu-item')
      .find(n => n.text().includes('Site Config'))!;
    expect(siteConfig).toBeTruthy(); // Should be present for super admin
    await siteConfig.trigger('click');
    expect(currentPath).toBe('/admin/site-config');

    // Click same route again — no change
    await siteConfig.trigger('click');
    expect(currentPath).toBe('/admin/site-config');

    // Test that non-super admin (only tenant_admin) cannot see app_admin items but can see tenant_admin items
    hasSuper = false;
    hasTenantAdmin = true; // Keep tenant_admin so admin menu is still visible
    const wrapper2 = mount(SideNav, { props: { collapsed: false } });
    await wrapper2.find('[data-menu-key="admin"]').trigger('click');
    const siteConfigNonSuper = wrapper2
      .findAll('.submenu-item')
      .find(n => n.text().includes('Site Config'));
    expect(siteConfigNonSuper).toBeTruthy(); // Should be present for tenant_admin

    const clearCacheNonSuper = wrapper2
      .findAll('.submenu-item')
      .find(n => n.text().includes('Clear Cache'));
    expect(clearCacheNonSuper).toBeFalsy(); // Should NOT be present for non-app_admin
  });
});
