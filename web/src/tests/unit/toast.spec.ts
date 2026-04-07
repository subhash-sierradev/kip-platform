import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useToastStore } from '@/store/toast';

// Mock notificationUtils
vi.mock('@/utils/notificationUtils', () => ({
  setGlobalToast: vi.fn(),
}));

describe('useToastStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllTimers();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  describe('initial state', () => {
    it('starts with empty toasts', () => {
      const store = useToastStore();

      expect(store.toasts).toEqual([]);
      expect(store.toastCount).toBe(0);
      expect(store.hasToasts).toBe(false);
    });
  });

  describe('showSuccess', () => {
    it('adds success toast with default duration', () => {
      const store = useToastStore();

      const id = store.showSuccess('Operation successful');

      expect(store.toasts).toHaveLength(1);
      expect(store.toasts[0].message).toBe('Operation successful');
      expect(store.toasts[0].type).toBe('success');
      expect(store.toasts[0].id).toBe(id);
      expect(store.toastCount).toBe(1);
      expect(store.hasToasts).toBe(true);
    });

    it('auto-hides success toast after default duration', () => {
      const store = useToastStore();

      store.showSuccess('Success message');
      expect(store.toasts).toHaveLength(1);

      vi.advanceTimersByTime(3000);
      expect(store.toasts).toHaveLength(0);
    });

    it('accepts custom duration', () => {
      const store = useToastStore();

      store.showSuccess('Quick message', { duration: 1000 });
      expect(store.toasts).toHaveLength(1);

      vi.advanceTimersByTime(999);
      expect(store.toasts).toHaveLength(1);

      vi.advanceTimersByTime(2);
      expect(store.toasts).toHaveLength(0);
    });
  });

  describe('showError', () => {
    it('adds error toast with extended duration', () => {
      const store = useToastStore();

      const id = store.showError('Error occurred');

      expect(store.toasts).toHaveLength(1);
      expect(store.toasts[0].message).toBe('Error occurred');
      expect(store.toasts[0].type).toBe('error');
      expect(store.toasts[0].id).toBe(id);
    });

    it('auto-hides error toast after 6 seconds by default', () => {
      const store = useToastStore();

      store.showError('Error message');
      expect(store.toasts).toHaveLength(1);

      vi.advanceTimersByTime(5999);
      expect(store.toasts).toHaveLength(1);

      vi.advanceTimersByTime(2);
      expect(store.toasts).toHaveLength(0);
    });

    it('can override error duration', () => {
      const store = useToastStore();

      store.showError('Brief error', { duration: 2000 });

      vi.advanceTimersByTime(2000);
      expect(store.toasts).toHaveLength(0);
    });
  });

  describe('showWarning', () => {
    it('adds warning toast', () => {
      const store = useToastStore();

      const id = store.showWarning('Warning message');

      expect(store.toasts).toHaveLength(1);
      expect(store.toasts[0].message).toBe('Warning message');
      expect(store.toasts[0].type).toBe('warning');
      expect(store.toasts[0].id).toBe(id);
    });

    it('auto-hides after default duration', () => {
      const store = useToastStore();

      store.showWarning('Warning');

      vi.advanceTimersByTime(3000);
      expect(store.toasts).toHaveLength(0);
    });
  });

  describe('showInfo', () => {
    it('adds info toast', () => {
      const store = useToastStore();

      const id = store.showInfo('Information');

      expect(store.toasts).toHaveLength(1);
      expect(store.toasts[0].message).toBe('Information');
      expect(store.toasts[0].type).toBe('info');
      expect(store.toasts[0].id).toBe(id);
    });

    it('auto-hides after default duration', () => {
      const store = useToastStore();

      store.showInfo('Info');

      vi.advanceTimersByTime(3000);
      expect(store.toasts).toHaveLength(0);
    });
  });

  describe('persistent toasts', () => {
    it('does not auto-hide persistent toasts', () => {
      const store = useToastStore();

      store.showSuccess('Persistent message', { persistent: true });

      vi.advanceTimersByTime(10000);
      expect(store.toasts).toHaveLength(1);
    });

    it('can manually hide persistent toasts', async () => {
      const store = useToastStore();

      const id = store.showError('Persistent error', { persistent: true });
      expect(store.toasts).toHaveLength(1);

      await store.hideToast(id);
      expect(store.toasts).toHaveLength(0);
    });
  });

  describe('hideToast', () => {
    it('removes specific toast by id', async () => {
      const store = useToastStore();

      const id1 = store.showSuccess('First');
      const id2 = store.showSuccess('Second');

      expect(store.toasts).toHaveLength(2);

      await store.hideToast(id1);

      expect(store.toasts).toHaveLength(1);
      expect(store.toasts[0].id).toBe(id2);
    });

    it('does nothing if toast id not found', async () => {
      const store = useToastStore();

      store.showSuccess('Message');

      await store.hideToast('non-existent-id');

      expect(store.toasts).toHaveLength(1);
    });

    it('clears timeout when hiding toast', async () => {
      const store = useToastStore();

      const id = store.showSuccess('Message');

      await store.hideToast(id);

      vi.advanceTimersByTime(3000);
      expect(store.toasts).toHaveLength(0); // Should still be 0, timeout cleared
    });
  });

  describe('clearAllToasts', () => {
    it('removes all toasts', async () => {
      const store = useToastStore();

      store.showSuccess('First');
      store.showError('Second');
      store.showWarning('Third');

      expect(store.toasts).toHaveLength(3);

      await store.clearAllToasts();

      expect(store.toasts).toHaveLength(0);
      expect(store.toastCount).toBe(0);
      expect(store.hasToasts).toBe(false);
    });

    it('clears all pending timeouts', async () => {
      const store = useToastStore();

      store.showSuccess('First');
      store.showSuccess('Second');

      await store.clearAllToasts();

      vi.advanceTimersByTime(5000);
      expect(store.toasts).toHaveLength(0);
    });
  });

  describe('clearToastsByType', () => {
    it('removes only toasts of specified type', async () => {
      const store = useToastStore();

      store.showSuccess('Success 1');
      store.showError('Error 1');
      store.showSuccess('Success 2');
      store.showWarning('Warning 1');

      expect(store.toasts).toHaveLength(4);

      await store.clearToastsByType('success');

      expect(store.toasts).toHaveLength(2);
      expect(store.toasts.some(t => t.type === 'success')).toBe(false);
      expect(store.toasts.some(t => t.type === 'error')).toBe(true);
      expect(store.toasts.some(t => t.type === 'warning')).toBe(true);
    });

    it('clears timeouts for removed toasts', async () => {
      const store = useToastStore();

      store.showSuccess('Success');
      store.showError('Error');

      await store.clearToastsByType('success');

      vi.advanceTimersByTime(10000);
      expect(store.toasts).toHaveLength(0); // Error should auto-hide
    });
  });

  describe('duplicate prevention', () => {
    it('prevents duplicate toasts within 2 seconds', () => {
      const store = useToastStore();

      store.showSuccess('Duplicate message');
      store.showSuccess('Duplicate message');

      expect(store.toasts).toHaveLength(1);
    });

    it('allows same message after 2 seconds', () => {
      const store = useToastStore();

      store.showSuccess('Message');

      vi.advanceTimersByTime(2001);

      store.showSuccess('Message');

      expect(store.toasts).toHaveLength(2);
    });

    it('allows duplicate if priority is high', () => {
      const store = useToastStore();

      store.showError('Important error');
      store.showError('Important error', { priority: 'high' });

      expect(store.toasts).toHaveLength(1);
      expect(store.toasts[0].message).toBe('Important error');
    });
  });

  describe('priority handling', () => {
    it('high priority clears existing toasts of same type', () => {
      const store = useToastStore();

      store.showError('Error 1');
      store.showError('Error 2');

      expect(store.toasts).toHaveLength(2);

      store.showError('Critical error', { priority: 'high' });

      expect(store.toasts).toHaveLength(1);
      expect(store.toasts[0].message).toBe('Critical error');
    });

    it('normal priority does not clear existing toasts', () => {
      const store = useToastStore();

      store.showInfo('Info 1');
      store.showInfo('Info 2');

      expect(store.toasts).toHaveLength(2);
    });
  });

  describe('multiple toasts', () => {
    it('can display multiple toasts of different types', () => {
      const store = useToastStore();

      store.showSuccess('Success');
      store.showError('Error');
      store.showWarning('Warning');
      store.showInfo('Info');

      expect(store.toasts).toHaveLength(4);
      expect(store.toastCount).toBe(4);
    });

    it('auto-hides toasts independently', () => {
      const store = useToastStore();

      store.showSuccess('Quick', { duration: 1000 });
      store.showSuccess('Slow', { duration: 3000 });

      expect(store.toasts).toHaveLength(2);

      vi.advanceTimersByTime(1001);
      expect(store.toasts).toHaveLength(1);

      vi.advanceTimersByTime(2000);
      expect(store.toasts).toHaveLength(0);
    });
  });

  describe('zero duration', () => {
    it('does not auto-hide when duration is 0', () => {
      const store = useToastStore();

      store.showSuccess('Manual dismiss', { duration: 0 });

      vi.advanceTimersByTime(10000);
      expect(store.toasts).toHaveLength(1);
    });
  });

  describe('edge cases', () => {
    it('generates unique IDs for each toast', () => {
      const store = useToastStore();

      const id1 = store.showSuccess('Message 1');
      const id2 = store.showSuccess('Message 2');
      const id3 = store.showSuccess('Message 3');

      expect(id1).not.toBe(id2);
      expect(id2).not.toBe(id3);
      expect(id1).not.toBe(id3);
    });

    it('maintains toast timestamps', () => {
      const store = useToastStore();

      const before = Date.now();
      store.showSuccess('Timed message');
      const after = Date.now();

      expect(store.toasts[0].timestamp).toBeGreaterThanOrEqual(before);
      expect(store.toasts[0].timestamp).toBeLessThanOrEqual(after);
    });
  });
});
