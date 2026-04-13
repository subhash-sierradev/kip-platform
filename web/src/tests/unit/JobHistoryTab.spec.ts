import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import JobHistoryTab from '@/components/outbound/arcgisintegration/details/JobHistoryTab.vue';

const historyRef = ref<any[]>([]);
const loadingRef = ref(false);
const errorRef = ref<string | null>(null);
const fetchJobHistoryMock = vi.fn();
const showSuccess = vi.fn();
const showError = vi.fn();
let routeId = 'test-id';

vi.mock('@/composables/useArcGISJobHistory', () => ({
  useArcGISJobHistory: () => ({
    history: historyRef,
    loading: loadingRef,
    error: errorRef,
    fetchJobHistory: fetchJobHistoryMock,
  }),
}));

// Mock the service
vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getJobHistory: vi.fn(),
  },
}));

// Mock Vue Router
vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { id: routeId },
  }),
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showSuccess,
    showError,
  }),
}));

const GenericDataGridStub = {
  props: ['data'],
  template: `
    <div class="grid-stub">
      <div v-for="row in (data || [])" :key="row.id" class="grid-row">
        <slot name="jobIdTemplate" :data="row" />
        <slot name="keyTimeTemplate" :data="row" />
        <slot name="windowStartTemplate" :data="row" />
        <slot name="statusTemplate" :data="row" />
        <slot name="addedTemplate" :data="row" />
        <slot name="updatedTemplate" :data="row" />
        <slot name="failedTemplate" :data="row" />
        <slot name="totalTemplate" :data="row" />
      </div>
    </div>
  `,
};

