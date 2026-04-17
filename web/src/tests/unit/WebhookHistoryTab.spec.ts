/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, it, expect, vi } from 'vitest';

// Use a mutable source for history to switch between tests
let historyData: any[] = [
  {
    id: 'e1',
    triggeredAt: '2024-01-01',
    triggeredBy: 'alice',
    status: 'SUCCESS',
    jiraIssueUrl: 'https://jira.example.com/browse/KPC-1',
    retryAttempt: 0,
  },
];
const loadingRef = ref(false);
const fetchWebhookHistory = vi.fn();
const retryWebhookExecution = vi.fn();
const closeTroubleshootDialog = vi.fn();
const showTroubleshootDialog = vi.fn();
const selectedExecution = ref<any>(null);
const troubleshootDialogVisible = ref(false);
const currentExecution = ref<any>(null);

vi.mock('@/composables/useWebhookHistoryData', () => ({
  useWebhookHistoryData: () => ({
    loading: loadingRef,
    executionHistory: ref(historyData),
    fetchWebhookHistory,
  }),
}));

vi.mock('@/composables/useTroubleshootDialog', () => ({
  useTroubleshootDialog: () => ({
    troubleshootDialogVisible,
    selectedExecution,
    currentExecution,
    showTroubleshootDialog,
    closeTroubleshootDialog,
  }),
}));

vi.mock('@/composables/useWebhookHistoryExport', () => ({
  useWebhookHistoryExport: () => ({ headers: [], pickFields: () => [], filenamePrefix: 'x' }),
}));

// Mock actions to avoid Pinia store requirement
vi.mock('@/composables/useWebhookActions', () => ({
  useWebhookActions: () => ({
    retryWebhookExecution,
    isExecutionRetrying: () => false,
    canRetryExecution: () => true,
    getRetryLabel: () => '',
  }),
}));

// Stub GenericDataGrid and devextreme components
vi.mock('@/components/common/GenericDataGrid.vue', () => ({
  default: {
    name: 'GenericDataGrid',
    props: ['data'],
    template:
      '<div class="grid-stub"><div v-for="row in (data||[])" :key="row.id" class="grid-row"><slot name="triggeredAtTemplate" :data="row" /><slot name="statusTemplate" :data="row" /><slot name="actionsTemplate" :data="row" /></div></div>',
  },
}));
vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    emits: ['click'],
    template: '<button class="dx-btn" @click="$emit(\'click\')"><slot /></button>',
  },
}));

import WebhookHistoryTab from '@/components/outbound/jirawebhooks/details/WebhookHistoryTab.vue';

