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
});
