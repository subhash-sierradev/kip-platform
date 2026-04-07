/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';

let filteredLogsData: any[] = [];

vi.mock('@/composables/useAuditLogs', () => {
  return {
    useAuditLogs: () => ({
      tenants: ['t1'],
      selectedTenant: 't1',
      setSelectedTenant: vi.fn(),
      entityTypes: ['Webhook'],
      selectedEntityType: 'Webhook',
      setSelectedEntityType: vi.fn(),
      actions: ['CREATE'],
      selectedAction: 'CREATE',
      setSelectedAction: vi.fn(),
      hasActiveFilters: false,
      clearFilters: vi.fn(),
      logs: [],
      filteredLogs: filteredLogsData,
      loading: false,
      error: null,
      fetchAuditLogs: vi.fn(),
    }),
  };
});

import AuditLogPage from '@/components/admin/audit/AuditLogPage.vue';

describe('AuditLogPage', () => {
  it('renders status chips for type, activity, and result', () => {
    filteredLogsData = [
      {
        entityName: 'Webhook A',
        entityType: 'INTEGRATION',
        tenantId: 't1',
        performedBy: 'alice',
        action: 'CREATE',
        result: 'SUCCESS',
        timestamp: new Date().toISOString(),
      },
    ];

    const wrapper = mount(AuditLogPage, {
      global: {
        stubs: {
          GenericDataGrid: {
            props: ['data'],
            template:
              '<div class="generic-grid-stub"><div v-for="row in data" class="grid-row"><slot name="typeTemplate" :data="row" /><slot name="activityTemplate" :data="row" /><slot name="resultTemplate" :data="row" /></div></div>',
          },
          AuditLogFilters: { template: '<div />' },
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label || status }}</span>',
          },
        },
      },
    });
    expect(wrapper.find('.generic-grid-stub').exists()).toBe(true);
    expect(wrapper.findAll('.status-chip-stub').length).toBe(3);
  });
});
