import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it } from 'vitest';

import StringFieldWithPreview from '@/components/outbound/jirawebhooks/wizard/fields/StringFieldWithPreview.vue';

const baseProps = {
  value: '',
  previewText: 'Preview output',
  previewOpen: false,
  actionTipVisible: false,
  actionTipX: 0,
  actionTipY: 0,
  actionTipId: 'tip-1',
  actionTipText: '',
};

describe('StringFieldWithPreview', () => {
  const mountedWrappers: Array<ReturnType<typeof mount>> = [];

  afterEach(() => {
    mountedWrappers.splice(0).forEach(wrapper => {
      wrapper.unmount();
    });
  });

  it('emits value update and string-input on typing', async () => {
    const wrapper = mount(StringFieldWithPreview, {
      props: baseProps,
      global: {
        stubs: {
          Tooltip: true,
        },
      },
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('input.ms-input.ms-value').setValue('hello');

    expect(wrapper.emitted('update:value')?.[0]).toEqual(['hello']);
    expect(wrapper.emitted('string-input')).toBeTruthy();
  });

  it('emits open-insert and action tooltip events from add button', async () => {
    const wrapper = mount(StringFieldWithPreview, {
      props: { ...baseProps, value: 'value' },
      global: {
        stubs: {
          Tooltip: true,
        },
      },
    });
    mountedWrappers.push(wrapper);

    const addButton = wrapper.find('.ps-add-icon');
    await addButton.trigger('mouseenter', { clientX: 10, clientY: 15 });
    await addButton.trigger('mousemove', { clientX: 20, clientY: 25 });
    await addButton.trigger('mouseleave');
    await addButton.trigger('click');

    expect((wrapper.emitted('action-enter')?.[0][0] as any).text).toBe('Insert fields');
    expect(wrapper.emitted('action-move')).toBeTruthy();
    expect(wrapper.emitted('action-leave')).toBeTruthy();
    expect(wrapper.emitted('open-insert')).toBeTruthy();
  });

  it('disables preview button when value empty and emits toggle-preview when value exists', async () => {
    const wrapperEmpty = mount(StringFieldWithPreview, {
      props: baseProps,
      global: {
        stubs: {
          Tooltip: true,
        },
      },
    });
    mountedWrappers.push(wrapperEmpty);

    const emptyEye = wrapperEmpty.find('.ps-eye-icon');
    expect(emptyEye.attributes('disabled')).toBeDefined();

    const wrapperValue = mount(StringFieldWithPreview, {
      props: { ...baseProps, value: 'abc', previewOpen: true },
      global: {
        stubs: {
          Tooltip: true,
        },
      },
    });
    mountedWrappers.push(wrapperValue);

    const eye = wrapperValue.find('.ps-eye-icon');
    await eye.trigger('mouseenter', { clientX: 5, clientY: 6 });
    await eye.trigger('click');

    expect((wrapperValue.emitted('action-enter')?.[0][0] as any).text).toBe('Hide preview');
    expect(wrapperValue.emitted('toggle-preview')).toBeTruthy();
  });
});
