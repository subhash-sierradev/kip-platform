/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const toastSpies = vi.hoisted(() => ({
  showSuccess: vi.fn(),
  showError: vi.fn(),
  showWarning: vi.fn(),
}));

const actionSpies = vi.hoisted(() => ({
  deleteIntegration: vi.fn(),
  toggleIntegrationStatus: vi.fn(),
}));

const routerSpies = vi.hoisted(() => ({
  back: vi.fn(),
  push: vi.fn(),
}));

vi.mock('@/composables/useConfluenceIntegrationActions', () => ({
  useConfluenceIntegrationActions: () => ({
    deleteIntegration: actionSpies.deleteIntegration,
  }),
  useConfluenceIntegrationStatus: () => ({
    toggleIntegrationStatus: actionSpies.toggleIntegrationStatus,
  }),
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => toastSpies,
}));

const dialogSpies = vi.hoisted(() => ({
  openDialog: vi.fn(),
  closeDialog: vi.fn(),
  confirmWithHandlers: vi.fn(),
}));

vi.mock('@/composables/useConfirmationDialog', () => ({
  useConfirmationDialog: () => ({
    dialogOpen: false,
    actionLoading: false,
    pendingAction: null,
    dialogTitle: '',
    dialogDescription: '',
    dialogConfirmLabel: '',
    openDialog: dialogSpies.openDialog,
    closeDialog: dialogSpies.closeDialog,
    confirmWithHandlers: dialogSpies.confirmWithHandlers,
  }),
  createDefaultDialogConfig: () => ({}),
}));

vi.mock('vue-router', () => ({
  useRouter: () => routerSpies,
}));

vi.mock('@/components/common/ConfirmationDialog.vue', () => ({
  default: {
    name: 'ConfirmationDialog',
    props: ['open', 'type', 'title', 'description', 'confirmLabel', 'loading'],
    emits: ['cancel', 'confirm'],
    template: '<div data-testid="confirmation-dialog"></div>',
  },
}));

import ConfluenceOverviewTab from '@/components/outbound/confluenceintegration/details/ConfluenceOverviewTab.vue';
import type { ConfluenceIntegrationResponse } from '@/api/models/ConfluenceIntegrationResponse';

function makeIntegration(
  overrides: Partial<ConfluenceIntegrationResponse> = {}
): ConfluenceIntegrationResponse {
  return {
    id: 'ci-1',
    name: 'My Confluence',
    normalizedName: 'my-confluence',
    itemType: 'DOCUMENT',
    itemSubtype: 'DESIGN',
    languages: [],
    languageCodes: ['en'],
    confluenceSpaceKey: 'TEST',
    reportNameTemplate: 'Report',
    includeTableOfContents: true,
    connectionId: 'conn-1',
    schedule: {} as any,
    tenantId: 'tenant-1',
    isEnabled: true,
    createdBy: 'admin',
    createdDate: '2026-01-01T00:00:00Z',
    lastModifiedBy: 'admin',
    lastModifiedDate: '2026-01-02T00:00:00Z',
    version: 1,
    ...overrides,
  };
}

