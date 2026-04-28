/* eslint-disable simple-import-sort/imports */
import { describe, beforeEach, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';

const pushMock = vi.fn();
let currentPath = '/';
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: (route: string) => {
      currentPath = route;
      pushMock(route);
      return Promise.resolve();
    },
    currentRoute: { value: { path: currentPath } },
  }),
}));

let hasSuper = true;
let hasTenantAdmin = true;
let featureSet: Record<string, boolean> = {
  feature_integration: true,
  feature_jira_webhook: true,
  feature_arcgis_integration: true,
  feature_confluence_integration: true,
};
vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    hasRole: (role: string) => {
      if (role === 'app_admin') return hasSuper;
      if (role === 'tenant_admin') return hasTenantAdmin;
      return featureSet[role] ?? false;
    },
    get userRoles() {
      const roles: string[] = [];
      if (hasSuper) roles.push('app_admin');
      if (hasTenantAdmin) roles.push('tenant_admin');
      return [...roles, ...Object.keys(featureSet).filter(k => featureSet[k])];
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
      feature_confluence_integration: true,
    };
    currentPath = '/';
    pushMock.mockReset();
  });

  it('shows admin-only items for super admin and hides them for tenant admin only', async () => {
    const wrapper = mount(SideNav, { props: { collapsed: false } });
    await wrapper.find('[data-menu-key="admin"]').trigger('click');

    const adminItems = wrapper
      .findAll('.submenu-item')
      .map(node => node.text())
      .join('|');
    expect(adminItems).toContain('Clear Cache');
    expect(adminItems).toContain('Cache Statistics');
    expect(adminItems).toContain('Site Config');

    hasSuper = false;
    const wrapper2 = mount(SideNav, { props: { collapsed: false } });
    await wrapper2.find('[data-menu-key="admin"]').trigger('click');

    const tenantAdminItems = wrapper2
      .findAll('.submenu-item')
      .map(node => node.text())
      .join('|');
    expect(tenantAdminItems).not.toContain('Clear Cache');
    expect(tenantAdminItems).not.toContain('Cache Statistics');
    expect(tenantAdminItems).toContain('Site Config');
  });

  it('hover expand and collapse closes submenus', async () => {
    const wrapper = mount(SideNav, { props: { collapsed: true }, attachTo: document.body });

    await wrapper.trigger('mouseenter');
    await wrapper.find('[data-menu-key="outbound"]').trigger('click');
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(true);

    await wrapper.trigger('mouseleave');
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(false);
  });

  it('navigates for a different route and suppresses duplicate navigation', async () => {
    currentPath = '/admin/audit-log';
    const wrapper = mount(SideNav, { props: { collapsed: false } });

    await wrapper.find('[data-menu-key="admin"]').trigger('click');
    const siteConfig = wrapper
      .findAll('.submenu-item')
      .find(node => node.text().includes('Site Config'));
    expect(siteConfig).toBeTruthy();

    await siteConfig!.trigger('click');
    expect(currentPath).toBe('/admin/site-config');
    expect(pushMock).toHaveBeenCalledWith('/admin/site-config');

    currentPath = '/admin/site-config';
    pushMock.mockClear();

    const wrapper2 = mount(SideNav, { props: { collapsed: false } });
    await wrapper2.find('[data-menu-key="admin"]').trigger('click');
    const sameRouteSiteConfig = wrapper2
      .findAll('.submenu-item')
      .find(node => node.text().includes('Site Config'));
    await sameRouteSiteConfig!.trigger('click');

    expect(pushMock).not.toHaveBeenCalled();
  });

  it('hides menu groups when features and roles are unavailable', () => {
    hasSuper = false;
    hasTenantAdmin = false;
    featureSet = {
      feature_integration: false,
      feature_jira_webhook: false,
      feature_arcgis_integration: false,
      feature_confluence_integration: false,
    };

    const wrapper = mount(SideNav, { props: { collapsed: false } });

    expect(wrapper.find('[data-menu-key="inbound"]').exists()).toBe(false);
    expect(wrapper.find('[data-menu-key="outbound"]').exists()).toBe(false);
    expect(wrapper.find('[data-menu-key="admin"]').exists()).toBe(false);
    expect(wrapper.find('.sidebar-menu-divider').exists()).toBe(false);
  });

  it('shows confluence outbound item only when the feature is enabled', async () => {
    featureSet.feature_jira_webhook = false;
    featureSet.feature_arcgis_integration = false;
    featureSet.feature_confluence_integration = true;

    const wrapper = mount(SideNav, { props: { collapsed: false } });
    await wrapper.find('[data-menu-key="outbound"]').trigger('click');

    const items = wrapper.findAll('.submenu-flyout-outbound .submenu-item');
    expect(items).toHaveLength(1);
    expect(items[0].text()).toContain('Confluence Integration');

    featureSet.feature_confluence_integration = false;
    const wrapper2 = mount(SideNav, { props: { collapsed: false } });
    expect(wrapper2.find('[data-menu-key="outbound"]').exists()).toBe(false);
  });
});
