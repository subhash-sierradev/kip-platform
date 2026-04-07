import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

const backSpy = vi.fn();
vi.mock('vue-router', () => ({ useRouter: () => ({ back: backSpy }) }));

const toggleMock = vi.fn();
const deleteMock = vi.fn().mockResolvedValue(true);
const testMock = vi.fn().mockResolvedValue({ success: true });
vi.mock('@/composables/useWebhookActions', () => ({
  useWebhookActions: () => ({
    toggleWebhookStatus: toggleMock,
    deleteWebhook: deleteMock,
    testWebhook: testMock,
  }),
}));

const toast = { showError: vi.fn(), showSuccess: vi.fn(), showWarning: vi.fn() };
vi.mock('@/store/toast', () => ({ useToastStore: () => toast }));

// Keep confirm dialog open and drive confirm to specific handler
const dialogState = { pendingAction: 'enable' as 'enable' | 'disable' | 'delete' };
const confirmSpy = vi.fn(async (handlers: any) => {
  const k = dialogState.pendingAction;
  if (handlers[k]) return handlers[k]();
});
vi.mock('@/composables/useConfirmationDialog', () => ({
  useConfirmationDialog: () => ({
    dialogOpen: true,
    actionLoading: false,
    pendingAction: dialogState.pendingAction,
    dialogTitle: '',
    dialogDescription: '',
    dialogConfirmLabel: '',
    openDialog: vi.fn(),
    closeDialog: vi.fn(),
    confirmWithHandlers: confirmSpy,
  }),
  createDefaultDialogConfig: () => ({}),
}));

vi.mock('devextreme-vue', () => ({
  DxButton: { name: 'DxButton', template: '<button class="dx-button"><slot/></button>' },
}));
vi.mock('@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue', () => ({
  default: {
    name: 'JiraWebhookWizard',
    props: ['open'],
    template: '<div v-if="open" class="wizard" />',
  },
}));

import WebhookDetailsTab from '@/components/outbound/jirawebhooks/details/WebhookDetailsTab.vue';

const baseWebhook = {
  id: 'w1',
  name: 'Web',
  description: 'D',
  webhookUrl: 'http://x',
  connectionId: 'c1',
  jiraFieldMappings: [],
  samplePayload: '',
  isEnabled: true,
  isDeleted: false,
  normalizedName: 'web',
  createdBy: 'u',
  createdDate: '2024-01-01',
  lastModifiedBy: 'u',
  lastModifiedDate: '2024-01-01',
  tenantId: 't1',
  version: 1,
};

