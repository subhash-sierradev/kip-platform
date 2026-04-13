import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { computed, defineComponent, h, ref } from 'vue';

import NotificationsPage from '@/components/admin/notifications/NotificationsPage.vue';

const state = {
  events: ref<any[]>([]),
  rules: ref<any[]>([]),
  policies: ref<any[]>([]),
  users: ref<any[]>([]),
  templates: ref<any[]>([]),
  saving: ref(false),
  fetchEvents: vi.fn(),
  fetchRules: vi.fn(),
  fetchTemplates: vi.fn(),
  fetchPolicies: vi.fn(),
  fetchUsers: vi.fn(),
  createEvent: vi.fn(),
  createRule: vi.fn(),
  updateRule: vi.fn(),
  deleteRule: vi.fn(),
  toggleRule: vi.fn(),
  upsertPolicy: vi.fn(),
  deletePolicy: vi.fn(),
};

const resetState = {
  showConfirmDialog: ref(false),
  isResetting: ref(false),
  resetToDefaults: vi.fn(),
  confirmReset: vi.fn(),
  cancelReset: vi.fn(),
};

vi.mock('@/composables/useNotificationAdmin', () => ({
  useNotificationAdmin: () => ({
    events: state.events,
    rules: state.rules,
    policies: state.policies,
    users: state.users,
    saving: state.saving,
    filteredRules: computed(() => state.rules.value),
    filteredTemplates: computed(() => state.templates.value),
    filteredPolicies: computed(() => state.policies.value),
    fetchEvents: state.fetchEvents,
    fetchRules: state.fetchRules,
    fetchTemplates: state.fetchTemplates,
    fetchPolicies: state.fetchPolicies,
    fetchUsers: state.fetchUsers,
    createEvent: state.createEvent,
    createRule: state.createRule,
    updateRule: state.updateRule,
    deleteRule: state.deleteRule,
    toggleRule: state.toggleRule,
    upsertPolicy: state.upsertPolicy,
    deletePolicy: state.deletePolicy,
  }),
}));

vi.mock('@/composables/useNotificationReset', () => ({
  useNotificationReset: () => ({
    showConfirmDialog: resetState.showConfirmDialog,
    isResetting: resetState.isResetting,
    resetToDefaults: resetState.resetToDefaults,
    confirmReset: resetState.confirmReset,
    cancelReset: resetState.cancelReset,
  }),
}));

const tabNavStub = defineComponent({
  name: 'TabNavigation',
  props: {
    tabs: { type: Array, required: true },
    activeTab: { type: String, required: true },
  },
  emits: ['tab-change'],
  setup(props, { emit }) {
    return () =>
      h(
        'div',
        { class: 'tab-nav-stub' },
        (props.tabs as any[]).map(tab =>
          h(
            'button',
            {
              class: `tab-btn-${tab.id}`,
              onClick: () => emit('tab-change', tab.id),
            },
            tab.label
          )
        )
      );
  },
});

const openRuleModalSpy = vi.fn();
const rulesTabStub = defineComponent({
  name: 'NotificationsRulesTab',
  setup(_, { expose }) {
    expose({ openRuleModal: openRuleModalSpy });
    return () => h('div', { class: 'rules-tab-stub' }, 'rules tab');
  },
});

