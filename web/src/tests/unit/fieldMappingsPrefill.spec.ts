import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { prefillWizardFieldMappingsFromDetail } from '@/components/outbound/arcgisintegration/wizard/fieldMappingsPrefill';

describe('fieldMappingsPrefill', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Use fake timers for consistent ID generation
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-02-05T10:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('prefillWizardFieldMappingsFromDetail', () => {
    it('should handle empty array and add default mapping', () => {
      const result = prefillWizardFieldMappingsFromDetail([]);

      expect(result).toHaveLength(2); // Default + empty slot
      expect(result[0]).toMatchObject({
        sourceField: 'id',
        targetField: 'external_location_id',
        transformationType: 'PASSTHROUGH',
        isMandatory: true,
        displayOrder: 0,
        isDefault: true,
      });
      expect(result[0].id).toMatch(/^default-/);

      expect(result[1]).toMatchObject({
        sourceField: '',
        targetField: '',
        transformationType: '',
        isMandatory: false,
        displayOrder: 1,
        isDefault: false,
      });
      expect(result[1].id).toMatch(/^field-/);
    });

    it('should handle undefined input and treat as empty array', () => {
      const result = prefillWizardFieldMappingsFromDetail(undefined as any);

      expect(result).toHaveLength(2);
      expect(result[0].isDefault).toBe(true);
    });

    it('should map existing field mappings with all properties', () => {
      const input = [
        {
          id: 'existing-1',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          transformationType: 'UPPERCASE',
          isMandatory: true,
          displayOrder: 5,
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      // Should add default mapping at start
      // Empty slot logic: only adds when mapped.length === 1 AFTER adding default
      // So: input (1) + default (1) = 2, no empty slot added
      expect(result).toHaveLength(2); // Default + 1 existing

      expect(result[0].isDefault).toBe(true);
      expect(result[0].displayOrder).toBe(0);

      expect(result[1]).toMatchObject({
        id: 'existing-1',
        sourceField: 'name',
        targetField: 'location_name',
        transformationType: 'UPPERCASE',
        isMandatory: true,
        displayOrder: 1, // Reordered after default
        isDefault: false,
      });
    });

    it('should preserve default mapping if already present', () => {
      const input = [
        {
          id: 'default-existing',
          sourceFieldPath: 'id',
          targetFieldPath: 'external_location_id',
          transformationType: 'PASSTHROUGH',
          isMandatory: true,
          displayOrder: 0,
        },
        {
          id: 'field-2',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          transformationType: '',
          isMandatory: false,
          displayOrder: 1,
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result).toHaveLength(2); // Default + 1 field (no empty slot, 2+ mappings)
      expect(result[0].id).toBe('default-existing');
      expect(result[0].isDefault).toBe(true);
      expect(result[1].id).toBe('field-2');
    });

    it('should not add empty slot if 2 or more mappings exist', () => {
      const input = [
        {
          id: 'default-1',
          sourceFieldPath: 'id',
          targetFieldPath: 'external_location_id',
        },
        {
          id: 'field-2',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result).toHaveLength(2); // No empty slot added
    });

    it('should generate ID when not provided', () => {
      const input = [
        {
          sourceFieldPath: 'address',
          targetFieldPath: 'location_address',
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result).toHaveLength(2); // Default + 1 field (no empty slot, 2+ mappings)
      expect(result[0].id).toMatch(/^default-/);
      expect(result[1].id).toMatch(/^field-/);
    });

    it('should use index as displayOrder when not provided', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          // no displayOrder
        },
        {
          id: 'field-2',
          sourceFieldPath: 'address',
          targetFieldPath: 'location_address',
          // no displayOrder
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      // Default mapping added, then reordered
      expect(result[0].isDefault).toBe(true);
      expect(result[0].displayOrder).toBe(0);
      expect(result[1].id).toBe('field-1');
      expect(result[1].displayOrder).toBe(1);
      expect(result[2].id).toBe('field-2');
      expect(result[2].displayOrder).toBe(2);
    });

    it('should handle displayOrder: 0 correctly', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          displayOrder: 0,
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      // Default added at start, original gets reordered
      expect(result[0].isDefault).toBe(true);
      expect(result[0].displayOrder).toBe(0);
      expect(result[1].id).toBe('field-1');
      expect(result[1].displayOrder).toBe(1);
    });

    it('should handle empty sourceFieldPath and targetFieldPath', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: '',
          targetFieldPath: '',
          transformationType: '',
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result).toHaveLength(2); // Default + empty field (no extra empty slot, 2+ mappings)
      expect(result[1]).toMatchObject({
        id: 'field-1',
        sourceField: '',
        targetField: '',
        transformationType: '',
        isDefault: false,
      });
    });

    it('should handle undefined optional fields with defaults', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          // transformationType undefined
          // isMandatory undefined
          // displayOrder undefined
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result[1]).toMatchObject({
        id: 'field-1',
        sourceField: 'name',
        targetField: 'location_name',
        transformationType: '', // Default empty string
        isMandatory: false, // Default false
        isDefault: false,
      });
    });

    it('should handle isMandatory: false correctly (not truthy coercion issue)', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          isMandatory: false,
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result[1].isMandatory).toBe(false);
    });

    it('should handle isMandatory: true correctly', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          isMandatory: true,
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result[1].isMandatory).toBe(true);
    });

    it('should detect default mapping correctly (case sensitive)', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'id',
          targetFieldPath: 'external_location_id',
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      // Should recognize as default and not add duplicate
      expect(result).toHaveLength(2); // Default (existing) + empty slot
      expect(result[0].isDefault).toBe(true);
    });

    it('should not detect as default if case differs', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'ID', // Different case
          targetFieldPath: 'external_location_id',
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      // Should add default mapping (no empty slot with 2+ mappings)
      expect(result).toHaveLength(2);
      expect(result[0].isDefault).toBe(true);
      expect(result[0].sourceField).toBe('id');
      expect(result[1].isDefault).toBe(false);
      expect(result[1].sourceField).toBe('ID');
    });

    it('should add default when only target field matches', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'objectId',
          targetFieldPath: 'external_location_id', // Matches default target
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      // Not a default mapping (source doesn't match), so default should be added (no empty slot, 2+ mappings)
      expect(result).toHaveLength(2);
      expect(result[0].isDefault).toBe(true);
      expect(result[1].isDefault).toBe(false);
    });

    it('should add default when only source field matches', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'id', // Matches default source
          targetFieldPath: 'location_id',
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      // Not a default mapping (target doesn't match), so default should be added (no empty slot, 2+ mappings)
      expect(result).toHaveLength(2);
      expect(result[0].isDefault).toBe(true);
      expect(result[1].isDefault).toBe(false);
    });

    it('should handle multiple field mappings and reorder correctly', () => {
      const input = [
        {
          id: 'field-1',
          sourceFieldPath: 'name',
          targetFieldPath: 'location_name',
          displayOrder: 10,
        },
        {
          id: 'field-2',
          sourceFieldPath: 'address',
          targetFieldPath: 'location_address',
          displayOrder: 20,
        },
        {
          id: 'field-3',
          sourceFieldPath: 'city',
          targetFieldPath: 'location_city',
          displayOrder: 30,
        },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      expect(result).toHaveLength(4); // Default + 3 fields
      expect(result[0].isDefault).toBe(true);
      expect(result[0].displayOrder).toBe(0);
      expect(result[1].id).toBe('field-1');
      expect(result[1].displayOrder).toBe(1);
      expect(result[2].id).toBe('field-2');
      expect(result[2].displayOrder).toBe(2);
      expect(result[3].id).toBe('field-3');
      expect(result[3].displayOrder).toBe(3);
    });

    it('should generate unique IDs for different mappings', () => {
      const input = [
        { sourceFieldPath: 'field1', targetFieldPath: 'target1' },
        { sourceFieldPath: 'field2', targetFieldPath: 'target2' },
      ];

      const result = prefillWizardFieldMappingsFromDetail(input);

      const ids = result.map(m => m.id);
      const uniqueIds = new Set(ids);
      expect(uniqueIds.size).toBe(ids.length); // All IDs should be unique
    });
  });
});
