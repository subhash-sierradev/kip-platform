import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

// Mock useAuditLogs to control states
const fetchMock = vi.fn();
vi.mock('@/composables/useAuditLogs', () => {
  let state: any = { loading: false, error: '', filteredLogs: [] };
  return {
    useAuditLogs: () => ({
      get filteredLogs() {
        return state.filteredLogs;
      },
      get loading() {
        return state.loading;
      },
      get error() {
        return state.error;
      },
      fetchAuditLogs: (...args: any[]) => fetchMock(...args),
      // Helpers to mutate in tests
      __setState: (s: any) => {
        state = { ...state, ...s };
      },
    }),
  };
});

// Reuse existing GenericDataGrid component behavior
import AuditLogPage from '@/components/admin/audit/AuditLogPage.vue';
import { useAuditLogs } from '@/composables/useAuditLogs';

describe('AuditLogPage branches', () => {
  it('shows loading state when loading true', () => {
    (useAuditLogs() as any).__setState({ loading: true, error: '' });
    const wrapper = mount(AuditLogPage);
    expect(wrapper.find('.loading-state').exists()).toBe(true);
  });

  it('shows error state and retry triggers fetch', async () => {
    (useAuditLogs() as any).__setState({ loading: false, error: 'Boom!', filteredLogs: [] });
    const wrapper = mount(AuditLogPage);
    expect(wrapper.find('.error-state').exists()).toBe(true);
    await wrapper.find('.retry-btn').trigger('click');
    expect(fetchMock).toHaveBeenCalled();
  });

  it('renders grid with data when not loading and no error', () => {
    (useAuditLogs() as any).__setState({
      loading: false,
      error: '',
      filteredLogs: [
        {
          entityName: 'X',
          entityType: 'INTEGRATION',
          tenantId: 't',
          performedBy: 'u',
          action: 'UPDATE',
          timestamp: new Date().toISOString(),
        },
      ],
    });
    const wrapper = mount(AuditLogPage);
    // GenericDataGrid presence implies success branch
    expect(wrapper.findComponent({ name: 'GenericDataGrid' }).exists()).toBe(true);
  });
});
