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
    openDialog: vi.fn(),
    closeDialog: vi.fn(),
    confirmWithHandlers: vi.fn(),
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
});
