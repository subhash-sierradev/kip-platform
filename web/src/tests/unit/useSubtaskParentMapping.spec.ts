import { describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';
import { useSubtaskParentMapping } from '@/components/outbound/jirawebhooks/utils/useSubtaskParentMapping';

vi.mock('@/components/outbound/jirawebhooks/utils/CustomFieldHelper', () => ({
  generateUid: vi.fn(() => 'uid-parent-fixed'),
}));

describe('useSubtaskParentMapping', () => {
  function setup(overrides?: {
    issueTypes?: Array<{ id: string; name: string; subtask?: boolean }>;
    selectedIssueType?: string;
    customFields?: CustomFieldMapping[];
    parentIssues?: Array<{ key?: string; summary?: string }>;
  }) {
    const issueTypes = ref((overrides?.issueTypes || []) as any);
    const selectedIssueType = ref(overrides?.selectedIssueType || '');
    const customFields = ref((overrides?.customFields || []) as any);
    const parentIssues = ref((overrides?.parentIssues || []) as any);

    const composable = useSubtaskParentMapping({
      issueTypes,
      selectedIssueType,
      customFields,
      parentIssues,
    });

    return { issueTypes, selectedIssueType, customFields, parentIssues, ...composable };
  }

  it('detects subtask by explicit subtask flag and shows required error when parent is missing', () => {
    const ctx = setup({
      issueTypes: [{ id: 'SUB', name: 'Sub-task', subtask: true }],
      selectedIssueType: 'SUB',
    });

    expect(ctx.isSubtaskIssueType.value).toBe(true);
    expect(ctx.showParentRequiredError.value).toBe(true);
  });

  it('detects subtask by name normalization and handles non-subtask cases', () => {
    const byName = setup({
      issueTypes: [{ id: 'S1', name: ' Sub Task ' }],
      selectedIssueType: 'S1',
    });
    expect(byName.isSubtaskIssueType.value).toBe(true);

    const byDash = setup({
      issueTypes: [{ id: 'S2', name: 'Sub-task' }],
      selectedIssueType: 'S2',
    });
    expect(byDash.isSubtaskIssueType.value).toBe(true);

    const explicitFalseFlag = setup({
      issueTypes: [{ id: 'S3', name: 'Sub-task', subtask: false }],
      selectedIssueType: 'S3',
    });
    expect(explicitFalseFlag.isSubtaskIssueType.value).toBe(false);

    const noSelection = setup({
      issueTypes: [{ id: 'B1', name: 'Bug', subtask: false }],
      selectedIssueType: '',
    });
    expect(noSelection.isSubtaskIssueType.value).toBe(false);

    const missingType = setup({
      issueTypes: [{ id: 'B1', name: 'Bug', subtask: false }],
      selectedIssueType: 'UNKNOWN',
    });
    expect(missingType.isSubtaskIssueType.value).toBe(false);
  });

  it('builds parent field options with summary and filters invalid keys', () => {
    const ctx = setup({
      parentIssues: [
        { key: 'SCRUM-1', summary: 'First parent' },
        { key: 'SCRUM-2', summary: '   ' },
        { key: '   ', summary: 'Ignored' },
        {},
      ],
    });

    expect(ctx.parentFieldOptions.value).toEqual([
      { value: 'SCRUM-1', label: 'SCRUM-1 - First parent' },
      { value: 'SCRUM-2', label: 'SCRUM-2' },
    ]);
  });

  it('upserts parent custom field and updates existing one', () => {
    const ctx = setup({
      customFields: [
        {
          _id: 'legacy-id',
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          type: 'object',
          required: true,
          valueSource: 'literal',
          value: '{"key":"OLD-1"}',
        } as any,
      ],
    });

    ctx.upsertParentCustomField('SCRUM-200');
    expect(ctx.customFields.value).toHaveLength(1);
    expect(ctx.customFields.value[0]).toMatchObject({
      _id: 'legacy-id',
      jiraFieldKey: 'parent',
      jiraFieldLabel: 'Parent',
      value: '{"key":"SCRUM-200"}',
      valueSource: 'literal',
      required: true,
      type: 'object',
    });

    ctx.upsertParentCustomField('SCRUM-201');
    expect(ctx.customFields.value[0].value).toBe('{"key":"SCRUM-201"}');
  });

  it('adds new parent mapping and removes it when placeholder is blank', () => {
    const ctx = setup({ customFields: [] });

    ctx.upsertParentCustomField('SCRUM-123');
    expect(ctx.customFields.value).toHaveLength(1);
    expect(ctx.customFields.value[0]).toMatchObject({
      _id: 'uid-parent-fixed',
      jiraFieldKey: 'parent',
      value: '{"key":"SCRUM-123"}',
    });

    ctx.upsertParentCustomField('   ');
    expect(ctx.customFields.value).toEqual([]);
  });

  it('removeParentCustomField returns change flag and supports case-insensitive key matching', () => {
    const ctx = setup({
      customFields: [
        { jiraFieldKey: 'Parent', value: '{"key":"SCRUM-1"}' } as any,
        { jiraFieldKey: 'customfield_10001', value: 'X' } as any,
      ],
    });

    expect(ctx.removeParentCustomField()).toBe(true);
    expect(ctx.customFields.value).toHaveLength(1);
    expect(ctx.customFields.value[0].jiraFieldKey).toBe('customfield_10001');

    expect(ctx.removeParentCustomField()).toBe(false);
  });

  it('syncs selected parent from valid parent json key', () => {
    const ctx = setup({
      customFields: [
        {
          jiraFieldKey: 'parent',
          value: '{"key":"SCRUM-450"}',
        } as any,
      ],
    });

    ctx.syncSelectedParentFromCustomFields();
    expect(ctx.selectedParentField.value).toBe('SCRUM-450');
    expect(ctx.showParentRequiredError.value).toBe(false);
  });

  it('syncs selected parent from regex fallback when json parsing fails', () => {
    const ctx = setup({
      customFields: [
        {
          jiraFieldKey: 'parent',
          value: '{"key":"SCRUM-777"}trailing',
        } as any,
      ],
    });

    ctx.syncSelectedParentFromCustomFields();
    expect(ctx.selectedParentField.value).toBe('SCRUM-777');
  });

  it('syncSelectedParentFromCustomFields falls back to empty string when parent is missing or invalid', () => {
    const noParent = setup({ customFields: [{ jiraFieldKey: 'cf', value: 'x' } as any] });
    noParent.syncSelectedParentFromCustomFields();
    expect(noParent.selectedParentField.value).toBe('');

    const invalidParent = setup({
      customFields: [{ jiraFieldKey: 'parent', value: '{"id":"123"}' } as any],
    });
    invalidParent.syncSelectedParentFromCustomFields();
    expect(invalidParent.selectedParentField.value).toBe('');
  });

  it('computed required error toggles as parent value changes in subtask mode', () => {
    const ctx = setup({
      issueTypes: [{ id: 'SUB', name: 'Subtask' }],
      selectedIssueType: 'SUB',
    });

    expect(ctx.isSubtaskIssueType.value).toBe(true);
    expect(ctx.showParentRequiredError.value).toBe(true);

    ctx.selectedParentField.value = 'SCRUM-999';
    expect(ctx.showParentRequiredError.value).toBe(false);
  });
});