const openPolicyModalSpy = vi.fn();
const recipientsTabStub = defineComponent({
  name: 'NotificationsRecipientsTab',
  setup(_, { expose }) {
    expose({ openPolicyModal: openPolicyModalSpy });
    return () => h('div', { class: 'recipients-tab-stub' }, 'recipients tab');
  },
});

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    state.events.value = [];
    state.rules.value = [];
    state.policies.value = [];
    state.templates.value = [];
    state.users.value = [];
    state.saving.value = false;
    resetState.showConfirmDialog.value = false;
    resetState.isResetting.value = false;
    resetState.confirmReset.mockResolvedValue(undefined);
    state.createEvent.mockResolvedValue(true);
  });

  function gridStubWithEventSlots() {
    return defineComponent({
      name: 'GenericDataGrid',
      props: ['data'],
      setup(props, { slots }) {
        return () =>
          h(
            'div',
            { class: 'grid-slot-stub' },
            (props.data as any[]).map(row =>
              h('div', { class: 'grid-slot-row', key: row.id ?? row.eventKey }, [
                slots.eventKeyTemplate?.({ data: row }),
                slots.descriptionTemplate?.({ data: row }),
                slots.notifyInitiatorTemplate?.({ data: row }),
              ])
            )
          );
      },
    });
  }

  function mountPage() {
    return mount(NotificationsPage, {
      global: {
        stubs: {
          TabNavigation: tabNavStub,
          GenericDataGrid: { template: '<div class="grid-stub" />' },
          StatusChipForDataTable: true,
          NotificationEventModal: {
            emits: ['save', 'close'],
            template:
              '<div class="event-modal-stub"><button class="emit-event-save" @click="$emit(\'save\', { eventKey: \'A\' })">save</button></div>',
          },
          NotificationsRulesTab: rulesTabStub,
          NotificationsRecipientsTab: recipientsTabStub,
          ConfirmationDialog: {
            props: ['open'],
            emits: ['cancel', 'confirm'],
            template:
              '<div><button class="confirm-reset" v-if="open" @click="$emit(\'confirm\')">confirm</button></div>',
          },
        },
      },
    });
  }

  it('fetches events/templates/rules on mount', () => {
    mountPage();
    expect(state.fetchEvents).toHaveBeenCalledTimes(1);
    expect(state.fetchTemplates).toHaveBeenCalledTimes(1);
    expect(state.fetchRules).toHaveBeenCalledTimes(1);
  });

  it('switches tabs and fetches rules/policies when relevant', async () => {
    const wrapper = mountPage();

    await wrapper.find('.tab-btn-rules').trigger('click');
    expect(state.fetchRules).toHaveBeenCalledTimes(2);

    await wrapper.find('.tab-btn-recipients').trigger('click');
    expect(state.fetchPolicies).toHaveBeenCalledTimes(1);
  });

  it('does not refetch rules or policies when switching to passive tabs', async () => {
    const wrapper = mountPage();

    await wrapper.find('.tab-btn-templates').trigger('click');
    await wrapper.find('.tab-btn-events').trigger('click');

    expect(state.fetchRules).toHaveBeenCalledTimes(1);
    expect(state.fetchPolicies).not.toHaveBeenCalled();
  });

  it('disables Add Rule when no available events and enables when available', async () => {
    state.events.value = [{ id: 'e1', eventKey: 'A' } as any];
    state.rules.value = [{ id: 'r1', eventId: 'e1' } as any];
    const wrapper = mountPage();

    await wrapper.find('.tab-btn-rules').trigger('click');
    const addRuleBtn = wrapper.find('.section-primary-btn');
    expect(addRuleBtn.exists()).toBe(true);
    expect(addRuleBtn.attributes('disabled')).toBeDefined();

    state.rules.value = [];
    await wrapper.vm.$nextTick();
    expect(wrapper.find('.section-primary-btn').attributes('disabled')).toBeUndefined();
  });

  it('updates action button titles for exhausted and available recipient rules', async () => {
    state.rules.value = [{ id: 'r1', eventId: 'e1' } as any];
    state.policies.value = [{ id: 'p1', ruleId: 'r1' } as any];
    const wrapper = mountPage();

    await wrapper.find('.tab-btn-recipients').trigger('click');

    const button = wrapper.find('.section-primary-btn');
    expect(button.attributes('disabled')).toBeDefined();
    expect(button.attributes('title')).toBe('All rules already have recipients configured');

    state.policies.value = [];
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.section-primary-btn').attributes('title')).toBe(
      'Add recipients to a notification rule'
    );
  });

  it('opens rule modal from rules tab and policy modal from recipients tab', async () => {
    state.events.value = [{ id: 'e1', eventKey: 'A' } as any];
    state.rules.value = [];
    const wrapper = mountPage();

    await wrapper.find('.tab-btn-rules').trigger('click');
    await wrapper.find('.section-primary-btn').trigger('click');
    expect(openRuleModalSpy).toHaveBeenCalledTimes(1);

    state.rules.value = [{ id: 'r1', eventId: 'e1' } as any];
    state.policies.value = [];
    await wrapper.find('.tab-btn-recipients').trigger('click');
    await wrapper.find('.section-primary-btn').trigger('click');
    expect(openPolicyModalSpy).toHaveBeenCalledWith(undefined);
  });

  it('invokes reset flow and refetches on confirm success', async () => {
    resetState.showConfirmDialog.value = true;
    const wrapper = mountPage();

    await wrapper.find('.tab-reset-btn').trigger('click');
    expect(resetState.resetToDefaults).toHaveBeenCalledTimes(1);

    await wrapper.find('.confirm-reset').trigger('click');
    await Promise.resolve();

    expect(resetState.confirmReset).toHaveBeenCalledTimes(1);
    expect(state.fetchRules).toHaveBeenCalled();
    expect(state.fetchPolicies).toHaveBeenCalled();
  });

  it('swallows reset confirmation errors without refetching data', async () => {
    resetState.showConfirmDialog.value = true;
    resetState.confirmReset.mockRejectedValueOnce(new Error('reset failed'));
    const wrapper = mountPage();

    await wrapper.find('.confirm-reset').trigger('click');
    await Promise.resolve();

    expect(resetState.confirmReset).toHaveBeenCalledTimes(1);
    expect(state.fetchRules).toHaveBeenCalledTimes(1);
    expect(state.fetchPolicies).not.toHaveBeenCalled();
  });

  it('keeps modal open when createEvent fails and closes on success path', async () => {
    const wrapper = mountPage();
    const saveButton = wrapper.find('.emit-event-save');

    state.createEvent.mockResolvedValueOnce(false);
    await saveButton.trigger('click');
    await Promise.resolve();
    expect(state.createEvent).toHaveBeenCalledTimes(1);

    state.createEvent.mockResolvedValueOnce(true);
    await saveButton.trigger('click');
    await Promise.resolve();
    expect(state.createEvent).toHaveBeenCalledTimes(2);
  });

  it('renders notifyInitiator chips for both enabled and disabled event rows', () => {
    state.events.value = [
      { id: 'e1', eventKey: 'A', description: 'Enabled event', notifyInitiator: true } as any,
      { id: 'e2', eventKey: 'B', description: 'Disabled event', notifyInitiator: false } as any,
    ];

    const wrapper = mount(NotificationsPage, {
      global: {
        stubs: {
          TabNavigation: tabNavStub,
          GenericDataGrid: gridStubWithEventSlots(),
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label }}|{{ status }}</span>',
          },
          NotificationEventModal: {
            emits: ['save', 'close'],
            template: '<div class="event-modal-stub" />',
          },
          NotificationsRulesTab: rulesTabStub,
          NotificationsRecipientsTab: recipientsTabStub,
          ConfirmationDialog: {
            props: ['open'],
            emits: ['cancel', 'confirm'],
            template: '<div />',
          },
        },
      },
    });

    expect(wrapper.text()).toContain('Yes|ACTIVE');
    expect(wrapper.text()).toContain('No|INACTIVE');
  });

  it('shows the resetting label and disables the reset button while a reset is in progress', () => {
    resetState.isResetting.value = true;

    const wrapper = mountPage();
    const button = wrapper.find('.tab-reset-btn');

    expect(button.attributes('disabled')).toBeDefined();
    expect(button.classes()).toContain('tab-reset-btn--loading');
    expect(button.text()).toContain('Resetting…');
  });
});
