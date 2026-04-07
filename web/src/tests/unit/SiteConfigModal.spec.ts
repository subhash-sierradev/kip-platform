/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import { defineComponent, h, nextTick } from 'vue';

// Mock DevExtreme DateBox
vi.mock('devextreme-vue/date-box', () => ({
  DxDateBox: {
    name: 'DxDateBox',
    template: '<div class="dx-datebox-mock"><input class="dx-texteditor-input" /></div>',
    props: ['value', 'type', 'displayFormat', 'useMaskBehavior', 'showClearButton', 'placeholder'],
    emits: ['value-changed'],
  },
}));

import SiteConfigModal from '@/components/admin/siteconfig/SiteConfigModal.vue';

const AppModalStub = {
  template:
    '<div class="app-modal"><div class="modal-title">{{ title }}</div><slot /><slot name="footer" /></div>',
  props: ['open', 'title', 'size', 'closeOnOverlayClick'],
  emits: ['update:open'],
};

const DxButtonStub = defineComponent({
  name: 'DxButton',
  props: ['text', 'disabled', 'type', 'stylingMode'],
  emits: ['click'],
  setup(props, { emit }) {
    return () => {
      const isCancel = (props.text as string) === 'Cancel';
      const isSavingText = (props.text as string) === 'Saving...';
      return h(
        'button',
        {
          class: ['dx-button', isCancel ? 'cancel-btn' : 'save-btn'],
          disabled: props.disabled || undefined,
          onClick: () => emit('click'),
        },
        [isSavingText ? h('span', { class: 'loading-spinner' }) : null, props.text as string]
      );
    };
  },
});

