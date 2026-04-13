/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, nextTick, reactive } from 'vue';
import { mount } from '@vue/test-utils';

const { mockAlert, mockAdminService, mockUserService } = vi.hoisted(() => ({
  mockAlert: {
    success: vi.fn(),
    error: vi.fn(),
  },
  mockAdminService: {
    getEvents: vi.fn(),
    createEvent: vi.fn(),
    deactivateEvent: vi.fn(),
    getPolicies: vi.fn(),
    createPolicy: vi.fn(),
    updatePolicy: vi.fn(),
    deletePolicy: vi.fn(),
    getRules: vi.fn(),
    createRule: vi.fn(),
    updateRule: vi.fn(),
    deleteRule: vi.fn(),
    toggleRule: vi.fn(),
    getTemplates: vi.fn(),
    createRulesBatch: vi.fn(),
  },
  mockUserService: {
    getUsers: vi.fn(),
  },
}));

const authState = reactive({ isAuthenticated: true });

vi.mock('@/utils/notificationUtils', () => ({
  default: mockAlert,
}));

vi.mock('@/api/services/NotificationAdminService', () => ({
  NotificationAdminService: mockAdminService,
}));

vi.mock('@/api/services/UserService', () => ({
  UserService: mockUserService,
}));

vi.mock('@/store/auth', () => ({
  useAuthStore: () => authState,
}));

import { useNotificationAdmin } from '@/composables/useNotificationAdmin';
import { useNotificationEvents } from '@/composables/useNotificationEvents';
import { useNotificationPoliciesManager } from '@/composables/useNotificationPoliciesManager';
import { useNotificationRulesManager } from '@/composables/useNotificationRulesManager';
import { useNotificationTemplatesManager } from '@/composables/useNotificationTemplatesManager';

function mountComposable<T>(factory: () => T): T {
  let exposed!: T;
  const Comp = defineComponent({
    setup() {
      exposed = factory();
      return () => null;
    },
  });
  mount(Comp);
  return exposed;
}

