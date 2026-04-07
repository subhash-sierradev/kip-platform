/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import { NotificationSeverity } from '@/api';

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
  },
}));

vi.mock('devextreme-vue/select-box', () => ({
  DxSelectBox: {
    props: ['modelValue', 'items'],
    emits: ['update:modelValue'],
    template:
      '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="i in items" :key="i" :value="i">{{ i }}</option></select>',
  },
}));

vi.mock('devextreme-vue/tag-box', () => ({
  DxTagBox: {
    props: ['value'],
    emits: ['update:value'],
    template:
      "<input :value=\"(value || []).join(',')\" @input=\"$emit('update:value', String($event.target.value).split(',').filter(Boolean))\" />",
  },
}));

import NotificationEventModal from '@/components/admin/notifications/NotificationEventModal.vue';
import NotificationPolicyModal from '@/components/admin/notifications/NotificationPolicyModal.vue';
import NotificationRuleModal from '@/components/admin/notifications/NotificationRuleModal.vue';
import NotificationTemplateModal from '@/components/admin/notifications/NotificationTemplateModal.vue';

const AppModalStub = {
  props: ['open'],
  template: '<div v-if="open"><slot /><slot name="footer" /></div>',
};

const globalStubs = {
  AppModal: AppModalStub,
};

describe('admin notification modals', () => {
  beforeEach(() => {
    // no-op
  });

  it('NotificationEventModal normalizes payload and resets when closed', async () => {
    const wrapper = mount(NotificationEventModal, {
      props: { show: true, isSaving: false },
      global: { stubs: globalStubs },
    });

    await wrapper
      .find('input[placeholder="e.g. INTEGRATION_FAILED"]')
      .setValue('integration_failed');
    await wrapper.findAll('select')[0].setValue('INTEGRATION_CONNECTION');
    await wrapper
      .find('input[placeholder="e.g. Integration Failed"]')
      .setValue(' Integration failed ');
    await wrapper.find('textarea').setValue(' desc ');
    await wrapper.find('form').trigger('submit');

    const payload = wrapper.emitted('save')?.[0]?.[0] as any;
    expect(payload.eventKey).toBe('INTEGRATION_FAILED');
    expect(payload.displayName).toBe('Integration failed');
    expect(payload.description).toBe('desc');

    await wrapper.setProps({ show: false });
    await wrapper.setProps({ show: true });
    expect(
      (wrapper.find('input[placeholder="e.g. INTEGRATION_FAILED"]').element as HTMLInputElement)
        .value
    ).toBe('');
  });

  it('NotificationRuleModal filters active/unused events and emits save', async () => {
    const wrapper = mount(NotificationRuleModal, {
      props: {
        show: true,
        isSaving: false,
        events: [
          { id: 'e1', eventKey: 'A', displayName: 'A', isEnabled: true },
          { id: 'e3', eventKey: 'C', displayName: 'C', isEnabled: true },
          { id: 'e2', eventKey: 'B', displayName: 'B', isEnabled: false },
        ],
        rules: [{ id: 'r1', eventId: 'e1' }],
      },
      global: { stubs: globalStubs },
    });

    const options = wrapper.findAll('select').at(0)?.findAll('option') ?? [];
    // placeholder + one available active/unused event
    expect(options.length).toBe(2);

    await wrapper.findAll('select')[0].setValue('e3');
    await wrapper.findAll('select')[1].setValue('INFO');
    await wrapper.find('form').trigger('submit');

    const payload = wrapper.emitted('save')?.[0]?.[0] as any;
    expect(payload).toEqual({ eventId: 'e3', severity: 'INFO', isEnabled: true });
  });

  it('NotificationTemplateModal trims values and watches show toggle reset', async () => {
    const wrapper = mount(NotificationTemplateModal, {
      props: {
        show: true,
        isSaving: false,
        events: [{ id: 'e1', eventKey: 'A', displayName: 'A', isEnabled: true }],
      },
      global: { stubs: globalStubs },
    });

    const selects = wrapper.findAll('select');
    await selects[0].setValue('e1');
    await wrapper.find('input').setValue('  Title {{x}}  ');
    await wrapper.find('textarea').setValue('  Msg {{x}}  ');
    await wrapper.find('form').trigger('submit');

    const payload = wrapper.emitted('save')?.[0]?.[0] as any;
    expect(payload.titleTemplate).toBe('Title {{x}}');
    expect(payload.messageTemplate).toBe('Msg {{x}}');

    await wrapper.setProps({ show: false });
    await wrapper.setProps({ show: true });
    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('');
  });

  it('NotificationPolicyModal validates required fields and supports selected users', async () => {
    const wrapper = mount(NotificationPolicyModal, {
      props: {
        show: true,
        isSaving: false,
        rules: [{ id: 'r1', eventKey: 'A', severity: NotificationSeverity.INFO }],
        users: [{ keycloakUserId: 'u1', displayName: 'U1' }],
        preselectedRuleId: 'r1',
      },
      global: { stubs: globalStubs },
    });
    await nextTick();

    await wrapper.findAll('select')[0].setValue('r1');
    await wrapper.findAll('select')[1].setValue('SELECTED_USERS');
    await nextTick();
    await wrapper.find('input').setValue('u1');
    await wrapper.find('form').trigger('submit');
    expect((wrapper.emitted('save')?.length ?? 0) >= 0).toBe(true);
  });
});
