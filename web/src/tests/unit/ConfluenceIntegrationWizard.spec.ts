import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { computed, reactive, type Ref } from 'vue';

import ConfluenceIntegrationWizard from '@/components/outbound/confluenceintegration/wizard/ConfluenceIntegrationWizard.vue';
import type { ConfluenceFormData } from '@/types/ConfluenceFormData';

interface WizardStateMock {
  formData: ConfluenceFormData;
  stepValidation: Record<number, boolean>;
  unifiedIntegrationStepData: Ref<Record<string, unknown>>;
  scheduleData: Ref<Record<string, unknown>>;
  pageConfigData: Ref<Record<string, unknown>>;
  genericConnectionData: Ref<Record<string, unknown>>;
  updateUnifiedIntegrationStepData: ReturnType<typeof vi.fn>;
  updateScheduleData: ReturnType<typeof vi.fn>;
  updatePageConfigData: ReturnType<typeof vi.fn>;
  handleConnectionDataUpdate: ReturnType<typeof vi.fn>;
  commitConnectionSiteUrl: ReturnType<typeof vi.fn>;
  setStepValidation: (step: number, valid: boolean) => void;
  resetFormData: ReturnType<typeof vi.fn>;
}

const hoisted = vi.hoisted(() => ({
  showErrorMock: vi.fn(),
  showSuccessMock: vi.fn(),
  createIntegrationFromWizardMock: vi.fn(),
  updateIntegrationFromWizardMock: vi.fn(),
  loadNormalizedNamesMock: vi.fn(),
  getConfluenceIntegrationByIdMock: vi.fn(),
  convertUtcTimeToUserTimezoneMock: vi.fn((time: string) => time?.substring(0, 5) || '02:00'),
  isDuplicateNameRef: { value: false },
  allNormalizedNamesRef: { value: [] as string[] },
  wizardState: null as unknown as WizardStateMock,
}));

function createFormData(overrides: Partial<ConfluenceFormData> = {}): ConfluenceFormData {
  return {
    name: 'Confluence Integration',
    description: 'Description',
    itemType: 'DOCUMENT',
    subType: 'DAILY',
    subTypeLabel: 'Daily',
    dynamicDocument: '',
    dynamicDocumentLabel: '',
    languageCodes: ['en'],
    reportNameTemplate: 'Aggregated Daily Report - {date}',
    includeTableOfContents: true,
    executionDate: null,
    executionTime: '02:00',
    frequencyPattern: 'DAILY',
    dailyFrequency: '24',
    selectedDays: [],
    selectedMonths: [],
    isExecuteOnMonthEnd: false,
    cronExpression: undefined,
    confluenceSpaceKey: 'SPACE1',
    confluenceSpaceKeyFolderKey: '',
    connectionMethod: 'existing',
    existingConnectionId: 'conn-1',
    createdConnectionId: '',
    connectionName: '',
    username: '',
    password: '',
    ...overrides,
  };
}

