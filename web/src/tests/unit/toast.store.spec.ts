/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('@/utils/notificationUtils', () => ({
  setGlobalToast: vi.fn(),
}));

import { useToastStore } from '@/store/toast';

describe('useToastStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.useFakeTimers();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('initializeToast wires global toast handler', async () => {
    const store = useToastStore();
    store.initializeToast();
    const notificationUtils = await import('@/utils/notificationUtils');
    const mockedSetGlobalToast = notificationUtils.setGlobalToast as unknown as ReturnType<
      typeof vi.fn
    >;
    expect(mockedSetGlobalToast).toHaveBeenCalledTimes(1);
    const handler = (mockedSetGlobalToast as any).mock.calls[0][0];
    expect(typeof handler).toBe('function');

    handler('Saved', 'success');
    expect(store.toastCount).toBe(1);
    expect(store.toasts[0].message).toBe('Saved');
  });

  it('showToast creates toast, auto-hides after duration', () => {
    const store = useToastStore();
    const id = store.showToast('Hello', 'info', { duration: 1000 });
    expect(store.toastCount).toBe(1);
    expect(store.toasts[0].id).toBe(id);

    vi.advanceTimersByTime(999);
    expect(store.toastCount).toBe(1);
    vi.advanceTimersByTime(2);
    expect(store.toastCount).toBe(0);
  });

  it('does not create duplicate toast within 2s unless high priority', () => {
    const store = useToastStore();
    const first = store.showToast('Dup', 'warning', { duration: 0 });
    const second = store.showToast('Dup', 'warning', { duration: 0 });

    // second returns an id but should not add a second toast
    expect(store.toastCount).toBe(1);
    expect(first).not.toBeUndefined();
    expect(second).not.toBeUndefined();

    // High priority clears existing of same type and adds new one
    const third = store.showToast('Dup', 'warning', { duration: 0, priority: 'high' });
    expect(store.toastCount).toBe(1);
    expect(store.toasts[0].id).toBe(third);
  });

  it('clearToastsByType and clearAllToasts remove toasts and cancel timers', () => {
    const store = useToastStore();
    store.showToast('A', 'success', { duration: 5000 });
    store.showToast('B', 'error', { duration: 5000 });
    expect(store.toastCount).toBe(2);

    // clear only success
    store.clearToastsByType('success');
    expect(store.toastCount).toBe(1);
    expect(store.toasts[0].type).toBe('error');

    // clear all
    store.clearAllToasts();
    expect(store.toastCount).toBe(0);
  });

  it('hideToast removes a specific toast', () => {
    const store = useToastStore();
    const id = store.showToast('C', 'info', { duration: 0 });
    expect(store.toastCount).toBe(1);
    store.hideToast(id);
    expect(store.toastCount).toBe(0);
  });

  it('showSuccess, showError, showWarning, showInfo convenience methods work', () => {
    const store = useToastStore();

    store.showSuccess('Success');
    expect(store.toasts[0].type).toBe('success');
    expect(store.toasts[0].message).toBe('Success');

    store.showError('Error');
    expect(store.toasts[1].type).toBe('error');
    expect(store.toasts[1].message).toBe('Error');

    store.showWarning('Warning');
    expect(store.toasts[2].type).toBe('warning');
    expect(store.toasts[2].message).toBe('Warning');

    store.showInfo('Info');
    expect(store.toasts[3].type).toBe('info');
    expect(store.toasts[3].message).toBe('Info');

    expect(store.toastCount).toBe(4);
  });

  it('showError has extended default duration of 6000ms', () => {
    const store = useToastStore();
    store.showError('Long error');
    expect(store.toastCount).toBe(1);

    vi.advanceTimersByTime(3000);
    expect(store.toastCount).toBe(1); // Still visible

    vi.advanceTimersByTime(3000);
    expect(store.toastCount).toBe(0); // Hidden after 6s
  });

  it('persistent toasts do not auto-hide', () => {
    const store = useToastStore();
    store.showToast('Persistent', 'info', { persistent: true });
    expect(store.toastCount).toBe(1);

    vi.advanceTimersByTime(100000);
    expect(store.toastCount).toBe(1); // Still there
  });

  it('duration 0 prevents auto-hide', () => {
    const store = useToastStore();
    store.showToast('No hide', 'info', { duration: 0 });
    expect(store.toastCount).toBe(1);

    vi.advanceTimersByTime(100000);
    expect(store.toastCount).toBe(1);
  });

  it('hasToasts computed property updates correctly', () => {
    const store = useToastStore();
    expect(store.hasToasts).toBe(false);

    store.showSuccess('Test');
    expect(store.hasToasts).toBe(true);

    store.clearAllToasts();
    expect(store.hasToasts).toBe(false);
  });

  it('toasts is readonly', () => {
    const store = useToastStore();
    const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    // Readonly doesn't throw in Vue 3, but prevents mutation
    const initialLength = store.toasts.length;
    (store.toasts as any).push({
      id: 'test',
      message: 'test',
      type: 'info',
      timestamp: Date.now(),
    });

    expect(store.toasts.length).toBe(initialLength); // Length unchanged
    consoleSpy.mockRestore();
  });

  it('handles multiple toasts with different durations', () => {
    const store = useToastStore();
    store.showToast('Short', 'info', { duration: 1000 });
    store.showToast('Long', 'info', { duration: 5000 });
    expect(store.toastCount).toBe(2);

    vi.advanceTimersByTime(1000);
    expect(store.toastCount).toBe(1);
    expect(store.toasts[0].message).toBe('Long');

    vi.advanceTimersByTime(4000);
    expect(store.toastCount).toBe(0);
  });

  it('hideToast handles non-existent toast gracefully', async () => {
    const store = useToastStore();
    await expect(store.hideToast('non-existent-id')).resolves.not.toThrow();
    expect(store.toastCount).toBe(0);
  });

  it('clearToastsByType handles non-existent type gracefully', async () => {
    const store = useToastStore();
    store.showSuccess('Test');
    await store.clearToastsByType('warning');
    expect(store.toastCount).toBe(1); // Success toast still there
  });

  it('allows duplicates after 2 second window', () => {
    const store = useToastStore();
    store.showToast('Dup', 'info', { duration: 0 });
    expect(store.toastCount).toBe(1);

    vi.advanceTimersByTime(2000);

    store.showToast('Dup', 'info', { duration: 0 });
    expect(store.toastCount).toBe(2);
  });

  it('high priority clears all toasts of same type', () => {
    const store = useToastStore();
    store.showError('Error 1');
    store.showError('Error 2');
    store.showError('Error 3');
    expect(store.toastCount).toBe(3);

    store.showError('Important Error', { priority: 'high' });
    expect(store.toastCount).toBe(1);
    expect(store.toasts[0].message).toBe('Important Error');
  });

  it('clearAllToasts clears all pending timeouts', () => {
    const store = useToastStore();
    const clearTimeoutSpy = vi.spyOn(globalThis, 'clearTimeout');

    store.showSuccess('Toast 1', { duration: 5000 });
    store.showSuccess('Toast 2', { duration: 5000 });
    store.showSuccess('Toast 3', { duration: 5000 });

    store.clearAllToasts();
    expect(clearTimeoutSpy).toHaveBeenCalledTimes(3);
  });

  it('generates unique toast IDs', () => {
    const store = useToastStore();
    const id1 = store.showSuccess('Toast 1');
    const id2 = store.showSuccess('Toast 2');
    const id3 = store.showSuccess('Toast 3');

    expect(id1).not.toBe(id2);
    expect(id2).not.toBe(id3);
    expect(id1).not.toBe(id3);
    expect(id1).toMatch(/^toast-\d+-[a-z0-9]+$/);
  });

  it('showError can override default duration', () => {
    const store = useToastStore();
    store.showError('Quick error', { duration: 1000 });
    expect(store.toastCount).toBe(1);

    vi.advanceTimersByTime(1000);
    expect(store.toastCount).toBe(0);
  });

  it('convenience methods accept options', () => {
    const store = useToastStore();
    store.showSuccess('Test', { persistent: true });
    store.showWarning('Test', { duration: 2000 });
    store.showInfo('Test', { priority: 'high' });

    // High priority only clears same type (info), not success/warning
    expect(store.toastCount).toBe(3);
  });
});