describe('WebhookHistoryTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    loadingRef.value = false;
    selectedExecution.value = null;
    troubleshootDialogVisible.value = false;
    currentExecution.value = null;
    retryWebhookExecution.mockResolvedValue(true);
    historyData = [
      {
        id: 'e1',
        triggeredAt: '2024-01-01',
        triggeredBy: 'alice',
        status: 'SUCCESS',
        jiraIssueUrl: 'https://jira.example.com/browse/KPC-1',
        retryAttempt: 0,
      },
    ];
  });

  it('shows loading state', () => {
    loadingRef.value = true;
    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: true,
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });
    expect(wrapper.find('.loading-state').exists()).toBe(true);
  });

  it('renders grid when execution history is present', async () => {
    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: true,
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });
    expect(wrapper.find('.grid-stub').exists()).toBe(true);
    expect(wrapper.find('.empty-state').exists()).toBe(false);
    expect(fetchWebhookHistory).toHaveBeenCalled();
  });

  it('shows empty state when no history', async () => {
    historyData = [];
    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w2' },
      global: {
        stubs: {
          AppModal: true,
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });
    expect(wrapper.find('.empty-state').exists()).toBe(true);
  });

  it('renders status chip for execution status', () => {
    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: true,
          Bug: true,
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label || status }}</span>',
          },
        },
      },
    });

    expect(wrapper.findAll('.status-chip-stub').length).toBe(1);
  });

  it('renders jira link only when jiraIssueUrl exists', () => {
    historyData = [
      {
        id: 'e1',
        triggeredAt: '2024-01-01',
        triggeredBy: 'alice',
        status: 'SUCCESS',
        jiraIssueUrl: 'https://jira.example.com/browse/KPC-99',
        retryAttempt: 0,
      },
      {
        id: 'e2',
        triggeredAt: '2024-01-01',
        triggeredBy: 'bob',
        status: 'SUCCESS',
        jiraIssueUrl: '',
        retryAttempt: 0,
      },
    ];

    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: true,
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });

    const links = wrapper.findAll('a.action-link.jira-link');
    expect(links.length).toBe(1);
    expect(links[0].attributes('href')).toContain('KPC-99');
  });

  it('shows failed actions and retry badge for failed rows', async () => {
    historyData = [
      {
        id: 'e3',
        triggeredAt: '2024-01-01',
        triggeredBy: 'alice',
        status: 'FAILED',
        jiraIssueUrl: '',
        retryAttempt: 2,
      },
    ];

    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: {
            props: ['open'],
            emits: ['update:open'],
            template: '<div class="app-modal-stub"><slot /><slot name="footer" /></div>',
          },
          StatusChipForDataTable: true,
          Bug: { template: '<i class="bug-stub" />' },
        },
      },
    });

    expect(wrapper.find('.troubleshoot-btn').exists()).toBe(true);
    expect(wrapper.find('.retry-badge').exists()).toBe(true);

    await wrapper.find('.troubleshoot-btn').trigger('click');
    expect(showTroubleshootDialog).toHaveBeenCalled();
  });

  it('retries selected execution and refreshes history', async () => {
    selectedExecution.value = { id: 'e9', status: 'FAILED' } as any;

    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: {
            props: ['open'],
            emits: ['update:open'],
            template: '<div class="app-modal-stub"><slot /><slot name="footer" /></div>',
          },
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });

    const buttons = wrapper.findAll('.dx-btn');
    await buttons[1].trigger('click');

    expect(retryWebhookExecution).toHaveBeenCalledWith(selectedExecution.value);
    expect(closeTroubleshootDialog).toHaveBeenCalled();
    expect(fetchWebhookHistory).toHaveBeenCalled();
  });

  it('does nothing in retry handler when no selected execution', async () => {
    selectedExecution.value = null;

    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: {
            props: ['open'],
            emits: ['update:open'],
            template: '<div class="app-modal-stub"><slot /><slot name="footer" /></div>',
          },
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });

    const buttons = wrapper.findAll('.dx-btn');
    await buttons[1].trigger('click');
    expect(retryWebhookExecution).not.toHaveBeenCalled();
    expect(closeTroubleshootDialog).not.toHaveBeenCalled();
  });

  it('keeps the troubleshoot dialog open when retry execution fails', async () => {
    selectedExecution.value = { id: 'e10', status: 'FAILED' } as any;
    retryWebhookExecution.mockResolvedValueOnce(false);

    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: {
            props: ['open'],
            emits: ['update:open'],
            template: '<div class="app-modal-stub"><slot /><slot name="footer" /></div>',
          },
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });

    const buttons = wrapper.findAll('.dx-btn');
    await buttons[1].trigger('click');

    expect(retryWebhookExecution).toHaveBeenCalledWith(selectedExecution.value);
    expect(closeTroubleshootDialog).not.toHaveBeenCalled();
    expect(fetchWebhookHistory).not.toHaveBeenCalledTimes(2);
  });

  it('formats triggered dates and exposes jira ticket fallbacks from helper methods', () => {
    historyData = [
      {
        id: 'exec-42',
        triggeredAt: '2024-02-03T10:00:00Z',
        triggeredBy: 'alice',
        status: 'SUCCESS',
        jiraIssueUrl: 'https://jira.example.com/issues/not-a-ticket',
        retryAttempt: 0,
      },
    ];

    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: true,
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });

    expect(wrapper.text()).toContain('2024');
    expect((wrapper.vm as any).getJiraTicket(historyData[0])).toBe('KPC-42');
    expect((wrapper.vm as any).getJiraTicket({ id: 'exec-77', jiraIssueUrl: '' })).toBe('KPC-77');
  });

  it('formats troubleshoot content for parsed and raw payloads', () => {
    currentExecution.value = {
      responseStatusCode: 422,
      responseBody: '{"error":"bad request"}',
      incomingPayload: 'plain text payload',
    } as any;
    troubleshootDialogVisible.value = true;

    const wrapper = mount(WebhookHistoryTab, {
      props: { webhookId: 'w1' },
      global: {
        stubs: {
          AppModal: {
            props: ['open'],
            emits: ['update:open'],
            template: '<div class="app-modal-stub"><slot /><slot name="footer" /></div>',
          },
          StatusChipForDataTable: true,
          Bug: true,
        },
      },
    });

    expect(wrapper.text()).toContain('HTTP 422');
    expect(wrapper.text()).toContain('"error": "bad request"');
    expect(wrapper.text()).toContain('plain text payload');
    expect((wrapper.vm as any).formatErrorContent('raw content')).toBe('raw content');
  });
});
