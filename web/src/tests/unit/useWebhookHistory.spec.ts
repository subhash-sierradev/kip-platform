/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    getWebhookExecutions: vi.fn(),
  },
}));

vi.mock('@/utils/dateUtils', () => ({
  formatDate: vi.fn().mockImplementation((d: string) => `FMT:${d}`),
  parseDateOnlyBoundary: vi.fn().mockImplementation((dateValue: string, endOfDay: boolean) => {
    const [yearStr, monthStr, dayStr] = dateValue.split('-');
    const year = Number(yearStr);
    const month = Number(monthStr);
    const day = Number(dayStr);

    return endOfDay
      ? new Date(year, month - 1, day, 23, 59, 59, 999)
      : new Date(year, month - 1, day, 0, 0, 0, 0);
  }),
}));

import { JiraWebhookService, type WebhookExecution } from '@/api/services/JiraWebhookService';
import { useWebhookHistory } from '@/composables/useWebhookHistory';
import { formatDate, parseDateOnlyBoundary } from '@/utils/dateUtils';

const makeExec = (over: Partial<WebhookExecution>): WebhookExecution => ({
  id: 'e1',
  tenantId: 't1',
  triggeredBy: 'u1',
  triggeredAt: '2025-01-01T10:00:00Z',
  webhookId: 'w1',
  incomingPayload: '{}',
  transformedPayload: '{}',
  responseStatusCode: 200,
  responseBody: '{}',
  status: 'success',
  retryAttempt: 0,
  originalEventId: 'evt-1',
  ...over,
});

