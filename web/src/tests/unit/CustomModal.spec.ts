import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { nextTick } from 'vue';

import CustomModal from '@/utils/CustomModal.vue';

describe('CustomModal', () => {
  it('renders overlay and container when open, with title and slots', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: true,
        title: 'My Modal',
        width: '600px',
        maxWidth: '80vw',
      },
      slots: {
        default: '<p>Modal content</p>',
        footer: '<button>OK</button>',
      },
    });

    await nextTick();

    const overlay = document.body.querySelector('.modal-overlay') as HTMLElement | null;
    expect(overlay).not.toBeNull();

    const container = document.body.querySelector('.modal-container') as HTMLElement | null;
    expect(container).not.toBeNull();
    expect(container!.style.width).toBe('600px');
    expect(container!.style.maxWidth).toBe('80vw');

    const titleEl = document.body.querySelector('.modal-title') as HTMLElement | null;
    expect(titleEl).not.toBeNull();
    expect(titleEl!.textContent).toBe('My Modal');

    const content = document.body.querySelector('.modal-content') as HTMLElement | null;
    expect(content).not.toBeNull();
    expect(content!.textContent).toContain('Modal content');

    const footer = document.body.querySelector('.modal-footer') as HTMLElement | null;
    expect(footer).not.toBeNull();

    // cleanup
    wrapper.unmount();
  });

  it('does not render footer when slot is not provided', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: true,
        title: 'No Footer',
      },
    });

    await nextTick();

    const footer = document.body.querySelector('.modal-footer');
    expect(footer).toBeNull();

    // cleanup
    wrapper.unmount();
  });

  it('closes when overlay is clicked if closeOnOverlay is true', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: true,
        title: 'Close Test',
        closeOnOverlay: true,
      },
    });

    await nextTick();

    const overlay = document.body.querySelector('.modal-overlay') as HTMLElement;
    overlay.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await nextTick();

    const emitted = wrapper.emitted();
    expect(emitted['update:modelValue']).toBeTruthy();
    expect(emitted['update:modelValue']![0]).toEqual([false]);
    expect(emitted['close']).toBeTruthy();

    // cleanup
    wrapper.unmount();
  });

  it('does not close when overlay is clicked if closeOnOverlay is false', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: true,
        title: 'No Close',
        closeOnOverlay: false,
      },
    });

    await nextTick();

    const overlay = document.body.querySelector('.modal-overlay') as HTMLElement;
    overlay.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await nextTick();

    const emitted = wrapper.emitted();
    expect(emitted['update:modelValue']).toBeUndefined();
    expect(emitted['close']).toBeUndefined();

    // cleanup
    wrapper.unmount();
  });

  it('does not emit close when clicking inside the container due to stop modifier', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: true,
        title: 'Stop Click',
      },
    });

    await nextTick();

    const container = document.body.querySelector('.modal-container') as HTMLElement;
    container.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await nextTick();

    const emitted = wrapper.emitted();
    expect(emitted['update:modelValue']).toBeUndefined();
    expect(emitted['close']).toBeUndefined();

    // cleanup
    wrapper.unmount();
  });

  it('closes when the close button is clicked', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: true,
        title: 'Button Close',
      },
    });

    await nextTick();

    const btn = document.body.querySelector('.modal-close-btn') as HTMLElement;
    expect(btn).not.toBeNull();
    btn.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await nextTick();

    const emitted = wrapper.emitted();
    expect(emitted['update:modelValue']).toBeTruthy();
    expect(emitted['update:modelValue']![0]).toEqual([false]);
    expect(emitted['close']).toBeTruthy();

    // cleanup
    wrapper.unmount();
  });

  it('closes on Escape key when open', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: false,
        title: 'Escape Close',
      },
    });

    // open the modal to trigger the watch
    await wrapper.setProps({ modelValue: true });
    await nextTick();

    // ensure listener attached and body scroll disabled via watch
    expect(document.body.style.overflow).toBe('hidden');

    // dispatch escape
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await nextTick();

    const emitted = wrapper.emitted();
    expect(emitted['update:modelValue']).toBeTruthy();
    expect(emitted['update:modelValue']![0]).toEqual([false]);
    expect(emitted['close']).toBeTruthy();

    // cleanup
    wrapper.unmount();
  });

  it('toggles body overflow based on open/close state', async () => {
    const wrapper = mount(CustomModal, {
      props: {
        modelValue: false,
        title: 'Overflow Toggle',
      },
    });

    // open to trigger watch side-effects
    await wrapper.setProps({ modelValue: true });
    await nextTick();
    expect(document.body.style.overflow).toBe('hidden');

    await wrapper.setProps({ modelValue: false });
    await nextTick();
    expect(document.body.style.overflow).toBe('auto');

    // reset to avoid affecting other tests
    document.body.style.overflow = '';
    wrapper.unmount();
  });
});
