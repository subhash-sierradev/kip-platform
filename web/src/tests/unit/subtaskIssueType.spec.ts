import { describe, expect, it } from 'vitest';

import { isSubtaskIssueType } from '@/utils/subtaskIssueType';

describe('subtaskIssueType', () => {
  it('returns false for empty selection or missing type', () => {
    expect(isSubtaskIssueType([], '')).toBe(false);
    expect(isSubtaskIssueType([{ id: '1', name: 'Bug' } as any], 'UNKNOWN')).toBe(false);
  });

  it('uses explicit subtask flag when available', () => {
    expect(isSubtaskIssueType([{ id: '1', name: 'Bug', subtask: true } as any], '1')).toBe(true);
    expect(isSubtaskIssueType([{ id: '1', name: 'Sub-task', subtask: false } as any], '1')).toBe(
      false
    );
  });

  it('falls back to name normalization when flag is missing', () => {
    expect(isSubtaskIssueType([{ id: '1', name: 'Sub task' } as any], '1')).toBe(true);
    expect(isSubtaskIssueType([{ id: '1', name: 'Sub-task' } as any], '1')).toBe(true);
    expect(isSubtaskIssueType([{ id: '1', name: 'Task' } as any], '1')).toBe(false);
  });
});
