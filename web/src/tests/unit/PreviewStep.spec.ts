/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/utils/dateUtils', () => ({
  formatDisplayDate: vi.fn(() => 'DISPLAY_DATE'),
  formatMetadataDate: vi.fn(() => 'META_DATE'),
}));

import PreviewStep from '@/components/outbound/jirawebhooks/wizard/PreviewStep.vue';

describe('PreviewStep', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const json = JSON.stringify({ fields: { summary: 'S', desc: 'D', users: ['u1', 'u2'] } });
  const mappingData = {
    selectedProject: 'PR',
    selectedIssueType: '1',
    selectedAssignee: 'u1',
    summary: 'Ticket {{ fields.summary }}',
    descriptionFieldMapping: 'Body: {{ fields.desc }}',
    customFields: [
      {
        jiraFieldKey: 'cf1',
        jiraFieldLabel: 'Array',
        type: 'array',
        value: 'a,b',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'cf2',
        jiraFieldLabel: 'Number',
        type: 'number',
        value: '42',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'cf3',
        jiraFieldLabel: 'Boolean',
        type: 'boolean',
        value: 'true',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'cf4',
        jiraFieldLabel: 'Date',
        type: 'date',
        value: '2024-01-01',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'cf4b',
        jiraFieldLabel: 'DateTime',
        type: 'datetime',
        value: '2024-01-01T10:00:00Z',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'cf5',
        jiraFieldLabel: 'User',
        type: 'user',
        value: 'u1',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'cf6',
        jiraFieldLabel: 'Multi',
        type: 'multiuser',
        value: 'u1,u2',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'cf7',
        jiraFieldLabel: 'FromJson',
        type: 'string',
        value: '{{ fields.summary }}',
        valueSource: 'json',
      } as any,
    ],
  };
  const projects = [{ key: 'PR', name: 'Project' }];
  const issueTypes = [{ id: '1', name: 'Sub-task', subtask: true }];
  const users = [
    { accountId: 'u1', displayName: 'Alice' },
    { accountId: 'u2', displayName: 'Bob' },
  ];
  const sprints = [{ value: '67', label: 'Sprint 24 (active)' }];

  it('toggles summary/description preview and resolves templates', async () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    // start with raw
    expect(wrapper.text()).toContain('Ticket {{ fields.summary }}');
    await wrapper.findAll('.ps-eye-icon')[0].trigger('click');
    expect(wrapper.text()).toContain('Ticket S');
    await wrapper.findAll('.ps-eye-icon')[1].trigger('click');
    expect(wrapper.text()).toContain('Body: D');
  });

  it('formats custom field values and labels', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    // Labels present
    expect(wrapper.text()).toContain('Array');
    expect(wrapper.text()).toContain('Number');
    expect(wrapper.text()).toContain('Boolean');
    expect(wrapper.text()).toContain('Date');
    expect(wrapper.text()).toContain('DateTime');
    expect(wrapper.text()).toContain('User');
    expect(wrapper.text()).toContain('Multi');

    // Date formatting uses the correct formatter
    expect(wrapper.text()).toContain('DISPLAY_DATE');
    expect(wrapper.text()).toContain('META_DATE');

    // user names mapped
    expect(wrapper.text()).toContain('Alice');
    expect(wrapper.text()).toContain('Bob');
  });

  it('formats sprint custom field value as sprint label', () => {
    const sprintMapping = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf_sprint',
          jiraFieldLabel: 'Sprint',
          type: 'sprint',
          value: '67',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: sprintMapping,
        projects,
        issueTypes,
        users,
        sprints,
      },
    });

    expect(wrapper.text()).toContain('Sprint 24 (active)');
  });

  it('formats parent custom field value as issue key', () => {
    const parentMapping = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          type: 'object',
          value: '{"key":"SCRUM-436"}',
          valueSource: 'literal',
        } as any,
      ],
    };

    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: parentMapping,
        projects,
        issueTypes,
        users,
      },
    });

    expect(wrapper.text()).toContain('SCRUM-436');
    expect(wrapper.text()).not.toContain('{"key":"SCRUM-436"}');
  });

  it('computes project/issue/assignee display names', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Project');
    expect(wrapper.text()).toContain('Sub-task');
    expect(wrapper.text()).toContain('Alice');
  });

  it('shows "Not Selected" when project not found', () => {
    const noProjectMapping = { ...mappingData, selectedProject: 'NONEXISTENT' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: noProjectMapping,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Not Selected');
  });

  it('shows "Not Selected" when issue type not found', () => {
    const noIssueMapping = { ...mappingData, selectedIssueType: 'NONEXISTENT' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: noIssueMapping,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Not Selected');
  });

  it('shows "Unassigned" when assignee not found', () => {
    const noAssigneeMapping = { ...mappingData, selectedAssignee: 'NONEXISTENT' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: noAssigneeMapping,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Unassigned');
  });

  it('handles empty assignee correctly', () => {
    const emptyAssigneeMapping = { ...mappingData, selectedAssignee: '' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: emptyAssigneeMapping,
        projects,
        issueTypes,
        users,
      },
    });
    // Should not show assignee section when empty
    const assigneeItems = wrapper.text().match(/Assignee:/g);
    expect(assigneeItems).toBeNull();
  });

  it('renders duplicate name error message', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: true,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Jira webhook name already exists');
  });

  it('renders description when provided', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        description: 'My description',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('My description');
  });

  it('shows placeholder when description not provided', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('No description provided');
  });

  it('does not render description field when mapping is empty', () => {
    const noDescMapping = { ...mappingData, descriptionFieldMapping: '' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: noDescMapping,
        projects,
        issueTypes,
        users,
      },
    });
    // Description preview should not appear
    const eyeIcons = wrapper.findAll('.ps-eye-icon');
    expect(eyeIcons.length).toBe(1); // Only summary toggle
  });

  it('toggles JSON payload accordion', async () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });

    // Initially hidden
    expect(wrapper.find('.ps-json-box').exists()).toBe(false);

    // Click accordion header
    await wrapper.find('.ps-accordion-header').trigger('click');
    expect(wrapper.find('.ps-json-box').exists()).toBe(true);

    // Toggle off
    await wrapper.find('.ps-accordion-header').trigger('click');
    expect(wrapper.find('.ps-json-box').exists()).toBe(false);
  });

  it('formats JSON payload correctly', async () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });

    await wrapper.find('.ps-accordion-header').trigger('click');
    const jsonText = wrapper.find('.ps-json-box pre').text();
    expect(jsonText).toContain('"fields"');
    expect(jsonText).toContain('"summary": "S"');
  });

  it('handles invalid JSON in sample payload', async () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: 'invalid json',
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });

    await wrapper.find('.ps-accordion-header').trigger('click');
    expect(wrapper.find('.ps-json-box pre').text()).toBe('invalid json');
  });

  it('resolves template with undefined placeholder', () => {
    const undefinedMapping = { ...mappingData, summary: 'Ticket {{ fields.nonexistent }}' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: undefinedMapping,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Ticket {{ fields.nonexistent }}');
  });

  it('resolves template with null value', async () => {
    const nullJson = JSON.stringify({ fields: { summary: null } });
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: nullJson,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });

    // Toggle to resolved view
    await wrapper.findAll('.ps-eye-icon')[0].trigger('click');
    // Null should render as empty string
    expect(wrapper.find('.ps-preview-box pre').text()).toBe('Ticket');
  });

  it('resolves template with object value', async () => {
    const objectJson = JSON.stringify({ fields: { summary: { nested: 'value' } } });
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: objectJson,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });

    await wrapper.findAll('.ps-eye-icon')[0].trigger('click');
    expect(wrapper.find('.ps-preview-box pre').text()).toContain('"nested"');
  });

  it('handles JSON parse error in resolveTemplate', () => {
    const invalidMapping = { ...mappingData, summary: 'Test {{ x }}' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: '{invalid',
        mappingData: invalidMapping,
        projects,
        issueTypes,
        users,
      },
    });
    // Should return original template on parse error
    expect(wrapper.text()).toContain('Test {{ x }}');
  });

  it('renders custom fields when present', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Custom Fields');
  });

  it('does not render custom fields section when empty', () => {
    const noCustomFields = { ...mappingData, customFields: [] };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: noCustomFields,
        projects,
        issueTypes,
        users,
      },
    });
    // Custom Fields title should not appear
    expect(wrapper.text()).not.toContain('Custom Fields');
  });

  it('formats array custom field from JSON source', () => {
    const arrayMapping = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Users',
          type: 'array',
          value: '{{ fields.users }}',
          valueSource: 'json',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: arrayMapping,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('u1');
    expect(wrapper.text()).toContain('u2');
  });

  it('formats number custom field', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('42');
  });

  it('formats boolean true custom field', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('True');
  });

  it('formats boolean false custom field', () => {
    const falseMapping = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Flag',
          type: 'boolean',
          value: 'false',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: falseMapping,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('False');
  });

  it('handles invalid number format', () => {
    const invalidNumber = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Count',
          type: 'number',
          value: 'not-a-number',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: invalidNumber,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('not-a-number');
  });

  it('handles invalid date format', () => {
    const invalidDate = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Date',
          type: 'date',
          value: 'invalid-date',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: invalidDate,
        projects,
        issueTypes,
        users,
      },
    });
    // Invalid date should be returned as-is
    expect(wrapper.text()).toContain('invalid-date');
  });

  it('handles empty custom field value', () => {
    const emptyValue = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Field',
          type: 'string',
          value: '',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: emptyValue,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Field');
  });

  it('handles null custom field value', () => {
    const nullValue = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Field',
          type: 'string',
          value: null,
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: nullValue,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Field');
  });

  it('formats multiuser with unmapped IDs', () => {
    const unknownUsers = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Users',
          type: 'multiuser',
          value: 'u1,unknown',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: unknownUsers,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Alice, unknown');
  });

  it('formats user with unknown ID', () => {
    const unknownUser = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Assignee',
          type: 'user',
          value: 'unknown',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: unknownUser,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('unknown');
  });

  it('handles custom field with missing label', () => {
    const noLabel = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: '',
          type: 'string',
          value: 'test',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: noLabel,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Custom Field');
  });

  it('handles custom field with unknown type', () => {
    const unknownType = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Field',
          type: 'unknown-type',
          value: 'test',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: unknownType,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('test');
  });

  it('handles array custom field with empty value', () => {
    const emptyArray = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Tags',
          type: 'array',
          value: '',
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: emptyArray,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Tags');
  });

  it('handles JSON path with numeric index', () => {
    const arrayJson = JSON.stringify({ items: ['first', 'second', 'third'] });
    const arrayPath = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Item',
          type: 'string',
          value: '{{ items.1 }}',
          valueSource: 'json',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: arrayJson,
        mappingData: arrayPath,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('second');
  });

  it('handles JSON path with non-object intermediate value', () => {
    const stringJson = JSON.stringify({ value: 'string-value' });
    const invalidPath = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Field',
          type: 'string',
          value: '{{ value.nested }}',
          valueSource: 'json',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: stringJson,
        mappingData: invalidPath,
        projects,
        issueTypes,
        users,
      },
    });
    // Should render Field label even if value resolution returns undefined
    expect(wrapper.text()).toContain('Field');
  });

  it('handles pure placeholder JSON template', () => {
    const pureTemplate = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Data',
          type: 'string',
          value: '{{ fields.users }}',
          valueSource: 'json',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: pureTemplate,
        projects,
        issueTypes,
        users,
      },
    });
    // Should render array values
    expect(wrapper.text()).toContain('u1');
    expect(wrapper.text()).toContain('u2');
  });

  it('resolves wildcard array placeholders in summary preview', async () => {
    const wildcardJson = JSON.stringify({
      serials: [{ filingNumberDisplay: '2025-0000430' }, { filingNumberDisplay: '2025-0000431' }],
    });
    const wildcardMapping = {
      ...mappingData,
      summary: 'Issue {{serials[].filingNumberDisplay}}',
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: wildcardJson,
        mappingData: wildcardMapping,
        projects,
        issueTypes,
        users,
      },
    });

    await wrapper.findAll('.ps-eye-icon')[0].trigger('click');
    expect(wrapper.find('.ps-preview-box pre').text()).toContain(
      'Issue 2025-0000430, 2025-0000431'
    );
  });

  it('returns empty output for wildcard on empty arrays', async () => {
    const wildcardJson = JSON.stringify({ serials: [] });
    const wildcardMapping = {
      ...mappingData,
      summary: 'Issue {{serials[].filingNumberDisplay}}',
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: wildcardJson,
        mappingData: wildcardMapping,
        projects,
        issueTypes,
        users,
      },
    });

    await wrapper.findAll('.ps-eye-icon')[0].trigger('click');
    const previewText = wrapper.find('.ps-preview-box pre').text();
    expect(previewText).not.toContain('{{serials[].filingNumberDisplay}}');
    expect(previewText.trim()).toBe('Issue');
  });

  it('handles object value in custom field formatting', () => {
    const objectJson = JSON.stringify({ data: { nested: 'value' } });
    const objectValue = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Object',
          type: 'string',
          value: '{{ data }}',
          valueSource: 'json',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: objectJson,
        mappingData: objectValue,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('nested');
  });

  it('handles Date object in datetime formatting', () => {
    const dateObject = {
      ...mappingData,
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'Time',
          type: 'datetime',
          value: new Date('2024-01-01T10:00:00Z'),
          valueSource: 'literal',
        } as any,
      ],
    };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: dateObject,
        projects,
        issueTypes,
        users,
      },
    });
    // Should format Date object using META_DATE
    expect(wrapper.text()).toContain('META_DATE');
  });

  it('renders integration workflow steps', () => {
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData,
        projects,
        issueTypes,
        users,
      },
    });
    expect(wrapper.text()).toContain('Integration Workflow');
    expect(wrapper.text()).toContain('Receive webhook payload');
    expect(wrapper.text()).toContain('Transform data');
    expect(wrapper.text()).toContain('Create new Jira ticket');
    expect(wrapper.text()).toContain('Assign ticket to');
    expect(wrapper.text()).toContain('Log execution results');
  });

  it('omits assignee step when no assignee', () => {
    const noAssignee = { ...mappingData, selectedAssignee: '' };
    const wrapper = mount(PreviewStep, {
      props: {
        integrationName: 'Int',
        isDuplicateName: false,
        jsonSample: json,
        mappingData: noAssignee,
        projects,
        issueTypes,
        users,
      },
    });
    // Should not contain the assign step
    const text = wrapper.text();
    const assignSteps = text.match(/Assign ticket to/g);
    expect(assignSteps).toBeNull();
  });
});

