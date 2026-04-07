/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { nextTick } from 'vue';
import CustomFieldRow from '@/components/outbound/jirawebhooks/wizard/CustomFieldRow.vue';

// Mock composables
vi.mock('@/composables/useJiraTeams', () => ({
  useJiraTeams: () => ({
    options: { value: [{ value: 'team1', label: 'Team 1' }] },
    search: vi.fn().mockResolvedValue([]),
  }),
}));

vi.mock('@/composables/useJiraSprints', () => ({
  useJiraSprints: () => ({
    options: { value: [{ value: 'sprint1', label: 'Sprint 1' }] },
    search: vi.fn().mockResolvedValue([]),
  }),
}));

describe('CustomFieldRow', () => {
  const fields = [
    { key: 'cf_string', name: 'String Field', type: 'string', allowedValues: [], isCustom: true },
    {
      key: 'cf_option',
      name: 'Option Field',
      type: 'option',
      allowedValues: ['A', 'B'],
      isCustom: true,
    },
    { key: 'cf_number', name: 'Number Field', type: 'number', allowedValues: [], isCustom: true },
    {
      key: 'cf_boolean',
      name: 'Boolean Field',
      type: 'boolean',
      allowedValues: [],
      isCustom: true,
    },
    { key: 'cf_date', name: 'Date Field', type: 'date', allowedValues: [], isCustom: true },
    {
      key: 'cf_datetime',
      name: 'DateTime Field',
      type: 'datetime',
      allowedValues: [],
      isCustom: true,
    },
    { key: 'cf_user', name: 'User Field', type: 'user', allowedValues: [], isCustom: true },
    {
      key: 'cf_multiuser',
      name: 'Multi User Field',
      type: 'multiuser',
      allowedValues: [],
      isCustom: true,
    },
    { key: 'cf_team', name: 'Team Field', type: 'team', allowedValues: [], isCustom: true },
    { key: 'cf_sprint', name: 'Sprint Field', type: 'sprint', allowedValues: [], isCustom: true },
    { key: 'cf_url', name: 'URL Field', type: 'url', allowedValues: [], isCustom: true },
    {
      key: 'cf_labels',
      name: 'Labels Field',
      type: 'labels',
      allowedValues: ['bug', 'feature'],
      isCustom: true,
    },
    {
      key: 'cf_array',
      name: 'Array Field',
      type: 'array',
      allowedValues: ['val1', 'val2'],
      isCustom: true,
    },
  ];

  const projectUsers = [
    { accountId: 'user1', displayName: 'John Doe' },
    { accountId: 'user2', displayName: 'Jane Smith' },
    { accountId: 'user3', displayName: 'Bob Johnson' },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('emits row-change on input and toggles preview emit', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    // Enter a value
    const input = wrapper.find('input.ms-input.ms-value');
    await input.setValue('hello');
    const change = wrapper.emitted('row-change');
    expect(change).toBeTruthy();
    expect(change?.[0][0]).toMatchObject({ idx: 0, field: 'cf_string', value: 'hello' });

    // Toggle preview emits toggle-preview
    const eye = wrapper.find('.ps-eye-icon');
    await eye.trigger('click');
    const toggles = wrapper.emitted('toggle-preview');
    expect(toggles).toBeTruthy();
    expect(toggles?.[0][0]).toMatchObject({ rowIndex: 0, value: 'hello' });
  });

  it('supports selecting options and emits row-change with label/type', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_option', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const select = wrapper.find('select.ms-input.ms-value');
    await select.setValue('A');
    const change = wrapper.emitted('row-change');
    expect(change).toBeTruthy();
    const payload = change?.[0][0] as any;
    expect(payload.idx).toBe(0);
    expect(payload.field).toBe('cf_option');
    expect(payload.value).toBe('A');
  });

  it('renders number input for number field type', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_number', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const input = wrapper.find('input[type="number"]');
    expect(input.exists()).toBe(true);
    await input.setValue('42');
    expect(wrapper.emitted('row-change')).toBeTruthy();
  });

  it('renders boolean select with true/false options', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_boolean', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const select = wrapper.find('select.ms-input.ms-value');
    expect(select.exists()).toBe(true);
    await select.setValue('true');
    const change = wrapper.emitted('row-change');
    expect(change?.[0][0]).toMatchObject({ value: 'true' });
  });

  it('renders date input for date field type', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_date', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const input = wrapper.find('input[type="date"]');
    expect(input.exists()).toBe(true);
    await input.setValue('2024-01-15');
    expect(wrapper.emitted('row-change')).toBeTruthy();
  });

  it('renders datetime input for datetime field type', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_datetime', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const input = wrapper.find('input[type="datetime-local"]');
    expect(input.exists()).toBe(true);
    await input.setValue('2024-01-15T10:30');
    expect(wrapper.emitted('row-change')).toBeTruthy();
  });

  it('renders user select dropdown', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_user', value: '' },
        idx: 0,
        fields,
        projectUsers,
      },
    });

    const select = wrapper.find('select.ms-input.ms-value');
    expect(select.exists()).toBe(true);
    await select.setValue('user1');
    expect(wrapper.emitted('row-change')?.[0][0]).toMatchObject({ value: 'user1' });
  });

  it('renders multi-user picker and toggles dropdown', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_multiuser', value: '' },
        idx: 0,
        fields,
        projectUsers,
      },
    });

    const trigger = wrapper.find('.ms-multiuser-trigger');
    expect(trigger.exists()).toBe(true);
    await trigger.trigger('click');
    await nextTick();

    const dropdown = wrapper.find('.ms-multiuser-dropdown');
    expect(dropdown.exists()).toBe(true);
  });

  it('multi-user picker: toggles user selection', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_multiuser', value: '' },
        idx: 0,
        fields,
        projectUsers,
      },
    });

    await wrapper.find('.ms-multiuser-trigger').trigger('click');
    await nextTick();

    const checkboxes = wrapper.findAll('input[type="checkbox"]');
    expect(checkboxes.length).toBeGreaterThan(0);

    await checkboxes[0].setValue(true);
    const change = wrapper.emitted('row-change');
    expect(change).toBeTruthy();
    expect((change?.[0][0] as any).value).toContain('user1');
  });

  it('multi-user picker: deselects user when already selected', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_multiuser', value: 'user1,user2' },
        idx: 0,
        fields,
        projectUsers,
      },
    });

    await wrapper.find('.ms-multiuser-trigger').trigger('click');
    await nextTick();

    const checkbox = wrapper.findAll('input[type="checkbox"]')[0];
    await checkbox.setValue(false);

    const value = (wrapper.emitted('row-change')?.[0][0] as any).value;
    expect(value).not.toContain('user1');
  });

  it('multi-user picker: filters users by search query', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_multiuser', value: '' },
        idx: 0,
        fields,
        projectUsers,
      },
    });

    await wrapper.find('.ms-multiuser-trigger').trigger('click');
    await nextTick();

    const searchInput = wrapper.find('.ms-multiuser-search');
    await searchInput.setValue('John');
    await nextTick();

    const items = wrapper.findAll('.ms-multiuser-item');
    expect(items.length).toBeLessThanOrEqual(projectUsers.length);
  });

  it('multi-user picker: closes on ESC key in search', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_multiuser', value: '' },
        idx: 0,
        fields,
        projectUsers,
      },
    });

    await wrapper.find('.ms-multiuser-trigger').trigger('click');
    await nextTick();
    expect(wrapper.find('.ms-multiuser-dropdown').exists()).toBe(true);

    const searchInput = wrapper.find('.ms-multiuser-search');
    await searchInput.trigger('keydown', { key: 'Escape' });
    await nextTick();

    expect((wrapper.vm as any).userDropdownOpen).toBe(false);
  });

  it('renders team select dropdown with options', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_team', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    await nextTick();
    const select = wrapper.find('select.ms-input.ms-value');
    expect(select.exists()).toBe(true);
    await select.setValue('team1');
    expect(wrapper.emitted('row-change')?.[0][0]).toMatchObject({ value: 'team1' });
  });

  it('renders sprint select dropdown with options', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        projectKey: 'PROJ',
        row: { _id: '1', field: 'cf_sprint', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    await nextTick();
    const select = wrapper.find('select.ms-input.ms-value');
    expect(select.exists()).toBe(true);
    await select.setValue('sprint1');
    expect(wrapper.emitted('row-change')?.[0][0]).toMatchObject({ value: 'sprint1' });
  });

  it('renders URL input for url field type', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_url', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const input = wrapper.find('input[type="url"]');
    expect(input.exists()).toBe(true);
    await input.setValue('https://example.com');
    expect(wrapper.emitted('row-change')?.[0][0]).toMatchObject({ value: 'https://example.com' });
  });

  it('renders labels as multi-select chips', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_labels', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const chips = wrapper.findAll('.ms-chip');
    expect(chips.length).toBe(2); // bug, feature

    await chips[0].trigger('click');
    const change = wrapper.emitted('row-change');
    expect((change?.[0][0] as any).value).toContain('bug');
  });

  it('labels: toggles selection on chip click', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_labels', value: 'bug' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const chips = wrapper.findAll('.ms-chip');
    const selectedChip = chips.find(c => c.classes().includes('selected'));
    expect(selectedChip).toBeTruthy();

    // Click to deselect
    await chips[0].trigger('click');
    const value = (wrapper.emitted('row-change')?.[0][0] as any).value;
    expect(value).not.toContain('bug');
  });

  it('renders array field as multi-select chips', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_array', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const chips = wrapper.findAll('.ms-chip');
    expect(chips.length).toBe(2); // val1, val2

    await chips[0].trigger('click');
    expect(wrapper.emitted('row-change')).toBeTruthy();
  });

  it('field selector dropdown: opens on trigger click', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: '', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
        initialLabel: 'Search custom fields...',
      },
    });

    const trigger = wrapper.find('.ms-select-trigger');
    await trigger.trigger('click');
    await nextTick();

    expect((wrapper.vm as any).dropdownOpen).toBe(true);
    const dropdown = wrapper.find('.ms-dropdown');
    expect(dropdown.exists()).toBe(true);
  });

  it('field selector dropdown: filters fields by search query', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: '', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    await wrapper.find('.ms-select-trigger').trigger('click');
    await nextTick();

    const searchInput = wrapper.find('input.ms-search-visible');
    await searchInput.setValue('Number');
    await nextTick();

    const items = wrapper.findAll('.ms-dropdown-item');
    expect(items.length).toBeGreaterThan(0);
    expect(items.length).toBeLessThan(fields.length);
  });

  it('field selector dropdown: excludes used field keys', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
        usedFieldKeys: ['cf_number', 'cf_boolean'],
      },
    });

    await wrapper.find('.ms-select-trigger').trigger('click');
    await nextTick();

    const items = wrapper.findAll('.ms-dropdown-item');
    const itemKeys = items.map(i => i.find('.key').text());
    expect(itemKeys).not.toContain('cf_number');
    expect(itemKeys).not.toContain('cf_boolean');
    expect(itemKeys).toContain('cf_string'); // Current field still visible
  });

  it('field selector dropdown: applies field selection and clears value', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: 'old value' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    await wrapper.find('.ms-select-trigger').trigger('click');
    await nextTick();

    const items = wrapper.findAll('.ms-dropdown-item');
    const numberFieldItem = items.find(i => i.find('.key').text() === 'cf_number');
    await numberFieldItem?.trigger('mousedown');
    await nextTick();

    expect((wrapper.vm as any).dropdownOpen).toBe(false);
    const change = wrapper.emitted('row-change');
    expect((change?.[0][0] as any).field).toBe('cf_number');
    expect((change?.[0][0] as any).value).toBe(''); // Value cleared on field change
  });

  it('string field: emits open-insert event', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: '' },
        idx: 3,
        fields,
        projectUsers: [],
      },
    });

    const addButton = wrapper.find('.ps-add-icon');
    await addButton.trigger('click');

    const insertEvent = wrapper.emitted('open-insert');
    expect(insertEvent).toBeTruthy();
    expect(insertEvent?.[0][0]).toMatchObject({ rowIndex: 3 });
  });

  it('string field: toggle preview button disabled when value empty', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const eyeButton = wrapper.find('.ps-eye-icon');
    expect(eyeButton.attributes('disabled')).toBeDefined();

    await eyeButton.trigger('click');
    expect(wrapper.emitted('toggle-preview')).toBeFalsy();
  });

  it('string field: tooltip shows on action button hover', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const addButton = wrapper.find('.ps-add-icon');
    await addButton.trigger('mouseenter', { clientX: 100, clientY: 100 });
    await nextTick();

    expect((wrapper.vm as any).actionTipVisible).toBe(true);
    expect((wrapper.vm as any).actionTipText).toBe('Insert fields');
  });

  it('string field: tooltip hides on mouseleave', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    const addButton = wrapper.find('.ps-add-icon');
    await addButton.trigger('mouseenter', { clientX: 100, clientY: 100 });
    await addButton.trigger('mouseleave');
    await nextTick();

    expect((wrapper.vm as any).actionTipVisible).toBe(false);
  });

  it('updates localRow when props.row changes', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: 'cf_string', value: 'initial' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    expect((wrapper.vm as any).localRow.value).toBe('initial');

    await wrapper.setProps({
      row: { _id: '1', field: 'cf_string', value: 'updated' },
    });
    await nextTick();

    expect((wrapper.vm as any).localRow.value).toBe('updated');
  });

  it('dropdown placement: calculates up when insufficient space below', async () => {
    const wrapper = mount(CustomFieldRow, {
      props: {
        connectionId: 'c1',
        row: { _id: '1', field: '', value: '' },
        idx: 0,
        fields,
        projectUsers: [],
      },
    });

    // Mock getBoundingClientRect to simulate bottom of viewport
    const triggerEl = wrapper.find('.ms-select-trigger').element;
    vi.spyOn(triggerEl, 'getBoundingClientRect').mockReturnValue({
      top: 800,
      bottom: 850,
      left: 0,
      right: 100,
      width: 100,
      height: 50,
      x: 0,
      y: 800,
      toJSON: () => ({}),
    });

    await wrapper.find('.ms-select-trigger').trigger('click');
    await nextTick();

    // Should place dropdown upward when near bottom
    expect((wrapper.vm as any).dropdownPlacement).toBe('up');
  });
});
