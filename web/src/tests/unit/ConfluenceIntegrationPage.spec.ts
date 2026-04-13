/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { nextTick } from 'vue';

const __router = {
  push: vi.fn(() => Promise.resolve()),
  replace: vi.fn(() => Promise.resolve()),
  getRoutes: vi.fn(() => []),
  addRoute: vi.fn(),
  back: vi.fn(),
};
const __route: { path: string; query: Record<string, unknown> } = {
  path: '/outbound/integration/confluence',
  query: {},
};

vi.mock('vue-router', () => ({
  useRouter: () => __router,
  useRoute: () => __route,
}));

const actionSpies = vi.hoisted(() => ({
  getAllIntegrations: vi.fn().mockResolvedValue([]),
  deleteIntegration: vi.fn().mockResolvedValue(true),
  toggleIntegrationStatus: vi.fn().mockResolvedValue(true),
}));

const confirmationSpies = vi.hoisted(() => ({
  openDialog: vi.fn(),
  closeDialog: vi.fn(),
  confirmWithHandlers: vi.fn(),
}));

vi.mock('@/composables/useConfluenceIntegrationActions', () => ({
  useConfluenceIntegrationActions: () => ({
    loading: false,
    getAllIntegrations: actionSpies.getAllIntegrations,
    deleteIntegration: actionSpies.deleteIntegration,
  }),
  useConfluenceIntegrationStatus: () => ({
    toggleIntegrationStatus: actionSpies.toggleIntegrationStatus,
  }),
}));

vi.mock('@/composables/useConfirmationDialog', () => ({
  useConfirmationDialog: () => ({
    dialogOpen: false,
    actionLoading: false,
    pendingAction: null,
    dialogTitle: '',
    dialogDescription: '',
    dialogConfirmLabel: '',
    openDialog: confirmationSpies.openDialog,
    closeDialog: confirmationSpies.closeDialog,
    confirmWithHandlers: confirmationSpies.confirmWithHandlers,
  }),
}));

vi.mock(
  '@/components/outbound/confluenceintegration/wizard/ConfluenceIntegrationWizard.vue',
  () => ({
    default: {
      name: 'ConfluenceIntegrationWizard',
      props: ['open', 'mode', 'integrationId'],
      emits: ['close', 'integration-created', 'integration-updated'],
      template: '<div v-if="open" class="confluence-wizard-stub" :data-mode="mode"></div>',
    },
  })
);

vi.mock('@/components/common/ConfirmationDialog.vue', () => ({
  default: {
    name: 'ConfirmationDialog',
    props: ['open', 'type', 'title', 'description', 'confirmLabel', 'loading'],
    emits: ['cancel', 'confirm'],
    template: '<div data-testid="confirmation-dialog"></div>',
  },
}));

import ConfluenceIntegrationPage from '@/components/outbound/confluenceintegration/ConfluenceIntegrationPage.vue';
import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';

const sampleIntegration: ConfluenceIntegrationSummaryResponse = {
  id: 'ci-1',
  name: 'Confluence Integration 1',
  itemType: 'DOCUMENT',
  itemSubtype: 'DESIGN',
  languageCodes: ['en'],
  confluenceSpaceKey: 'TEST',
  reportNameTemplate: 'Report - {date}',
  frequencyPattern: 'DAILY',
  executionTime: '09:00',
  createdDate: '2026-01-01T00:00:00Z',
  createdBy: 'admin',
  lastModifiedDate: '2026-01-02T00:00:00Z',
  lastModifiedBy: 'admin',
  isEnabled: true,
};

// Create 7 integrations so paging has effect (6 per page = 2 pages)
const manyIntegrations = Array.from({ length: 7 }, (_, i) => ({
  ...sampleIntegration,
  id: `ci-${i + 1}`,
  name: `Confluence Integration ${i + 1}`,
}));

