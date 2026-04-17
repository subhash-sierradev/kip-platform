/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import JiraFieldMappingTab from '@/components/outbound/jirawebhooks/details/JiraFieldMappingTab.vue';

vi.mock('@/components/common/GenericDataGrid.vue', () => ({
  default: {
    name: 'GenericDataGrid',
    props: ['data', 'columns', 'pageSize', 'enableExport', 'enableClearFilters', 'exportConfig'],
    template:
      '<div class="grid-stub"><div v-for="r in (data||[])" class="row"><slot name="fieldNameTemplate" :data="r"/><slot name="typeTemplate" :data="r"/><slot name="mappedValueTemplate" :data="r"/><slot name="requiredTemplate" :data="r"/></div></div>',
  },
}));

vi.mock('@/utils/dateUtils', () => ({
  formatDisplayDate: () => 'DISPLAY_DATE',
  formatMetadataDate: () => 'META_DATE',
}));

describe('JiraFieldMappingTab', () => {
  const baseWebhook = {
    id: 'w1',
    name: 'Web',
    webhookUrl: 'http://x',
    jiraFieldMappings: [
      {
        id: '1',
        jiraFieldId: 'cf_date',
        jiraFieldName: 'Due',
        dataType: 'DATE',
        displayLabel: '2024-01-01',
        required: true,
      },
      {
        id: '2',
        jiraFieldId: 'cf_dt',
        jiraFieldName: 'When',
        dataType: 'DATETIME',
        displayLabel: '2024-01-01T10:00:00Z',
        required: false,
      },
      {
        id: '3',
        jiraFieldId: 'assignee',
        jiraFieldName: 'Assignee',
        dataType: 'USER',
        displayLabel: 'Alice',
        required: false,
      },
      {
        id: '4',
        jiraFieldId: 'watchers',
        jiraFieldName: 'Watchers',
        dataType: 'MULTIUSER',
        displayLabel: 'Bob, Cara',
        required: false,
      },
      {
        id: '5',
        jiraFieldId: 'tmpl',
        jiraFieldName: 'Template',
        dataType: 'STRING',
        displayLabel: '{{ fields.summary }}',
        required: false,
      },
      {
        id: '6',
        jiraFieldId: 'plain',
        jiraFieldName: 'Plain',
        dataType: 'STRING',
        displayLabel: 'hello',
        required: false,
      },
      {
        id: '7',
        jiraFieldId: 'customfield_10021',
        jiraFieldName: 'Sprint',
        dataType: 'NUMBER',
        displayLabel: '67',
        required: false,
        metadata: { fieldType: 'sprint' },
      },
    ],
  } as any;

  it('renders mapped values with correct formatting', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: baseWebhook,
        webhookId: 'w1',
        sprints: [{ value: '67', label: 'Sprint 24 (active)' }],
      },
    });

    const rows = wrapper.findAll('.mapped-value');
    // DATE/DATETIME are formatted
    expect(rows[0].text()).toBe('DISPLAY_DATE');
    expect(rows[1].text()).toBe('META_DATE');
    // USER and MULTIUSER use persisted display labels
    expect(rows[2].text()).toBe('Alice');
    expect(rows[3].text()).toBe('Bob, Cara');
    // Template with {{}} remains unchanged
    expect(rows[4].text()).toBe('{{ fields.summary }}');
    // Plain string shown as-is
    expect(rows[5].text()).toBe('hello');
    // Sprint number displays sprint label
    expect(rows[6].text()).toBe('Sprint 24 (active)');
  });

  it('renders status chip for required values', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: baseWebhook,
        webhookId: 'w1',
      },
      global: {
        stubs: {
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label || status }}</span>',
          },
        },
      },
    });

    expect(wrapper.findAll('.status-chip-stub').length).toBe(baseWebhook.jiraFieldMappings.length);
  });

  it('shows empty state when no field mappings', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          id: 'w1',
          name: 'Empty',
          description: '',
          webhookUrl: '',
          connectionId: '',
          jiraFieldMappings: [],
          samplePayload: '',
          isEnabled: false,
          isDeleted: false,
          normalizedName: '',
          createdBy: '',
          createdDate: '',
          lastModifiedBy: '',
          lastModifiedDate: '',
          tenantId: '',
          version: 0,
        },
        webhookId: 'w1',
        loading: false,
      },
    });
    // With our grid stub, empty mappings render zero rows
    expect(wrapper.findAll('.row').length).toBe(0);
  });

  it('prefers parent displayLabel over template', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'parent-1',
              jiraFieldId: 'parent',
              jiraFieldName: 'Parent',
              dataType: 'STRING',
              displayLabel: 'SCRUM-436 - Parent one',
              template: '{"key":"SCRUM-436"}',
              required: true,
            },
          ],
        },
        webhookId: 'w1',
      },
    });

    expect(wrapper.find('.mapped-value').text()).toBe('SCRUM-436 - Parent one');
  });

  it('keeps parent token template unchanged for mapped value display', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'parent-2',
              jiraFieldId: 'parent',
              jiraFieldName: 'Parent',
              dataType: 'STRING',
              template: '{{fields.parentKey}}',
              required: true,
            },
          ],
        },
        webhookId: 'w1',
      },
    });

    expect(wrapper.find('.mapped-value').text()).toBe('{{fields.parentKey}}');
  });

  it('falls back to default values, raw sprint ids, and unknown-field labels when display labels are missing', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'fallback-1',
              jiraFieldId: 'plain_fallback',
              jiraFieldName: 'Plain Fallback',
              dataType: 'STRING',
              defaultValue: 'plain-default',
              required: false,
            },
            {
              id: 'sprint-2',
              jiraFieldId: 'customfield_10021',
              jiraFieldName: 'Sprint',
              dataType: 'NUMBER',
              defaultValue: '77',
              metadata: { fieldType: 'sprint' },
              required: false,
            },
            {
              id: '',
              jiraFieldId: '',
              jiraFieldName: '',
              dataType: 'STRING',
              required: false,
            },
          ],
        },
        webhookId: 'w1',
        sprints: [],
      },
    });

    const rows = wrapper.findAll('.mapped-value');
    expect(rows[0].text()).toBe('plain-default');
    expect(rows[1].text()).toBe('77');
    expect(rows[2].text()).toBe('No mapping');

    const fieldNames = wrapper.findAll('.field-name');
    expect(fieldNames[2].text()).toBe('Unknown Field');
  });

  it('shows a loading sprint label when sprint metadata is still loading', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'sprint-loading',
              jiraFieldId: 'customfield_10021',
              jiraFieldName: 'Sprint',
              dataType: 'NUMBER',
              displayLabel: '91',
              metadata: { fieldType: 'sprint' },
              required: false,
            },
          ],
        },
        webhookId: 'w1',
        sprints: [],
        sprintsLoading: true,
      },
    });

    expect(wrapper.find('.mapped-value').text()).toBe('Loading sprints...');
  });

  it('detects sprint fields from the field name even without metadata', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'sprint-name-only',
              jiraFieldId: 'customfield_55',
              jiraFieldName: 'Sprint',
              dataType: 'string',
              displayLabel: '42,43',
              required: false,
            },
          ],
        },
        webhookId: 'w1',
        sprints: [{ value: '42', label: 'Sprint 42' }],
      },
    });

    expect(wrapper.find('.mapped-value').text()).toBe('Sprint 42, 43');
  });

  it('returns raw values for invalid dates and unparseable parent mappings', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'bad-date',
              jiraFieldId: 'cf_date',
              jiraFieldName: 'Bad Date',
              dataType: 'DATE',
              displayLabel: 'not-a-date',
              required: false,
            },
            {
              id: 'bad-parent',
              jiraFieldId: 'parent',
              jiraFieldName: 'Parent',
              dataType: 'STRING',
              displayLabel: '{"summary":"Missing key"}',
              required: false,
            },
          ],
        },
        webhookId: 'w1',
      },
    });

    const rows = wrapper.findAll('.mapped-value');
    expect(rows[0].text()).toBe('not-a-date');
    expect(rows[1].text()).toBe('{"summary":"Missing key"}');
  });

  it('prefers templates for string and date fields while preserving template tokens', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'string-template-preferred',
              jiraFieldId: 'cf_string',
              jiraFieldName: 'String Template',
              dataType: 'STRING',
              displayLabel: 'Rendered value',
              template: '{{fields.summary}}',
              required: false,
            },
            {
              id: 'date-template-preferred',
              jiraFieldId: 'cf_due',
              jiraFieldName: 'Due Date',
              dataType: 'DATE',
              displayLabel: '2024-01-01',
              template: '{{issue.dueDate}}',
              required: false,
            },
          ],
        },
        webhookId: 'w1',
      },
    });

    const rows = wrapper.findAll('.mapped-value');
    expect(rows[0].text()).toBe('{{fields.summary}}');
    expect(rows[1].text()).toBe('{{issue.dueDate}}');
  });

  it('falls back to a parent default value and clears empty sprint content', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'parent-default',
              jiraFieldId: 'parent',
              jiraFieldName: 'Parent',
              dataType: 'STRING',
              defaultValue: 'SCRUM-999',
              required: false,
            },
            {
              id: 'empty-sprint',
              jiraFieldId: 'customfield_10021',
              jiraFieldName: 'Sprint',
              dataType: 'NUMBER',
              displayLabel: ' , ',
              metadata: { fieldType: 'sprint' },
              required: false,
            },
          ],
        },
        webhookId: 'w1',
        sprints: [{ value: '67', label: 'Sprint 67' }],
      },
    });

    const rows = wrapper.findAll('.mapped-value');
    expect(rows[0].text()).toBe('SCRUM-999');
    expect(rows[1].text()).toBe('');
  });

  it('prefers display labels for non-template-preferred types even when ids and names are missing', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'boolean-fallback',
              jiraFieldId: undefined,
              jiraFieldName: undefined,
              dataType: 'BOOLEAN',
              displayLabel: 'Enabled',
              template: '{{ignored.boolean}}',
              required: false,
            },
          ],
        },
        webhookId: 'w1',
      },
    });

    expect(wrapper.find('.mapped-value').text()).toBe('Enabled');
    expect(wrapper.find('.field-name').text()).toBe('Unknown Field');
  });

  it('falls back to raw sprint ids when the sprint list prop is nullish', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: 'null-sprint-list',
              jiraFieldId: 'customfield_10021',
              jiraFieldName: 'Sprint',
              dataType: 'NUMBER',
              displayLabel: '91',
              metadata: { fieldType: 'sprint' },
              required: false,
            },
          ],
        },
        webhookId: 'w1',
        sprints: null as any,
      },
    });

    expect(wrapper.find('.mapped-value').text()).toBe('91');
  });

  it('uses default mapping values for nullish webhook metadata and non-sprint number fields', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          ...baseWebhook,
          jiraFieldMappings: [
            {
              id: undefined,
              jiraFieldId: undefined,
              jiraFieldName: undefined,
              dataType: undefined,
              defaultValue: 'fallback-value',
              required: false,
            },
            {
              id: 'number-non-sprint',
              jiraFieldId: 'customfield_20000',
              jiraFieldName: undefined,
              dataType: 'NUMBER',
              displayLabel: '123',
              required: false,
            },
          ],
        },
        webhookId: 'w1',
      },
    });

    const rows = wrapper.findAll('.mapped-value');
    expect(rows[0].text()).toBe('fallback-value');
    expect(rows[1].text()).toBe('123');

    const fieldNames = wrapper.findAll('.field-name');
    expect(fieldNames[0].text()).toBe('Unknown Field');
    expect(fieldNames[1].text()).toBe('customfield_20000');
  });

  it('renders zero rows when webhook data is null', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: null,
        webhookId: 'w1',
      },
    });

    expect(wrapper.findAll('.row')).toHaveLength(0);
  });
});

