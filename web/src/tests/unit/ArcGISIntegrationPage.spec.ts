import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import ArcGISIntegrationPage from '@/components/outbound/arcgisintegration/ArcGISIntegrationPage.vue';
import { ROUTE_UPDATE_DEBOUNCE_MS } from '@/composables/useListRouteSync';

const hoisted = vi.hoisted(() => ({
  router: {
    push: vi.fn(() => Promise.resolve()),
    replace: vi.fn(() => Promise.resolve()),
    getRoutes: () => [],
    addRoute: vi.fn(),
  },
  route: {
    path: '/outbound/integration/arcgis',
    query: {} as Record<string, unknown>,
  },
  getAllIntegrationsMock: vi.fn(),
  deleteIntegrationMock: vi.fn(),
  toggleIntegrationStatusMock: vi.fn(),
  triggerJobExecutionMock: vi.fn(),
  showErrorMock: vi.fn(),
  showSuccessMock: vi.fn(),
  openDialogMock: vi.fn(),
  closeDialogMock: vi.fn(),
  confirmWithHandlersMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRouter: () => hoisted.router,
  useRoute: () => hoisted.route,
}));

vi.mock('@/composables/useArcGISIntegrationActions', () => ({
  useArcGISIntegrationActions: () => ({
    loading: false,
    getAllIntegrations: hoisted.getAllIntegrationsMock,
    deleteIntegration: hoisted.deleteIntegrationMock,
  }),
  useArcGISIntegrationEditor: () => ({
    loading: false,
    updateIntegrationFromWizard: vi.fn().mockResolvedValue(true),
  }),
  useArcGISIntegrationStatus: () => ({
    toggleIntegrationStatus: hoisted.toggleIntegrationStatusMock,
  }),
  useArcGISIntegrationTrigger: () => ({
    loading: false,
    triggerJobExecution: hoisted.triggerJobExecutionMock,
  }),
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showInfo: vi.fn(),
    showError: hoisted.showErrorMock,
    showSuccess: hoisted.showSuccessMock,
  }),
}));

vi.mock('@/composables/useConfirmationDialog', () => ({
  useConfirmationDialog: () => ({
    dialogOpen: false,
    actionLoading: false,
    pendingAction: null,
    dialogTitle: 'Confirm Action',
    dialogDescription: 'Are you sure?',
    dialogConfirmLabel: 'Yes',
    openDialog: hoisted.openDialogMock,
    closeDialog: hoisted.closeDialogMock,
    confirmWithHandlers: hoisted.confirmWithHandlersMock,
  }),
  createDefaultDialogConfig: () => ({
    enable: { title: 'Enable', desc: 'Enable this integration?', label: 'Enable' },
    disable: { title: 'Disable', desc: 'Disable this integration?', label: 'Disable' },
    delete: { title: 'Delete', desc: 'Delete this integration?', label: 'Delete' },
  }),
}));

const DashboardToolbarStub = {
  name: 'DashboardToolbar',
  props: [
    'search',
    'sortBy',
    'viewMode',
    'currentPage',
    'pageSize',
    'totalCount',
    'sortOptions',
    'pageSizeOptions',
  ],
  emits: ['update:search', 'update:sortBy', 'update:pageSize', 'setViewMode', 'create'],
  template: '<div class="toolbar-stub" />',
};

const ArcGISIntegrationCardStub = {
  name: 'ArcGISIntegrationCard',
  props: ['integration', 'menuItems'],
  emits: ['action', 'open'],
  template: '<div class="integration-card" />',
};

const ConfirmationDialogStub = {
  name: 'ConfirmationDialog',
  props: ['open', 'type', 'title', 'description', 'confirmLabel', 'loading'],
  emits: ['confirm', 'cancel'],
  template: '<div data-testid="confirmation-dialog" />',
};

const ArcGISWizardStub = {
  name: 'ArcGISIntegrationWizard',
  props: ['open', 'mode', 'integrationId'],
  emits: ['close', 'integration-created', 'integration-updated'],
  template: '<div class="wizard-stub" />',
};

