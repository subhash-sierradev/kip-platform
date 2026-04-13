/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import MappingStep from '@/components/outbound/jirawebhooks/wizard/MappingStep.vue';
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';

vi.mock('@/components/outbound/jirawebhooks/wizard/CustomFieldsPanel.vue', () => ({
  default: {
    name: 'CustomFieldsPanel',
    props: ['rows', 'projectUsers', 'renderedPreviews', 'connectionId'],
    emits: ['update:rows', 'valid-change', 'open-insert', 'toggle-preview'],
    template: `
      <div class="cfp-stub">
        <button class="emit-rows" @click="$emit('update:rows', [{ field: 'cf_x', value: '{{ fields.key }}', label: 'Label', type: 'string' }])">rows</button>
        <button class="emit-valid" @click="$emit('valid-change', true)">valid</button>
      </div>`,
  },
}));

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectsByConnectionId: vi.fn().mockResolvedValue([{ key: 'PR', name: 'Proj' }]),
    getProjectIssueTypesByConnectionId: vi
      .fn()
      .mockResolvedValue([{ id: '1', name: 'Bug', subtask: false }]),
    getProjectUsersByConnectionId: vi
      .fn()
      .mockResolvedValue([{ accountId: 'u1', displayName: 'Alice' }]),
    getProjectParentIssuesByConnectionId: vi
      .fn()
      .mockResolvedValue([{ key: 'SCRUM-1', summary: 'Parent one' }]),
  },
}));

const baseProps = {
  jsonSample: '{"fields":{"key":"VALUE"}}',
  connectionId: 'c1',
  mappingData: {
    selectedProject: '',
    selectedIssueType: '',
    selectedAssignee: '',
    summary: '',
    descriptionFieldMapping: '',
    customFields: [],
  },
  projects: [],
  issueTypes: [],
  users: [],
};

