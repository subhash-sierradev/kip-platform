import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it } from 'vitest';

import CustomFieldRow from '@/components/outbound/jirawebhooks/wizard/CustomFieldRow.vue';

describe('CustomFieldRow interactions', () => {
  const mountedWrappers: Array<ReturnType<typeof mount>> = [];

  afterEach(() => {
    mountedWrappers.splice(0).forEach(wrapper => {
      wrapper.unmount();
    });
  });

  const fields = [
    { key: 'cf_string', name: 'String Field', type: 'string' },
    { key: 'cf_labels', name: 'Labels', type: 'labels', allowedValues: ['a', 'b'] },
    { key: 'cf_user', name: 'User', type: 'user' },
    { key: 'cf_multi', name: 'MultiUser', type: 'multiuser' },
  ] as any;

  it('opens field dropdown and applies selection, emitting row-change', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: '', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('.ms-select-trigger').trigger('click');
    const options = wrapper.findAll('.ms-dropdown-item');
    expect(options.length).toBeGreaterThan(0);
    await options[0].trigger('mousedown');
    // After selection, label text should reflect picked field
    expect(wrapper.find('.ms-select-trigger').text().toLowerCase()).toContain('string field');
  });

  it('toggles string inline preview only when value present and emits toggle-preview', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });
    mountedWrappers.push(wrapper);

    // Attempt to toggle with empty value -> stays hidden (v-show)
    await wrapper.find('.ps-eye-icon').trigger('click');
    const preview = wrapper.find('.ms-inline-preview');
    expect(preview.isVisible()).toBe(false);
  });

  it('labels toggles selection using chips', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_labels', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });
    mountedWrappers.push(wrapper);

    const chips = wrapper.findAll('.ms-chip');
    await chips[0].trigger('click');
    await chips[1].trigger('click');
    // Both chips now selected
    expect(chips[0].classes()).toContain('selected');
    expect(chips[1].classes()).toContain('selected');
  });

  it('multiuser picker toggles users and shows names', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_multi', value: '' },
        idx: 0,
        fields,
        projectUsers: [
          { accountId: 'u1', displayName: 'Alice' },
          { accountId: 'u2', displayName: 'Bob' },
        ],
      },
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('.ms-multiuser-trigger').trigger('click');
    // open dropdown and click first two list items
    const list = wrapper.find('.ms-multiuser-list');
    expect(list.exists()).toBe(true);
    const checks = list.findAll('input[type="checkbox"]');
    await checks[0].setValue(true);
    await checks[1].setValue(true);

    const rowChanges = wrapper.emitted('row-change') || [];
    expect((rowChanges[0][0] as any).value).toContain('u1');
    expect((rowChanges[rowChanges.length - 1][0] as any).value).toContain('u2');

    // value display should include both names
    expect(wrapper.find('.ms-multiuser-trigger .value').text()).toContain('Alice');
    expect(wrapper.find('.ms-multiuser-trigger .value').text()).toContain('Bob');
  });

  it('string field child emits open-insert through parent', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: 'abc' },
        idx: 2,
        fields,
        projectUsers: [],
      },
    });
    mountedWrappers.push(wrapper);

    await wrapper.find('.ps-add-icon').trigger('click');

    expect(wrapper.emitted('open-insert')?.[0][0]).toEqual({ rowIndex: 2 });
  });
});
