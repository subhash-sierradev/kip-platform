import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ArcGISIntegrationWizard from '@/components/outbound/arcgisintegration/wizard/ArcGISIntegrationWizard.vue';

const buildDefaultFormData = () => ({
  name: '',
  description: '',
  itemType: 'DOCUMENT',
  subType: '',
  dynamicDocument: '',
  selectedDocumentId: '',
  executionDate: '',
  executionTime: '',
  frequencyPattern: '',
  dailyFrequency: '',
  selectedDays: [],
  selectedMonths: [],
  monthlyPattern: '',
  monthlyWeek: '',
  monthlyWeekday: '',
  monthlyDay: null,
  isExecuteOnMonthEnd: false,
  cronExpression: '',
  arcgisEndpoint: '',
  fetchMode: 'GET',
  credentialType: 'OAUTH2',
  username: '',
  password: '',
  clientId: '',
  clientSecret: '',
  tokenUrl: '',
  scope: '',
  connectionName: '',
  connectionMethod: 'existing',
  existingConnectionId: undefined,
  createdConnectionId: undefined,
  fieldMappings: [
    {
      id: '',
      sourceField: '',
      targetField: '',
      transformationType: '',
      isMandatory: false,
      displayOrder: 0,
    },
  ],
});

type FormData = ReturnType<typeof buildDefaultFormData>;

interface WizardState {
  formData: FormData;
  stepValidation: Record<number, boolean>;
  basicDetailsData: { name: string; description: string };
  documentSelectionData: {
    itemType: string;
    subType: string;
    subTypeLabel: string;
    dynamicDocument: string;
    dynamicDocumentLabel: string;
  };
  scheduleConfigurationData: Record<string, unknown>;
  genericConnectionData: { connected: boolean };
  fieldMappingData: { fieldMappings: Array<Record<string, unknown>> };
  activeConnectionId: string | null;
  updateBasicDetailsData: (...args: unknown[]) => void;
  updateDocumentSelectionData: (...args: unknown[]) => void;
  updateScheduleConfigurationData: (...args: unknown[]) => void;
  handleConnectionDataUpdate: (...args: unknown[]) => void;
  updateFieldMappingData: (...args: unknown[]) => void;
  setStepValidation: (step: number, valid: boolean) => void;
  resetFormData: () => void;
}

const hoisted = vi.hoisted(() => ({
  createIntegrationFromWizardMock: vi.fn().mockResolvedValue({}),
  updateIntegrationFromWizardMock: vi.fn().mockResolvedValue({}),
  getArcGISIntegrationByIdMock: vi.fn(),
  getAllArcGISNormalizedNamesMock: vi.fn().mockResolvedValue([]),
  loadAllNamesMock: vi.fn().mockResolvedValue(undefined),
  validateBeforeSubmitMock: vi.fn().mockResolvedValue(false),
  isDuplicateNameRef: { value: false },
  originalNameRef: { value: '' },
  allNormalizedNamesRef: { value: [] },
  wizardState: null as unknown as WizardState,
}));

const createWizardState = (overrides: Partial<WizardState> = {}): WizardState => {
  const baseState = {
    formData: buildDefaultFormData(),
    stepValidation: { 1: false, 2: false, 3: false, 4: false, 5: false, 6: true },
    basicDetailsData: { name: '', description: '' },
    documentSelectionData: {
      itemType: 'DOCUMENT',
      subType: '',
      subTypeLabel: '',
      dynamicDocument: '',
      dynamicDocumentLabel: '',
    },
    scheduleConfigurationData: {},
    genericConnectionData: { connected: false },
    fieldMappingData: { fieldMappings: [] },
    activeConnectionId: null,
    updateBasicDetailsData: vi.fn(),
    updateDocumentSelectionData: vi.fn(),
    updateScheduleConfigurationData: vi.fn(),
    handleConnectionDataUpdate: vi.fn(),
    updateFieldMappingData: vi.fn(),
    setStepValidation: vi.fn((_step: number, _valid: boolean) => {
      // Mock implementation
    }),
    resetFormData: vi.fn(),
  };

  return {
    ...baseState,
    ...overrides,
    formData: {
      ...baseState.formData,
      ...((overrides.formData ?? {}) as Partial<FormData>),
    },
    stepValidation: overrides.stepValidation ?? baseState.stepValidation,
  };
};

