import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import ArcGISIntegrationWizard from '@/components/outbound/arcgisintegration/wizard/ArcGISIntegrationWizard.vue';
import ConfluenceIntegrationWizard from '@/components/outbound/confluenceintegration/wizard/ConfluenceIntegrationWizard.vue';
import JiraWebhookWizard from '@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue';
import { useToastStore } from '@/store/toast';

const wizardMocks = vi.hoisted(() => ({
  createArcGISIntegrationFromWizardMock: vi.fn(),
  updateArcGISIntegrationFromWizardMock: vi.fn(),
  createConfluenceIntegrationFromWizardMock: vi.fn(),
  updateConfluenceIntegrationFromWizardMock: vi.fn(),
  createJiraWebhookMock: vi.fn(),
  updateJiraWebhookMock: vi.fn(),
  getAllArcGISNormalizedNamesMock: vi.fn(),
  getArcGISIntegrationByIdMock: vi.fn(),
  getArcGISFieldMappingsMock: vi.fn(),
  getAllConfluenceNormalizedNamesMock: vi.fn(),
  getConfluenceIntegrationByIdMock: vi.fn(),
  getAllJiraNormalizedNamesMock: vi.fn(),
  getProjectsByConnectionIdMock: vi.fn(),
  getProjectIssueTypesByConnectionIdMock: vi.fn(),
  getProjectUsersByConnectionIdMock: vi.fn(),
  createFieldMappingsMock: vi.fn(),
  searchJiraSprintsMock: vi.fn(),
}));

vi.mock('@/composables/useArcGISIntegrationActions', () => ({
  useArcGISIntegrationActions: () => ({
    createIntegrationFromWizard: wizardMocks.createArcGISIntegrationFromWizardMock,
  }),
  useArcGISIntegrationEditor: () => ({
    updateIntegrationFromWizard: wizardMocks.updateArcGISIntegrationFromWizardMock,
  }),
}));

vi.mock('@/composables/useConfluenceIntegrationActions', () => ({
  useConfluenceIntegrationActions: () => ({
    createIntegrationFromWizard: wizardMocks.createConfluenceIntegrationFromWizardMock,
  }),
  useConfluenceIntegrationEditor: () => ({
    updateIntegrationFromWizard: wizardMocks.updateConfluenceIntegrationFromWizardMock,
  }),
}));

vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getAllArcGISNormalizedNames: wizardMocks.getAllArcGISNormalizedNamesMock,
    getArcGISIntegrationById: wizardMocks.getArcGISIntegrationByIdMock,
    getFieldMappings: wizardMocks.getArcGISFieldMappingsMock,
  },
}));

vi.mock('@/api/services/ConfluenceIntegrationService', () => ({
  ConfluenceIntegrationService: {
    getAllNormalizedNames: wizardMocks.getAllConfluenceNormalizedNamesMock,
    getConfluenceIntegrationById: wizardMocks.getConfluenceIntegrationByIdMock,
  },
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    createWebhook: wizardMocks.createJiraWebhookMock,
    updateWebhook: wizardMocks.updateJiraWebhookMock,
    getAllJiraNormalizedNames: wizardMocks.getAllJiraNormalizedNamesMock,
  },
}));

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectsByConnectionId: wizardMocks.getProjectsByConnectionIdMock,
    getProjectIssueTypesByConnectionId: wizardMocks.getProjectIssueTypesByConnectionIdMock,
    getProjectUsersByConnectionId: wizardMocks.getProjectUsersByConnectionIdMock,
  },
}));

vi.mock('@/utils/jiraFieldMappingUtils', () => ({
  createFieldMappings: wizardMocks.createFieldMappingsMock,
}));

vi.mock('@/composables/useJiraSprints', () => ({
  useJiraSprints: () => ({
    options: { value: [] },
    search: wizardMocks.searchJiraSprintsMock,
  }),
}));

vi.mock('@/utils/fieldMappingHelpers', async importOriginal => {
  const actual = await importOriginal<typeof import('@/utils/fieldMappingHelpers')>();
  return {
    ...actual,
    ensureEmptyMappingSlot: (mappings: Array<Record<string, unknown>>) =>
      mappings.length > 0
        ? mappings
        : [
            {
              id: 'integration-slot',
              sourceField: '',
              targetField: '',
              transformationType: '',
              isMandatory: false,
              displayOrder: 0,
            },
          ],
  };
});

const ArcGISIntegrationDetailsStepStub = defineComponent({
  name: 'IntegrationDetailsStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'arcgis-details-step' }, [
        h(
          'button',
          {
            class: 'arcgis-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                name: 'ArcGIS Integration',
                description: 'ArcGIS desc',
                itemType: 'DOCUMENT',
                subType: 'DOCUMENT_FINAL',
                subTypeLabel: 'Final',
                dynamicDocument: '',
                dynamicDocumentLabel: '',
              });
              emit('validation-change', true);
            },
          },
          'validate details'
        ),
      ]);
  },
});

const ArcGISScheduleStepStub = defineComponent({
  name: 'ScheduleConfigurationStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'arcgis-schedule-step' }, [
        h(
          'button',
          {
            class: 'arcgis-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                executionDate: '2026-04-10',
                executionTime: '08:30',
                frequencyPattern: 'DAILY',
                dailyFrequency: '24',
                selectedDays: [],
                selectedMonths: [],
                monthlyPattern: '',
                monthlyWeek: '',
                monthlyWeekday: '',
                monthlyDay: null,
                isExecuteOnMonthEnd: false,
                cronExpression: '',
                businessTimeZone: 'UTC',
              });
              emit('validation-change', true);
            },
          },
          'validate schedule'
        ),
      ]);
  },
});

