/* eslint-disable simple-import-sort/imports */
import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { nextTick } from 'vue';
import JiraWebhookWizard from '@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue';

// Mocks (use vi.hoisted for variables referenced by vi.mock factories)
const showSuccess = vi.hoisted(() => vi.fn());
const showError = vi.hoisted(() => vi.fn());
const showWarning = vi.hoisted(() => vi.fn());

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({ showSuccess, showError, showWarning }),
}));

const jiraWebhookServiceMock = vi.hoisted(() => ({
  createWebhook: vi.fn(),
  updateWebhook: vi.fn(),
  listJiraWebhooks: vi.fn().mockResolvedValue([]),
  getAllJiraNormalizedNames: vi.fn().mockResolvedValue([]),
}));
vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: jiraWebhookServiceMock,
}));

const createFieldMappingsMock = vi.hoisted(() => vi.fn());
vi.mock('@/utils/jiraFieldMappingUtils', () => ({
  createFieldMappings: createFieldMappingsMock,
}));

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectsByConnectionId: vi.fn().mockResolvedValue([{ key: 'P1', name: 'Project One' }]),
    getProjectIssueTypesByConnectionId: vi.fn().mockResolvedValue([{ id: 'BUG', name: 'Bug' }]),
    getProjectUsersByConnectionId: vi
      .fn()
      .mockResolvedValue([{ accountId: 'u1', displayName: 'Alice' }]),
  },
}));

// Helper stubs for child components
const BasicStub: any = {
  name: 'BasicDetailsStep',
  template: '<div class="basic-stub" />',
  emits: ['validation-change', 'update:integrationName', 'update:description'],
  mounted() {
    (this as any).$emit('update:integrationName', 'Name');
    (this as any).$emit('update:description', 'Desc');
    (this as any).$emit('validation-change', true);
  },
};

const SampleStub: any = {
  name: 'SampleDataStep',
  template: '<div class="sample-stub" />',
  props: ['jsonSample'],
  emits: ['json-validity-change', 'update:jsonSample'],
  mounted() {
    // emit invalid first by default; tests toggle as needed
    (this as any).$emit('update:jsonSample', '');
    (this as any).$emit('json-validity-change', false);
  },
};

function makeConnectStub(
  initial: { method?: 'oauth' | 'apiToken'; connected?: boolean; id?: string; saved?: number } = {}
) {
  return {
    name: 'ConnectionStep',
    template: '<div class="connect-stub" />',
    emits: [
      'update:modelValue',
      'validation-change',
      'connection-success',
      'update:connectionId',
      'update:jiraConnected',
    ],
    mounted() {
      if (!initial.connected) return;

      const connectionData = {
        connectionMethod: 'existing',
        credentialType: initial.method === 'apiToken' ? 'BASIC_AUTH' : 'OAUTH2',
        connected: true,
        existingConnectionId: initial.id,
        createdConnectionId: undefined,
        baseUrl: '',
        username: '',
        password: '',
        connectionName: '',
      };

      (this as any).$emit('update:modelValue', connectionData);
      (this as any).$emit('validation-change', true);

      // Intentionally do NOT emit connection-success here.
      // The Jira webhook wizard should be able to proceed for existing connections
      // based solely on a verified selection (validation-change + v-model id).
    },
  };
}

const MappingStub = {
  template: '<div class="mapping-stub" />',
  emits: ['validation-change', 'update:mappingData'],
  mounted() {
    // allow tests to control mapping validity by emitting later
  },
};

const SuccessStub = {
  props: ['open', 'webhook', 'editMode'],
  emits: ['close', 'copy-url'],
  template:
    '<div class="success-stub"><button class="close" @click="$emit(\'close\')">x</button></div>',
};

// Clipboard polyfill for copy test
Object.assign(globalThis, {
  navigator: {
    clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
  },
});