describe('notification composables', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.isAuthenticated = true;
  });

  it('useNotificationEvents handles fetch/create/deactivate success and failure', async () => {
    const c = useNotificationEvents();

    (mockAdminService.getEvents as any).mockResolvedValueOnce([{ id: 'e1', isActive: true }]);
    await c.fetchEvents();
    expect(c.events.value).toHaveLength(1);
    expect(c.loadingEvents.value).toBe(false);

    (mockAdminService.getEvents as any).mockRejectedValueOnce(new Error('x'));
    await c.fetchEvents();
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to load event catalog');

    (mockAdminService.createEvent as any).mockResolvedValueOnce({ id: 'e2', isActive: true });
    const ok = await c.createEvent({ eventKey: 'E2' } as any);
    expect(ok).toBe(true);
    expect(c.events.value[0].id).toBe('e2');

    (mockAdminService.createEvent as any).mockRejectedValueOnce(new Error('x'));
    const failed = await c.createEvent({ eventKey: 'E3' } as any);
    expect(failed).toBe(false);

    (mockAdminService.deactivateEvent as any).mockResolvedValueOnce(undefined);
    await c.deactivateEvent('e2');
    expect((c.events.value[0] as any).isActive).toBe(false);

    (mockAdminService.deactivateEvent as any).mockRejectedValueOnce(new Error('x'));
    await c.deactivateEvent('missing');
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to deactivate event');
  });

  it('useNotificationRulesManager handles CRUD/toggle states', async () => {
    const c = useNotificationRulesManager();

    (mockAdminService.getRules as any).mockResolvedValueOnce([{ id: 'r1', isEnabled: true }]);
    await c.fetchRules();
    expect(c.rules.value).toHaveLength(1);

    (mockAdminService.createRule as any).mockResolvedValueOnce({});
    (mockAdminService.getRules as any).mockResolvedValueOnce([
      { id: 'r1', isEnabled: true },
      { id: 'r2', isEnabled: false },
    ]);
    expect(await c.createRule({ eventId: 'e1' } as any)).toBe(true);
    expect(c.rules.value).toHaveLength(2);

    (mockAdminService.createRule as any).mockRejectedValueOnce(new Error('x'));
    expect(await c.createRule({ eventId: 'e2' } as any)).toBe(false);

    (mockAdminService.deleteRule as any).mockResolvedValueOnce(undefined);
    await c.deleteRule('r2');
    expect(c.rules.value.find(r => r.id === 'r2')).toBeUndefined();

    (mockAdminService.toggleRule as any).mockResolvedValueOnce({ id: 'r1', isEnabled: false });
    (mockAdminService.getRules as any).mockResolvedValueOnce([{ id: 'r1', isEnabled: false }]);
    expect(await c.toggleRule('r1')).toBe(true);
    expect(c.rules.value[0].isEnabled).toBe(false);
    expect(mockAlert.success).toHaveBeenCalledWith('Rule disabled');

    (mockAdminService.toggleRule as any).mockResolvedValueOnce({ id: 'r1', isEnabled: true });
    (mockAdminService.getRules as any).mockResolvedValueOnce([{ id: 'r1', isEnabled: true }]);
    expect(await c.toggleRule('r1')).toBe(true);
    expect(mockAlert.success).toHaveBeenCalledWith('Rule enabled');

    (mockAdminService.toggleRule as any).mockRejectedValueOnce(new Error('x'));
    expect(await c.toggleRule('r1')).toBe(false);

    // updateRule: success
    (mockAdminService.updateRule as any).mockResolvedValueOnce(undefined);
    (mockAdminService.getRules as any).mockResolvedValueOnce([{ id: 'r1', isEnabled: false }]);
    expect(await c.updateRule('r1', 'ERROR')).toBe(true);
    expect(mockAdminService.updateRule).toHaveBeenCalled();

    // updateRule: failure
    (mockAdminService.updateRule as any).mockRejectedValueOnce(new Error('x'));
    expect(await c.updateRule('r1', 'INFO')).toBe(false);
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to update notification rule');
  });

  it('useNotificationTemplatesManager handles fetch success and failure', async () => {
    const c = useNotificationTemplatesManager();

    (mockAdminService.getTemplates as any).mockResolvedValueOnce([{ id: 't1' }]);
    await c.fetchTemplates();
    expect(c.templates.value).toHaveLength(1);
    expect(c.loadingTemplates.value).toBe(false);

    (mockAdminService.getTemplates as any).mockRejectedValueOnce(new Error('x'));
    await c.fetchTemplates();
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to load notification templates');
  });

  it('useNotificationAdmin loads on mount when authenticated and supports batch rule creation', async () => {
    authState.isAuthenticated = true;

    (mockAdminService.getEvents as any).mockResolvedValue([]);
    (mockAdminService.getRules as any).mockResolvedValue([]);
    (mockAdminService.getTemplates as any).mockResolvedValue([]);
    (mockAdminService.getPolicies as any).mockResolvedValue([]);
    (mockAdminService.createRulesBatch as any).mockResolvedValue(undefined);
    (mockUserService.getUsers as any).mockResolvedValue([{ id: 'u1' }]);

    const c = mountComposable(() => useNotificationAdmin());
    await nextTick();

    expect(mockAdminService.getEvents).toHaveBeenCalled();
    expect(mockUserService.getUsers).not.toHaveBeenCalled();

    await c.createAllRemainingRules(
      [{ event: { id: 'e1' } as any, severity: 'INFO' }],
      'SELECTED_USERS',
      ['u1']
    );

    expect(mockAdminService.createRulesBatch).toHaveBeenCalledWith(
      expect.objectContaining({
        requestBody: expect.objectContaining({ recipientType: 'SELECTED_USERS', userIds: ['u1'] }),
      })
    );

    (mockUserService.getUsers as any).mockRejectedValueOnce(new Error('x'));
    await c.fetchUsers();
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to load users');
  });

  it('useNotificationPoliciesManager handles fetch/create/delete/upsert', async () => {
    const c = useNotificationPoliciesManager();

    // fetchPolicies: success
    (mockAdminService.getPolicies as any).mockResolvedValueOnce([{ id: 'p1', name: 'Policy A' }]);
    await c.fetchPolicies();
    expect(c.policies.value).toHaveLength(1);
    expect(c.loadingPolicies.value).toBe(false);

    // fetchPolicies: failure
    (mockAdminService.getPolicies as any).mockRejectedValueOnce(new Error('fail'));
    await c.fetchPolicies();
    expect(mockAlert.error).toHaveBeenCalled();

    // createPolicy: success (prepends to list)
    (mockAdminService.getPolicies as any).mockResolvedValue([{ id: 'p1', name: 'Policy A' }]);
    (mockAdminService.createPolicy as any).mockResolvedValueOnce({ id: 'p2', name: 'Policy B' });
    expect(await c.createPolicy({ name: 'Policy B' } as any)).toBe(true);
    expect(mockAdminService.createPolicy).toHaveBeenCalled();

    // createPolicy: failure
    (mockAdminService.createPolicy as any).mockRejectedValueOnce(new Error('fail'));
    expect(await c.createPolicy({ name: 'Bad' } as any)).toBe(false);

    // deletePolicy: success (removes from list)
    (mockAdminService.getPolicies as any).mockResolvedValue([]);
    c.policies.value = [{ id: 'p1', name: 'Policy A' } as any];
    (mockAdminService.deletePolicy as any).mockResolvedValueOnce(undefined);
    await c.deletePolicy('p1');
    expect(c.policies.value.find(p => p.id === 'p1')).toBeUndefined();

    // deletePolicy: failure
    c.policies.value = [{ id: 'p2', name: 'Policy B' } as any];
    (mockAdminService.deletePolicy as any).mockRejectedValueOnce(new Error('fail'));
    await c.deletePolicy('p2');
    expect(mockAlert.error).toHaveBeenCalled();

    // upsertPolicy: create branch (no existingId)
    (mockAdminService.createPolicy as any).mockResolvedValueOnce({ id: 'p3', name: 'New' });
    (mockAdminService.getPolicies as any).mockResolvedValueOnce([{ id: 'p3', name: 'New' }]);
    await c.upsertPolicy({ name: 'New' } as any, undefined);
    expect(mockAdminService.createPolicy).toHaveBeenCalled();

    // upsertPolicy: update branch (with existingId)
    c.policies.value = [{ id: 'p3', name: 'New' } as any];
    (mockAdminService.updatePolicy as any).mockResolvedValueOnce({ id: 'p3', name: 'Updated' });
    (mockAdminService.getPolicies as any).mockResolvedValueOnce([{ id: 'p3', name: 'Updated' }]);
    await c.upsertPolicy({ name: 'Updated' } as any, 'p3');
    expect(mockAdminService.updatePolicy).toHaveBeenCalled();
  });

  it('useNotificationAdmin waits for auth and then loads once', async () => {
    authState.isAuthenticated = false;

    (mockAdminService.getEvents as any).mockResolvedValue([]);
    (mockAdminService.getRules as any).mockResolvedValue([]);
    (mockAdminService.getTemplates as any).mockResolvedValue([]);
    (mockAdminService.getPolicies as any).mockResolvedValue([]);
    (mockUserService.getUsers as any).mockResolvedValue([]);

    mountComposable(() => useNotificationAdmin());
    expect(mockAdminService.getEvents).not.toHaveBeenCalled();

    authState.isAuthenticated = true;
    await nextTick();
    await nextTick();

    expect(mockAdminService.getEvents).toHaveBeenCalledTimes(1);
  });

  it('useNotificationAdmin creates batch without userIds for non-selected recipients', async () => {
    authState.isAuthenticated = true;
    (mockAdminService.getEvents as any).mockResolvedValue([]);
    (mockAdminService.getRules as any).mockResolvedValue([]);
    (mockAdminService.getTemplates as any).mockResolvedValue([]);
    (mockAdminService.getPolicies as any).mockResolvedValue([]);
    (mockAdminService.createRulesBatch as any).mockResolvedValue(undefined);

    const c = mountComposable(() => useNotificationAdmin());
    await nextTick();

    await c.createAllRemainingRules(
      [{ event: { id: 'e2' } as any, severity: 'ERROR' }],
      'ALL_USERS'
    );

    expect(mockAdminService.createRulesBatch).toHaveBeenCalledWith(
      expect.objectContaining({
        requestBody: expect.objectContaining({
          recipientType: 'ALL_USERS',
          rules: [{ eventId: 'e2', severity: 'ERROR', isEnabled: true }],
        }),
      })
    );
    const callArg = (mockAdminService.createRulesBatch as any).mock.calls[0][0];
    expect(callArg.requestBody.userIds).toBeUndefined();
  });

  it('useNotificationAdmin omits userIds when SELECTED_USERS has empty list', async () => {
    authState.isAuthenticated = true;
    (mockAdminService.getEvents as any).mockResolvedValue([]);
    (mockAdminService.getRules as any).mockResolvedValue([]);
    (mockAdminService.getTemplates as any).mockResolvedValue([]);
    (mockAdminService.getPolicies as any).mockResolvedValue([]);
    (mockAdminService.createRulesBatch as any).mockResolvedValue(undefined);

    const c = mountComposable(() => useNotificationAdmin());
    await nextTick();

    await c.createAllRemainingRules(
      [{ event: { id: 'e3' } as any, severity: 'INFO' }],
      'SELECTED_USERS',
      []
    );

    const callArg = (mockAdminService.createRulesBatch as any).mock.calls[0][0];
    expect(callArg.requestBody.recipientType).toBe('SELECTED_USERS');
    expect(callArg.requestBody.userIds).toBeUndefined();
  });

  it('useNotificationAdmin handles batch create failure and still refreshes managers', async () => {
    authState.isAuthenticated = true;
    (mockAdminService.getEvents as any).mockResolvedValue([]);
    (mockAdminService.getRules as any).mockResolvedValue([]);
    (mockAdminService.getTemplates as any).mockResolvedValue([]);
    (mockAdminService.getPolicies as any).mockResolvedValue([]);
    (mockAdminService.createRulesBatch as any).mockRejectedValue(new Error('batch failure'));

    const c = mountComposable(() => useNotificationAdmin());
    await nextTick();

    await c.createAllRemainingRules(
      [{ event: { id: 'e4' } as any, severity: 'WARN' }],
      'ALL_USERS'
    );

    expect(mockAlert.error).toHaveBeenCalledWith('Failed to create notification rules');
    expect(mockAdminService.getRules).toHaveBeenCalledTimes(1);
    expect(mockAdminService.getPolicies).toHaveBeenCalledTimes(1);
  });
});