const ArcGISConnectionStepStub = defineComponent({
  name: 'ConnectionStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'arcgis-connection-step' }, [
        h(
          'button',
          {
            class: 'arcgis-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                connectionMethod: 'existing',
                baseUrl: 'https://arcgis.example.com',
                credentialType: 'OAUTH2',
                connectionName: 'ArcGIS Connection',
                existingConnectionId: 'conn-arcgis',
                createdConnectionId: '',
              });
              emit('validation-change', true);
            },
          },
          'validate connection'
        ),
      ]);
  },
});

const ArcGISFieldMappingStepStub = defineComponent({
  name: 'FieldMappingStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'arcgis-mapping-step' }, [
        h(
          'button',
          {
            class: 'arcgis-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                fieldMappings: [
                  {
                    id: 'map-1',
                    sourceField: 'caseNumber',
                    targetField: 'CASE_NO',
                    transformationType: 'DIRECT',
                    isMandatory: true,
                    displayOrder: 0,
                  },
                ],
              });
              emit('validation-change', true);
            },
          },
          'validate mapping'
        ),
      ]);
  },
});

const ConfluenceIntegrationDetailsStepStub = defineComponent({
  name: 'IntegrationDetailsStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'confluence-details-step' }, [
        h(
          'button',
          {
            class: 'confluence-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                name: 'Confluence Integration',
                description: 'Confluence desc',
                itemType: 'DOCUMENT',
                subType: 'DOCUMENT_FINAL',
                subTypeLabel: 'Final',
                dynamicDocument: '',
                dynamicDocumentLabel: '',
              });
              emit('validation-change', true);
            },
          },
          'validate details'
        ),
      ]);
  },
});

const ConfluenceScheduleStepStub = defineComponent({
  name: 'ScheduleStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'confluence-schedule-step' }, [
        h(
          'button',
          {
            class: 'confluence-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                executionDate: null,
                executionTime: '09:00',
                frequencyPattern: 'DAILY',
                dailyFrequency: '24',
                selectedDays: [],
                selectedMonths: [],
                isExecuteOnMonthEnd: false,
                cronExpression: undefined,
                businessTimeZone: 'UTC',
                timeCalculationMode: 'FIXED_DAY_BOUNDARY',
              });
              emit('validation-change', true);
            },
          },
          'validate schedule'
        ),
      ]);
  },
});

const ConfluenceConnectionStepStub = defineComponent({
  name: 'ConnectionStep',
  emits: ['update:modelValue', 'validation-change', 'connection-success'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'confluence-connection-step' }, [
        h(
          'button',
          {
            class: 'confluence-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                connectionMethod: 'existing',
                baseUrl: 'https://confluence.example.com',
                credentialType: 'BASIC_AUTH',
                connectionName: 'Confluence Connection',
                username: 'user',
                password: 'pass',
                existingConnectionId: 'conn-confluence',
                createdConnectionId: '',
              });
              emit('connection-success', 'conn-confluence');
              emit('validation-change', true);
            },
          },
          'validate connection'
        ),
      ]);
  },
});

const ConfluenceConfigStepStub = defineComponent({
  name: 'ConfluenceConfigStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'confluence-config-step' }, [
        h(
          'button',
          {
            class: 'confluence-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                confluenceSpaceKey: 'SPACE',
                confluenceSpaceKeyFolderKey: 'ROOT',
                languageCodes: ['en'],
                reportNameTemplate: 'Daily Report',
                includeTableOfContents: true,
              });
              emit('validation-change', true);
            },
          },
          'validate config'
        ),
      ]);
  },
});

const ReviewSummaryStub = {
  name: 'ReviewSummary',
  props: ['formData'],
  template: '<div class="review-summary-stub">review</div>',
};

const JiraBasicDetailsStepStub = defineComponent({
  name: 'BasicDetailsStep',
  emits: ['validation-change', 'update:integrationName', 'update:description'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'jira-basic-step' }, [
        h(
          'button',
          {
            class: 'jira-step-valid',
            onClick: () => {
              emit('update:integrationName', 'Jira Webhook Integration');
              emit('update:description', 'Jira webhook desc');
              emit('validation-change', true);
            },
          },
          'validate basic'
        ),
      ]);
  },
});

const JiraSampleDataStepStub = defineComponent({
  name: 'SampleDataStep',
  emits: ['update:jsonSample', 'json-validity-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'jira-sample-step' }, [
        h(
          'button',
          {
            class: 'jira-step-valid',
            onClick: () => {
              emit(
                'update:jsonSample',
                '{"summary":"Webhook summary","description":"Webhook description"}'
              );
              emit('json-validity-change', true);
            },
          },
          'validate sample'
        ),
      ]);
  },
});

