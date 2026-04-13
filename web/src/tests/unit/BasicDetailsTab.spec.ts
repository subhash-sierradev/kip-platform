/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import BasicDetailsTab from '@/components/outbound/arcgisintegration/details/BasicDetailsTab.vue';

const routerSpies = vi.hoisted(() => ({
  back: vi.fn(),
}));

const toastSpies = vi.hoisted(() => ({
  showInfo: vi.fn(),
  showError: vi.fn(),
}));

const actionSpies = vi.hoisted(() => ({
  deleteIntegration: vi.fn(async () => true),
  toggleIntegrationStatus: vi.fn(async () => true),
  triggerJobExecution: vi.fn().mockResolvedValue(undefined),
}));

const dialogSpies = vi.hoisted(() => ({
  openDialog: vi.fn(),
  closeDialog: vi.fn(),
  confirmWithHandlers: vi.fn(),
}));

const kwDocSpies = vi.hoisted(() => ({
  getDynamicDocuments: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRouter: () => routerSpies,
}));

vi.mock('devextreme-vue', () => ({
  DxButton: { props: ['text'], template: '<button><slot />{{ text }}</button>' },
}));

vi.mock('@/components/common/ConfirmationDialog.vue', () => ({
  default: { template: '<div />' },
}));

// Stub EntityManageActions to expose buttons matching legacy selectors
vi.mock('@/components/common/EntityManageActions.vue', () => ({
  default: {
    props: ['actions', 'title'],
    emits: ['action'],
    template: `
      <div class="actions-stub">
        <button
          class="action-button toggle-btn"
          @click="$emit('action', 'toggle')"
        >{{ (actions || []).find(x => x.id === 'toggle')?.label || 'Toggle' }}</button>
        <button
          class="action-button clone-btn"
          @click="$emit('action', 'clone'); $parent.$emit('refresh')"
        >{{ (actions || []).find(x => x.id === 'clone')?.label || 'Clone' }}</button>
      </div>
    `,
  },
}));

vi.mock('@/composables/useArcGISIntegrationActions', () => ({
  useArcGISIntegrationActions: () => ({
    deleteIntegration: actionSpies.deleteIntegration,
    cloneIntegration: vi.fn(async () => 'new-id'),
  }),
  useArcGISIntegrationEditor: () => ({
    loading: false,
    updateIntegrationFromWizard: vi.fn().mockResolvedValue(true),
  }),
  useArcGISIntegrationStatus: () => ({
    toggleIntegrationStatus: actionSpies.toggleIntegrationStatus,
  }),
  useArcGISIntegrationTrigger: () => ({
    triggerJobExecution: actionSpies.triggerJobExecution,
  }),
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => toastSpies,
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
}));

vi.mock('@/api/services/KwIntegrationService', () => ({
  KwDocService: kwDocSpies,
}));

