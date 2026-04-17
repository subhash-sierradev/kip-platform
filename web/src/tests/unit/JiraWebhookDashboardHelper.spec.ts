import { describe, expect, it, vi } from 'vitest';

import {
  buildStateQuery,
  copyToClipboard,
  enableDisableAria,
  enableDisableIcon,
  enableDisableLabel,
  getAssignee,
  getIssueTypeLabel,
  getProjectLabel,
  getSortFunction,
  sortByCreatedDate,
  sortByLastModifiedDate,
  sortByLastTrigger,
  sortByName,
  sortByStatus,
} from '@/components/outbound/jirawebhooks/utils/JiraWebhookDashboardHelper';
import type { JiraWebhook } from '@/types/JiraWebhook';

function makeWebhook(overrides: Partial<JiraWebhook> = {}): JiraWebhook {
  return {
    id: overrides.id ?? 'id-1',
    name: overrides.name ?? 'Alpha',
    webhookUrl: overrides.webhookUrl ?? 'https://example.com/hook',
    createdBy: overrides.createdBy ?? 'tester',
    createdDate: overrides.createdDate ?? '2024-01-01T00:00:00Z',
    lastModifiedDate: overrides.lastModifiedDate,
    updatedDate: overrides.updatedDate ?? undefined,
    isEnabled: overrides.isEnabled ?? true,
    jiraFieldMappings: overrides.jiraFieldMappings ?? [],
    lastEventHistory: overrides.lastEventHistory ?? undefined,
  } as JiraWebhook;
}

