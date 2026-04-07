import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';

import { generateUid } from './CustomFieldHelper';

export interface MappingRow {
  field: string;
  value: string;
  label?: string;
  type?: string;
}

function isParentField(fieldKey?: string): boolean {
  return (
    String(fieldKey || '')
      .trim()
      .toLowerCase() === 'parent'
  );
}

function isTemplateValue(value: string): boolean {
  return /{{\s*.+\s*}}/.test(String(value || ''));
}

function buildUpdatedMapping(
  existing: CustomFieldMapping,
  row: MappingRow,
  value: string
): CustomFieldMapping {
  return {
    ...existing,
    value,
    valueSource: isTemplateValue(value) ? 'json' : (existing.valueSource ?? 'literal'),
    jiraFieldLabel: row.label ?? existing.jiraFieldLabel,
    type: (row.type as CustomFieldMapping['type']) ?? existing.type,
  };
}

function buildNewMapping(fieldKey: string, row: MappingRow, value: string): CustomFieldMapping {
  return {
    _id: generateUid(),
    jiraFieldKey: fieldKey,
    value,
    valueSource: isTemplateValue(value) ? 'json' : 'literal',
    jiraFieldLabel: row.label ?? undefined,
    type: row.type as CustomFieldMapping['type'],
  };
}

export function toNonParentRows(customFields?: CustomFieldMapping[]): MappingRow[] {
  return (customFields || [])
    .filter(customField => !isParentField(customField.jiraFieldKey))
    .map(customField => ({
      field: customField.jiraFieldKey,
      value: customField.value,
      label: customField.jiraFieldLabel,
      type: customField.type,
    }));
}

export function mergeRowsIntoCustomFields(
  nextRows: MappingRow[],
  existingCustomFields: CustomFieldMapping[]
): CustomFieldMapping[] {
  const nextValidRows = nextRows.filter(row => !!row.field && row.field.trim() !== '');
  const rowByField = new Map(nextValidRows.map(row => [row.field, row]));
  const existingByField = new Map(
    (existingCustomFields || []).map(customField => [customField.jiraFieldKey, customField])
  );
  const existingParentField = (existingCustomFields || []).find(customField =>
    isParentField(customField.jiraFieldKey)
  );

  const merged: CustomFieldMapping[] = [];
  for (const [fieldKey, row] of rowByField.entries()) {
    const value = String(row.value ?? '');
    const existing = existingByField.get(fieldKey);
    merged.push(
      existing ? buildUpdatedMapping(existing, row, value) : buildNewMapping(fieldKey, row, value)
    );
  }

  if (existingParentField && !merged.some(customField => isParentField(customField.jiraFieldKey))) {
    merged.push(existingParentField);
  }

  return merged;
}