function mountPage() {
  return mount(ArcGISIntegrationPage, {
    global: {
      stubs: {
        DashboardToolbar: DashboardToolbarStub,
        ArcGISIntegrationCard: ArcGISIntegrationCardStub,
        ConfirmationDialog: ConfirmationDialogStub,
        ArcGISIntegrationWizard: ArcGISWizardStub,
      },
    },
  });
}

describe('ArcGISIntegrationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.route.query = {};
    hoisted.getAllIntegrationsMock.mockResolvedValue([]);
    hoisted.deleteIntegrationMock.mockResolvedValue(true);
    hoisted.toggleIntegrationStatusMock.mockResolvedValue(true);
    hoisted.triggerJobExecutionMock.mockResolvedValue(undefined);
    hoisted.router.push.mockImplementation(() => Promise.resolve());
    hoisted.router.replace.mockImplementation(() => Promise.resolve());
  });

  it('renders with no integration cards when list is empty', async () => {
    const wrapper = mountPage();
    await nextTick();
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.findAll('.integration-card').length).toBe(0);
  });

  it('opens create wizard when toolbar emits create', async () => {
    const wrapper = mountPage();
    await nextTick();

    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('create');
    await nextTick();

    const wizard = wrapper
      .findAllComponents({ name: 'ArcGISIntegrationWizard' })
      .find(w => (w.props() as any).mode === undefined);
    expect(wizard?.exists()).toBe(true);
    expect((wizard?.props() as any).open).toBe(true);
  });

  it('applies route query to initial toolbar state', async () => {
    hoisted.route.query = { search: 'road', sort: 'name', view: 'list', page: '2', size: '12' };
    const wrapper = mountPage();
    await nextTick();

    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    const props = toolbar.props() as Record<string, unknown>;
    expect(props.search).toBe('road');
    expect(props.sortBy).toBe('name');
    expect(props.viewMode).toBe('list');
    expect(props.currentPage).toBe(2);
    expect(props.pageSize).toBe(12);
  });

  it('syncs search changes to route query via debounced router.replace', async () => {
    vi.useFakeTimers();
    const wrapper = mountPage();

    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('update:search', 'forest');
    await nextTick();

    vi.advanceTimersByTime(ROUTE_UPDATE_DEBOUNCE_MS - 1);
    expect(hoisted.router.replace).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(hoisted.router.replace).toHaveBeenCalledTimes(1);

    const replaceCalls = hoisted.router.replace.mock.calls as unknown as Array<Array<unknown>>;
    const replaceArg =
      (replaceCalls[0]?.[0] as
        | {
            query?: Record<string, unknown>;
          }
        | undefined) ?? {};
    expect(replaceArg.query).toMatchObject({ search: 'forest' });
    vi.useRealTimers();
  });

  it('opens details and shows toast on navigation failure', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-1',
        name: 'Arc One',
        createdBy: 'alice',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
      },
    ]);
    hoisted.router.push.mockImplementationOnce(() => Promise.reject(new Error('nav failed')));

    const wrapper = mountPage();
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    const card = wrapper.findComponent({ name: 'ArcGISIntegrationCard' });
    expect(card.exists()).toBe(true);

    card.vm.$emit('open', 'arc-1');
    await new Promise(r => setTimeout(r, 0));

    expect(hoisted.router.push).toHaveBeenCalled();
    expect(hoisted.showErrorMock).toHaveBeenCalledWith('Failed to open integration details');
  });

  it('opens confirmation dialog for run-now action and warns for unknown action', async () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-2',
        name: 'Arc Two',
        createdBy: 'bob',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
      },
    ]);

    const wrapper = mountPage();
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    const card = wrapper.findComponent({ name: 'ArcGISIntegrationCard' });
    card.vm.$emit('action', 'trigger');
    card.vm.$emit('action', 'something-else');

    expect(hoisted.openDialogMock).toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalledWith('Unknown action:', 'something-else');

    warnSpy.mockRestore();
  });

  it('opens confirmation dialogs for enable, disable, and delete actions', async () => {
    const wrapper = mountPage() as any;
    await nextTick();

    const enabledIntegration = {
      id: 'arc-enabled',
      name: 'Enabled Arc',
      createdBy: 'bob',
      createdDate: '2024-01-01T00:00:00Z',
      isEnabled: true,
    };
    const disabledIntegration = {
      ...enabledIntegration,
      id: 'arc-disabled',
      isEnabled: false,
    };

    wrapper.vm.handleIntegrationAction('disable', enabledIntegration);
    wrapper.vm.handleIntegrationAction('enable', disabledIntegration);
    wrapper.vm.handleIntegrationAction('delete', enabledIntegration);

    expect(hoisted.openDialogMock).toHaveBeenNthCalledWith(1, 'disable');
    expect(hoisted.openDialogMock).toHaveBeenNthCalledWith(2, 'enable');
    expect(hoisted.openDialogMock).toHaveBeenNthCalledWith(3, 'delete');
  });

  it('filters integrations by creator name when search text is set', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-1',
        name: 'Road Sync',
        createdBy: 'alice',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
      },
      {
        id: 'arc-2',
        name: 'River Sync',
        createdBy: 'bob',
        createdDate: '2024-01-02T00:00:00Z',
        isEnabled: false,
      },
    ]);

    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.search = 'bob';
    await nextTick();

    expect(wrapper.vm.filteredIntegrations).toHaveLength(1);
    expect(wrapper.vm.filteredIntegrations[0].id).toBe('arc-2');
  });

  it('filters integrations by integration name when search text matches the title', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-road',
        name: 'Road Sync',
        createdBy: 'alice',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
      },
      {
        id: 'arc-river',
        name: 'River Sync',
        createdBy: 'bob',
        createdDate: '2024-01-02T00:00:00Z',
        isEnabled: false,
      },
    ]);

    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.search = 'road';
    await nextTick();

    expect(wrapper.vm.filteredIntegrations).toHaveLength(1);
    expect(wrapper.vm.filteredIntegrations[0].id).toBe('arc-road');
  });

  it('omits the trigger menu item for disabled integrations', async () => {
    const wrapper = mountPage() as any;
    await nextTick();

    const items = wrapper.vm.getIntegrationMenuItems({
      id: 'arc-disabled',
      name: 'Disabled',
      createdBy: 'user',
      createdDate: '2024-01-01T00:00:00Z',
      isEnabled: false,
    });

    expect(items.some((item: { id: string }) => item.id === 'trigger')).toBe(false);
    expect(items.some((item: { id: string }) => item.id === 'enable')).toBe(true);
  });

  it('opens edit and clone wizards from integration actions', async () => {
    const wrapper = mountPage() as any;
    await nextTick();

    const integration = {
      id: 'arc-3',
      name: 'Arc Three',
      createdBy: 'eve',
      createdDate: '2024-01-01T00:00:00Z',
      isEnabled: true,
    };

    wrapper.vm.handleIntegrationAction('edit', integration);
    wrapper.vm.handleIntegrationAction('clone', integration);
    await nextTick();

    expect(wrapper.vm.editWizardOpen).toBe(true);
    expect(wrapper.vm.editingIntegrationId).toBe('arc-3');
    expect(wrapper.vm.cloneWizardOpen).toBe(true);
    expect(wrapper.vm.cloningIntegrationId).toBe('arc-3');
  });

  it('stores pending integration and stops propagation when opening a dialog', async () => {
    const wrapper = mountPage() as any;
    await nextTick();
    const stopPropagation = vi.fn();
    const integration = {
      id: 'arc-4',
      name: 'Arc Four',
      createdBy: 'sam',
      createdDate: '2024-01-01T00:00:00Z',
      isEnabled: true,
    };

    wrapper.vm.openDialog('delete', integration, { stopPropagation } as unknown as Event);

    expect(stopPropagation).toHaveBeenCalledTimes(1);
    expect(wrapper.vm.pendingIntegration).toEqual(integration);
    expect(hoisted.openDialogMock).toHaveBeenCalledWith('delete');
  });

  it('returns early from handleConfirm when there is no pending integration', async () => {
    const wrapper = mountPage() as any;
    await nextTick();

    await wrapper.vm.handleConfirm();

    expect(hoisted.confirmWithHandlersMock).not.toHaveBeenCalled();
  });

  it('passes delete and run-now handlers to confirmWithHandlers', async () => {
    const wrapper = mountPage() as any;
    await nextTick();
    wrapper.vm.pendingIntegration = {
      id: 'arc-5',
      name: 'Arc Five',
      createdBy: 'lee',
      createdDate: '2024-01-01T00:00:00Z',
      isEnabled: true,
    };

    await wrapper.vm.handleConfirm();

    const handlers = hoisted.confirmWithHandlersMock.mock.calls[0][0] as {
      delete: () => Promise<boolean>;
      runNow: () => Promise<void>;
    };

    hoisted.deleteIntegrationMock.mockResolvedValueOnce(true);
    await expect(handlers.delete()).resolves.toBe(true);

    hoisted.deleteIntegrationMock.mockRejectedValueOnce(new Error('delete failed'));
    await expect(handlers.delete()).resolves.toBe(false);

    await handlers.runNow();
    expect(hoisted.triggerJobExecutionMock).toHaveBeenCalledWith('arc-5', 'Arc Five');
  });

  it('passes enable and disable handlers to confirmWithHandlers', async () => {
    const wrapper = mountPage() as any;
    await nextTick();
    wrapper.vm.pendingIntegration = {
      id: 'arc-6',
      name: 'Arc Six',
      createdBy: 'maya',
      createdDate: '2024-01-01T00:00:00Z',
      isEnabled: false,
    };

    await wrapper.vm.handleConfirm();

    const handlers = hoisted.confirmWithHandlersMock.mock.calls[0][0] as {
      enable: () => Promise<void>;
      disable: () => Promise<void>;
    };

    hoisted.toggleIntegrationStatusMock.mockResolvedValueOnce(true);
    await handlers.enable();

    hoisted.toggleIntegrationStatusMock.mockResolvedValueOnce(false);
    await handlers.disable();

    expect(hoisted.toggleIntegrationStatusMock).toHaveBeenNthCalledWith(1, 'arc-6', false);
    expect(hoisted.toggleIntegrationStatusMock).toHaveBeenNthCalledWith(2, 'arc-6', false);
  });

  it('updates a local integration when toggle returns a boolean and refetches when it does not', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-7',
        name: 'Arc Seven',
        createdBy: 'maya',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: false,
      },
    ]);
    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    hoisted.toggleIntegrationStatusMock.mockResolvedValueOnce(true);
    await wrapper.vm.enableDisableIntegration('arc-7', false);
    expect(wrapper.vm.integrations[0].isEnabled).toBe(true);

    hoisted.toggleIntegrationStatusMock.mockResolvedValueOnce('non-boolean');
    await wrapper.vm.enableDisableIntegration('arc-7', true);
    expect(hoisted.getAllIntegrationsMock).toHaveBeenCalledTimes(2);
  });

  it('leaves local integrations unchanged when the toggled integration is not found', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-known',
        name: 'Arc Known',
        createdBy: 'maya',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: false,
      },
    ]);

    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    hoisted.toggleIntegrationStatusMock.mockResolvedValueOnce(true);
    await wrapper.vm.enableDisableIntegration('arc-missing', false);

    expect(wrapper.vm.integrations[0].isEnabled).toBe(false);
    expect(hoisted.getAllIntegrationsMock).toHaveBeenCalledTimes(1);
  });

  it('removes an integration only when delete succeeds', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-7',
        name: 'Arc Seven',
        createdBy: 'maya',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
      },
    ]);
    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    hoisted.deleteIntegrationMock.mockResolvedValueOnce(false);
    await wrapper.vm.deleteIntegrationAction('arc-7');
    expect(wrapper.vm.integrations).toHaveLength(1);

    hoisted.deleteIntegrationMock.mockResolvedValueOnce(true);
    await wrapper.vm.deleteIntegrationAction('arc-7');
    expect(wrapper.vm.integrations).toHaveLength(0);
  });

  it('respects pagination boundaries and clears pending integration on local dialog close', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue(
      Array.from({ length: 7 }, (_, index) => ({
        id: `arc-${index}`,
        name: `Arc ${index}`,
        createdBy: 'user',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
      }))
    );
    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.prevPage();
    expect(wrapper.vm.currentPage).toBe(1);
    wrapper.vm.nextPage();
    expect(wrapper.vm.currentPage).toBe(2);
    wrapper.vm.nextPage();
    expect(wrapper.vm.currentPage).toBe(2);

    wrapper.vm.pendingIntegration = { id: 'arc-1' };
    wrapper.vm.closeDialogLocal();
    expect(wrapper.vm.pendingIntegration).toBeNull();
    expect(hoisted.closeDialogMock).toHaveBeenCalled();
  });

  it('sorts integrations by name, status, created date, and leaves lastTrigger unchanged', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-b',
        name: 'Beta',
        createdBy: 'zoe',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: false,
      },
      {
        id: 'arc-a',
        name: 'Alpha',
        createdBy: 'amy',
        createdDate: '2024-02-01T00:00:00Z',
        isEnabled: true,
      },
    ]);

    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.sortBy = 'name';
    await nextTick();
    expect(wrapper.vm.sortedIntegrations.map((item: { id: string }) => item.id)).toEqual([
      'arc-a',
      'arc-b',
    ]);

    wrapper.vm.sortBy = 'isEnabled';
    await nextTick();
    expect(wrapper.vm.sortedIntegrations.map((item: { id: string }) => item.id)).toEqual([
      'arc-a',
      'arc-b',
    ]);

    wrapper.vm.sortBy = 'createdDate';
    await nextTick();
    expect(wrapper.vm.sortedIntegrations.map((item: { id: string }) => item.id)).toEqual([
      'arc-a',
      'arc-b',
    ]);

    wrapper.vm.sortBy = 'lastTrigger';
    await nextTick();
    expect(wrapper.vm.sortedIntegrations.map((item: { id: string }) => item.id)).toEqual([
      'arc-b',
      'arc-a',
    ]);
  });

  it('updates the page state from toolbar events and resets page size through the setter', async () => {
    const wrapper = mountPage() as any;
    await nextTick();

    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('setViewMode', 'list');
    toolbar.vm.$emit('update:pageSize', 12);
    await nextTick();

    expect(wrapper.vm.viewMode).toBe('list');
    expect(wrapper.vm.pageSize).toBe(12);
  });

  it('renders search-aware empty-state messaging', async () => {
    const wrapper = mountPage() as any;
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    expect(wrapper.find('.empty-title').text()).toBe('No ArcGIS Integration Found');
    expect(wrapper.find('.empty-subtitle').text()).toBe('Create Your First ArcGIS Integration');

    wrapper.vm.search = 'roads';
    await nextTick();

    expect(wrapper.find('.empty-title').text()).toBe('No matching integrations found');
    expect(wrapper.find('.empty-subtitle').text()).toContain('No integrations match "roads"');
  });

  it('opens details without showing an error when router navigation succeeds', async () => {
    hoisted.getAllIntegrationsMock.mockResolvedValue([
      {
        id: 'arc-success',
        name: 'Arc Success',
        createdBy: 'alice',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
      },
    ]);

    const wrapper = mountPage();
    await nextTick();
    await new Promise(r => setTimeout(r, 0));

    wrapper.findComponent({ name: 'ArcGISIntegrationCard' }).vm.$emit('open', 'arc-success');
    await new Promise(r => setTimeout(r, 0));

    expect(hoisted.router.push).toHaveBeenCalled();
    expect(hoisted.showErrorMock).not.toHaveBeenCalled();
  });
});
