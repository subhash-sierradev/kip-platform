import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import WebhookDetailsTab from '@/components/outbound/jirawebhooks/details/WebhookDetailsTab.vue';

const hoisted = vi.hoisted(() => ({
  openDialog: vi.fn(),
  closeDialog: vi.fn(),
  confirmWithHandlers: vi.fn(),
  toggleWebhookStatus: vi.fn(),
  deleteWebhook: vi.fn(),
  testWebhook: vi.fn(),
  showInfo: vi.fn(),
  showSuccess: vi.fn(),
  showError: vi.fn(),
  showWarning: vi.fn(),
  routerBack: vi.fn(),
}));

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
      openDialog: hoisted.openDialog,
      closeDialog: hoisted.closeDialog,
      confirmWithHandlers: hoisted.confirmWithHandlers,
    }),
    createDefaultDialogConfig: () => ({}),
  };
});

vi.mock('@/composables/useWebhookActions', () => ({
  useWebhookActions: () => ({
    toggleWebhookStatus: hoisted.toggleWebhookStatus,
    deleteWebhook: hoisted.deleteWebhook,
    testWebhook: hoisted.testWebhook,
  }),
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    getAllJiraNormalizedNames: vi.fn().mockResolvedValue([]),
    listJiraWebhooks: vi.fn().mockResolvedValue([]),
  },
}));

// Mock toast store
vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showInfo: hoisted.showInfo,
    showSuccess: hoisted.showSuccess,
    showError: hoisted.showError,
    showWarning: hoisted.showWarning,
  }),
}));

