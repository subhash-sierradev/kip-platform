import { defineStore } from 'pinia';

interface GlobalLoadingState {
  isLoading: boolean;
  loadingMessage: string;
  activeRequests: number;
}

/**
 * Global loading state management using Pinia
 * Tracks concurrent requests and provides loading indicators
 */
export const useGlobalLoadingStore = defineStore('globalLoading', {
  state: (): GlobalLoadingState => ({
    isLoading: false,
    loadingMessage: 'Loading...',
    activeRequests: 0,
  }),

  getters: {
    hasActiveRequests: (state): boolean => state.activeRequests > 0,
    currentMessage: (state): string => state.loadingMessage,
  },

  actions: {
    /**
     * Set loading state with optional message
     * @param loading - Whether to start or stop loading
     * @param message - Optional loading message
     */
    setLoading(loading: boolean, message = 'Loading...'): void {
      if (loading) {
        this.activeRequests++;
        this.isLoading = true;
        this.loadingMessage = message;
      } else {
        this.activeRequests = Math.max(0, this.activeRequests - 1);
        if (this.activeRequests === 0) {
          this.isLoading = false;
        }
      }
    },

    /**
     * Reset all loading state
     */
    resetLoading(): void {
      this.activeRequests = 0;
      this.isLoading = false;
      this.loadingMessage = 'Loading...';
    },
  },
});