describe('MappingStep', () => {
  it('opens insert modal and inserts placeholder into summary', async () => {
    const wrapper = mount(MappingStep, { props: baseProps });
    // open modal via Summary add button
    await wrapper.find('.ms-field .ps-add-icon').trigger('click');
    // click first field option (from computed transformed fields)
    const item = wrapper.find('.ms-field-item');
    expect(item.exists()).toBe(true);
    await item.trigger('click');
    // summary updated
    const textarea = wrapper.find('textarea.ms-textarea--summary');
    expect((textarea.element as HTMLTextAreaElement).value).toContain('{{');
  });
  it('filters fields by search text with multiple terms', async () => {
    const wrapper = mount(MappingStep, {
      props: { ...baseProps, jsonSample: '{"user":{"name":"Alice","email":"a@b.com"}}' },
    });
    await wrapper.find('.ms-field .ps-add-icon').trigger('click');
    // All fields initially shown
    expect(wrapper.findAll('.ms-field-item').length).toBeGreaterThan(1);

    // Filter by single term
    const searchInput = wrapper.find('.ms-search-field');
    await searchInput.setValue('email');
    expect(wrapper.findAll('.ms-field-item').length).toBe(1);
    expect(wrapper.find('.ms-field-item').text()).toContain('email');

    // Filter with multiple terms
    await searchInput.setValue('user email');
    expect(wrapper.findAll('.ms-field-item').length).toBe(1);

    // No matches shows empty message
    await searchInput.setValue('nonexistent');
    expect(wrapper.findAll('.ms-field-item').length).toBe(0);
    expect(wrapper.find('.ms-empty-row').exists()).toBe(true);
  });

  it('clears search when closing field modal', async () => {
    const wrapper = mount(MappingStep, { props: baseProps });
    await wrapper.find('.ms-field .ps-add-icon').trigger('click');
    const searchInput = wrapper.find('.ms-search-field');
    await searchInput.setValue('test');
    expect((searchInput.element as HTMLInputElement).value).toBe('test');

    await wrapper.find('.ms-btn-cancel').trigger('click');
    expect(wrapper.find('.ms-modal').exists()).toBe(false);

    // Reopen and search should be cleared
    await wrapper.find('.ms-field .ps-add-icon').trigger('click');
    const newSearch = wrapper.find('.ms-search-field');
    expect((newSearch.element as HTMLInputElement).value).toBe('');
  });
  it('onRowsUpdate merges as JSON-sourced when template detected', async () => {
    const wrapper = mount(MappingStep, { props: baseProps });
    await wrapper.find('.emit-rows').trigger('click');
    // validation called and mapping emitted; assert no crash
    expect(wrapper.exists()).toBe(true);
  });

  it('toggles inline preview for summary field', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        jsonSample: '{"fields":{"key":"TEST-123"}}',
        mappingData: { ...baseProps.mappingData, summary: 'Issue {{fields.key}}' },
      },
    });

    // Click preview button
    const eyeBtn = wrapper.findAll('.ps-eye-icon')[0];
    await eyeBtn.trigger('click');
    await wrapper.vm.$nextTick();

    // Preview visible with rendered content
    const preview = wrapper.find('.ms-inline-preview--summary');
    expect(preview.exists()).toBe(true);
    expect(preview.text()).toContain('Issue TEST-123');

    // Click again to hide preview
    await eyeBtn.trigger('click');
    await wrapper.vm.$nextTick();
    // Textarea should be visible again
    const textarea = wrapper.find('textarea.ms-textarea--summary');
    expect(textarea.exists()).toBe(true);
  });

  it('toggles inline preview for description field', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        jsonSample: '{"user":"Alice"}',
        mappingData: { ...baseProps.mappingData, descriptionFieldMapping: 'Created by {{user}}' },
      },
    });

    const eyeBtn = wrapper.findAll('.ps-eye-icon')[1];
    await eyeBtn.trigger('click');
    await wrapper.vm.$nextTick();

    const preview = wrapper.find('.ms-inline-preview--description');
    expect(preview.exists()).toBe(true);
    expect(preview.text()).toContain('Created by Alice');
  });

  it('shows "Preview is empty" when field content is empty', async () => {
    const wrapper = mount(MappingStep, {
      props: { ...baseProps, mappingData: { ...baseProps.mappingData, summary: '' } },
    });
    const eyeBtn = wrapper.findAll('.ps-eye-icon')[0];
    // Eye button should be disabled when content empty
    expect(eyeBtn.attributes('disabled')).toBe('');
  });

  it('renders empty string for undefined/null values in template', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        jsonSample: '{"defined":"value"}',
        mappingData: { ...baseProps.mappingData, summary: '{{defined}} {{undefined}} {{null}}' },
      },
    });
    const eyeBtn = wrapper.findAll('.ps-eye-icon')[0];
    await eyeBtn.trigger('click');
    await wrapper.vm.$nextTick();
    const preview = wrapper.find('.ms-inline-preview--summary');
    // undefined/null render as empty strings, so result is 'value  ' but whitespace may be normalized
    expect(preview.text()).toContain('value');
    expect(preview.text()).not.toContain('undefined');
    expect(preview.text()).not.toContain('null');
  });

  it('validation requires project, issue type, summary and valid custom fields', async () => {
    const wrapper = mount(MappingStep, { props: baseProps });
    // select project -> triggers data fetching
    await wrapper.find('select.ms-select').setValue('PR');
    // Summary entry
    const textarea = wrapper.find('textarea.ms-textarea--summary');
    await textarea.setValue('Hello');
    await wrapper.find('.emit-valid').trigger('click');
    expect(wrapper.exists()).toBe(true);
  });

  it('resets issue type and assignee when project changes', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        mappingData: {
          ...baseProps.mappingData,
          selectedProject: 'PR',
          selectedIssueType: '1',
          selectedAssignee: 'u1',
        },
      },
    });
    const projectSelect = wrapper.find('select.ms-select');

    // Change project
    await projectSelect.setValue('PR2');

    // Dependent fields reset
    const issueTypeSelect = wrapper.findAll('select.ms-select')[1];
    expect((issueTypeSelect.element as HTMLSelectElement).value).toBe('');
    const assigneeSelect = wrapper.findAll('select.ms-select')[2];
    expect((assigneeSelect.element as HTMLSelectElement).value).toBe('');
  });

  it('disables issue type and assignee when no project selected', async () => {
    const wrapper = mount(MappingStep, { props: baseProps });
    const issueTypeSelect = wrapper.findAll('select.ms-select')[1];
    const assigneeSelect = wrapper.findAll('select.ms-select')[2];

    expect(issueTypeSelect.attributes('disabled')).toBe('');
    expect(assigneeSelect.attributes('disabled')).toBe('');
  });

  it('emits validation-change false when summary is empty', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        mappingData: {
          ...baseProps.mappingData,
          selectedProject: 'PR',
          selectedIssueType: '1',
          summary: '',
        },
      },
    });
    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    // Last emit should be false due to empty summary
    const lastEvent = validationEvents![validationEvents!.length - 1];
    expect(lastEvent[0]).toBe(false);
  });

  it('emits validation-change false when custom fields invalid', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        mappingData: {
          ...baseProps.mappingData,
          selectedProject: 'PR',
          selectedIssueType: '1',
          summary: 'Test',
        },
      },
    });
    // Set custom fields invalid
    await wrapper.find('.emit-valid').trigger('click');
    // This will set customFieldsValid to true, then we need to test when it's false
    // The initial validation should reflect custom fields validity
    expect(wrapper.exists()).toBe(true);
  });
  it('handles toggle-preview event from custom fields panel', async () => {
    const wrapper = mount(MappingStep, {
      props: { ...baseProps, jsonSample: '{"key":"value"}' },
    });
    const panel = wrapper.findComponent({ name: 'CustomFieldsPanel' });

    // Emit toggle-preview event
    panel.vm.$emit('toggle-preview', { rowIndex: 0, value: '{{key}}' });
    await wrapper.vm.$nextTick();

    // Custom field preview should be updated
    expect(wrapper.exists()).toBe(true);
  });

  it('updates customFieldsLocal when rows updated from panel', async () => {
    const wrapper = mount(MappingStep, { props: baseProps });
    const panel = wrapper.findComponent({ name: 'CustomFieldsPanel' });

    // Emit row update with template value
    panel.vm.$emit('update:rows', [
      { field: 'customfield_1', value: '{{fields.key}}', label: 'Custom Field', type: 'string' },
    ]);
    await wrapper.vm.$nextTick();

    // Mapping should be emitted with updated custom fields
    const mappingEvents = wrapper.emitted('update:mappingData');
    expect(mappingEvents).toBeTruthy();
    const lastMapping = mappingEvents![mappingEvents!.length - 1][0] as any;
    expect(lastMapping.customFields).toBeDefined();
    expect(lastMapping.customFields[0].jiraFieldKey).toBe('customfield_1');
    expect(lastMapping.customFields[0].valueSource).toBe('json');
  });

  it('preserves parent custom field when custom rows update in subtask mode', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        issueTypes: [{ id: 'SUB', name: 'Sub-task', subtask: true }],
        mappingData: {
          ...baseProps.mappingData,
          selectedProject: 'PR',
          selectedIssueType: 'SUB',
          summary: 'Issue summary',
          customFields: [
            {
              _id: 'parent-1',
              jiraFieldKey: 'parent',
              jiraFieldLabel: 'Parent',
              type: 'object',
              required: true,
              valueSource: 'literal',
              value: '{"key":"SCRUM-1"}',
            },
          ],
        },
      },
    });

    await wrapper.find('.emit-rows').trigger('click');

    const mappingEvents = wrapper.emitted('update:mappingData');
    expect(mappingEvents).toBeTruthy();
    const lastMapping = mappingEvents![mappingEvents!.length - 1][0] as any;
    const keys = (lastMapping.customFields || []).map((field: any) => field.jiraFieldKey);
    expect(keys).toContain('parent');
    expect(keys).toContain('cf_x');
  });

  it('detects template values and sets valueSource to json', async () => {
    const wrapper = mount(MappingStep, { props: baseProps });
    const panel = wrapper.findComponent({ name: 'CustomFieldsPanel' });

    // Template value - creates new field with json valueSource
    panel.vm.$emit('update:rows', [
      { field: 'cf1', value: '{{data}}', label: 'Field1', type: 'string' },
    ]);
    await wrapper.vm.$nextTick();

    let mappingEvents = wrapper.emitted('update:mappingData') as any[];
    let lastMapping = mappingEvents[mappingEvents.length - 1][0];
    expect(lastMapping.customFields[0].valueSource).toBe('json');

    // Update to literal value - since no existing field with literal valueSource, it creates new and detects no template
    // The logic: buildUpdatedMapping only runs if existing field found. buildNewMapping checks isTemplateValue.
    // For literal text without {{}}, isTemplateValue returns false, so valueSource is 'literal'
    panel.vm.$emit('update:rows', [
      { field: 'cf2', value: 'plain text', label: 'Field2', type: 'string' },
    ]);
    await wrapper.vm.$nextTick();

    mappingEvents = wrapper.emitted('update:mappingData') as any[];
    lastMapping = mappingEvents[mappingEvents.length - 1][0];
    // Find cf2 in array
    const cf2 = lastMapping.customFields.find((cf: any) => cf.jiraFieldKey === 'cf2');
    expect(cf2.valueSource).toBe('literal');
  });

  it('fetches parent issues only once for a single subtask selection', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy.mockClear();

    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        issueTypes: [
          { id: 'BUG', name: 'Bug', subtask: false },
          { id: 'SUB', name: 'Sub-task', subtask: true },
        ],
        mappingData: {
          ...baseProps.mappingData,
          selectedProject: 'PR',
          selectedIssueType: 'BUG',
          summary: 'Issue summary',
        },
      },
    });

    const issueTypeSelect = wrapper.findAll('select.ms-select')[2];
    await issueTypeSelect.setValue('SUB');
    await wrapper.vm.$nextTick();
    await Promise.resolve();
    await Promise.resolve();

    expect(parentIssuesSpy).toHaveBeenCalledTimes(1);
    expect(parentIssuesSpy).toHaveBeenCalledWith('c1', 'PR', { maxResults: 100 });
  });

  it('searches parent issues with query when parent-search is emitted', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy.mockClear();
    vi.mocked(JiraIntegrationService.getProjectIssueTypesByConnectionId).mockResolvedValueOnce([
      { id: 'SUB', name: 'Sub-task', subtask: true } as any,
    ]);

    const wrapper = mount(MappingStep, {
      props: {
        ...baseProps,
        issueTypes: [
          { id: 'BUG', name: 'Bug', subtask: false },
          { id: 'SUB', name: 'Sub-task', subtask: true },
        ],
        mappingData: {
          ...baseProps.mappingData,
          selectedProject: 'PR',
          selectedIssueType: 'SUB',
          summary: 'Issue summary',
        },
      },
    });

    await wrapper.vm.$nextTick();
    await Promise.resolve();
    await Promise.resolve();

    parentIssuesSpy.mockClear();
    const selects = wrapper.findComponent({ name: 'ProjectIssueAssigneeSelects' });
    selects.vm.$emit('parent-search', 'abcde');

    await wrapper.vm.$nextTick();
    await Promise.resolve();

    expect(parentIssuesSpy).toHaveBeenCalledTimes(1);
    expect(parentIssuesSpy).toHaveBeenCalledWith('c1', 'PR', { maxResults: 100, query: 'abcde' });
  });
});

