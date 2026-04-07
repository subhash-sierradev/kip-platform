import { storeToRefs } from 'pinia';
import { computed, readonly } from 'vue';

import { useGlobalLoadingStore } from '@/store/globalLoading';

/**
 * Global loading composable using Pinia store
 * Provides reactive loading state with better testability
 */
export function useGlobalLoading() {
  const store = useGlobalLoadingStore();
  const { isLoading, loadingMessage, activeRequests } = storeToRefs(store);

  return {
    isLoading: readonly(isLoading),
    loadingMessage: readonly(loadingMessage),
    activeRequests: readonly(activeRequests),
    hasActiveRequests: computed(() => store.hasActiveRequests),
    setLoading: store.setLoading,
    resetLoading: store.resetLoading,
  };
}
