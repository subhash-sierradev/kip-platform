import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import ConfluenceIntegrationPage from '@/components/outbound/confluenceintegration/ConfluenceIntegrationPage.vue';
import { ROUTES } from '@/router/routes';

const confluenceMocks = vi.hoisted(() => ({
  getAllIntegrationsMock: vi.fn(),
  deleteIntegrationMock: vi.fn(),
  toggleIntegrationStatusMock: vi.fn(),
}));

vi.mock('@/composables/useConfluenceIntegrationActions', () => ({
  useConfluenceIntegrationActions: () => ({
    loading: false,
    getAllIntegrations: confluenceMocks.getAllIntegrationsMock,
    deleteIntegration: confluenceMocks.deleteIntegrationMock,
  }),
  useConfluenceIntegrationStatus: () => ({
    toggleIntegrationStatus: confluenceMocks.toggleIntegrationStatusMock,
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
        path: ROUTES.confluenceIntegration,
        name: 'confluence-integration-list',
        component: ConfluenceIntegrationPage,
      },
      {
        path: ROUTES.confluenceIntegrationDetails(':id'),
        name: 'confluence-integration-details',
        component: { template: '<div class="confluence-details-page">Details</div>' },
      },
    ],
  });
}

async function mountDashboard(initialPath: string = ROUTES.confluenceIntegration) {
  const pinia = createPinia();
  setActivePinia(pinia);
  const router = createTestRouter();

  await router.push(initialPath);
  await router.isReady();

  const wrapper = mount(ConfluenceIntegrationPage, {
    global: {
      plugins: [pinia, router],
      stubs: {
        DashboardToolbar: {
          emits: ['create'],
          template:
            '<button class="confluence-create" type="button" @click="$emit(\'create\')">create</button>',
        },
        ConfluenceIntegrationCard: {
          emits: ['open'],
          template:
            '<button class="confluence-open" type="button" @click="$emit(\'open\', \'confluence-1\')">open</button>',
        },
        ConfirmationDialog: true,
        ConfluenceIntegrationWizard: {
          name: 'ConfluenceIntegrationWizard',
          props: ['open', 'mode', 'integrationId'],
          emits: ['close', 'integration-created', 'integration-updated'],
          template:
            '<div v-if="open" class="confluence-wizard"><button class="confluence-wizard-close" type="button" @click="$emit(\'close\')">close</button></div>',
        },
      },
    },
  });

  await flushPromises();
  return { wrapper, router };
}

describe('Confluence dashboard integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    stubMatchMedia();
    Object.defineProperty(window, 'scrollY', {
      configurable: true,
      writable: true,
      value: 320,
    });

    confluenceMocks.getAllIntegrationsMock.mockResolvedValue(
      Array.from({ length: 7 }, (_, index) => ({
        id: `confluence-${index + 1}`,
        name: `Wiki Confluence Integration ${index + 1}`,
        createdBy: 'alice',
        createdDate: `2024-01-0${(index % 7) + 1}T00:00:00Z`,
        isEnabled: true,
      }))
    );
    confluenceMocks.deleteIntegrationMock.mockResolvedValue(true);
    confluenceMocks.toggleIntegrationStatusMock.mockResolvedValue(true);
  });

  it('opens and closes the create wizard from the dashboard toolbar', async () => {
    const { wrapper } = await mountDashboard();

    expect(wrapper.find('.confluence-wizard').exists()).toBe(false);

    await wrapper.find('.confluence-create').trigger('click');
    await flushPromises();

    expect(wrapper.find('.confluence-wizard').exists()).toBe(true);

    await wrapper.find('.confluence-wizard-close').trigger('click');
    await flushPromises();

    expect(wrapper.find('.confluence-wizard').exists()).toBe(false);
  });

  it('navigates to Confluence details while preserving dashboard query state', async () => {
    const { wrapper, router } = await mountDashboard(
      `${ROUTES.confluenceIntegration}?search=wiki&sort=name&view=list&page=2&size=6`
    );

    await wrapper.find('.confluence-open').trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe(
      ROUTES.confluenceIntegrationDetails('confluence-1')
    );
    expect(router.currentRoute.value.query).toEqual({
      search: 'wiki',
      sort: 'name',
      view: 'list',
      page: '2',
      size: '6',
      scroll: '320',
    });
  });
});
