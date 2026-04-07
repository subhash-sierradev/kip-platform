import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import AppShell from '@/components/layout/AppShell.vue';

vi.mock('@/composables/useNotifications', () => ({
  useNotifications: vi.fn(),
}));

const TopBarStub = {
  name: 'TopBar',
  props: ['collapsed'],
  emits: ['toggle'],
  template: '<button class="topbar-toggle" @click="$emit(\'toggle\')">toggle</button>',
};

const SideNavStub = {
  name: 'SideNav',
  props: ['sections', 'collapsed'],
  template: '<nav class="sidenav-stub" />',
};

function buildRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/notifications', name: 'notifications', component: { template: '<div />' } },
      {
        path: '/admin/notifications',
        name: 'admin-notifications',
        component: { template: '<div />' },
      },
      {
        path: '/admin/audit-log',
        name: 'admin-audit-log',
        component: { template: '<div />' },
      },
      {
        path: '/admin/clear-cache',
        name: 'admin-clear-cache',
        component: { template: '<div />' },
      },
      {
        path: '/admin/connections/jira',
        name: 'admin-jira-connect',
        component: { template: '<div />' },
      },
      {
        path: '/admin/connections/confluence',
        name: 'admin-confluence-connect',
        component: { template: '<div />' },
      },
      {
        path: '/admin/connections/arcgis',
        name: 'admin-arcgis-connect',
        component: { template: '<div />' },
      },
      {
        path: '/outbound/integration/arcgis',
        name: 'arcgis-root',
        component: { template: '<div />' },
      },
      {
        path: '/outbound/integration/arcgis/:id',
        name: 'arcgis-detail',
        component: { template: '<div />' },
      },
      {
        path: '/outbound/integration/confluence',
        name: 'confluence-root',
        component: { template: '<div />' },
      },
      {
        path: '/outbound/integration/confluence/:id',
        name: 'confluence-detail',
        component: { template: '<div />' },
      },
      {
        path: '/paddingless',
        name: 'paddingless',
        component: { template: '<div />' },
        meta: { disableShellPadding: true },
      },
      {
        path: '/outbound/webhook/jira/ABC-1',
        name: 'jira-detail',
        component: { template: '<div />' },
      },
      {
        path: '/outbound/webhook/jira',
        name: 'jira-root',
        component: { template: '<div />' },
      },
    ],
  });
}

describe('AppShell branches', () => {
  it('builds breadcrumb from explicit notification rule', async () => {
    const router = buildRouter();
    await router.push('/admin/notifications');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    const crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Admin', 'Notifications']);
  });

  it('builds breadcrumb from top-level notifications rule', async () => {
    const router = buildRouter();
    await router.push('/notifications');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    const crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Notifications']);
  });

  it('builds breadcrumb from admin connection rules', async () => {
    const router = buildRouter();
    await router.push('/admin/connections/jira');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    let crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Admin', 'Jira Connect']);

    await router.push('/admin/connections/confluence');
    await Promise.resolve();
    crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Admin', 'Confluence Connect']);

    await router.push('/admin/connections/arcgis');
    await Promise.resolve();
    crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Admin', 'ArcGIS Connect']);
  });

  it('builds breadcrumb via navigation fallback for non-rule route', async () => {
    const router = buildRouter();
    await router.push('/admin/audit-log');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    const crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toContain('Home');
    expect(crumbs).toContain('Admin');
    expect(crumbs).toContain('Audit Log');
  });

  it('builds ArcGIS and Confluence breadcrumbs for root and detail routes', async () => {
    const router = buildRouter();
    await router.push('/outbound/integration/arcgis');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    let crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Outbound', 'ArcGIS Integration']);

    await router.push('/outbound/integration/arcgis/sync-1');
    await Promise.resolve();
    crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Outbound', 'ArcGIS Integration', 'ArcGIS Details']);

    await router.push('/outbound/integration/confluence');
    await Promise.resolve();
    crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Outbound', 'Confluence Integration']);

    await router.push('/outbound/integration/confluence/page-1');
    await Promise.resolve();
    crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Outbound', 'Confluence Integration', 'Confluence Details']);
  });

  it('keeps Jira breadcrumb at base route without detail item', async () => {
    const router = buildRouter();
    await router.push('/outbound/webhook/jira');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    const crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Outbound', 'Jira Webhook']);
  });

  it('builds fallback breadcrumb for admin clear-cache route', async () => {
    const router = buildRouter();
    await router.push('/admin/clear-cache');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    const crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Admin', 'Clear Cache']);
  });

  it('applies no-page-padding class when route meta disables shell padding', async () => {
    const router = buildRouter();
    await router.push('/paddingless');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    expect(wrapper.find('.page-content').classes()).toContain('no-page-padding');
  });

  it('shows and hides mobile overlay on resize and nav toggle', async () => {
    const router = buildRouter();
    await router.push('/');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    (window as any).innerWidth = 600;
    window.dispatchEvent(new Event('resize'));
    await Promise.resolve();

    expect(wrapper.find('.mobile-overlay').exists()).toBe(true);

    await wrapper.find('.mobile-overlay').trigger('click');
    expect(wrapper.find('.mobile-overlay').exists()).toBe(false);
  });
});
