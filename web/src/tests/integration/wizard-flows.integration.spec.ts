import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import ArcGISIntegrationWizard from '@/components/outbound/arcgisintegration/wizard/ArcGISIntegrationWizard.vue';
import ConfluenceIntegrationWizard from '@/components/outbound/confluenceintegration/wizard/ConfluenceIntegrationWizard.vue';
import { useToastStore } from '@/store/toast';

const wizardMocks = vi.hoisted(() => ({
  createArcGISIntegrationFromWizardMock: vi.fn(),
  updateArcGISIntegrationFromWizardMock: vi.fn(),
  createConfluenceIntegrationFromWizardMock: vi.fn(),
  updateConfluenceIntegrationFromWizardMock: vi.fn(),
  getAllArcGISNormalizedNamesMock: vi.fn(),
  getArcGISIntegrationByIdMock: vi.fn(),
  getArcGISFieldMappingsMock: vi.fn(),
  getAllConfluenceNormalizedNamesMock: vi.fn(),
  getConfluenceIntegrationByIdMock: vi.fn(),
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

describe('Wizard flow integration', () => {
  let submittedArcGISPayloads: Array<Record<string, unknown>>;
  let submittedConfluencePayloads: Array<Record<string, unknown>>;

  beforeEach(() => {
    vi.clearAllMocks();
    submittedArcGISPayloads = [];
    submittedConfluencePayloads = [];
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
    wizardMocks.getAllArcGISNormalizedNamesMock.mockResolvedValue([]);
    wizardMocks.getArcGISIntegrationByIdMock.mockResolvedValue({});
    wizardMocks.getArcGISFieldMappingsMock.mockResolvedValue([]);
    wizardMocks.getAllConfluenceNormalizedNamesMock.mockResolvedValue([]);
    wizardMocks.getConfluenceIntegrationByIdMock.mockResolvedValue({});
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
});