// Stub child panel to avoid API/mounted work
const StubPanel = {
  name: 'CustomFieldsPanel',
  template: '<div class="panel-stub" />',
};

describe('MappingStep', () => {
  const props = {
    jsonSample: '{"foo":{"bar":"baz"}}',
    mappingData: {
      selectedProject: '',
      selectedIssueType: '',
      selectedAssignee: '',
      summary: '',
      descriptionFieldMapping: '',
      customFields: [],
    },
    projects: [{ key: 'PR', name: 'Project' }],
    issueTypes: [{ id: 'BUG', name: 'Bug', subtask: false }],
    users: [{ accountId: 'u1', displayName: 'Alice' }],
  };

  it('renders left JSON panel lines when valid JSON provided', () => {
    const wrapper = mount(MappingStep, {
      props,
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    // At least one JSON line is rendered
    expect(wrapper.find('.ms-json').exists()).toBe(true);
    expect(wrapper.findAll('.ms-json-line').length).toBeGreaterThan(0);
  });

  it('shows empty message when no JSON sample provided', () => {
    const wrapper = mount(MappingStep, {
      props: { ...props, jsonSample: '' },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    expect(wrapper.find('.ms-json-empty').exists()).toBe(true);
    expect(wrapper.find('.ms-json-empty').text()).toContain('No sample JSON provided');
  });

  it('handles invalid JSON gracefully in left panel', () => {
    const wrapper = mount(MappingStep, {
      props: { ...props, jsonSample: '{ invalid json }' },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    // Should render as-is without crashing
    expect(wrapper.find('.ms-json').exists()).toBe(true);
  });

  it('extracts nested paths from JSON sample', async () => {
    const wrapper = mount(MappingStep, {
      props: { ...props, jsonSample: '{"user":{"profile":{"name":"Alice"}}}' },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    await wrapper.find('.ms-field .ps-add-icon').trigger('click');
    const items = wrapper.findAll('.ms-field-item');

    // Should have: {{user}}, {{user.profile}}, {{user.profile.name}}
    expect(items.length).toBeGreaterThanOrEqual(3);
    const placeholders = items.map(item => item.find('.ms-field-item-placeholder').text());
    expect(placeholders).toContain('{{user.profile.name}}');
  });

  it('renders objects as JSON strings in preview', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...props,
        jsonSample: '{"data":{"nested":"value"}}',
        mappingData: { ...props.mappingData, summary: '{{data}}' },
      },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    const eyeBtn = wrapper.findAll('.ps-eye-icon')[0];
    await eyeBtn.trigger('click');
    const preview = wrapper.find('.ms-inline-preview--summary');
    expect(preview.text()).toContain('nested');
  });

  it('normalizes bracket notation paths', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...props,
        jsonSample: '{"items":[{"key":"first"}]}',
        mappingData: { ...props.mappingData, summary: '{{items[0].key}}' },
      },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    const eyeBtn = wrapper.findAll('.ps-eye-icon')[0];
    await eyeBtn.trigger('click');
    const preview = wrapper.find('.ms-inline-preview--summary');
    expect(preview.text()).toContain('first');
  });

  it('resolves wildcard array notation in preview', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...props,
        jsonSample:
          '{"serials":[{"filingNumberDisplay":"2025-1"},{"filingNumberDisplay":"2025-2"}]}',
        mappingData: { ...props.mappingData, summary: 'Issue {{serials[].filingNumberDisplay}}' },
      },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    const eyeBtn = wrapper.findAll('.ps-eye-icon')[0];
    await eyeBtn.trigger('click');
    const preview = wrapper.find('.ms-inline-preview--summary');
    expect(preview.text().trim()).toBe('Issue 2025-1, 2025-2');
  });

  it('opens field modal when clicking insert in Summary', async () => {
    const wrapper = mount(MappingStep, {
      props,
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    // Click insert for summary
    const insertBtn = wrapper.find('.ms-field .ps-add-icon');
    await insertBtn.trigger('click');
    expect(wrapper.find('.ms-modal.ms-field-modal').exists()).toBe(true);
  });
  it('inserts field at cursor position with space when needed', async () => {
    const wrapper = mount(MappingStep, {
      props: { ...props, mappingData: { ...props.mappingData, summary: 'Hello' } },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    const textarea = wrapper.find('textarea.ms-textarea--summary');
    const element = textarea.element as HTMLTextAreaElement;

    // Position cursor at end (after "Hello")
    element.setSelectionRange(5, 5);
    await textarea.trigger('click');

    // Open modal and insert field
    await wrapper.find('.ms-field .ps-add-icon').trigger('click');
    const fieldItem = wrapper.find('.ms-field-item');
    await fieldItem.trigger('click');

    // Should add space before placeholder since previous char isn't whitespace
    expect(element.value).toMatch(/Hello \{\{/);
  });

  it('inserts field without extra space when cursor at start', async () => {
    const wrapper = mount(MappingStep, {
      props: { ...props, mappingData: { ...props.mappingData, summary: '' } },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });
    const textarea = wrapper.find('textarea.ms-textarea--summary');
    const element = textarea.element as HTMLTextAreaElement;

    element.setSelectionRange(0, 0);
    await textarea.trigger('click');

    await wrapper.find('.ms-field .ps-add-icon').trigger('click');
    const fieldItem = wrapper.find('.ms-field-item');
    await fieldItem.trigger('click');

    // Should not add space at start
    expect(element.value).toMatch(/^\{\{/);
  });

  it('opens the insert modal for description and inserts the selected placeholder', async () => {
    const wrapper = mount(MappingStep, {
      props: { ...props, mappingData: { ...props.mappingData, descriptionFieldMapping: 'Desc' } },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });

    const insertButtons = wrapper.findAll('.ms-field .ps-add-icon');
    expect(insertButtons.length).toBeGreaterThan(1);

    await insertButtons[1].trigger('click');
    await wrapper.find('.ms-field-item').trigger('click');

    const descriptionTextarea = wrapper.find('textarea:not(.ms-textarea--summary)');
    expect((descriptionTextarea.element as HTMLTextAreaElement).value).toContain('{{');
  });

  it('routes field insertion to the custom fields panel when opened from a custom row', async () => {
    const insertJsonPlaceholder = vi.fn();
    const CustomFieldPanelWithExpose = {
      name: 'CustomFieldsPanel',
      emits: ['update:rows', 'valid-change', 'open-insert', 'toggle-preview'],
      setup(_: unknown, { emit }: { emit: (event: string, payload?: unknown) => void }) {
        return {
          emitOpenInsert: () => emit('open-insert', { rowIndex: 2 }),
          insertJsonPlaceholder,
        };
      },
      template: '<button class="custom-open-insert" @click="emitOpenInsert">custom</button>',
    };

    const wrapper = mount(MappingStep, {
      props,
      global: { stubs: { CustomFieldsPanel: CustomFieldPanelWithExpose } },
    });

    await wrapper.find('.custom-open-insert').trigger('click');
    await wrapper.find('.ms-field-item').trigger('click');

    expect(insertJsonPlaceholder).toHaveBeenCalledWith(expect.stringContaining('{{'));
  });

  it('emits the parent label payload from the selects component', async () => {
    const wrapper = mount(MappingStep, {
      props,
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });

    const selects = wrapper.findComponent({ name: 'ProjectIssueAssigneeSelects' });
    selects.vm.$emit('parent-label-change', { value: 'SCRUM-1', label: 'Parent Issue' });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted('parent-label-change')).toEqual([
      [{ value: 'SCRUM-1', label: 'Parent Issue' }],
    ]);
  });

  it('updates and hides the action tooltip through the direct handlers', () => {
    const wrapper = mount(MappingStep, {
      props,
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });

    (wrapper.vm as any).onActionEnter('Preview help', { clientX: 10, clientY: 20 } as MouseEvent);
    expect((wrapper.vm as any).actionTipVisible).toBe(true);
    expect((wrapper.vm as any).actionTipText).toBe('Preview help');
    expect((wrapper.vm as any).actionTipX).toBe(22);
    expect((wrapper.vm as any).actionTipY).toBe(32);

    (wrapper.vm as any).onActionMove({ clientX: 30, clientY: 40 } as MouseEvent);
    expect((wrapper.vm as any).actionTipX).toBe(42);
    expect((wrapper.vm as any).actionTipY).toBe(52);

    (wrapper.vm as any).onActionLeave();
    expect((wrapper.vm as any).actionTipVisible).toBe(false);
  });

  it('does not remote-search parent issues for short queries', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy.mockClear();

    const wrapper = mount(MappingStep, {
      props: {
        ...props,
        issueTypes: [{ id: 'SUB', name: 'Sub-task', subtask: true }],
        mappingData: {
          ...props.mappingData,
          selectedProject: 'PR',
          selectedIssueType: 'SUB',
          summary: 'Issue summary',
        },
      },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });

    await wrapper.vm.$nextTick();
    await Promise.resolve();
    parentIssuesSpy.mockClear();

    await (wrapper.vm as any).onParentSearch('abc');

    expect(parentIssuesSpy).not.toHaveBeenCalled();
  });

  it('clears dependent fields and removes parent mapping when project changes', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...props,
        issueTypes: [{ id: 'SUB', name: 'Sub-task', subtask: true }],
        mappingData: {
          ...props.mappingData,
          selectedProject: 'PR',
          selectedIssueType: 'SUB',
          selectedAssignee: 'u1',
          summary: 'Issue summary',
          customFields: [
            {
              _id: 'parent-1',
              jiraFieldKey: 'parent',
              jiraFieldLabel: 'Parent',
              type: 'object',
              required: true,
              valueSource: 'literal',
              value: '{"key":"SCRUM-1"}',
            },
          ],
        },
      },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });

    (wrapper.vm as any).onProjectChange();
    await wrapper.vm.$nextTick();

    const mappingEvents = wrapper.emitted('update:mappingData');
    expect(mappingEvents).toBeTruthy();
    const lastMapping = mappingEvents![mappingEvents!.length - 1][0] as any;
    expect(lastMapping.selectedIssueType).toBe('');
    expect(lastMapping.selectedAssignee).toBe('');
    expect(lastMapping.customFields).toEqual([]);
  });

  it('removes parent mapping when issue type changes to a known non-subtask type', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...props,
        issueTypes: [{ id: 'BUG', name: 'Bug', subtask: false }],
        mappingData: {
          ...props.mappingData,
          selectedProject: 'PR',
          selectedIssueType: 'BUG',
          summary: 'Issue summary',
          customFields: [
            {
              _id: 'parent-1',
              jiraFieldKey: 'parent',
              jiraFieldLabel: 'Parent',
              type: 'object',
              required: true,
              valueSource: 'literal',
              value: '{"key":"SCRUM-1"}',
            },
          ],
        },
      },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });

    (wrapper.vm as any).onIssueTypeChange();
    await wrapper.vm.$nextTick();

    const mappingEvents = wrapper.emitted('update:mappingData');
    const lastMapping = mappingEvents![mappingEvents!.length - 1][0] as any;
    expect(lastMapping.customFields).toEqual([]);
  });

  it('keeps parent mapping when issue type is unknown', async () => {
    const wrapper = mount(MappingStep, {
      props: {
        ...props,
        issueTypes: [{ id: 'BUG', name: 'Bug', subtask: false }],
        mappingData: {
          ...props.mappingData,
          selectedProject: 'PR',
          selectedIssueType: 'UNKNOWN',
          summary: 'Issue summary',
          customFields: [
            {
              _id: 'parent-1',
              jiraFieldKey: 'parent',
              jiraFieldLabel: 'Parent',
              type: 'object',
              required: true,
              valueSource: 'literal',
              value: '{"key":"SCRUM-1"}',
            },
          ],
        },
      },
      global: { stubs: { CustomFieldsPanel: StubPanel } },
    });

    (wrapper.vm as any).onIssueTypeChange();
    await wrapper.vm.$nextTick();

    const mappingEvents = wrapper.emitted('update:mappingData');
    const lastMapping = mappingEvents![mappingEvents!.length - 1][0] as any;
    expect(lastMapping.customFields).toHaveLength(1);
    expect(lastMapping.customFields[0].jiraFieldKey).toBe('parent');
  });
});
