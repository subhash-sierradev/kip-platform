import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import ArcGISIntegrationPage from '@/components/outbound/arcgisintegration/ArcGISIntegrationPage.vue';
import { ROUTES } from '@/router/routes';

const arcgisMocks = vi.hoisted(() => ({
  getAllIntegrationsMock: vi.fn(),
  deleteIntegrationMock: vi.fn(),
  toggleIntegrationStatusMock: vi.fn(),
  triggerJobExecutionMock: vi.fn(),
  showErrorMock: vi.fn(),
}));

vi.mock('@/composables/useArcGISIntegrationActions', () => ({
  useArcGISIntegrationActions: () => ({
    loading: false,
    getAllIntegrations: arcgisMocks.getAllIntegrationsMock,
    deleteIntegration: arcgisMocks.deleteIntegrationMock,
  }),
  useArcGISIntegrationStatus: () => ({
    toggleIntegrationStatus: arcgisMocks.toggleIntegrationStatusMock,
  }),
  useArcGISIntegrationTrigger: () => ({
    loading: false,
    triggerJobExecution: arcgisMocks.triggerJobExecutionMock,
  }),
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showInfo: vi.fn(),
    showSuccess: vi.fn(),
    showError: arcgisMocks.showErrorMock,
    showWarning: vi.fn(),
  }),
}));

function stubMatchMedia() {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(() => ({
      matches: false,
      media: '',
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: ROUTES.arcgisIntegration,
        name: 'arcgis-integration-list',
        component: ArcGISIntegrationPage,
      },
      {
        path: ROUTES.arcgisIntegrationDetails(':id'),
        name: 'arcgis-integration-details',
        component: { template: '<div class="arcgis-details-page">Details</div>' },
      },
    ],
  });
}

async function mountDashboard(initialPath: string = ROUTES.arcgisIntegration) {
  const pinia = createPinia();
  setActivePinia(pinia);
  const router = createTestRouter();

  await router.push(initialPath);
  await router.isReady();

  const wrapper = mount(ArcGISIntegrationPage, {
    global: {
      plugins: [pinia, router],
      stubs: {
        DashboardToolbar: {
          emits: ['create'],
          template:
            '<button class="arcgis-create" type="button" @click="$emit(\'create\')">create</button>',
        },
        ArcGISIntegrationCard: {
          emits: ['open'],
          template:
            '<button class="arcgis-open" type="button" @click="$emit(\'open\', \'arcgis-1\')">open</button>',
        },
        ConfirmationDialog: true,
        ArcGISIntegrationWizard: {
          name: 'ArcGISIntegrationWizard',
          props: ['open', 'mode', 'integrationId'],
          emits: ['close', 'integration-created', 'integration-updated'],
          template:
            '<div v-if="open" class="arcgis-wizard"><button class="arcgis-wizard-close" type="button" @click="$emit(\'close\')">close</button></div>',
        },
      },
    },
  });

  await flushPromises();
  return { wrapper, router };
}

describe('ArcGIS dashboard integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    stubMatchMedia();
    Object.defineProperty(window, 'scrollY', {
      configurable: true,
      writable: true,
      value: 180,
    });

    arcgisMocks.getAllIntegrationsMock.mockResolvedValue(
      Array.from({ length: 7 }, (_, index) => ({
        id: `arcgis-${index + 1}`,
        name: `Roads ArcGIS Integration ${index + 1}`,
        createdBy: 'alice',
        createdDate: `2024-01-0${(index % 7) + 1}T00:00:00Z`,
        isEnabled: true,
      }))
    );
    arcgisMocks.deleteIntegrationMock.mockResolvedValue(true);
    arcgisMocks.toggleIntegrationStatusMock.mockResolvedValue(true);
    arcgisMocks.triggerJobExecutionMock.mockResolvedValue(undefined);
  });

  it('opens and closes the create wizard from the dashboard toolbar', async () => {
    const { wrapper } = await mountDashboard();

    expect(wrapper.find('.arcgis-wizard').exists()).toBe(false);

    await wrapper.find('.arcgis-create').trigger('click');
    await flushPromises();

    expect(wrapper.find('.arcgis-wizard').exists()).toBe(true);

    await wrapper.find('.arcgis-wizard-close').trigger('click');
    await flushPromises();

    expect(wrapper.find('.arcgis-wizard').exists()).toBe(false);
  });

  it('navigates to ArcGIS details while preserving dashboard query state', async () => {
    const { wrapper, router } = await mountDashboard(
      `${ROUTES.arcgisIntegration}?search=roads&sort=name&view=list&page=2&size=6`
    );

    await wrapper.find('.arcgis-open').trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe(ROUTES.arcgisIntegrationDetails('arcgis-1'));
    expect(router.currentRoute.value.query).toEqual({
      search: 'roads',
      sort: 'name',
      view: 'list',
      page: '2',
      size: '6',
      scroll: '180',
    });
  });
});