const JiraConnectionStepStub = defineComponent({
  name: 'ConnectionStep',
  emits: ['update:modelValue', 'validation-change'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'jira-connection-step' }, [
        h(
          'button',
          {
            class: 'jira-step-valid',
            onClick: () => {
              emit('update:modelValue', {
                connectionMethod: 'existing',
                credentialType: 'OAUTH2',
                connected: true,
                existingConnectionId: 'jira-conn-1',
                createdConnectionId: '',
                baseUrl: '',
                username: '',
                password: '',
                connectionName: 'Jira Connection',
              });
              emit('validation-change', true);
            },
          },
          'validate connection'
        ),
      ]);
  },
});

const JiraMappingStepStub = defineComponent({
  name: 'MappingStep',
  emits: ['validation-change', 'update:mappingData'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'jira-mapping-step' }, [
        h(
          'button',
          {
            class: 'jira-step-valid',
            onClick: () => {
              emit('update:mappingData', {
                selectedProject: 'P1',
                selectedIssueType: 'BUG',
                selectedAssignee: 'u1',
                summary: '{{summary}}',
                descriptionFieldMapping: '{{description}}',
                customFields: [],
              });
              emit('validation-change', true);
            },
          },
          'validate mapping'
        ),
      ]);
  },
});

const JiraPreviewStepStub = {
  name: 'PreviewStep',
  template: '<div class="jira-preview-step">preview</div>',
};

const JiraSuccessDialogStub = defineComponent({
  name: 'WebhookSuccessDialog',
  props: ['open', 'webhook', 'editMode'],
  emits: ['close', 'copy-url'],
  setup(_, { emit }) {
    return () =>
      h('div', { class: 'jira-success-dialog' }, [
        h(
          'button',
          {
            class: 'jira-success-close',
            onClick: () => emit('close'),
          },
          'close success'
        ),
      ]);
  },
});

function snapshotPayload<T>(payload: T): T {
  return JSON.parse(JSON.stringify(payload)) as T;
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div>root</div>' } }],
  });
}

async function mountArcGISWizard(props: Record<string, unknown> = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const router = createTestRouter();
  await router.push('/');
  await router.isReady();

  const wrapper = mount(ArcGISIntegrationWizard, {
    props: {
      open: true,
      mode: 'create',
      ...props,
    },
    global: {
      plugins: [pinia, router],
      stubs: {
        IntegrationDetailsStep: ArcGISIntegrationDetailsStepStub,
        ScheduleConfigurationStep: ArcGISScheduleStepStub,
        ConnectionStep: ArcGISConnectionStepStub,
        FieldMappingStep: ArcGISFieldMappingStepStub,
        ReviewSummary: ReviewSummaryStub,
      },
    },
  });

  await flushPromises();
  return { wrapper, toastStore: useToastStore() };
}

async function mountConfluenceWizard(props: Record<string, unknown> = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const router = createTestRouter();
  await router.push('/');
  await router.isReady();

  const wrapper = mount(ConfluenceIntegrationWizard, {
    props: {
      open: true,
      mode: 'create',
      ...props,
    },
    global: {
      plugins: [pinia, router],
      stubs: {
        IntegrationDetailsStep: ConfluenceIntegrationDetailsStepStub,
        ScheduleStep: ConfluenceScheduleStepStub,
        ConnectionStep: ConfluenceConnectionStepStub,
        ConfluenceConfigStep: ConfluenceConfigStepStub,
        ReviewSummary: ReviewSummaryStub,
      },
    },
  });

  await flushPromises();
  return { wrapper, toastStore: useToastStore() };
}

async function mountJiraWizard(props: Record<string, unknown> = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const router = createTestRouter();
  await router.push('/');
  await router.isReady();

  const wrapper = mount(JiraWebhookWizard, {
    props: {
      open: true,
      ...props,
    },
    global: {
      plugins: [pinia, router],
      stubs: {
        BasicDetailsStep: JiraBasicDetailsStepStub,
        SampleDataStep: JiraSampleDataStepStub,
        ConnectionStep: JiraConnectionStepStub,
        MappingStep: JiraMappingStepStub,
        PreviewStep: JiraPreviewStepStub,
        WebhookSuccessDialog: JiraSuccessDialogStub,
      },
    },
  });

  await flushPromises();
  return { wrapper, toastStore: useToastStore() };
}

