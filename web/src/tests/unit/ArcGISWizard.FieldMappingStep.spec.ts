import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import { FieldTransformationType } from '@/api/models/FieldTransformationType';
import {
  DEFAULT_SOURCE_FIELD,
  DEFAULT_TARGET_FIELD,
} from '@/components/outbound/arcgisintegration/utils/fieldMappingConstants';
import FieldMappingStep from '@/components/outbound/arcgisintegration/wizard/steps/FieldMappingStep.vue';
import type { FieldMapping, FieldMappingData } from '@/types/ArcGISFormData';

const hoisted = vi.hoisted(() => ({
  sourceFields: null as any,
  sourceFieldsLoading: null as any,
  sourceFieldsError: null as any,
  loadSourceFieldsMock: vi.fn(),
  arcgisFields: null as any,
  loadArcgisFieldsMock: vi.fn(),
}));

hoisted.sourceFields = ref<any[]>([]);
hoisted.sourceFieldsLoading = ref(false);
hoisted.sourceFieldsError = ref<string | null>(null);
hoisted.arcgisFields = ref<any[]>([]);

vi.mock('@/composables/useSourceFields', () => ({
  useSourceFields: () => ({
    fields: hoisted.sourceFields,
    loading: hoisted.sourceFieldsLoading,
    error: hoisted.sourceFieldsError,
    load: hoisted.loadSourceFieldsMock,
  }),
}));

vi.mock('@/composables/useArcgisFeatures', () => ({
  useArcgisFeatures: () => ({
    fields: hoisted.arcgisFields,
    load: hoisted.loadArcgisFieldsMock,
  }),
}));

