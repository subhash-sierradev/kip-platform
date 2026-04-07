import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import { NotificationSeverity } from '@/api/models/NotificationSeverity';

const confirmWithHandlers = vi.fn(async (handlers: { delete: () => Promise<boolean> }) =>
  handlers.delete()
);
const moveTooltip = vi.fn();
const showTooltip = vi.fn();
const hideTooltip = vi.fn();

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    emits: ['click'],
    template: '<button class="dx-button" @click="$emit(\'click\')"><slot /></button>',
  },
}));

vi.mock('@/composables/useConfirmationDialog', () => ({
  useConfirmationDialog: () => {
    const dialogOpen = ref(false);
    const pendingAction = ref<'delete' | null>(null);
    return {
      dialogOpen,
      actionLoading: ref(false),
      pendingAction,
      dialogTitle: ref('Delete'),
      dialogDescription: ref('Delete desc'),
      dialogConfirmLabel: ref('Delete'),
      openDialog: vi.fn((action: 'delete') => {
        pendingAction.value = action;
        dialogOpen.value = true;
      }),
      closeDialog: vi.fn(() => {
        dialogOpen.value = false;
        pendingAction.value = null;
      }),
      confirmWithHandlers,
    };
  },
}));

vi.mock('@/composables/useTooltip', () => ({
  useTooltip: () => ({
    tooltip: ref({ visible: false, x: 0, y: 0 }),
    showTooltip,
    moveTooltip,
    hideTooltip,
  }),
}));

import NotificationsRecipientsTab from '@/components/admin/notifications/NotificationsRecipientsTab.vue';

const GenericDataGridStub = {
  props: ['data'],
  template: `
    <div>
      <slot name="recipientTypeTemplate" :data="data[0]" />
      <slot name="recipientUpdatedByTemplate" :data="data[0]" />
      <slot name="recipientDateTemplate" :data="data[0]" />
      <slot name="recipientActionTemplate" :data="data[0]" />
    </div>
  `,
};

const NotificationPolicyModalStub = {
  props: ['show'],
  emits: ['close', 'save'],
  template: `
    <div>
      <div class="policy-modal" v-if="show">open</div>
      <button class="emit-save" @click="$emit('save', { ruleId: 'r1', recipientType: 'ALL_USERS', userIds: [] })">save</button>
      <button class="emit-close" @click="$emit('close')">close</button>
    </div>
  `,
};

const ConfirmationDialogStub = {
  props: ['open'],
  emits: ['confirm', 'cancel'],
  template:
    '<div><button v-if="open" class="confirm" @click="$emit(\'confirm\')">confirm</button></div>',
};

const CommonTooltipStub = {
  props: ['visible'],
  template: '<div class="tooltip" :data-visible="visible"><slot /></div>',
};

describe('NotificationsRecipientsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  function mountTab(overrides: Record<string, unknown> = {}) {
    return mount(NotificationsRecipientsTab, {
      props: {
        filteredPolicies: [
          {
            id: 'p1',
            ruleId: 'r1',
            recipientType: 'SELECTED_USERS',
            users: [
              { userId: 'u1', displayName: 'One' },
              { userId: 'u2', displayName: 'Two' },
              { userId: 'u3', displayName: 'Three' },
            ],
            lastModifiedBy: 'admin',
            lastModifiedDate: new Date().toISOString(),
          },
        ],
        policies: [{ id: 'p1', ruleId: 'r1', recipientType: 'SELECTED_USERS', users: [] }],
        availableRulesForPolicy: [],
        rules: [{ id: 'r1', eventKey: 'A', severity: NotificationSeverity.INFO }],
        users: [{ keycloakUserId: 'u1', displayName: 'One' }],
        saving: false,
        fetchUsers: vi.fn(async () => undefined),
        upsertPolicy: vi.fn(async () => true),
        deletePolicy: vi.fn(async () => undefined),
        ...overrides,
      },
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          NotificationPolicyModal: NotificationPolicyModalStub,
          ConfirmationDialog: ConfirmationDialogStub,
          CommonTooltip: CommonTooltipStub,
          StatusChipForDataTable: true,
        },
      },
    });
  }

  it('opens modal with exposed method and closes after successful save', async () => {
    const upsertPolicy = vi.fn(async () => true);
    const wrapper = mountTab({ upsertPolicy });

    await (wrapper.vm as { openPolicyModal: (id?: string) => Promise<void> }).openPolicyModal('r1');
    expect(wrapper.find('.policy-modal').exists()).toBe(true);

    await wrapper.find('.emit-save').trigger('click');
    expect(upsertPolicy).toHaveBeenCalledTimes(1);
    expect(wrapper.find('.policy-modal').exists()).toBe(false);
  });

  it('fetches users when opening modal and users list is empty', async () => {
    const fetchUsers = vi.fn(async () => undefined);
    const wrapper = mountTab({ users: [], fetchUsers });

    await (wrapper.vm as { openPolicyModal: (id?: string) => Promise<void> }).openPolicyModal('r1');

    expect(fetchUsers).toHaveBeenCalledTimes(1);
  });

  it('shows tooltip handlers for overflow users chip', async () => {
    const wrapper = mountTab();
    const overflowButton = wrapper.find('.user-chip--overflow');
    expect(overflowButton.exists()).toBe(true);

    await overflowButton.trigger('mouseenter');
    await overflowButton.trigger('mousemove');
    await overflowButton.trigger('mouseleave');

    expect(showTooltip).toHaveBeenCalled();
    expect(moveTooltip).toHaveBeenCalled();
    expect(hideTooltip).toHaveBeenCalled();
  });

  it('deletes policy via confirmation flow', async () => {
    const deletePolicy = vi.fn(async () => undefined);
    const wrapper = mountTab({ deletePolicy });

    await wrapper.findAll('.dx-button')[1].trigger('click');
    await wrapper.vm.$nextTick();
    await wrapper.find('.confirm').trigger('click');

    expect(confirmWithHandlers).toHaveBeenCalled();
    expect(deletePolicy).toHaveBeenCalledWith('p1');
  });
});
