import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AuditLogPage from '@/components/admin/audit/AuditLogPage.vue';

const state = {
  filteredLogs: [] as any[],
  loading: false,
  error: '' as string,
  fetchAuditLogs: vi.fn(),
};

vi.mock('@/composables/useAuditLogs', () => ({
  useAuditLogs: () => ({
    filteredLogs: state.filteredLogs,
    loading: state.loading,
    error: state.error,
    fetchAuditLogs: state.fetchAuditLogs,
  }),
}));

const GenericDataGridStub = {
  name: 'GenericDataGrid',
  props: ['data', 'columns', 'pageSize', 'enableExport', 'enableClearFilters', 'exportConfig'],
  template: `
    <div class="grid-stub">
      <slot name="nameTemplate" :data="data[0]" />
      <slot name="tenantTemplate" :data="data[0]" />
      <slot name="resultTemplate" :data="data[0]" />
      <slot name="timestampTemplate" :data="data[0]" />
    </div>
  `,
};

const StatusChipStub = {
  name: 'StatusChipForDataTable',
  props: ['status', 'label'],
  template: '<span class="status-chip-stub">{{ label }}</span>',
};

describe('AuditLogPage coverage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    state.filteredLogs = [];
    state.loading = false;
    state.error = '';
  });

  it('renders loading branch', () => {
    state.loading = true;
    const wrapper = mount(AuditLogPage);
    expect(wrapper.find('.loading-state').exists()).toBe(true);
  });

  it('renders error branch and retries', async () => {
    state.error = 'fetch failed';
    const wrapper = mount(AuditLogPage);

    expect(wrapper.find('.error-state').exists()).toBe(true);
    await wrapper.find('.retry-btn').trigger('click');
    expect(state.fetchAuditLogs).toHaveBeenCalledTimes(1);
  });

  it('renders grid branch and slot fallbacks for missing fields', () => {
    state.filteredLogs = [
      {
        entityName: '',
        entityType: 'INTEGRATION',
        tenantId: 'tenant-1',
        performedBy: 'alice',
        action: 'UPDATE',
        result: '',
        timestamp: 1735689600000,
      },
    ];

    const wrapper = mount(AuditLogPage, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: StatusChipStub,
        },
      },
    });

    expect(wrapper.find('.grid-stub').exists()).toBe(true);
    expect(wrapper.text()).toContain('N/A');
    expect(wrapper.text()).toContain('tenant-1');
    expect(wrapper.text()).toContain('-');
  });

  it('provides exportConfig with formatted timestamp values', () => {
    state.filteredLogs = [
      {
        entityName: 'Webhook A',
        entityType: 'INTEGRATION',
        tenantId: 'tenant-1',
        performedBy: 'alice',
        action: 'CREATE',
        result: 'SUCCESS',
        timestamp: 1735689600000,
      },
    ];

    const wrapper = mount(AuditLogPage, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: StatusChipStub,
        },
      },
    });

    const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
    const exportConfig = (grid.props() as any).exportConfig;

    expect(exportConfig.filenamePrefix).toBe('audit-logs');
    expect(exportConfig.headers).toContain('Timestamp');
    expect(exportConfig.headers).toContain('Client IP');

    const picked = exportConfig.pickFields(state.filteredLogs[0]);
    expect(Array.isArray(picked)).toBe(true);
    expect(picked.length).toBe(8);
    expect(typeof picked[7]).toBe('string');
  });

  it('exports empty timestamp when source timestamp is missing', () => {
    state.filteredLogs = [
      {
        entityName: 'Webhook B',
        entityType: 'INTEGRATION',
        tenantId: 'tenant-1',
        performedBy: 'bob',
        action: 'UPDATE',
        result: 'SUCCESS',
      },
    ];

    const wrapper = mount(AuditLogPage, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: StatusChipStub,
        },
      },
    });

    const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
    const exportConfig = (grid.props() as any).exportConfig;
    const picked = exportConfig.pickFields(state.filteredLogs[0]);

    expect(picked[7]).toBe('');
  });

  it('uses export fallback values when optional row fields are missing', () => {
    state.filteredLogs = [
      {
        entityName: '',
        entityType: '',
        tenantId: '',
        performedBy: '',
        action: '',
        result: '',
        timestamp: null,
      },
    ];

    const wrapper = mount(AuditLogPage, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: StatusChipStub,
        },
      },
    });

    const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
    const exportConfig = (grid.props() as any).exportConfig;
    const picked = exportConfig.pickFields(state.filteredLogs[0]);

    expect(picked).toEqual(['N/A', '', '', '', '', '', '', '']);
  });

  it('renders success result label and handles non-number timestamp in template/export', () => {
    state.filteredLogs = [
      {
        entityName: 'Webhook C',
        entityType: 'INTEGRATION',
        tenantId: 'tenant-1',
        performedBy: 'carol',
        action: 'CREATE',
        result: 'SUCCESS',
        timestamp: '2025-01-01T00:00:00Z',
      },
    ];

    const wrapper = mount(AuditLogPage, {
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          StatusChipForDataTable: StatusChipStub,
        },
      },
    });

    expect(wrapper.text()).toContain('SUCCESS');

    const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
    const exportConfig = (grid.props() as any).exportConfig;
    const picked = exportConfig.pickFields(state.filteredLogs[0]);
    expect(typeof picked[7]).toBe('string');
  });
});
