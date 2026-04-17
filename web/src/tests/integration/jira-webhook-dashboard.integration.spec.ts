import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import JiraWebhookDashboard from '@/components/outbound/jirawebhooks/JiraWebhookDashboard.vue';
import { ROUTES } from '@/router/routes';

const serviceMocks = vi.hoisted(() => ({
  listJiraWebhooksMock: vi.fn(),
  getWebhookByIdMock: vi.fn(),
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    listJiraWebhooks: serviceMocks.listJiraWebhooksMock,
    getWebhookById: serviceMocks.getWebhookByIdMock,
    getAllJiraNormalizedNames: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showError: vi.fn(),
    showSuccess: vi.fn(),
    showWarning: vi.fn(),
  }),
}));

vi.mock('@/composables/useWebhookActions', () => ({
  useWebhookActions: () => ({
    toggleWebhookStatus: vi.fn(),
    deleteWebhook: vi.fn().mockResolvedValue(true),
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
        path: ROUTES.jiraWebhook,
        name: 'jira-webhook-list',
        component: JiraWebhookDashboard,
      },
      {
        path: ROUTES.jiraWebhookDetails(':id'),
        name: 'jira-webhook-details',
        component: { template: '<div class="details-page">Details</div>' },
      },
    ],
  });
}

async function mountDashboard(initialPath: string = ROUTES.jiraWebhook) {
  const pinia = createPinia();
  setActivePinia(pinia);
  const router = createTestRouter();

  await router.push(initialPath);
  await router.isReady();

  const wrapper = mount(JiraWebhookDashboard, {
    global: {
      plugins: [pinia, router],
      stubs: {
        DashboardToolbar: {
          emits: ['create'],
          template:
            '<button class="dashboard-create" type="button" @click="$emit(\'create\')">create</button>',
        },
        JiraWebhookCard: {
          emits: ['open'],
          template:
            '<button class="dashboard-open" type="button" @click="$emit(\'open\', \'webhook-1\')">open</button>',
        },
        ConfirmationDialog: true,
        JiraWebhookWizard: {
          name: 'JiraWebhookWizard',
          props: ['open', 'editMode', 'cloneMode'],
          emits: ['close', 'webhook-created'],
          template:
            '<div v-if="open" class="dashboard-wizard"><button class="dashboard-wizard-close" type="button" @click="$emit(\'close\')">close</button></div>',
        },
      },
    },
  });

  await flushPromises();
  return { wrapper, router };
}

describe('Jira webhook dashboard integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    stubMatchMedia();
    Object.defineProperty(window, 'scrollY', {
      configurable: true,
      writable: true,
      value: 240,
    });

    serviceMocks.listJiraWebhooksMock.mockResolvedValue([
      {
        id: 'webhook-1',
        name: 'Webhook 1',
        createdBy: 'alice',
        createdDate: '2024-01-01T00:00:00Z',
        isEnabled: true,
        webhookUrl: 'https://example.test/webhook-1',
        jiraFieldMappings: [],
      },
    ]);
    serviceMocks.getWebhookByIdMock.mockResolvedValue({});
  });

  it('opens and closes the inline wizard from the dashboard toolbar', async () => {
    const { wrapper } = await mountDashboard();

    expect(wrapper.find('.dashboard-wizard').exists()).toBe(false);

    await wrapper.find('.dashboard-create').trigger('click');
    await flushPromises();

    expect(wrapper.find('.dashboard-wizard').exists()).toBe(true);

    await wrapper.find('.dashboard-wizard-close').trigger('click');
    await flushPromises();

    expect(wrapper.find('.dashboard-wizard').exists()).toBe(false);
  });

  it('navigates to the details route while preserving dashboard state in the query', async () => {
    const { wrapper, router } = await mountDashboard(
      `${ROUTES.jiraWebhook}?search=alpha&sort=name&view=list&page=2&size=6`
    );

    await wrapper.find('.dashboard-open').trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe(ROUTES.jiraWebhookDetails('webhook-1'));
    expect(router.currentRoute.value.query).toEqual({
      search: 'alpha',
      sort: 'name',
      view: 'list',
      page: '2',
      size: '6',
      scroll: '240',
    });
  });
});
