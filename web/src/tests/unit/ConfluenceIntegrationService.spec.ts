import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/api/core/request', () => ({ request: vi.fn() }));

import { request as coreRequest } from '@/api/core/request';
import { ConfluenceIntegrationService } from '@/api/services/ConfluenceIntegrationService';

async function importConfluenceServiceWithAuth(authState: {
  token?: string | null;
  user?: { tenantId?: string; userId?: string } | null;
}) {
  vi.resetModules();

  const requestMock = vi.fn().mockResolvedValue([]);

  vi.doMock('@/api/core/request', () => ({ request: requestMock }));
  vi.doMock('@/config/keycloak', () => ({
    getToken: () => authState.token ?? null,
    getUserInfo: () => authState.user ?? null,
  }));

  const serviceModule = await import('@/api/services/ConfluenceIntegrationService');
  return {
    requestMock,
    ConfluenceIntegrationService: serviceModule.ConfluenceIntegrationService,
  };
}

describe('ConfluenceIntegrationService', () => {
  beforeEach(() => {
    (coreRequest as any).mockReset?.();
  });

  it('listConfluenceIntegrations uses GET /integrations/confluence', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    const result = await ConfluenceIntegrationService.listConfluenceIntegrations();
    expect(result).toEqual([]);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/integrations/confluence' })
    );
  });

  it('gets, creates, updates, and deletes integration', async () => {
    (coreRequest as any).mockResolvedValueOnce({ id: 'c1' });
    const one = await ConfluenceIntegrationService.getConfluenceIntegrationById('c1');
    expect((one as any).id).toBe('c1');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/integrations/confluence/c1' })
    );

    const createBody = { name: 'Conf', connectionId: 'x' } as any;
    (coreRequest as any).mockResolvedValueOnce({ id: 'created' });
    await ConfluenceIntegrationService.createIntegration(createBody);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/confluence',
        body: createBody,
        mediaType: 'application/json',
      })
    );

    const updateBody = { name: 'Conf2' } as any;
    (coreRequest as any).mockResolvedValueOnce(undefined);
    await ConfluenceIntegrationService.updateIntegration('c1', updateBody);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'PUT',
        url: '/integrations/confluence/c1',
        body: updateBody,
        mediaType: 'application/json',
      })
    );

    (coreRequest as any).mockResolvedValueOnce(undefined);
    await ConfluenceIntegrationService.deleteIntegration('c1');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'DELETE', url: '/integrations/confluence/c1' })
    );
  });

  it('handles executions endpoints and toggle', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    const history = await ConfluenceIntegrationService.getJobHistory('i1');
    expect(history).toEqual([]);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/integrations/confluence/i1/executions' })
    );

    (coreRequest as any).mockResolvedValueOnce(undefined);
    await ConfluenceIntegrationService.triggerJobExecution('i1');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'POST', url: '/integrations/confluence/i1/trigger' })
    );

    (coreRequest as any).mockResolvedValueOnce(true);
    const toggled = await ConfluenceIntegrationService.toggleIntegrationStatus('i1');
    expect(toggled).toBe(true);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'POST', url: '/integrations/confluence/i1/toggle' })
    );
  });

  it('fetches names/spaces/pages endpoints', async () => {
    (coreRequest as any).mockResolvedValueOnce(['one']);
    const names = await ConfluenceIntegrationService.getAllNormalizedNames();
    expect(names).toEqual(['one']);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/integrations/confluence/normalized/names' })
    );

    (coreRequest as any).mockResolvedValueOnce([{ key: 'ABC', name: 'Space' }]);
    const spaces = await ConfluenceIntegrationService.getSpacesByConnectionId('conn-1');
    expect(spaces).toHaveLength(1);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/confluence/connections/conn-1/spaces',
      })
    );

    (coreRequest as any).mockResolvedValueOnce([{ id: 'p1', title: 'Page' }]);
    const pages = await ConfluenceIntegrationService.getPagesByConnectionIdAndSpace(
      'conn-1',
      'ABC'
    );
    expect(pages).toHaveLength(1);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/confluence/connections/conn-1/spaces/ABC/pages',
      })
    );
  });

  it('adds authorization, tenant, and user headers to protected requests when auth context exists', async () => {
    const { requestMock, ConfluenceIntegrationService: DynamicService } =
      await importConfluenceServiceWithAuth({
        token: 'token-123',
        user: { tenantId: 'tenant-1', userId: 'user-7' },
      });

    await DynamicService.getJobHistory('i1');
    await DynamicService.retryJobExecution('i1', 'job-9');

    expect(requestMock).toHaveBeenNthCalledWith(
      1,
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/confluence/i1/executions',
        headers: {
          Authorization: 'Bearer token-123',
          'X-TENANT-ID': 'tenant-1',
          'X-USER-ID': 'user-7',
        },
      })
    );
    expect(requestMock).toHaveBeenNthCalledWith(
      2,
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/confluence/i1/executions/job-9/retry',
        headers: {
          Authorization: 'Bearer token-123',
          'X-TENANT-ID': 'tenant-1',
          'X-USER-ID': 'user-7',
        },
      })
    );
  });

  it('omits missing auth headers when token or user data is unavailable', async () => {
    const { requestMock, ConfluenceIntegrationService: DynamicService } =
      await importConfluenceServiceWithAuth({
        token: null,
        user: { tenantId: 'tenant-1' },
      });

    await DynamicService.getJobHistory('i2');

    expect(requestMock).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        headers: {
          'X-TENANT-ID': 'tenant-1',
        },
      })
    );

    const secondImport = await importConfluenceServiceWithAuth({
      token: undefined,
      user: null,
    });
    await secondImport.ConfluenceIntegrationService.retryJobExecution('i3', 'job-3');

    expect(secondImport.requestMock).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        headers: {},
      })
    );
  });
});
