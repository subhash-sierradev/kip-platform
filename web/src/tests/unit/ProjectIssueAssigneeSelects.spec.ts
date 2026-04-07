import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

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
});
