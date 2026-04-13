import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import { FieldTransformationType } from '@/api/models/FieldTransformationType';
import {
  DEFAULT_SOURCE_FIELD,
  DEFAULT_TARGET_FIELD,
} from '@/components/outbound/arcgisintegration/utils/fieldMappingConstants';
import FieldMappingStep from '@/components/outbound/arcgisintegration/wizard/steps/FieldMappingStep.vue';

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

describe('FieldMappingStep', () => {
  const defaultProps = {
    modelValue: { fieldMappings: [] },
    connectionId: 'test-connection-id',
  };

  const mountStep = (overrides: Record<string, unknown> = {}) =>
    mount(FieldMappingStep, {
      props: {
        ...defaultProps,
        ...overrides,
      },
      global: {
        stubs: {
          i: { template: '<span></span>' },
        },
      },
    });

  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.sourceFields.value = [
      { id: 'source-0', fieldName: DEFAULT_SOURCE_FIELD, fieldType: 'TEXT' },
      { id: 'source-1', fieldName: 'name', fieldType: 'TEXT' },
      { id: 'source-2', fieldName: 'status', fieldType: 'TEXT' },
      { id: 'source-3', fieldName: 'priority', fieldType: 'TEXT' },
    ];
    hoisted.sourceFieldsLoading.value = false;
    hoisted.sourceFieldsError.value = null;
    hoisted.arcgisFields.value = [
      { name: DEFAULT_TARGET_FIELD, nullable: false },
      { name: 'Email Id', nullable: true },
      { name: 'Status', nullable: true },
      { name: 'Priority', nullable: false },
    ];
  });

  describe('Rendering', () => {
    it('renders component successfully', () => {
      const wrapper = mountStep();

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.fm-root').exists()).toBe(true);
      expect(wrapper.find('.fm-mapping-table').exists()).toBe(true);
    });

    it('displays table headers', () => {
      const wrapper = mountStep();

      expect(wrapper.text()).toContain('Document Field');
      expect(wrapper.text()).toContain('Transformation');
      expect(wrapper.text()).toContain('ArcGIS Layer Field');
      expect(wrapper.text()).toContain('Mandatory');
    });
  });

  describe('Mode-specific initialization', () => {
    it('adds empty row in create mode with only default mapping', async () => {
      const wrapper = mountStep({ mode: 'create' });
      await flushPromises();

      expect(wrapper.findAll('.fm-mapping-row')).toHaveLength(2);
    });

    it('does not add empty row if create mode already has multiple mappings', async () => {
      const wrapper = mountStep({
        mode: 'create',
        modelValue: {
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
              sourceField: 'field1',
              targetField: 'Email Id',
              transformationType: FieldTransformationType.PASSTHROUGH,
              isMandatory: false,
              displayOrder: 1,
            },
          ],
        },
      });
      await flushPromises();

      expect(wrapper.findAll('.fm-mapping-row')).toHaveLength(2);
    });

    it('does not add empty row in edit mode with only default mapping', async () => {
      const wrapper = mountStep({ mode: 'edit' });
      await flushPromises();

      expect(wrapper.findAll('.fm-mapping-row')).toHaveLength(1);
    });

    it('preserves exact mapping structure in clone mode', async () => {
      const wrapper = mountStep({
        mode: 'clone',
        modelValue: {
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
              sourceField: 'field1',
              targetField: 'target1',
              transformationType: FieldTransformationType.LOWERCASE,
              isMandatory: true,
              displayOrder: 1,
            },
            {
              id: 'mapping-2',
              sourceField: 'field2',
              targetField: 'target2',
              transformationType: FieldTransformationType.TRIM,
              isMandatory: false,
              displayOrder: 2,
            },
          ],
        },
      });
      await flushPromises();

      expect(wrapper.findAll('.fm-mapping-row')).toHaveLength(3);
    });

    it('moves an existing default mapping to the first row and normalizes missing values', async () => {
      const wrapper = mountStep({
        mode: 'edit',
        modelValue: {
          fieldMappings: [
            {
              id: 'mapping-1',
              sourceField: 'status',
              targetField: 'Status',
              transformationType: '',
              isMandatory: null,
              displayOrder: null,
            },
            {
              id: 'default',
              sourceField: DEFAULT_SOURCE_FIELD,
              targetField: DEFAULT_TARGET_FIELD,
              transformationType: FieldTransformationType.UPPERCASE,
              isMandatory: false,
              displayOrder: 99,
              isDefault: true,
            },
          ],
        },
      });
      await flushPromises();

      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows).toHaveLength(2);

      const firstRowSelects = rows[0].findAll('select');
      const secondRowSelects = rows[1].findAll('select');

      expect((firstRowSelects[0].element as HTMLSelectElement).value).toBe(DEFAULT_SOURCE_FIELD);
      expect((firstRowSelects[1].element as HTMLSelectElement).value).toBe(
        FieldTransformationType.PASSTHROUGH
      );
      expect((firstRowSelects[2].element as HTMLSelectElement).value).toBe(DEFAULT_TARGET_FIELD);

      expect((secondRowSelects[0].element as HTMLSelectElement).value).toBe('status');
      expect((secondRowSelects[1].element as HTMLSelectElement).value).toBe(
        FieldTransformationType.PASSTHROUGH
      );
    });
  });

  describe('Default mapping behavior', () => {
    it('ensures default mapping exists and is first', async () => {
      const wrapper = mountStep({
        mode: 'edit',
        modelValue: { fieldMappings: [] },
      });
      await flushPromises();

      expect(wrapper.findAll('.fm-mapping-row').length).toBeGreaterThanOrEqual(1);
    });

    it('disables editing of default mapping', async () => {
      const wrapper = mountStep();
      await flushPromises();

      expect(wrapper.find('.fm-source-field select').attributes('disabled')).toBeDefined();
    });
  });

  describe('Validation and loading', () => {
    it('emits validation-change event on mount', async () => {
      const wrapper = mountStep({ mode: 'edit' });
      await flushPromises();

      expect(wrapper.emitted('validation-change')).toBeTruthy();
    });

    it('invalid when mappings are incomplete', async () => {
      const wrapper = mountStep({
        mode: 'edit',
        modelValue: {
          fieldMappings: [
            {
              id: 'empty',
              sourceField: '',
              targetField: '',
              transformationType: '',
              isMandatory: false,
              displayOrder: 0,
            },
          ],
        },
      });
      await flushPromises();

      expect((wrapper.emitted('validation-change') ?? []).at(-1)?.[0]).toBe(false);
    });

    it('emits invalid when duplicate source and target mappings are present', async () => {
      const wrapper = mountStep({
        mode: 'edit',
        modelValue: {
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
              targetField: 'Email Id',
              transformationType: FieldTransformationType.UPPERCASE,
              isMandatory: false,
              displayOrder: 2,
            },
          ],
        },
      });
      await flushPromises();

      expect((wrapper.emitted('validation-change') ?? []).at(-1)).toEqual([false]);
    });

    it('loads source fields and ArcGIS fields on mount when a connection id is provided', async () => {
      mountStep();
      await flushPromises();

      expect(hoisted.loadSourceFieldsMock).toHaveBeenCalledTimes(1);
      expect(hoisted.loadArcgisFieldsMock).toHaveBeenCalledWith('test-connection-id');
    });

    it('skips ArcGIS field loading when connection id is not provided', async () => {
      mountStep({ connectionId: null });
      await flushPromises();

      expect(hoisted.loadSourceFieldsMock).toHaveBeenCalledTimes(1);
      expect(hoisted.loadArcgisFieldsMock).not.toHaveBeenCalled();
    });

    it('surfaces source field loading errors in the UI', async () => {
      hoisted.sourceFieldsError.value = 'network down';
      const wrapper = mountStep();
      await flushPromises();

      expect(wrapper.text()).toContain('Failed to load source fields:');
      expect(wrapper.text()).toContain('network down');
    });
  });

  describe('Field mapping operations', () => {
    it('handles model value updates via prop', async () => {
      const wrapper = mountStep();
      const newModelValue = {
        fieldMappings: [
          {
            id: 'mapping-1',
            sourceField: 'field1',
            targetField: 'target1',
            transformationType: FieldTransformationType.PASSTHROUGH,
            isMandatory: false,
            displayOrder: 0,
          },
        ],
      };

      await wrapper.setProps({ modelValue: newModelValue });
      await flushPromises();

      const lastUpdate = ((wrapper.emitted('update:modelValue') ?? []).at(-1)?.[0] ?? {
        fieldMappings: [],
      }) as {
        fieldMappings: Array<{
          sourceField?: string;
          targetField?: string;
          transformationType?: FieldTransformationType;
        }>;
      };
      expect(lastUpdate.fieldMappings).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            sourceField: 'field1',
            targetField: 'target1',
            transformationType: FieldTransformationType.PASSTHROUGH,
          }),
        ])
      );
    });

    it('normalizes transformation type to passthrough by default', async () => {
      const wrapper = mountStep({ mode: 'edit' });

      await wrapper.setProps({
        modelValue: {
          fieldMappings: [
            {
              id: 'mapping-1',
              sourceField: 'field1',
              targetField: 'target1',
              transformationType: '',
              isMandatory: false,
              displayOrder: 0,
            },
          ],
        },
      });
      await flushPromises();

      const lastUpdate = ((wrapper.emitted('update:modelValue') ?? []).at(-1)?.[0] ?? {
        fieldMappings: [],
      }) as {
        fieldMappings: Array<{
          sourceField?: string;
          targetField?: string;
          transformationType?: FieldTransformationType;
        }>;
      };
      const normalizedMapping = lastUpdate.fieldMappings.find(
        mapping => mapping.sourceField === 'field1' && mapping.targetField === 'target1'
      );

      expect(normalizedMapping?.transformationType).toBe(FieldTransformationType.PASSTHROUGH);
    });

    it('forces mandatory when a non-nullable target field is selected', async () => {
      const wrapper = mountStep({
        mode: 'edit',
        modelValue: {
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
              targetField: '',
              transformationType: FieldTransformationType.PASSTHROUGH,
              isMandatory: false,
              displayOrder: 1,
            },
          ],
        },
      });
      await flushPromises();

      const targetSelects = wrapper.findAll('.fm-target-field select');
      await targetSelects[1].setValue('Priority');
      await flushPromises();

      const mandatoryCheckboxes = wrapper.findAll('.fm-checkbox');
      expect((mandatoryCheckboxes[1].element as HTMLInputElement).checked).toBe(true);
      expect(mandatoryCheckboxes[1].attributes('disabled')).toBeDefined();
    });

    it('adds a row when the last mapping is complete and removes non-default rows', async () => {
      const wrapper = mountStep({
        mode: 'edit',
        modelValue: {
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
        },
      });
      await flushPromises();

      await wrapper.find('.fm-btn-add').trigger('click');
      await flushPromises();
      expect(wrapper.findAll('.fm-mapping-row')).toHaveLength(3);

      await wrapper.find('.fm-btn-remove').trigger('click');
      await flushPromises();

      const latest = (wrapper.emitted('update:modelValue') ?? []).at(-1)?.[0] as {
        fieldMappings: Array<Record<string, unknown>>;
      };
      expect(latest.fieldMappings).toHaveLength(2);
      expect(latest.fieldMappings[0]).toMatchObject({
        sourceField: DEFAULT_SOURCE_FIELD,
        targetField: DEFAULT_TARGET_FIELD,
      });
      expect(latest.fieldMappings[1]).toMatchObject({ displayOrder: 1 });
    });
  });

  describe('Props validation', () => {
    it('accepts all valid mode props', () => {
      for (const mode of ['create', 'edit', 'clone']) {
        const wrapper = mountStep({ mode: mode as 'create' | 'edit' | 'clone' });
        expect(wrapper.exists()).toBe(true);
      }
    });

    it('defaults mode to create when not provided', async () => {
      const wrapper = mountStep();
      await flushPromises();

      expect(wrapper.findAll('.fm-mapping-row')).toHaveLength(2);
    });
  });
});
