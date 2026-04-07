import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import WebhookDetailsTab from '@/components/outbound/jirawebhooks/details/WebhookDetailsTab.vue';

// Mock DevExtreme components
vi.mock('devextreme-vue', () => ({
  DxButton: {
    name: 'DxButton',
    props: ['text', 'icon', 'type', 'stylingMode', 'class'],
    template:
      '<button class="dx-button" :class="$attrs.class" :data-testid="$attrs[\'data-testid\']">{{ text }}</button>',
  },
}));

// Mock composables that might use DOM APIs
vi.mock('@/composables/useConfirmationDialog', () => {
  return {
    useConfirmationDialog: () => ({
      dialogOpen: ref(false),
      actionLoading: ref(false),
      pendingAction: ref(null),
      dialogTitle: ref(''),
      dialogDescription: ref(''),
      dialogConfirmLabel: ref('Confirm'),
      openDialog: vi.fn(),
      closeDialog: vi.fn(),
      confirmWithHandlers: vi.fn(),
    }),
    createDefaultDialogConfig: () => ({}),
  };
});

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    // Remove testWebhook mock since functionality is removed
  },
}));

// Mock toast store
vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showInfo: vi.fn(),
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarning: vi.fn(),
  }),
}));

// Router mock
vi.mock('vue-router', () => ({
  useRouter: () => ({ back: vi.fn() }),
}));

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn().mockResolvedValue(undefined),
  },
});

// Mock date utils
vi.mock('@/utils/dateUtils', () => ({
  formatMetadataDate: (date: string) => date || 'N/A',
}));

describe('WebhookDetailsTab', () => {
  const webhookData = {
    id: 'abc123',
    name: 'Test Webhook',
    description: '',
    webhookUrl: 'https://example.com/backend/api/webhooks/jira/execute/abc123',
    connectionId: 'conn',
    jiraFieldMappings: [],
    samplePayload: '{"a":1}',
    isEnabled: true,
    isDeleted: false,
    normalizedName: 'test webhook',
    createdBy: 'org2-admin',
    createdDate: new Date().toISOString(),
    lastModifiedBy: 'org2-admin',
    lastModifiedDate: new Date().toISOString(),
    tenantId: 'tenant',
    version: 1,
  };

  let wrapper: any;

  beforeEach(() => {
    setActivePinia(createPinia());
    wrapper = mount(WebhookDetailsTab, {
      props: {
        webhookId: webhookData.id,
        webhookData,
      },
    });
  });

  it('renders webhook details correctly', () => {
    expect(wrapper.text()).toContain('Test Webhook');
    expect(wrapper.text()).toContain(webhookData.description || 'No description provided');
  });

  it('displays webhook URL correctly', () => {
    expect(wrapper.text()).toContain(
      'https://example.com/backend/api/webhooks/jira/execute/abc123'
    );
  });

  it('shows webhook is enabled through button text', () => {
    // Since webhook is enabled, the button should show "Disable Webhook"
    expect(wrapper.text()).toContain('Disable Webhook');
  });

  it('displays creation information', () => {
    expect(wrapper.text()).toContain('org2-admin');
    expect(wrapper.text()).toContain('Created');
  });

  it('renders Edit button', () => {
    const editButton = wrapper.find('[data-testid="edit-button"]');
    expect(editButton.exists()).toBe(true);
  });

  it('renders Enable/Disable toggle button', () => {
    const toggleButton = wrapper.find('[data-testid="toggle-button"]');
    expect(toggleButton.exists()).toBe(true);
    expect(toggleButton.text()).toContain('Webhook'); // Contains either 'Enable Webhook' or 'Disable Webhook'
  });

  it('renders Clone button', () => {
    const cloneButton = wrapper.find('[data-testid="clone-button"]');
    expect(cloneButton.exists()).toBe(true);
  });

  it('renders Delete button', () => {
    const deleteButton = wrapper.find('[data-testid="delete-button"]');
    expect(deleteButton.exists()).toBe(true);
  });
});
