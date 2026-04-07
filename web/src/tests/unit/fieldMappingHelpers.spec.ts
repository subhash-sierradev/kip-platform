import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { FieldMapping } from '@/types/ArcGISFormData';
import {
  DEFAULT_SOURCE_FIELD,
  DEFAULT_TARGET_FIELD,
  ensureDefaultMapping,
  ensureEmptyMappingSlot,
  isDefaultMapping,
  transformFieldMappingsForEdit,
} from '@/utils/fieldMappingHelpers';

describe('fieldMappingHelpers', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-15T10:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('isDefaultMapping', () => {
    it('returns true for default mandatory mapping', () => {
      const mapping: Partial<FieldMapping> = {
        sourceField: 'ObjectId',
        targetField: 'arcgis-object-id',
      };

      expect(isDefaultMapping(mapping)).toBe(true);
    });

    it('returns false when sourceField does not match', () => {
      const mapping: Partial<FieldMapping> = {
        sourceField: 'CustomField',
        targetField: 'arcgis-object-id',
      };

      expect(isDefaultMapping(mapping)).toBe(false);
    });

    it('returns false when targetField does not match', () => {
      const mapping: Partial<FieldMapping> = {
        sourceField: 'ObjectId',
        targetField: 'custom-target',
      };

      expect(isDefaultMapping(mapping)).toBe(false);
    });

    it('returns false when both fields do not match', () => {
      const mapping: Partial<FieldMapping> = {
        sourceField: 'CustomField',
        targetField: 'custom-target',
      };

      expect(isDefaultMapping(mapping)).toBe(false);
    });

    it('returns false for empty mapping', () => {
      const mapping: Partial<FieldMapping> = {
        sourceField: '',
        targetField: '',
      };

      expect(isDefaultMapping(mapping)).toBe(false);
    });

    it('returns false when fields are undefined', () => {
      const mapping: Partial<FieldMapping> = {};

      expect(isDefaultMapping(mapping)).toBe(false);
    });

    it('uses correct default constants', () => {
      expect(DEFAULT_SOURCE_FIELD).toBe('ObjectId');
      expect(DEFAULT_TARGET_FIELD).toBe('arcgis-object-id');
    });
  });

  describe('transformFieldMappingsForEdit', () => {
    it('transforms single API response to edit format', () => {
      const apiMappings = [
        {
          id: 123,
          sourceFieldPath: 'field1',
          targetFieldPath: 'target1',
          transformationType: 'UPPERCASE',
          isMandatory: true,
          displayOrder: 0,
        },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result).toHaveLength(1);
      expect(result[0]).toEqual({
        id: '123',
        sourceField: 'field1',
        targetField: 'target1',
        transformationType: 'UPPERCASE',
        isMandatory: true,
        displayOrder: 0,
        isDefault: false,
      });
    });

    it('marks default mapping with isDefault flag', () => {
      const apiMappings = [
        {
          id: 1,
          sourceFieldPath: 'ObjectId',
          targetFieldPath: 'arcgis-object-id',
          transformationType: 'PASSTHROUGH',
          isMandatory: true,
          displayOrder: 0,
        },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result[0].isDefault).toBe(true);
    });

    it('generates unique ID when id is missing', () => {
      const apiMappings = [
        {
          sourceFieldPath: 'field1',
          targetFieldPath: 'target1',
        },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result[0].id).toMatch(/^field-\d+-/);
    });

    it('handles string ID from API', () => {
      const apiMappings = [
        {
          id: 'abc-123',
          sourceFieldPath: 'field1',
          targetFieldPath: 'target1',
        },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result[0].id).toBe('abc-123');
    });

    it('uses empty strings for missing field paths', () => {
      const apiMappings = [
        {
          id: 1,
        },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result[0].sourceField).toBe('');
      expect(result[0].targetField).toBe('');
      expect(result[0].transformationType).toBe('');
      expect(result[0].isMandatory).toBe(false);
    });

    it('uses index as displayOrder when not provided', () => {
      const apiMappings = [
        { sourceFieldPath: 'field1', targetFieldPath: 'target1' },
        { sourceFieldPath: 'field2', targetFieldPath: 'target2' },
        { sourceFieldPath: 'field3', targetFieldPath: 'target3' },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result[0].displayOrder).toBe(0);
      expect(result[1].displayOrder).toBe(1);
      expect(result[2].displayOrder).toBe(2);
    });

    it('preserves provided displayOrder', () => {
      const apiMappings = [
        { sourceFieldPath: 'field1', targetFieldPath: 'target1', displayOrder: 5 },
        { sourceFieldPath: 'field2', targetFieldPath: 'target2', displayOrder: 3 },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result[0].displayOrder).toBe(5);
      expect(result[1].displayOrder).toBe(3);
    });

    it('handles displayOrder of 0', () => {
      const apiMappings = [
        { sourceFieldPath: 'field1', targetFieldPath: 'target1', displayOrder: 0 },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result[0].displayOrder).toBe(0);
    });

    it('transforms multiple mappings', () => {
      const apiMappings = [
        {
          id: 1,
          sourceFieldPath: 'field1',
          targetFieldPath: 'target1',
          transformationType: 'DIRECT',
        },
        {
          id: 2,
          sourceFieldPath: 'field2',
          targetFieldPath: 'target2',
          transformationType: 'UPPERCASE',
        },
        {
          id: 3,
          sourceFieldPath: 'field3',
          targetFieldPath: 'target3',
          transformationType: 'LOWERCASE',
        },
      ];

      const result = transformFieldMappingsForEdit(apiMappings);

      expect(result).toHaveLength(3);
      expect(result[0].id).toBe('1');
      expect(result[1].id).toBe('2');
      expect(result[2].id).toBe('3');
    });

    it('handles empty array', () => {
      const result = transformFieldMappingsForEdit([]);

      expect(result).toEqual([]);
    });
  });

  describe('ensureDefaultMapping', () => {
    it('adds default mapping when not present', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'field-1',
          sourceField: 'customField',
          targetField: 'customTarget',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 0,
        },
      ];

      const result = ensureDefaultMapping(mappings);

      expect(result).toHaveLength(2);
      expect(result[0]).toMatchObject({
        sourceField: 'ObjectId',
        targetField: 'arcgis-object-id',
        transformationType: 'PASSTHROUGH',
        isMandatory: true,
        isDefault: true,
      });
    });

    it('does not add default mapping when already present', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'default-1',
          sourceField: 'ObjectId',
          targetField: 'arcgis-object-id',
          transformationType: 'PASSTHROUGH',
          isMandatory: true,
          displayOrder: 0,
        },
        {
          id: 'field-1',
          sourceField: 'customField',
          targetField: 'customTarget',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 1,
        },
      ];

      const result = ensureDefaultMapping(mappings);

      expect(result).toHaveLength(2);
      expect(result[0].sourceField).toBe('ObjectId');
    });

    it('reorders displayOrder when adding default mapping', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'field-1',
          sourceField: 'field1',
          targetField: 'target1',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 0,
        },
        {
          id: 'field-2',
          sourceField: 'field2',
          targetField: 'target2',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 1,
        },
      ];

      const result = ensureDefaultMapping(mappings);

      expect(result[0].displayOrder).toBe(0); // default mapping
      expect(result[1].displayOrder).toBe(1); // field-1
      expect(result[2].displayOrder).toBe(2); // field-2
    });

    it('generates unique ID with timestamp for default mapping', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'field-1',
          sourceField: 'field1',
          targetField: 'target1',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 0,
        },
      ];

      const result = ensureDefaultMapping(mappings);

      // Check ID starts with 'default-' followed by a timestamp
      expect(result[0].id).toMatch(/^default-\d+$/);
    });

    it('handles empty array by adding default mapping', () => {
      const mappings: FieldMapping[] = [];

      const result = ensureDefaultMapping(mappings);

      expect(result).toHaveLength(1);
      expect(result[0].sourceField).toBe('ObjectId');
      expect(result[0].targetField).toBe('arcgis-object-id');
    });

    it('returns the same array reference (mutates in place)', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'field-1',
          sourceField: 'field1',
          targetField: 'target1',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 0,
        },
      ];

      const result = ensureDefaultMapping(mappings);

      expect(result).toBe(mappings); // Same reference
    });
  });

  describe('ensureEmptyMappingSlot', () => {
    it('adds empty mapping when only one mapping exists', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'default-1',
          sourceField: 'ObjectId',
          targetField: 'arcgis-object-id',
          transformationType: 'PASSTHROUGH',
          isMandatory: true,
          displayOrder: 0,
        },
      ];

      const result = ensureEmptyMappingSlot(mappings);

      expect(result).toHaveLength(2);
      expect(result[1]).toMatchObject({
        sourceField: '',
        targetField: '',
        transformationType: '',
        isMandatory: false,
        displayOrder: 1,
      });
    });

    it('does not add empty mapping when more than one mapping exists', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'default-1',
          sourceField: 'ObjectId',
          targetField: 'arcgis-object-id',
          transformationType: 'PASSTHROUGH',
          isMandatory: true,
          displayOrder: 0,
        },
        {
          id: 'field-1',
          sourceField: 'field1',
          targetField: 'target1',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 1,
        },
      ];

      const result = ensureEmptyMappingSlot(mappings);

      expect(result).toHaveLength(2);
    });

    it('generates unique ID for empty mapping slot', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'default-1',
          sourceField: 'ObjectId',
          targetField: 'arcgis-object-id',
          transformationType: 'PASSTHROUGH',
          isMandatory: true,
          displayOrder: 0,
        },
      ];

      const result = ensureEmptyMappingSlot(mappings);

      expect(result[1].id).toMatch(/^field-\d+-/);
    });

    it('handles empty array (does not add empty slot)', () => {
      const mappings: FieldMapping[] = [];

      const result = ensureEmptyMappingSlot(mappings);

      expect(result).toHaveLength(0);
    });

    it('handles array with three or more mappings (no change)', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'field-1',
          sourceField: 'field1',
          targetField: 'target1',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 0,
        },
        {
          id: 'field-2',
          sourceField: 'field2',
          targetField: 'target2',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 1,
        },
        {
          id: 'field-3',
          sourceField: 'field3',
          targetField: 'target3',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 2,
        },
      ];

      const result = ensureEmptyMappingSlot(mappings);

      expect(result).toHaveLength(3);
    });

    it('returns the same array reference (mutates in place)', () => {
      const mappings: FieldMapping[] = [
        {
          id: 'field-1',
          sourceField: 'field1',
          targetField: 'target1',
          transformationType: 'DIRECT',
          isMandatory: false,
          displayOrder: 0,
        },
      ];

      const result = ensureEmptyMappingSlot(mappings);

      expect(result).toBe(mappings); // Same reference
    });
  });
});
