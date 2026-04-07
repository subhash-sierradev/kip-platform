/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';

const { mockAdminService, mockAlert } = vi.hoisted(() => ({
  mockAdminService: {
    getPolicies: vi.fn(),
    createPolicy: vi.fn(),
    updatePolicy: vi.fn(),
    deletePolicy: vi.fn(),
  },
  mockAlert: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

vi.mock('@/api/services/NotificationAdminService', () => ({
  NotificationAdminService: mockAdminService,
}));

vi.mock('@/utils/notificationUtils', () => ({
  default: mockAlert,
}));

import { useNotificationPoliciesManager } from '@/composables/useNotificationPoliciesManager';

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

describe('useNotificationPoliciesManager', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetchPolicies: success populates policies and clears loading', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    mockAdminService.getPolicies.mockResolvedValueOnce([
      { id: 'p1', name: 'Policy A' },
      { id: 'p2', name: 'Policy B' },
    ]);
    await c.fetchPolicies();
    expect(c.policies.value).toHaveLength(2);
    expect(c.loadingPolicies.value).toBe(false);
  });

  it('fetchPolicies: failure shows error and clears loading', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    mockAdminService.getPolicies.mockRejectedValueOnce(new Error('network'));
    await c.fetchPolicies();
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to load recipient policies');
    expect(c.loadingPolicies.value).toBe(false);
  });

  it('createPolicy: success prepends policy and shows success', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    c.policies.value = [{ id: 'p1', name: 'Existing' } as any];
    mockAdminService.createPolicy.mockResolvedValueOnce({ id: 'p2', name: 'New Policy' });
    const result = await c.createPolicy({ name: 'New Policy' } as any);
    expect(result).toBe(true);
    expect(c.policies.value[0]).toMatchObject({ id: 'p2' });
    expect(mockAlert.success).toHaveBeenCalledWith('Recipient policy created');
    expect(c.saving.value).toBe(false);
  });

  it('createPolicy: failure shows error and returns false', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    mockAdminService.createPolicy.mockRejectedValueOnce(new Error('bad'));
    const result = await c.createPolicy({ name: 'Bad' } as any);
    expect(result).toBe(false);
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to create recipient policy');
    expect(c.saving.value).toBe(false);
  });

  it('deletePolicy: success removes policy from list', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    c.policies.value = [{ id: 'p1', name: 'Keep' } as any, { id: 'p2', name: 'Remove' } as any];
    mockAdminService.deletePolicy.mockResolvedValueOnce(undefined);
    await c.deletePolicy('p2');
    expect(c.policies.value.find(p => p.id === 'p2')).toBeUndefined();
    expect(c.policies.value).toHaveLength(1);
    expect(mockAlert.success).toHaveBeenCalledWith('Recipient policy deleted');
  });

  it('deletePolicy: failure shows error and preserves list', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    c.policies.value = [{ id: 'p1', name: 'Keep' } as any];
    mockAdminService.deletePolicy.mockRejectedValueOnce(new Error('fail'));
    await c.deletePolicy('p1');
    expect(c.policies.value).toHaveLength(1);
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to delete recipient policy');
  });

  it('upsertPolicy (create branch): calls createPolicy, then fetchPolicies', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    mockAdminService.createPolicy.mockResolvedValueOnce({ id: 'p3', name: 'Created' });
    mockAdminService.getPolicies.mockResolvedValueOnce([{ id: 'p3', name: 'Created' }]);
    const result = await c.upsertPolicy({ name: 'Created' } as any, undefined);
    expect(result).toBe(true);
    expect(mockAdminService.createPolicy).toHaveBeenCalled();
    expect(mockAdminService.getPolicies).toHaveBeenCalled();
    expect(mockAlert.success).toHaveBeenCalledWith('Recipient policy created');
    expect(c.saving.value).toBe(false);
  });

  it('upsertPolicy (update branch): calls updatePolicy, splices into list, then fetchPolicies', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    c.policies.value = [{ id: 'p1', name: 'Old Name' } as any];
    mockAdminService.updatePolicy.mockResolvedValueOnce({ id: 'p1', name: 'Updated Name' });
    mockAdminService.getPolicies.mockResolvedValueOnce([{ id: 'p1', name: 'Updated Name' }]);
    const result = await c.upsertPolicy({ name: 'Updated Name' } as any, 'p1');
    expect(result).toBe(true);
    expect(mockAdminService.updatePolicy).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'p1' })
    );
    expect(mockAlert.success).toHaveBeenCalledWith('Recipient policy updated');
    expect(c.saving.value).toBe(false);
  });

  it('upsertPolicy: failure shows error and returns false', async () => {
    const c = mountComposable(() => useNotificationPoliciesManager());
    mockAdminService.createPolicy.mockRejectedValueOnce(new Error('err'));
    const result = await c.upsertPolicy({ name: 'Fail' } as any, undefined);
    expect(result).toBe(false);
    expect(mockAlert.error).toHaveBeenCalledWith('Failed to create recipient policy');
    expect(c.saving.value).toBe(false);
  });
});
