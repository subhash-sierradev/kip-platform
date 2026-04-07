import { computed, type Ref, ref } from 'vue';

import type { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import type { JiraParentIssueResponse } from '@/api/services/JiraIntegrationService';
import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';
import { isSubtaskIssueType } from '@/utils/subtaskIssueType';

import { generateUid } from './CustomFieldHelper';

const PARENT_FIELD_KEY = 'parent';
const PARENT_FIELD_LABEL = 'Parent';

interface UseSubtaskParentMappingOptions {
  issueTypes: Ref<JiraIssueType[]>;
  selectedIssueType: Ref<string>;
  customFields: Ref<CustomFieldMapping[]>;
  parentIssues: Ref<JiraParentIssueResponse[]>;
}

export function useSubtaskParentMapping(options: UseSubtaskParentMappingOptions) {
  const selectedParentField = ref('');

  const isSubtaskIssueTypeSelected = computed(() =>
    isSubtaskIssueType(options.issueTypes.value, options.selectedIssueType.value)
  );

  const showParentRequiredError = computed(
    () => isSubtaskIssueTypeSelected.value && !selectedParentField.value.trim()
  );

  const parentFieldOptions = computed(() => buildParentFieldOptions(options.parentIssues.value));

  function upsertParentCustomField(parentPlaceholder: string) {
    options.customFields.value = upsertParentField(options.customFields.value, parentPlaceholder);
  }

  function removeParentCustomField() {
    const nextFields = options.customFields.value.filter(
      field => normalizeFieldKey(field.jiraFieldKey) !== PARENT_FIELD_KEY
    );
    const changed = nextFields.length !== options.customFields.value.length;
    options.customFields.value = nextFields;
    return changed;
  }

  function syncSelectedParentFromCustomFields() {
    selectedParentField.value = extractParentPlaceholder(options.customFields.value);
  }

  return {
    selectedParentField,
    isSubtaskIssueType: isSubtaskIssueTypeSelected,
    showParentRequiredError,
    parentFieldOptions,
    upsertParentCustomField,
    removeParentCustomField,
    syncSelectedParentFromCustomFields,
  };
}

function buildParentFieldOptions(
  parentIssues: JiraParentIssueResponse[]
): Array<{ value: string; label: string }> {
  return (parentIssues || [])
    .filter(issue => typeof issue.key === 'string' && issue.key.trim().length > 0)
    .map(issue => {
      const key = issue.key.trim();
      const summary = (issue.summary || '').trim();
      const label = summary ? `${key} - ${summary}` : key;
      return {
        label,
        value: key,
      };
    });
}

function upsertParentField(
  currentFields: CustomFieldMapping[],
  parentIssueKey: string
): CustomFieldMapping[] {
  const trimmedKey = parentIssueKey.trim();
  const next = [...(currentFields || [])];
  const existingIndex = next.findIndex(
    field => normalizeFieldKey(field.jiraFieldKey) === PARENT_FIELD_KEY
  );

  if (!trimmedKey) {
    if (existingIndex >= 0) {
      next.splice(existingIndex, 1);
    }
    return next;
  }

  const parentField: CustomFieldMapping = {
    _id: existingIndex >= 0 ? next[existingIndex]._id : generateUid(),
    jiraFieldKey: PARENT_FIELD_KEY,
    jiraFieldLabel: PARENT_FIELD_LABEL,
    type: 'object',
    required: true,
    valueSource: 'literal',
    value: buildParentLiteral(trimmedKey),
  };

  if (existingIndex >= 0) {
    next[existingIndex] = parentField;
  } else {
    next.push(parentField);
  }

  return next;
}

function extractParentPlaceholder(customFields: CustomFieldMapping[]): string {
  const parentField = (customFields || []).find(
    field => normalizeFieldKey(field.jiraFieldKey) === PARENT_FIELD_KEY
  );

  if (!parentField?.value) {
    return '';
  }

  try {
    const parsed = JSON.parse(parentField.value);
    if (parsed && typeof parsed.key === 'string' && parsed.key.trim().length > 0) {
      return parsed.key.trim();
    }
  } catch {
    // Fall through to additional fallback checks.
  }

  const wrappedMatch = parentField.value.match(/\{"key":"([^"]+)"\}/);
  if (wrappedMatch?.[1]) {
    return wrappedMatch[1].trim();
  }

  return '';
}

function buildParentLiteral(parentIssueKey: string): string {
  const keyValue = parentIssueKey.trim();
  if (!keyValue) return '';
  return `{"key":"${keyValue}"}`;
}

function normalizeFieldKey(fieldKey: string | undefined): string {
  return String(fieldKey || '')
    .trim()
    .toLowerCase();
}
