/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    getWebhookExecutions: vi.fn(),
  },
}));

import { JiraWebhookService, type WebhookExecution } from '@/api/services/JiraWebhookService';
import { useWebhookHistoryData } from '@/composables/useWebhookHistoryData';

const makeExec = (over: Partial<WebhookExecution> = {}): WebhookExecution => ({
  id: 'e1',
  tenantId: 't1',
  triggeredBy: 'u1',
  triggeredAt: '2025-01-01T10:00:00Z',
  webhookId: 'w1',
  incomingPayload: '{}',
  transformedPayload: '{}',
  responseStatusCode: 200,
  responseBody: '{}',
  status: 'SUCCESS',
  retryAttempt: 0,
  originalEventId: 'evt-1',
  ...over,
});

describe('useWebhookHistoryData', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns early when webhookId is missing', async () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const d = useWebhookHistoryData('');
    await d.fetchWebhookHistory();
    expect(JiraWebhookService.getWebhookExecutions).not.toHaveBeenCalled();
    expect(d.loading.value).toBe(false);
    expect(d.executionHistory.value).toEqual([]);
    expect(spy).toHaveBeenCalled();
    spy.mockRestore();
  });

  it('loads executions and toggles loading on success', async () => {
    (JiraWebhookService.getWebhookExecutions as any).mockResolvedValue([
      makeExec({ id: 'a' }),
      makeExec({ id: 'b' }),
    ]);
    const d = useWebhookHistoryData('webhook-1');
    const p = d.fetchWebhookHistory();
    expect(d.loading.value).toBe(true);
    await p;
    expect(d.loading.value).toBe(false);
    expect(d.executionHistory.value.map(e => e.id)).toEqual(['a', 'b']);
  });

  it('handles API error and leaves existing data unchanged', async () => {
    const d = useWebhookHistoryData('webhook-1');
    d.executionHistory.value = [makeExec({ id: 'existing' })];
    (JiraWebhookService.getWebhookExecutions as any).mockRejectedValue(new Error('fail'));
    await d.fetchWebhookHistory();
    expect(d.loading.value).toBe(false);
    expect(d.executionHistory.value.map(e => e.id)).toEqual(['existing']);
  });
});
