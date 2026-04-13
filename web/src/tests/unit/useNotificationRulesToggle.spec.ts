import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick, ref } from 'vue';

import { useNotificationRulesToggle } from '@/composables/useNotificationRulesToggle';

describe('useNotificationRulesToggle', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('opens toggle dialog and cancels with loading cleanup', () => {
    const toggleRule = vi.fn();
    const rules = ref([{ id: 'r1', isEnabled: true }] as any);
    const c = useNotificationRulesToggle(toggleRule, rules);

    c.handleToggleClick('r1');
    expect(c.rowToggleLoading.value.r1).toBe(true);
    expect(c.toggleDialogOpen.value).toBe(true);
    expect(c.togglePendingAction.value).toBe('disable');

    c.handleToggleCancel();
    expect(c.rowToggleLoading.value.r1).toBe(false);
    expect(c.toggleDialogOpen.value).toBe(false);
  });

  it('guards duplicate click while row loading and returns default false for unknown rule', () => {
    const toggleRule = vi.fn();
    const rules = ref([{ id: 'r1', isEnabled: false }] as any);
    const c = useNotificationRulesToggle(toggleRule, rules);

    expect(c.getRuleEnabled('missing')).toBe(false);

    c.rowToggleLoading.value.r1 = true;
    c.handleToggleClick('r1');
    expect(c.toggleDialogOpen.value).toBe(false);
  });

  it('confirms toggle, resets state, and repaints grid on success', async () => {
    const toggleRule = vi.fn().mockResolvedValue(true);
    const rules = ref([{ id: 'r1', isEnabled: false }] as any);
    const c = useNotificationRulesToggle(toggleRule, rules);

    const repaint = vi.fn();
    c.rulesGridRef.value = { instance: { repaint } } as any;

    c.handleToggleClick('r1');
    await c.handleToggleConfirm();
    await nextTick();

    expect(toggleRule).toHaveBeenCalledWith('r1');
    expect(c.rowToggleLoading.value.r1).toBe(false);
    expect(repaint).toHaveBeenCalled();
    expect(c.toggleDialogOpen.value).toBe(false);
  });

  it('returns early when confirm called without pending id', async () => {
    const toggleRule = vi.fn();
    const rules = ref([{ id: 'r1', isEnabled: true }] as any);
    const c = useNotificationRulesToggle(toggleRule, rules);

    c.toggleDialogOpen.value = true;
    await c.handleToggleConfirm();
    expect(toggleRule).not.toHaveBeenCalled();
  });

  it('opens the enable dialog for disabled rules and does not repaint on failed toggle', async () => {
    const toggleRule = vi.fn().mockResolvedValue(false);
    const rules = ref([{ id: 'r2', isEnabled: false }] as any);
    const c = useNotificationRulesToggle(toggleRule, rules);

    const repaint = vi.fn();
    c.rulesGridRef.value = { instance: { repaint } } as any;

    c.handleToggleClick('r2');
    expect(c.togglePendingAction.value).toBe('enable');

    await c.handleToggleConfirm();
    await nextTick();

    expect(toggleRule).toHaveBeenCalledWith('r2');
    expect(repaint).not.toHaveBeenCalled();
    expect(c.rowToggleLoading.value.r2).toBe(false);
  });

  it('cancels safely when there is no pending toggle rule id', () => {
    const toggleRule = vi.fn();
    const rules = ref([{ id: 'r1', isEnabled: true }] as any);
    const c = useNotificationRulesToggle(toggleRule, rules);

    c.handleToggleCancel();

    expect(c.toggleDialogOpen.value).toBe(false);
    expect(c.rowToggleLoading.value).toEqual({});
  });
});
