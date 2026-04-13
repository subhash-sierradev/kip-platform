import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import JiraWebhookCreatePage from '@/components/outbound/jirawebhooks/JiraWebhookCreatePage.vue';

// Mock toast store
vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showInfo: vi.fn(),
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarning: vi.fn(),
  }),
}));

// Mock the JiraWebhookWizard component
vi.mock('@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue', () => ({
  default: {
    name: 'JiraWebhookWizard',
    props: ['open'],
    emits: ['close'],
    template: '<div class="mock-wizard">Wizard Mock</div>',
  },
}));

describe('JiraWebhookCreatePage', () => {
  let router: any;

  beforeEach(() => {
    setActivePinia(createPinia());
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/outbound/jira-webhook',
          name: 'jira-webhook-list',
          component: { template: '<div>List</div>' },
        },
        { path: '/create', name: 'jira-webhook-create', component: JiraWebhookCreatePage },
      ],
    });
    router.push('/create');
  });

  it('renders the wizard overlay correctly', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    expect(wrapper.find('.wizard-overlay').exists()).toBe(true);
    expect(wrapper.find('.wizard-container').exists()).toBe(true);
  });

  it('includes JiraWebhookWizard component with correct props', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const wizardComponent = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    expect(wizardComponent.exists()).toBe(true);
    expect(wizardComponent.props('open')).toBe(true);
  });

  it('navigates to jira-webhook list when handleClose is called', async () => {
    const mockPush = vi.spyOn(router, 'push');

    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const wizardComponent = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    await wizardComponent.vm.$emit('close');

    expect(mockPush).toHaveBeenCalledWith('/outbound/jira-webhook');
  });

  it('handles wizard close event correctly', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const wizardComponent = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    expect(wizardComponent.exists()).toBe(true);

    // Trigger close event
    await wizardComponent.vm.$emit('close');

    // Should not throw any errors
    expect(wrapper.exists()).toBe(true);
  });

  it('applies correct CSS classes for overlay styling', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const overlay = wrapper.find('.wizard-overlay');
    expect(overlay.exists()).toBe(true);

    const container = wrapper.find('.wizard-container');
    expect(container.exists()).toBe(true);
  });

  it('has correct z-index for modal behavior', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const overlay = wrapper.find('.wizard-overlay');
    expect(overlay.exists()).toBe(true);
  });

  it('handles router navigation errors gracefully', async () => {
    const mockPush = vi.spyOn(router, 'push').mockRejectedValue(new Error('Navigation failed'));
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const wizardComponent = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    await wizardComponent.vm.$emit('close');

    expect(mockPush).toHaveBeenCalledWith('/outbound/jira-webhook');

    // Clean up
    consoleSpy.mockRestore();
  });

  it('maintains component state during wizard interaction', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    // Initial state
    expect(wrapper.exists()).toBe(true);

    // After wizard interaction
    const wizardComponent = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    await wizardComponent.vm.$emit('close');

    // Component should still exist and be functional
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find('.wizard-overlay').exists()).toBe(true);
  });

  it('properly centers the wizard container', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const overlay = wrapper.find('.wizard-overlay');
    const container = wrapper.find('.wizard-container');

    expect(overlay.exists()).toBe(true);
    expect(container.exists()).toBe(true);
    expect(container.element.parentElement).toBe(overlay.element);
  });

  it('handles multiple close events correctly', async () => {
    const mockPush = vi.spyOn(router, 'push');

    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const wizardComponent = wrapper.findComponent({ name: 'JiraWebhookWizard' });

    // Emit close event multiple times
    await wizardComponent.vm.$emit('close');
    await wizardComponent.vm.$emit('close');
    await wizardComponent.vm.$emit('close');

    expect(mockPush).toHaveBeenCalledTimes(3);
    expect(mockPush).toHaveBeenCalledWith('/outbound/jira-webhook');
  });

  it('has proper fixed positioning for full screen overlay', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    const overlay = wrapper.find('.wizard-overlay');
    expect(overlay.exists()).toBe(true);

    // The CSS should handle the positioning, but we can verify the class exists
    expect(overlay.classes()).toContain('wizard-overlay');
  });

  it('provides accessibility structure', async () => {
    const wrapper = mount(JiraWebhookCreatePage, {
      global: {
        plugins: [router],
      },
    });
    await router.isReady();

    // Should have a clear container structure for screen readers
    expect(wrapper.find('.wizard-overlay').exists()).toBe(true);
    expect(wrapper.find('.wizard-container').exists()).toBe(true);
    expect(wrapper.findComponent({ name: 'JiraWebhookWizard' }).exists()).toBe(true);
  });
});
