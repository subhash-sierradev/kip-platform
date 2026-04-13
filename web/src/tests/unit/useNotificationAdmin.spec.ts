import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { computed, defineComponent, nextTick, ref } from 'vue';

let eventsRef = ref<any[]>([]);
let rulesRef = ref<any[]>([]);
let templatesRef = ref<any[]>([]);
let policiesRef = ref<any[]>([]);
let isAuthenticatedRef = ref(true);
let eventsSavingRef = ref(false);
let rulesSavingRef = ref(false);
let templatesSavingRef = ref(false);
let policiesSavingRef = ref(false);

const hoisted = vi.hoisted(() => ({
  fetchEventsMock: vi.fn(),
  fetchRulesMock: vi.fn().mockResolvedValue(undefined),
  fetchTemplatesMock: vi.fn(),
  fetchPoliciesMock: vi.fn().mockResolvedValue(undefined),
  createRulesBatchMock: vi.fn().mockResolvedValue(undefined),
  getUsersMock: vi.fn(),
  successMock: vi.fn(),
  errorMock: vi.fn(),
}));

vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({
    get isAuthenticated() {
      return isAuthenticatedRef.value;
    },
  }),
}));

vi.mock('@/composables/useActiveIntegrationTypes', () => ({
  useActiveIntegrationTypes: () => ({ activeEntityTypes: computed(() => ['ARCGIS']) }),
}));

vi.mock('@/composables/useNotificationEvents', () => ({
  useNotificationEvents: () => ({
    events: eventsRef,
    loadingEvents: ref(false),
    saving: eventsSavingRef,
    fetchEvents: hoisted.fetchEventsMock,
    createEvent: vi.fn(),
    deactivateEvent: vi.fn(),
  }),
}));

vi.mock('@/composables/useNotificationRulesManager', () => ({
  useNotificationRulesManager: () => ({
    rules: rulesRef,
    loadingRules: ref(false),
    saving: rulesSavingRef,
    fetchRules: hoisted.fetchRulesMock,
    createRule: vi.fn(),
    updateRule: vi.fn(),
    deleteRule: vi.fn(),
    toggleRule: vi.fn(),
  }),
}));

vi.mock('@/composables/useNotificationTemplatesManager', () => ({
  useNotificationTemplatesManager: () => ({
    templates: templatesRef,
    loadingTemplates: ref(false),
    saving: templatesSavingRef,
    fetchTemplates: hoisted.fetchTemplatesMock,
  }),
}));

vi.mock('@/composables/useNotificationPoliciesManager', () => ({
  useNotificationPoliciesManager: () => ({
    policies: policiesRef,
    loadingPolicies: ref(false),
    saving: policiesSavingRef,
    fetchPolicies: hoisted.fetchPoliciesMock,
    createPolicy: vi.fn(),
    upsertPolicy: vi.fn(),
    deletePolicy: vi.fn(),
  }),
}));

vi.mock('@/api/services/NotificationAdminService', () => ({
  NotificationAdminService: {
    createRulesBatch: hoisted.createRulesBatchMock,
  },
}));

vi.mock('@/api/services/UserService', () => ({
  UserService: {
    getUsers: hoisted.getUsersMock,
  },
}));

vi.mock('@/utils/notificationUtils', () => ({
  default: {
    success: hoisted.successMock,
    error: hoisted.errorMock,
  },
}));

import { useNotificationAdmin } from '@/composables/useNotificationAdmin';

const Host = defineComponent({
  setup() {
    return useNotificationAdmin();
  },
  template: '<div />',
});