describe('useWebhookHistory', () => {
  let originalTz: string | undefined;

  beforeEach(() => {
    originalTz = process.env.TZ;
    process.env.TZ = 'UTC';
    vi.clearAllMocks();
  });

  afterEach(() => {
    process.env.TZ = originalTz;
  });

  it('fetchHistory loads executions and toggles loading', async () => {
    (JiraWebhookService.getWebhookExecutions as any).mockResolvedValue([
      makeExec({ id: 'a', triggeredAt: '2025-01-01T10:00:00Z' }),
      makeExec({ id: 'b', triggeredAt: '2025-01-01T12:00:00Z' }),
    ]);

    const h = useWebhookHistory('webhook-1');
    expect(h.loading.value).toBe(false);

    const p = h.fetchHistory();
    expect(h.loading.value).toBe(true);
    await p;

    expect(h.loading.value).toBe(false);
    expect(h.executionHistory.value).toHaveLength(2);
  });

  it('fetchHistory handles errors and sets empty list', async () => {
    (JiraWebhookService.getWebhookExecutions as any).mockRejectedValue(new Error('boom'));
    const h = useWebhookHistory('webhook-1');
    await h.fetchHistory();
    expect(h.loading.value).toBe(false);
    expect(h.executionHistory.value).toEqual([]);
  });

  it('filters by status and date ranges, sorts and paginates', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', status: 'success', triggeredAt: '2025-01-02T10:00:00Z' }),
      makeExec({ id: '2', status: 'failed', triggeredAt: '2025-01-01T09:00:00Z' }),
      makeExec({ id: '3', status: 'success', triggeredAt: '2025-01-03T08:00:00Z' }),
    ];

    // Status filter (sorted by triggeredAt desc by default)
    h.statusFilter.value = 'success';
    expect(h.filteredHistory.value.map(e => e.id)).toEqual(['3', '1']);

    // Date from
    h.dateFromFilter.value = '2025-01-03';
    expect(h.filteredHistory.value.map(e => e.id)).toEqual(['3']);

    // Date to (with end-of-day inclusion)
    h.dateFromFilter.value = '';
    h.statusFilter.value = '';
    h.dateToFilter.value = '2025-01-01';
    expect(h.filteredHistory.value.map(e => e.id)).toEqual(['2']);

    // Sorting by triggeredAt desc (default)
    h.statusFilter.value = '';
    h.dateToFilter.value = '';
    h.sortField.value = 'triggeredAt';
    h.sortOrder.value = 'desc';
    expect(h.filteredHistory.value.map(e => e.id)).toEqual(['3', '1', '2']);

    // asc
    h.sortOrder.value = 'asc';
    expect(h.filteredHistory.value.map(e => e.id)).toEqual(['2', '1', '3']);

    // Pagination (recordsPerPage = 1)
    h.recordsPerPage.value = 1;
    h.currentPage.value = 1;
    expect(h.paginatedHistory.value.map(e => e.id)).toEqual(['2']);
    h.currentPage.value = 2;
    expect(h.paginatedHistory.value.map(e => e.id)).toEqual(['1']);
    h.currentPage.value = 3;
    expect(h.paginatedHistory.value.map(e => e.id)).toEqual(['3']);

    // Totals
    expect(h.totalRecords.value).toBe(3);
    expect(h.totalPages.value).toBe(3);
  });

  it('resetAll clears filters and resets pagination', () => {
    const h = useWebhookHistory('webhook-1');
    h.statusFilter.value = 'failed';
    h.dateFromFilter.value = '2025-01-01';
    h.dateToFilter.value = '2025-01-02';
    h.currentPage.value = 3;
    h.resetAll();
    expect(h.statusFilter.value).toBe('');
    expect(h.dateFromFilter.value).toBe('');
    expect(h.dateToFilter.value).toBe('');
    expect(h.currentPage.value).toBe(1);
  });

  it('formatExecutionDate uses formatDate with seconds', () => {
    const h = useWebhookHistory('webhook-1');
    const out = h.formatExecutionDate('2025-01-01T00:00:00Z');
    expect(formatDate).toHaveBeenCalledWith('2025-01-01T00:00:00Z', {
      includeTime: true,
      includeSeconds: true,
    });
    expect(out).toBe('FMT:2025-01-01T00:00:00Z');
  });

  it('refreshHistory calls fetchHistory', async () => {
    (JiraWebhookService.getWebhookExecutions as any).mockResolvedValue([
      makeExec({ id: 'refresh' }),
    ]);

    const h = useWebhookHistory('webhook-1');
    await h.refreshHistory();

    expect(h.executionHistory.value).toHaveLength(1);
    expect(h.executionHistory.value[0].id).toBe('refresh');
  });

  it('does not fetch history when webhookId is empty', async () => {
    (JiraWebhookService.getWebhookExecutions as any).mockClear();

    const h = useWebhookHistory('');
    await h.fetchHistory();

    expect(JiraWebhookService.getWebhookExecutions).not.toHaveBeenCalled();
  });

  it('sorts by string fields (status)', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', status: 'pending' }),
      makeExec({ id: '2', status: 'failed' }),
      makeExec({ id: '3', status: 'success' }),
    ];

    h.sortField.value = 'status';
    h.sortOrder.value = 'asc';
    expect(h.filteredHistory.value.map(e => e.status)).toEqual(['failed', 'pending', 'success']);

    h.sortOrder.value = 'desc';
    expect(h.filteredHistory.value.map(e => e.status)).toEqual(['success', 'pending', 'failed']);
  });

  it('sorts by number fields (responseStatusCode)', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', responseStatusCode: 500 }),
      makeExec({ id: '2', responseStatusCode: 200 }),
      makeExec({ id: '3', responseStatusCode: 404 }),
    ];

    h.sortField.value = 'responseStatusCode';
    h.sortOrder.value = 'asc';
    expect(h.filteredHistory.value.map(e => e.responseStatusCode)).toEqual([200, 404, 500]);

    h.sortOrder.value = 'desc';
    expect(h.filteredHistory.value.map(e => e.responseStatusCode)).toEqual([500, 404, 200]);
  });

  it('sorts with undefined values (triggeredAt)', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', triggeredAt: '2025-01-03T10:00:00Z' }),
      makeExec({ id: '2', triggeredAt: undefined as any }),
      makeExec({ id: '3', triggeredAt: '2025-01-01T10:00:00Z' }),
    ];

    h.sortField.value = 'triggeredAt';
    h.sortOrder.value = 'asc';
    // Undefined should sort to the end
    const sorted = h.filteredHistory.value;
    expect(sorted[0].id).toBe('3');
    expect(sorted[1].id).toBe('1');
    expect(sorted[2].id).toBe('2');
    expect(sorted[2].triggeredAt).toBeUndefined();
  });

  it('handles all undefined values in sorting', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', triggeredAt: undefined as any }),
      makeExec({ id: '2', triggeredAt: undefined as any }),
    ];

    h.sortField.value = 'triggeredAt';
    h.sortOrder.value = 'asc';
    // Should not throw, both undefined are equal
    expect(h.filteredHistory.value).toHaveLength(2);
  });

  it('converts non-string/number values to strings for comparison', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', incomingPayload: '{"a":2}' }),
      makeExec({ id: '2', incomingPayload: '{"a":1}' }),
    ];

    h.sortField.value = 'incomingPayload';
    h.sortOrder.value = 'asc';
    // Should compare as strings
    const sorted = h.filteredHistory.value.map(e => e.incomingPayload);
    expect(sorted[0]).toBe('{"a":1}');
    expect(sorted[1]).toBe('{"a":2}');
  });

  it('applies dateFrom filter correctly at start of day', () => {
    const h = useWebhookHistory('webhook-1');
    const executions = [
      makeExec({ id: '1', triggeredAt: '2025-01-01T23:59:59Z' }),
      makeExec({ id: '2', triggeredAt: '2025-01-02T00:00:00Z' }),
      makeExec({ id: '3', triggeredAt: '2025-01-02T00:00:01Z' }),
    ];
    h.executionHistory.value = executions;

    h.dateFromFilter.value = '2025-01-02';

    const fromBoundary = parseDateOnlyBoundary('2025-01-02', false);
    const expectedIds = executions
      .filter(exec => !fromBoundary || new Date(exec.triggeredAt) >= fromBoundary)
      .sort((a, b) => new Date(b.triggeredAt).getTime() - new Date(a.triggeredAt).getTime())
      .map(exec => exec.id);

    expect(h.filteredHistory.value.map(e => e.id)).toEqual(expectedIds);
  });

  it('applies dateTo filter correctly at end of day', () => {
    const h = useWebhookHistory('webhook-1');
    const executions = [
      makeExec({ id: '1', triggeredAt: '2025-01-01T23:59:59Z' }),
      makeExec({ id: '2', triggeredAt: '2025-01-02T00:00:00Z' }),
      makeExec({ id: '3', triggeredAt: '2025-01-02T23:59:59Z' }),
      makeExec({ id: '4', triggeredAt: '2025-01-03T00:00:00Z' }),
    ];
    h.executionHistory.value = executions;

    h.dateToFilter.value = '2025-01-02';

    const toBoundary = parseDateOnlyBoundary('2025-01-02', true);
    const expectedIds = executions
      .filter(exec => !toBoundary || new Date(exec.triggeredAt) <= toBoundary)
      .sort((a, b) => new Date(b.triggeredAt).getTime() - new Date(a.triggeredAt).getTime())
      .map(exec => exec.id);

    expect(h.filteredHistory.value.map(e => e.id)).toEqual(expectedIds);
  });

  it('combines status and date filters', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', status: 'success', triggeredAt: '2025-01-01T10:00:00Z' }),
      makeExec({ id: '2', status: 'failed', triggeredAt: '2025-01-02T10:00:00Z' }),
      makeExec({ id: '3', status: 'success', triggeredAt: '2025-01-03T10:00:00Z' }),
    ];

    h.statusFilter.value = 'success';
    h.dateFromFilter.value = '2025-01-02';
    expect(h.filteredHistory.value.map(e => e.id)).toEqual(['3']);
  });

  it('updateSort changes field and order', () => {
    const h = useWebhookHistory('webhook-1');

    expect(h.sortField.value).toBe('triggeredAt');
    expect(h.sortOrder.value).toBe('desc');

    h.updateSort('status', 'asc');
    expect(h.sortField.value).toBe('status');
    expect(h.sortOrder.value).toBe('asc');
  });

  it('calculates totalRecords correctly after filtering', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = [
      makeExec({ id: '1', status: 'success' }),
      makeExec({ id: '2', status: 'failed' }),
      makeExec({ id: '3', status: 'success' }),
    ];

    expect(h.totalRecords.value).toBe(3);

    h.statusFilter.value = 'success';
    expect(h.totalRecords.value).toBe(2);
  });

  it('calculates totalPages correctly', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = Array.from({ length: 25 }, (_, i) => makeExec({ id: `${i}` }));

    h.recordsPerPage.value = 10;
    expect(h.totalPages.value).toBe(3);

    h.recordsPerPage.value = 5;
    expect(h.totalPages.value).toBe(5);

    h.recordsPerPage.value = 100;
    expect(h.totalPages.value).toBe(1);
  });

  it('paginates correctly with partial last page', () => {
    const h = useWebhookHistory('webhook-1');
    h.executionHistory.value = Array.from({ length: 7 }, (_, i) => makeExec({ id: `${i}` }));

    h.recordsPerPage.value = 3;
    h.currentPage.value = 1;
    expect(h.paginatedHistory.value).toHaveLength(3);

    h.currentPage.value = 2;
    expect(h.paginatedHistory.value).toHaveLength(3);

    h.currentPage.value = 3;
    expect(h.paginatedHistory.value).toHaveLength(1);
  });
});