describe('JiraWebhookWizard branches', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gates step 1 and 2 Next button by validity and saved connections (oauth)', async () => {
    const wrapper = mount(JiraWebhookWizard, {
      props: { open: true },
      global: {
        stubs: {
          BasicDetailsStep: BasicStub,
          SampleDataStep: SampleStub,
          ConnectionStep: makeConnectStub({
            method: 'oauth',
            connected: false,
            id: 'c1',
            saved: 0,
          }),
          MappingStep: MappingStub,
          PreviewStep: { template: '<div />' },
          WebhookSuccessDialog: SuccessStub,
        },
      },
    });

    const nextBtn = () => wrapper.find('.jw-footer-right .jw-btn-primary');

    // At step 0, Basic stub makes valid, so Next enabled
    await flushPromises();
    await nextTick();
    // initCreateMode runs after the async open hook; re-assert valid state after it completes
    (wrapper.vm as any).integrationName = 'Name';
    (wrapper.vm as any).isBasicDetailsValid = true;
    await nextTick();
    expect(nextBtn().attributes('disabled')).toBeUndefined();

    await nextBtn().trigger('click'); // -> step 1
    // Sample stub emitted invalid; Next should be disabled
    expect(nextBtn().attributes('disabled')).toBeDefined();

    // Make JSON valid
    wrapper.findComponent(SampleStub as any).vm.$emit('update:jsonSample', '{"a":1}');
    wrapper.findComponent(SampleStub as any).vm.$emit('json-validity-change', true);
    await nextTick();
    expect(nextBtn().attributes('disabled')).toBeUndefined();

    await nextBtn().trigger('click'); // -> step 2 (connect)
    // Connection not validated yet, so button should be disabled
    expect(nextBtn().attributes('disabled')).toBeDefined();

    // Existing connection: update v-model + emit validation-change true (no connection-success)
    const connectStub = wrapper.findComponent({ name: 'ConnectionStep' });
    connectStub.vm.$emit('update:modelValue', {
      connectionMethod: 'existing',
      credentialType: 'OAUTH2',
      connected: true,
      existingConnectionId: 'conn-123',
      createdConnectionId: undefined,
      baseUrl: '',
      username: '',
      password: '',
      connectionName: '',
    });
    connectStub.vm.$emit('validation-change', true);
    await nextTick();
    expect(nextBtn().attributes('disabled')).toBe(undefined);
  });

  it('connection change at step 2 resets mapping validity in edit mode', async () => {
    const editingWebhookData = {
      id: 'w1',
      name: 'Orig',
      description: 'D',
      webhookUrl: 'https://x',
      connectionId: 'c1',
      samplePayload: '{"a":1}',
      jiraFieldMappings: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: 'Project',
          dataType: 'OBJECT',
          template: 'P1',
          required: true,
          defaultValue: null,
          metadata: undefined,
        },
        {
          jiraFieldId: 'issuetype',
          jiraFieldName: 'Issue Type',
          displayLabel: 'Issue Type',
          dataType: 'OBJECT',
          template: 'BUG',
          required: true,
          defaultValue: null,
          metadata: undefined,
        },
        {
          jiraFieldId: 'summary',
          jiraFieldName: 'Summary',
          displayLabel: 'Summary',
          dataType: 'STRING',
          template: 'S',
          required: true,
          defaultValue: null,
          metadata: undefined,
        },
      ],
      isEnabled: true,
      isDeleted: false,
      normalizedName: 'orig',
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastModifiedBy: 'tester',
      lastModifiedDate: '2024-01-01T00:00:00Z',
      tenantId: 'tenant',
      version: 1,
    };

    const wrapper = mount(JiraWebhookWizard, {
      props: { open: true, editMode: true, editingWebhookData },
      global: {
        stubs: {
          BasicDetailsStep: BasicStub,
          SampleDataStep: {
            ...SampleStub,
            mounted() {
              this.$emit('update:jsonSample', '{"a":1}');
              this.$emit('json-validity-change', true);
            },
          },
          ConnectionStep: makeConnectStub({ method: 'oauth', connected: true, id: 'c1', saved: 1 }),
          MappingStep: MappingStub,
          PreviewStep: { template: '<div />' },
          WebhookSuccessDialog: SuccessStub,
        },
      },
    });

    const nextBtn = () => wrapper.find('.jw-footer-right .jw-btn-primary');
    await flushPromises();
    await nextTick();

    // Drive wizard state directly to step 2 (connect) to avoid timing issues
    const vm: any = wrapper.vm as any;
    vm.activeStep = 2;
    vm.jiraConnected = true;
    vm.authMethod = 'oauth';
    vm.savedConnectionsCount = 1;
    vm.connectionId = 'c1';
    vm.originalConnectionIdRef = 'c1';
    vm.isMappingValid = true;
    await nextTick();

    // At step 2 currently valid; simulate connection change to new id
    vm.connectionId = 'c2';
    vm.jiraConnected = true;
    await nextTick();

    // Proceed to step 3; mapping validity should be reset -> Next disabled
    await vm.handleNext();
    await nextTick();
    expect(vm.activeStep).toBe(3);
    expect(nextBtn().attributes('disabled')).toBeDefined();
  });

  it('create flow: shows success dialog; update flow: shows toast and closes', async () => {
    // Arrange mappings to be valid and >= 3
    createFieldMappingsMock.mockReturnValue([
      {
        jiraFieldId: 'project',
        jiraFieldName: 'Project',
        displayLabel: 'Project',
        dataType: 'OBJECT',
        template: 'P1',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
      {
        jiraFieldId: 'issuetype',
        jiraFieldName: 'Issue Type',
        displayLabel: 'Issue Type',
        dataType: 'OBJECT',
        template: 'BUG',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
      {
        jiraFieldId: 'summary',
        jiraFieldName: 'Summary',
        displayLabel: 'Summary',
        dataType: 'STRING',
        template: 'S',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
    ]);
    jiraWebhookServiceMock.createWebhook.mockResolvedValue({
      name: 'New',
      webhookUrl: 'https://hook',
    });

    const wrapper = mount(JiraWebhookWizard, {
      props: { open: true },
      global: {
        stubs: {
          BasicDetailsStep: BasicStub,
          SampleDataStep: {
            ...SampleStub,
            mounted() {
              this.$emit('update:jsonSample', '{"a":1}');
              this.$emit('json-validity-change', true);
            },
          },
          ConnectionStep: makeConnectStub({
            method: 'apiToken',
            connected: true,
            id: 'c1',
            saved: 0,
          }),
          MappingStep: {
            ...MappingStub,
            mounted() {
              this.$emit('validation-change', true);
              this.$emit('update:mappingData', {
                selectedProject: 'P1',
                selectedIssueType: 'BUG',
                selectedAssignee: '',
                summary: 'S',
                descriptionFieldMapping: '',
              });
            },
          },
          PreviewStep: { template: '<div class="preview-stub" />' },
          WebhookSuccessDialog: SuccessStub,
        },
      },
    });

    const nextBtn = () => wrapper.find('.jw-footer-right .jw-btn-primary');
    await flushPromises();
    await nextTick();
    // initCreateMode runs after the async open hook; ensure basic details are valid
    (wrapper.vm as any).integrationName = 'Name';
    (wrapper.vm as any).isBasicDetailsValid = true;
    await nextTick();
    // step0 -> 1
    await nextBtn().trigger('click');
    // step1 -> 2
    await nextBtn().trigger('click');
    // step2 -> 3
    await nextBtn().trigger('click');
    // step3 -> 4
    await nextBtn().trigger('click');

    // Final submit
    await nextBtn().trigger('click');
    await flushPromises();
    await nextTick();
    // Success dialog shown
    expect(wrapper.find('.success-stub').exists()).toBe(true);
    // Copy action
    await wrapper.findComponent(SuccessStub as any).vm.$emit('copy-url');
    expect(navigator.clipboard.writeText as any).toHaveBeenCalled();
    // Close success -> emits close + webhook-created
    await wrapper.find('.success-stub .close').trigger('click');
    expect(wrapper.emitted('close')).toBeTruthy();
    expect(wrapper.emitted('webhook-created')).toBeTruthy();

    // Now test update path
    jiraWebhookServiceMock.createWebhook.mockReset();
    createFieldMappingsMock.mockReturnValue([
      {
        jiraFieldId: 'project',
        jiraFieldName: 'Project',
        displayLabel: 'Project',
        dataType: 'OBJECT',
        template: 'P1',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
      {
        jiraFieldId: 'issuetype',
        jiraFieldName: 'Issue Type',
        displayLabel: 'Issue Type',
        dataType: 'OBJECT',
        template: 'BUG',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
      {
        jiraFieldId: 'summary',
        jiraFieldName: 'Summary',
        displayLabel: 'Summary',
        dataType: 'STRING',
        template: 'S',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
    ]);
    jiraWebhookServiceMock.updateWebhook.mockResolvedValue(undefined);

    const editData = {
      id: 'w1',
      name: 'E',
      description: '',
      webhookUrl: 'x',
      connectionId: 'c1',
      jiraFieldMappings: [],
      samplePayload: '{"a":1}',
      isEnabled: true,
      isDeleted: false,
      normalizedName: 'e',
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastModifiedBy: 'tester',
      lastModifiedDate: '2024-01-01T00:00:00Z',
      tenantId: 'tenant',
      version: 1,
    };
    const edit = mount(JiraWebhookWizard, {
      props: { open: true, editMode: true, editingWebhookData: editData },
      global: {
        stubs: {
          BasicDetailsStep: BasicStub,
          SampleDataStep: {
            ...SampleStub,
            mounted() {
              this.$emit('update:jsonSample', '{"a":1}');
              this.$emit('json-validity-change', true);
            },
          },
          ConnectionStep: makeConnectStub({
            method: 'apiToken',
            connected: true,
            id: 'c1',
            saved: 0,
          }),
          MappingStep: {
            ...MappingStub,
            mounted() {
              this.$emit('validation-change', true);
              this.$emit('update:mappingData', {
                selectedProject: 'P1',
                selectedIssueType: 'BUG',
                selectedAssignee: '',
                summary: 'S',
                descriptionFieldMapping: '',
              });
            },
          },
          PreviewStep: { template: '<div />' },
          WebhookSuccessDialog: SuccessStub,
        },
      },
    });

    const n = () => edit.find('.jw-footer-right .jw-btn-primary');
    await flushPromises();
    await nextTick();
    await n().trigger('click'); // 0->1
    await n().trigger('click'); // 1->2
    await n().trigger('click'); // 2->3
    await n().trigger('click'); // 3->4
    await n().trigger('click'); // submit update
    await flushPromises();
    await nextTick();
    expect(jiraWebhookServiceMock.updateWebhook).toHaveBeenCalled();
    expect(showSuccess).toHaveBeenCalled();
    expect(edit.emitted('close')).toBeTruthy();
    expect(edit.emitted('webhook-created')).toBeTruthy();
  });

  it('shows errors for insufficient mappings and invalid JSON on submit', async () => {
    // First: insufficient mappings
    createFieldMappingsMock.mockReturnValue([
      {
        jiraFieldId: 'project',
        jiraFieldName: 'Project',
        displayLabel: 'Project',
        dataType: 'OBJECT',
        template: 'P1',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
    ]);
    const wrapper = mount(JiraWebhookWizard, {
      props: { open: true },
      global: {
        stubs: {
          BasicDetailsStep: BasicStub,
          SampleDataStep: {
            ...SampleStub,
            mounted() {
              this.$emit('update:jsonSample', '{"a":1}');
              this.$emit('json-validity-change', true);
            },
          },
          ConnectionStep: makeConnectStub({ method: 'apiToken', connected: true, id: 'c1' }),
          MappingStep: {
            ...MappingStub,
            mounted() {
              this.$emit('validation-change', true);
              this.$emit('update:mappingData', {
                selectedProject: 'P1',
                selectedIssueType: 'BUG',
                selectedAssignee: '',
                summary: 'S',
                descriptionFieldMapping: '',
              });
            },
          },
          PreviewStep: { template: '<div />' },
          WebhookSuccessDialog: SuccessStub,
        },
      },
    });
    const nextBtn = () => wrapper.find('.jw-footer-right .jw-btn-primary');
    await flushPromises();
    await nextTick();
    // initCreateMode runs after the async open hook; ensure basic details are valid
    (wrapper.vm as any).integrationName = 'Name';
    (wrapper.vm as any).isBasicDetailsValid = true;
    await nextTick();
    await nextBtn().trigger('click'); // 0->1
    await nextBtn().trigger('click'); // 1->2
    await nextBtn().trigger('click'); // 2->3
    await nextBtn().trigger('click'); // 3->4
    await nextBtn().trigger('click'); // submit
    await nextTick();
    expect(showError).toHaveBeenCalledWith('At least 3 field mappings are required.');

    // Reset error spy before second scenario
    showError.mockClear();
    // Second: invalid JSON
    createFieldMappingsMock.mockReturnValue([
      {
        jiraFieldId: 'project',
        jiraFieldName: 'Project',
        displayLabel: 'Project',
        dataType: 'OBJECT',
        template: 'P1',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
      {
        jiraFieldId: 'issuetype',
        jiraFieldName: 'Issue Type',
        displayLabel: 'Issue Type',
        dataType: 'OBJECT',
        template: 'BUG',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
      {
        jiraFieldId: 'summary',
        jiraFieldName: 'Summary',
        displayLabel: 'Summary',
        dataType: 'STRING',
        template: 'S',
        required: true,
        defaultValue: null,
        metadata: undefined,
      },
    ]);
    const wrap2 = mount(JiraWebhookWizard, {
      props: { open: true },
      global: {
        stubs: {
          BasicDetailsStep: BasicStub,
          SampleDataStep: {
            ...SampleStub,
            mounted() {
              this.$emit('update:jsonSample', 'not-json');
              this.$emit('json-validity-change', false);
            },
          },
          ConnectionStep: makeConnectStub({ method: 'apiToken', connected: true, id: 'c1' }),
          MappingStep: {
            ...MappingStub,
            mounted() {
              this.$emit('validation-change', true);
              this.$emit('update:mappingData', {
                selectedProject: 'P1',
                selectedIssueType: 'BUG',
                selectedAssignee: '',
                summary: 'S',
                descriptionFieldMapping: '',
              });
            },
          },
          PreviewStep: { template: '<div />' },
          WebhookSuccessDialog: SuccessStub,
        },
      },
    });
    await flushPromises();
    await nextTick();
    // Populate required state for canCreateWebhook without navigating steps
    const vm: any = wrap2.vm as any;
    vm.integrationName = 'Name';
    vm.isBasicDetailsValid = true;
    vm.connectionId = 'c1';
    vm.mappingDataRef = {
      selectedProject: 'P1',
      selectedIssueType: 'BUG',
      selectedAssignee: '',
      summary: 'S',
      descriptionFieldMapping: '',
    };
    vm.jsonSample = 'not-json';
    await nextTick();
    // Directly invoke submit handler to reach JSON parse branch
    await vm.handleCreateWebhook();
    await nextTick();
    expect(showError).toHaveBeenCalledWith('Invalid JSON payload.');
  });
});