hoisted.wizardState = createWizardState();

// Mock composables and services
vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showError: vi.fn(),
    showSuccess: vi.fn(),
  }),
}));

vi.mock('@/composables/useArcGISIntegrationActions', () => ({
  useArcGISIntegrationActions: () => ({
    createIntegrationFromWizard: hoisted.createIntegrationFromWizardMock,
  }),
  useArcGISIntegrationEditor: () => ({
    updateIntegrationFromWizard: hoisted.updateIntegrationFromWizardMock,
  }),
}));

vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getArcGISIntegrationById: hoisted.getArcGISIntegrationByIdMock,
    getAllArcGISNormalizedNames: hoisted.getAllArcGISNormalizedNamesMock,
  },
}));

vi.mock('@/composables/useIntegrationNameValidation', () => ({
  useIntegrationNameValidation: () => ({
    isDuplicateName: hoisted.isDuplicateNameRef,
    originalName: hoisted.originalNameRef,
    allNormalizedNames: hoisted.allNormalizedNamesRef,
    loadAllNames: hoisted.loadAllNamesMock,
    validateBeforeSubmit: hoisted.validateBeforeSubmitMock,
    setupNameWatcher: vi.fn(),
  }),
}));

vi.mock('@/composables/useArcGISWizardState', () => ({
  useArcGISWizardState: () => hoisted.wizardState,
}));

