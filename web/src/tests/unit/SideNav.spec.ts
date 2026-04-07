import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';

import SideNav from '@/components/layout/SideNav.vue';

// Mock auth store with flexible role handling
vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    hasRole: (role: string) =>
      [
        'tenant_admin',
        'app_admin',
        'feature_integration',
        'feature_jira_webhook',
        'feature_arcgis_integration',
      ].includes(role),
    currentUser: { userName: 'jane doe', tenantId: 'AcmeCorp' },
    logout: vi.fn(),
  }),
}));

// Router for navigation assertions
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div />' } },
    { path: '/inbound/integrations', component: { template: '<div />' } },
    { path: '/outbound/webhook/jira', component: { template: '<div />' } },
    { path: '/outbound/integration/arcgis', component: { template: '<div />' } },
    { path: '/admin/audit-log', component: { template: '<div />' } },
    { path: '/admin/clear-cache', component: { template: '<div />' } },
    { path: '/admin/cache-statistics', component: { template: '<div />' } },
    { path: '/admin/site-config', component: { template: '<div />' } },
    { path: '/admin/connections/jira', component: { template: '<div />' } },
    { path: '/admin/connections/arcgis', component: { template: '<div />' } },
    { path: '/admin/notifications', component: { template: '<div />' } },
  ],
});

beforeEach(async () => {
  router.push('/');
  await router.isReady();
});

