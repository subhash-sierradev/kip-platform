import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock before imports so service uses the mocked request
vi.mock('@/api/core/request', () => ({ request: vi.fn() }));

import { request as coreRequest } from '@/api/core/request';
import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';

beforeEach(() => {
  (coreRequest as any).mockReset?.();
});

describe('IntegrationConnectionService', () => {
  it('testAndCreateConnection posts to /test-connection', async () => {
    (coreRequest as any).mockResolvedValueOnce({ id: '1' });
    const body = { baseUrl: 'http://x', credentials: {} } as any;
    const res = await IntegrationConnectionService.testAndCreateConnection({
      requestBody: body,
    } as any);
    expect(res).toEqual({ id: '1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/connections/test-connection',
        body,
      })
    );
  });

  it('getAllConnections composes optional query', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await IntegrationConnectionService.getAllConnections({ serviceType: 'JIRA' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/connections',
        query: { serviceType: 'JIRA' },
      })
    );

    (coreRequest as any).mockResolvedValueOnce([]);
    await IntegrationConnectionService.getAllConnections();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/connections',
        query: undefined,
      })
    );
  });

  it('getConnectionById passes path', async () => {
    (coreRequest as any).mockResolvedValueOnce({ id: '1' });
    await IntegrationConnectionService.getConnectionById({ connectionId: '1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/connections/{connectionId}',
        path: { connectionId: '1' },
      })
    );
  });

  it('getConnectionDependents GETs /{id}/dependents', async () => {
    (coreRequest as any).mockResolvedValueOnce({ serviceType: 'JIRA', jiraWebhooks: [] });
    await IntegrationConnectionService.getConnectionDependents({ connectionId: '1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/connections/{connectionId}/dependents',
        path: { connectionId: '1' },
      })
    );
  });

  it('updateConnection PUTs with path and body', async () => {
    (coreRequest as any).mockResolvedValueOnce({ id: '1' });
    await IntegrationConnectionService.updateConnection({
      connectionId: '1',
      requestBody: { baseUrl: 'x' } as any,
    });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'PUT',
        url: '/integrations/connections/{connectionId}',
        path: { connectionId: '1' },
        body: { baseUrl: 'x' },
      })
    );
  });

  it('deleteConnection DELETEs with path', async () => {
    (coreRequest as any).mockResolvedValueOnce(undefined);
    await IntegrationConnectionService.deleteConnection({ connectionId: '1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'DELETE',
        url: '/integrations/connections/{connectionId}',
        path: { connectionId: '1' },
      })
    );
  });

  it('testExistingConnection POSTs to /{id}/test', async () => {
    (coreRequest as any).mockResolvedValueOnce({ success: true });
    await IntegrationConnectionService.testExistingConnection({ connectionId: '1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/connections/{connectionId}/test',
        path: { connectionId: '1' },
      })
    );
  });
});
