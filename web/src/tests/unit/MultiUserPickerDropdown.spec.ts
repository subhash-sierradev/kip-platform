import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it } from 'vitest';
import { nextTick } from 'vue';

import MultiUserPickerDropdown from '@/components/outbound/jirawebhooks/wizard/fields/MultiUserPickerDropdown.vue';

const users = [
  { accountId: 'u1', displayName: 'Alice' },
  { accountId: 'u2', displayName: 'Bob' },
] as any;

describe('MultiUserPickerDropdown', () => {
  const mountedWrappers: Array<ReturnType<typeof mount>> = [];

  afterEach(() => {
    mountedWrappers.splice(0).forEach(wrapper => {
      wrapper.unmount();
    });
  });

  it('shows selected user names when value is provided', () => {
    const wrapper = mount(MultiUserPickerDropdown, {
      props: {
        open: false,
        placement: 'down',
        selectedValue: 'u1,u2',
        users,
      },
    });
    mountedWrappers.push(wrapper);

    expect(wrapper.find('.ms-multiuser-trigger .value').text()).toContain('Alice');
    expect(wrapper.find('.ms-multiuser-trigger .value').text()).toContain('Bob');
  });

  it('emits open toggle and placement calculation on trigger click', async () => {
    const wrapper = mount(MultiUserPickerDropdown, {
      props: {
        open: false,
        placement: 'down',
        selectedValue: '',
        users,
      },
      attachTo: document.body,
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('.ms-multiuser-trigger').trigger('click');

    expect(wrapper.emitted('update:open')?.[0]).toEqual([true]);

    // Simulate the parent updating the prop (triggers the watch → decideUserDropdownPlacement)
    await wrapper.setProps({ open: true });
    await nextTick();

    expect(wrapper.emitted('update:placement')).toBeTruthy();
  });

  it('supports keyboard semantics and Space key toggle on trigger', async () => {
    const wrapper = mount(MultiUserPickerDropdown, {
      props: {
        open: false,
        placement: 'down',
        selectedValue: '',
        users,
      },
      attachTo: document.body,
    });
    mountedWrappers.push(wrapper);

    const trigger = wrapper.find('.ms-multiuser-trigger');
    expect(trigger.attributes('role')).toBe('button');
    expect(trigger.attributes('tabindex')).toBe('0');
    expect(trigger.attributes('aria-haspopup')).toBe('dialog');
    expect(trigger.attributes('aria-expanded')).toBe('false');

    await trigger.trigger('keydown', { key: ' ' });
    expect(wrapper.emitted('update:open')?.[0]).toEqual([true]);
  });

  it('filters users and emits toggle-user on checkbox change', async () => {
    const wrapper = mount(MultiUserPickerDropdown, {
      props: {
        open: true,
        placement: 'down',
        selectedValue: '',
        users,
      },
      attachTo: document.body,
    });
    mountedWrappers.push(wrapper);

    const searchInput = wrapper.find('.ms-multiuser-search');
    await searchInput.setValue('Ali');

    const items = wrapper.findAll('.ms-multiuser-item');
    expect(items.length).toBe(1);
    expect(items[0].text()).toContain('Alice');

    await items[0].find('input[type="checkbox"]').setValue(true);
    expect(wrapper.emitted('toggle-user')?.[0]).toEqual(['u1']);
  });

  it('emits close when escape is pressed and when clicking outside', async () => {
    const wrapper = mount(MultiUserPickerDropdown, {
      props: {
        open: true,
        placement: 'down',
        selectedValue: '',
        users,
      },
      attachTo: document.body,
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('.ms-multiuser-search').trigger('keydown', { key: 'Escape' });

    document.body.dispatchEvent(new MouseEvent('click', { bubbles: true }));

    const closeEvents = wrapper.emitted('update:open') || [];
    expect(closeEvents.some(e => e[0] === false)).toBe(true);
  });
});
