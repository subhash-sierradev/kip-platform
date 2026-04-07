import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

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
});
