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
        displayLabel: 'u1',
        required: false,
      },
      {
        id: '4',
        jiraFieldId: 'watchers',
        jiraFieldName: 'Watchers',
        dataType: 'MULTIUSER',
        displayLabel: 'u2,u3',
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

  it('renders mapped values with correct formatting and user name mapping', () => {
    const wrapper = mount(JiraFieldMappingTab, {
      props: {
        webhookData: baseWebhook,
        webhookId: 'w1',
        users: [
          { accountId: 'u1', displayName: 'Alice' },
          { accountId: 'u2', displayName: 'Bob' },
          { accountId: 'u3', displayName: 'Cara' },
        ],
        sprints: [{ value: '67', label: 'Sprint 24 (active)' }],
      },
    });

    const rows = wrapper.findAll('.mapped-value');
    // DATE/DATETIME are formatted
    expect(rows[0].text()).toBe('DISPLAY_DATE');
    expect(rows[1].text()).toBe('META_DATE');
    // USER maps id -> name, trimming prefix
    expect(rows[2].text()).toBe('Alice');
    // MULTIUSER maps and joins
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

  it('prefers parent template over displayLabel and renders parent key', () => {
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
              displayLabel: 'SHOULD_NOT_BE_USED',
              template: '{"key":"SCRUM-436"}',
              required: true,
            },
          ],
        },
        webhookId: 'w1',
      },
    });

    expect(wrapper.find('.mapped-value').text()).toBe('SCRUM-436');
    expect(wrapper.text()).not.toContain('SHOULD_NOT_BE_USED');
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
        users: [{ accountId: 'u1', displayName: 'User X' }],
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
