/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { nextTick } from 'vue';
import { ARCGIS_UNIFIED_STEP_CONFIG } from '@/utils/unifiedIntegrationStepConfig';
import IntegrationDetailsStep from '@/components/common/combinedstep/IntegrationDetailsStep.vue';
import { KwDocService } from '@/api/services/KwIntegrationService';

const INTEGRATION_NAME_MAX_LENGTH = 100;

vi.mock('@/api/services/KwIntegrationService', () => ({
  KwDocService: {
    getSubItemTypes: vi.fn().mockResolvedValue([]),
    getDynamicDocuments: vi.fn(),
  },
}));

const flushPromises = async () => {
  await Promise.resolve();
  await Promise.resolve();
  await nextTick();
};

describe('ArcGIS BasicDetailsStep', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([]);
    vi.mocked(KwDocService.getDynamicDocuments).mockResolvedValue([]);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('emits updates and validation-change as user types', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: 'DOCUMENT_PDF' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: ['existing_integration'],
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('My ArcGIS');

    // Flush debounce validation
    vi.advanceTimersByTime(350);
    await nextTick();

    const updates = wrapper.emitted()['update:modelValue'];
    expect(updates?.length).toBeGreaterThan(0);
    expect(updates?.at(-1)).toEqual([
      {
        name: 'My ArcGIS',
        description: '',
        itemType: 'DOCUMENT',
        subType: 'DOCUMENT_PDF',
        subTypeLabel: '',
      },
    ]);

    const validations = wrapper.emitted('validation-change') as
      | Array<[boolean, ...any[]]>
      | undefined;
    expect(validations?.some(args => args[0] === true)).toBe(true);
  });

  it('detects duplicate name and emits false validation', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: ['duplicate_name', 'another_integration'],
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('Duplicate Name');

    vi.advanceTimersByTime(350);
    await nextTick();

    const validations = wrapper.emitted('validation-change') as
      | Array<[boolean, ...any[]]>
      | undefined;
    expect(validations?.some(args => args[0] === false)).toBe(true);
  });

  it('allows unique name and emits true validation when subtype is also selected', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: 'DOCUMENT_PDF' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: ['existing_integration'],
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('Unique Integration');

    vi.advanceTimersByTime(350);
    await nextTick();

    const validations = wrapper.emitted('validation-change') as
      | Array<[boolean, ...any[]]>
      | undefined;
    expect(validations?.some(args => args[0] === true)).toBe(true);
  });

  it('allows original name in edit mode when subtype is selected', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'Original Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_PDF',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        editMode: true,
        originalName: 'Original Name',
        normalizedNames: ['original_name', 'another'],
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('Original Name');

    vi.advanceTimersByTime(350);
    await nextTick();

    const validations = wrapper.emitted('validation-change') as
      | Array<[boolean, ...any[]]>
      | undefined;
    expect(validations?.some(args => args[0] === true)).toBe(true);
  });

  it('handles missing normalizedNames prop gracefully', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: 'DOCUMENT_PDF' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('Test Name');

    vi.advanceTimersByTime(350);
    await nextTick();

    const validations = wrapper.emitted('validation-change') as
      | Array<[boolean, ...any[]]>
      | undefined;
    expect(validations?.some(args => args[0] === true)).toBe(true);
  });

  it('sets the integration name maxlength to the configured max', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await nextTick();

    expect(wrapper.find('input.uis-input').attributes('maxlength')).toBe('100');
  });

  it('truncates integration names longer than the configured max before emitting', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: 'DOCUMENT_PDF' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    const input = wrapper.find('input.uis-input');
    const longName = 'a'.repeat(125);

    await input.setValue(longName);
    vi.advanceTimersByTime(350);
    await nextTick();

    const updateEvents = wrapper.emitted('update:modelValue');
    expect(updateEvents).toBeTruthy();
    const lastModel = updateEvents?.at(-1)?.[0] as { name?: string } | undefined;
    expect(String(lastModel?.name).length).toBe(100);
    expect((input.element as HTMLInputElement).value.length).toBe(100);
  });

  it('shows the required-name validation after the user clears the field', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'Existing Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_PDF',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('');
    await nextTick();

    expect(wrapper.text()).toContain('Integration name is required');

    const validations = wrapper.emitted('validation-change') as
      | Array<[boolean, ...any[]]>
      | undefined;
    expect(validations?.at(-1)?.[0]).toBe(false);
  });

  it('revalidates the current name when normalized names change', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Unique',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_PDF',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: [],
      },
    });
    await nextTick();

    vi.advanceTimersByTime(350);
    await nextTick();

    await wrapper.setProps({ normalizedNames: ['arcgis_unique'] });
    vi.advanceTimersByTime(350);
    await nextTick();

    expect(wrapper.text()).toContain(ARCGIS_UNIFIED_STEP_CONFIG.duplicateNameMessage);

    const validations = wrapper.emitted('validation-change') as
      | Array<[boolean, ...any[]]>
      | undefined;
    expect(validations?.at(-1)?.[0]).toBe(false);
  });

  // ── Bug 1: Next button should stay disabled until Item Subtype is selected ──

  it('emits false when name is valid but Item Subtype is not selected', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: [],
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('My Integration');

    vi.advanceTimersByTime(350);
    await nextTick();

    const validations = wrapper.emitted('validation-change') as Array<[boolean]> | undefined;
    expect(validations?.at(-1)?.[0]).toBe(false);
  });

  it('emits true only after both name and Item Subtype are provided', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: [],
      },
    });
    await nextTick();

    // Type a name — Next should still be disabled because subType is empty
    const input = wrapper.find('input.uis-input');
    await input.setValue('My Integration');
    vi.advanceTimersByTime(350);
    await nextTick();

    const validationsAfterName = wrapper.emitted('validation-change') as
      | Array<[boolean]>
      | undefined;
    expect(validationsAfterName?.at(-1)?.[0]).toBe(false);

    // Simulate parent updating the prop with the selected subtype
    await wrapper.setProps({
      modelValue: {
        name: 'My Integration',
        description: '',
        itemType: 'DOCUMENT',
        subType: 'DOCUMENT_PDF',
      },
    });
    await nextTick();

    const validationsAfterSubtype = wrapper.emitted('validation-change') as
      | Array<[boolean]>
      | undefined;
    expect(validationsAfterSubtype?.at(-1)?.[0]).toBe(true);
  });

  // ── Bug 2: Integration Name max-length validation ──

  it('truncates typed names longer than 100 characters and keeps the step valid', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: 'DOCUMENT_PDF' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: [],
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    const longName = 'A'.repeat(INTEGRATION_NAME_MAX_LENGTH + 1);
    (input.element as HTMLInputElement).value = longName;
    await input.trigger('input');

    vi.advanceTimersByTime(350);
    await nextTick();

    expect((input.element as HTMLInputElement).value).toHaveLength(INTEGRATION_NAME_MAX_LENGTH);
    expect(wrapper.text()).not.toContain(
      `Maximum ${INTEGRATION_NAME_MAX_LENGTH} characters allowed`
    );

    const validations = wrapper.emitted('validation-change') as Array<[boolean]> | undefined;
    expect(validations?.at(-1)?.[0]).toBe(true);
  });

  it('allows a name of exactly 100 characters and emits true when subtype is selected', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: 'DOCUMENT_PDF' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: [],
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    await input.setValue('A'.repeat(INTEGRATION_NAME_MAX_LENGTH));

    vi.advanceTimersByTime(350);
    await nextTick();

    expect(wrapper.text()).not.toContain(
      `Maximum ${INTEGRATION_NAME_MAX_LENGTH} characters allowed`
    );

    const validations = wrapper.emitted('validation-change') as Array<[boolean]> | undefined;
    expect(validations?.at(-1)?.[0]).toBe(true);
  });

  it('sets the maxlength attribute on the name input to the configured max', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });
    await nextTick();

    const input = wrapper.find('input.uis-input');
    expect(input.attributes('maxlength')).toBe('100');
  });

  it('emits false when modelValue is prefilled with a name exceeding the max length', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'A'.repeat(INTEGRATION_NAME_MAX_LENGTH + 1),
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_PDF',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: [],
      },
    });
    await nextTick();

    const validations = wrapper.emitted('validation-change') as Array<[boolean]> | undefined;
    expect(validations?.at(-1)?.[0]).toBe(false);
  });

  it('shows max-length error immediately when name is updated via prop (clone/edit prefill)', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: 'DOCUMENT_PDF' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
        normalizedNames: [],
      },
    });
    await nextTick();

    // Simulate wizard programmatically setting the name (e.g. "Copy of <long name>" in clone)
    await wrapper.setProps({
      modelValue: {
        name: 'A'.repeat(INTEGRATION_NAME_MAX_LENGTH + 1),
        description: '',
        itemType: 'DOCUMENT',
        subType: 'DOCUMENT_PDF',
      },
    });
    await nextTick();

    expect(wrapper.text()).toContain(`Maximum ${INTEGRATION_NAME_MAX_LENGTH} characters allowed`);

    const validations = wrapper.emitted('validation-change') as Array<[boolean]> | undefined;
    expect(validations?.at(-1)?.[0]).toBe(false);
  });

  it('shows subtype loading errors and retries loading subtypes', async () => {
    vi.mocked(KwDocService.getSubItemTypes)
      .mockRejectedValueOnce({ body: { message: 'Subtype load failed' } })
      .mockResolvedValueOnce([{ code: 'DOCUMENT_PDF', displayValue: 'PDF Document' }]);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: 'ArcGIS Name', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();

    expect(wrapper.text()).toContain('Subtype load failed');

    await wrapper.find('button.uis-retry-btn').trigger('click');
    await flushPromises();

    expect(KwDocService.getSubItemTypes).toHaveBeenCalledTimes(2);
    expect(wrapper.text()).toContain('PDF Document');
  });

  it('normalizes a dynamic document title to its matching id on mount', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
    ]);
    vi.mocked(KwDocService.getDynamicDocuments).mockResolvedValue([
      { id: 'doc-1', title: 'Dynamic Packet' },
    ]);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_DRAFT_DYNAMIC',
          dynamicDocument: 'Dynamic Packet',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();

    const updateEvents = wrapper.emitted('update:modelValue');
    expect(updateEvents).toBeTruthy();
    const lastModel = updateEvents![updateEvents!.length - 1][0] as {
      dynamicDocument: string;
      dynamicDocumentLabel: string;
    };

    expect(lastModel.dynamicDocument).toBe('doc-1');
    expect(lastModel.dynamicDocumentLabel).toBe('Dynamic Packet');
  });

  it('clears a stale dynamic document when it does not match any loaded option', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
    ]);
    vi.mocked(KwDocService.getDynamicDocuments).mockResolvedValue([
      { id: 'doc-1', title: 'Dynamic Packet' },
    ]);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_DRAFT_DYNAMIC',
          dynamicDocument: 'Unknown Packet',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();

    const updateEvents = wrapper.emitted('update:modelValue');
    expect(updateEvents).toBeTruthy();
    const lastModel = updateEvents![updateEvents!.length - 1][0] as {
      dynamicDocument: string;
      dynamicDocumentLabel?: string;
    };

    expect(lastModel.dynamicDocument).toBe('');
    expect(lastModel.dynamicDocumentLabel).toBe('');
  });

  it('retries dynamic document loading after an API error', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
    ]);
    vi.mocked(KwDocService.getDynamicDocuments)
      .mockRejectedValueOnce(new Error('Dynamic docs failed'))
      .mockResolvedValueOnce([{ id: 'doc-1', title: 'Dynamic Packet' }]);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_DRAFT_DYNAMIC',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();

    expect(wrapper.text()).toContain('Dynamic docs failed');

    const retryButtons = wrapper.findAll('button.uis-retry-btn');
    await retryButtons[retryButtons.length - 1].trigger('click');
    await flushPromises();

    expect(KwDocService.getDynamicDocuments).toHaveBeenCalledTimes(2);
    expect(wrapper.text()).toContain('Dynamic Packet');
  });

  it('clears dynamic selections when switching from a dynamic subtype to a static subtype', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
      { code: 'DOCUMENT_PDF', displayValue: 'PDF Document' },
    ]);
    vi.mocked(KwDocService.getDynamicDocuments).mockResolvedValue([
      { id: 'doc-1', title: 'Dynamic Packet' },
    ]);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_DRAFT_DYNAMIC',
          dynamicDocument: 'doc-1',
          dynamicDocumentLabel: 'Dynamic Packet',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();

    const selects = wrapper.findAll('select.uis-input');
    await selects[1].setValue('DOCUMENT_PDF');
    await flushPromises();

    const updateEvents = wrapper.emitted('update:modelValue');
    expect(updateEvents).toBeTruthy();
    const lastModel = updateEvents![updateEvents!.length - 1][0] as {
      subType: string;
      dynamicDocument: string;
      dynamicDocumentLabel: string;
    };

    expect(lastModel.subType).toBe('DOCUMENT_PDF');
    expect(lastModel.dynamicDocument).toBe('');
    expect(lastModel.dynamicDocumentLabel).toBe('');
    expect(KwDocService.getDynamicDocuments).toHaveBeenCalledTimes(1);
  });

  it('uses the default dynamic subtype codes when config overrides are omitted', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
    ]);
    vi.mocked(KwDocService.getDynamicDocuments).mockResolvedValue([
      { id: 'doc-1', title: 'Dynamic Packet' },
    ]);

    const configWithoutDynamicCodes = {
      ...ARCGIS_UNIFIED_STEP_CONFIG,
      dynamicSubtypeCodes: undefined,
    };

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_DRAFT_DYNAMIC',
        },
        config: configWithoutDynamicCodes,
      },
    });

    await flushPromises();

    expect(KwDocService.getDynamicDocuments).toHaveBeenCalledWith(
      'DOCUMENT',
      'DOCUMENT_DRAFT_DYNAMIC'
    );
    expect(wrapper.findAll('select.uis-input')).toHaveLength(3);
  });

  it('falls back to the generic subtype load error message when the API error is empty', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockRejectedValueOnce({});

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: 'ArcGIS Name', description: '', itemType: 'DOCUMENT', subType: '' },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();

    expect(wrapper.text()).toContain('Failed to load subtypes');
  });

  it('uses the body message when dynamic document loading fails', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
    ]);
    vi.mocked(KwDocService.getDynamicDocuments).mockRejectedValueOnce({
      body: { message: 'Body-level dynamic failure' },
    });

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_DRAFT_DYNAMIC',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();

    expect(wrapper.text()).toContain('Body-level dynamic failure');
  });

  it('loads dynamic documents when props switch from a static subtype to a dynamic subtype', async () => {
    vi.mocked(KwDocService.getSubItemTypes).mockResolvedValue([
      { code: 'DOCUMENT_PDF', displayValue: 'PDF Document' },
      { code: 'DOCUMENT_DRAFT_DYNAMIC', displayValue: 'Dynamic Draft' },
    ]);
    vi.mocked(KwDocService.getDynamicDocuments).mockResolvedValue([
      { id: 'doc-1', title: 'Dynamic Packet' },
    ]);

    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: {
          name: 'ArcGIS Name',
          description: '',
          itemType: 'DOCUMENT',
          subType: 'DOCUMENT_PDF',
        },
        config: ARCGIS_UNIFIED_STEP_CONFIG,
      },
    });

    await flushPromises();
    vi.mocked(KwDocService.getDynamicDocuments).mockClear();

    await wrapper.setProps({
      modelValue: {
        name: 'ArcGIS Name',
        description: '',
        itemType: 'DOCUMENT',
        subType: 'DOCUMENT_DRAFT_DYNAMIC',
      },
    });
    await flushPromises();

    expect(KwDocService.getDynamicDocuments).toHaveBeenCalledWith(
      'DOCUMENT',
      'DOCUMENT_DRAFT_DYNAMIC'
    );
  });
});
