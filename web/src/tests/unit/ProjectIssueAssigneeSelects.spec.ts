import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { nextTick } from 'vue';

import ProjectIssueAssigneeSelects from '@/components/outbound/jirawebhooks/wizard/ProjectIssueAssigneeSelects.vue';

describe('ProjectIssueAssigneeSelects', () => {
  it('emits updates and change events', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [
          { key: 'P1', name: 'Project 1' },
          { key: 'P2', name: 'Project 2' },
        ],
        issueTypes: [{ id: 'I1', name: 'Bug' }],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: '',
        selectedIssueType: '',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent issue' }],
      },
    });

    const selects = wrapper.findAll('select');
    const projectSelect = selects[0];
    const assigneeSelect = selects[1];
    const issueTypeSelect = selects[2];

    await projectSelect.setValue('P2');
    expect(wrapper.emitted('update:selectedProject')).toEqual([['P2']]);
    expect(wrapper.emitted('project-change')).toBeTruthy();

    await wrapper.setProps({ selectedProject: 'P2' });

    await issueTypeSelect.setValue('I1');
    expect(wrapper.emitted('update:selectedIssueType')).toEqual([['I1']]);
    expect(wrapper.emitted('issue-type-change')).toBeTruthy();
    await wrapper.setProps({ selectedIssueType: 'I1' });

    const parentInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await parentInput.trigger('focus');
    const parentOption = wrapper
      .findAll('.ms-field--parent .ms-search-select__option')
      .find(node => node.text().includes('PRJ-101'));

    expect(parentOption).toBeTruthy();
    await parentOption!.trigger('click');
    expect(wrapper.emitted('update:selectedParent')).toEqual([['PRJ-101']]);
    expect(wrapper.emitted('parent-change')).toBeTruthy();

    await assigneeSelect.setValue('U1');
    expect(wrapper.emitted('update:selectedAssignee')).toEqual([['U1']]);
    expect(wrapper.emitted('assignee-change')).toBeTruthy();
  });

  it('renders Subtask parent label and no legacy parent-required message', () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent issue' }],
        showParentRequiredError: true,
      },
    });

    const labels = wrapper.findAll('label.ms-label').map(node => node.text());
    expect(labels.some(text => text.includes("Subtask's Parent"))).toBe(true);
    expect(wrapper.text()).not.toContain('Parent field is required for Subtask issue type.');
  });

  it('hides parent select when subtask parent field is disabled', () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Bug', subtask: false } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: false,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent issue' }],
        showParentRequiredError: false,
      },
    });

    expect(wrapper.text()).not.toContain("Subtask's Parent");
    expect(wrapper.find('.ms-field--parent').exists()).toBe(false);
  });

  it('filters parent options using search input', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [
          { value: 'PRJ-101', label: 'PRJ-101 - Parent one' },
          { value: 'SCRUM-201', label: 'SCRUM-201 - Parent two' },
        ],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue('scrum');

    const optionLabels = wrapper
      .findAll('.ms-field--parent .ms-search-select__option')
      .map(node => node.text());

    expect(optionLabels.some(text => text.includes('SCRUM-201'))).toBe(true);
    expect(optionLabels.some(text => text.includes('PRJ-101'))).toBe(false);
  });

  it('emits parent-search when typing reaches 5+ characters without local match', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue('abcd');
    expect(wrapper.emitted('parent-search')).toBeFalsy();

    await searchInput.setValue('abcde');
    expect(wrapper.emitted('parent-search')).toEqual([['abcde']]);
  });

  it('on Enter emits parent-search when no local result exists', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue('zzzzz');
    await searchInput.trigger('keydown', { key: 'Enter' });

    const parentSearchEvents = wrapper.emitted('parent-search') || [];
    expect(parentSearchEvents[parentSearchEvents.length - 1]).toEqual(['zzzzz']);
  });

  it('adds combobox/listbox ARIA semantics and supports keyboard option selection', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');

    expect(searchInput.attributes('role')).toBe('combobox');
    expect(searchInput.attributes('aria-expanded')).toBe('true');

    const listbox = wrapper.find('.ms-field--parent .ms-search-select__menu');
    expect(listbox.attributes('role')).toBe('listbox');

    const option = wrapper
      .findAll('.ms-field--parent .ms-search-select__option')
      .find(node => node.text().includes('PRJ-101'));

    expect(option?.attributes('role')).toBe('option');
    expect(option?.attributes('aria-selected')).toBe('false');

    await option!.trigger('keydown', { key: 'Enter' });

    const selectedEvents = wrapper.emitted('update:selectedParent') || [];
    expect(selectedEvents[selectedEvents.length - 1]).toEqual(['PRJ-101']);
  });

  it('restores selected parent label on outside click after manual text edit', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: 'PRJ-101',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
      attachTo: document.body,
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    expect((searchInput.element as HTMLInputElement).value).toBe('PRJ-101 - Parent one');

    await searchInput.trigger('focus');
    await searchInput.setValue('PRJ-101 - Parent');

    await wrapper.setProps({
      parentFieldOptions: [{ value: 'OTHER-1', label: 'OTHER-1 - Another parent' }],
    });

    document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    await nextTick();

    expect((searchInput.element as HTMLInputElement).value).toBe('PRJ-101 - Parent one');
    wrapper.unmount();
  });

  it('emits parent label for preselected parent when options load', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: 'PRJ-101',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    await nextTick();

    const labelEvents = wrapper.emitted('parent-label-change') || [];
    expect(labelEvents.length).toBeGreaterThan(0);
    expect(labelEvents[labelEvents.length - 1]).toEqual([
      { value: 'PRJ-101', label: 'PRJ-101 - Parent one' },
    ]);
  });

  it('keeps the parent dropdown disabled until both project and issue type are selected', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: '',
        selectedIssueType: '',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    const toggleButton = wrapper.find('.ms-field--parent .ms-search-select__toggle');

    expect(searchInput.attributes('disabled')).toBeDefined();
    expect(toggleButton.attributes('disabled')).toBeDefined();

    await toggleButton.trigger('click');
    expect(wrapper.find('.ms-field--parent .ms-search-select__menu').exists()).toBe(false);
  });

  it('selects an exact local parent match on Enter without falling back to remote search', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue(' prj-101 ');
    await searchInput.trigger('keydown', { key: 'Enter' });

    const selectedEvents = wrapper.emitted('update:selectedParent') || [];
    expect(selectedEvents[selectedEvents.length - 1]).toEqual(['PRJ-101']);
    expect(wrapper.emitted('parent-search')).toBeFalsy();
  });

  it('deduplicates remote searches and clears stale results when the query becomes too short', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue('abcde');
    await searchInput.setValue('abcde');
    await searchInput.setValue('abcd');
    await searchInput.setValue('abcde');

    expect(wrapper.emitted('parent-search')).toEqual([['abcde'], [''], ['abcde']]);
  });

  it('uses cached and raw selected parent labels across watcher sync paths', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: 'PRJ-101',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    expect((searchInput.element as HTMLInputElement).value).toBe('PRJ-101 - Parent one');

    await wrapper.setProps({ parentFieldOptions: [] });
    await nextTick();
    expect((searchInput.element as HTMLInputElement).value).toBe('PRJ-101 - Parent one');

    await wrapper.setProps({ selectedParent: 'RAW-999' });
    await nextTick();
    expect((searchInput.element as HTMLInputElement).value).toBe('RAW-999');
  });

  it('sanitizes parent option ids and supports selecting with the space key', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ 101/#1', label: 'PRJ 101/#1 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');

    const option = wrapper.find('.ms-field--parent .ms-search-select__option');
    expect(option.attributes('id')).toContain('PRJ_101__1');

    await option.trigger('keydown', { key: ' ' });

    const selectedEvents = wrapper.emitted('update:selectedParent') || [];
    expect(selectedEvents[selectedEvents.length - 1]).toEqual(['PRJ 101/#1']);
  });

  it('opens on ArrowDown, ignores inside clicks, and closes again from the toggle button', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: 'PRJ-101',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
      attachTo: document.body,
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    const toggleButton = wrapper.find('.ms-field--parent .ms-search-select__toggle');

    await searchInput.trigger('keydown', { key: 'ArrowDown' });
    expect(wrapper.find('.ms-field--parent .ms-search-select__menu').exists()).toBe(true);

    searchInput.element.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    await nextTick();
    expect(wrapper.find('.ms-field--parent .ms-search-select__menu').exists()).toBe(true);

    await toggleButton.trigger('click');
    expect(wrapper.find('.ms-field--parent .ms-search-select__menu').exists()).toBe(false);

    wrapper.unmount();
  });

  it('clears cached parent labels when the selected parent is removed', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: 'PRJ-101',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    expect((searchInput.element as HTMLInputElement).value).toBe('PRJ-101 - Parent one');

    await wrapper.setProps({ selectedParent: '' });
    await nextTick();

    expect((searchInput.element as HTMLInputElement).value).toBe('');
  });

  it('does not search or select anything when Enter is pressed on an empty query', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue('   ');
    await searchInput.trigger('keydown', { key: 'Enter' });

    expect(wrapper.emitted('update:selectedParent')).toBeFalsy();
    expect(wrapper.emitted('parent-search')).toBeFalsy();
  });

  it('ignores non-selection keys on parent options', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');

    const option = wrapper.find('.ms-field--parent .ms-search-select__option');
    await option.trigger('keydown', { key: 'Escape' });

    expect(wrapper.emitted('update:selectedParent')).toBeFalsy();
    expect(wrapper.emitted('parent-change')).toBeFalsy();
  });

  it('keeps the manual parent text while the dropdown is open and only resyncs after closing', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: 'PRJ-101',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
      attachTo: document.body,
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue('manual draft');

    await wrapper.setProps({
      parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Updated parent label' }],
    });
    await nextTick();

    expect((searchInput.element as HTMLInputElement).value).toBe('manual draft');

    document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    await nextTick();

    expect((searchInput.element as HTMLInputElement).value).toBe('PRJ-101 - Updated parent label');
    wrapper.unmount();
  });

  it('does not emit remote searches while the parent selector is disabled', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: '',
        selectedIssueType: 'I1',
        selectedParent: '',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    (searchInput.element as HTMLInputElement).value = 'abcde';
    await searchInput.trigger('input');

    expect(wrapper.find('.ms-field--parent .ms-search-select__menu').exists()).toBe(false);
    expect(wrapper.emitted('parent-search')).toBeFalsy();
  });

  it('closes and resynchronizes the parent display when the parent field is hidden and shown again', async () => {
    const wrapper = mount(ProjectIssueAssigneeSelects, {
      props: {
        projects: [{ key: 'P1', name: 'Project 1' }],
        issueTypes: [{ id: 'I1', name: 'Sub-task', subtask: true } as any],
        users: [{ accountId: 'U1', displayName: 'User 1' }],
        selectedProject: 'P1',
        selectedIssueType: 'I1',
        selectedParent: 'PRJ-101',
        selectedAssignee: '',
        showParentField: true,
        parentFieldOptions: [{ value: 'PRJ-101', label: 'PRJ-101 - Parent one' }],
      },
    });

    const searchInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    await searchInput.trigger('focus');
    await searchInput.setValue('temporary text');

    await wrapper.setProps({ showParentField: false });
    await nextTick();
    expect(wrapper.find('.ms-field--parent').exists()).toBe(false);

    await wrapper.setProps({ showParentField: true });
    await nextTick();

    const restoredInput = wrapper.find('.ms-field--parent .ms-search-select__input');
    expect((restoredInput.element as HTMLInputElement).value).toBe('PRJ-101 - Parent one');
  });
});
