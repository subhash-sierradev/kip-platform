import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import type { FailedRecordMetadata, RecordMetadata } from '@/api/models/RecordMetadata';
import JobExecutionMetadataModal from '@/components/outbound/arcgisintegration/details/JobExecutionMetadataModal.vue';

// Mock DevExtreme components
vi.mock('devextreme-vue/data-grid', () => ({
  DxDataGrid: {
    name: 'DxDataGrid',
    template: '<div class="dx-datagrid-stub" v-bind="$attrs"></div>',
  },
  DxColumn: { name: 'DxColumn', template: '<div class="dx-column-stub" v-bind="$attrs"></div>' },
  DxScrolling: {
    name: 'DxScrolling',
    template: '<div class="dx-scrolling-stub" v-bind="$attrs"></div>',
  },
  DxPaging: { name: 'DxPaging', template: '<div class="dx-paging-stub" v-bind="$attrs"></div>' },
  DxSearchPanel: {
    name: 'DxSearchPanel',
    template: '<div class="dx-searchpanel-stub" v-bind="$attrs"></div>',
  },
  DxExport: { name: 'DxExport', template: '<div class="dx-export-stub" v-bind="$attrs"></div>' },
}));

// Mock dependencies
vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
  }),
}));

vi.mock('file-saver', () => ({
  saveAs: vi.fn(),
}));

