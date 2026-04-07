import { beforeEach, describe, expect, it, vi } from 'vitest';

import { request as coreRequest } from '@/api/core/request';
import { SettingsService } from '@/api/services/SettingsService';

vi.mock('@/api/core/request', () => ({ request: vi.fn() }));

beforeEach(() => {
  (coreRequest as any).mockReset?.();
});

describe('SettingsService', () => {
  it('clearAllCaches DELETE /management/caches', async () => {
    (coreRequest as any).mockResolvedValueOnce('ok');
    const res = await SettingsService.clearAllCaches();
    expect(res).toBe('ok');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'DELETE', url: '/management/caches' })
    );
  });

  it('clearCacheByName DELETE /management/caches/{cacheName}', async () => {
    (coreRequest as any).mockResolvedValueOnce('ok');
    const res = await SettingsService.clearCacheByName({ cacheName: 'x' });
    expect(res).toBe('ok');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'DELETE',
        url: '/management/caches/{cacheName}',
        path: { cacheName: 'x' },
      })
    );
  });

  it('getAllCacheStats GET /management/caches/stats', async () => {
    (coreRequest as any).mockResolvedValueOnce({});
    await SettingsService.getAllCacheStats();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/caches/stats' })
    );
  });

  it('listSiteConfigs GET /management/site-configs', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await SettingsService.listSiteConfigs();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/site-configs' })
    );
  });

  it('listAuditLogs GET /management/audits/logs', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await SettingsService.listAuditLogs();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/audits/logs' })
    );
  });

  it('listAuditLogsByTenant GET /management/audits/logs/tenants/{tenantId}', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await SettingsService.listAuditLogsByTenant({ tenantId: 't1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/management/audits/logs/tenants/{tenantId}',
        path: { tenantId: 't1' },
      })
    );
  });

  it('listTenantsWithAuditLogs GET /management/audits/tenants', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await SettingsService.listTenantsWithAuditLogs();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/audits/tenants' })
    );
  });

  it('listAllTenants GET /management/tenants', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await SettingsService.listAllTenants();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/tenants' })
    );
  });

  it('findAllConnections GET /management/connections/jira and by tenant path', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await SettingsService.findAllConnections();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/connections/jira' })
    );

    (coreRequest as any).mockResolvedValueOnce([]);
    await SettingsService.findAllConnectionsByTenant({ tenantId: 't1' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/management/connections/jira/tenants/{tenantId}',
        path: { tenantId: 't1' },
      })
    );
  });
});