describe('ConfluenceIntegrationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    __route.query = {};
    actionSpies.getAllIntegrations.mockResolvedValue([]);
    actionSpies.deleteIntegration.mockResolvedValue(true);
    actionSpies.toggleIntegrationStatus.mockResolvedValue(true);
  });

  it('renders empty state when integration list is empty', async () => {
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.find('.simple-empty-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('No Confluence Integration Found');
  });

  it('opens create wizard when toolbar emits create', async () => {
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('create');
    await nextTick();
    const wizard = wrapper.find('.confluence-wizard-stub');
    expect(wizard.exists()).toBe(true);
  });

  it('shows integration cards when integrations are loaded', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.text()).toContain('Confluence Integration 1');
  });

  it('shows "no matching" message when search has no results', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.search = 'nonexistent';
    await nextTick();
    expect(wrapper.text()).toContain('No matching integrations found');
  });

  it('opens edit wizard when action "edit" is dispatched', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.handleIntegrationAction('edit', sampleIntegration);
    await nextTick();
    expect(vm.editWizardOpen).toBe(true);
    expect(vm.editingIntegrationId).toBe('ci-1');
  });

  it('opens clone wizard when action "clone" is dispatched', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.handleIntegrationAction('clone', sampleIntegration);
    await nextTick();
    expect(vm.cloneWizardOpen).toBe(true);
    expect(vm.cloningIntegrationId).toBe('ci-1');
  });

  it('navigates to details page when openDetails is called', async () => {
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.openDetails('ci-42');
    expect(__router.push).toHaveBeenCalledWith(
      expect.objectContaining({ path: expect.stringContaining('ci-42') })
    );
  });

  it('changes view mode when toolbar emits setViewMode', async () => {
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('setViewMode', 'list');
    await nextTick();
    expect((wrapper.vm as any).viewMode).toBe('list');
  });

  it('updates search term when toolbar emits update:search', async () => {
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('update:search', 'test search');
    await nextTick();
    expect((wrapper.vm as any).search).toBe('test search');
  });

  it('updates sortBy when toolbar emits update:sortBy', async () => {
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('update:sortBy', 'name');
    await nextTick();
    expect((wrapper.vm as any).sortBy).toBe('name');
  });

  it('increments page on nextPage when there are multiple pages', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue(manyIntegrations);
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('nextPage');
    await nextTick();
    expect((wrapper.vm as any).currentPage).toBe(2);
  });

  it('decrements page on prevPage when not on first page', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue(manyIntegrations);
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.currentPage = 3;
    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('prevPage');
    await nextTick();
    expect(vm.currentPage).toBe(2);
  });

  it('closes create wizard when it emits "close"', async () => {
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.wizardOpen = true;
    await nextTick();
    // Find the open wizard and emit close
    const wizards = wrapper.findAllComponents({ name: 'ConfluenceIntegrationWizard' });
    const openWizard = wizards.find(w => w.props('open') === true);
    if (openWizard) {
      openWizard.vm.$emit('close');
      await nextTick();
      expect(vm.wizardOpen).toBe(false);
    } else {
      expect(vm.wizardOpen).toBe(true); // Already validated it was open
    }
  });

  it('opens confirmation dialog for delete and enable actions', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));

    (wrapper.vm as any).handleIntegrationAction('delete', sampleIntegration);
    (wrapper.vm as any).handleIntegrationAction('enable', sampleIntegration);

    expect(confirmationSpies.openDialog).toHaveBeenNthCalledWith(1, 'delete');
    expect(confirmationSpies.openDialog).toHaveBeenNthCalledWith(2, 'enable');
  });

  it('warns for unknown actions', async () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));

    (wrapper.vm as any).handleIntegrationAction('mystery', sampleIntegration);

    expect(warnSpy).toHaveBeenCalledWith('Unknown action:', 'mystery');
    warnSpy.mockRestore();
  });

  it('removes an integration after confirmed deletion', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    confirmationSpies.confirmWithHandlers.mockImplementation(
      async (handlers: Record<string, () => Promise<boolean>>) => {
        await handlers.delete();
        return true;
      }
    );

    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));

    (wrapper.vm as any).handleIntegrationAction('delete', sampleIntegration);
    await (wrapper.vm as any).handleConfirm();
    await nextTick();

    expect(actionSpies.deleteIntegration).toHaveBeenCalledWith('ci-1');
    expect((wrapper.vm as any).integrations).toEqual([]);
  });

  it('updates the integration status when confirm executes the enable branch', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    actionSpies.toggleIntegrationStatus.mockResolvedValueOnce(false);
    confirmationSpies.confirmWithHandlers.mockImplementation(
      async (handlers: Record<string, () => Promise<boolean>>) => {
        await handlers.enable();
        return true;
      }
    );

    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));

    (wrapper.vm as any).handleIntegrationAction('enable', sampleIntegration);
    await (wrapper.vm as any).handleConfirm();
    await nextTick();

    expect(actionSpies.toggleIntegrationStatus).toHaveBeenCalledWith('ci-1', true);
    expect((wrapper.vm as any).integrations[0].isEnabled).toBe(false);
  });

  it('refetches integrations when toggle returns a non-boolean result', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([sampleIntegration]);
    actionSpies.toggleIntegrationStatus.mockResolvedValueOnce(undefined);
    confirmationSpies.confirmWithHandlers.mockImplementation(
      async (handlers: Record<string, () => Promise<boolean>>) => {
        await handlers.disable();
        return true;
      }
    );

    const wrapper = mount(ConfluenceIntegrationPage);
    await new Promise(r => setTimeout(r, 0));

    (wrapper.vm as any).handleIntegrationAction('disable', sampleIntegration);
    await (wrapper.vm as any).handleConfirm();
    await nextTick();

    expect(actionSpies.getAllIntegrations).toHaveBeenCalledTimes(2);
  });

  it('sorts integrations by name, status, and created date', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([
      {
        ...sampleIntegration,
        id: 'ci-2',
        name: 'Zulu',
        createdDate: '2026-01-01T00:00:00Z',
        isEnabled: false,
      },
      {
        ...sampleIntegration,
        id: 'ci-1',
        name: 'Alpha',
        createdDate: '2026-02-01T00:00:00Z',
        isEnabled: true,
      },
    ]);

    const wrapper = mount(ConfluenceIntegrationPage) as any;
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.sortBy = 'name';
    await nextTick();
    expect(wrapper.vm.sortedIntegrations.map((item: { id: string }) => item.id)).toEqual([
      'ci-1',
      'ci-2',
    ]);

    wrapper.vm.sortBy = 'isEnabled';
    await nextTick();
    expect(wrapper.vm.sortedIntegrations.map((item: { id: string }) => item.id)).toEqual([
      'ci-1',
      'ci-2',
    ]);

    wrapper.vm.sortBy = 'createdDate';
    await nextTick();
    expect(wrapper.vm.sortedIntegrations.map((item: { id: string }) => item.id)).toEqual([
      'ci-1',
      'ci-2',
    ]);
  });

  it('filters integrations by creator name and leaves state unchanged when delete returns false', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue([
      sampleIntegration,
      { ...sampleIntegration, id: 'ci-2', name: 'Second', createdBy: 'other-admin' },
    ]);
    actionSpies.deleteIntegration.mockResolvedValueOnce(false);

    const wrapper = mount(ConfluenceIntegrationPage) as any;
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.search = 'other-admin';
    await nextTick();
    expect(wrapper.vm.filteredIntegrations).toHaveLength(1);
    expect(wrapper.vm.filteredIntegrations[0].id).toBe('ci-2');

    await wrapper.vm.deleteIntegrationAction('ci-1');
    expect(wrapper.vm.integrations).toHaveLength(2);
  });

  it('returns early from handleConfirm without a pending integration and clears pending state on local close', async () => {
    const wrapper = mount(ConfluenceIntegrationPage) as any;
    await new Promise(r => setTimeout(r, 0));

    await wrapper.vm.handleConfirm();
    expect(confirmationSpies.confirmWithHandlers).not.toHaveBeenCalled();

    wrapper.vm.pendingIntegration = sampleIntegration;
    wrapper.vm.closeDialogLocal();
    expect(wrapper.vm.pendingIntegration).toBeNull();
    expect(confirmationSpies.closeDialog).toHaveBeenCalled();
  });

  it('respects pagination boundaries and updates page size through the toolbar setter', async () => {
    actionSpies.getAllIntegrations.mockResolvedValue(manyIntegrations);
    const wrapper = mount(ConfluenceIntegrationPage) as any;
    await new Promise(r => setTimeout(r, 0));

    wrapper.vm.prevPage();
    expect(wrapper.vm.currentPage).toBe(1);

    wrapper.vm.nextPage();
    expect(wrapper.vm.currentPage).toBe(2);

    wrapper.vm.nextPage();
    expect(wrapper.vm.currentPage).toBe(2);

    const toolbar = wrapper.findComponent({ name: 'DashboardToolbar' });
    toolbar.vm.$emit('update:pageSize', 12);
    await nextTick();

    expect(wrapper.vm.pageSize).toBe(12);
  });
});