describe('BasicDetailsTab', () => {
  const integration = {
    name: 'Roads Integration',
    description: 'Sync roads from ArcGIS',
    createdBy: 'Alice',
    createdDate: '2024-01-01T00:00:00Z',
    lastModifiedBy: 'Bob',
    lastModifiedDate: '2024-01-02T00:00:00Z',
    itemType: 'Document',
    itemSubtypeLabel: 'Subtype-A',
    dynamicDocumentType: 'DynamicType',
    dynamicDocumentTypeLabel: 'DynamicType',
    isEnabled: true,
  } as any;

  beforeEach(() => {
    vi.clearAllMocks();
    kwDocSpies.getDynamicDocuments.mockResolvedValue([]);
  });

  it('renders integration info and configuration', () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    expect(wrapper.text()).toContain('Roads Integration');
    expect(wrapper.text()).toContain('Sync roads from ArcGIS');
    expect(wrapper.text()).toContain('Alice');
    expect(wrapper.text()).toContain('Bob');
    // Document type shows itemType or dynamicDocumentType
    expect(wrapper.text()).toContain('Document');
    expect(wrapper.text()).toContain('Subtype-A');
    expect(wrapper.text()).toContain('DynamicType');

    // Toggle button reflects enabled state
    const toggleBtn = wrapper.find('button.action-button.toggle-btn');
    expect(toggleBtn.text()).toContain('Disable Integration');
  });

  it('shows dynamicDocumentType when label is null', () => {
    const wrapper = mount(BasicDetailsTab, {
      props: {
        integrationData: {
          ...integration,
          dynamicDocumentType: 'A Test for today',
          dynamicDocumentTypeLabel: null,
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    expect(wrapper.text()).toContain('Dynamic Document Type');
    expect(wrapper.text()).toContain('A Test for today');
  });

  it('emits clone-requested on clone action', async () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });
    const cloneBtn = wrapper.find('button.action-button.clone-btn');
    await cloneBtn.trigger('click');
    // Verify clone-requested emit was emitted
    expect(wrapper.emitted('clone-requested')).toBeTruthy();
  });

  it('shows empty state when no data and not loading', () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: null, integrationId: 'abc', loading: false },
    });
    expect(wrapper.find('.empty-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('No Integration Data Found');
  });

  it('uses resolved dynamic document title when backend label is missing', async () => {
    kwDocSpies.getDynamicDocuments.mockResolvedValue([{ id: 'doc-22', title: 'Resolved Title' }]);

    const wrapper = mount(BasicDetailsTab, {
      props: {
        integrationData: {
          ...integration,
          dynamicDocumentType: 'doc-22',
          dynamicDocumentTypeLabel: null,
          itemSubtype: 'DOCUMENT_DRAFT_DYNAMIC',
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await wrapper.vm.$nextTick();
    await Promise.resolve();

    expect(kwDocSpies.getDynamicDocuments).toHaveBeenCalledWith(
      'DOCUMENT',
      'DOCUMENT_DRAFT_DYNAMIC'
    );
    expect(wrapper.text()).toContain('Resolved Title');
  });

  it('falls back to raw dynamic document type when lookup fails', async () => {
    kwDocSpies.getDynamicDocuments.mockRejectedValue(new Error('lookup failed'));

    const wrapper = mount(BasicDetailsTab, {
      props: {
        integrationData: {
          ...integration,
          dynamicDocumentType: 'raw-doc-id',
          dynamicDocumentTypeLabel: null,
          itemSubtype: 'DOCUMENT_FINAL_DYNAMIC',
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await wrapper.vm.$nextTick();
    await Promise.resolve();

    expect(wrapper.text()).toContain('raw-doc-id');
  });

  it('does not resolve dynamic label for unsupported subtype', async () => {
    mount(BasicDetailsTab, {
      props: {
        integrationData: {
          ...integration,
          dynamicDocumentType: 'raw-doc-id',
          dynamicDocumentTypeLabel: null,
          itemSubtype: 'STATIC_SUBTYPE',
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await Promise.resolve();
    expect(kwDocSpies.getDynamicDocuments).not.toHaveBeenCalled();
  });

  it('opens the correct confirmation dialogs for run, toggle, and delete', async () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });
    const vm = wrapper.vm as any;

    vm.handleActionClick('run');
    vm.handleActionClick('toggle');
    vm.handleActionClick('delete');

    expect(dialogSpies.openDialog).toHaveBeenNthCalledWith(1, 'runNow');
    expect(dialogSpies.openDialog).toHaveBeenNthCalledWith(2, 'disable');
    expect(dialogSpies.openDialog).toHaveBeenNthCalledWith(3, 'delete');
  });

  it('emits edit-requested when edit action is clicked', () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    (wrapper.vm as any).handleActionClick('edit');

    expect(wrapper.emitted('edit-requested')).toBeTruthy();
  });

  it('calls trigger execution with integration id and name', async () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    await (wrapper.vm as any).handleTriggerIntegration();

    expect(actionSpies.triggerJobExecution).toHaveBeenCalledWith('abc', 'Roads Integration');
  });

  it('shows an error and returns false when toggling without an integration id', async () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: '', loading: false },
    });

    await expect((wrapper.vm as any).handleToggleIntegration()).resolves.toBe(false);
    expect(toastSpies.showError).toHaveBeenCalledWith('Integration id missing');
  });

  it('emits status-updated and updates local state when toggle succeeds', async () => {
    actionSpies.toggleIntegrationStatus.mockResolvedValueOnce(false);

    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    await expect((wrapper.vm as any).handleToggleIntegration()).resolves.toBe(true);
    expect(actionSpies.toggleIntegrationStatus).toHaveBeenCalledWith('abc', true);
    expect(wrapper.emitted('status-updated')).toEqual([[false]]);
    expect((wrapper.vm as any).manageActions.find((item: any) => item.id === 'toggle').label).toBe(
      'Enable Integration'
    );
  });

  it('returns false when toggle action returns null', async () => {
    actionSpies.toggleIntegrationStatus.mockResolvedValueOnce(null as any);

    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    await expect((wrapper.vm as any).handleToggleIntegration()).resolves.toBe(false);
  });

  it('shows an error and returns false when deleting without an integration id', async () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: '', loading: false },
    });

    await expect((wrapper.vm as any).handleDeleteIntegration()).resolves.toBe(false);
    expect(toastSpies.showError).toHaveBeenCalledWith('Integration id missing');
  });

  it('navigates back after successful delete', async () => {
    actionSpies.deleteIntegration.mockResolvedValueOnce(true);

    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    await expect((wrapper.vm as any).handleDeleteIntegration()).resolves.toBe(true);
    expect(routerSpies.back).toHaveBeenCalled();
  });

  it('does not navigate when delete returns false', async () => {
    actionSpies.deleteIntegration.mockResolvedValueOnce(false);

    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    await expect((wrapper.vm as any).handleDeleteIntegration()).resolves.toBe(false);
    expect(routerSpies.back).not.toHaveBeenCalled();
  });

  it('delegates confirm handling to the dialog handler map', async () => {
    const wrapper = mount(BasicDetailsTab, {
      props: { integrationData: integration, integrationId: 'abc', loading: false },
    });

    await (wrapper.vm as any).handleConfirm();

    expect(dialogSpies.confirmWithHandlers).toHaveBeenCalledTimes(1);
    const handlers = dialogSpies.confirmWithHandlers.mock.calls[0]?.[0];
    expect(Object.keys(handlers)).toEqual(
      expect.arrayContaining(['enable', 'disable', 'delete', 'runNow'])
    );
  });
});