describe('useNotificationAdmin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    isAuthenticatedRef = ref(true);
    eventsSavingRef = ref(false);
    rulesSavingRef = ref(false);
    templatesSavingRef = ref(false);
    policiesSavingRef = ref(false);
    eventsRef = ref([
      { id: 'e1', eventKey: 'A' },
      { id: 'e2', eventKey: 'B' },
    ]);
    rulesRef = ref([
      { id: 'r1', eventKey: 'A' },
      { id: 'r2', eventKey: 'Z' },
    ]);
    templatesRef = ref([
      { id: 't1', eventKey: 'B' },
      { id: 't2', eventKey: 'X' },
    ]);
    policiesRef = ref([
      { id: 'p1', eventKey: 'A' },
      { id: 'p2', eventKey: null },
    ]);
  });

  it('filters rules, templates, and policies by active event keys and fetches events on mount', async () => {
    const wrapper = mount(Host);
    await flushPromises();

    expect(hoisted.fetchEventsMock).toHaveBeenCalledTimes(1);
    expect(wrapper.vm.filteredRules).toEqual([{ id: 'r1', eventKey: 'A' }]);
    expect(wrapper.vm.filteredTemplates).toEqual([{ id: 't1', eventKey: 'B' }]);
    expect(wrapper.vm.filteredPolicies).toEqual([
      { id: 'p1', eventKey: 'A' },
      { id: 'p2', eventKey: null },
    ]);
  });

  it('keeps rules without an event key and drops policies whose event key is inactive', async () => {
    rulesRef.value = [
      { id: 'r1', eventKey: 'A' },
      { id: 'r2', eventKey: null },
    ];
    policiesRef.value = [
      { id: 'p1', eventKey: 'B' },
      { id: 'p2', eventKey: 'missing' },
    ];

    const wrapper = mount(Host);
    await flushPromises();

    expect(wrapper.vm.filteredRules).toEqual([
      { id: 'r1', eventKey: 'A' },
      { id: 'r2', eventKey: null },
    ]);
    expect(wrapper.vm.filteredPolicies).toEqual([{ id: 'p1', eventKey: 'B' }]);
  });

  it('waits for authentication before fetching events', async () => {
    isAuthenticatedRef.value = false;
    mount(Host);
    await flushPromises();
    expect(hoisted.fetchEventsMock).not.toHaveBeenCalled();

    isAuthenticatedRef.value = true;
    await nextTick();

    expect(hoisted.fetchEventsMock).toHaveBeenCalledTimes(1);
  });

  it('fetches users successfully and handles errors', async () => {
    hoisted.getUsersMock.mockResolvedValueOnce([{ keycloakUserId: 'u1', displayName: 'User' }]);
    const wrapper = mount(Host);
    await flushPromises();

    await wrapper.vm.fetchUsers();
    expect(wrapper.vm.users).toEqual([{ keycloakUserId: 'u1', displayName: 'User' }]);
    expect(wrapper.vm.loadingUsers).toBe(false);

    hoisted.getUsersMock.mockRejectedValueOnce(new Error('bad'));
    await wrapper.vm.fetchUsers();
    expect(hoisted.errorMock).toHaveBeenCalledWith('Failed to load users');
  });

  it('reflects saving when any child manager is saving', async () => {
    const wrapper = mount(Host);
    await flushPromises();

    expect(wrapper.vm.saving).toBe(false);

    rulesSavingRef.value = true;
    await nextTick();
    expect(wrapper.vm.saving).toBe(true);

    rulesSavingRef.value = false;
    templatesSavingRef.value = true;
    await nextTick();
    expect(wrapper.vm.saving).toBe(true);
  });

  it('creates remaining rules and always refreshes rules and policies', async () => {
    const wrapper = mount(Host);
    await flushPromises();

    await wrapper.vm.createAllRemainingRules(
      [{ event: { id: 'e1' }, severity: 'ERROR' }],
      'SELECTED_USERS',
      ['u1']
    );

    expect(hoisted.createRulesBatchMock).toHaveBeenCalledWith({
      requestBody: {
        rules: [{ eventId: 'e1', severity: 'ERROR', isEnabled: true }],
        recipientType: 'SELECTED_USERS',
        userIds: ['u1'],
      },
    });
    expect(hoisted.successMock).toHaveBeenCalledWith('1 rule created successfully');
    expect(hoisted.fetchRulesMock).toHaveBeenCalled();
    expect(hoisted.fetchPoliciesMock).toHaveBeenCalled();

    hoisted.createRulesBatchMock.mockRejectedValueOnce(new Error('bad'));
    await wrapper.vm.createAllRemainingRules(
      [{ event: { id: 'e2' }, severity: 'INFO' }],
      'ALL_USERS'
    );
    expect(hoisted.errorMock).toHaveBeenCalledWith('Failed to create notification rules');
  });

  it('omits userIds for selected-users batches when none are provided and pluralizes success messages', async () => {
    const wrapper = mount(Host);
    await flushPromises();

    await wrapper.vm.createAllRemainingRules(
      [
        { event: { id: 'e1' }, severity: 'ERROR' },
        { event: { id: 'e2' }, severity: 'INFO' },
      ],
      'SELECTED_USERS',
      []
    );

    expect(hoisted.createRulesBatchMock).toHaveBeenCalledWith({
      requestBody: {
        rules: [
          { eventId: 'e1', severity: 'ERROR', isEnabled: true },
          { eventId: 'e2', severity: 'INFO', isEnabled: true },
        ],
        recipientType: 'SELECTED_USERS',
      },
    });
    expect(hoisted.successMock).toHaveBeenCalledWith('2 rules created successfully');
  });
});
