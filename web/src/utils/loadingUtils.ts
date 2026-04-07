import type { Ref } from 'vue';

/**
 * Generic loading wrapper utility functions
 */

/**
 * Wraps an async action with loading state management using a provided ref
 */
export async function withLoadingRef<T>(
  loadingRef: Ref<boolean>,
  action: () => Promise<T>
): Promise<T> {
  loadingRef.value = true;
  try {
    return await action();
  } finally {
    loadingRef.value = false;
  }
}

/**
 * Creates a withLoading function bound to a specific loading ref
 */
export function createWithLoading(loadingRef: Ref<boolean>) {
  return async function withLoading<T>(action: () => Promise<T>): Promise<T> {
    return withLoadingRef(loadingRef, action);
  };
}