const gridStub = {
  props: ['data'],
  template: '<div class="grid-stub"><div v-for="r in (data||[])" class="row"></div><slot /></div>',
};

describe('JiraFieldMappingTab', () => {
  const webhookData = {
    id: 'w1',
    jiraFieldMappings: [
      {
        id: 'm1',
        jiraFieldName: 'project',
        jiraFieldId: 'project',
        dataType: 'string',
        displayLabel: 'Project A',
        required: true,
      },
      {
        id: 'm2',
        jiraFieldName: 'assignee',
        jiraFieldId: 'assignee',
        dataType: 'user',
        displayLabel: 'User X',
        required: false,
      },
    ],
  } as any;

  it('renders grid when mappings exist, else shows empty state', async () => {
    // With mappings
    let wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData,
        webhookId: 'w1',
        loading: false,
      },
      global: { stubs: { GenericDataGrid: gridStub } },
    });
    expect(wrapper.find('.grid-stub').exists()).toBe(true);
    expect(wrapper.find('.empty-state').exists()).toBe(false);

    // No mappings
    wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: {
          id: 'w2',
          jiraFieldMappings: [],
          name: '',
          description: '',
          webhookUrl: '',
          connectionId: '',
          samplePayload: '',
          isEnabled: false,
          isDeleted: false,
          normalizedName: '',
          createdBy: '',
          createdDate: '',
          lastModifiedBy: '',
          lastModifiedDate: '',
          tenantId: '',
          version: 0,
        },
        webhookId: 'w2',
        loading: false,
      },
      global: { stubs: { GenericDataGrid: gridStub } },
    });
    // No mappings -> zero rows in grid
    expect(wrapper.findAll('.row').length).toBe(0);
  });
});