describe('SiteConfigModal', () => {
  describe('Modal Rendering and Props', () => {
    it('renders edit mode title', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: {
            id: 'test-key',
            value: 'test-value',
            type: 'STRING',
            description: 'Test description',
          },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      expect(wrapper.text()).toContain('Edit Configuration');
    });

    it('renders with show prop controlling modal visibility', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      expect(wrapper.findComponent(AppModalStub).props('open')).toBe(true);
    });

    it('disables overlay click close behavior', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      expect(wrapper.findComponent(AppModalStub).props('closeOnOverlayClick')).toBe(false);
    });

    it('displays error message when saveError is provided', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: 'Failed to save configuration',
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      expect(wrapper.find('.error-message').exists()).toBe(true);
      expect(wrapper.find('.error-message').text()).toBe('Failed to save configuration');
    });

    it('does not display error message when saveError is null', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      expect(wrapper.find('.error-message').exists()).toBe(false);
    });

    it('disables key input field in edit mode', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'test-key', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const keyInput = wrapper.find('input.form-input[type="text"]');
      expect(keyInput.attributes('disabled')).toBeDefined();
      expect(keyInput.attributes('readonly')).toBeDefined();
    });

    it('disables type select field in edit mode', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'test-key', value: 'v', type: 'NUMBER', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const typeSelect = wrapper.find('select.form-select');
      expect(typeSelect.attributes('disabled')).toBeDefined();
    });

    it('disables description textarea', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'Read-only description' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const descTextarea = wrapper.findAll('textarea.form-textarea')[0];
      expect(descTextarea.attributes('disabled')).toBeDefined();
      expect(descTextarea.attributes('readonly')).toBeDefined();
    });
  });

  describe('Field Type Rendering - STRING', () => {
    it('renders text input for STRING type', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'key1', value: 'string value', type: 'STRING', description: 'desc' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const valueInput = wrapper.findAll('input.form-input[type="text"]')[1]; // Second text input (first is key)
      expect(valueInput.exists()).toBe(true);
      expect((valueInput.element as HTMLInputElement).value).toBe('string value');
    });

    it('emits update:value when STRING input changes', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'old', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const valueInput = wrapper.findAll('input.form-input[type="text"]')[1];
      await valueInput.setValue('new string value');

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual(['new string value']);
    });
  });

  describe('Field Type Rendering - NUMBER', () => {
    it('renders number input for NUMBER type', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'key2', value: '42', type: 'NUMBER', description: 'desc' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const numberInput = wrapper.find('input.form-input[type="number"]');
      expect(numberInput.exists()).toBe(true);
      expect((numberInput.element as HTMLInputElement).value).toBe('42');
    });

    it('emits update:value when NUMBER input changes', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: '10', type: 'NUMBER', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const numberInput = wrapper.find('input.form-input[type="number"]');
      await numberInput.setValue('999');

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual(['999']);
    });
  });

  describe('Field Type Rendering - BOOLEAN', () => {
    it('renders text input for BOOLEAN type', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'key3', value: 'true', type: 'BOOLEAN', description: 'desc' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const boolInput = wrapper.findAll('input.form-input[type="text"]')[1];
      expect(boolInput.exists()).toBe(true);
      expect((boolInput.element as HTMLInputElement).value).toBe('true');
      expect(boolInput.attributes('placeholder')).toBe('true or false');
    });

    it('emits update:value when BOOLEAN input changes', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'false', type: 'BOOLEAN', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const boolInput = wrapper.findAll('input.form-input[type="text"]')[1];
      await boolInput.setValue('true');

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual(['true']);
    });
  });

  describe('Field Type Rendering - JSON', () => {
    it('renders textarea for JSON type', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'key4', value: '{"key":"value"}', type: 'JSON', description: 'desc' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      // For JSON type, the value textarea is the first one (description is second)
      const jsonTextarea = wrapper.findAll('textarea.form-textarea')[0];
      expect(jsonTextarea.exists()).toBe(true);
      expect((jsonTextarea.element as HTMLTextAreaElement).value).toBe('{"key":"value"}');
      expect(jsonTextarea.attributes('placeholder')).toBe('Enter valid JSON');
    });

    it('emits update:value when JSON textarea changes', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: '{}', type: 'JSON', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const jsonTextarea = wrapper.findAll('textarea.form-textarea')[0];
      await jsonTextarea.setValue('{"updated":"data"}');

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual(['{"updated":"data"}']);
    });
  });

  describe('Field Type Rendering - TIMESTAMP', () => {
    it('renders DxDateBox for TIMESTAMP type', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: {
            id: 'key5',
            value: '2025-01-15T10:00:00Z',
            type: 'TIMESTAMP',
            description: 'desc',
          },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const datebox = wrapper.findComponent({ name: 'DxDateBox' });
      expect(datebox.exists()).toBe(true);
      expect(datebox.props('value')).toBe('2025-01-15T10:00:00Z');
    });

    it('emits update:value with ISO string when TIMESTAMP changes with Date object', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: '2025-01-01T00:00:00Z', type: 'TIMESTAMP', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const datebox = wrapper.findComponent({ name: 'DxDateBox' });
      const newDate = new Date('2025-12-31T23:59:59Z');
      datebox.vm.$emit('value-changed', { value: newDate });
      await nextTick();

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual([newDate.toISOString()]);
    });

    it('emits update:value with string when TIMESTAMP changes with string value', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: '2025-01-01T00:00:00Z', type: 'TIMESTAMP', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const datebox = wrapper.findComponent({ name: 'DxDateBox' });
      datebox.vm.$emit('value-changed', { value: '2025-06-15T12:00:00Z' });
      await nextTick();

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual(['2025-06-15T12:00:00Z']);
    });

    it('emits empty string when TIMESTAMP is cleared', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: '2025-01-01T00:00:00Z', type: 'TIMESTAMP', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const datebox = wrapper.findComponent({ name: 'DxDateBox' });
      datebox.vm.$emit('value-changed', { value: null });
      await nextTick();

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual(['']);
    });
  });

  describe('Button Actions and States', () => {
    it('emits close when Cancel button clicked', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      const cancelBtn = wrapper.find('.cancel-btn');
      await cancelBtn.trigger('click');

      expect(wrapper.emitted('close')).toBeTruthy();
    });

    it('emits save when Save button clicked', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      const saveBtn = wrapper.find('.save-btn');
      await saveBtn.trigger('click');

      expect(wrapper.emitted('save')).toBeTruthy();
    });

    it('disables buttons when isSaving is true', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: true,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      const cancelBtn = wrapper.find('.cancel-btn');
      const saveBtn = wrapper.find('.save-btn');

      expect(cancelBtn.attributes('disabled')).toBeDefined();
      expect(saveBtn.attributes('disabled')).toBeDefined();
    });

    it('disables Update button when required fields are missing', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: '', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      const cancelBtn = wrapper.find('.cancel-btn');
      const saveBtn = wrapper.find('.save-btn');

      expect(cancelBtn.attributes('disabled')).toBeUndefined();
      expect(saveBtn.attributes('disabled')).toBeDefined();
    });

    it('shows "Saving..." text when isSaving is true', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: true,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      expect(wrapper.find('.save-btn').text()).toContain('Saving...');
    });

    it('shows "Update Changes" text when isSaving is false', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      expect(wrapper.find('.save-btn').text()).toContain('Update Changes');
    });

    it('renders loading spinner when isSaving is true', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: true,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      expect(wrapper.find('.loading-spinner').exists()).toBe(true);
    });

    it('does not render loading spinner when isSaving is false', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub, DxButton: DxButtonStub } },
      });

      expect(wrapper.find('.loading-spinner').exists()).toBe(false);
    });
  });

  describe('Form Display and Values', () => {
    it('displays all form fields with correct labels', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: {
            id: 'test-key',
            value: 'test-value',
            type: 'STRING',
            description: 'Test description',
          },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      expect(wrapper.text()).toContain('Key');
      expect(wrapper.text()).toContain('Type');
      expect(wrapper.text()).toContain('Value');
      expect(wrapper.text()).toContain('Description');
    });

    it('renders key field value from formData', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'my-config-key', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const keyInput = wrapper.find('input.form-input[type="text"]');
      expect((keyInput.element as HTMLInputElement).value).toBe('my-config-key');
    });

    it('renders type field value from formData', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'BOOLEAN', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const typeSelect = wrapper.find('select.form-select');
      expect((typeSelect.element as HTMLSelectElement).value).toBe('BOOLEAN');
    });

    it('renders description field value from formData', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'My config description' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const descTextarea = wrapper.findAll('textarea.form-textarea')[0];
      expect((descTextarea.element as HTMLTextAreaElement).value).toBe('My config description');
    });

    it('renders all type options in select', () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const typeSelect = wrapper.find('select.form-select');
      const options = typeSelect.findAll('option');

      expect(options.length).toBe(5);
      expect(options[0].attributes('value')).toBe('STRING');
      expect(options[1].attributes('value')).toBe('NUMBER');
      expect(options[2].attributes('value')).toBe('BOOLEAN');
      expect(options[3].attributes('value')).toBe('JSON');
      expect(options[4].attributes('value')).toBe('TIMESTAMP');
    });
  });

  describe('Event Handler Edge Cases', () => {
    it('handles TIMESTAMP update with undefined value', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: '2025-01-01T00:00:00Z', type: 'TIMESTAMP', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const datebox = wrapper.findComponent({ name: 'DxDateBox' });
      datebox.vm.$emit('value-changed', { value: undefined });
      await nextTick();

      expect(wrapper.emitted('update:value')).toBeTruthy();
      expect(wrapper.emitted('update:value')![0]).toEqual(['']);
    });

    it('handles handleIdUpdate with event target without value property', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'test-key', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const vm = wrapper.vm as any;
      // Call handler with event that has no value property
      vm.handleIdUpdate({ target: {} });

      // Should not emit update:id
      expect(wrapper.emitted('update:id')).toBeFalsy();
    });

    it('handles handleIdUpdate with null event target', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'test-key', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const vm = wrapper.vm as any;
      // Call handler with event that has null target
      vm.handleIdUpdate({ target: null });

      // Should not emit update:id
      expect(wrapper.emitted('update:id')).toBeFalsy();
    });

    it('handles handleTypeUpdate with event target without value property', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const vm = wrapper.vm as any;
      vm.handleTypeUpdate({ target: {} });

      expect(wrapper.emitted('update:type')).toBeFalsy();
    });

    it('handles handleTypeUpdate with null event target', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const vm = wrapper.vm as any;
      vm.handleTypeUpdate({ target: null });

      expect(wrapper.emitted('update:type')).toBeFalsy();
    });

    it('handles handleValueUpdate with event target without value property', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const vm = wrapper.vm as any;
      vm.handleValueUpdate({ target: {} });

      expect(wrapper.emitted('update:value')).toBeFalsy();
    });

    it('handles handleValueUpdate with null event target', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const vm = wrapper.vm as any;
      vm.handleValueUpdate({ target: null });

      expect(wrapper.emitted('update:value')).toBeFalsy();
    });

    it('emits close when AppModal emits update:open with false', async () => {
      const wrapper = mount(SiteConfigModal, {
        props: {
          show: true,
          isSaving: false,
          saveError: null,
          formData: { id: 'k', value: 'v', type: 'STRING', description: 'd' },
        },
        global: { stubs: { AppModal: AppModalStub } },
      });

      const appModal = wrapper.findComponent(AppModalStub);
      appModal.vm.$emit('update:open', false);
      await nextTick();

      expect(wrapper.emitted('close')).toBeTruthy();
    });
  });
});
