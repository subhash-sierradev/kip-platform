/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const routerSpies = vi.hoisted(() => ({
  back: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  route: { params: { id: 'ci-1' }, query: { search: 'abc' } },
}));

vi.mock('@/components/common/StandardDetailPageLayout.vue', () => ({
  default: {
    name: 'StandardDetailPageLayout',
    props: [
      'title',
      'loading',
      'error',
      'status',
      'version',
      'activeTab',
      'tabs',
      'showBackButton',
      'icon',
      'componentProps',
      'componentEvents',
    ],
    emits: ['back', 'retry', 'tab-change', 'status-updated', 'refresh'],
    template: `<div class="standard-layout-stub">
      <span data-testid="title">{{ title }}</span>
      <span v-if="error" data-testid="error">{{ error }}</span>
    </div>`,
  },
}));

vi.mock(
  '@/components/outbound/confluenceintegration/wizard/ConfluenceIntegrationWizard.vue',
  () => ({
    default: {
      name: 'ConfluenceIntegrationWizard',
      props: ['open', 'mode', 'integrationId'],
      emits: ['close', 'integration-updated', 'integration-created'],
      template: '<div v-if="open" class="confluence-wizard-stub">{{ mode }}</div>',
    },
  })
);

vi.mock('vue-router', () => ({
  useRoute: () => routerSpies.route,
  useRouter: () => ({
    back: routerSpies.back,
    push: routerSpies.push,
    replace: routerSpies.replace,
  }),
}));

const serviceSpies = vi.hoisted(() => ({
  getConfluenceIntegrationById: vi.fn(),
}));

vi.mock('@/api/services/ConfluenceIntegrationService', () => ({
  ConfluenceIntegrationService: {
    getConfluenceIntegrationById: serviceSpies.getConfluenceIntegrationById,
  },
}));

import ConfluenceIntegrationDetailsPage from '@/components/outbound/confluenceintegration/details/ConfluenceIntegrationDetailsPage.vue';
import { ROUTES } from '@/router/routes';

const mockIntegration = {
  id: 'ci-1',
  name: 'Confluence Integration',
  isEnabled: true,
  version: 1,
  normalizedName: 'confluence-integration',
  languages: [],
  languageCodes: ['en'],
  confluenceSpaceKey: 'TEST',
  reportNameTemplate: 'Report',
  includeTableOfContents: true,
  connectionId: 'conn-1',
  schedule: {},
  tenantId: 'tenant-1',
  createdBy: 'admin',
  createdDate: '2026-01-01T00:00:00Z',
  lastModifiedBy: 'admin',
  lastModifiedDate: '2026-01-02T00:00:00Z',
  itemType: 'DOCUMENT',
  itemSubtype: 'DESIGN',
};

describe('ConfluenceIntegrationDetailsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    serviceSpies.getConfluenceIntegrationById.mockResolvedValue(mockIntegration);
  });

  it('loads details and renders integration name', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.find('[data-testid="title"]').text()).toBe('Confluence Integration');
  });

  it('shows error state when service call fails', async () => {
    serviceSpies.getConfluenceIntegrationById.mockRejectedValue(new Error('Not found'));
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(wrapper.find('[data-testid="error"]').exists()).toBe(true);
  });

  it('sets breadcrumb title on mount', async () => {
    const setBreadcrumbTitle = vi.fn();
    mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle } },
    });
    await new Promise(r => setTimeout(r, 0));
    expect(setBreadcrumbTitle).toHaveBeenCalledWith('Confluence Integration Details');
  });

  it('opens edit wizard when openEditWizard is called', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.openEditWizard();
    await wrapper.vm.$nextTick();
    expect(vm.editWizardOpen).toBe(true);
  });

  it('opens clone wizard when openCloneWizard is called', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.openCloneWizard();
    await wrapper.vm.$nextTick();
    expect(vm.cloneWizardOpen).toBe(true);
    expect(vm.cloningIntegrationId).toBe('ci-1');
  });

  it('triggers a refresh after onStatusUpdated is called', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    serviceSpies.getConfluenceIntegrationById.mockClear();
    const vm = wrapper.vm as any;
    vm.onStatusUpdated(false);
    await wrapper.vm.$nextTick();
    // onStatusUpdated re-fetches integration details
    expect(serviceSpies.getConfluenceIntegrationById).toHaveBeenCalled();
  });

  it('handles tab change by id', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    const vm = wrapper.vm as any;
    vm.handleTabChangeById('settings');
    expect(vm.activeTab).toBe(1);
  });

  it('fallback to generic error message when error has no message', async () => {
    serviceSpies.getConfluenceIntegrationById.mockRejectedValue({});
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));
    expect((wrapper.vm as any).error).toContain('Failed to load');
  });

  it('ignores unknown tab ids and updates the selected tab for known ids', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));

    const vm = wrapper.vm as any;
    vm.handleTabChangeById('settings');
    expect(vm.activeTab).toBe(1);

    vm.handleTabChangeById('missing');
    expect(vm.activeTab).toBe(1);
  });

  it('updates local status and refetches integration details', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));

    serviceSpies.getConfluenceIntegrationById.mockClear();
    (wrapper.vm as any).onStatusUpdated(false);
    await new Promise(r => setTimeout(r, 0));

    expect((wrapper.vm as any).confluenceIntegrationResponse.isEnabled).toBe(true);
    expect(serviceSpies.getConfluenceIntegrationById).toHaveBeenCalledWith('ci-1');
  });

  it('navigates back to the list route with preserved query state', async () => {
    const wrapper = mount(ConfluenceIntegrationDetailsPage, {
      global: { provide: { setBreadcrumbTitle: vi.fn() } },
    });
    await new Promise(r => setTimeout(r, 0));

    (wrapper.vm as any).handleBack();
    expect(routerSpies.replace).toHaveBeenCalledWith({
      path: ROUTES.confluenceIntegration,
      query: { search: 'abc' },
    });
    expect(routerSpies.push).not.toHaveBeenCalled();
    expect(routerSpies.back).not.toHaveBeenCalled();
  });
});
