import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ConfirmDialog from '@/components/outbound/jirawebhooks/common/ConfirmDialog.vue';

describe('Outbound ConfirmDialog.vue', () => {
  const defaultProps = {
    open: true,
    title: 'Delete Webhook',
    message: 'Are you sure?\nThis action cannot be undone.',
    confirmText: 'Delete',
    cancelText: 'Cancel',
  };

  it('renders when open with title, message lines, and buttons', () => {
    const wrapper = mount(ConfirmDialog, { props: defaultProps });

    const overlay = wrapper.find('.confirm');
    expect(overlay.exists()).toBe(true);

    const title = wrapper.find('h4');
    expect(title.text()).toBe('Delete Webhook');

    const lines = wrapper.findAll('p.msg');
    expect(lines.length).toBe(2);
    expect(lines[0].text()).toBe('Are you sure?');
    expect(lines[1].text()).toBe('This action cannot be undone.');

    const buttons = wrapper.findAll('.row .btn');
    expect(buttons.length).toBe(2);
    expect(buttons[0].text()).toBe('Delete');
    expect(buttons[1].text()).toBe('Cancel');
    expect(buttons[1].classes()).toContain('warn');
  });

  it('does not render when open is false', () => {
    const wrapper = mount(ConfirmDialog, { props: { ...defaultProps, open: false } });
    expect(wrapper.find('.confirm').exists()).toBe(false);
  });

  it('emits confirm on primary button click and cancel on secondary', async () => {
    const wrapper = mount(ConfirmDialog, { props: defaultProps });
    const confirmBtn = wrapper.find('.row .btn:not(.warn)');
    const cancelBtn = wrapper.find('.row .btn.warn');

    await confirmBtn.trigger('click');
    await cancelBtn.trigger('click');

    expect(wrapper.emitted('confirm')).toBeTruthy();
    expect(wrapper.emitted('cancel')).toBeTruthy();
    expect(wrapper.emitted('confirm')?.length).toBe(1);
    expect(wrapper.emitted('cancel')?.length).toBe(1);
  });

  it('handles single-line message', () => {
    const wrapper = mount(ConfirmDialog, {
      props: { ...defaultProps, message: 'Single line' },
    });
    const lines = wrapper.findAll('p.msg');
    expect(lines.length).toBe(1);
    expect(lines[0].text()).toBe('Single line');
  });

  it('supports special characters in message and labels', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        open: true,
        title: 'Confirm & Proceed',
        message: 'Use < > & " \' safely',
        confirmText: 'Yes & OK',
        cancelText: 'No <Cancel>',
      },
    });
    expect(wrapper.find('h4').text()).toBe('Confirm & Proceed');
    const lines = wrapper.findAll('p.msg');
    expect(lines.length).toBe(1);
    expect(lines[0].text()).toBe('Use < > & " \' safely');
    const buttons = wrapper.findAll('.row .btn');
    expect(buttons[0].text()).toBe('Yes & OK');
    expect(buttons[1].text()).toBe('No <Cancel>');
  });
});
