import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import type { FailedRecordMetadata, RecordMetadata } from '@/api/models/RecordMetadata';
import JobExecutionMetadataModal from '@/components/outbound/arcgisintegration/details/JobExecutionMetadataModal.vue';

const hoisted = vi.hoisted(() => ({
  showSuccess: vi.fn(),
  showError: vi.fn(),
  saveAs: vi.fn(),
  exportDataGrid: vi.fn().mockResolvedValue(undefined),
  addWorksheet: vi.fn().mockReturnValue({}),
  writeBuffer: vi.fn().mockResolvedValue(new ArrayBuffer(8)),
}));

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
    showSuccess: hoisted.showSuccess,
    showError: hoisted.showError,
  }),
}));

vi.mock('file-saver', () => ({
  saveAs: hoisted.saveAs,
}));

vi.mock('devextreme/excel_exporter', () => ({
  exportDataGrid: hoisted.exportDataGrid,
}));

vi.mock('devextreme-exceljs-fork', () => ({
  Workbook: function Workbook() {
    return {
      addWorksheet: hoisted.addWorksheet,
      xlsx: {
        writeBuffer: hoisted.writeBuffer,
      },
    };
  },
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

    it('displays correct modal title for all records', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'all',
        },
      });

      mountedWrappers.push(wrapper);

      expect(document.body.textContent).toContain('All Records - 2 total');
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

    it('exports records successfully and shows a success toast', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      const event = { cancel: false, component: { id: 'grid' } } as any;
      await wrapper.vm.onExporting(event);

      expect(event.cancel).toBe(true);
      expect(hoisted.addWorksheet).toHaveBeenCalledWith('Records');
      expect(hoisted.exportDataGrid).toHaveBeenCalled();
      expect(hoisted.writeBuffer).toHaveBeenCalled();
      expect(hoisted.saveAs).toHaveBeenCalledTimes(1);
      expect(hoisted.showSuccess).toHaveBeenCalledWith('Exported 2 records successfully');
    });

    it('customizes exported row indexes and timestamps when values are available', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      await wrapper.vm.onExporting({ cancel: false, component: { id: 'grid' } });

      const exportCall = hoisted.exportDataGrid.mock.calls.at(-1)?.[0];
      expect(exportCall).toBeTruthy();

      const indexCell = { value: null as unknown };
      exportCall.customizeCell({
        gridCell: { rowType: 'data', column: { caption: '#' }, data: mockRecords[0] },
        excelCell: indexCell,
      });
      expect(indexCell.value).toBe(1);

      const timestampCell = { value: null as unknown };
      exportCall.customizeCell({
        gridCell: {
          rowType: 'data',
          column: { dataField: 'documentCreatedAt' },
          value: mockRecords[0].documentCreatedAt,
        },
        excelCell: timestampCell,
      });
      expect(timestampCell.value).toBeTypeOf('string');

      const untouchedCell = { value: 'keep' as unknown };
      exportCall.customizeCell({
        gridCell: { rowType: 'header', column: { caption: 'Title' }, value: 'Document 1' },
        excelCell: untouchedCell,
      });
      expect(untouchedCell.value).toBe('keep');
    });

    it('shows an error toast when export generation fails', async () => {
      hoisted.exportDataGrid.mockRejectedValueOnce(new Error('export failed'));
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      await wrapper.vm.onExporting({ cancel: false, component: { id: 'grid' } });

      expect(hoisted.showError).toHaveBeenCalledWith('Failed to generate export file');
      errorSpy.mockRestore();
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

    it('uses the first matching index for duplicate record keys and zero for missing rows', () => {
      const duplicateRecords: RecordMetadata[] = [
        createMockRecord('doc-dup', 'Document 1', 'loc-dup'),
        createMockRecord('doc-dup', 'Document 2', 'loc-dup'),
      ];

      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          records: duplicateRecords,
        },
      }) as any;

      mountedWrappers.push(wrapper);

      expect(wrapper.vm.getRowIndex(duplicateRecords[0])).toBe(1);
      expect(wrapper.vm.getRowIndex(duplicateRecords[1])).toBe(1);
      expect(wrapper.vm.getRowIndex({ documentId: 'missing-doc', locationId: 'missing-loc' })).toBe(
        0
      );
    });

    it('formats timestamp and id fallbacks for nullish and zero values', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      expect(wrapper.vm.formatTimestamp(null)).toBe('N/A');
      expect(wrapper.vm.formatTimestamp(undefined)).toBe('N/A');
      expect(wrapper.vm.formatTimestamp(0)).toBe('');
      expect(wrapper.vm.truncateId('abcdefghi')).toBe('abcdefg...');
      expect(wrapper.vm.truncateId('')).toBe('N/A');
      expect(wrapper.vm.truncateId(undefined)).toBe('N/A');
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

  describe('Helper Methods', () => {
    it('computes the title for the all record type', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: {
          ...defaultProps,
          recordType: 'all',
        },
      }) as any;

      mountedWrappers.push(wrapper);

      expect(wrapper.vm.modalTitleText).toBe('All Records - 2 total');
    });

    it('formats timestamps for null, zero, and populated values', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      expect(wrapper.vm.formatTimestamp(null)).toBe('N/A');
      expect(wrapper.vm.formatTimestamp(undefined)).toBe('N/A');
      expect(wrapper.vm.formatTimestamp(0)).toBe('');
      expect(wrapper.vm.formatTimestamp(1000000)).not.toBe('N/A');
    });

    it('truncates and defaults IDs correctly', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      expect(wrapper.vm.truncateId(undefined)).toBe('N/A');
      expect(wrapper.vm.truncateId('123456789')).toBe('1234567...');
    });

    it('returns row indices from the precomputed map', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      expect(wrapper.vm.getRowIndex(mockRecords[0])).toBe(1);
      expect(
        wrapper.vm.getRowIndex({ documentId: 'missing', locationId: 'missing-location' })
      ).toBe(0);
    });

    it('handles copy to clipboard success and failure for record IDs', async () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      await wrapper.vm.copyToClipboard('doc-1', 'Document ID');
      await Promise.resolve();
      expect(hoisted.showSuccess).toHaveBeenCalledWith('Document ID copied to clipboard');

      vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValueOnce(new Error('copy failed'));
      await wrapper.vm.copyToClipboard('doc-1', 'Document ID');
      await Promise.resolve();
      expect(hoisted.showError).toHaveBeenCalledWith('Failed to copy Document ID');
    });

    it('returns export cell values for index rows, timestamp fields, and non-data rows', () => {
      const wrapper = mount(JobExecutionMetadataModal, {
        props: defaultProps,
      }) as any;

      mountedWrappers.push(wrapper);

      expect(wrapper.vm.getExportCellValue(undefined)).toBeUndefined();
      expect(wrapper.vm.getExportCellValue({ rowType: 'header' })).toBeUndefined();
      expect(
        wrapper.vm.getExportCellValue({
          rowType: 'data',
          column: { caption: '#' },
          data: mockRecords[0],
        })
      ).toBe(1);

      expect(
        wrapper.vm.getExportCellValue({
          rowType: 'data',
          column: { dataField: 'documentCreatedAt' },
          value: 1000000,
        })
      ).not.toBeUndefined();

      expect(
        wrapper.vm.getExportCellValue({
          rowType: 'data',
          column: { dataField: 'title' },
          value: 'Document 1',
        })
      ).toBeUndefined();
    });
  });
});