describe('JobHistoryTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    historyRef.value = [];
    loadingRef.value = false;
    errorRef.value = null;
    routeId = 'test-id';
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    });
  });

  it('renders component', () => {
    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    expect(wrapper.exists()).toBe(true);
    expect(fetchJobHistoryMock).toHaveBeenCalledWith('test-id');
  });

  it('shows loading state when loading is true', () => {
    loadingRef.value = true;
    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    expect(wrapper.find('.loading-state').exists()).toBe(true);
  });

  it('does not fetch history when route id is missing', () => {
    routeId = '';

    mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    expect(fetchJobHistoryMock).not.toHaveBeenCalled();
  });

  it('shows error panel when error exists', () => {
    errorRef.value = 'Unable to load';
    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: { template: '<div class="error-stub" />' },
          JobExecutionMetadataModal: true,
        },
      },
    });

    expect(wrapper.find('.error-stub').exists()).toBe(true);
  });

  it('copies job id successfully and opens metadata modal for non-zero records', async () => {
    historyRef.value = [
      {
        id: 'job-id-very-long-12345',
        status: 'SUCCESS',
        startedAt: '2024-01-01T00:00:00Z',
        completedAt: '2024-01-01T00:01:00Z',
        windowStart: '2024-01-01T00:00:00Z',
        windowEnd: '2024-01-01T00:01:00Z',
        triggeredByUser: 'alice',
        addedRecords: 2,
        addedRecordsMetadata: [{ documentId: 'd1', title: 'Doc', locationId: 'l1' }],
        updatedRecords: 0,
        failedRecords: 0,
        totalRecords: 0,
      },
    ];

    const writeTextSpy = vi.spyOn(navigator.clipboard, 'writeText');
    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label || status }}</span>',
          },
          Tooltip: { template: '<div class="tooltip-stub" />' },
          ErrorPanel: { template: '<div class="error-stub" />' },
          JobExecutionMetadataModal: {
            props: ['visible', 'recordType', 'records', 'jobId'],
            template:
              '<div class="metadata-modal-stub" :data-visible="visible" :data-type="recordType" :data-job-id="jobId" :data-count="records?.length || 0" />',
          },
        },
      },
    });

    expect(wrapper.find('.job-id-text').text()).toContain('...');

    await wrapper.find('.copy-icon-btn').trigger('click');
    expect(writeTextSpy).toHaveBeenCalledWith('job-id-very-long-12345');
    expect(showSuccess).toHaveBeenCalled();

    await wrapper.find('.count-badge.clickable.success').trigger('click');
    const modal = wrapper.find('.metadata-modal-stub');
    expect(modal.attributes('data-visible')).toBe('true');
    expect(modal.attributes('data-type')).toBe('added');
    expect(modal.attributes('data-job-id')).toBe('job-id-very-long-12345');
    expect(modal.attributes('data-count')).toBe('1');
  });

  it('shows toast error when clipboard copy fails', async () => {
    historyRef.value = [
      {
        id: 'job-1',
        status: 'SUCCESS',
        startedAt: '2024-01-01T00:00:00Z',
        completedAt: '2024-01-01T00:01:00Z',
        windowStart: '2024-01-01T00:00:00Z',
        windowEnd: '2024-01-01T00:01:00Z',
        triggeredByUser: 'alice',
        addedRecords: 0,
        updatedRecords: 0,
        failedRecords: 0,
        totalRecords: 0,
      },
    ];

    vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValueOnce(new Error('Clipboard denied'));

    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    await wrapper.find('.copy-icon-btn').trigger('click');
    expect(showError).toHaveBeenCalled();
  });

  it('shows tooltip on mouse enter, updates position on move, and hides on leave', async () => {
    historyRef.value = [
      {
        id: 'job-tooltip-1',
        status: 'SUCCESS',
        startedAt: '2024-01-01T00:00:00Z',
        completedAt: '2024-01-01T00:01:00Z',
        windowStart: '2024-01-01T00:00:00Z',
        windowEnd: '2024-01-01T00:01:00Z',
        triggeredByUser: 'alice',
        addedRecords: 0,
        updatedRecords: 0,
        failedRecords: 0,
        totalRecords: 0,
      },
    ];

    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: {
            props: ['visible', 'text', 'x', 'y'],
            template:
              '<div class="tooltip-stub" :data-visible="visible" :data-text="text" :data-x="x" :data-y="y" />',
          },
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    const idText = wrapper.find('.job-id-text');
    await idText.trigger('mouseenter', { clientX: 100, clientY: 150 });

    let tooltip = wrapper.find('.tooltip-stub');
    expect(tooltip.attributes('data-visible')).toBe('true');
    expect(tooltip.attributes('data-text')).toBe('job-tooltip-1');
    expect(tooltip.attributes('data-x')).toBe('112');
    expect(tooltip.attributes('data-y')).toBe('162');

    await idText.trigger('mousemove', { clientX: 30, clientY: 40 });
    tooltip = wrapper.find('.tooltip-stub');
    expect(tooltip.attributes('data-x')).toBe('42');
    expect(tooltip.attributes('data-y')).toBe('52');

    await idText.trigger('mouseleave');
    tooltip = wrapper.find('.tooltip-stub');
    expect(tooltip.attributes('data-visible')).toBe('false');
  });

  it('opens metadata modal for updated, failed, and total record branches', async () => {
    historyRef.value = [
      {
        id: 'job-record-branches',
        status: 'FAILED',
        startedAt: '2024-01-01T00:00:00Z',
        completedAt: '2024-01-01T00:01:00Z',
        windowStart: '2024-01-01T00:00:00Z',
        windowEnd: '2024-01-01T00:01:00Z',
        triggeredByUser: 'alice',
        addedRecords: 0,
        updatedRecords: 2,
        updatedRecordsMetadata: [{ documentId: 'u1', title: 'Updated', locationId: 'l1' }],
        failedRecords: 1,
        failedRecordsMetadata: [
          {
            recordId: 'f1',
            title: 'Failed',
            reason: 'Validation failed',
            details: 'Missing required field',
          },
        ],
        errorMessage: 'Job had failed records',
        totalRecords: 3,
        totalRecordsMetadata: [{ documentId: 'a1', title: 'All', locationId: 'l2' }],
      },
    ];

    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label || status }}</span>',
          },
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: {
            props: ['visible', 'recordType', 'records', 'jobId'],
            template:
              '<div class="metadata-modal-stub" :data-visible="visible" :data-type="recordType" :data-job-id="jobId" :data-count="records?.length || 0" />',
          },
        },
      },
    });

    expect(wrapper.find('.job-id-text').text()).toBe('job-record-branc...');

    const failedBadge = wrapper.find('.count-badge.clickable.error');
    expect(failedBadge.attributes('title')).toBe('Job had failed records');

    await wrapper.find('.count-badge.clickable.info').trigger('click');
    let modal = wrapper.find('.metadata-modal-stub');
    expect(modal.attributes('data-type')).toBe('updated');
    expect(modal.attributes('data-count')).toBe('1');

    await wrapper.find('.count-badge.clickable.error').trigger('click');
    modal = wrapper.find('.metadata-modal-stub');
    expect(modal.attributes('data-type')).toBe('failed');
    expect(modal.attributes('data-count')).toBe('1');

    await wrapper.find('.count-badge.clickable.total').trigger('click');
    modal = wrapper.find('.metadata-modal-stub');
    expect(modal.attributes('data-type')).toBe('all');
    expect(modal.attributes('data-count')).toBe('1');
  });

  it('shows "-" for all count columns when job status is RUNNING', async () => {
    historyRef.value = [
      {
        id: 'job-running-1',
        status: 'RUNNING',
        startedAt: '2024-01-01T00:00:00Z',
        completedAt: null,
        windowStart: '2024-01-01T00:00:00Z',
        windowEnd: '2024-01-01T00:01:00Z',
        triggeredByUser: 'alice',
        addedRecords: 0,
        updatedRecords: 0,
        failedRecords: 0,
        totalRecords: 0,
      },
    ];

    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    const allBadges = wrapper.findAll('.count-badge');
    // All 4 count badges (added, updated, failed, total) should show "-"
    const dashBadges = allBadges.filter(b => b.text() === '-');
    expect(dashBadges).toHaveLength(4);

    // No clickable badges should exist for a RUNNING job
    expect(wrapper.findAll('.count-badge.clickable')).toHaveLength(0);
  });

  it('shows zero for all count columns when job status is ABORTED', async () => {
    historyRef.value = [
      {
        id: 'job-aborted-1',
        status: 'ABORTED',
        startedAt: '2024-01-01T00:00:00Z',
        completedAt: null,
        windowStart: '2024-01-01T00:00:00Z',
        windowEnd: '2024-01-01T00:01:00Z',
        triggeredByUser: 'alice',
        addedRecords: 0,
        updatedRecords: 0,
        failedRecords: 0,
        totalRecords: 0,
      },
    ];

    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    const allBadges = wrapper.findAll('.count-badge');
    const zeroBadges = allBadges.filter(b => b.text() === '0');
    expect(zeroBadges).toHaveLength(4);

    expect(wrapper.findAll('.count-badge.clickable')).toHaveLength(0);
  });

  it('shows actual values for all count columns when job status is SUCCESS', async () => {
    historyRef.value = [
      {
        id: 'job-success-1',
        status: 'SUCCESS',
        startedAt: '2024-01-01T00:00:00Z',
        completedAt: '2024-01-01T00:05:00Z',
        windowStart: '2024-01-01T00:00:00Z',
        windowEnd: '2024-01-01T00:01:00Z',
        triggeredByUser: 'alice',
        addedRecords: 10,
        updatedRecords: 5,
        failedRecords: 0,
        totalRecords: 15,
        addedRecordsMetadata: [],
        updatedRecordsMetadata: [],
        totalRecordsMetadata: [],
      },
    ];

    const wrapper = mount(JobHistoryTab, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: true,
          Tooltip: true,
          ErrorPanel: true,
          JobExecutionMetadataModal: true,
        },
      },
    });

    // No "-" dashes should appear for completed jobs
    const dashBadges = wrapper.findAll('.count-badge').filter(b => b.text() === '-');
    expect(dashBadges).toHaveLength(0);

    // Clickable badges for added (10) and updated (5) and total (15)
    expect(wrapper.find('.count-badge.clickable.success').text()).toBe('10');
    expect(wrapper.find('.count-badge.clickable.info').text()).toBe('5');
    expect(wrapper.find('.count-badge.clickable.total').text()).toBe('15');
  });
});
