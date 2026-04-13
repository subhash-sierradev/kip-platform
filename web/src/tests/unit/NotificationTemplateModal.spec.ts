import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    props: ['text', 'disabled'],
    emits: ['click'],
    template:
      '<button class="dx-button" :disabled="disabled" @click="$emit(\'click\')">{{ text }}</button>',
  },
}));

import NotificationTemplateModal from '@/components/admin/notifications/NotificationTemplateModal.vue';

const AppModalStub = {
  props: ['open', 'title'],
  emits: ['update:open'],
  template:
    '<div><div class="title">{{ title }}</div><slot /><slot name="footer" /><button class="close-modal" @click="$emit(\'update:open\', false)">close</button></div>',
};

describe('NotificationTemplateModal', () => {
  function mountModal(overrides: Record<string, unknown> = {}) {
    return mount(NotificationTemplateModal, {
      props: {
        show: true,
        isSaving: false,
        events: [
          { id: 'e1', displayName: 'Enabled', eventKey: 'A', isEnabled: true },
          { id: 'e2', displayName: 'Disabled', eventKey: 'B', isEnabled: false },
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

  it('filters to active events and emits a trimmed payload', async () => {
    const wrapper = mountModal();

    const options = wrapper
      .find('select')
      .findAll('option')
      .map(option => option.attributes('value'));
    expect(options).toContain('e1');
    expect(options).not.toContain('e2');

    await wrapper.find('select').setValue('e1');
    await wrapper.find('input').setValue('  Hello {{name}}  ');
    await wrapper.find('textarea').setValue('  Body {{value}}  ');
    await wrapper.findAll('.dx-button')[1].trigger('click');

    expect(wrapper.emitted('save')?.[0]?.[0]).toEqual({
      eventId: 'e1',
      titleTemplate: 'Hello {{name}}',
      messageTemplate: 'Body {{value}}',
    });
  });

  it('resets the form when the modal closes and emits close from the shell', async () => {
    const wrapper = mountModal();

    await wrapper.find('select').setValue('e1');
    await wrapper.find('input').setValue('Title');
    await wrapper.find('textarea').setValue('Message');
    await wrapper.setProps({ show: false });
    await wrapper.setProps({ show: true });

    expect((wrapper.find('select').element as HTMLSelectElement).value).toBe('');
    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('');
    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('');

    await wrapper.find('.close-modal').trigger('click');
    expect(wrapper.emitted('close')).toBeTruthy();
  });
});
