import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock before importing modules under test
vi.mock('@/api/core/request', () => ({ request: vi.fn() }));
vi.mock('@/api/OpenAPI', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

import { request as coreRequest } from '@/api/core/request';
import { api } from '@/api/OpenAPI';
import { JiraWebhookService } from '@/api/services/JiraWebhookService';

beforeEach(() => {
  (coreRequest as any).mockReset?.();
  (api.get as any).mockReset?.();
  (api.post as any).mockReset?.();
  (api.put as any).mockReset?.();
});

describe('JiraWebhookService', () => {
  it('getWebhookById returns data or throws formatted error', async () => {
    (api.get as any).mockResolvedValueOnce({ data: { id: 'abc' } });
    const ok = await JiraWebhookService.getWebhookById('abc');
    expect(ok.id).toBe('abc');

    (api.get as any).mockRejectedValueOnce({
      response: { status: 404, data: { message: 'missing' } },
      message: 'boom',
    });
    await expect(JiraWebhookService.getWebhookById('bad')).rejects.toThrow(/missing/);

    // Test error without response data message
    (api.get as any).mockRejectedValueOnce({ message: 'network error' });
    await expect(JiraWebhookService.getWebhookById('network')).rejects.toThrow(
      /Failed to fetch webhook: network error/
    );
  });

  it('testWebhook returns success on execute, error response on failure', async () => {
    (api.post as any).mockResolvedValueOnce({ data: {} });
    const res1 = await JiraWebhookService.testWebhook('id', '{ }');
    expect(res1.success).toBe(true);
    expect(res1.responseCode).toBe(200);

    (api.post as any).mockRejectedValueOnce({
      response: { status: 500, data: { message: 'bad' } },
      message: 'x',
    });
    const res2 = await JiraWebhookService.testWebhook('id', '{ }');
    expect(res2.success).toBe(false);
    expect(res2.errorMessage).toMatch(/bad/);
    expect(res2.responseCode).toBe(500);

    // Test error without response status
    (api.post as any).mockRejectedValueOnce({ message: 'network error' });
    const res3 = await JiraWebhookService.testWebhook('id', '{ }');
    expect(res3.success).toBe(false);
    expect(res3.responseCode).toBe(500);
    expect(res3.errorMessage).toMatch(/network error/);

    // Test with default sample payload
    (api.post as any).mockResolvedValueOnce({ data: {} });
    const res4 = await JiraWebhookService.testWebhook('id');
    expect(res4.success).toBe(true);
  });

  it('getWebhookExecutions returns list or throws formatted error', async () => {
    (api.get as any).mockResolvedValueOnce({ data: [] });
    const ex = await JiraWebhookService.getWebhookExecutions('id');
    expect(ex).toEqual([]);

    (api.get as any).mockRejectedValueOnce({
      response: { status: 500, data: { message: 'fail' } },
      message: 'oops',
    });
    await expect(JiraWebhookService.getWebhookExecutions('id')).rejects.toThrow(/fail|oops/);
  });

  it('listJiraWebhooks requests GET /webhooks/jira', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    const res = await JiraWebhookService.listJiraWebhooks();
    expect(res).toEqual([]);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/webhooks/jira' })
    );
  });

  it('deleteJiraWebhook deletes by id', async () => {
    (coreRequest as any).mockResolvedValueOnce(undefined);
    await JiraWebhookService.deleteJiraWebhook('x');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'DELETE', url: '/webhooks/jira/x' })
    );
  });

  it('createWebhook posts and returns data; updateWebhook PUTs and propagates error', async () => {
    (api.post as any).mockResolvedValueOnce({ data: { name: 'w', webhookUrl: 'u' } });
    const ok = await JiraWebhookService.createWebhook({
      name: 'w',
      connectionId: 'c',
      fieldsMapping: [],
      samplePayload: '{}',
    });
    expect(ok.webhookUrl).toBe('u');

    (api.put as any).mockRejectedValueOnce({
      response: { status: 400, data: { message: 'bad' } },
      message: 'x',
    });
    await expect(
      JiraWebhookService.updateWebhook('id', {
        name: 'w',
        connectionId: 'c',
        fieldsMapping: [],
        samplePayload: '{}',
      })
    ).rejects.toThrow(/bad/);

    // Test createWebhook error handling
    (api.post as any).mockRejectedValueOnce({
      response: { status: 400, data: { message: 'validation error' } },
    });
    await expect(
      JiraWebhookService.createWebhook({
        name: 'w',
        connectionId: 'c',
        fieldsMapping: [],
        samplePayload: '{}',
      })
    ).rejects.toThrow(/validation error/);

    // Test updateWebhook error without response data
    (api.put as any).mockRejectedValueOnce({ message: 'network failure' });
    await expect(
      JiraWebhookService.updateWebhook('id', {
        name: 'w',
        connectionId: 'c',
        fieldsMapping: [],
        samplePayload: '{}',
      })
    ).rejects.toThrow(/network failure/);
  });

  it('toggleWebhookActive hits POST /webhooks/jira/{id}/active and retryWebhookEvent hits retry path', async () => {
    (coreRequest as any).mockResolvedValueOnce(true);
    const toggled = await JiraWebhookService.toggleWebhookActive({ id: 'a' });
    expect(toggled).toBe(true);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/webhooks/jira/{id}/active',
        path: { id: 'a' },
      })
    );

    (coreRequest as any).mockResolvedValueOnce(undefined);
    await JiraWebhookService.retryWebhookEvent({ eventId: 'e1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/webhooks/jira/triggers/retry/{eventId}',
        path: { eventId: 'e1' },
      })
    );
  });

  it('canRetryExecution returns true only for FAILED status', () => {
    const exec = { status: 'FAILED', retryAttempt: 1 } as any;
    expect(JiraWebhookService.canRetryExecution(exec)).toBe(true);

    const exec2 = { status: 'SUCCESS', retryAttempt: 0 } as any;
    expect(JiraWebhookService.canRetryExecution(exec2)).toBe(false);

    // Test other status values
    const execPending = { status: 'PENDING', retryAttempt: 0 } as any;
    expect(JiraWebhookService.canRetryExecution(execPending)).toBe(false);

    const execProcessing = { status: 'PROCESSING', retryAttempt: 0 } as any;
    expect(JiraWebhookService.canRetryExecution(execProcessing)).toBe(false);

    const execError = { status: 'ERROR', retryAttempt: 0 } as any;
    expect(JiraWebhookService.canRetryExecution(execError)).toBe(false);
  });

  it('getAllJiraNormalizedNames fetches list of webhook names', async () => {
    const mockNames = ['Webhook_A', 'webhook_b', 'Webhook_C'];
    (coreRequest as any).mockResolvedValueOnce(mockNames);
    const names = await JiraWebhookService.getAllJiraNormalizedNames();
    expect(names).toEqual(mockNames);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/webhooks/jira/normalized/names',
      })
    );
  });

  it('getAllJiraNormalizedNames returns empty array when no webhooks exist', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    const names = await JiraWebhookService.getAllJiraNormalizedNames();
    expect(names).toEqual([]);
  });

  it('getAllJiraNormalizedNames handles names with various formats', async () => {
    const mockNames = ['Jira Webhook', 'jira_webhook_2', 'Jira-Webhook-3', 'JIRA WEBHOOK 4'];
    (coreRequest as any).mockResolvedValueOnce(mockNames);
    const names = await JiraWebhookService.getAllJiraNormalizedNames();
    expect(names).toEqual(mockNames);
    expect(names).toHaveLength(4);
  });
});