async function advanceArcGISWizard(wrapper: ReturnType<typeof mount>) {
  const vm = wrapper.vm as any;

  for (let step = 0; step < 4; step += 1) {
    await wrapper.find('.arcgis-step-valid').trigger('click');
    await flushPromises();

    if (step === 0) {
      vm.formData.name = 'ArcGIS Integration';
      vm.formData.description = 'ArcGIS desc';
      vm.formData.subType = 'DOCUMENT_FINAL';
      vm.formData.subTypeLabel = 'Final';
    }

    if (step === 1) {
      vm.formData.executionDate = '2026-04-10';
      vm.formData.executionTime = '08:30';
      vm.formData.frequencyPattern = 'DAILY';
      vm.formData.dailyFrequency = '24';
    }

    if (step === 2) {
      vm.formData.arcgisEndpoint = 'https://arcgis.example.com';
      vm.formData.connectionMethod = 'existing';
      vm.formData.existingConnectionId = 'conn-arcgis';
      vm.formData.connectionName = 'ArcGIS Connection';
    }

    if (step === 3) {
      vm.formData.fieldMappings = [
        {
          id: 'map-1',
          sourceField: 'caseNumber',
          targetField: 'CASE_NO',
          transformationType: 'DIRECT',
          isMandatory: true,
          displayOrder: 0,
        },
      ];
    }

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();
  }

  vm.updateUnifiedIntegrationStepData({
    name: 'ArcGIS Integration',
    description: 'ArcGIS desc',
    itemType: 'DOCUMENT',
    subType: 'DOCUMENT_FINAL',
    subTypeLabel: 'Final',
    dynamicDocument: '',
    dynamicDocumentLabel: '',
  });
  vm.updateScheduleConfigurationData({
    executionDate: '2026-04-10',
    executionTime: '08:30',
    frequencyPattern: 'DAILY',
    dailyFrequency: '24',
    selectedDays: [],
    selectedMonths: [],
    monthlyPattern: '',
    monthlyWeek: '',
    monthlyWeekday: '',
    monthlyDay: null,
    isExecuteOnMonthEnd: false,
    cronExpression: '',
    businessTimeZone: 'UTC',
  });
  vm.handleConnectionDataUpdate({
    connectionMethod: 'existing',
    baseUrl: 'https://arcgis.example.com',
    credentialType: 'OAUTH2',
    username: '',
    password: '',
    clientId: '',
    clientSecret: '',
    tokenUrl: 'https://www.arcgis.com/sharing/rest/oauth2/token',
    scope: '',
    connectionName: 'ArcGIS Connection',
    connected: true,
    existingConnectionId: 'conn-arcgis',
    createdConnectionId: '',
  });
  vm.updateFieldMappingData({
    fieldMappings: [
      {
        id: 'map-1',
        sourceField: 'caseNumber',
        targetField: 'CASE_NO',
        transformationType: 'DIRECT',
        isMandatory: true,
        displayOrder: 0,
      },
    ],
  });

  await flushPromises();
}

async function advanceConfluenceWizard(wrapper: ReturnType<typeof mount>) {
  const vm = wrapper.vm as any;

  for (let step = 0; step < 4; step += 1) {
    await wrapper.find('.confluence-step-valid').trigger('click');
    await flushPromises();

    if (step === 0) {
      vm.formData.name = 'Confluence Integration';
      vm.formData.description = 'Confluence desc';
      vm.formData.subType = 'DOCUMENT_FINAL';
      vm.formData.subTypeLabel = 'Final';
    }

    if (step === 1) {
      vm.formData.executionTime = '09:00';
      vm.formData.frequencyPattern = 'DAILY';
      vm.formData.dailyFrequency = '24';
    }

    if (step === 2) {
      vm.formData.connectionMethod = 'existing';
      vm.formData.existingConnectionId = 'conn-confluence';
      vm.formData.connectionName = 'Confluence Connection';
      vm.formData.username = 'user';
      vm.formData.password = 'pass';
    }

    if (step === 3) {
      vm.formData.confluenceSpaceKey = 'SPACE';
      vm.formData.confluenceSpaceKeyFolderKey = 'ROOT';
      vm.formData.languageCodes = ['en'];
      vm.formData.reportNameTemplate = 'Daily Report';
      vm.formData.includeTableOfContents = true;
    }

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();
  }

  vm.updateUnifiedIntegrationStepData({
    name: 'Confluence Integration',
    description: 'Confluence desc',
    itemType: 'DOCUMENT',
    subType: 'DOCUMENT_FINAL',
    subTypeLabel: 'Final',
    dynamicDocument: '',
    dynamicDocumentLabel: '',
  });
  vm.updateScheduleData({
    executionDate: null,
    executionTime: '09:00',
    frequencyPattern: 'DAILY',
    dailyFrequency: '24',
    selectedDays: [],
    selectedMonths: [],
    isExecuteOnMonthEnd: false,
    cronExpression: undefined,
    businessTimeZone: 'UTC',
    timeCalculationMode: 'FIXED_DAY_BOUNDARY',
  });
  vm.handleConnectionDataUpdate({
    connectionMethod: 'existing',
    baseUrl: 'https://confluence.example.com',
    credentialType: 'BASIC_AUTH',
    username: 'user',
    password: 'pass',
    connectionName: 'Confluence Connection',
    connected: true,
    existingConnectionId: 'conn-confluence',
    createdConnectionId: '',
  });
  vm.updatePageConfigData({
    confluenceSpaceKey: 'SPACE',
    confluenceSpaceKeyFolderKey: 'ROOT',
    languageCodes: ['en'],
    reportNameTemplate: 'Daily Report',
    includeTableOfContents: true,
  });

  await flushPromises();
}

async function advanceJiraWizard(wrapper: ReturnType<typeof mount>) {
  const vm = wrapper.vm as any;

  for (let step = 0; step < 4; step += 1) {
    await wrapper.find('.jira-step-valid').trigger('click');
    await flushPromises();

    if (step === 0) {
      vm.integrationName = 'Jira Webhook Integration';
      vm.webhookDescription = 'Jira webhook desc';
      vm.isBasicDetailsValid = true;
    }

    if (step === 1) {
      vm.jsonSample = '{"summary":"Webhook summary","description":"Webhook description"}';
    }

    if (step === 2) {
      vm.connectionData.connectionMethod = 'existing';
      vm.connectionData.existingConnectionId = vm.connectionId || 'jira-conn-1';
      vm.connectionData.connectionName = 'Jira Connection';
      vm.connectionId = vm.connectionId || 'jira-conn-1';
      vm.jiraConnected = true;
    }

    if (step === 3) {
      vm.mappingData.selectedProject = 'P1';
      vm.mappingData.selectedIssueType = 'BUG';
      vm.mappingData.selectedAssignee = 'u1';
      vm.mappingData.summary = '{{summary}}';
      vm.mappingData.descriptionFieldMapping = '{{description}}';
      vm.projects = [{ key: 'P1', name: 'Project One' }];
      vm.issueTypes = [{ id: 'BUG', name: 'Bug' }];
      vm.users = [{ accountId: 'u1', displayName: 'Alice' }];
      vm.isMappingValid = true;
    }

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();
  }
}

