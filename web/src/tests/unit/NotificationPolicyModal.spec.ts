/* eslint-disable simple-import-sort/imports */
import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { NotificationSeverity } from '@/api/models/NotificationSeverity';

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    props: ['text', 'disabled'],
    emits: ['click'],
    template:
      '<button class="dx-button" :disabled="disabled" @click="$emit(\'click\')">{{ text }}</button>',
  },
}));

vi.mock('devextreme-vue/tag-box', () => ({
  DxTagBox: {
    props: ['value'],
    emits: ['update:value'],
    template:
      '<input class="tagbox" :value="(value || []).join(\',\')" @input="$emit(\'update:value\', $event.target.value ? $event.target.value.split(\',\') : [])" />',
  },
}));

import NotificationPolicyModal from '@/components/admin/notifications/NotificationPolicyModal.vue';

const AppModalStub = {
  props: ['open', 'title'],
  emits: ['update:open'],
  template: `
    <div>
      <div class="title">{{ title }}</div>
      <slot />
      <slot name="footer" />
      <button class="close-modal" @click="$emit('update:open', false)">close</button>
    </div>
  `,
};

describe('NotificationPolicyModal', () => {
  function mountModal(overrides: Record<string, unknown> = {}) {
    return mount(NotificationPolicyModal, {
      props: {
        show: true,
        isSaving: false,
        rules: [{ id: 'r1', eventKey: 'E1', severity: NotificationSeverity.INFO }],
        users: [
          { keycloakUserId: 'u1', displayName: 'One' },
          { keycloakUserId: 'u2', displayName: 'Two' },
        ],
        ...overrides,
      },
      global: {
        stubs: {
          AppModal: AppModalStub,
        },
      },
    });
  }

  it('does not emit save when required fields are missing', async () => {
    const wrapper = mountModal();
    await wrapper.findAll('.dx-button')[1].trigger('click');
    expect(wrapper.emitted('save')).toBeFalsy();
  });

  it('emits save with selected users payload when recipient type is SELECTED_USERS', async () => {
    const wrapper = mountModal({ preselectedRuleId: 'r1' });
    await wrapper.setProps({ show: false });
    await wrapper.setProps({ show: true });

    const recipientTypeSelect = wrapper.findAll('select')[1];
    await recipientTypeSelect.setValue('SELECTED_USERS');
    await wrapper.find('.tagbox').setValue('u1,u2');
    await wrapper.findAll('.dx-button')[1].trigger('click');

    expect(wrapper.emitted('save')?.[0]?.[0]).toEqual({
      ruleId: 'r1',
      recipientType: 'SELECTED_USERS',
      userIds: ['u1', 'u2'],
    });
  });

  it('loads existing policy values and filters falsy user ids', async () => {
    const wrapper = mountModal({
      preselectedRuleId: 'r1',
      existingPolicy: {
        id: 'p1',
        ruleId: 'r1',
        recipientType: 'SELECTED_USERS',
        users: [
          { userId: 'u1', displayName: 'One' },
          { userId: undefined, displayName: 'NoId' },
        ],
      },
    });
    await wrapper.setProps({ show: false });
    await wrapper.setProps({ show: true });

    await wrapper.findAll('.dx-button')[1].trigger('click');

    expect(wrapper.emitted('save')?.[0]?.[0]).toEqual({
      ruleId: 'r1',
      recipientType: 'SELECTED_USERS',
      userIds: ['u1'],
    });
  });

  it('resets and closes when show turns false, and reacts to preselected rule changes', async () => {
    const wrapper = mountModal({ show: true, preselectedRuleId: 'r1' });

    await wrapper.setProps({ preselectedRuleId: 'r1' });
    await wrapper.setProps({ show: false });
    await wrapper.find('.close-modal').trigger('click');

    expect(wrapper.emitted('close')).toBeTruthy();

    await wrapper.setProps({ show: true, preselectedRuleId: 'r1' });
    const selects = wrapper.findAll('select');
    expect(selects[0].element.value).toBe('r1');
  });
});