describe('PreviewStep - Additional Coverage', () => {
  const baseProps = {
    integrationName: 'My Hook',
    description: 'Desc',
    isDuplicateName: false,
    jsonSample: '{"sample":{"x":1}}',
    mappingData: {
      selectedProject: 'PR',
      selectedIssueType: 'BUG',
      selectedAssignee: 'u1',
      summary: 'Issue {{sample.x}}',
      descriptionFieldMapping: 'Details {{sample}}',
      customFields: [],
    },
    projects: [{ key: 'PR', name: 'Project' }],
    issueTypes: [{ id: 'BUG', name: 'Bug' }],
    users: [{ accountId: 'u1', displayName: 'Alice' }],
  };

  it('renders configuration and toggles summary/description preview', async () => {
    const wrapper = mount(PreviewStep, { props: baseProps });

    // Integration name displayed
    expect(wrapper.find('.ps-value').text()).toContain('My Hook');

    // Toggle summary
    const summaryToggle = wrapper.findAll('.ps-eye-icon').at(0)!;
    await summaryToggle.trigger('click');
    expect(wrapper.find('.ps-preview-box pre').text()).toContain('Issue 1');

    // Toggle description
    const descriptionToggle = wrapper.findAll('.ps-eye-icon').at(1)!;
    await descriptionToggle.trigger('click');
    const previews = wrapper.findAll('.ps-preview-box pre');
    expect(previews.at(1)!.text()).toContain('"x":1');
  });

  it('handles undefined props arrays gracefully', () => {
    const noArrays = { ...baseProps, projects: undefined, issueTypes: undefined, users: undefined };
    const wrapper = mount(PreviewStep, { props: noArrays });
    expect(wrapper.text()).toContain('Not Selected');
    expect(wrapper.text()).toContain('Unassigned');
  });
});
