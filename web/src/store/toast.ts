import { defineStore } from 'pinia';
import { computed, nextTick, readonly, shallowRef } from 'vue';

import { setGlobalToast, type ToastMessage, type ToastType } from '../utils/notificationUtils';

// Helper functions
const generateToastId = (): string => {
  const timestamp = Date.now();
  return `toast-${timestamp}-${Math.random().toString(36).slice(2)}`;
};

const isDuplicateToast = (
  toasts: ToastMessage[],
  message: string,
  type: ToastType,
  timestamp: number
): boolean => {
  return toasts.some(
    existingToast =>
      existingToast.message === message &&
      existingToast.type === type &&
      timestamp - existingToast.timestamp < 2000
  );
};

interface ToastOptions {
  duration?: number;
  persistent?: boolean;
  priority?: 'low' | 'normal' | 'high';
}

// Helper functions outside the store to reduce complexity
const useToastState = () => {
  const toasts = shallowRef<ToastMessage[]>([]);
  const timeoutRefs = new Map<string, ReturnType<typeof setTimeout>>();
  const activeToasts = computed(() => readonly(toasts.value));
  const toastCount = computed(() => toasts.value.length);
  const hasToasts = computed(() => toasts.value.length > 0);

  return { toasts, timeoutRefs, activeToasts, toastCount, hasToasts };
};

const useToastActions = (
  toasts: { value: ToastMessage[] },
  timeoutRefs: Map<string, ReturnType<typeof setTimeout>>
) => {
  const hideToast = async (id: string): Promise<void> => {
    try {
      toasts.value = toasts.value.filter(toast => toast.id !== id);

      const timeout = timeoutRefs.get(id);
      if (timeout) {
        clearTimeout(timeout);
        timeoutRefs.delete(id);
      }

      await nextTick();
    } catch (err) {
      if (import.meta.env.DEV) {
        console.error('Error hiding toast:', err);
      }
    }
  };

  const clearAllToasts = async (): Promise<void> => {
    try {
      toasts.value = [];
      timeoutRefs.forEach(timeout => clearTimeout(timeout));
      timeoutRefs.clear();
      await nextTick();
    } catch (err) {
      if (import.meta.env.DEV) {
        console.error('Error clearing toasts:', err);
      }
      toasts.value = [];
      timeoutRefs.clear();
    }
  };

  return { hideToast, clearAllToasts };
};

const useToastHelpers = (
  toasts: { value: ToastMessage[] },
  timeoutRefs: Map<string, ReturnType<typeof setTimeout>>,
  hideToastFn: (id: string) => Promise<void>
) => {
  const clearToastsByType = async (type: ToastType): Promise<void> => {
    const toastsToClear = toasts.value.filter(t => t.type === type);
    toastsToClear.forEach(toast => {
      const timeout = timeoutRefs.get(toast.id);
      if (timeout) {
        clearTimeout(timeout);
        timeoutRefs.delete(toast.id);
      }
    });
    toasts.value = toasts.value.filter(t => t.type !== type);
    await nextTick();
  };

  const showToast = (message: string, type: ToastType, options: ToastOptions = {}): string => {
    const { duration = 3000, persistent = false, priority = 'normal' } = options;
    const timestamp = Date.now();
    const id = generateToastId();

    if (isDuplicateToast(toasts.value, message, type, timestamp) && priority !== 'high') {
      return id;
    }

    if (priority === 'high') {
      toasts.value = toasts.value.filter(t => t.type !== type);
    }

    const newToast: ToastMessage = { id, message, type, timestamp };
    toasts.value = [...toasts.value, newToast];

    if (!persistent && duration > 0) {
      const timeout = setTimeout(() => hideToastFn(id), duration);
      timeoutRefs.set(id, timeout);
    }

    return id;
  };

  return { clearToastsByType, showToast };
};

export const useToastStore = defineStore('toast', () => {
  const { toasts, timeoutRefs, activeToasts, toastCount, hasToasts } = useToastState();
  const { hideToast, clearAllToasts } = useToastActions(toasts, timeoutRefs);
  const { clearToastsByType, showToast } = useToastHelpers(toasts, timeoutRefs, hideToast);

  const initializeToast = (): void => {
    setGlobalToast((message: string, type: ToastType) => {
      showToast(message, type);
    });
  };

  const showSuccess = (message: string, options?: ToastOptions) =>
    showToast(message, 'success', options);

  const showError = (message: string, options?: ToastOptions) =>
    showToast(message, 'error', { duration: 6000, ...options });

  const showWarning = (message: string, options?: ToastOptions) =>
    showToast(message, 'warning', options);

  const showInfo = (message: string, options?: ToastOptions) => showToast(message, 'info', options);

  return {
    toasts: activeToasts,
    toastCount,
    hasToasts,
    showToast,
    hideToast,
    clearAllToasts,
    clearToastsByType,
    initializeToast,
    showSuccess,
    showError,
    showWarning,
    showInfo,
  };
});