describe('JiraWebhookDashboardHelper', () => {
  describe('label extraction', () => {
    it('getProjectLabel returns displayLabel when present', () => {
      const w = makeWebhook({
        jiraFieldMappings: [{ jiraFieldId: 'project', displayLabel: 'Project A' } as any],
      });
      expect(getProjectLabel(w)).toBe('Project A');
    });

    it('getProjectLabel falls back to empty string when not found', () => {
      const w = makeWebhook();
      expect(getProjectLabel(w)).toBe('');
    });

    it('getIssueTypeLabel returns displayLabel when present', () => {
      const w = makeWebhook({
        jiraFieldMappings: [{ jiraFieldId: 'issuetype', displayLabel: 'Bug' } as any],
      });
      expect(getIssueTypeLabel(w)).toBe('Bug');
    });

    it('getIssueTypeLabel falls back to empty string when not found', () => {
      const w = makeWebhook();
      expect(getIssueTypeLabel(w)).toBe('');
    });

    it('getAssignee prefers displayLabel, then defaultValue, else Unassigned', () => {
      const w1 = makeWebhook({
        jiraFieldMappings: [{ jiraFieldId: 'assignee', displayLabel: 'Alice' } as any],
      });
      expect(getAssignee(w1)).toBe('Alice');

      const w2 = makeWebhook({
        jiraFieldMappings: [{ jiraFieldId: 'assignee', defaultValue: 'Bob' } as any],
      });
      expect(getAssignee(w2)).toBe('Bob');

      const w3 = makeWebhook({
        jiraFieldMappings: [{ jiraFieldId: 'assignee', defaultValue: '   ' } as any],
      });
      expect(getAssignee(w3)).toBe('Unassigned');

      const w4 = makeWebhook();
      expect(getAssignee(w4)).toBe('Unassigned');
    });
  });

  describe('sorting', () => {
    const a = makeWebhook({
      id: 'a',
      name: 'Alpha',
      createdDate: '2024-01-01T00:00:00Z',
      isEnabled: true,
      lastEventHistory: { triggeredAt: '2024-01-03T00:00:00Z', status: 'SUCCESS' } as any,
    });
    const b = makeWebhook({
      id: 'b',
      name: 'Beta',
      createdDate: '2024-01-05T00:00:00Z',
      isEnabled: false,
      lastEventHistory: { triggeredAt: '2024-01-01T12:00:00Z', status: 'SUCCESS' } as any,
    });

    it('sortByName sorts alphabetically by name', () => {
      const arr = [b, a];
      arr.sort(sortByName);
      expect(arr.map(x => x.name)).toEqual(['Alpha', 'Beta']);
    });

    it('sortByCreatedDate sorts newest first', () => {
      const arr = [a, b];
      arr.sort(sortByCreatedDate);
      expect(arr.map(x => x.id)).toEqual(['b', 'a']);
    });

    it('sortByLastModifiedDate sorts newest first', () => {
      const older = makeWebhook({ id: 'x', lastModifiedDate: '2024-01-01T00:00:00Z' });
      const newer = makeWebhook({ id: 'y', lastModifiedDate: '2024-06-01T00:00:00Z' });
      const arr = [older, newer];
      arr.sort(sortByLastModifiedDate);
      expect(arr.map(x => x.id)).toEqual(['y', 'x']);
    });

    it('sortByLastModifiedDate treats missing lastModifiedDate as oldest', () => {
      const withDate = makeWebhook({ id: 'p', lastModifiedDate: '2024-01-01T00:00:00Z' });
      const noDate = makeWebhook({ id: 'q', lastModifiedDate: undefined });
      const arr = [noDate, withDate];
      arr.sort(sortByLastModifiedDate);
      expect(arr[0].id).toBe('p');
    });

    it('sortByStatus sorts enabled first', () => {
      const arr = [b, a];
      arr.sort(sortByStatus);
      expect(arr.map(x => x.id)).toEqual(['a', 'b']);
    });

    it('sortByStatus returns 0 when both have same enabled state', () => {
      const x = makeWebhook({ id: 'x', isEnabled: true });
      const y = makeWebhook({ id: 'y', isEnabled: true });
      expect(sortByStatus(x, y)).toBe(0);
    });

    it('sortByLastTrigger sorts by lastEventHistory.triggeredAt newest first', () => {
      const arr = [b, a];
      arr.sort(sortByLastTrigger);
      expect(arr.map(x => x.id)).toEqual(['a', 'b']);
    });

    it('sortByLastTrigger handles missing lastEventHistory (falls back to 0)', () => {
      const noHistory = makeWebhook({ id: 'c', lastEventHistory: undefined });
      const withHistory = makeWebhook({
        id: 'd',
        lastEventHistory: { triggeredAt: '2024-06-01T00:00:00Z', status: 'SUCCESS' } as any,
      });
      const arr = [noHistory, withHistory];
      arr.sort(sortByLastTrigger);
      expect(arr[0].id).toBe('d');
    });
  });

  describe('getSortFunction', () => {
    it('returns sortByName for "name"', () => {
      expect(getSortFunction('name')).toBe(sortByName);
    });

    it('returns sortByCreatedDate for "createdDate"', () => {
      expect(getSortFunction('createdDate')).toBe(sortByCreatedDate);
    });

    it('returns sortByLastModifiedDate for "lastModifiedDate"', () => {
      expect(getSortFunction('lastModifiedDate')).toBe(sortByLastModifiedDate);
    });

    it('returns sortByStatus for "isEnabled"', () => {
      expect(getSortFunction('isEnabled')).toBe(sortByStatus);
    });

    it('returns sortByLastTrigger for "lastTrigger"', () => {
      expect(getSortFunction('lastTrigger')).toBe(sortByLastTrigger);
    });

    it('returns no-op comparator for unknown sort type', () => {
      const noOp = getSortFunction('unknown');
      const x = makeWebhook({ id: 'x' });
      const y = makeWebhook({ id: 'y' });
      expect(noOp(x, y)).toBe(0);
    });
  });

  describe('copyToClipboard', () => {
    it('calls clipboard writeText and invokes onSuccess callback', async () => {
      const writeMock = vi.fn().mockResolvedValue(undefined);
      Object.defineProperty(window, 'navigator', {
        value: { clipboard: { writeText: writeMock } },
        writable: true,
        configurable: true,
      });

      const onSuccess = vi.fn();
      copyToClipboard('https://example.com/hook', onSuccess);
      expect(writeMock).toHaveBeenCalledWith('https://example.com/hook');
      expect(onSuccess).toHaveBeenCalledWith('Webhook URL successfully copied to clipboard');
    });

    it('works without onSuccess callback', () => {
      const writeMock = vi.fn().mockResolvedValue(undefined);
      Object.defineProperty(window, 'navigator', {
        value: { clipboard: { writeText: writeMock } },
        writable: true,
        configurable: true,
      });
      expect(() => copyToClipboard('https://example.com/hook')).not.toThrow();
    });
  });

  describe('enableDisable helpers', () => {
    it('enableDisableLabel returns Disable for enabled webhook', () => {
      expect(enableDisableLabel(makeWebhook({ isEnabled: true }))).toBe('Disable');
      expect(enableDisableLabel(makeWebhook({ isEnabled: false }))).toBe('Enable');
    });

    it('enableDisableIcon returns correct icons', () => {
      expect(enableDisableIcon(makeWebhook({ isEnabled: true }))).toBe('dx-icon-cursorprohibition');
      expect(enableDisableIcon(makeWebhook({ isEnabled: false }))).toBe('dx-icon-video');
    });

    it('enableDisableAria returns accessible label', () => {
      expect(enableDisableAria(makeWebhook({ isEnabled: true }))).toBe('Disable webhook');
      expect(enableDisableAria(makeWebhook({ isEnabled: false }))).toBe('Enable webhook');
    });
  });

  describe('buildStateQuery', () => {
    it('returns empty object when all defaults', () => {
      const q = buildStateQuery({
        search: '',
        sortBy: '',
        viewMode: '',
        currentPage: 1,
        pageSize: 6,
      });
      expect(q).toEqual({});
    });

    it('includes non-default values', () => {
      const q = buildStateQuery({
        search: 'alpha',
        sortBy: 'name',
        viewMode: 'list',
        currentPage: 3,
        pageSize: 12,
        persistPageSize: true,
      });
      expect(q.search).toBe('alpha');
      expect(q.sort).toBe('name');
      expect(q.view).toBe('list');
      expect(q.page).toBe('3');
      expect(q.size).toBe('12');
    });

    it('includes scroll position when includeScroll is true', () => {
      const q = buildStateQuery({
        search: '',
        sortBy: '',
        viewMode: '',
        currentPage: 1,
        pageSize: 6,
        includeScroll: true,
      });
      expect(q.scroll).toBeDefined();
    });

    it('omits page size when using the responsive default', () => {
      const q = buildStateQuery({
        search: '',
        sortBy: 'createdDate',
        viewMode: 'grid',
        currentPage: 1,
        pageSize: 12,
        persistPageSize: false,
      });

      expect(q).toEqual({ sort: 'createdDate', view: 'grid' });
    });
  });
});