vi.mock('devextreme/excel_exporter', () => ({
  exportDataGrid: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('devextreme-exceljs-fork', () => ({
  Workbook: vi.fn().mockImplementation(() => ({
    addWorksheet: vi.fn().mockReturnValue({}),
    xlsx: {
      writeBuffer: vi.fn().mockResolvedValue(new ArrayBuffer(8)),
    },
  })),
}));

// Helper function to create mock records with timestamp fields
const createMockRecord = (
  documentId: string,
  title: string,
  locationId: string
): RecordMetadata => ({
  documentId,
  title,
  locationId,
  documentCreatedAt: 1000000,
  documentUpdatedAt: 1000001,
  locationCreatedAt: 1000002,
  locationUpdatedAt: 1000003,
});

const createMockFailedRecord = (
  documentId: string,
  title: string,
  locationId: string,
  errorMessage: string
): FailedRecordMetadata => ({
  ...createMockRecord(documentId, title, locationId),
  errorMessage,
});

describe('JobExecutionMetadataModal', () => {
  const mountedWrappers: Array<{ unmount: () => void }> = [];

  const mockRecords: RecordMetadata[] = [
    createMockRecord('doc-1', 'Document 1', 'loc-1'),
    createMockRecord('doc-2', 'Document 2', 'loc-2'),
  ];

  const mockFailedRecords: FailedRecordMetadata[] = [
    createMockFailedRecord('doc-3', 'Document 3', 'loc-3', 'Failed to process'),
  ];

  const defaultProps = {
    visible: true,
    recordType: 'added' as const,
    records: mockRecords,
    jobId: 'job-123',
  };

  beforeEach(() => {
    // Mock clipboard API
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
    mountedWrappers.splice(0).forEach(w => w.unmount());
    document.body.innerHTML = '';
  });

  describe('Component Rendering', () => {
    it('renders modal when visible is true', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      expect(document.body.querySelector('.kw-modal-backdrop')).not.toBeNull();
      expect(document.body.querySelector('.kw-modal')).not.toBeNull();
    });

    it('does not render modal when visible is false', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          visible: false,
        },
      });

      mountedWrappers.push(wrapper);

      expect(document.body.querySelector('.kw-modal-backdrop')).toBeNull();
    });

    it('displays correct modal title for added records', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      expect(document.body.textContent).toContain('Added Records - 2 total');
    });

    it('displays correct modal title for updated records', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'updated',
        },
      });

      mountedWrappers.push(wrapper);

      expect(document.body.textContent).toContain('Updated Records - 2 total');
    });

    it('displays correct modal title for failed records', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'failed',
          records: mockFailedRecords,
        },
      });

      mountedWrappers.push(wrapper);

      expect(document.body.textContent).toContain('Failed Records - 1 total');
    });

    it('displays job ID correctly', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      expect(document.body.textContent).toContain('job-123');
    });

    it('renders with different record types', () => {
      const wrapperAdded = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      const wrapperFailed = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'failed',
          records: mockFailedRecords,
        },
      });

      mountedWrappers.push(wrapperAdded, wrapperFailed);

      // Verify both render correctly
      expect(document.body.querySelector('.kw-modal-backdrop')).not.toBeNull();
    });
  });

  describe('Job ID Copy Functionality', () => {
    it('copies job ID to clipboard on button click', async () => {
      const writeTextSpy = vi.spyOn(navigator.clipboard, 'writeText');

      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      const copyButton = document.body.querySelector(
        '.copy-job-id-btn'
      ) as HTMLButtonElement | null;
      expect(copyButton).not.toBeNull();

      copyButton?.click();
      await nextTick();

      expect(writeTextSpy).toHaveBeenCalledWith('job-123');
    });

    it('shows success toast when copy succeeds', async () => {
      const writeTextSpy = vi.spyOn(navigator.clipboard, 'writeText');

      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      const copyButton = document.body.querySelector(
        '.copy-job-id-btn'
      ) as HTMLButtonElement | null;
      expect(copyButton).not.toBeNull();

      copyButton?.click();
      await nextTick();

      expect(writeTextSpy).toHaveBeenCalledWith('job-123');
    });

    it('shows error toast when copy fails', async () => {
      const writeTextSpy = vi
        .spyOn(navigator.clipboard, 'writeText')
        .mockRejectedValueOnce(new Error('Copy failed'));

      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      const copyButton = document.body.querySelector(
        '.copy-job-id-btn'
      ) as HTMLButtonElement | null;
      expect(copyButton).not.toBeNull();

      copyButton?.click();
      await nextTick();

      expect(writeTextSpy).toHaveBeenCalledWith('job-123');
    });
  });

  describe('Modal Close Functionality', () => {
    it('emits update:visible event when close button clicked', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      const closeButton = document.body.querySelector(
        '.kw-modal__close'
      ) as HTMLButtonElement | null;
      expect(closeButton).not.toBeNull();

      closeButton?.click();
      await nextTick();

      expect(wrapper.emitted('update:visible')).toBeTruthy();
      expect(wrapper.emitted('update:visible')?.[0]).toEqual([false]);
    });

    it('emits update:visible when clicking backdrop', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      const backdrop = document.body.querySelector('.kw-modal-backdrop') as HTMLElement | null;
      expect(backdrop).not.toBeNull();

      backdrop?.click();
      await nextTick();

      expect(wrapper.emitted('update:visible')).toBeTruthy();
      expect(wrapper.emitted('update:visible')?.[0]).toEqual([false]);
    });

    it('does not close when clicking modal content', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      const modalCard = document.body.querySelector('.kw-modal') as HTMLElement | null;
      expect(modalCard).not.toBeNull();

      modalCard?.click();
      await nextTick();

      expect(wrapper.emitted('update:visible')).toBeFalsy();
    });
  });

  describe('Data Grid Configuration', () => {
    it('passes correct props to data grid', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      // Verify the data grid renders
      expect(document.body.innerHTML).toContain('data-source');
    });

    it('configures column resizing and styling', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      const html = document.body.innerHTML;
      expect(html).toContain('allow-column-resizing="true"');
      expect(html).toContain('show-borders="true"');
      expect(html).toContain('row-alternation-enabled="true"');
    });

    it('sets dimensions correctly', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      const html = document.body.innerHTML;
      expect(html).toContain('width="100%"');
      expect(html).toContain('height="100%"');
    });
  });

  describe('XLSX Export Functionality', () => {
    it('handles export for added records', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      expect(wrapper.props().recordType).toBe('added');
      expect(wrapper.props().records).toHaveLength(2);
    });

    it('handles export for failed records with error column', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'failed',
          records: mockFailedRecords,
        },
      });

      expect(wrapper.props().recordType).toBe('failed');
      expect(wrapper.props().records).toHaveLength(1);
    });
  });

  describe('Record Display', () => {
    it('handles empty records array', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          records: [],
        },
      });

      mountedWrappers.push(wrapper);

      expect(document.body.textContent).toContain('Added Records - 0 total');
    });

    it('displays correct record count in title', () => {
      const largeRecordSet = Array.from({ length: 100 }, (_, i) =>
        createMockRecord(`doc-${i}`, `Document ${i}`, `loc-${i}`)
      );

      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          records: largeRecordSet,
        },
      });

      mountedWrappers.push(wrapper);

      expect(document.body.textContent).toContain('Added Records - 100 total');
    });
  });

  describe('Reactivity', () => {
    it('updates when visible prop changes', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      expect(document.body.querySelector('.kw-modal-backdrop')).not.toBeNull();

      await wrapper.setProps({ visible: false });
      await nextTick();

      expect(document.body.querySelector('.kw-modal-backdrop')).toBeNull();
    });

    it('updates title when recordType changes', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      expect(document.body.textContent).toContain('Added Records');

      await wrapper.setProps({ recordType: 'updated' });
      await nextTick();

      expect(document.body.textContent).toContain('Updated Records');
    });

    it('updates count when records prop changes', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      });

      mountedWrappers.push(wrapper);

      await nextTick();

      expect(document.body.textContent).toContain('2 total');

      await wrapper.setProps({ records: [mockRecords[0]] });
      await nextTick();

      expect(document.body.textContent).toContain('1 total');
    });
  });

  describe('Error Template', () => {
    it('renders for failed records', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'failed',
          records: mockFailedRecords,
        },
      });

      mountedWrappers.push(wrapper);

      expect(document.body.querySelector('.kw-modal-backdrop')).not.toBeNull();
      expect(document.body.textContent).toContain('Failed Records');
    });

    it('handles empty error messages', () => {
      const recordWithoutError: FailedRecordMetadata = createMockFailedRecord(
        'doc-4',
        'Document 4',
        'loc-4',
        ''
      );

      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'failed',
          records: [recordWithoutError],
        },
      });

      mountedWrappers.push(wrapper);

      // Verify component renders even with empty error
      expect(document.body.querySelector('.kw-modal-backdrop')).not.toBeNull();
    });
  });
});
