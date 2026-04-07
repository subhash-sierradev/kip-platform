/**
 * Helper utilities for ArcGIS Integration Wizard field mapping
 */
import type { FieldMapping } from '@/types/ArcGISFormData';

// API response structure for field mappings from backend
interface FieldMappingApiResponse {
  id?: number | string;
  sourceFieldPath?: string;
  targetFieldPath?: string;
  transformationType?: string;
  isMandatory?: boolean;
  displayOrder?: number;
}

export const DEFAULT_SOURCE_FIELD = 'ObjectId';
export const DEFAULT_TARGET_FIELD = 'arcgis-object-id';

/**
 * Check if a mapping is the default mandatory mapping
 */
export function isDefaultMapping(mapping: Partial<FieldMapping>): boolean {
  return (
    mapping.sourceField === DEFAULT_SOURCE_FIELD && mapping.targetField === DEFAULT_TARGET_FIELD
  );
}

/**
 * Transform field mappings for edit mode - preserve IDs and order
 */
export function transformFieldMappingsForEdit(
  originalFieldMappings: FieldMappingApiResponse[]
): FieldMapping[] {
  return originalFieldMappings.map((originalMapping: FieldMappingApiResponse, index: number) => ({
    id: String(
      originalMapping.id || `field-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`
    ),
    sourceField: originalMapping.sourceFieldPath || '',
    targetField: originalMapping.targetFieldPath || '',
    transformationType: originalMapping.transformationType || '',
    isMandatory: originalMapping.isMandatory || false,
    displayOrder: originalMapping.displayOrder !== undefined ? originalMapping.displayOrder : index,
    isDefault: isDefaultMapping({
      sourceField: originalMapping.sourceFieldPath,
      targetField: originalMapping.targetFieldPath,
    }),
  }));
}

/**
 * Ensure mandatory default mapping exists
 */
export function ensureDefaultMapping(fieldMappings: FieldMapping[]): FieldMapping[] {
  const hasDefaultMapping = fieldMappings.some(isDefaultMapping);

  if (!hasDefaultMapping) {
    fieldMappings.unshift({
      id: `default-${Date.now()}`,
      sourceField: DEFAULT_SOURCE_FIELD,
      targetField: DEFAULT_TARGET_FIELD,
      transformationType: 'PASSTHROUGH',
      isMandatory: true,
      displayOrder: 0,
      isDefault: true,
    });

    // Reorder displayOrder for other mappings
    fieldMappings.forEach((m: FieldMapping, idx: number) => {
      m.displayOrder = idx;
    });
  }

  return fieldMappings;
}

/**
 * Ensure at least one additional mapping slot exists
 */
export function ensureEmptyMappingSlot(fieldMappings: FieldMapping[]): FieldMapping[] {
  if (fieldMappings.length === 1) {
    fieldMappings.push({
      id: `field-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`,
      sourceField: '',
      targetField: '',
      transformationType: '',
      isMandatory: false,
      displayOrder: 1,
    });
  }

  return fieldMappings;
}