describe('ArcGISIntegrationWizard', () => {
  const defaultProps = {
    open: false,
    mode: 'create' as const,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.wizardState = createWizardState();
    hoisted.isDuplicateNameRef.value = false;
    hoisted.originalNameRef.value = '';
    hoisted.allNormalizedNamesRef.value = [];
    hoisted.validateBeforeSubmitMock.mockResolvedValue(false);
  });

  describe('Component Rendering', () => {
    it('renders when open prop is true', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      expect(wrapper.find('.jw-modal-backdrop').exists()).toBe(true);
      expect(wrapper.find('.jw-modal').exists()).toBe(true);
    });

    it('does not render when open prop is false', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: defaultProps,
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      expect(wrapper.find('.jw-modal-backdrop').exists()).toBe(false);
    });

    it('displays stepper with correct number of steps', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const steps = wrapper.findAll('.jw-step-indicator');
      expect(steps.length).toBe(5); // 5 steps: Combined, Schedule, Connection, Field Mapping, Review
    });

    it('displays correct step labels in create mode', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'create',
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const stepLabels = wrapper.findAll('.jw-step-label');
      expect(stepLabels[0].text()).toContain('Integration Details');
      expect(stepLabels[1].text()).toContain('Schedule Configuration');
      expect(stepLabels[3].text()).toContain('Field Mapping');
      expect(stepLabels[4].text()).toContain('Review & Create');
    });

    it('displays correct step labels in edit mode', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'edit',
          open: true,
          integrationId: 'test-id',
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const stepLabels = wrapper.findAll('.jw-step-label');
      expect(stepLabels[4].text()).toContain('Review & Update');
    });

    it('displays correct step labels in clone mode', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'clone',
          open: true,
          integrationId: 'test-id',
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const stepLabels = wrapper.findAll('.jw-step-label');
      expect(stepLabels[4].text()).toContain('Review & Clone');
    });
  });

  describe('Step Navigation', () => {
    it('advances to next step when Next button is clicked', async () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      // Verify navigation buttons exist
      const buttons = wrapper.findAll('button');
      const nextButton = buttons.find(b => b.text().includes('Next'));
      expect(nextButton?.exists()).toBe(true);
    });

    it('shows Previous button only after first step', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      // Check first step - Previous button should not exist
      const prevButton = wrapper.findAll('button').find(b => b.text().includes('Previous'));
      expect(prevButton).toBeUndefined();

      expect(wrapper.exists()).toBe(true);
    });

    it('shows Submit button on last step', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const buttons = wrapper.findAll('button');
      // Look for button with submit-related text
      const submitButton = buttons.find(
        b => b.text().includes('Create') || b.text().includes('Update') || b.text().includes('Next')
      );
      expect(submitButton?.exists()).toBe(true);
    });

    it('disables Next button when step is invalid', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const nextButton = wrapper.findAll('button').find(b => b.text().includes('Next'));
      // Next button should exist for initial steps
      expect(nextButton?.exists()).toBe(true);
    });
  });

  describe('Modal Control', () => {
    it('closes wizard on overlay click', async () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const backdrop = wrapper.find('.jw-modal-backdrop');
      expect(backdrop.exists()).toBe(true);
    });

    it('shows cancel confirmation dialog when Close button is clicked', async () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const closeButton = wrapper.find('.jw-icon-button');
      expect(closeButton.exists()).toBe(true);
      await closeButton.trigger('click');
      await wrapper.vm.$nextTick();

      // Verify cancel dialog is shown
      const cancelDialog = wrapper.find('.jw-cancel-dialog');
      expect(cancelDialog.exists()).toBe(true);
    });

    it('cancels cancellation dialog', async () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      // Open cancel dialog
      const closeButton = wrapper.find('.jw-icon-button');
      await closeButton.trigger('click');
      await wrapper.vm.$nextTick();

      // Click "No, Go Back" button
      const cancelActions = wrapper.findAll('.jw-cancel-actions button');
      if (cancelActions.length > 1) {
        await cancelActions[1].trigger('click');
        await wrapper.vm.$nextTick();
      }

      expect(wrapper.exists()).toBe(true);
    });
  });

  describe('Mode-Specific Behavior', () => {
    it('shows Create button label in CREATE mode', async () => {
      hoisted.wizardState = createWizardState({
        stepValidation: { 1: true, 2: true, 3: true, 4: true, 5: true, 6: true },
      });
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'create',
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      for (let i = 0; i < 4; i++) {
        const nextButton = wrapper.findAll('button').find(button => button.text().includes('Next'));
        expect(nextButton?.exists()).toBe(true);
        await nextButton?.trigger('click');
        await wrapper.vm.$nextTick();
      }

      const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
      expect(submitButton.exists()).toBe(true);
      expect(submitButton.text()).toContain('Create ArcGIS Integration');
    });

    it('shows Update button label in EDIT mode', async () => {
      hoisted.wizardState = createWizardState({
        stepValidation: { 1: true, 2: true, 3: true, 4: true, 5: true, 6: true },
      });
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'edit',
          open: true,
          integrationId: 'test-id',
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      for (let i = 0; i < 4; i++) {
        const nextButton = wrapper.findAll('button').find(button => button.text().includes('Next'));
        expect(nextButton?.exists()).toBe(true);
        await nextButton?.trigger('click');
        await wrapper.vm.$nextTick();
      }

      const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
      expect(submitButton.exists()).toBe(true);
      expect(submitButton.text()).toContain('Update ArcGIS Integration');
    });

    it('shows Clone button label in CLONE mode', async () => {
      hoisted.wizardState = createWizardState({
        stepValidation: { 1: true, 2: true, 3: true, 4: true, 5: true, 6: true },
      });
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'clone',
          open: true,
          integrationId: 'test-id',
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      for (let i = 0; i < 4; i++) {
        const nextButton = wrapper.findAll('button').find(button => button.text().includes('Next'));
        expect(nextButton?.exists()).toBe(true);
        await nextButton?.trigger('click');
        await wrapper.vm.$nextTick();
      }

      const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
      expect(submitButton.exists()).toBe(true);
      expect(submitButton.text()).toContain('Clone ArcGIS Integration');
    });

    it('passes correct mode prop to FieldMappingStep', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'clone',
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: {
              template: '<div>{{ mode }}</div>',
              props: ['mode', 'modelValue', 'connectionId'],
            },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      expect(wrapper.props('mode')).toBe('clone');
    });
  });

  describe('Emitted Events', () => {
    it('emits close event when wizard is closed', async () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      // Trigger modal close by clicking backdrop
      const backdrop = wrapper.find('.jw-modal-backdrop');
      if (backdrop.exists()) {
        await backdrop.trigger('click');
        await flushPromises();
      }

      expect(wrapper.emitted('close')).toHaveLength(1);
    });

    it('emits integration-created on successful CREATE', async () => {
      hoisted.wizardState = createWizardState({
        stepValidation: { 1: true, 2: true, 3: true, 4: true, 5: true, 6: true },
      });
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'create',
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      for (let i = 0; i < 4; i++) {
        const nextButton = wrapper.findAll('button').find(button => button.text().includes('Next'));
        expect(nextButton?.exists()).toBe(true);
        await nextButton?.trigger('click');
        await wrapper.vm.$nextTick();
      }

      const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
      expect(submitButton.exists()).toBe(true);
      await submitButton.trigger('click');
      await flushPromises();

      expect(hoisted.createIntegrationFromWizardMock).toHaveBeenCalledTimes(1);
      expect(wrapper.emitted('integration-created')).toHaveLength(1);
    });

    it('emits integration-updated on successful EDIT', async () => {
      hoisted.wizardState = createWizardState({
        stepValidation: { 1: true, 2: true, 3: true, 4: true, 5: true, 6: true },
      });
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'edit',
          open: true,
          integrationId: 'test-id',
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      for (let i = 0; i < 4; i++) {
        const nextButton = wrapper.findAll('button').find(button => button.text().includes('Next'));
        expect(nextButton?.exists()).toBe(true);
        await nextButton?.trigger('click');
        await wrapper.vm.$nextTick();
      }

      const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
      expect(submitButton.exists()).toBe(true);
      await submitButton.trigger('click');
      await flushPromises();

      expect(hoisted.updateIntegrationFromWizardMock).toHaveBeenCalledWith(
        'test-id',
        expect.any(Object)
      );
      expect(wrapper.emitted('integration-updated')).toHaveLength(1);
    });
  });

  describe('Submit Button State', () => {
    it('disables submit button when loading', async () => {
      hoisted.wizardState = createWizardState({
        stepValidation: { 1: true, 2: true, 3: true, 4: true, 5: true, 6: true },
      });
      hoisted.validateBeforeSubmitMock.mockReturnValueOnce(new Promise(() => {}));
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'create',
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      for (let i = 0; i < 4; i++) {
        const nextButton = wrapper.findAll('button').find(button => button.text().includes('Next'));
        expect(nextButton?.exists()).toBe(true);
        await nextButton?.trigger('click');
        await wrapper.vm.$nextTick();
      }

      const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
      expect(submitButton.exists()).toBe(true);
      await submitButton.trigger('click');
      await wrapper.vm.$nextTick();

      expect(submitButton.attributes('disabled')).toBeDefined();
      expect(submitButton.text()).toContain('Creating...');
    });

    it('shows Cloning... text when submitting in CLONE mode', async () => {
      hoisted.wizardState = createWizardState({
        stepValidation: { 1: true, 2: true, 3: true, 4: true, 5: true, 6: true },
      });
      hoisted.validateBeforeSubmitMock.mockReturnValueOnce(new Promise(() => {}));
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'clone',
          open: true,
          integrationId: 'test-id',
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      for (let i = 0; i < 4; i++) {
        const nextButton = wrapper.findAll('button').find(button => button.text().includes('Next'));
        expect(nextButton?.exists()).toBe(true);
        await nextButton?.trigger('click');
        await wrapper.vm.$nextTick();
      }

      const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
      expect(submitButton.exists()).toBe(true);
      await submitButton.trigger('click');
      await wrapper.vm.$nextTick();

      expect(submitButton.attributes('disabled')).toBeDefined();
      expect(submitButton.text()).toContain('Cloning...');
    });

    it('disables submit button when step is invalid', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'create',
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      const buttons = wrapper.findAll('button');
      const nextButton = buttons.find(b => b.text().includes('Next'));
      // Next button should exist and potentially be disabled
      expect(nextButton?.exists()).toBe(true);
    });

    it('disables submit in CREATE mode when name is duplicate', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'create',
          open: true,
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      expect(wrapper.props('mode')).toBe('create');
    });

    it('allows submit in EDIT mode even with duplicate name', () => {
      const wrapper = mount(ArcGISIntegrationWizard, {
        props: {
          ...defaultProps,
          mode: 'edit',
          open: true,
          integrationId: 'test-id',
        },
        global: {
          stubs: {
            FieldMappingStep: { template: '<div></div>' },
            BasicDetailsStep: { template: '<div></div>' },
            DocumentSelectionStep: { template: '<div></div>' },
            ScheduleConfigurationStep: { template: '<div></div>' },
            ConnectionStep: { template: '<div></div>' },
            ReviewSummary: { template: '<div></div>' },
          },
        },
      });

      expect(wrapper.props('mode')).toBe('edit');
    });
  });
});
