import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { nextTick } from 'vue';

import AppModal from '@/components/common/AppModal.vue';

describe('AppModal', () => {
  it('renders into body when open, and applies size class', async () => {
    const wrapper = mount(AppModal, {
      props: { open: true, title: 'My Modal', size: 'sm' },
      slots: { default: '<div>Body</div>' },
    });

    await nextTick();

    const backdrop = document.body.querySelector('[data-testid="kw-modal-backdrop"]');
    expect(backdrop).not.toBeNull();

    const card = document.body.querySelector('[data-testid="kw-modal-card"]');
    expect(card).not.toBeNull();
    expect(card?.classList.contains('kw-modal--sm')).toBe(true);

    expect(document.body.textContent).toContain('My Modal');
    expect(document.body.textContent).toContain('Body');

    wrapper.unmount();
  });

  it('emits update:open=false when clicking backdrop', async () => {
    const wrapper = mount(AppModal, {
      props: { open: true, title: 'Closable', closeOnOverlayClick: true },
      slots: { default: '<div>Body</div>' },
    });

    await nextTick();

    const backdrop = document.body.querySelector(
      '[data-testid="kw-modal-backdrop"]'
    ) as HTMLElement | null;
    expect(backdrop).not.toBeNull();

    backdrop?.click();
    await nextTick();

    expect(wrapper.emitted('update:open')).toBeTruthy();
    expect(wrapper.emitted('update:open')?.[0]).toEqual([false]);

    wrapper.unmount();
  });

  it('closes on Escape when enabled', async () => {
    const wrapper = mount(AppModal, {
      props: { open: true, title: 'Esc' },
    });

    await nextTick();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await nextTick();

    expect(wrapper.emitted('update:open')).toBeTruthy();

    wrapper.unmount();
  });

  it('does not close on Escape when closeOnEsc is disabled', async () => {
    const wrapper = mount(AppModal, {
      props: { open: true, title: 'Esc', closeOnEsc: false },
    });

    await nextTick();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await nextTick();

    expect(wrapper.emitted('update:open')).toBeFalsy();
    wrapper.unmount();
  });

  it('renders lg and fixed-height classes plus footer slot content', async () => {
    const wrapper = mount(AppModal, {
      props: { open: true, title: 'Large', size: 'lg', fixedHeight: true },
      slots: {
        default: '<div>Body</div>',
        footer: '<button class="footer-action">Save</button>',
      },
    });

    await nextTick();

    const card = document.body.querySelector('[data-testid="kw-modal-card"]');
    const footer = document.body.querySelector('.kw-modal__footer');

    expect(card?.classList.contains('kw-modal--lg')).toBe(true);
    expect(card?.classList.contains('kw-modal--fixed-height')).toBe(true);
    expect(footer?.textContent).toContain('Save');

    wrapper.unmount();
  });

  it('does not close on backdrop clicks when overlay closing is disabled', async () => {
    const wrapper = mount(AppModal, {
      props: { open: true, title: 'Sticky', closeOnOverlayClick: false },
      slots: { default: '<div>Body</div>' },
    });

    await nextTick();

    const backdrop = document.body.querySelector(
      '[data-testid="kw-modal-backdrop"]'
    ) as HTMLElement | null;
    backdrop?.click();
    await nextTick();

    expect(wrapper.emitted('update:open')).toBeFalsy();
    wrapper.unmount();
  });
});