describe('ArcGIS Wizard - FieldMappingStep', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.sourceFields.value = [
      { id: 'source-0', fieldName: DEFAULT_SOURCE_FIELD, fieldType: 'TEXT' },
      { id: 'source-1', fieldName: 'name', fieldType: 'TEXT' },
      { id: 'source-2', fieldName: 'status', fieldType: 'TEXT' },
    ];
    hoisted.sourceFieldsLoading.value = false;
    hoisted.sourceFieldsError.value = null;
    hoisted.arcgisFields.value = [
      { name: DEFAULT_TARGET_FIELD, nullable: false },
      { name: 'Email Id', nullable: true },
      { name: 'Status', nullable: true },
    ];
  });

  const mountStep = (
    modelValue: FieldMappingData = { fieldMappings: [] as FieldMapping[] },
    extra: Record<string, unknown> = {}
  ) =>
    mount(FieldMappingStep, {
      props: { modelValue, connectionId: 'conn-1', ...extra },
      global: { stubs: { i: { template: '<span></span>' } } },
    });

  it('emits validation-change false on mount when no user mappings are complete', async () => {
    const wrapper = mountStep();
    await flushPromises();

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);
  });

  it('emits validation-change true when all mappings are complete', async () => {
    const wrapper = mountStep({
      fieldMappings: [
        {
          id: 'default',
          sourceField: DEFAULT_SOURCE_FIELD,
          targetField: DEFAULT_TARGET_FIELD,
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: true,
          displayOrder: 0,
          isDefault: true,
        },
        {
          id: 'mapping-1',
          sourceField: 'name',
          targetField: 'Email Id',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 1,
        },
      ],
    });
    await flushPromises();

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(true);
  });

  it('emits update:modelValue when a source field is changed', async () => {
    const wrapper = mountStep({
      fieldMappings: [
        {
          id: 'default',
          sourceField: DEFAULT_SOURCE_FIELD,
          targetField: DEFAULT_TARGET_FIELD,
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: true,
          displayOrder: 0,
          isDefault: true,
        },
        {
          id: 'mapping-1',
          sourceField: '',
          targetField: '',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 1,
        },
      ],
    });
    await flushPromises();

    const selects = wrapper.findAll('select.fm-input');
    const nonDefaultSourceSelect = selects.find(
      s => !s.attributes('disabled') && (s.element as HTMLSelectElement).value === ''
    );
    expect(nonDefaultSourceSelect).toBeTruthy();
    await nonDefaultSourceSelect!.setValue('name');
    await flushPromises();

    const updateEvents = wrapper.emitted('update:modelValue');
    expect(updateEvents).toBeTruthy();
    expect(updateEvents!.length).toBeGreaterThan(0);
  });

  it('adds a new mapping row when the add button is clicked', async () => {
    const wrapper = mountStep({
      fieldMappings: [
        {
          id: 'default',
          sourceField: DEFAULT_SOURCE_FIELD,
          targetField: DEFAULT_TARGET_FIELD,
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: true,
          displayOrder: 0,
          isDefault: true,
        },
        {
          id: 'mapping-1',
          sourceField: 'name',
          targetField: 'Email Id',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 1,
        },
      ],
    });
    await flushPromises();

    const beforeCount = wrapper.findAll('.fm-mapping-row').length;
    await wrapper.find('.fm-btn-add').trigger('click');
    await flushPromises();

    expect(wrapper.findAll('.fm-mapping-row').length).toBe(beforeCount + 1);
  });

  it('removes a non-default mapping row when the remove button is clicked', async () => {
    const wrapper = mountStep({
      fieldMappings: [
        {
          id: 'default',
          sourceField: DEFAULT_SOURCE_FIELD,
          targetField: DEFAULT_TARGET_FIELD,
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: true,
          displayOrder: 0,
          isDefault: true,
        },
        {
          id: 'mapping-1',
          sourceField: 'name',
          targetField: 'Email Id',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 1,
        },
        {
          id: 'mapping-2',
          sourceField: 'status',
          targetField: 'Status',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 2,
        },
      ],
    });
    await flushPromises();

    const beforeCount = wrapper.findAll('.fm-mapping-row').length;
    await wrapper.find('.fm-btn-remove').trigger('click');
    await flushPromises();

    expect(wrapper.findAll('.fm-mapping-row').length).toBe(beforeCount - 1);
  });

  it('does not show remove button for the default mandatory mapping', async () => {
    const wrapper = mountStep({
      fieldMappings: [
        {
          id: 'default',
          sourceField: DEFAULT_SOURCE_FIELD,
          targetField: DEFAULT_TARGET_FIELD,
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: true,
          displayOrder: 0,
          isDefault: true,
        },
        {
          id: 'mapping-1',
          sourceField: 'name',
          targetField: 'Email Id',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 1,
        },
      ],
    });
    await flushPromises();

    const rows = wrapper.findAll('.fm-mapping-row');
    expect(rows[0].find('.fm-btn-remove').exists()).toBe(false);
  });

  it('calls load composables on mount with the provided connectionId', async () => {
    mountStep({ fieldMappings: [] }, { connectionId: 'conn-xyz' });
    await flushPromises();

    expect(hoisted.loadSourceFieldsMock).toHaveBeenCalledTimes(1);
    expect(hoisted.loadArcgisFieldsMock).toHaveBeenCalledWith('conn-xyz');
  });

  it('emits validation-change false when duplicate source fields exist', async () => {
    const wrapper = mountStep({
      fieldMappings: [
        {
          id: 'default',
          sourceField: DEFAULT_SOURCE_FIELD,
          targetField: DEFAULT_TARGET_FIELD,
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: true,
          displayOrder: 0,
          isDefault: true,
        },
        {
          id: 'mapping-1',
          sourceField: 'name',
          targetField: 'Email Id',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 1,
        },
        {
          id: 'mapping-2',
          sourceField: 'name',
          targetField: 'Status',
          transformationType: FieldTransformationType.PASSTHROUGH,
          isMandatory: false,
          displayOrder: 2,
        },
      ],
    });
    await flushPromises();

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);
  });
});
