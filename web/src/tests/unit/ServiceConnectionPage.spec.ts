/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick, ref } from 'vue';
import { mount } from '@vue/test-utils';

const {
  mockGetDependents,
  mockToastError,
  mockToastSuccess,
  mockSetPage,
  mockSetRowsPerPage,
  mockTestConnection,
  mockDeleteConnection,
  mockRotateSecret,
  mockFetchConnections,
} = vi.hoisted(() => ({
  mockGetDependents: vi.fn(),
  mockToastError: vi.fn(),
  mockToastSuccess: vi.fn(),
  mockSetPage: vi.fn(),
  mockSetRowsPerPage: vi.fn(),
  mockTestConnection: vi.fn(),
  mockDeleteConnection: vi.fn(),
  mockRotateSecret: vi.fn(),
  mockFetchConnections: vi.fn(),
}));

const state = {
  allConnections: ref<any[]>([]),
  loading: ref(false),
  error: ref(''),
  page: ref(0),
  rowsPerPage: ref(10),
};

vi.mock('@/composables/useServiceConnectionsAdmin', () => ({
  useServiceConnectionsAdmin: () => ({
    allConnections: state.allConnections,
    loading: state.loading,
    error: state.error,
    page: state.page,
    setPage: mockSetPage,
    rowsPerPage: state.rowsPerPage,
    setRowsPerPage: mockSetRowsPerPage,
    testConnection: mockTestConnection,
    deleteConnection: mockDeleteConnection,
    rotateSecret: mockRotateSecret,
    fetchConnections: mockFetchConnections,
  }),
}));

vi.mock('@/api/services/IntegrationConnectionService', () => ({
  IntegrationConnectionService: {
    getConnectionDependents: mockGetDependents,
  },
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showError: mockToastError,
    showSuccess: mockToastSuccess,
  }),
}));

vi.mock('@/utils/dateUtils', () => ({
  formatMetadataDate: (v: string) => (v ? `date:${v}` : 'N/A'),
}));

const GenericDataGridStub = defineComponent({
  name: 'GenericDataGrid',
  props: {
    data: { type: Array, default: () => [] },
  },
  emits: ['option-changed'],
  setup(props, { slots, emit }) {
    return () =>
      h('div', { class: 'gdg-stub' }, [
        h('button', {
          class: 'emit-page-index',
          onClick: () => emit('option-changed', { fullName: 'paging.pageIndex', value: 2 }),
        }),
        h('button', {
          class: 'emit-page-size',
          onClick: () => emit('option-changed', { fullName: 'paging.pageSize', value: 25 }),
        }),
        ...(props.data as any[]).flatMap((row: any) => [
          h(
            'div',
            { class: 'row-actions' },
            slots.actionsCell ? slots.actionsCell({ data: row }) : []
          ),
          h(
            'div',
            { class: 'row-status' },
            slots.statusCell ? slots.statusCell({ data: row }) : []
          ),
        ]),
      ]);
  },
});

const AppModalStub = {
  props: ['open'],
  template: '<div v-if="open"><slot /><slot name="footer" /></div>',
};

const ConfirmationDialogStub = {
  props: ['open', 'description'],
  emits: ['cancel', 'confirm'],
  template:
    '<div v-if="open"><span class="dialog-desc">{{ description }}</span><button class="confirm-btn" @click="$emit(\'confirm\')">confirm</button><button class="cancel-btn" @click="$emit(\'cancel\')">cancel</button></div>',
};

vi.mock('devextreme-vue/button', () => ({
  default: {
    emits: ['click'],
    props: ['elementAttr'],
    template:
      '<button type="button" v-bind="elementAttr" @click="$emit(\'click\')"><slot /></button>',
  },
}));
vi.mock('devextreme-vue/load-indicator', () => ({
  default: { template: '<div class="load-indicator" />' },
}));
vi.mock('devextreme-vue/data-grid', () => ({
  DxDataGrid: { template: '<div class="dx-grid"><slot /></div>' },
  DxColumn: { template: '<div><slot /></div>' },
  DxPaging: { template: '<div />' },
  DxScrolling: { template: '<div />' },
}));
vi.mock('lucide-vue-next', () => ({
  Eye: { template: '<span />' },
  EyeOff: { template: '<span />' },
}));

