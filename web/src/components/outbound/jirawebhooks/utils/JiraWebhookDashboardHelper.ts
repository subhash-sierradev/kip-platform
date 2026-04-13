import type { JiraWebhook } from '@/types/JiraWebhook';
export { debounce } from '@/utils/debounceUtils';

export function getProjectLabel(webhook: JiraWebhook): string {
  const mapping = webhook.jiraFieldMappings?.find(
    f =>
      (typeof f.jiraFieldId === 'string' && f.jiraFieldId === 'project') ||
      (typeof f.displayLabel === 'string' && f.displayLabel.toLowerCase().includes('project'))
  );
  return typeof mapping?.displayLabel === 'string' ? mapping.displayLabel : '';
}

export function getIssueTypeLabel(webhook: JiraWebhook): string {
  const mapping = webhook.jiraFieldMappings?.find(
    f =>
      (typeof f.jiraFieldId === 'string' && f.jiraFieldId === 'issuetype') ||
      (typeof f.displayLabel === 'string' && f.displayLabel.toLowerCase().includes('issue type'))
  );
  return typeof mapping?.displayLabel === 'string' ? mapping.displayLabel : '';
}

export function getAssignee(webhook: JiraWebhook): string {
  const assigneeMapping = webhook.jiraFieldMappings?.find(
    mapping =>
      (typeof mapping.jiraFieldId === 'string' && mapping.jiraFieldId === 'assignee') ||
      (typeof mapping.jiraFieldName === 'string' &&
        mapping.jiraFieldName.toLowerCase() === 'assignee')
  );
  if (typeof assigneeMapping?.displayLabel === 'string') {
    return assigneeMapping.displayLabel;
  }
  if (
    typeof assigneeMapping?.defaultValue === 'string' &&
    assigneeMapping.defaultValue.trim() !== ''
  ) {
    return assigneeMapping.defaultValue;
  }
  return 'Unassigned';
}

// Sorting helpers - only the ones we need
export const sortByName = (a: JiraWebhook, b: JiraWebhook): number => a.name.localeCompare(b.name);
export const sortByCreatedDate = (a: JiraWebhook, b: JiraWebhook): number =>
  new Date(b.createdDate).getTime() - new Date(a.createdDate).getTime();
export const sortByStatus = (a: JiraWebhook, b: JiraWebhook): number => {
  // Sort enabled webhooks first (true > false)
  if (a.isEnabled === b.isEnabled) return 0;
  return a.isEnabled ? -1 : 1;
};
export const sortByLastTrigger = (a: JiraWebhook, b: JiraWebhook): number => {
  const aT =
    typeof a.lastEventHistory?.triggeredAt === 'string'
      ? new Date(a.lastEventHistory.triggeredAt).getTime()
      : 0;
  const bT =
    typeof b.lastEventHistory?.triggeredAt === 'string'
      ? new Date(b.lastEventHistory.triggeredAt).getTime()
      : 0;
  return bT - aT;
};

export const getSortFunction = (sortType: string): ((a: JiraWebhook, b: JiraWebhook) => number) => {
  switch (sortType) {
    case 'name':
      return sortByName;
    case 'createdDate':
      return sortByCreatedDate;
    case 'isEnabled':
      return sortByStatus;
    case 'lastTrigger':
      return sortByLastTrigger;
    default:
      return () => 0;
  }
};

// Clipboard helper
export function copyToClipboard(text: string, onSuccess?: (msg: string) => void): void {
  if (typeof window !== 'undefined' && window.navigator && window.navigator.clipboard) {
    window.navigator.clipboard.writeText(text);
    onSuccess?.('Webhook URL successfully copied to clipboard');
  }
}

// Enable/Disable helpers
export function enableDisableLabel(w: JiraWebhook): string {
  return w.isEnabled ? 'Disable' : 'Enable';
}
export function enableDisableIcon(w: JiraWebhook): string {
  return w.isEnabled ? 'dx-icon-cursorprohibition' : 'dx-icon-video';
}
export function enableDisableAria(w: JiraWebhook): string {
  return `${w.isEnabled ? 'Disable' : 'Enable'} webhook`;
}

// Build state query for router
export function buildStateQuery(params: {
  search: string;
  sortBy: string;
  viewMode: string;
  currentPage: number;
  pageSize: number;
  persistPageSize?: boolean;
  includeScroll?: boolean;
}): Record<string, string | undefined> {
  const { search, sortBy, viewMode, currentPage, pageSize, persistPageSize, includeScroll } =
    params;
  const query: Record<string, string | undefined> = {};
  if (search) query.search = search;
  if (sortBy) query.sort = sortBy;
  if (viewMode) query.view = viewMode;
  if (currentPage !== 1) query.page = String(currentPage);
  if (persistPageSize) query.size = String(pageSize);
  if (includeScroll) query.scroll = String(window.scrollY || 0);
  return query;
}
