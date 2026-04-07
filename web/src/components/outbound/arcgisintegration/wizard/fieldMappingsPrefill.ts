import {
  DEFAULT_SOURCE_FIELD,
  DEFAULT_TARGET_FIELD,
  isDefaultMapping,
} from '../utils/fieldMappingConstants';

interface PrefillFieldMappingInput {
  id?: string;
  sourceFieldPath?: string;
  targetFieldPath?: string;
  transformationType?: string;
  isMandatory?: boolean;
  displayOrder?: number;
}

interface WizardFieldMapping {
  id: string;
  sourceField: string;
  targetField: string;
  transformationType: string;
  isMandatory: boolean;
  displayOrder: number;
  isDefault: boolean;
}

function generateFieldMappingId(prefix: 'default' | 'field'): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
}

export function prefillWizardFieldMappingsFromDetail(
  originalFieldMappings: PrefillFieldMappingInput[]
): WizardFieldMapping[] {
  const mapped = (originalFieldMappings || []).map((originalMapping, index) => ({
    id: originalMapping.id || generateFieldMappingId('field'),
    sourceField: originalMapping.sourceFieldPath || '',
    targetField: originalMapping.targetFieldPath || '',
    transformationType: originalMapping.transformationType || '',
    isMandatory: !!originalMapping.isMandatory,
    displayOrder: originalMapping.displayOrder !== undefined ? originalMapping.displayOrder : index,
    isDefault:
      originalMapping.sourceFieldPath === DEFAULT_SOURCE_FIELD &&
      originalMapping.targetFieldPath === DEFAULT_TARGET_FIELD,
  })) as WizardFieldMapping[];

  const hasDefault = mapped.some(isDefaultMapping);
  if (!hasDefault) {
    mapped.unshift({
      id: generateFieldMappingId('default'),
      sourceField: DEFAULT_SOURCE_FIELD,
      targetField: DEFAULT_TARGET_FIELD,
      transformationType: 'PASSTHROUGH',
      isMandatory: true,
      displayOrder: 0,
      isDefault: true,
    });
    mapped.forEach((mapping, idx) => {
      mapping.displayOrder = idx;
    });
  }

  if (mapped.length === 1) {
    mapped.push({
      id: generateFieldMappingId('field'),
      sourceField: '',
      targetField: '',
      transformationType: '',
      isMandatory: false,
      displayOrder: 1,
      isDefault: false,
    });
  }

  return mapped;
}
