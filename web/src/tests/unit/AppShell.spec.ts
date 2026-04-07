import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { defineComponent, inject } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';

import AppShell from '@/components/layout/AppShell.vue';

// Minimal routes used for breadcrumb testing
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: { template: '<div />' } },
    {
      path: '/inbound/integrations',
      name: 'inbound-integrations',
      component: { template: '<div />' },
    },
    { path: '/outbound/webhook/jira', name: 'outbound-jira', component: { template: '<div />' } },
    {
      path: '/outbound/webhook/jira/:id',
      name: 'outbound-jira-detail',
      component: { template: '<div />' },
    },
    {
      path: '/outbound/integration/arcgis',
      name: 'outbound-arcgis',
      component: { template: '<div />' },
    },
  ],
});

// Lightweight stubs for child components
const TopBarStub = {
  template: '<div class="topbar-stub" @click="$emit(\'toggle\')"></div>',
};

const SideNavStub = {
  template: '<nav class="sidenav-stub"></nav>',
};

describe('AppShell.vue', () => {
  it('renders and computes breadcrumbs for Jira detail route', async () => {
    router.push('/outbound/webhook/jira/PROJECT-123');
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
    expect(crumbs).toEqual(['Home', 'Outbound', 'Jira Webhook', 'Jira Details']);
  });

  it('toggles collapsed state when TopBar emits toggle', async () => {
    router.push('/');
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

    expect(wrapper.classes()).not.toContain('sidebar-collapsed');
    await wrapper.find('.topbar-stub').trigger('click');
    // After toggle, collapsed should be true and class applied
    expect(wrapper.classes()).toContain('sidebar-collapsed');
  });

  it('applies dynamic breadcrumb titles via injection', async () => {
    router.push('/outbound/webhook/jira/ISSUE-1');
    await router.isReady();

    // Mount and inject dynamic title using a slot component
    const wrapper = mount(AppShell, {
      slots: {
        default: defineComponent({
          template: '<div></div>',
          setup() {
            const setBreadcrumbTitle = inject('setBreadcrumbTitle') as (t: string | null) => void;
            setBreadcrumbTitle('Custom Jira Title');
            return {};
          },
        }),
      },
      global: {
        plugins: [router],
        stubs: {
          TopBar: TopBarStub,
          SideNav: SideNavStub,
        },
      },
    });

    // Wait for injection to propagate
    await Promise.resolve();
    await Promise.resolve();
    const crumbs = wrapper.findAll('.breadcrumb-item .breadcrumb-text').map(n => n.text());
    expect(crumbs).toEqual(['Home', 'Outbound', 'Jira Webhook', 'Custom Jira Title']);
  });

  it('shows mobile overlay when viewport is mobile and hides after click', async () => {
    router.push('/');
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

    // Simulate mobile viewport
    (window as any).innerWidth = 600;
    window.dispatchEvent(new Event('resize'));
    await Promise.resolve();

    // Overlay appears when not collapsed
    expect(wrapper.find('.mobile-overlay').exists()).toBe(true);

    // Clicking overlay toggles nav (collapses)
    await wrapper.find('.mobile-overlay').trigger('click');
    expect(wrapper.classes()).toContain('sidebar-collapsed');
  });
});
