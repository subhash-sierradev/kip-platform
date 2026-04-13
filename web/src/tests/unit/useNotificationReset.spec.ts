/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';

const { mockAdminService, mockToast } = vi.hoisted(() => ({
  mockAdminService: {
    resetRulesToDefaults: vi.fn(),
  },
  mockToast: {
    showSuccess: vi.fn(),
    showError: vi.fn(),
  },
}));

vi.mock('@/api/services/NotificationAdminService', () => ({
  NotificationAdminService: mockAdminService,
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => mockToast,
}));

import { useNotificationReset } from '@/composables/useNotificationReset';

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

describe('useNotificationReset', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('resetToDefaults sets showConfirmDialog to true', async () => {
    const c = mountComposable(() => useNotificationReset());
    expect(c.showConfirmDialog.value).toBe(false);
    await c.resetToDefaults();
    expect(c.showConfirmDialog.value).toBe(true);
  });

  it('cancelReset sets showConfirmDialog to false', async () => {
    const c = mountComposable(() => useNotificationReset());
    await c.resetToDefaults();
    expect(c.showConfirmDialog.value).toBe(true);
    c.cancelReset();
    expect(c.showConfirmDialog.value).toBe(false);
  });

  it('confirmReset success: calls API, shows success toast, resets flags', async () => {
    mockAdminService.resetRulesToDefaults.mockResolvedValueOnce({ rulesRestored: 5 });
    const c = mountComposable(() => useNotificationReset());

    await c.resetToDefaults();
    await c.confirmReset();

    expect(mockAdminService.resetRulesToDefaults).toHaveBeenCalledTimes(1);
    expect(mockToast.showSuccess).toHaveBeenCalledWith(expect.stringContaining('5 rules restored'));
    expect(c.isResetting.value).toBe(false);
    expect(c.showConfirmDialog.value).toBe(false);
  });

  it('confirmReset failure: shows error toast, resets isResetting, and rethrows', async () => {
    const error = new Error('Network error');
    mockAdminService.resetRulesToDefaults.mockRejectedValueOnce(error);
    const c = mountComposable(() => useNotificationReset());

    await c.resetToDefaults();
    await expect(c.confirmReset()).rejects.toThrow('Network error');

    expect(mockToast.showError).toHaveBeenCalled();
    expect(c.isResetting.value).toBe(false);
  });

  it('confirmReset uses the generic fallback message for non-Error failures', async () => {
    mockAdminService.resetRulesToDefaults.mockRejectedValueOnce('plain failure');
    const c = mountComposable(() => useNotificationReset());

    await c.resetToDefaults();
    await expect(c.confirmReset()).rejects.toBe('plain failure');

    expect(mockToast.showError).toHaveBeenCalledWith('Error: Failed to reset notification rules');
    expect(c.isResetting.value).toBe(false);
  });
});