function createWizardState(overrides: Partial<ConfluenceFormData> = {}): WizardStateMock {
  const formData = reactive(createFormData(overrides));
  const stepValidation = reactive<Record<number, boolean>>({
    1: true,
    2: true,
    3: true,
    4: true,
    5: true,
  });

  return {
    formData,
    stepValidation,
    unifiedIntegrationStepData: computed(() => ({
      name: formData.name,
      description: formData.description,
      itemType: formData.itemType,
      subType: formData.subType,
      subTypeLabel: formData.subTypeLabel,
      dynamicDocument: formData.dynamicDocument,
      dynamicDocumentLabel: formData.dynamicDocumentLabel,
    })),
    scheduleData: computed(() => ({
      executionDate: formData.executionDate,
      executionTime: formData.executionTime,
      frequencyPattern: formData.frequencyPattern,
      dailyFrequency: formData.dailyFrequency,
      selectedDays: formData.selectedDays,
      selectedMonths: formData.selectedMonths,
      isExecuteOnMonthEnd: formData.isExecuteOnMonthEnd,
      cronExpression: formData.cronExpression,
    })),
    pageConfigData: computed(() => ({
      confluenceSpaceKey: formData.confluenceSpaceKey,
      confluenceSpaceKeyFolderKey: formData.confluenceSpaceKeyFolderKey,
      languageCodes: formData.languageCodes,
      reportNameTemplate: formData.reportNameTemplate,
      includeTableOfContents: formData.includeTableOfContents,
    })),
    genericConnectionData: computed(() => ({
      connectionMethod: formData.connectionMethod,
      existingConnectionId: formData.existingConnectionId,
      createdConnectionId: formData.createdConnectionId,
      connected: true,
      baseUrl: 'https://confluence.example.com',
      credentialType: 'BASIC_AUTH',
      connectionName: formData.connectionName,
      username: formData.username,
      password: formData.password,
    })),
    updateUnifiedIntegrationStepData: vi.fn(),
    updateScheduleData: vi.fn(),
    updatePageConfigData: vi.fn(),
    handleConnectionDataUpdate: vi.fn(),
    commitConnectionSiteUrl: vi.fn(),
    setStepValidation: (step: number, valid: boolean) => {
      stepValidation[step] = valid;
    },
    resetFormData: vi.fn(),
  };
}

hoisted.wizardState = createWizardState();

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showError: hoisted.showErrorMock,
    showSuccess: hoisted.showSuccessMock,
  }),
}));

vi.mock('@/composables/useConfluenceIntegrationActions', () => ({
  useConfluenceIntegrationActions: () => ({
    createIntegrationFromWizard: hoisted.createIntegrationFromWizardMock,
  }),
  useConfluenceIntegrationEditor: () => ({
    updateIntegrationFromWizard: hoisted.updateIntegrationFromWizardMock,
  }),
}));

vi.mock('@/composables/useConfluenceWizardState', () => ({
  useConfluenceWizardState: () => hoisted.wizardState,
}));

vi.mock('@/composables/useConfluenceNameValidation', () => ({
  useConfluenceNameValidation: () => ({
    allNormalizedNames: hoisted.allNormalizedNamesRef,
    isDuplicateName: hoisted.isDuplicateNameRef,
    loadNormalizedNames: hoisted.loadNormalizedNamesMock,
  }),
}));

vi.mock('@/api/services/ConfluenceIntegrationService', () => ({
  ConfluenceIntegrationService: {
    getConfluenceIntegrationById: hoisted.getConfluenceIntegrationByIdMock,
  },
}));

vi.mock('@/utils/scheduleDisplayUtils', () => ({
  convertUtcTimeToUserTimezone: hoisted.convertUtcTimeToUserTimezoneMock,
}));

const IntegrationDetailsStepStub = {
  name: 'IntegrationDetailsStep',
  props: ['modelValue', 'config', 'editMode', 'originalName', 'normalizedNames'],
  emits: ['update:modelValue', 'validation-change'],
  template: '<div class="integration-details-step-stub" />',
};

const ScheduleStepStub = {
  name: 'ScheduleStep',
  props: ['modelValue'],
  emits: ['update:modelValue', 'validation-change'],
  template: '<div class="schedule-step-stub" />',
};

const ConnectionStepStub = {
  name: 'ConnectionStep',
  props: ['modelValue', 'config'],
  emits: ['update:modelValue', 'validation-change', 'connection-success'],
  template: '<div class="connection-step-stub" />',
};

const ConfluenceConfigStepStub = {
  name: 'ConfluenceConfigStep',
  props: ['modelValue', 'connectionId'],
  emits: ['update:modelValue', 'validation-change'],
  template: '<div class="confluence-config-step-stub" />',
};

const ReviewSummaryStub = {
  name: 'ReviewSummary',
  props: ['formData', 'isDuplicateName'],
  emits: ['validation-change'],
  template: '<div class="review-summary-stub" />',
};

