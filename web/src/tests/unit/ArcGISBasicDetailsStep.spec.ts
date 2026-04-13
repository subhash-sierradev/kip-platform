/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { nextTick } from 'vue';
import { ARCGIS_UNIFIED_STEP_CONFIG } from '@/utils/unifiedIntegrationStepConfig';
import IntegrationDetailsStep from '@/components/common/combinedstep/IntegrationDetailsStep.vue';

vi.mock('@/api/services/KwIntegrationService', () => ({
  KwDocService: {
    getSubItemTypes: vi.fn().mockResolvedValue([]),
    getDynamicDocuments: vi.fn(),
  },
}));

describe('ArcGIS BasicDetailsStep', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('emits updates and validation-change as user types', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
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
      { name: 'My ArcGIS', description: '', itemType: 'DOCUMENT', subType: '' },
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

  it('allows unique name and emits true validation', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
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

  it('allows original name in edit mode', async () => {
    const wrapper = mount(IntegrationDetailsStep, {
      props: {
        modelValue: { name: 'Original Name', description: '', itemType: 'DOCUMENT', subType: '' },
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
        modelValue: { name: '', description: '', itemType: 'DOCUMENT', subType: '' },
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
});