import ServiceConnectionPage from '@/components/common/ServiceConnectionPage.vue';
import { ServiceType } from '@/api/models/enums';

function mountPage(serviceType: ServiceType = ServiceType.JIRA) {
  return mount(ServiceConnectionPage, {
    props: { serviceType },
    global: {
      stubs: {
        GenericDataGrid: GenericDataGridStub,
        Tooltip: { template: '<div />' },
        ConfirmationDialog: ConfirmationDialogStub,
        StatusChipForDataTable: { template: '<span />' },
        AppModal: AppModalStub,
      },
    },
  });
}

describe('ServiceConnectionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    state.loading.value = false;
    state.error.value = '';
    state.page.value = 0;
    state.rowsPerPage.value = 10;
    state.allConnections.value = [
      { id: 'c1', lastConnectionStatus: 'OK', lastConnectionTest: new Date().toISOString() },
    ];
  });

  it('calls fetch on mount and renders loading/error branches', async () => {
    state.loading.value = true;
    let wrapper = mountPage();
    expect(mockFetchConnections).toHaveBeenCalled();
    expect(wrapper.find('.loading-wrap').exists()).toBe(true);
    wrapper.unmount();

    state.loading.value = false;
    state.error.value = 'boom';
    wrapper = mountPage();
    expect(wrapper.find('.error-wrap').text()).toContain('boom');
  });

  it('handles grid option changes for page index and page size', async () => {
    const wrapper = mountPage();

    await wrapper.find('.emit-page-index').trigger('click');
    expect(mockSetPage).toHaveBeenCalledWith(2);

    await wrapper.find('.emit-page-size').trigger('click');
    expect(mockSetRowsPerPage).toHaveBeenCalledWith(25);
    expect(mockSetPage).toHaveBeenCalledWith(0);
  });

  it('opens dependents dialog when delete check returns dependents', async () => {
    mockGetDependents.mockResolvedValueOnce({
      serviceType: ServiceType.ARCGIS,
      integrations: [{ id: 'a1', name: 'A', description: 'd', isEnabled: true }],
    });

    const wrapper = mountPage();
    const buttons = wrapper.findAll('button');
    const deleteBtn = buttons.find(b => b.attributes('aria-label') === 'Delete');
    await deleteBtn?.trigger('click');
    await nextTick();

    expect(mockGetDependents).toHaveBeenCalledWith({ connectionId: 'c1' });
    expect(wrapper.find('.dependents-dialog').exists()).toBe(true);
  });

  it('opens confirm dialog when no dependents and deletes on confirm', async () => {
    mockGetDependents.mockResolvedValueOnce({ serviceType: ServiceType.JIRA, integrations: [] });
    mockDeleteConnection.mockResolvedValueOnce(undefined);

    const wrapper = mountPage();
    const buttons = wrapper.findAll('button');
    const deleteBtn = buttons.find(b => b.attributes('aria-label') === 'Delete');
    await deleteBtn?.trigger('click');

    await wrapper.find('.confirm-btn').trigger('click');

    expect(mockDeleteConnection).toHaveBeenCalledWith('c1');
  });

  it('handles secret rotation flow and test action', async () => {
    mockRotateSecret.mockResolvedValueOnce(true);
    mockTestConnection.mockResolvedValueOnce(undefined);

    const wrapper = mountPage();
    const buttons = wrapper.findAll('button');

    // test action button has title attribute
    const testBtn = buttons.find(b => b.attributes('title') === 'Test Connection');
    await testBtn?.trigger('click');
    await wrapper.find('.confirm-btn').trigger('click');
    expect(mockTestConnection).toHaveBeenCalledWith('c1');

    const keyBtn = buttons.find(b => b.attributes('aria-label') === 'Edit Secret');
    await keyBtn?.trigger('click');
    await wrapper.find('#secret-input').setValue('new-secret');

    // confirm secret update is footer second button in secret modal
    await wrapper.find('.secret-actions button:last-child').trigger('click');

    expect(mockRotateSecret).toHaveBeenCalledWith('c1', 'new-secret');
  });

  it('shows toast when dependents check fails', async () => {
    mockGetDependents.mockRejectedValueOnce(new Error('x'));

    const wrapper = mountPage();
    const buttons = wrapper.findAll('button');
    const deleteBtn = buttons.find(b => b.attributes('aria-label') === 'Delete');
    await deleteBtn?.trigger('click');

    expect(mockToastError).toHaveBeenCalledWith('Failed to check connection dependents');
  });

  describe('confirmDialogDescription', () => {
    it('provides delete description for Jira service type', async () => {
      mockGetDependents.mockResolvedValueOnce({ serviceType: ServiceType.JIRA, integrations: [] });

      const wrapper = mountPage(ServiceType.JIRA);
      const deleteBtn = wrapper
        .findAll('button')
        .find(b => b.attributes('aria-label') === 'Delete');
      await deleteBtn?.trigger('click');

      const desc = wrapper.find('.dialog-desc');
      expect(desc.text()).toContain('Are you sure you want to delete this connection');
      expect(desc.text()).toContain('cannot be undone');
    });

    it('provides delete description for ArcGIS service type', async () => {
      mockGetDependents.mockResolvedValueOnce({
        serviceType: ServiceType.ARCGIS,
        integrations: [],
      });

      const wrapper = mountPage(ServiceType.ARCGIS);
      const deleteBtn = wrapper
        .findAll('button')
        .find(b => b.attributes('aria-label') === 'Delete');
      await deleteBtn?.trigger('click');

      const desc = wrapper.find('.dialog-desc');
      expect(desc.text()).toContain('Are you sure you want to delete this connection');
      expect(desc.text()).toContain('cannot be undone');
    });

    it('provides test description for Jira service type', async () => {
      const wrapper = mountPage(ServiceType.JIRA);
      const testBtn = wrapper
        .findAll('button')
        .find(b => b.attributes('title') === 'Test Connection');
      await testBtn?.trigger('click');

      const desc = wrapper.find('.dialog-desc');
      expect(desc.text()).toContain('Jira');
      expect(desc.text()).toContain('test the');
    });
  });

  describe('dependentsNameCaption', () => {
    it('returns Jira Webhook Name when dependentsResponse has JIRA serviceType', async () => {
      mockGetDependents.mockResolvedValueOnce({
        serviceType: ServiceType.JIRA,
        integrations: [{ id: 'w1', name: 'Webhook', description: '', isEnabled: true }],
      });
      const wrapper = mountPage(ServiceType.JIRA);
      const deleteBtn = wrapper
        .findAll('button')
        .find(b => b.attributes('aria-label') === 'Delete');
      await deleteBtn?.trigger('click');
      await nextTick();
      const vm = wrapper.vm as any;
      expect(vm.dependentsNameCaption).toBe('Jira Webhook Name');
    });

    it('returns ArcGIS Integration Name when dependentsResponse has ARCGIS serviceType', async () => {
      mockGetDependents.mockResolvedValueOnce({
        serviceType: ServiceType.ARCGIS,
        integrations: [{ id: 'a1', name: 'ArcGIS', description: '', isEnabled: true }],
      });
      const wrapper = mountPage(ServiceType.ARCGIS);
      const deleteBtn = wrapper
        .findAll('button')
        .find(b => b.attributes('aria-label') === 'Delete');
      await deleteBtn?.trigger('click');
      await nextTick();
      const vm = wrapper.vm as any;
      expect(vm.dependentsNameCaption).toBe('ArcGIS Integration Name');
    });
  });
});
