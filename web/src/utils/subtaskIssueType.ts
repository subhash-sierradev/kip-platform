import type { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';

function normalizeIssueTypeName(name: string | undefined): string {
  return String(name || '')
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '');
}

export function isSubtaskIssueType(
  issueTypes: JiraIssueType[],
  selectedIssueTypeId: string
): boolean {
  if (!selectedIssueTypeId) {
    return false;
  }

  const selectedType = (issueTypes || []).find(item => item.id === selectedIssueTypeId);
  if (!selectedType) {
    return false;
  }

  if (typeof selectedType.subtask === 'boolean') {
    return selectedType.subtask;
  }

  const normalizedName = normalizeIssueTypeName(selectedType.name);
  return normalizedName === 'subtask';
}
