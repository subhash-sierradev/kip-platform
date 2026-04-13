/* eslint-disable simple-import-sort/imports */
import { mount as baseMount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { nextTick } from 'vue';

const hoisted = vi.hoisted(() => ({
  pushSpy: vi.fn(),
  replaceSpy: vi.fn(),
  routeQuery: {} as Record<string, any>,
}));
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: hoisted.pushSpy, back: vi.fn(), replace: hoisted.replaceSpy }),
  useRoute: () => ({ query: hoisted.routeQuery }),
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    listJiraWebhooks: vi.fn(),
    getWebhookById: vi.fn(),
    getAllJiraNormalizedNames: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({ showError: vi.fn(), showSuccess: vi.fn() }),
}));
vi.mock('@/components/common/WebhookIcon.vue', () => ({
  default: { name: 'WebhookIcon', template: '<span class="icon-stub" />' },
}));
// Provide a stub that can emit confirm to drive handleConfirm
vi.mock('@/components/common/ConfirmationDialog.vue', () => ({
  default: {
    name: 'ConfirmationDialog',
    emits: ['confirm', 'cancel'],
    props: ['open'],
    template:
      '<div class="confirm-stub" v-if="open"><button class="confirm" @click="$emit(\'confirm\')">Confirm</button><button class="cancel" @click="$emit(\'cancel\')">Cancel</button></div>',
  },
}));
vi.mock('@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue', () => ({
  default: {
    name: 'JiraWebhookWizard',
    props: ['open', 'editMode', 'editingWebhookData', 'cloneMode', 'cloningWebhookData'],
    template:
      '<div class="wizard-stub" v-if="open" :data-edit-mode="String(!!editMode)" :data-clone-mode="String(!!cloneMode)">Wizard</div>',
  },
}));
// Dialog state hoisted to align confirm handler with last opened action
const dialogHoisted = vi.hoisted(() => ({
  lastAction: '' as 'delete' | 'enable' | 'disable' | '',
}));
const openDialogMock = vi.fn((type: 'delete' | 'enable' | 'disable') => {
  dialogHoisted.lastAction = type;
});
const closeDialogMock = vi.fn();
const confirmWithHandlersMock = vi.fn(async (handlers: any) => {
  const t = dialogHoisted.lastAction;
  if (t && handlers[t]) return handlers[t]();
  if (handlers.delete) return handlers.delete();
  if (handlers.enable) return handlers.enable();
  if (handlers.disable) return handlers.disable();
});
vi.mock('@/composables/useConfirmationDialog', () => ({
  useConfirmationDialog: () => ({
    dialogOpen: true,
    actionLoading: false,
    pendingAction: dialogHoisted.lastAction,
    dialogTitle: '',
    dialogDescription: '',
    dialogConfirmLabel: '',
    openDialog: openDialogMock,
    closeDialog: closeDialogMock,
    confirmWithHandlers: confirmWithHandlersMock,
  }),
  createDefaultDialogConfig: () => ({}),
}));
const toggleStatusMock = vi.fn();
const deleteWebhookMock = vi.fn().mockResolvedValue(true);
vi.mock('@/composables/useWebhookActions', () => ({
  useWebhookActions: () => ({
    toggleWebhookStatus: toggleStatusMock,
    deleteWebhook: deleteWebhookMock,
  }),
}));
// Mock helper functions used by dashboard (hoisted to avoid factory ordering issues)
const helperHoisted = vi.hoisted(() => ({
  copyHelperMock: vi.fn((text: string, cb: (msg: string) => void) => cb('Copied')),
  buildQueryMock: vi.fn((state: any) => ({
    q: 'state',
    scroll: state.includeScroll ? '100' : undefined,
  })),
  getSortFuncMock: vi.fn((_key: string) => (a: any, b: any) => a.name.localeCompare(b.name)),
  debounceMock: (fn: (...args: any[]) => void) => fn,
}));
vi.mock('@/components/outbound/jirawebhooks/utils/JiraWebhookDashboardHelper', () => ({
  copyToClipboard: helperHoisted.copyHelperMock,
  buildStateQuery: helperHoisted.buildQueryMock,
  getSortFunction: helperHoisted.getSortFuncMock,
  debounce: helperHoisted.debounceMock,
  getProjectLabel: (w: any) => (w.connectionId ? `Project ${w.connectionId}` : 'Project'),
  getIssueTypeLabel: () => 'Issue',
  getAssignee: () => 'Assignee',
  enableDisableLabel: (w: any) => (w.isEnabled ? 'Disable' : 'Enable'),
  enableDisableIcon: () => 'icon',
  enableDisableAria: (w: any) => (w.isEnabled ? 'Disable webhook' : 'Enable webhook'),
}));

