import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import { NotificationEntityType } from '@/api';
import NotificationEventModal from '@/components/admin/notifications/NotificationEventModal.vue';

vi.mock('@/components/common/AppModal.vue', () => ({
  default: {
    name: 'AppModal',
    template:
      '<div class="app-modal"><button class="modal-close" @click="$emit(\'update:open\', false)">close</button><slot /><slot name="footer" /></div>',
    props: ['open', 'title', 'size'],
    emits: ['update:open'],
  },
}));

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ text }}</button>',
    props: ['text', 'disabled', 'stylingMode', 'type'],
    emits: ['click'],
  },
}));

vi.mock('devextreme-vue/select-box', () => ({
  DxSelectBox: {
    name: 'DxSelectBox',
    template:
      '<select class="entity-type-select" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option value=""></option><option v-for="item in items" :key="item" :value="item">{{ item }}</option></select>',
    props: ['modelValue', 'items', 'placeholder', 'showClearButton'],
    emits: ['update:modelValue'],
  },
}));

describe('NotificationEventModal', () => {
  it('normalizes the save payload before emitting it', async () => {
    const wrapper = mount(NotificationEventModal, {
      props: {
        show: true,
        isSaving: false,
      },
    });

    const inputs = wrapper.findAll('input');
    await inputs[0].setValue(' integration_failed ');
    await wrapper.find('.entity-type-select').setValue(NotificationEntityType.ARCGIS_INTEGRATION);
    await inputs[1].setValue(' Integration Failed ');
    await wrapper.find('textarea').setValue('   ');

    await wrapper
      .findAll('button')
      .find(button => button.text() === 'Save')
      ?.trigger('click');

    expect(wrapper.emitted('save')).toEqual([
      [
        {
          eventKey: 'INTEGRATION_FAILED',
          entityType: NotificationEntityType.ARCGIS_INTEGRATION,
          displayName: 'Integration Failed',
          description: undefined,
        },
      ],
    ]);
  });

  it('resets the form when hidden and emits close when the modal requests it', async () => {
    const wrapper = mount(NotificationEventModal, {
      props: {
        show: true,
        isSaving: false,
      },
    });

    const inputs = wrapper.findAll('input');
    await inputs[0].setValue('event_key');
    await wrapper.find('.entity-type-select').setValue(NotificationEntityType.SITE_CONFIG);
    await inputs[1].setValue('Display Name');
    await wrapper.find('textarea').setValue('Description');

    await wrapper.find('.modal-close').trigger('click');
    expect(wrapper.emitted('close')).toHaveLength(1);

    await wrapper.setProps({ show: false });
    await wrapper.setProps({ show: true });

    expect(wrapper.findAll('input')[0].element.value).toBe('');
    expect((wrapper.find('.entity-type-select').element as HTMLSelectElement).value).toBe('');
    expect(wrapper.findAll('input')[1].element.value).toBe('');
    expect(wrapper.find('textarea').element.value).toBe('');
  });

  it('emits close when the cancel button is clicked', async () => {
    const wrapper = mount(NotificationEventModal, {
      props: {
        show: true,
        isSaving: false,
      },
    });

    await wrapper
      .findAll('button')
      .find(button => button.text() === 'Cancel')
      ?.trigger('click');

    expect(wrapper.emitted('close')).toHaveLength(1);
  });
});
