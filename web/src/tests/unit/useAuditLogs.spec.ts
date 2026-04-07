/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api', () => ({
  SettingsService: {
    listAuditLogs: vi.fn(),
    listAuditLogsByTenant: vi.fn(),
  },
  EntityType: {
    JIRA_WEBHOOK: 'JIRA_WEBHOOK',
    INTEGRATION: 'INTEGRATION',
    JIRA_WEBHOOK_EVENT: 'JIRA_WEBHOOK_EVENT',
    SITE_CONFIG: 'SITE_CONFIG',
    CACHE: 'CACHE',
  },
}));

vi.mock('@/store/auth', () => ({
  useAuthStore: vi.fn(),
}));

import { SettingsService, EntityType } from '@/api';
import { useAuthStore } from '@/store/auth';
import { useAuditLogs } from '@/composables/useAuditLogs';

const makeLog = (over: any = {}) => ({
  entityId: 'e-1',
  entityType: EntityType.JIRA_WEBHOOK,
  performedBy: 'user1',
  action: 'CREATE',
  timestamp: '2025-01-01T00:00:00Z',
  details: 'Something',
  ...over,
});

describe('useAuditLogs', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetchAuditLogs returns early when not authenticated', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: false,
      hasRole: vi.fn().mockReturnValue(false),
    });
    const logs = useAuditLogs();
    await logs.fetchAuditLogs();
    expect(SettingsService.listAuditLogs).not.toHaveBeenCalled();
    expect(logs.loading.value).toBe(false);
    expect(logs.error.value).toBeNull();
    expect(logs.auditLogs.value).toEqual([]);
  });

  it('fetchAuditLogs loads logs and toggles loading', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
    });
    (SettingsService.listAuditLogs as any).mockResolvedValue([
      makeLog({ entityId: 'a', performedBy: 'alice', action: 'CREATE' }),
      makeLog({ entityId: 'b', performedBy: 'bob', action: 'UPDATE' }),
    ]);
    const logs = useAuditLogs();

    const p = logs.fetchAuditLogs();
    expect(logs.loading.value).toBe(true);
    await p;

    expect(logs.loading.value).toBe(false);
    expect(logs.error.value).toBeNull();
    expect(logs.auditLogs.value.map(l => l.entityId)).toEqual(['a', 'b']);
  });

  it('fetchAuditLogs handles error and sets message', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
    });
    (SettingsService.listAuditLogs as any).mockRejectedValue(new Error('network'));
    const logs = useAuditLogs();
    await logs.fetchAuditLogs();
    expect(logs.loading.value).toBe(false);
    expect(logs.error.value).toBe('network');
    expect(logs.auditLogs.value).toEqual([]);
  });

  it('fetchAuditLogsByTenant returns early when not app_admin', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
    });
    const logs = useAuditLogs();
    await logs.fetchAuditLogsByTenant('tenant-1');
    expect(SettingsService.listAuditLogsByTenant).not.toHaveBeenCalled();
    expect(logs.loading.value).toBe(false);
    expect(logs.error.value).toBeNull();
  });

  it('fetchAuditLogsByTenant loads logs for app_admin', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockImplementation((r: string) => r === 'app_admin'),
    });
    (SettingsService.listAuditLogsByTenant as any).mockResolvedValue([
      makeLog({
        entityId: 't1',
        performedBy: 'sa',
        action: 'DELETE',
        timestamp: '2025-01-02T03:04:05Z',
      }),
    ]);
    const logs = useAuditLogs();

    const p = logs.fetchAuditLogsByTenant('tenant-1');
    expect(logs.loading.value).toBe(true);
    await p;

    expect(SettingsService.listAuditLogsByTenant).toHaveBeenCalledWith({ tenantId: 'tenant-1' });
    expect(logs.loading.value).toBe(false);
    expect(logs.error.value).toBeNull();
    expect(logs.auditLogs.value[0].entityId).toBe('t1');
  });

  it('fetchAuditLogsByTenant handles error', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockImplementation((r: string) => r === 'app_admin'),
    });
    (SettingsService.listAuditLogsByTenant as any).mockRejectedValue(new Error('bad'));
    const logs = useAuditLogs();
    await logs.fetchAuditLogsByTenant('tenant-1');
    expect(logs.loading.value).toBe(false);
    expect(logs.error.value).toBe('bad');
  });

  it('filters: entityTypes, userIds, activities reflect dataset', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
    });
    (SettingsService.listAuditLogs as any).mockResolvedValue([
      makeLog({ entityId: '1', performedBy: 'alice', action: 'CREATE' }),
      makeLog({ entityId: '2', performedBy: 'alice', action: 'UPDATE' }),
      makeLog({ entityId: '3', performedBy: 'bob', action: 'DELETE' }),
      makeLog({ entityId: '4', performedBy: 'carol', action: 'LOGIN' }),
      makeLog({ entityId: '5', performedBy: 'dave', action: 'LOGOUT', timestamp: 'invalid' }),
    ]);
    const logs = useAuditLogs();
    await logs.fetchAuditLogs();

    // entityTypes static list
    expect(logs.entityTypes.value.map(e => e.value)).toEqual([
      EntityType.JIRA_WEBHOOK,
      EntityType.INTEGRATION,
      EntityType.JIRA_WEBHOOK_EVENT,
      EntityType.SITE_CONFIG,
      EntityType.CACHE,
    ]);

    // unique users
    expect(logs.userIds.value.map(u => u.value).sort()).toEqual(['alice', 'bob', 'carol', 'dave']);

    // activities includes only present ones
    const acts = logs.activities.value.map(a => a.value).sort();
    expect(acts).toEqual(['CREATE', 'DELETE', 'LOGIN', 'LOGOUT', 'UPDATE']);
  });

  it('filteredLogs converts timestamp to ISO and adds displayName', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
    });
    (SettingsService.listAuditLogs as any).mockResolvedValue([
      makeLog({
        entityId: 'A1',
        entityType: EntityType.JIRA_WEBHOOK_EVENT,
        details: 'webhookName=My Hook',
        timestamp: '2025-01-03T05:06:07Z',
      }),
      makeLog({
        entityId: 'B2',
        entityType: EntityType.JIRA_WEBHOOK_EVENT,
        details: 'no name here',
        timestamp: '2025-01-03T05:06:07Z',
      }),
      makeLog({
        entityId: 'C3',
        entityType: EntityType.JIRA_WEBHOOK,
        details: 'Other details',
        timestamp: '2025-01-03T05:06:07Z',
      }),
      makeLog({ entityId: 'D4', timestamp: 'invalid' }),
    ]);
    const logs = useAuditLogs();
    await logs.fetchAuditLogs();

    const out = logs.filteredLogs.value;
    // invalid timestamp filtered out
    expect(out.map(l => l.entityId)).toEqual(['A1', 'B2', 'C3']);
    // ISO conversion
    expect(out[0].timestamp.endsWith('Z')).toBe(true);
    // displayName rules
    expect(out[0].displayName).toBe('My Hook');
    expect(out[1].displayName).toBe('B2'); // fallback to entityId
    expect(out[2].displayName).toBe('Other details');
  });

  it('filtering by selections and clearFilters resets them', async () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
    });
    (SettingsService.listAuditLogs as any).mockResolvedValue([
      makeLog({
        entityId: 'A1',
        entityType: EntityType.JIRA_WEBHOOK,
        performedBy: 'alice',
        action: 'CREATE',
      }),
      makeLog({
        entityId: 'B2',
        entityType: EntityType.INTEGRATION,
        performedBy: 'bob',
        action: 'UPDATE',
      }),
    ]);
    const logs = useAuditLogs();
    await logs.fetchAuditLogs();

    logs.selectedEntityType.value = EntityType.JIRA_WEBHOOK;
    logs.selectedUserId.value = 'alice';
    logs.selectedActivity.value = 'CREATE';
    expect(logs.filteredLogs.value.map(l => l.entityId)).toEqual(['A1']);

    logs.clearFilters();
    expect(logs.selectedEntityType.value).toBeNull();
    expect(logs.selectedUserId.value).toBeNull();
    expect(logs.selectedActivity.value).toBeNull();
    expect(logs.filteredLogs.value.map(l => l.entityId)).toEqual(['A1', 'B2']);
  });

  it('format helpers return empty string for undefined', () => {
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
    });
    const logs = useAuditLogs();
    expect(logs.formatEntityType(undefined)).toBe('');
    expect(logs.formatEntityType(EntityType.CACHE)).toBe(EntityType.CACHE);
    expect(logs.formatActivity(undefined)).toBe('');
    expect(logs.formatActivity('CREATE')).toBe('CREATE');
  });
});
