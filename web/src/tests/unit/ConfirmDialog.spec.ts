import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import ConfirmDialog from '@/components/common/ConfirmationDialog.vue';

describe('ConfirmDialog', () => {
  const defaultProps = {
    open: true,
    title: 'Confirm Action',
    description: 'Are you sure you want to proceed?',
    confirmLabel: 'Yes',
    cancelLabel: 'No',
  };

  beforeEach(() => {
    // Clear any existing dialogs from the DOM
    const existingDialogs = document.querySelectorAll('.confirm-overlay');
    existingDialogs.forEach(dialog => dialog.remove());
  });

  afterEach(() => {
    // Clean up after each test
    const existingDialogs = document.querySelectorAll('.confirm-overlay');
    existingDialogs.forEach(dialog => dialog.remove());
  });

  it('renders correctly when open', () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps,
    });

    // Check teleported content in document body
    expect(document.querySelector('.confirm-overlay')).toBeTruthy();
    expect(document.querySelector('h3')?.textContent).toBe('Confirm Action');
    expect(document.querySelector('.confirm-description')?.textContent).toBe(
      'Are you sure you want to proceed?'
    );
    const buttons = document.querySelectorAll('.confirm-actions button');
    expect(buttons[0]?.textContent).toBe('No'); // Cancel button comes first
    expect(buttons[1]?.textContent?.trim()).toBe('Yes'); // Confirm button comes second

    wrapper.unmount();
  });

  it('does not render when open is false', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        open: false,
      },
    });

    expect(document.querySelector('.confirm-overlay')).toBeFalsy();
    wrapper.unmount();
  });

  it('handles multiline messages correctly', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        description: 'Line 1\nLine 2\nLine 3',
      },
    });

    const description = document.querySelector('.confirm-description');
    expect(description).toBeTruthy();
    expect(description?.textContent).toBe('Line 1\nLine 2\nLine 3');
    wrapper.unmount();
  });

  it('emits confirm event when confirm button is clicked', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps,
    });

    const confirmButton = document.querySelectorAll('.confirm-actions button')[1];
    confirmButton?.dispatchEvent(new Event('click'));

    await wrapper.vm.$nextTick();
    expect(wrapper.emitted('confirm')).toBeTruthy();
    expect(wrapper.emitted('confirm')).toHaveLength(1);
    wrapper.unmount();
  });

  it('emits cancel event when cancel button is clicked', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps,
    });

    const cancelButton = document.querySelectorAll('.confirm-actions button')[0];
    cancelButton?.dispatchEvent(new Event('click'));

    await wrapper.vm.$nextTick();
    expect(wrapper.emitted('cancel')).toBeTruthy();
    expect(wrapper.emitted('cancel')).toHaveLength(1);
    wrapper.unmount();
  });

  it('applies correct CSS classes', () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps,
    });

    expect(document.querySelector('.confirm-overlay')).toBeTruthy();
    expect(document.querySelector('.confirm-card')).toBeTruthy();
    expect(document.querySelector('.confirm-actions')).toBeTruthy();
    wrapper.unmount();
  });

  it('handles single line message', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        description: 'Single line message',
      },
    });

    const description = document.querySelector('.confirm-description');
    expect(description).toBeTruthy();
    expect(description?.textContent).toBe('Single line message');
    wrapper.unmount();
  });

  it('renders custom button text correctly', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        cancelLabel: 'Keep',
        confirmLabel: 'Delete',
      },
    });

    const buttons = document.querySelectorAll('.confirm-actions button');
    expect(buttons[0]?.textContent).toBe('Keep'); // Cancel button
    expect(buttons[1]?.textContent?.trim()).toBe('Delete'); // Confirm button
    wrapper.unmount();
  });

  it('has accessible structure', () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps,
    });

    expect(document.querySelector('h3')).toBeTruthy();
    expect(document.querySelectorAll('.confirm-actions button')).toHaveLength(2);
    // Check for ARIA attributes
    const dialog = document.querySelector('[role="dialog"]');
    expect(dialog).toBeTruthy();
    expect(dialog?.getAttribute('aria-modal')).toBe('true');
    wrapper.unmount();
  });

  it('prevents multiple emissions on rapid clicks', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { ...defaultProps, loading: false },
    });

    const confirmButton = document.querySelectorAll('.confirm-actions button')[1];
    confirmButton?.dispatchEvent(new Event('click'));
    confirmButton?.dispatchEvent(new Event('click'));
    confirmButton?.dispatchEvent(new Event('click'));

    await wrapper.vm.$nextTick();
    expect(wrapper.emitted('confirm')).toHaveLength(3); // Each click should emit
    wrapper.unmount();
  });

  it('handles long messages appropriately', () => {
    const longMessage =
      'This is a very long message that should be displayed correctly in the confirmation dialog without breaking the layout or causing accessibility issues.';
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        description: longMessage,
      },
    });

    expect(document.querySelector('.confirm-description')?.textContent).toBe(longMessage);
    wrapper.unmount();
  });

  it('handles special characters in messages', () => {
    const specialMessage = 'Message with & < > " \' special characters!';
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        description: specialMessage,
      },
    });

    expect(document.querySelector('.confirm-description')?.textContent).toBe(specialMessage);
    wrapper.unmount();
  });
});
