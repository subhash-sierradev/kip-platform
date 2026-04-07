import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { FieldTransformationType } from '@/api/models/FieldTransformationType';
import {
  DEFAULT_SOURCE_FIELD,
  DEFAULT_TARGET_FIELD,
} from '@/components/outbound/arcgisintegration/utils/fieldMappingConstants';
import FieldMappingStep from '@/components/outbound/arcgisintegration/wizard/steps/FieldMappingStep.vue';

// Mock composables
vi.mock('@/composables/useSourceFields', () => ({
  useSourceFields: () => ({
    fields: { value: [] },
    loading: { value: false },
    error: { value: null },
    load: vi.fn(),
  }),
}));

vi.mock('@/composables/useArcgisFeatures', () => ({
  useArcgisFeatures: () => ({
    fields: { value: [] },
    load: vi.fn(),
  }),
}));

describe('FieldMappingStep', () => {
  const defaultProps = {
    modelValue: { fieldMappings: [] },
    connectionId: 'test-connection-id',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders component successfully', () => {
      const wrapper = mount(FieldMappingStep, {
        props: defaultProps,
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.fm-root').exists()).toBe(true);
      expect(wrapper.find('.fm-mapping-table').exists()).toBe(true);
    });

    it('displays table headers', () => {
      const wrapper = mount(FieldMappingStep, {
        props: defaultProps,
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      expect(wrapper.text()).toContain('Document Field');
      expect(wrapper.text()).toContain('Transformation');
      expect(wrapper.text()).toContain('ArcGIS Layer Field');
      expect(wrapper.text()).toContain('Mandatory');
    });
  });

  describe('CREATE Mode - Empty Slot Addition', () => {
    it('adds empty row in CREATE mode with only default mapping', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: {
          ...defaultProps,
          mode: 'create',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should have default mapping + empty row (2 total)
      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBe(2);

      // First row should be default mapping
      expect(rows[0].find('select').exists()).toBe(true);
    });

    it('does not add empty row if already has multiple mappings', async () => {
      const modelValue = {
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
            transformationType: FieldTransformationType.PASSTHROUGH,
            isMandatory: false,
            displayOrder: 1,
          },
        ],
      };

      const wrapper = mount(FieldMappingStep, {
        props: {
          modelValue,
          connectionId: 'test-connection-id',
          mode: 'create',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should have exactly 2 rows (no extra empty row)
      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBe(2);
    });
  });

  describe('EDIT Mode - No Empty Slot', () => {
    it('does NOT add empty row in EDIT mode with only default mapping', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: {
          ...defaultProps,
          mode: 'edit',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should have only default mapping (1 row)
      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBe(1);
    });

    it('preserves exact mapping structure in EDIT mode', async () => {
      const modelValue = {
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
            transformationType: FieldTransformationType.UPPERCASE,
            isMandatory: false,
            displayOrder: 1,
          },
        ],
      };

      const wrapper = mount(FieldMappingStep, {
        props: {
          modelValue,
          connectionId: 'test-connection-id',
          mode: 'edit',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should have exactly 2 rows, no extra empty rows
      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBe(2);
    });
  });

  describe('CLONE Mode - No Empty Slot', () => {
    it('does NOT add empty row in CLONE mode with only default mapping', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: {
          ...defaultProps,
          mode: 'clone',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should have only default mapping (1 row)
      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBe(1);
    });

    it('preserves exact mapping structure in CLONE mode', async () => {
      const modelValue = {
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
      };

      const wrapper = mount(FieldMappingStep, {
        props: {
          modelValue,
          connectionId: 'test-connection-id',
          mode: 'clone',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should have exactly 3 rows (exact replica, no empty row added)
      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBe(3);
    });
  });

  describe('Default Mapping Behavior', () => {
    it('ensures default mapping exists and is first', async () => {
      const modelValue = {
        fieldMappings: [],
      };

      const wrapper = mount(FieldMappingStep, {
        props: {
          modelValue,
          connectionId: 'test-connection-id',
          mode: 'edit',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should have created default mapping
      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBeGreaterThanOrEqual(1);
    });

    it('disables editing of default mapping', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: defaultProps,
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // First select should be disabled (default mapping source field)
      const firstSelect = wrapper.find('.fm-source-field select');
      expect(firstSelect.attributes('disabled')).toBeDefined();
    });
  });

  describe('Validation', () => {
    it('emits validation-change event on mount', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: {
          ...defaultProps,
          mode: 'edit',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      // Should emit validation-change event
      expect(wrapper.emitted('validation-change')).toBeTruthy();
    });

    it('invalid when mappings are incomplete', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: {
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
          connectionId: 'test-connection-id',
          mode: 'edit',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      const emitted = wrapper.emitted('validation-change');
      expect(emitted).toBeTruthy();
      // Last emitted should indicate invalid state
      const lastValidation = emitted?.[emitted.length - 1];
      expect(lastValidation?.[0]).toBe(false);
    });
  });

  describe('Field Mapping Operations', () => {
    it('handles model value updates via prop', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: defaultProps,
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

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

      const emitted = wrapper.emitted('update:modelValue');
      expect(emitted).toBeTruthy();

      const lastUpdate = (emitted?.[emitted.length - 1]?.[0] ?? { fieldMappings: [] }) as {
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

    it('normalizes transformation type to PASSTHROUGH by default', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: {
          modelValue: { fieldMappings: [] },
          connectionId: 'test-connection-id',
          mode: 'edit',
        },
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await wrapper.setProps({
        modelValue: {
          fieldMappings: [
            {
              id: 'mapping-1',
              sourceField: 'field1',
              targetField: 'target1',
              transformationType: '', // empty
              isMandatory: false,
              displayOrder: 0,
            },
          ],
        },
      });
      await flushPromises();

      const emitted = wrapper.emitted('update:modelValue');
      expect(emitted).toBeTruthy();

      const lastUpdate = (emitted?.[emitted.length - 1]?.[0] ?? { fieldMappings: [] }) as {
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
  });

  describe('Props Validation', () => {
    it('accepts all valid mode props', async () => {
      for (const mode of ['create', 'edit', 'clone']) {
        const wrapper = mount(FieldMappingStep, {
          props: {
            ...defaultProps,
            mode: mode as 'create' | 'edit' | 'clone',
          },
          global: {
            stubs: {
              i: { template: '<span></span>' },
            },
          },
        });

        expect(wrapper.exists()).toBe(true);
      }
    });

    it('defaults mode to create when not provided', async () => {
      const wrapper = mount(FieldMappingStep, {
        props: defaultProps,
        global: {
          stubs: {
            i: { template: '<span></span>' },
          },
        },
      });

      await flushPromises();

      const rows = wrapper.findAll('.fm-mapping-row');
      expect(rows.length).toBe(2);
    });
  });
});
