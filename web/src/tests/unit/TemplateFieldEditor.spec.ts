import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import TemplateFieldEditor from '@/components/outbound/jirawebhooks/wizard/TemplateFieldEditor.vue';

describe('TemplateFieldEditor', () => {
  const baseProps = {
    label: 'Summary',
    required: true,
    modelValue: 'Hello',
    placeholder: 'Enter summary',
    showPreview: false,
    previewText: '',
    target: 'summary' as const,
    actionTipId: 'tip-id',
    onActionEnter: vi.fn(),
    onActionMove: vi.fn(),
    onActionLeave: vi.fn(),
  };

  it('emits update when typing', async () => {
    const wrapper = mount(TemplateFieldEditor, {
      props: baseProps,
    });

    await wrapper.find('textarea').setValue('Hello world');
    expect(wrapper.emitted('update:modelValue')).toEqual([['Hello world']]);
  });

  it('emits open-insert when add button clicked', async () => {
    const wrapper = mount(TemplateFieldEditor, {
      props: baseProps,
    });

    await wrapper.find('.ps-add-icon').trigger('click');
    expect(wrapper.emitted('open-insert')).toEqual([['summary']]);
  });

  it('inserts placeholder at end', async () => {
    const wrapper = mount(TemplateFieldEditor, {
      props: baseProps,
    });

    const vm = wrapper.vm as { insertPlaceholder: (value: string) => Promise<void> };
    await vm.insertPlaceholder('{{foo}}');

    const updates = wrapper.emitted('update:modelValue') || [];
    const lastUpdate = updates[updates.length - 1] as string[];
    expect(lastUpdate[0]).toBe('Hello {{foo}}');
  });

  it('replaces the selected range when inserting a placeholder', async () => {
    const wrapper = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: 'Hello world',
      },
      attachTo: document.body,
    });

    const textarea = wrapper.find('textarea');
    const element = textarea.element as HTMLTextAreaElement;
    element.setSelectionRange(6, 11);
    await textarea.trigger('click');

    const vm = wrapper.vm as { insertPlaceholder: (value: string) => Promise<void> };
    await vm.insertPlaceholder('{{name}}');

    const updates = wrapper.emitted('update:modelValue') || [];
    const lastUpdate = updates[updates.length - 1] as string[];
    expect(lastUpdate[0]).toBe('Hello {{name}}');
  });

  it('does not add a leading space when inserting at the beginning', async () => {
    const wrapper = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: 'Hello',
      },
      attachTo: document.body,
    });

    const textarea = wrapper.find('textarea');
    const element = textarea.element as HTMLTextAreaElement;
    element.setSelectionRange(0, 0);
    await textarea.trigger('click');

    const vm = wrapper.vm as { insertPlaceholder: (value: string) => Promise<void> };
    await vm.insertPlaceholder('{{start}}');

    const updates = wrapper.emitted('update:modelValue') || [];
    const lastUpdate = updates[updates.length - 1] as string[];
    expect(lastUpdate[0]).toBe('{{start}}Hello');
  });

  it('shows preview text, disables preview toggle without content, and emits toggle-preview when enabled', async () => {
    const wrapperDisabled = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: '   ',
        showPreview: true,
        previewText: '',
      },
    });

    expect(wrapperDisabled.text()).toContain('Preview is empty');
    expect(wrapperDisabled.find('.ps-eye-icon').attributes('disabled')).toBeDefined();

    const wrapperEnabled = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: 'Hello',
        showPreview: true,
        previewText: 'Rendered preview',
      },
    });

    expect(wrapperEnabled.text()).toContain('Rendered preview');
    await wrapperEnabled.find('.ps-eye-icon').trigger('click');
    expect(wrapperEnabled.emitted('toggle-preview')).toEqual([[]]);
  });

  it('forwards tooltip text for add and preview actions', async () => {
    const onActionEnter = vi.fn();
    const wrapper = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: 'Hello',
        onActionEnter,
      },
    });

    await wrapper.find('.ps-add-icon').trigger('mouseenter');
    await wrapper.find('.ps-eye-icon').trigger('mouseenter');
    await nextTick();

    expect(onActionEnter).toHaveBeenNthCalledWith(1, 'Insert fields', expect.any(MouseEvent));
    expect(onActionEnter).toHaveBeenNthCalledWith(2, 'Show preview', expect.any(MouseEvent));
  });

  it('forwards the hide-preview tooltip text when preview mode is active', async () => {
    const onActionEnter = vi.fn();
    const wrapper = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: 'Hello',
        showPreview: true,
        previewText: 'Rendered preview',
        onActionEnter,
      },
    });

    await wrapper.find('.ps-eye-icon').trigger('mouseenter');
    await nextTick();

    expect(onActionEnter).toHaveBeenCalledWith('Hide preview', expect.any(MouseEvent));
  });

  it('does not add an extra space when the insertion point already follows whitespace', async () => {
    const wrapper = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: 'Hello ',
      },
      attachTo: document.body,
    });

    const textarea = wrapper.find('textarea');
    const element = textarea.element as HTMLTextAreaElement;
    element.setSelectionRange(6, 6);
    await textarea.trigger('keyup');

    const vm = wrapper.vm as { insertPlaceholder: (value: string) => Promise<void> };
    await vm.insertPlaceholder('{{name}}');

    const updates = wrapper.emitted('update:modelValue') || [];
    const lastUpdate = updates[updates.length - 1] as string[];
    expect(lastUpdate[0]).toBe('Hello {{name}}');
  });

  it('appends the placeholder when no selection has been captured yet', async () => {
    const wrapper = mount(TemplateFieldEditor, {
      props: {
        ...baseProps,
        modelValue: 'Hello',
      },
    });

    const vm = wrapper.vm as { insertPlaceholder: (value: string) => Promise<void> };
    await vm.insertPlaceholder('{{tail}}');

    const updates = wrapper.emitted('update:modelValue') || [];
    const lastUpdate = updates[updates.length - 1] as string[];
    expect(lastUpdate[0]).toBe('Hello {{tail}}');
  });
});