import JiraWebhookDashboard from '@/components/outbound/jirawebhooks/JiraWebhookDashboard.vue';

const mount = (...args: Parameters<typeof baseMount>) => baseMount(...args) as any;
import { JiraWebhookService } from '@/api/services/JiraWebhookService';

describe('JiraWebhookDashboard', () => {
  // Ensure router query state doesn't leak between tests
  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.routeQuery = {};
    // Mock window.innerWidth for useResponsivePageSize composable
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1920,
    });
    Object.defineProperty(document.documentElement, 'clientWidth', {
      writable: true,
      configurable: true,
      value: 1920,
    });
  });

  it('renders cards after loading and opens wizard via toolbar', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard, {
      global: {
        stubs: {
          DashboardToolbar: {
            template:
              '<div class="toolbar-stub"><button class="create" @click="$emit(\'create\')">Create</button></div>',
          },
        },
      },
    });
    // Initially shows skeletons
    expect(wrapper.findAll('.skeleton').length).toBeGreaterThan(0);
    // Wait for fetchWebhooks
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.findAll('.webhook-card').length).toBe(1);

    // Emit create-webhook from toolbar
    await wrapper.find('.create').trigger('click');
    expect(wrapper.find('.wizard-stub').exists()).toBe(true);
  });

  it('navigates to details on card click', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard, {
      global: {
        stubs: { WebhookDashboardToolbar: { template: '<div />' } },
      },
    });
    await new Promise(r => setTimeout(r, 0));
    const cards = wrapper.findAll('.webhook-card');
    expect(cards.length).toBe(1);
    await cards[0].trigger('click');
    expect(hoisted.pushSpy).toHaveBeenCalled();
    const args = hoisted.pushSpy.mock.calls.at(-1)?.[0];
    expect(args?.query?.scroll).toBeDefined();
  });

  it('switches to list view and shows empty state when search has no matches', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
      {
        id: 'def',
        name: 'Second',
        createdBy: 'bob',
        createdDate: '2024-01-02',
        isEnabled: false,
        webhookUrl: 'http://def',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard, {
      global: {
        stubs: {
          DashboardToolbar: {
            template: `
              <div class="toolbar-stub">
                <button class="set-list" @click="$emit('setViewMode','list')">List</button>
                <input class="search" @input="$emit('update:search',$event.target.value)" />
              </div>
            `,
          },
        },
      },
    });
    await new Promise(r => setTimeout(r, 0));
    // Switch to list view
    await wrapper.find('.set-list').trigger('click');
    expect(wrapper.find('.title-row-list').exists()).toBe(true);
    // Apply unmatched search
    const inputEl = wrapper.find('input.search');
    await inputEl.setValue('zzz');
    await Promise.resolve();
    expect(wrapper.find('.simple-empty-state').exists()).toBe(true);
  });

  it('applies route query state and clamps/reshapes pagination on changes', async () => {
    // Initial route query with valid values and multiple pages
    hoisted.routeQuery = { search: 'web', sort: 'name', view: 'grid', page: '2', size: '6' };
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });
    Object.defineProperty(document.documentElement, 'clientWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });
    const data = Array.from({ length: 15 }).map((_, i) => ({
      id: `id${i}`,
      name: `Webhook ${i}`,
      createdBy: `u${i}`,
      createdDate: '2024-01-01',
      isEnabled: !!(i % 2),
      webhookUrl: `http://x${i}`,
      jiraFieldMappings: [],
    }));
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce(data);
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    // Current page starts at 2 from route
    expect(wrapper.find('.current-page').text()).toBe('2');
    // Change page size to 12 -> total pages reduces -> should clamp/reset to page 1
    await wrapper.find('#pageSizeSelect').setValue('12');
    await Promise.resolve();
    expect(wrapper.find('.current-page').text()).toBe('1');
    // Change search -> resets to page 1 (already 1), still clamped
    const searchInput = wrapper.find('input[type="text"]');
    await searchInput.setValue('Webhook 1');
    await Promise.resolve();
    expect(wrapper.find('.current-page').text()).toBe('1');
  });

  it('copies to clipboard and shows toast', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await Promise.resolve();
    await Promise.resolve();
    const card = wrapper.find('.webhook-card');
    expect(card.exists()).toBe(true);
    const copyBtn = card.find('.copy-icon-btn');
    await copyBtn.trigger('click');
    expect(helperHoisted.copyHelperMock).toHaveBeenCalled();
  });

  it('opens options menu and edits webhook (wizard opens)', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    (JiraWebhookService.getWebhookById as any).mockResolvedValueOnce({
      id: 'abc',
      name: 'Webhook ABC',
      isEnabled: true,
      jiraFieldMappings: [],
    });
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await Promise.resolve();
    await Promise.resolve();
    const card = wrapper.find('.webhook-card');
    expect(card.exists()).toBe(true);
    await card.find('.action-menu-trigger').trigger('click');
    await card.find('.action-menu-item').trigger('click'); // first is edit
    await Promise.resolve();
    expect(wrapper.find('.wizard-stub').exists()).toBe(true);
  });

  it('enable/disable via confirm uses toggle and updates state or refetches', async () => {
    // Reset service mock call history to isolate assertions
    (JiraWebhookService.listJiraWebhooks as any).mockReset();
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await Promise.resolve();
    await Promise.resolve();
    // Open menu and click enable/disable item (third item in dropdown)
    const card = wrapper.find('.webhook-card');
    expect(card.exists()).toBe(true);
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    // First branch: toggle returns boolean -> in-place update
    toggleStatusMock.mockResolvedValueOnce(false);
    await items[2].trigger('click');
    // Click confirm button on dialog stub
    await wrapper.find('.confirm').trigger('click');
    await nextTick();
    expect(toggleStatusMock).toHaveBeenCalled();
    // Now simulate non-boolean -> refetch
    const beforeRefetchCalls = (JiraWebhookService.listJiraWebhooks as any).mock.calls.length;
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    toggleStatusMock.mockResolvedValueOnce(undefined as any);
    await items[2].trigger('click');
    await wrapper.find('.confirm').trigger('click');
    await nextTick();
    expect((JiraWebhookService.listJiraWebhooks as any).mock.calls.length).toBe(
      beforeRefetchCalls + 1
    );
  });

  it('deletes webhook via confirm and removes from list', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
      {
        id: 'def',
        name: 'Second',
        createdBy: 'bob',
        createdDate: '2024-01-02',
        isEnabled: false,
        webhookUrl: 'http://def',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await Promise.resolve();
    await Promise.resolve();
    // Confirm initial render has both cards
    const initialCount = wrapper.findAll('.webhook-card').length;
    expect(initialCount).toBe(2);
    // Open dropdown then click delete (fourth item)
    const card = wrapper.find('.webhook-card');
    expect(card.exists()).toBe(true);
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    await items[3].trigger('click');
    await wrapper.find('.confirm').trigger('click');
    await nextTick();
    await Promise.resolve();
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.findAll('.webhook-card').length).toBe(initialCount - 1);
  });

  it('clones webhook and opens wizard in clone mode', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Original Webhook',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    (JiraWebhookService.getWebhookById as any).mockResolvedValueOnce({
      id: 'abc',
      name: 'Original Webhook',
      webhookUrl: 'http://abc',
      jiraFieldMappings: [
        {
          jiraFieldId: 'field1',
          displayLabel: 'Field 1',
          jiraFieldName: 'field1',
          template: '{{val}}',
          dataType: 'string',
          required: true,
          defaultValue: '',
          metadata: {},
        },
      ],
      isEnabled: true,
      description: 'desc',
      samplePayload: '{}',
      connectionId: 'conn1',
    });
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await Promise.resolve();
    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    // Clone is second item
    await items[1].trigger('click');
    await nextTick();
    expect(wrapper.find('.wizard-stub').exists()).toBe(true);
  });

  it('handles fetch error gracefully', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockRejectedValueOnce(
      new Error('Network failure')
    );
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await nextTick();
    // Component should handle error gracefully - no crash
    expect(wrapper.exists()).toBe(true);
    // Empty state shown when no data
    expect(wrapper.find('.simple-empty-state').exists()).toBe(true);
  });

  it('handles edit webhook error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    (JiraWebhookService.getWebhookById as any).mockRejectedValueOnce(new Error('Load failed'));
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    await card.find('.action-menu-item').trigger('click');
    await nextTick();
    await new Promise(r => setTimeout(r, 0));
    // Verify error was logged
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Failed to load webhook for editing:',
      expect.any(Error)
    );
    consoleErrorSpy.mockRestore();
  });

  it('handles clone webhook error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    (JiraWebhookService.getWebhookById as any).mockRejectedValueOnce(
      new Error('Clone load failed')
    );
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    await items[1].trigger('click'); // Clone item
    await nextTick();
    await new Promise(r => setTimeout(r, 0));
    // Verify error was logged
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Failed to load webhook for cloning:',
      expect.any(Error)
    );
    consoleErrorSpy.mockRestore();
  });

  it('closes wizard and resets all state', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard, {
      global: {
        stubs: {
          DashboardToolbar: {
            template:
              '<div class="toolbar-stub"><button class="create" @click="$emit(\'create\')">Create</button></div>',
          },
        },
      },
    });
    await new Promise(r => setTimeout(r, 0));
    await wrapper.find('.create').trigger('click');
    expect(wrapper.find('.wizard-stub').exists()).toBe(true);
    // Find wizard and emit close
    const wizard = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    wizard.vm.$emit('close');
    await nextTick();
    expect(wrapper.find('.wizard-stub').exists()).toBe(false);
  });

  it('restores state from route query on mount', async () => {
    hoisted.routeQuery = { search: 'test', sort: 'name', view: 'list', page: '2', size: '12' };
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });
    Object.defineProperty(document.documentElement, 'clientWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce(
      Array.from({ length: 20 }).map((_, i) => ({
        id: `id${i}`,
        name: `Test Webhook ${i}`,
        createdBy: 'user',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: `http://x${i}`,
        jiraFieldMappings: [],
      }))
    );
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    // Check restored state is applied
    expect(wrapper.find('.title-row-list').exists()).toBe(true); // list view
    expect((wrapper.find('#pageSizeSelect').element as HTMLSelectElement).value).toBe('12');
  });

  it('validates and ignores invalid route query parameters', async () => {
    hoisted.routeQuery = {
      search: 'valid',
      sort: 'invalid-sort',
      view: 'invalid-view',
      page: 'abc',
      size: '999',
    };
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await nextTick();
    // Should fallback to defaults for invalid params (grid view is default, but search is applied)
    // The component properly validates and uses defaults for invalid sort/view/page/size
    expect(wrapper.vm.viewMode).toBe('grid');
    expect(wrapper.vm.currentPage).toBe(1);
  });

  it('restores scroll position from route query', async () => {
    const scrollToSpy = vi.spyOn(window, 'scrollTo');
    hoisted.routeQuery = { scroll: '500' };
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await nextTick();
    await new Promise(r => requestAnimationFrame(r));
    expect(scrollToSpy).toHaveBeenCalled();
    scrollToSpy.mockRestore();
  });

  it('handles unknown action in action menu', async () => {
    const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    // Manually trigger unknown action
    wrapper.vm.handleWebhookAction('unknown-action', { id: 'abc', name: 'Test' } as any);
    expect(consoleWarnSpy).toHaveBeenCalledWith('Unknown action:', 'unknown-action');
    consoleWarnSpy.mockRestore();
  });

  it('shows empty state with create message when no webhooks exist', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([]);
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    const emptyState = wrapper.find('.simple-empty-state');
    expect(emptyState.exists()).toBe(true);
    expect(emptyState.find('.empty-title').text()).toContain('No webhooks found');
    expect(emptyState.find('.empty-subtitle').text()).toContain('Create your first webhook');
  });

  it('normalizes jiraFieldMappings with various data types', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    (JiraWebhookService.getWebhookById as any).mockResolvedValueOnce({
      id: 'abc',
      name: 'Webhook ABC',
      jiraFieldMappings: [
        {
          jiraFieldId: 'field1',
          displayLabel: 'Label1',
          jiraFieldName: 'name1',
          template: 'tmpl',
          dataType: 'string',
          required: true,
          defaultValue: 'def',
          metadata: { key: 'val' },
        },
        {
          jiraFieldId: 123,
          displayLabel: null,
          jiraFieldName: undefined,
          template: null,
          dataType: null,
          required: 'yes',
          defaultValue: null,
          metadata: null,
        },
      ],
    });
    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    await card.find('.action-menu-item').trigger('click'); // Edit
    await nextTick();
    // Wizard should open with normalized data
    expect(wrapper.find('.wizard-stub').exists()).toBe(true);
  });

  it('watches and resets currentPage when search changes', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce(
      Array.from({ length: 20 }).map((_, i) => ({
        id: `id${i}`,
        name: `Webhook ${i}`,
        createdBy: 'user',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: `http://x${i}`,
        jiraFieldMappings: [],
      }))
    );
    const wrapper = mount(JiraWebhookDashboard, {
      global: {
        stubs: {
          DashboardToolbar: {
            template: `
              <div class="toolbar-stub">
                <input class="search" @input="$emit('update:search',$event.target.value)" />
                <button class="next" @click="$emit('nextPage')">Next</button>
              </div>
            `,
          },
        },
      },
    });
    await new Promise(r => setTimeout(r, 0));
    // Go to page 2
    await wrapper.find('.next').trigger('click');
    await nextTick();
    // Change search -> should reset to page 1
    const searchInput = wrapper.find('input.search');
    await searchInput.setValue('Webhook 1');
    await nextTick();
    // currentPage should be 1 after search change
    expect(wrapper.vm.currentPage).toBe(1);
  });

  it('clamps currentPage when totalPages decreases', async () => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });
    Object.defineProperty(document.documentElement, 'clientWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });

    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce(
      Array.from({ length: 20 }).map((_, i) => ({
        id: `id${i}`,
        name: `Webhook ${i}`,
        createdBy: 'user',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: `http://x${i}`,
        jiraFieldMappings: [],
      }))
    );
    const wrapper = mount(JiraWebhookDashboard, {
      global: {
        stubs: {
          DashboardToolbar: {
            template: `
              <div class="toolbar-stub">
                <button class="next" @click="$emit('nextPage')">Next</button>
                <select class="size" @change="$emit('update:pageSize', Number($event.target.value))">
                  <option value="6">6</option>
                  <option value="12">12</option>
                </select>
              </div>
            `,
          },
        },
      },
    });
    await new Promise(r => setTimeout(r, 0));
    // Go to page 3 (with 6 per page, 20 items = 4 pages)
    await wrapper.find('.next').trigger('click');
    await nextTick();
    await wrapper.find('.next').trigger('click');
    await nextTick();
    expect(wrapper.vm.currentPage).toBe(3);
    // Change page size to 12 -> totalPages becomes 2, should clamp to 2
    const sizeSelect = wrapper.find('select.size');
    await sizeSelect.setValue('12');
    await nextTick();
    expect(wrapper.vm.currentPage).toBe(2);
  });

  it('treats non-array webhook responses as an empty list', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce({ items: [] });

    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));
    await nextTick();

    expect(wrapper.find('.simple-empty-state').exists()).toBe(true);
    expect(wrapper.findAll('.webhook-card')).toHaveLength(0);
  });

  it('returns early from confirm when there is no pending webhook', async () => {
    const wrapper = mount(JiraWebhookDashboard);

    await wrapper.vm.handleConfirm();

    expect(confirmWithHandlersMock).not.toHaveBeenCalled();
  });

  it('closes the dialog without performing an action when cancel is clicked', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));

    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    await items[3].trigger('click');
    await wrapper.find('.cancel').trigger('click');

    expect(deleteWebhookMock).not.toHaveBeenCalled();
    expect(closeDialogMock).toHaveBeenCalled();
  });

  it('keeps the webhook list unchanged when deletion fails during confirm', async () => {
    deleteWebhookMock.mockRejectedValueOnce(new Error('delete failed'));
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
      {
        id: 'def',
        name: 'Webhook DEF',
        createdBy: 'bob',
        createdDate: '2024-01-02',
        isEnabled: false,
        webhookUrl: 'http://def',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));

    const initialCount = wrapper.findAll('.webhook-card').length;
    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    await items[3].trigger('click');
    await wrapper.find('.confirm').trigger('click');
    await nextTick();

    expect(wrapper.findAll('.webhook-card')).toHaveLength(initialCount);
  });

  it('normalizes malformed edit payloads and opens the wizard in edit mode', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    (JiraWebhookService.getWebhookById as any).mockResolvedValueOnce({
      id: 123,
      name: null,
      webhookUrl: undefined,
      jiraFieldMappings: { bad: 'shape' },
      isEnabled: 'yes',
      isDeleted: 'no',
      createdBy: 99,
      createdDate: null,
      description: 42,
      samplePayload: false,
      connectionId: { nested: true },
      fieldsMapping: 'bad-shape',
    });

    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));

    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    await card.find('.action-menu-item').trigger('click');
    await nextTick();

    const wizard = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    expect(wizard.exists()).toBe(true);
    expect(wizard.attributes('data-edit-mode')).toBe('true');
    expect(wizard.attributes('data-clone-mode')).toBe('false');
    expect(wrapper.vm.editingWebhookId).toBe('abc');
    expect(wrapper.vm.editingWebhookData).toMatchObject({
      id: '',
      name: '',
      webhookUrl: '',
      jiraFieldMappings: [],
      isEnabled: false,
      isDeleted: false,
      createdBy: '',
      createdDate: '',
      description: '',
      samplePayload: '',
      connectionId: '',
      fieldsMapping: undefined,
    });
  });

  it('normalizes malformed clone payloads and opens the wizard in clone mode', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);
    (JiraWebhookService.getWebhookById as any).mockResolvedValueOnce({
      id: 'abc',
      name: null,
      jiraFieldMappings: { bad: 'shape' },
      description: 42,
      samplePayload: false,
      connectionId: { nested: true },
      fieldsMapping: 'bad-shape',
    });

    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));

    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    await items[1].trigger('click');
    await nextTick();

    const wizard = wrapper.findComponent({ name: 'JiraWebhookWizard' });
    expect(wizard.exists()).toBe(true);
    expect(wizard.attributes('data-edit-mode')).toBe('false');
    expect(wizard.attributes('data-clone-mode')).toBe('true');
    expect(wrapper.vm.cloneMode).toBe(true);
    expect(wrapper.vm.editingWebhookData).toBeUndefined();
    expect(wrapper.vm.cloningWebhookData).toMatchObject({
      id: '',
      name: 'Webhook',
      webhookUrl: '',
      jiraFieldMappings: [],
      isEnabled: false,
      isDeleted: false,
      createdBy: '',
      createdDate: '',
      description: '',
      samplePayload: '',
      connectionId: '',
      fieldsMapping: undefined,
    });
  });

  it('moves back one page and does not go below page one', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce(
      Array.from({ length: 20 }).map((_, i) => ({
        id: `id${i}`,
        name: `Webhook ${i}`,
        createdBy: 'user',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: `http://x${i}`,
        jiraFieldMappings: [],
      }))
    );

    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.nextPage();
    await nextTick();
    expect(wrapper.vm.currentPage).toBe(2);

    wrapper.vm.prevPage();
    await nextTick();
    expect(wrapper.vm.currentPage).toBe(1);

    wrapper.vm.prevPage();
    await nextTick();
    expect(wrapper.vm.currentPage).toBe(1);
  });

  it('clears the pending webhook when the dialog is cancelled', async () => {
    (JiraWebhookService.listJiraWebhooks as any).mockResolvedValueOnce([
      {
        id: 'abc',
        name: 'Webhook ABC',
        createdBy: 'alice',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://abc',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(JiraWebhookDashboard);
    await new Promise(r => setTimeout(r, 0));

    const card = wrapper.find('.webhook-card');
    await card.find('.action-menu-trigger').trigger('click');
    const items = card.findAll('.action-menu-item');
    await items[3].trigger('click');

    expect(wrapper.vm.pendingWebhook).toMatchObject({ id: 'abc' });

    await wrapper.find('.cancel').trigger('click');
    await nextTick();

    expect(wrapper.vm.pendingWebhook).toBe(null);
  });
});