describe('SideNav.vue', () => {
  it('expands on hover when collapsed', async () => {
    const wrapper = mount(SideNav, {
      props: { collapsed: true },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    expect(wrapper.find('.kaseware-sidebar').classes()).toContain('collapsed');
    await wrapper.find('.kaseware-sidebar').trigger('mouseenter');
    expect(wrapper.find('.kaseware-sidebar').classes()).not.toContain('collapsed');
  });

  it('opens outbound submenu and navigates on click', async () => {
    const pushSpy = vi.spyOn(router, 'push');

    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const outbound = wrapper.find('[data-menu-key="outbound"]');
    await outbound.trigger('click');

    // submenu should render inside component
    const flyout = wrapper.find('.submenu-flyout-outbound');
    expect(flyout.exists()).toBe(true);

    // click submenu item (ArcGIS Integration is now first alphabetically)
    const firstItem = wrapper.find('.submenu-flyout-outbound .submenu-item');
    await firstItem.trigger('click');

    expect(pushSpy).toHaveBeenCalledWith('/outbound/integration/arcgis');
  });

  it('shows tooltip after hover with delay and hides on mouseleave', async () => {
    vi.useFakeTimers();
    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: {
            props: ['title', 'description', 'visible'],
            template:
              '<div class="tooltip-stub" v-if="visible"><span class="tooltip-title">{{ title }}</span></div>',
          },
        },
      },
      attachTo: document.body,
    });

    const inbound = wrapper.find('[data-menu-key="inbound"]');
    await inbound.trigger('mouseenter');
    // Advance timers to pass tooltip delay
    vi.advanceTimersByTime(1100);
    await nextTick();
    await Promise.resolve();

    const tooltip = wrapper.find('.tooltip-stub .tooltip-title');
    expect(tooltip.exists()).toBe(true);
    expect(tooltip.text()).toBe('Inbound');

    // Hide on mouseleave
    await inbound.trigger('mouseleave');
    const tooltipGone = wrapper.find('.tooltip-stub');
    expect(tooltipGone.exists()).toBe(false);
    vi.useRealTimers();
  });

  it('collapses back and closes submenus after hover leave when initially collapsed', async () => {
    const wrapper = mount(SideNav, {
      props: { collapsed: true },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    // Expand via hover
    const sidebar = wrapper.find('.kaseware-sidebar');
    await sidebar.trigger('mouseenter');
    // Open an admin submenu
    const admin = wrapper.find('[data-menu-key="admin"]');
    await admin.trigger('click');
    expect(wrapper.find('.submenu-flyout-admin').exists()).toBe(true);

    // Leave to collapse and close submenus
    await sidebar.trigger('mouseleave');
    expect(wrapper.find('.kaseware-sidebar').classes()).toContain('collapsed');
    expect(wrapper.find('.submenu-flyout-admin').exists()).toBe(false);
  });

  it('navigates to all admin submenu items', async () => {
    const pushSpy = vi.spyOn(router, 'push');

    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const admin = wrapper.find('[data-menu-key="admin"]');
    await admin.trigger('click');

    const flyout = wrapper.find('.submenu-flyout-admin');
    const items = flyout.findAll('.submenu-item');

    // Should have arcgis connect, jira connect, audit-log, clear-cache, cache-statistics, notifications, site-config
    expect(items.length).toBeGreaterThan(3);

    // Click audit logs (now at index 2: ArcGIS Connect=0, Jira Connect=1, Audit Logs=2)
    await items[2].trigger('click');
    expect(pushSpy).toHaveBeenCalledWith('/admin/audit-log');
  });

  it('toggles between multiple parent menus', async () => {
    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const inbound = wrapper.find('[data-menu-key="inbound"]');
    const outbound = wrapper.find('[data-menu-key="outbound"]');

    // Open inbound
    await inbound.trigger('click');
    expect(wrapper.find('.submenu-flyout-inbound').exists()).toBe(true);

    // Open outbound - should close inbound
    await outbound.trigger('click');
    expect(wrapper.find('.submenu-flyout-inbound').exists()).toBe(false);
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(true);
  });

  it('handles multiple clicks on same parent menu', async () => {
    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const outbound = wrapper.find('[data-menu-key="outbound"]');

    // First click - open
    await outbound.trigger('click');
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(true);

    // Second click - close
    await outbound.trigger('click');
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(false);

    // Third click - open again
    await outbound.trigger('click');
    expect(wrapper.find('.submenu-flyout-outbound').exists()).toBe(true);
  });

  it('does not expand on hover when on mobile/touch device', async () => {
    // Mock mobile device
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 500 });
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: query === '(pointer: coarse)',
        media: query,
      })),
    });

    const wrapper = mount(SideNav, {
      props: { collapsed: true },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const sidebar = wrapper.find('.kaseware-sidebar');
    await sidebar.trigger('mouseenter');

    // Should remain collapsed on mobile
    expect(wrapper.find('.kaseware-sidebar').classes()).toContain('collapsed');
  });

  it('clears tooltip timeout when leaving before timeout completes', async () => {
    vi.useFakeTimers();
    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: {
            props: ['visible'],
            template: '<div class="tooltip-stub" v-if="visible">Tooltip</div>',
          },
        },
      },
    });

    const inbound = wrapper.find('[data-menu-key="inbound"]');
    await inbound.trigger('mouseenter');

    // Leave before timeout completes (1000ms)
    vi.advanceTimersByTime(500);
    await inbound.trigger('mouseleave');

    // Advance past original timeout
    vi.advanceTimersByTime(600);
    await nextTick();

    // Tooltip should not appear
    const tooltip = wrapper.find('.tooltip-stub');
    expect(tooltip.exists()).toBe(false);
    vi.useRealTimers();
  });

  it('handles navigation error gracefully', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const pushSpy = vi.spyOn(router, 'push').mockRejectedValue(new Error('Navigation failed'));

    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const outbound = wrapper.find('[data-menu-key="outbound"]');
    await outbound.trigger('click');

    const jiraItem = wrapper.find('.submenu-flyout-outbound .submenu-item');
    await jiraItem.trigger('click');

    await nextTick();
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
    pushSpy.mockRestore();
  });

  it('does not navigate when route is empty or to current route', async () => {
    await router.push('/admin/audit-log');
    const pushSpy = vi.spyOn(router, 'push');

    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    // Programmatically call navigateToRoute with empty/whitespace
    (wrapper.vm as any).navigateToRoute('');
    (wrapper.vm as any).navigateToRoute('   ');
    // Attempt to navigate to same route
    (wrapper.vm as any).navigateToRoute('/admin/audit-log');

    expect(pushSpy).not.toHaveBeenCalled();
  });

  it('closes all submenus when navigating from submenu item', async () => {
    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const admin = wrapper.find('[data-menu-key="admin"]');
    await admin.trigger('click');
    expect(wrapper.find('.submenu-flyout-admin').exists()).toBe(true);

    // Navigate to audit log
    const items = wrapper.findAll('.submenu-item');
    await items[0].trigger('click');

    // Submenu should close after navigation
    expect(wrapper.find('.submenu-flyout-admin').exists()).toBe(false);
  });

  it('hides nav text when collapsed and shows when expanded', () => {
    const collapsedWrapper = mount(SideNav, {
      props: { collapsed: true },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    // v-if="!isSidebarCollapsed" should hide nav text
    expect(collapsedWrapper.findAll('.nav-text').length).toBe(0);

    const expandedWrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    expect(expandedWrapper.findAll('.nav-text').length).toBeGreaterThan(0);
  });

  it('closes inbound submenu when clicking in submenu-flyout', async () => {
    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    const inbound = wrapper.find('[data-menu-key="inbound"]');
    await inbound.trigger('click');
    expect(wrapper.find('.submenu-flyout-inbound').exists()).toBe(true);

    // Navigate from inbound submenu
    const item = wrapper.find('.submenu-flyout-inbound .submenu-item');
    await item.trigger('click');

    // Submenu should close
    expect(wrapper.find('.submenu-flyout-inbound').exists()).toBe(false);
  });

  it('handles mouseenter on menu items to close other submenus', async () => {
    const wrapper = mount(SideNav, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        stubs: {
          AppVersion: { template: '<div />' },
          CommonTooltip: { template: '<div />' },
        },
      },
    });

    // Open admin submenu
    const admin = wrapper.find('[data-menu-key="admin"]');
    await admin.trigger('click');
    expect(wrapper.find('.submenu-flyout-admin').exists()).toBe(true);

    // Hover over inbound (should close admin submenu via onMenuMouseEnter)
    const inbound = wrapper.find('[data-menu-key="inbound"]');
    await inbound.trigger('mouseenter');

    // Admin submenu should be closed
    expect(wrapper.find('.submenu-flyout-admin').exists()).toBe(false);
  });
});