describe('Wizard flow integration', () => {
  let submittedArcGISPayloads: Array<Record<string, unknown>>;
  let submittedConfluencePayloads: Array<Record<string, unknown>>;
  let submittedJiraWebhookPayloads: Array<Record<string, unknown>>;
  let updatedJiraWebhookPayloads: Array<{ id: string; request: Record<string, unknown> }>;

  beforeEach(() => {
    vi.clearAllMocks();
    submittedArcGISPayloads = [];
    submittedConfluencePayloads = [];
    submittedJiraWebhookPayloads = [];
    updatedJiraWebhookPayloads = [];
    wizardMocks.createArcGISIntegrationFromWizardMock.mockImplementation(async formData => {
      submittedArcGISPayloads.push(snapshotPayload(formData));
      return { id: 'arcgis-new' };
    });
    wizardMocks.updateArcGISIntegrationFromWizardMock.mockResolvedValue(true);
    wizardMocks.createConfluenceIntegrationFromWizardMock.mockImplementation(
      async (formData, baseUrl) => {
        submittedConfluencePayloads.push({
          ...snapshotPayload(formData),
          submittedBaseUrl: baseUrl,
        });
        return 'confluence-new';
      }
    );
    wizardMocks.updateConfluenceIntegrationFromWizardMock.mockResolvedValue(true);
    wizardMocks.createJiraWebhookMock.mockImplementation(async request => {
      submittedJiraWebhookPayloads.push(snapshotPayload(request));
      return {
        name: String(request.name ?? 'Jira Webhook Integration'),
        webhookUrl: 'https://example.test/webhooks/jira-webhook-integration',
      };
    });
    wizardMocks.updateJiraWebhookMock.mockImplementation(async (id, request) => {
      updatedJiraWebhookPayloads.push({
        id: String(id),
        request: snapshotPayload(request),
      });
      return true;
    });
    wizardMocks.getAllArcGISNormalizedNamesMock.mockResolvedValue([]);
    wizardMocks.getArcGISIntegrationByIdMock.mockResolvedValue({});
    wizardMocks.getArcGISFieldMappingsMock.mockResolvedValue([]);
    wizardMocks.getAllConfluenceNormalizedNamesMock.mockResolvedValue([]);
    wizardMocks.getConfluenceIntegrationByIdMock.mockResolvedValue({});
    wizardMocks.getAllJiraNormalizedNamesMock.mockResolvedValue([]);
    wizardMocks.getProjectsByConnectionIdMock.mockResolvedValue([
      { key: 'P1', name: 'Project One' },
    ]);
    wizardMocks.getProjectIssueTypesByConnectionIdMock.mockResolvedValue([
      { id: 'BUG', name: 'Bug' },
    ]);
    wizardMocks.getProjectUsersByConnectionIdMock.mockResolvedValue([
      { accountId: 'u1', displayName: 'Alice' },
    ]);
    wizardMocks.createFieldMappingsMock.mockImplementation(mappingData => [
      {
        jiraFieldId: 'project',
        jiraFieldName: 'Project',
        displayLabel: 'Project',
        dataType: 'OBJECT',
        template: mappingData.selectedProject,
        required: true,
      },
      {
        jiraFieldId: 'issuetype',
        jiraFieldName: 'Issue Type',
        displayLabel: 'Issue Type',
        dataType: 'OBJECT',
        template: mappingData.selectedIssueType,
        required: true,
      },
      {
        jiraFieldId: 'summary',
        jiraFieldName: 'Summary',
        displayLabel: 'Summary',
        dataType: 'STRING',
        template: mappingData.summary,
        required: true,
      },
    ]);
    wizardMocks.searchJiraSprintsMock.mockResolvedValue(undefined);
  });

  it('drives the ArcGIS create wizard through validated steps and submits with real state', async () => {
    const { wrapper } = await mountArcGISWizard();

    expect(wrapper.find('button.jw-btn-primary').attributes('disabled')).toBeDefined();

    await advanceArcGISWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.createArcGISIntegrationFromWizardMock).toHaveBeenCalledTimes(1);
    expect(submittedArcGISPayloads[0]).toEqual(
      expect.objectContaining({
        name: 'ArcGIS Integration',
        description: 'ArcGIS desc',
        arcgisEndpoint: 'https://arcgis.example.com',
        existingConnectionId: 'conn-arcgis',
      })
    );
    expect(wrapper.emitted('integration-created')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('surfaces a duplicate-name error from the ArcGIS final submit validation', async () => {
    wizardMocks.getAllArcGISNormalizedNamesMock.mockResolvedValue(['ArcGIS Integration']);

    const { wrapper, toastStore } = await mountArcGISWizard();

    await advanceArcGISWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.createArcGISIntegrationFromWizardMock).not.toHaveBeenCalled();
    expect(wrapper.emitted('integration-created')).toBeFalsy();
    expect(wrapper.emitted('close')).toBeFalsy();
    expect(toastStore.toasts.some(toast => toast.message.includes('already exists'))).toBe(true);
  });

  it('drives the Confluence create wizard through validated steps and submits with real state', async () => {
    const { wrapper } = await mountConfluenceWizard();

    expect(wrapper.find('button.jw-btn-primary').attributes('disabled')).toBeDefined();

    await advanceConfluenceWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.createConfluenceIntegrationFromWizardMock).toHaveBeenCalledTimes(1);
    expect(submittedConfluencePayloads[0]).toEqual(
      expect.objectContaining({
        name: 'Confluence Integration',
        description: 'Confluence desc',
        confluenceSpaceKey: 'SPACE',
        existingConnectionId: 'conn-confluence',
        submittedBaseUrl: 'https://confluence.example.com',
      })
    );
    expect(wrapper.emitted('integration-created')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('prefills and submits the Confluence wizard in edit mode through the update path', async () => {
    wizardMocks.getConfluenceIntegrationByIdMock.mockResolvedValue({
      name: 'Existing Confluence Integration',
      description: 'Existing Confluence desc',
      itemType: 'DOCUMENT',
      itemSubtype: 'DOCUMENT_FINAL',
      itemSubtypeLabel: 'Final',
      dynamicDocumentType: '',
      dynamicDocumentTypeLabel: '',
      connectionId: 'conn-confluence-existing',
      confluenceSpaceKey: 'SPACE',
      confluenceSpaceKeyFolderKey: 'ROOT',
      languageCodes: ['en'],
      reportNameTemplate: 'Daily Report',
      includeTableOfContents: true,
      schedule: {
        executionDate: null,
        executionTime: '09:00',
        frequencyPattern: 'DAILY',
        dailyExecutionInterval: 24,
        daySchedule: [],
        monthSchedule: [],
        isExecuteOnMonthEnd: false,
        cronExpression: undefined,
        businessTimeZone: 'UTC',
        timeCalculationMode: 'FIXED_DAY_BOUNDARY',
      },
    });

    const { wrapper } = await mountConfluenceWizard({
      mode: 'edit',
      integrationId: 'confluence-existing',
    });
    const vm = wrapper.vm as any;

    await wrapper.setProps({ open: false });
    await flushPromises();
    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(vm.formData.name).toBe('Existing Confluence Integration');
    expect(vm.formData.existingConnectionId).toBe('conn-confluence-existing');

    await advanceConfluenceWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.updateConfluenceIntegrationFromWizardMock).toHaveBeenCalledTimes(1);
    expect(wizardMocks.updateConfluenceIntegrationFromWizardMock).toHaveBeenCalledWith(
      'confluence-existing',
      expect.any(Object),
      'https://confluence.example.com'
    );
    expect(wrapper.emitted('integration-updated')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('prefills and submits the ArcGIS wizard in edit mode through the update path', async () => {
    wizardMocks.getArcGISIntegrationByIdMock.mockResolvedValue({
      name: 'Existing ArcGIS Integration',
      description: 'Existing ArcGIS desc',
      itemType: 'DOCUMENT',
      itemSubtype: 'DOCUMENT_FINAL',
      itemSubtypeLabel: 'Final',
      dynamicDocumentType: '',
      dynamicDocumentTypeLabel: '',
      connectionId: 'conn-arcgis-existing',
      schedule: {
        executionDate: '2026-04-10',
        executionTime: '08:30',
        frequencyPattern: 'DAILY',
        dailyExecutionInterval: 24,
        daySchedule: [],
        monthSchedule: [],
        isExecuteOnMonthEnd: false,
        cronExpression: undefined,
      },
    });
    wizardMocks.getArcGISFieldMappingsMock.mockResolvedValue([
      {
        id: 'existing-map-1',
        sourceField: 'caseNumber',
        targetField: 'CASE_NO',
        transformationType: 'DIRECT',
        isMandatory: true,
        displayOrder: 0,
      },
    ]);

    const { wrapper } = await mountArcGISWizard({
      mode: 'edit',
      integrationId: 'arcgis-existing',
    });
    const vm = wrapper.vm as any;

    await wrapper.setProps({ open: false });
    await flushPromises();
    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(vm.formData.name).toBe('Existing ArcGIS Integration');
    expect(vm.formData.existingConnectionId).toBe('conn-arcgis-existing');

    await advanceArcGISWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.updateArcGISIntegrationFromWizardMock).toHaveBeenCalledTimes(1);
    expect(wizardMocks.updateArcGISIntegrationFromWizardMock).toHaveBeenCalledWith(
      'arcgis-existing',
      expect.any(Object)
    );
    expect(wrapper.emitted('integration-updated')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('prefills the Confluence wizard in clone mode and creates a new integration from the cloned state', async () => {
    wizardMocks.getConfluenceIntegrationByIdMock.mockResolvedValue({
      name: 'Existing Confluence Integration',
      description: 'Existing Confluence desc',
      itemType: 'DOCUMENT',
      itemSubtype: 'DOCUMENT_FINAL',
      itemSubtypeLabel: 'Final',
      dynamicDocumentType: '',
      dynamicDocumentTypeLabel: '',
      connectionId: 'conn-confluence-existing',
      confluenceSpaceKey: 'SPACE',
      confluenceSpaceKeyFolderKey: 'ROOT',
      languageCodes: ['en'],
      reportNameTemplate: 'Daily Report',
      includeTableOfContents: true,
      schedule: {
        executionDate: null,
        executionTime: '09:00',
        frequencyPattern: 'DAILY',
        dailyExecutionInterval: 24,
        daySchedule: [],
        monthSchedule: [],
        isExecuteOnMonthEnd: false,
        cronExpression: undefined,
        businessTimeZone: 'UTC',
        timeCalculationMode: 'FIXED_DAY_BOUNDARY',
      },
    });

    const { wrapper } = await mountConfluenceWizard({
      mode: 'clone',
      integrationId: 'confluence-existing',
    });
    const vm = wrapper.vm as any;

    await wrapper.setProps({ open: false });
    await flushPromises();
    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(vm.formData.name).toBe('Copy of Existing Confluence Integration');
    expect(vm.formData.existingConnectionId).toBe('conn-confluence-existing');

    await advanceConfluenceWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.createConfluenceIntegrationFromWizardMock).toHaveBeenCalledTimes(1);
    expect(wizardMocks.updateConfluenceIntegrationFromWizardMock).not.toHaveBeenCalled();
    expect(submittedConfluencePayloads[0]).toEqual(
      expect.objectContaining({
        name: 'Confluence Integration',
        existingConnectionId: 'conn-confluence',
        submittedBaseUrl: 'https://confluence.example.com',
      })
    );
    expect(wrapper.emitted('integration-created')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('resets ArcGIS wizard state when cancellation is confirmed mid-flow', async () => {
    const { wrapper } = await mountArcGISWizard();
    const vm = wrapper.vm as any;

    await wrapper.find('.arcgis-step-valid').trigger('click');
    await flushPromises();
    vm.updateUnifiedIntegrationStepData({
      name: 'Unsaved ArcGIS Integration',
      description: 'Unsaved desc',
      itemType: 'DOCUMENT',
      subType: 'DOCUMENT_FINAL',
      subTypeLabel: 'Final',
      dynamicDocument: '',
      dynamicDocumentLabel: '',
    });
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(vm.currentStep).toBe(2);
    expect(vm.formData.name).toBe('Unsaved ArcGIS Integration');

    await wrapper.find('button.jw-btn-neutral').trigger('click');
    await flushPromises();
    await wrapper.find('.jw-cancel-actions .jw-btn-neutral').trigger('click');
    await flushPromises();

    expect(vm.currentStep).toBe(1);
    expect(vm.formData.name).toBe('');
    expect(wrapper.emitted('close')).toBeTruthy();
    expect(wizardMocks.createArcGISIntegrationFromWizardMock).not.toHaveBeenCalled();
    expect(wizardMocks.updateArcGISIntegrationFromWizardMock).not.toHaveBeenCalled();
  });

  it('resets Confluence wizard state when cancellation is confirmed mid-flow', async () => {
    const { wrapper } = await mountConfluenceWizard();
    const vm = wrapper.vm as any;

    await wrapper.find('.confluence-step-valid').trigger('click');
    await flushPromises();
    vm.updateUnifiedIntegrationStepData({
      name: 'Unsaved Confluence Integration',
      description: 'Unsaved desc',
      itemType: 'DOCUMENT',
      subType: 'DOCUMENT_FINAL',
      subTypeLabel: 'Final',
      dynamicDocument: '',
      dynamicDocumentLabel: '',
    });
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(vm.currentStep).toBe(2);
    expect(vm.formData.name).toBe('Unsaved Confluence Integration');

    await wrapper.find('button.jw-btn-neutral').trigger('click');
    await flushPromises();
    await wrapper.find('.jw-cancel-actions .jw-btn-neutral').trigger('click');
    await flushPromises();

    expect(vm.currentStep).toBe(1);
    expect(vm.formData.name).toBe('');
    expect(wrapper.emitted('close')).toBeTruthy();
    expect(wizardMocks.createConfluenceIntegrationFromWizardMock).not.toHaveBeenCalled();
    expect(wizardMocks.updateConfluenceIntegrationFromWizardMock).not.toHaveBeenCalled();
  });

  it('drives the Jira webhook create wizard through validated steps and emits after success close', async () => {
    const { wrapper } = await mountJiraWizard();

    expect(wrapper.find('button.jw-btn-primary').attributes('disabled')).toBeDefined();

    await advanceJiraWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.createJiraWebhookMock).toHaveBeenCalledTimes(1);
    expect(submittedJiraWebhookPayloads[0]).toEqual(
      expect.objectContaining({
        name: 'Jira Webhook Integration',
        description: 'Jira webhook desc',
        connectionId: 'jira-conn-1',
        samplePayload: '{"summary":"Webhook summary","description":"Webhook description"}',
      })
    );
    expect(wrapper.find('.jira-success-dialog').exists()).toBe(true);
    expect(wrapper.emitted('close')).toBeFalsy();
    expect(wrapper.emitted('webhook-created')).toBeFalsy();

    await wrapper.find('.jira-success-close').trigger('click');
    await flushPromises();

    expect(wrapper.emitted('close')).toBeTruthy();
    expect(wrapper.emitted('webhook-created')).toBeTruthy();
  });

  it('surfaces a duplicate-name error from the Jira webhook final submit validation', async () => {
    wizardMocks.getAllJiraNormalizedNamesMock.mockResolvedValue(['Jira Webhook Integration']);

    const { wrapper } = await mountJiraWizard();

    await advanceJiraWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.createJiraWebhookMock).not.toHaveBeenCalled();
    expect(wrapper.find('.jira-success-dialog').exists()).toBe(false);
    expect(wrapper.emitted('close')).toBeFalsy();
    expect(wrapper.emitted('webhook-created')).toBeFalsy();
    expect((wrapper.vm as any).isDuplicateName).toBe(true);
  });

  it('prefills the Jira webhook wizard in edit mode and submits through the update path', async () => {
    const editingWebhookData = {
      id: 'jira-webhook-1',
      name: 'Existing Jira Webhook',
      description: 'Existing webhook desc',
      webhookUrl: 'https://example.test/webhooks/existing',
      connectionId: 'jira-conn-existing',
      samplePayload: '{"summary":"Existing summary"}',
      jiraFieldMappings: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: 'Project',
          dataType: 'OBJECT',
          template: 'P1',
          required: true,
        },
        {
          jiraFieldId: 'issuetype',
          jiraFieldName: 'Issue Type',
          displayLabel: 'Issue Type',
          dataType: 'OBJECT',
          template: 'BUG',
          required: true,
        },
        {
          jiraFieldId: 'summary',
          jiraFieldName: 'Summary',
          displayLabel: 'Summary',
          dataType: 'STRING',
          template: '{{summary}}',
          required: true,
        },
      ],
    };

    const { wrapper } = await mountJiraWizard({
      open: false,
      editMode: true,
      editingWebhookData,
    });
    const vm = wrapper.vm as any;

    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(vm.integrationName).toBe('Existing Jira Webhook');
    expect(vm.connectionId).toBe('jira-conn-existing');
    expect(vm.mappingData.selectedProject).toBe('P1');

    await advanceJiraWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.updateJiraWebhookMock).toHaveBeenCalledTimes(1);
    expect(updatedJiraWebhookPayloads[0]).toEqual({
      id: 'jira-webhook-1',
      request: expect.objectContaining({
        name: 'Jira Webhook Integration',
        connectionId: 'jira-conn-existing',
      }),
    });
    expect(wrapper.find('.jira-success-dialog').exists()).toBe(false);
    expect(wrapper.emitted('close')).toBeTruthy();
    expect(wrapper.emitted('webhook-created')).toBeTruthy();
  });

  it('prefills the Jira webhook wizard in clone mode and creates a new webhook from cloned state', async () => {
    const cloningWebhookData = {
      id: 'jira-webhook-source',
      name: 'Existing Jira Webhook',
      description: 'Existing webhook desc',
      webhookUrl: 'https://example.test/webhooks/source',
      connectionId: 'jira-conn-existing',
      samplePayload: '{"summary":"Existing summary"}',
      fieldsMapping: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: 'Project',
          dataType: 'OBJECT',
          template: 'P1',
          required: true,
        },
        {
          jiraFieldId: 'issuetype',
          jiraFieldName: 'Issue Type',
          displayLabel: 'Issue Type',
          dataType: 'OBJECT',
          template: 'BUG',
          required: true,
        },
        {
          jiraFieldId: 'summary',
          jiraFieldName: 'Summary',
          displayLabel: 'Summary',
          dataType: 'STRING',
          template: '{{summary}}',
          required: true,
        },
      ],
    };

    const { wrapper } = await mountJiraWizard({
      open: false,
      cloneMode: true,
      cloningWebhookData,
    });
    const vm = wrapper.vm as any;

    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(vm.integrationName).toBe('Copy of Existing Jira Webhook');
    expect(vm.connectionId).toBe('jira-conn-existing');

    await advanceJiraWizard(wrapper);
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wizardMocks.createJiraWebhookMock).toHaveBeenCalledTimes(1);
    expect(wizardMocks.updateJiraWebhookMock).not.toHaveBeenCalled();
    expect(submittedJiraWebhookPayloads[0]).toEqual(
      expect.objectContaining({
        name: 'Jira Webhook Integration',
        connectionId: 'jira-conn-existing',
      })
    );
    expect(wrapper.find('.jira-success-dialog').exists()).toBe(true);
  });

  it('resets the Jira webhook wizard state when cancellation is confirmed mid-flow', async () => {
    const { wrapper } = await mountJiraWizard();
    const vm = wrapper.vm as any;

    await wrapper.find('.jira-step-valid').trigger('click');
    await flushPromises();
    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(vm.activeStep).toBe(1);
    expect(vm.integrationName).toBe('Jira Webhook Integration');

    await wrapper.find('button.jw-btn-neutral').trigger('click');
    await flushPromises();
    await wrapper.find('.jw-cancel-actions .jw-btn-neutral').trigger('click');
    await flushPromises();

    expect(vm.activeStep).toBe(0);
    expect(vm.integrationName).toBe('');
    expect(vm.webhookDescription).toBe('');
    expect(wrapper.emitted('close')).toBeTruthy();
    expect(wizardMocks.createJiraWebhookMock).not.toHaveBeenCalled();
    expect(wizardMocks.updateJiraWebhookMock).not.toHaveBeenCalled();
  });
});
