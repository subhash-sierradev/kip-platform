import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it } from 'vitest';

import FieldSelectorDropdown from '@/components/outbound/jirawebhooks/wizard/fields/FieldSelectorDropdown.vue';

const fields = [
  { key: 'cf_string', name: 'String Field', type: 'string' },
  { key: 'cf_number', name: 'Number Field', type: 'number' },
] as any;

describe('FieldSelectorDropdown', () => {
  const mountedWrappers: Array<ReturnType<typeof mount>> = [];

  afterEach(() => {
    mountedWrappers.splice(0).forEach(wrapper => {
      wrapper.unmount();
    });
  });

  it('emits open/query reset and placement on trigger click', async () => {
    const wrapper = mount(FieldSelectorDropdown, {
      props: {
        query: 'Existing',
        dropdownOpen: false,
        dropdownPlacement: 'down',
        filteredFields: fields,
      },
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('.ms-select-trigger').trigger('click');

    expect(wrapper.emitted('update:dropdownOpen')?.[0]).toEqual([true]);
    expect(wrapper.emitted('update:query')?.[0]).toEqual(['']);
    expect(wrapper.emitted('placement-change')).toBeTruthy();
  });

  it('emits query update while dropdown is open', async () => {
    const wrapper = mount(FieldSelectorDropdown, {
      props: {
        query: '',
        dropdownOpen: true,
        dropdownPlacement: 'down',
        filteredFields: fields,
      },
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('.ms-search-visible').setValue('abc');

    expect(wrapper.emitted('update:query')?.[0]).toEqual(['abc']);
    expect(wrapper.emitted('update:dropdownOpen')).toBeFalsy();
  });

  it('emits apply-field when selecting an option', async () => {
    const wrapper = mount(FieldSelectorDropdown, {
      props: {
        query: '',
        dropdownOpen: true,
        dropdownPlacement: 'down',
        filteredFields: fields,
      },
    });
    mountedWrappers.push(wrapper);

    const items = wrapper.findAll('.ms-dropdown-item');
    expect(items.length).toBe(2);

    await items[1].trigger('mousedown');
    const applyEvent = wrapper.emitted('apply-field');
    expect(applyEvent).toBeTruthy();
    expect((applyEvent?.[0][0] as any).key).toBe('cf_number');
  });

  it('supports keyboard toggle with Enter and Space', async () => {
    const wrapper = mount(FieldSelectorDropdown, {
      props: {
        query: '',
        dropdownOpen: false,
        dropdownPlacement: 'down',
        filteredFields: fields,
      },
    });
    mountedWrappers.push(wrapper);

    const trigger = wrapper.find('.ms-select-trigger');
    expect(trigger.attributes('role')).toBe('button');
    expect(trigger.attributes('tabindex')).toBe('0');

    await trigger.trigger('keydown', { key: 'Enter' });
    await trigger.trigger('keydown', { key: ' ' });

    const openEvents = wrapper.emitted('update:dropdownOpen') || [];
    expect(openEvents[0]).toEqual([true]);
    expect(openEvents[1]).toEqual([true]);
  });
});