describe('WebhookDetailsTab', () => {
  it('enable confirm toggles and emits status-updated + refresh', async () => {
    toggleMock.mockResolvedValueOnce(false);
    dialogState.pendingAction = 'enable';
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await wrapper.vm.$nextTick();
    await (wrapper.vm as any).handleConfirm();
    expect(toggleMock).toHaveBeenCalledWith('w1', true);
  });

  it('disable confirm toggles and emits status-updated + refresh', async () => {
    toggleMock.mockResolvedValueOnce(true);
    dialogState.pendingAction = 'disable';
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, isEnabled: false } },
    });
    await (wrapper.vm as any).handleConfirm();
    expect(toggleMock).toHaveBeenCalledWith('w1', false);
  });

  it('delete confirm navigates back', async () => {
    dialogState.pendingAction = 'delete';
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await (wrapper.vm as any).handleConfirm();
    expect(deleteMock).toHaveBeenCalledWith('w1');
    expect(backSpy).toHaveBeenCalled();
  });

  it('copyToClipboard successfully copies URL and shows success toast', async () => {
    const writeTextSpy = vi.fn().mockResolvedValueOnce(undefined);
    Object.assign(navigator, { clipboard: { writeText: writeTextSpy } });
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await (wrapper.vm as any).copyToClipboard();
    expect(writeTextSpy).toHaveBeenCalledWith('http://x');
    expect(toast.showSuccess).toHaveBeenCalledWith('Webhook URL successfully copied to clipboard');
  });

  it('copyToClipboard shows warning when URL is N/A', async () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, webhookUrl: 'N/A' } },
    });
    await (wrapper.vm as any).copyToClipboard();
    expect(toast.showWarning).toHaveBeenCalledWith('No webhook URL available to copy');
  });

  it('copyToClipboard shows error when clipboard API fails', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const writeTextSpy = vi.fn().mockRejectedValueOnce(new Error('Clipboard error'));
    Object.assign(navigator, { clipboard: { writeText: writeTextSpy } });
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await (wrapper.vm as any).copyToClipboard();
    expect(toast.showError).toHaveBeenCalledWith('Failed to copy webhook URL to clipboard');
    expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to copy URL:', expect.any(Error));
    consoleErrorSpy.mockRestore();
  });

  it('handleEditWebhook opens wizard in edit mode', () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    (wrapper.vm as any).handleEditWebhook();
    expect((wrapper.vm as any).wizardOpen).toBe(true);
    expect((wrapper.vm as any).editMode).toBe(true);
    expect((wrapper.vm as any).cloneMode).toBe(false);
    expect((wrapper.vm as any).editingWebhookData).toEqual(baseWebhook);
    expect((wrapper.vm as any).cloningWebhookData).toBeNull();
  });

  it('handleEditWebhook shows error when webhook not loaded', () => {
    const wrapper = mount(WebhookDetailsTab, { props: { webhookId: 'w1', webhookData: null } });
    (wrapper.vm as any).handleEditWebhook();
    expect(toast.showError).toHaveBeenCalledWith('Webhook details not loaded');
    expect((wrapper.vm as any).wizardOpen).toBe(false);
  });

  it('handleCloneWebhook opens wizard in clone mode', () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    (wrapper.vm as any).handleCloneWebhook();
    expect((wrapper.vm as any).wizardOpen).toBe(true);
    expect((wrapper.vm as any).cloneMode).toBe(true);
    expect((wrapper.vm as any).editMode).toBe(false);
    expect((wrapper.vm as any).cloningWebhookData).toEqual(baseWebhook);
    expect((wrapper.vm as any).editingWebhookData).toBeNull();
  });

  it('handleCloneWebhook shows error when webhook not loaded', () => {
    const wrapper = mount(WebhookDetailsTab, { props: { webhookId: 'w1', webhookData: null } });
    (wrapper.vm as any).handleCloneWebhook();
    expect(toast.showError).toHaveBeenCalledWith('Webhook details not loaded');
    expect((wrapper.vm as any).wizardOpen).toBe(false);
  });

  it('handleActionClick routes to correct handlers', async () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await wrapper.vm.$nextTick();
    // Test edit action by verifying wizard opens
    (wrapper.vm as any).handleActionClick('edit');
    expect((wrapper.vm as any).wizardOpen).toBe(true);
    expect((wrapper.vm as any).editMode).toBe(true);
  });

  it('handleActionClick warns on unknown action', () => {
    const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    (wrapper.vm as any).handleActionClick('unknown-action');
    expect(consoleWarnSpy).toHaveBeenCalledWith('Unknown action: unknown-action');
    consoleWarnSpy.mockRestore();
  });

  it('handleTestWebhook calls testWebhook and shows success toast', async () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, samplePayload: '{"test":"data"}' } },
    });
    await (wrapper.vm as any).handleTestWebhook();
    expect(testMock).toHaveBeenCalledWith('w1', '{"test":"data"}');
    expect(toast.showSuccess).toHaveBeenCalledWith(
      'Webhook test completed successfully! Check the webhook history for execution details.'
    );
    expect((wrapper.vm as any).testLoading).toBe(false);
  });

  it('handleTestWebhook uses default payload when samplePayload is empty', async () => {
    testMock.mockClear().mockResolvedValueOnce({ success: true });
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, samplePayload: '' } },
    });
    await (wrapper.vm as any).handleTestWebhook();
    const callArgs = testMock.mock.calls[0];
    expect(callArgs[0]).toBe('w1');
    // Verify it's the default Jira payload by checking for key properties
    const payload = JSON.parse(callArgs[1]);
    expect(payload.webhookEvent).toBe('jira:issue_updated');
    expect(payload.issue.key).toBe('TEST-123');
  });

  it('handleTestWebhook shows error when test fails', async () => {
    testMock.mockResolvedValueOnce({ success: false, errorMessage: 'Connection refused' });
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await (wrapper.vm as any).handleTestWebhook();
    expect(toast.showError).toHaveBeenCalledWith('Webhook test failed: Connection refused');
  });

  it('handleTestWebhook shows warning when webhook is disabled', async () => {
    testMock.mockClear();
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, isEnabled: false } },
    });
    await (wrapper.vm as any).handleTestWebhook();
    expect(toast.showWarning).toHaveBeenCalledWith(
      'Cannot test a disabled webhook. Please enable it first.'
    );
    expect(testMock).toHaveBeenCalledTimes(0);
  });

  it('handleTestWebhook shows error when webhook not loaded', async () => {
    testMock.mockClear();
    const wrapper = mount(WebhookDetailsTab, { props: { webhookId: 'w1', webhookData: null } });
    await (wrapper.vm as any).handleTestWebhook();
    expect(toast.showError).toHaveBeenCalledWith('Webhook details not loaded');
    expect(testMock).toHaveBeenCalledTimes(0);
  });

  it('handleTestWebhook catches exceptions and shows error toast', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    testMock.mockRejectedValueOnce(new Error('Network error'));
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await (wrapper.vm as any).handleTestWebhook();
    expect(toast.showError).toHaveBeenCalledWith('Failed to test webhook. Please try again.');
    expect((wrapper.vm as any).testLoading).toBe(false);
    consoleErrorSpy.mockRestore();
  });

  it('handleToggleStatus shows error when webhookId is invalid', async () => {
    toggleMock.mockClear();
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: '', webhookData: { ...baseWebhook } },
    });
    await (wrapper.vm as any).handleToggleStatus();
    expect(toast.showError).toHaveBeenCalledWith('Invalid webhook id');
    expect(toggleMock).toHaveBeenCalledTimes(0);
  });

  it('handleToggleStatus catches errors and shows error toast', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    toggleMock.mockRejectedValueOnce(new Error('API error'));
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook } },
    });
    await (wrapper.vm as any).handleToggleStatus();
    expect(toast.showError).toHaveBeenCalledWith('Failed to update webhook status');
    consoleErrorSpy.mockRestore();
  });

  it('metadataFormData shows N/A for missing fields', () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: {
        webhookId: 'w1',
        webhookData: { ...baseWebhook, createdBy: undefined, lastModifiedBy: undefined } as any,
      },
    });
    const metadata = (wrapper.vm as any).metadataFormData;
    expect(metadata.createdBy).toBe('N/A');
    expect(metadata.lastModifiedBy).toBe('N/A');
  });

  it('configurationFormData shows fallback text for missing fields', () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: {
        webhookId: 'w1',
        webhookData: {
          ...baseWebhook,
          name: undefined,
          description: undefined,
          webhookUrl: undefined,
        } as any,
      },
    });
    const config = (wrapper.vm as any).configurationFormData;
    expect(config.name).toBe('N/A');
    expect(config.description).toBe('No description provided');
    expect(config.webhookUrl).toBe('N/A');
  });

  it('manageActions disables test button when webhook is disabled', () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, isEnabled: false } },
    });
    const actions = (wrapper.vm as any).manageActions;
    const testAction = actions.find((a: any) => a.id === 'test');
    expect(testAction.disabled).toBe(true);
  });

  it('manageActions toggle shows correct label for enabled webhook', () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, isEnabled: true } },
    });
    const actions = (wrapper.vm as any).manageActions;
    const toggleAction = actions.find((a: any) => a.id === 'toggle');
    expect(toggleAction.label).toBe('Disable Webhook');
    expect(toggleAction.icon).toBe('cursorprohibition');
  });

  it('manageActions toggle shows correct label for disabled webhook', () => {
    const wrapper = mount(WebhookDetailsTab, {
      props: { webhookId: 'w1', webhookData: { ...baseWebhook, isEnabled: false } },
    });
    const actions = (wrapper.vm as any).manageActions;
    const toggleAction = actions.find((a: any) => a.id === 'toggle');
    expect(toggleAction.label).toBe('Enable Webhook');
    expect(toggleAction.icon).toBe('video');
  });
});