async function createWrapper(
  props: { open?: boolean; mode?: 'create' | 'edit' | 'clone'; integrationId?: string } = {}
) {
  const shouldOpen = props.open ?? true;
  const wrapper = mount(ConfluenceIntegrationWizard, {
    props: {
      open: false,
      mode: 'create',
      ...props,
    },
    global: {
      stubs: {
        IntegrationDetailsStep: IntegrationDetailsStepStub,
        ScheduleStep: ScheduleStepStub,
        ConnectionStep: ConnectionStepStub,
        ConfluenceConfigStep: ConfluenceConfigStepStub,
        ReviewSummary: ReviewSummaryStub,
      },
    },
  });

  if (shouldOpen) {
    await wrapper.setProps({ open: true });
  }

  await flushPromises();
  return wrapper;
}

async function goToReviewStep(wrapper: ReturnType<typeof mount>) {
  for (let i = 0; i < 4; i += 1) {
    const nextButton = wrapper.find('button.jw-btn-primary');
    expect(nextButton.exists()).toBe(true);
    await nextButton.trigger('click');
    await flushPromises();
  }
}

describe('ConfluenceIntegrationWizard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.wizardState = createWizardState();
    hoisted.isDuplicateNameRef.value = false;
    hoisted.allNormalizedNamesRef.value = [];
    hoisted.createIntegrationFromWizardMock.mockResolvedValue('new-id');
    hoisted.updateIntegrationFromWizardMock.mockResolvedValue(true);
    hoisted.loadNormalizedNamesMock.mockResolvedValue(undefined);
    hoisted.getConfluenceIntegrationByIdMock.mockResolvedValue({
      name: 'Existing Name',
      description: 'Existing Description',
      itemType: 'DOCUMENT',
      itemSubtype: 'DAILY',
      itemSubtypeLabel: 'Daily',
      dynamicDocumentType: '',
      dynamicDocumentTypeLabel: '',
      languageCodes: ['en'],
      reportNameTemplate: 'Report - {date}',
      includeTableOfContents: true,
      confluenceSpaceKey: 'SPACE1',
      confluenceSiteUrl: 'https://example.atlassian.net/wiki',
      confluenceSpaceKeyFolderKey: '',
      connectionId: 'conn-existing',
      schedule: {
        executionTime: '02:00',
        frequencyPattern: 'DAILY',
        executionDate: null,
        dailyExecutionInterval: 24,
        daySchedule: [],
        monthSchedule: [],
        isExecuteOnMonthEnd: false,
        cronExpression: undefined,
      },
    });
  });

  it('renders modal when open and closes from overlay click', async () => {
    const wrapper = await createWrapper({ open: true });

    expect(wrapper.find('.jw-modal').exists()).toBe(true);

    await wrapper.find('.jw-modal-backdrop').trigger('click');

    expect(hoisted.wizardState.resetFormData).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('submits create flow and emits integration-created', async () => {
    const wrapper = await createWrapper({ mode: 'create' });
    await goToReviewStep(wrapper);

    const submitButton = wrapper.find('button.jw-btn-primary');
    expect(submitButton.text()).toContain('Create Confluence Integration');

    await submitButton.trigger('click');
    await flushPromises();

    expect(hoisted.createIntegrationFromWizardMock).toHaveBeenCalledWith(
      hoisted.wizardState.formData,
      'https://confluence.example.com'
    );
    expect(wrapper.emitted('integration-created')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('submits edit flow and emits integration-updated', async () => {
    const wrapper = await createWrapper({ mode: 'edit', integrationId: 'integration-123' });
    await goToReviewStep(wrapper);

    const submitButton = wrapper.find('button.jw-btn-primary');
    expect(submitButton.text()).toContain('Update Confluence Integration');

    await submitButton.trigger('click');
    await flushPromises();

    expect(hoisted.updateIntegrationFromWizardMock).toHaveBeenCalledWith(
      'integration-123',
      hoisted.wizardState.formData,
      'https://confluence.example.com'
    );
    expect(wrapper.emitted('integration-updated')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('shows error toast on submit exception', async () => {
    hoisted.createIntegrationFromWizardMock.mockRejectedValueOnce(new Error('submit failed'));

    const wrapper = await createWrapper({ mode: 'create' });
    await goToReviewStep(wrapper);

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(hoisted.showErrorMock).toHaveBeenCalledWith('submit failed');
  });

  it('disables submit for duplicate names in create mode', async () => {
    hoisted.isDuplicateNameRef.value = true;

    const wrapper = await createWrapper({ mode: 'create' });
    await goToReviewStep(wrapper);

    const submitButton = wrapper.find('button.jw-btn-primary');
    expect(submitButton.attributes('disabled')).toBeDefined();
  });

  it('allows submit in edit mode even when duplicate name flag is true', async () => {
    hoisted.isDuplicateNameRef.value = true;

    const wrapper = await createWrapper({ mode: 'edit', integrationId: 'integration-123' });
    await goToReviewStep(wrapper);

    const submitButton = wrapper.find('button.jw-btn-primary');
    expect(submitButton.attributes('disabled')).toBeUndefined();
  });

  it('loads prefill data in clone mode and calls normalized names loader', async () => {
    await createWrapper({ mode: 'clone', integrationId: 'integration-clone-id' });

    expect(hoisted.getConfluenceIntegrationByIdMock).toHaveBeenCalledWith('integration-clone-id');
    expect(hoisted.loadNormalizedNamesMock).toHaveBeenCalledTimes(1);
    expect(hoisted.wizardState.formData.name).toContain('Copy of');
    expect(hoisted.wizardState.stepValidation[3]).toBe(true);
  });

  it('shows prefill load error when integration details request fails', async () => {
    hoisted.getConfluenceIntegrationByIdMock.mockRejectedValueOnce(new Error('prefill failed'));

    await createWrapper({ mode: 'edit', integrationId: 'bad-id' });

    expect(hoisted.showErrorMock).toHaveBeenCalledWith('prefill failed');
  });

  it('opens cancel confirmation and closes on confirm', async () => {
    const wrapper = await createWrapper({ mode: 'create' });

    const buttons = wrapper.findAll('button');
    const cancelFooterButton = buttons.find(btn => btn.text() === 'Cancel');
    expect(cancelFooterButton).toBeDefined();

    await cancelFooterButton!.trigger('click');
    await flushPromises();
    expect(wrapper.text()).toContain('Are you sure you want to cancel?');

    const confirmCancelButton = wrapper
      .findAll('button')
      .find(btn => btn.text().includes('Yes, Cancel'));
    expect(confirmCancelButton).toBeDefined();

    await confirmCancelButton!.trigger('click');
    expect(hoisted.wizardState.resetFormData).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('converts UTC executionTime to user timezone when prefilling for edit', async () => {
    hoisted.getConfluenceIntegrationByIdMock.mockResolvedValueOnce({
      name: 'Existing Name',
      description: 'Existing Description',
      itemType: 'DOCUMENT',
      itemSubtype: 'DAILY',
      itemSubtypeLabel: 'Daily',
      dynamicDocumentType: '',
      dynamicDocumentTypeLabel: '',
      languageCodes: ['en'],
      reportNameTemplate: 'Report - {date}',
      includeTableOfContents: true,
      confluenceSpaceKey: 'SPACE1',
      confluenceSiteUrl: 'https://example.atlassian.net/wiki',
      confluenceSpaceKeyFolderKey: '',
      connectionId: 'conn-existing',
      schedule: {
        executionTime: '20:30:00',
        frequencyPattern: 'DAILY',
        executionDate: '2025-06-15',
        dailyExecutionInterval: 24,
        daySchedule: [],
        monthSchedule: [],
        isExecuteOnMonthEnd: false,
        cronExpression: undefined,
        businessTimeZone: 'America/New_York',
        timeCalculationMode: 'FIXED_DAY_BOUNDARY',
      },
    });

    await createWrapper({ mode: 'edit', integrationId: 'tz-test-id' });

    expect(hoisted.convertUtcTimeToUserTimezoneMock).toHaveBeenCalledWith('20:30:00', '2025-06-15');
  });
});
