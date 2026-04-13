/* eslint-disable simple-import-sort/imports */
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { NotificationSeverity } from '@/api/models/NotificationSeverity';

const handleToggleClick = vi.fn();
const handleToggleCancel = vi.fn();
const handleToggleConfirm = vi.fn();
const rowToggleLoadingState = ref<Record<string, boolean>>({});
const ruleEnabledState = ref(true);
const confirmWithHandlers = vi.fn(async (handlers: { delete: () => Promise<boolean> }) =>
  handlers.delete()
);

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    emits: ['click'],
    template: '<button class="dx-button" @click="$emit(\'click\')"><slot /></button>',
  },
}));

vi.mock('devextreme-vue/switch', () => ({
  DxSwitch: {
    props: ['value', 'disabled'],
    template: '<input class="dx-switch" type="checkbox" :checked="value" :disabled="disabled" />',
  },
}));

vi.mock('@/composables/useNotificationRulesToggle', () => ({
  useNotificationRulesToggle: () => ({
    rulesGridRef: ref(null),
    rowToggleLoading: rowToggleLoadingState,
    getRuleEnabled: () => ruleEnabledState.value,
    handleToggleClick,
    handleToggleCancel,
    handleToggleConfirm,
    toggleDialogOpen: ref(false),
    toggleActionLoading: ref(false),
    togglePendingAction: ref<'enable' | 'disable' | null>(null),
    toggleDialogTitle: ref('Toggle'),
    toggleDialogDescription: ref('desc'),
    toggleDialogConfirmLabel: ref('Confirm'),
  }),
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

import NotificationsRulesTab from '@/components/admin/notifications/NotificationsRulesTab.vue';

const GenericDataGridStub = {
  props: ['data'],
  template: `
    <div>
      <slot name="severityTemplate" :data="data[0]" />
      <slot name="enabledTemplate" :data="data[0]" />
      <slot name="dateTemplate" :data="data[0]" />
      <slot name="ruleUpdatedByTemplate" :data="data[0]" />
      <slot name="actionTemplate" :data="data[0]" />
    </div>
  `,
};

const NotificationRuleModalStub = {
  props: ['show'],
  emits: ['close', 'save', 'update'],
  template: `
    <div>
      <div class="rule-modal" v-if="show">open</div>
      <button class="emit-save" @click="$emit('save', { eventId: 'e1', severity: 'INFO', isEnabled: true })">save</button>
      <button class="emit-update" @click="$emit('update', 'ERROR')">update</button>
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

describe('NotificationsRulesTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    rowToggleLoadingState.value = {};
    ruleEnabledState.value = true;
  });

  function mountTab(overrides: Record<string, unknown> = {}) {
    return mount(NotificationsRulesTab, {
      props: {
        filteredRules: [
          {
            id: 'r1',
            severity: NotificationSeverity.INFO,
            lastModifiedDate: new Date().toISOString(),
            lastModifiedBy: 'u1',
          },
        ],
        rules: [{ id: 'r1', severity: NotificationSeverity.INFO }],
        events: [{ id: 'e1', displayName: 'E1', isEnabled: true }],
        saving: false,
        toggleRule: vi.fn(async () => true),
        createRule: vi.fn(async () => true),
        updateRule: vi.fn(async () => true),
        deleteRule: vi.fn(async () => undefined),
        ...overrides,
      },
      global: {
        stubs: {
          GenericDataGrid: GenericDataGridStub,
          NotificationRuleModal: NotificationRuleModalStub,
          ConfirmationDialog: ConfirmationDialogStub,
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label || status }}</span>',
          },
        },
      },
    });
  }

  it('opens modal via exposed openRuleModal and closes after successful save', async () => {
    const createRule = vi.fn(async () => true);
    const wrapper = mountTab({ createRule });

    (wrapper.vm as { openRuleModal: (id?: string) => void }).openRuleModal();
    await wrapper.vm.$nextTick();
    expect(wrapper.find('.rule-modal').exists()).toBe(true);

    await wrapper.find('.emit-save').trigger('click');
    expect(createRule).toHaveBeenCalledTimes(1);
    expect(wrapper.find('.rule-modal').exists()).toBe(false);
  });

  it('does not close modal when save fails', async () => {
    const createRule = vi.fn(async () => false);
    const wrapper = mountTab({ createRule });

    (wrapper.vm as { openRuleModal: (id?: string) => void }).openRuleModal();
    await wrapper.find('.emit-save').trigger('click');

    expect(createRule).toHaveBeenCalledTimes(1);
    expect(wrapper.find('.rule-modal').exists()).toBe(true);
  });

  it('handles update branch with/without existing id', async () => {
    const updateRule = vi.fn(async () => true);
    const wrapper = mountTab({ updateRule, rules: [] });

    await wrapper.find('.emit-update').trigger('click');
    expect(updateRule).not.toHaveBeenCalled();

    await wrapper.setProps({ rules: [{ id: 'r1', severity: NotificationSeverity.INFO }] });
    (wrapper.vm as { openRuleModal: (id?: string) => void }).openRuleModal('r1');
    await wrapper.find('.emit-update').trigger('click');
    expect(updateRule).toHaveBeenCalled();
  });

  it('invokes toggle handler from interceptor click', async () => {
    const wrapper = mountTab();
    await wrapper.find('.toggle-interceptor').trigger('click');
    expect(handleToggleClick).toHaveBeenCalledWith('r1');
  });

  it('deletes rule via confirmation dialog', async () => {
    const deleteRule = vi.fn(async () => undefined);
    const wrapper = mountTab({ deleteRule });

    await wrapper.findAll('.dx-button')[1].trigger('click');
    await wrapper.vm.$nextTick();
    await wrapper.find('.confirm').trigger('click');

    expect(confirmWithHandlers).toHaveBeenCalled();
    expect(deleteRule).toHaveBeenCalledWith('r1');
  });

  it('closes the rule modal and clears the selected rule when close is emitted', async () => {
    const wrapper = mountTab();

    (wrapper.vm as { openRuleModal: (id?: string) => void }).openRuleModal('r1');
    await wrapper.vm.$nextTick();
    expect(wrapper.find('.rule-modal').exists()).toBe(true);

    await wrapper.find('.emit-close').trigger('click');
    expect(wrapper.find('.rule-modal').exists()).toBe(false);
  });

  it('keeps the rule modal open when update fails', async () => {
    const updateRule = vi.fn(async () => false);
    const wrapper = mountTab({ updateRule });

    (wrapper.vm as { openRuleModal: (id?: string) => void }).openRuleModal('r1');
    await wrapper.find('.emit-update').trigger('click');

    expect(updateRule).toHaveBeenCalledWith('r1', 'ERROR');
    expect(wrapper.find('.rule-modal').exists()).toBe(true);
  });

  it('returns false from delete confirmation when no pending delete rule id exists', async () => {
    const wrapper = mountTab();

    await (wrapper.vm as any).handleConfirm();

    expect(confirmWithHandlers).toHaveBeenCalled();
  });

  it('renders disabled status and loading styles when a rule is inactive and toggling', () => {
    ruleEnabledState.value = false;
    rowToggleLoadingState.value = { r1: true };

    const wrapper = mountTab();
    const toggleInterceptor = wrapper.find('.toggle-interceptor');
    const toggleInput = wrapper.find('.dx-switch');

    expect(wrapper.text()).toContain('Disabled');
    expect(toggleInterceptor.classes()).toContain('toggle-interceptor--disabled');
    expect(toggleInput.attributes('disabled')).toBeDefined();
  });

  it('renders an em dash when the modifier is missing', () => {
    const wrapper = mountTab({
      filteredRules: [
        {
          id: 'r1',
          severity: NotificationSeverity.INFO,
          lastModifiedDate: new Date().toISOString(),
          lastModifiedBy: '',
        },
      ],
    });

    expect(wrapper.text()).toContain('—');
  });
});