// Router mock
vi.mock('vue-router', () => ({
  useRouter: () => ({ back: hoisted.routerBack }),
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

  function mountWrapper(
    props: { webhookId?: string; webhookData?: typeof webhookData | null } = {}
  ) {
    return mount(WebhookDetailsTab, {
      props: {
        webhookId: webhookData.id,
        webhookData,
        ...props,
      },
    });
  }

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    hoisted.toggleWebhookStatus.mockResolvedValue(true);
    hoisted.deleteWebhook.mockResolvedValue(true);
    hoisted.testWebhook.mockResolvedValue({ success: true });
    wrapper = mountWrapper();
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

  it('warns when trying to copy a missing webhook URL', async () => {
    const missingUrlWrapper = mountWrapper({
      webhookData: { ...webhookData, webhookUrl: '' },
    });

    await (missingUrlWrapper.vm as any).copyToClipboard();

    expect(hoisted.showWarning).toHaveBeenCalledWith('No webhook URL available to copy');
  });

  it('shows an error toast when clipboard copy fails', async () => {
    vi.mocked(navigator.clipboard.writeText).mockRejectedValueOnce(new Error('denied'));

    await (wrapper.vm as any).copyToClipboard();

    expect(hoisted.showError).toHaveBeenCalledWith('Failed to copy webhook URL to clipboard');
  });

  it('opens the correct dialogs for toggle and delete actions and warns on unknown actions', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    (wrapper.vm as any).handleActionClick('toggle');
    (wrapper.vm as any).handleActionClick('delete');
    (wrapper.vm as any).handleActionClick('unknown-action');

    expect(hoisted.openDialog).toHaveBeenNthCalledWith(1, 'disable');
    expect(hoisted.openDialog).toHaveBeenNthCalledWith(2, 'delete');
    expect(warnSpy).toHaveBeenCalledWith('Unknown action: unknown-action');
  });

  it('shows an error when toggling status without a valid webhook id', async () => {
    const missingIdWrapper = mountWrapper({ webhookId: '' });

    await (missingIdWrapper.vm as any).handleToggleStatus();

    expect(hoisted.showError).toHaveBeenCalledWith('Invalid webhook id');
    expect(hoisted.toggleWebhookStatus).not.toHaveBeenCalled();
  });

  it('updates local status and emits refresh events after a successful toggle', async () => {
    hoisted.toggleWebhookStatus.mockResolvedValueOnce(false);

    await (wrapper.vm as any).handleToggleStatus();

    expect(hoisted.toggleWebhookStatus).toHaveBeenCalledWith(webhookData.id, true);
    expect((wrapper.vm as any).localWebhook.isEnabled).toBe(false);
    expect(wrapper.emitted('status-updated')).toEqual([[false]]);
    expect(wrapper.emitted('refresh')).toEqual([[]]);
  });

  it('returns early when toggleWebhookStatus resolves to null', async () => {
    hoisted.toggleWebhookStatus.mockResolvedValueOnce(null);

    await (wrapper.vm as any).handleToggleStatus();

    expect(wrapper.emitted('status-updated')).toBeFalsy();
    expect(wrapper.emitted('refresh')).toBeFalsy();
  });

  it('surfaces toggle status failures through the toast store', async () => {
    hoisted.toggleWebhookStatus.mockRejectedValueOnce(new Error('toggle failed'));

    await (wrapper.vm as any).handleToggleStatus();

    expect(hoisted.showError).toHaveBeenCalledWith('Failed to update webhook status');
  });

  it('routes back only when deleteWebhook succeeds', async () => {
    await (wrapper.vm as any).handleDeleteWebhook();
    expect(hoisted.routerBack).toHaveBeenCalledTimes(1);

    hoisted.routerBack.mockClear();
    hoisted.deleteWebhook.mockResolvedValueOnce(false);

    await (wrapper.vm as any).handleDeleteWebhook();
    expect(hoisted.routerBack).not.toHaveBeenCalled();
  });

  it('opens edit and clone wizards from loaded webhook data', () => {
    (wrapper.vm as any).handleEditWebhook();
    expect((wrapper.vm as any).wizardOpen).toBe(true);
    expect((wrapper.vm as any).editMode).toBe(true);
    expect((wrapper.vm as any).cloneMode).toBe(false);

    (wrapper.vm as any).wizardOpen = false;
    (wrapper.vm as any).handleCloneWebhook();
    expect((wrapper.vm as any).wizardOpen).toBe(true);
    expect((wrapper.vm as any).editMode).toBe(false);
    expect((wrapper.vm as any).cloneMode).toBe(true);
  });

  it('shows an error when edit or clone is requested before webhook details load', () => {
    const missingDataWrapper = mountWrapper({ webhookData: null });

    (missingDataWrapper.vm as any).handleEditWebhook();
    (missingDataWrapper.vm as any).handleCloneWebhook();

    expect(hoisted.showError).toHaveBeenCalledWith('Webhook details not loaded');
    expect(hoisted.showError).toHaveBeenCalledTimes(2);
  });

  it('blocks test execution for missing or disabled webhook data', async () => {
    const missingDataWrapper = mountWrapper({ webhookData: null });
    await (missingDataWrapper.vm as any).handleTestWebhook();
    expect(hoisted.showError).toHaveBeenCalledWith('Webhook details not loaded');

    const disabledWrapper = mountWrapper({
      webhookData: { ...webhookData, isEnabled: false },
    });
    await (disabledWrapper.vm as any).handleTestWebhook();
    expect(hoisted.showWarning).toHaveBeenCalledWith(
      'Cannot test a disabled webhook. Please enable it first.'
    );
  });

  it('uses a default payload when testing a webhook without sample payload data', async () => {
    const noSampleWrapper = mountWrapper({
      webhookData: { ...webhookData, samplePayload: '' },
    });

    await (noSampleWrapper.vm as any).handleTestWebhook();

    expect(hoisted.testWebhook).toHaveBeenCalledTimes(1);
    expect(hoisted.testWebhook.mock.calls[0][0]).toBe(webhookData.id);
    expect(hoisted.testWebhook.mock.calls[0][1]).toContain('jira:issue_updated');
    expect(hoisted.showSuccess).toHaveBeenCalledWith(
      'Webhook test completed successfully! Check the webhook history for execution details.'
    );
  });

  it('shows specific error messaging for failed and thrown webhook test executions', async () => {
    hoisted.testWebhook.mockResolvedValueOnce({
      success: false,
      errorMessage: 'Backend rejected request',
    });
    await (wrapper.vm as any).handleTestWebhook();
    expect(hoisted.showError).toHaveBeenCalledWith('Webhook test failed: Backend rejected request');

    hoisted.showError.mockClear();
    hoisted.testWebhook.mockRejectedValueOnce(new Error('boom'));
    await (wrapper.vm as any).handleTestWebhook();
    expect(hoisted.showError).toHaveBeenCalledWith('Failed to test webhook. Please try again.');
    expect((wrapper.vm as any).testLoading).toBe(false);
  });
});
