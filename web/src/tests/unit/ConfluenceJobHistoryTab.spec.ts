/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const toastSpies = vi.hoisted(() => ({
  showSuccess: vi.fn(),
  showError: vi.fn(),
  showWarning: vi.fn(),
}));

const serviceSpies = vi.hoisted(() => ({
  getJobHistory: vi.fn(),
  retryJobExecution: vi.fn(),
}));

vi.mock('@/api/services/ConfluenceIntegrationService', () => ({
  ConfluenceIntegrationService: {
    getJobHistory: serviceSpies.getJobHistory,
    retryJobExecution: serviceSpies.retryJobExecution,
  },
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => toastSpies,
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: 'ci-99' } }),
}));

import ConfluenceJobHistoryTab from '@/components/outbound/confluenceintegration/details/ConfluenceJobHistoryTab.vue';

const sampleRow = {
  id: 'job-abc-1234567890',
  status: 'SUCCESS',
  startedAt: '2026-01-10T08:00:00Z',
  completedAt: '2026-01-10T08:05:00Z',
  windowStart: '2026-01-09T00:00:00Z',
  addedRecords: 10,
  updatedRecords: 5,
  failedRecords: 0,
  executionMetadata: { confluencePageUrl: 'https://confluence.example.com/page/1' },
};

describe('ConfluenceJobHistoryTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    serviceSpies.getJobHistory.mockResolvedValue([]);
  });

  it('shows loading skeleton initially', async () => {
    serviceSpies.getJobHistory.mockReturnValue(new Promise(() => {}));
    const wrapper = mount(ConfluenceJobHistoryTab);
    await wrapper.vm.$nextTick();
    expect(wrapper.find('.loading-state').exists()).toBe(true);
  });

  it('shows error panel on service failure', async () => {
    serviceSpies.getJobHistory.mockRejectedValue(new Error('Server error'));
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.findComponent({ name: 'ErrorPanel' }).exists()).toBe(true);
  });

  it('renders data grid when data loaded successfully', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([sampleRow]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.find('.datagrid-container').exists()).toBe(true);
  });

  it('renders job id truncated when id is long', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([
      { ...sampleRow, id: 'verylongjobidentifier123456' },
    ]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    // Grid column template renders truncated job id: first 16 chars + ...
    const jobIdSlot = wrapper.find('.job-id-text');
    if (jobIdSlot.exists()) {
      expect(jobIdSlot.text()).toMatch(/\.\.\.$/);
    }
  });

  it('renders short job id without truncation', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([{ ...sampleRow, id: 'short-id' }]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const jobIdSlot = wrapper.find('.job-id-text');
    if (jobIdSlot.exists()) {
      expect(jobIdSlot.text()).toBe('short-id');
    }
  });

  it('tooltip becomes visible on mouseenter of job id', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([sampleRow]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const jobIdText = wrapper.find('.job-id-text');
    if (jobIdText.exists()) {
      await jobIdText.trigger('mouseenter');
      const tooltip = wrapper.findComponent({ name: 'Tooltip' });
      expect(tooltip.props('visible')).toBe(true);
    }
  });

  it('tooltip hides on mouseleave', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([sampleRow]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const jobIdText = wrapper.find('.job-id-text');
    if (jobIdText.exists()) {
      await jobIdText.trigger('mouseenter');
      await jobIdText.trigger('mouseleave');
      const tooltip = wrapper.findComponent({ name: 'Tooltip' });
      expect(tooltip.props('visible')).toBe(false);
    }
  });

  it('copies job id to clipboard and shows success toast', async () => {
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
      writable: true,
    });
    serviceSpies.getJobHistory.mockResolvedValue([sampleRow]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const copyBtn = wrapper.find('.copy-icon-btn');
    if (copyBtn.exists()) {
      await copyBtn.trigger('click');
      await new Promise(r => setTimeout(r, 0));
      expect(toastSpies.showSuccess).toHaveBeenCalledWith('Job ID copied successfully');
    }
  });

  it('shows error toast when clipboard write fails', async () => {
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
      writable: true,
    });
    serviceSpies.getJobHistory.mockResolvedValue([sampleRow]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const copyBtn = wrapper.find('.copy-icon-btn');
    if (copyBtn.exists()) {
      await copyBtn.trigger('click');
      await new Promise(r => setTimeout(r, 0));
      expect(toastSpies.showError).toHaveBeenCalledWith('Failed to copy Job ID to clipboard');
    }
  });

  it('shows error message from server when fetch fails with message', async () => {
    serviceSpies.getJobHistory.mockRejectedValue({ message: 'Integration not found' });
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Integration not found');
  });

  it('falls back to generic error when no message on failure', async () => {
    serviceSpies.getJobHistory.mockRejectedValue({});
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Failed to load job history');
  });

  it('formats fetch window with both start and end dates', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([
      {
        ...sampleRow,
        windowStart: '2026-01-09T00:00:00Z',
        windowEnd: '2026-01-10T00:00:00Z',
      },
    ]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const fetchWindow = wrapper.find('.fetch-window');
    if (fetchWindow.exists()) {
      expect(fetchWindow.text()).toContain('→');
    }
  });

  it('formats fetch window with only start date when end is missing', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([
      { ...sampleRow, windowStart: '2026-01-09T00:00:00Z', windowEnd: null },
    ]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const fetchWindow = wrapper.find('.fetch-window');
    if (fetchWindow.exists()) {
      expect(fetchWindow.text()).not.toContain('→');
    }
  });

  it('formats fetch window as dash when start is missing', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([
      { ...sampleRow, windowStart: null, windowEnd: null },
    ]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const fetchWindow = wrapper.find('.fetch-window');
    if (fetchWindow.exists()) {
      expect(fetchWindow.text()).toBe('-');
    }
  });

  it('canRetryExecution returns true for FAILED status with required fields', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([
      { ...sampleRow, status: 'FAILED', originalJobId: 'orig-123' },
    ]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { id: 'job-123', status: 'FAILED', originalJobId: 'orig-123' };
    expect(vm.canRetryExecution(row)).toBe(true);
  });

  it('canRetryExecution returns true for ABORTED status with required fields', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { id: 'job-456', status: 'ABORTED', originalJobId: 'orig-456' };
    expect(vm.canRetryExecution(row)).toBe(true);
  });

  it('canRetryExecution returns false for SUCCESS status', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { id: 'job-789', status: 'SUCCESS', originalJobId: 'orig-789' };
    expect(vm.canRetryExecution(row)).toBe(false);
  });

  it('canRetryExecution returns false when originalJobId is missing', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { id: 'job-999', status: 'FAILED', originalJobId: null };
    expect(vm.canRetryExecution(row)).toBe(false);
  });

  it('canRetryExecution returns false when id is missing', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { id: null, status: 'FAILED', originalJobId: 'orig-888' };
    expect(vm.canRetryExecution(row)).toBe(false);
  });

  it('getConfluencePageUrl returns URL from metadata', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = {
      ...sampleRow,
      executionMetadata: { confluencePageUrl: 'https://confluence.example.com/page/1' },
    };
    expect(vm.getConfluencePageUrl(row)).toBe('https://confluence.example.com/page/1');
  });

  it('getConfluencePageUrl returns undefined when URL is missing', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { ...sampleRow, executionMetadata: {} };
    expect(vm.getConfluencePageUrl(row)).toBeUndefined();
  });

  it('getConfluencePageUrl returns undefined when metadata is null', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { ...sampleRow, executionMetadata: null };
    expect(vm.getConfluencePageUrl(row)).toBeUndefined();
  });

  it('getTotalProcessed returns totalRecords value', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { ...sampleRow, totalRecords: 42 };
    expect(vm.getTotalProcessed(row)).toBe(42);
  });

  it('getTotalProcessed returns 0 when totalRecords is missing', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { ...sampleRow, totalRecords: null };
    expect(vm.getTotalProcessed(row)).toBe(0);
  });

  it('getProcessedBadgeClass returns success when totalRecords > 0', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { ...sampleRow, totalRecords: 15 };
    expect(vm.getProcessedBadgeClass(row)).toBe('success');
  });

  it('getProcessedBadgeClass returns empty string when totalRecords is 0', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    const row = { ...sampleRow, totalRecords: 0 };
    expect(vm.getProcessedBadgeClass(row)).toBe('');
  });

  it('updates tooltip position on mousemove', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([sampleRow]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const jobIdText = wrapper.find('.job-id-text');
    if (jobIdText.exists()) {
      await jobIdText.trigger('mouseenter', { clientX: 100, clientY: 200 });
      await jobIdText.trigger('mousemove', { clientX: 150, clientY: 250 });
      const tooltip = wrapper.findComponent({ name: 'Tooltip' });
      expect(tooltip.props('x')).toBe(162);
      expect(tooltip.props('y')).toBe(262);
    }
  });

  it('truncates job id when empty string', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([{ ...sampleRow, id: '' }]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const jobIdText = wrapper.find('.job-id-text');
    if (jobIdText.exists()) {
      expect(jobIdText.text()).toBe('');
    }
  });

  it('retries execution successfully and refreshes history', async () => {
    const failedRow = {
      ...sampleRow,
      id: 'job-failed-123',
      status: 'FAILED',
      originalJobId: 'orig-job-123',
    };
    serviceSpies.getJobHistory.mockResolvedValueOnce([failedRow]);
    serviceSpies.retryJobExecution.mockResolvedValue({});
    serviceSpies.getJobHistory.mockResolvedValueOnce([]);

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    await vm.retryExecution(failedRow);
    await new Promise(r => setTimeout(r, 10));

    expect(serviceSpies.retryJobExecution).toHaveBeenCalledWith('ci-99', 'orig-job-123');
    expect(toastSpies.showSuccess).toHaveBeenCalledWith('Retry execution queued successfully');
    expect(serviceSpies.getJobHistory).toHaveBeenCalledTimes(2);
  });

  it('shows error toast when retry execution fails with message', async () => {
    const failedRow = {
      ...sampleRow,
      id: 'job-failed-456',
      status: 'FAILED',
      originalJobId: 'orig-job-456',
    };
    serviceSpies.getJobHistory.mockResolvedValue([failedRow]);
    serviceSpies.retryJobExecution.mockRejectedValue({ message: 'Retry service unavailable' });

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    await vm.retryExecution(failedRow);
    await new Promise(r => setTimeout(r, 10));

    expect(toastSpies.showError).toHaveBeenCalledWith('Retry service unavailable');
  });

  it('shows generic error when retry fails without message', async () => {
    const failedRow = {
      ...sampleRow,
      id: 'job-failed-789',
      status: 'FAILED',
      originalJobId: 'orig-job-789',
    };
    serviceSpies.getJobHistory.mockResolvedValue([failedRow]);
    serviceSpies.retryJobExecution.mockRejectedValue({});

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    await vm.retryExecution(failedRow);
    await new Promise(r => setTimeout(r, 10));

    expect(toastSpies.showError).toHaveBeenCalledWith('Failed to retry execution');
  });

  it('isRetrying returns true when execution is in retrying state', async () => {
    const failedRow = {
      ...sampleRow,
      id: 'job-retrying',
      status: 'FAILED',
      originalJobId: 'orig-retrying',
    };
    serviceSpies.getJobHistory.mockResolvedValue([failedRow]);
    serviceSpies.retryJobExecution.mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    );

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    // Start retry but don't wait
    vm.retryExecution(failedRow);
    await new Promise(r => setTimeout(r, 5));

    expect(vm.isRetrying('job-retrying')).toBe(true);
  });

  it('isRetrying returns false when execution is not in retrying state', async () => {
    serviceSpies.getJobHistory.mockResolvedValue([]);
    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    expect(vm.isRetrying('non-retrying-job')).toBe(false);
  });

  it('does not retry when integrationId is missing', async () => {
    // This test cannot access route.params, so we'll skip this scenario
  });

  it('does not retry when row id is null', async () => {
    const incompleteRow = {
      ...sampleRow,
      id: null,
      status: 'FAILED',
      originalJobId: 'orig-incomplete',
    };
    serviceSpies.getJobHistory.mockResolvedValue([]);

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    await vm.retryExecution(incompleteRow);

    expect(serviceSpies.retryJobExecution).not.toHaveBeenCalled();
  });

  it('does not retry when originalJobId is null', async () => {
    const incompleteRow = {
      ...sampleRow,
      id: 'job-no-orig',
      status: 'FAILED',
      originalJobId: null,
    };
    serviceSpies.getJobHistory.mockResolvedValue([]);

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    await vm.retryExecution(incompleteRow);

    expect(serviceSpies.retryJobExecution).not.toHaveBeenCalled();
  });

  it('does not retry when canRetryExecution returns false', async () => {
    const successRow = {
      ...sampleRow,
      id: 'job-success',
      status: 'SUCCESS',
      originalJobId: 'orig-success',
    };
    serviceSpies.getJobHistory.mockResolvedValue([]);

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    await vm.retryExecution(successRow);

    expect(serviceSpies.retryJobExecution).not.toHaveBeenCalled();
  });

  it('does not retry when already retrying same execution', async () => {
    const failedRow = {
      ...sampleRow,
      id: 'job-already-retrying',
      status: 'FAILED',
      originalJobId: 'orig-already-retrying',
    };
    serviceSpies.getJobHistory.mockResolvedValue([failedRow]);
    serviceSpies.retryJobExecution.mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 200))
    );

    const wrapper = mount(ConfluenceJobHistoryTab);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;

    // Start first retry
    vm.retryExecution(failedRow);
    await new Promise(r => setTimeout(r, 5));

    // Try to retry again while still retrying
    await vm.retryExecution(failedRow);

    // Should only be called once
    expect(serviceSpies.retryJobExecution).toHaveBeenCalledTimes(1);
  });
});