describe('ConfluenceOverviewTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders integration name', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });
    expect(wrapper.text()).toContain('My Confluence');
  });

  it('renders createdBy info', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ createdBy: 'john.doe' }),
        integrationId: 'ci-1',
        loading: false,
      },
    });
    expect(wrapper.text()).toContain('john.doe');
  });

  it('shows N/A when name is null/undefined', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ name: '' }),
        integrationId: 'ci-1',
        loading: false,
      },
    });
    expect(wrapper.text()).toContain('N/A');
  });

  it('shows empty state when integrationData is null and not loading', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: null, integrationId: 'ci-1', loading: false },
    });
    expect(wrapper.find('.empty-state').exists()).toBe(true);
  });

  it('does not show empty state when loading', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: null, integrationId: 'ci-1', loading: true },
    });
    expect(wrapper.find('.empty-state').exists()).toBe(false);
  });

  it('emits edit-requested when handleActionClick is called with "edit"', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });
    const vm = wrapper.vm as any;
    vm.handleActionClick('edit');
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted('edit-requested')).toBeTruthy();
  });

  it('emits clone-requested when handleActionClick is called with "clone"', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });
    const vm = wrapper.vm as any;
    vm.handleActionClick('clone');
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted('clone-requested')).toBeTruthy();
  });

  it('triggers openDialog with "delete" when handleActionClick is called with "delete"', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });
    const vm = wrapper.vm as any;
    vm.handleActionClick('delete');
    expect(dialogSpies.openDialog).toHaveBeenCalledWith('delete');
  });

  it('triggers openDialog with "disable" when toggle is called on enabled integration', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ isEnabled: true }),
        integrationId: 'ci-1',
        loading: false,
      },
    });
    const vm = wrapper.vm as any;
    vm.handleActionClick('toggle');
    expect(dialogSpies.openDialog).toHaveBeenCalledWith('disable');
  });

  it('triggers openDialog with "enable" when toggle is called on disabled integration', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ isEnabled: false }),
        integrationId: 'ci-1',
        loading: false,
      },
    });
    const vm = wrapper.vm as any;
    vm.handleActionClick('toggle');
    expect(dialogSpies.openDialog).toHaveBeenCalledWith('enable');
  });

  it('does nothing for unknown action id', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });
    const vm = wrapper.vm as any;
    expect(() => vm.handleActionClick('unknown')).not.toThrow();
  });

  it('manageActions shows "Disable Integration" when enabled', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ isEnabled: true }),
        integrationId: 'ci-1',
        loading: false,
      },
    });
    const vm = wrapper.vm as any;
    const toggleAction = vm.manageActions.find((a: any) => a.id === 'toggle');
    expect(toggleAction?.label).toBe('Disable Integration');
  });

  it('manageActions shows "Enable Integration" when disabled', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ isEnabled: false }),
        integrationId: 'ci-1',
        loading: false,
      },
    });
    const vm = wrapper.vm as any;
    const toggleAction = vm.manageActions.find((a: any) => a.id === 'toggle');
    expect(toggleAction?.label).toBe('Enable Integration');
  });

  it('shows confirmation dialog element', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });
    expect(wrapper.find('[data-testid="confirmation-dialog"]').exists()).toBe(true);
  });

  it('dynamicDocumentType is shown when present', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({
          dynamicDocumentType: 'type-x',
          dynamicDocumentTypeLabel: 'Type X',
        }),
        integrationId: 'ci-1',
        loading: false,
      },
    });
    expect(wrapper.text()).toContain('Type X');
  });

  it('falls back to raw subtype when subtype label is missing', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ itemSubtypeLabel: '', itemSubtype: 'SPACE_PAGE' }),
        integrationId: 'ci-1',
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('SPACE_PAGE');
  });

  it('shows placeholder description when none is provided', () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ description: '' } as any),
        integrationId: 'ci-1',
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('No description provided');
  });

  it('handleConfirm delegates to dialog handlers', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });

    await (wrapper.vm as any).handleConfirm();

    expect(dialogSpies.confirmWithHandlers).toHaveBeenCalledTimes(1);
    const handlers = dialogSpies.confirmWithHandlers.mock.calls[0]?.[0];
    expect(Object.keys(handlers)).toEqual(expect.arrayContaining(['enable', 'disable', 'delete']));
  });

  it('shows error and returns false when toggling without an id', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: '', loading: false },
    });

    await expect((wrapper.vm as any).handleToggleIntegration()).resolves.toBe(false);
    expect(toastSpies.showError).toHaveBeenCalledWith('Integration id missing');
  });

  it('updates local enabled state and emits status-updated on toggle success', async () => {
    actionSpies.toggleIntegrationStatus.mockResolvedValueOnce(false);
    const wrapper = mount(ConfluenceOverviewTab, {
      props: {
        integrationData: makeIntegration({ isEnabled: true }),
        integrationId: 'ci-1',
        loading: false,
      },
    });

    await expect((wrapper.vm as any).handleToggleIntegration()).resolves.toBe(true);
    expect(actionSpies.toggleIntegrationStatus).toHaveBeenCalledWith('ci-1', true);
    expect(wrapper.emitted('status-updated')).toEqual([[false]]);
    expect((wrapper.vm as any).manageActions.find((item: any) => item.id === 'toggle').label).toBe(
      'Enable Integration'
    );
  });

  it('returns false when status toggle returns null', async () => {
    actionSpies.toggleIntegrationStatus.mockResolvedValueOnce(null);
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });

    await expect((wrapper.vm as any).handleToggleIntegration()).resolves.toBe(false);
  });

  it('shows error and returns false when deleting without an id', async () => {
    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: '', loading: false },
    });

    await expect((wrapper.vm as any).handleDeleteIntegration()).resolves.toBe(false);
    expect(toastSpies.showError).toHaveBeenCalledWith('Integration id missing');
  });

  it('navigates back on successful delete and stays put on failure', async () => {
    actionSpies.deleteIntegration.mockResolvedValueOnce(true).mockResolvedValueOnce(false);

    const wrapper = mount(ConfluenceOverviewTab, {
      props: { integrationData: makeIntegration(), integrationId: 'ci-1', loading: false },
    });

    await expect((wrapper.vm as any).handleDeleteIntegration()).resolves.toBe(true);
    await expect((wrapper.vm as any).handleDeleteIntegration()).resolves.toBe(false);

    expect(routerSpies.back).toHaveBeenCalledTimes(1);
  });
});
