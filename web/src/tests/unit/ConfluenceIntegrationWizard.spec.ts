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

// Mock getUserTimezone to UTC so date/time conversions in buildPrefillSchedule are deterministic
vi.mock('@/utils/timezoneUtils', async () => {
  const actual = await vi.importActual('@/utils/timezoneUtils');
  return { ...actual, getUserTimezone: vi.fn(() => 'UTC') };
});

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

  it('shows the generic submit error message when the thrown value has no message', async () => {
    hoisted.createIntegrationFromWizardMock.mockRejectedValueOnce({});

    const wrapper = await createWrapper({ mode: 'create' });
    await goToReviewStep(wrapper);

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(hoisted.showErrorMock).toHaveBeenCalledWith('Operation failed');
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

  it('falls back to the create flow when edit mode is missing an integration id', async () => {
    const wrapper = await createWrapper({ mode: 'edit' });
    await goToReviewStep(wrapper);

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(hoisted.createIntegrationFromWizardMock).toHaveBeenCalledWith(
      hoisted.wizardState.formData,
      'https://confluence.example.com'
    );
    expect(hoisted.updateIntegrationFromWizardMock).not.toHaveBeenCalled();
    expect(wrapper.emitted('integration-created')).toBeTruthy();
  });

  it('converts UTC executionTime and executionDate to local timezone when prefilling for edit', async () => {
    // With getUserTimezone mocked to 'UTC', local time === UTC time; date is also unchanged.
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

    // UTC timezone: 20:30 UTC → 20:30 local; 2025-06-15 UTC → 2025-06-15 local
    expect(hoisted.wizardState.formData.executionTime).toBe('20:30');
    expect(hoisted.wizardState.formData.executionDate).toBe('2025-06-15');
  });

  it('converts UTC executionTime to local HH:mm when executionDate is null (end-of-month schedule)', async () => {
    // getUserTimezone is mocked to 'UTC', so UTC 14:30:00 → local 14:30 (no offset).
    // The key assertion is that executionTime is normalised to HH:mm and not left as the
    // raw HH:mm:ss UTC string, which would show the wrong time for non-UTC users.
    hoisted.getConfluenceIntegrationByIdMock.mockResolvedValueOnce({
      name: 'Month End Integration',
      description: '',
      itemType: 'DOCUMENT',
      itemSubtype: 'MONTHLY',
      itemSubtypeLabel: 'Monthly',
      dynamicDocumentType: '',
      dynamicDocumentTypeLabel: '',
      languageCodes: ['en'],
      reportNameTemplate: 'Report - {date}',
      includeTableOfContents: false,
      confluenceSpaceKey: 'SPACE1',
      confluenceSiteUrl: 'https://example.atlassian.net/wiki',
      confluenceSpaceKeyFolderKey: '',
      connectionId: 'conn-existing',
      schedule: {
        executionTime: '14:30:00',
        frequencyPattern: 'MONTHLY',
        executionDate: null,
        dailyExecutionInterval: null,
        daySchedule: [],
        monthSchedule: [],
        isExecuteOnMonthEnd: true,
        cronExpression: undefined,
        businessTimeZone: 'UTC',
        timeCalculationMode: 'FIXED_DAY_BOUNDARY',
      },
    });

    await createWrapper({ mode: 'edit', integrationId: 'month-end-test-id' });

    expect(hoisted.wizardState.formData.executionDate).toBeNull();
    expect(hoisted.wizardState.formData.executionTime).toBe('14:30');
  });

  it('does not emit created when create returns an empty id', async () => {
    hoisted.createIntegrationFromWizardMock.mockResolvedValueOnce('');

    const wrapper = await createWrapper({ mode: 'create' });
    await goToReviewStep(wrapper);

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wrapper.emitted('integration-created')).toBeFalsy();
    expect(wrapper.emitted('close')).toBeFalsy();
  });

  it('does not emit updated when edit returns false', async () => {
    hoisted.updateIntegrationFromWizardMock.mockResolvedValueOnce(false);

    const wrapper = await createWrapper({ mode: 'edit', integrationId: 'integration-123' });
    await goToReviewStep(wrapper);

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(wrapper.emitted('integration-updated')).toBeFalsy();
    expect(wrapper.emitted('close')).toBeFalsy();
  });

  it('marks connection step invalid when prefilling edit mode without a connection id', async () => {
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
      connectionId: '',
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

    await createWrapper({ mode: 'edit', integrationId: 'missing-connection' });

    expect(hoisted.wizardState.stepValidation[3]).toBe(false);
  });

  it('does not advance when the current step is invalid', async () => {
    hoisted.wizardState = createWizardState();
    hoisted.wizardState.stepValidation[1] = false;

    const wrapper = await createWrapper({ mode: 'create' });

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect((wrapper.vm as any).currentStep).toBe(1);
  });

  it('advances and goes back when step validity allows it', async () => {
    const wrapper = await createWrapper({ mode: 'create' });

    (wrapper.vm as any).nextStep();
    expect((wrapper.vm as any).currentStep).toBe(2);

    (wrapper.vm as any).previousStep();
    (wrapper.vm as any).previousStep();
    expect((wrapper.vm as any).currentStep).toBe(1);
  });

  it('does not close when overlay handler receives an inner-content click', async () => {
    const wrapper = await createWrapper({ open: true });

    (wrapper.vm as any).handleOverlayClick({ target: {}, currentTarget: {} });

    expect(wrapper.emitted('close')).toBeFalsy();
  });

  it('closes and resets when overlay itself is clicked', async () => {
    const wrapper = await createWrapper({ open: true });

    (wrapper.vm as any).handleOverlayClick({ target: 'x', currentTarget: 'x' });

    expect(hoisted.wizardState.resetFormData).toHaveBeenCalled();
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('opens the cancel dialog and keeps the wizard open when choosing no', async () => {
    const wrapper = await createWrapper({ mode: 'create' });

    (wrapper.vm as any).showCancelConfirmation();
    await flushPromises();
    expect((wrapper.vm as any).showCancelDialog).toBe(true);

    const noButton = wrapper.findAll('button').find(btn => btn.text().includes('No, Go Back'));
    expect(noButton).toBeDefined();
    await noButton!.trigger('click');

    expect((wrapper.vm as any).showCancelDialog).toBe(false);
    expect(wrapper.emitted('close')).toBeFalsy();
  });

  it('uses schedule defaults when prefilling an integration without a schedule', async () => {
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
      confluenceSpaceKeyFolderKey: '',
      connectionId: 'conn-existing',
      schedule: undefined,
    });

    await createWrapper({ mode: 'edit', integrationId: 'no-schedule-id' });

    expect(hoisted.wizardState.formData.executionTime).toBe('02:00');
    expect(hoisted.wizardState.formData.frequencyPattern).toBe('DAILY');
    expect(hoisted.wizardState.formData.dailyFrequency).toBe('24');
  });

  it('restores body overflow when the wizard closes', async () => {
    document.body.style.overflow = 'hidden';

    const wrapper = await createWrapper({ open: true });
    await wrapper.setProps({ open: false });
    await flushPromises();

    expect(document.body.style.overflow).toBe('');
  });

  it('sets body overflow while the wizard is open and exposes the no-op connection success handler', async () => {
    const wrapper = await createWrapper({ open: false });

    await wrapper.setProps({ open: true });
    await flushPromises();

    expect(document.body.style.overflow).toBe('hidden');
    expect(() => (wrapper.vm as any).handleConnectionSuccess('connection-1')).not.toThrow();
  });

  it('does not submit when the review step is invalid', async () => {
    const wrapper = await createWrapper({ mode: 'create' });
    await goToReviewStep(wrapper);

    hoisted.wizardState.stepValidation[5] = false;
    await flushPromises();

    await wrapper.find('button.jw-btn-primary').trigger('click');
    await flushPromises();

    expect(hoisted.createIntegrationFromWizardMock).not.toHaveBeenCalled();
    expect(wrapper.emitted('integration-created')).toBeFalsy();
  });

  it('does not advance past the final step', async () => {
    const wrapper = await createWrapper({ mode: 'create' });
    await goToReviewStep(wrapper);

    expect((wrapper.vm as any).currentStep).toBe(5);

    (wrapper.vm as any).nextStep();
    await flushPromises();

    expect((wrapper.vm as any).currentStep).toBe(5);
  });

  it('uses fallback prefill defaults for sparse clone details', async () => {
    hoisted.getConfluenceIntegrationByIdMock.mockResolvedValueOnce({
      name: 'Sparse Integration',
      description: '',
      itemType: '',
      itemSubtype: '',
      itemSubtypeLabel: '',
      dynamicDocumentType: '',
      dynamicDocumentTypeLabel: '',
      languageCodes: undefined,
      reportNameTemplate: '',
      includeTableOfContents: undefined,
      confluenceSpaceKey: '',
      confluenceSpaceKeyFolderKey: '',
      connectionId: '',
      schedule: {
        executionTime: '',
        frequencyPattern: '',
        executionDate: '',
        dailyExecutionInterval: undefined,
        daySchedule: undefined,
        monthSchedule: undefined,
        isExecuteOnMonthEnd: false,
        cronExpression: '',
        businessTimeZone: '',
        timeCalculationMode: '',
      },
    });

    await createWrapper({ mode: 'clone', integrationId: 'sparse-clone-id' });

    expect(hoisted.wizardState.formData.name).toBe('Copy of Sparse Integration');
    expect(hoisted.wizardState.formData.itemType).toBe('DOCUMENT');
    expect(hoisted.wizardState.formData.subType).toBe('');
    expect(hoisted.wizardState.formData.languageCodes).toEqual([]);
    expect(hoisted.wizardState.formData.includeTableOfContents).toBe(true);
    expect(hoisted.wizardState.formData.executionTime).toBe('02:00');
    expect(hoisted.wizardState.formData.frequencyPattern).toBe('DAILY');
    expect(hoisted.wizardState.formData.executionDate).toBe(null);
    expect(hoisted.wizardState.formData.dailyFrequency).toBe('24');
    expect(hoisted.wizardState.formData.selectedDays).toEqual([]);
    expect(hoisted.wizardState.formData.selectedMonths).toEqual([]);
    expect(hoisted.wizardState.formData.cronExpression).toBeUndefined();
    expect(hoisted.wizardState.formData.businessTimeZone).toBe('UTC');
    expect(hoisted.wizardState.formData.timeCalculationMode).toBe('FIXED_DAY_BOUNDARY');
    expect(hoisted.wizardState.formData.confluenceSpaceKeyFolderKey).toBe('ROOT');
    expect(hoisted.wizardState.formData.existingConnectionId).toBe('');
    expect(hoisted.wizardState.stepValidation[3]).toBe(true);
  });

  it('shows the cloning label while clone submission is in flight', async () => {
    let resolveCreate: ((value: string) => void) | undefined;
    hoisted.createIntegrationFromWizardMock.mockImplementationOnce(
      () =>
        new Promise(resolve => {
          resolveCreate = resolve;
        })
    );

    const wrapper = await createWrapper({ mode: 'clone', integrationId: 'clone-in-flight' });
    await goToReviewStep(wrapper);

    const submitButton = wrapper.find('button.jw-btn-primary');
    await submitButton.trigger('click');
    await flushPromises();

    expect(wrapper.find('button.jw-btn-primary').text()).toContain('Cloning...');
    expect(wrapper.find('button.jw-btn-primary').attributes('disabled')).toBeDefined();

    resolveCreate?.('cloned-id');
    await flushPromises();

    expect(wrapper.emitted('integration-created')).toBeTruthy();
  });
});
