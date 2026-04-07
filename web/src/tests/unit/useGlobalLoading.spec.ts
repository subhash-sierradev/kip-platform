import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';

import { useGlobalLoading } from '@/composables/useGlobalLoading';

describe('useGlobalLoading', () => {
  let setLoading: ReturnType<typeof useGlobalLoading>['setLoading'];
  let resetLoading: ReturnType<typeof useGlobalLoading>['resetLoading'];
  let isLoading: ReturnType<typeof useGlobalLoading>['isLoading'];
  let loadingMessage: ReturnType<typeof useGlobalLoading>['loadingMessage'];
  let activeRequests: ReturnType<typeof useGlobalLoading>['activeRequests'];
  let hasActiveRequests: ReturnType<typeof useGlobalLoading>['hasActiveRequests'];

  beforeEach(() => {
    // Create a fresh Pinia instance for each test to ensure isolation
    setActivePinia(createPinia());

    const composable = useGlobalLoading();
    setLoading = composable.setLoading;
    resetLoading = composable.resetLoading;
    isLoading = composable.isLoading;
    loadingMessage = composable.loadingMessage;
    activeRequests = composable.activeRequests;
    hasActiveRequests = composable.hasActiveRequests;
  });

  it('should initialize with loading false and zero active requests', () => {
    expect(isLoading.value).toBe(false);
    expect(loadingMessage.value).toBe('Loading...');
    expect(activeRequests.value).toBe(0);
    expect(hasActiveRequests.value).toBe(false);
  });

  it('should start loading with custom message and track active requests', () => {
    setLoading(true, 'Processing...');

    expect(isLoading.value).toBe(true);
    expect(loadingMessage.value).toBe('Processing...');
    expect(activeRequests.value).toBe(1);
    expect(hasActiveRequests.value).toBe(true);
  });

  it('should handle concurrent requests correctly', () => {
    // Start first request
    setLoading(true, 'Request 1');
    expect(isLoading.value).toBe(true);
    expect(activeRequests.value).toBe(1);
    expect(hasActiveRequests.value).toBe(true);

    // Start second request
    setLoading(true, 'Request 2');
    expect(isLoading.value).toBe(true);
    expect(loadingMessage.value).toBe('Request 2');
    expect(activeRequests.value).toBe(2);
    expect(hasActiveRequests.value).toBe(true);

    // End first request - should still be loading
    setLoading(false);
    expect(isLoading.value).toBe(true);
    expect(activeRequests.value).toBe(1);
    expect(hasActiveRequests.value).toBe(true);

    // End second request - should stop loading
    setLoading(false);
    expect(isLoading.value).toBe(false);
    expect(activeRequests.value).toBe(0);
    expect(hasActiveRequests.value).toBe(false);
  });

  it('should never go below zero active requests', () => {
    // Try to end request when none are active
    setLoading(false);
    setLoading(false);
    expect(isLoading.value).toBe(false);
    expect(activeRequests.value).toBe(0);
    expect(hasActiveRequests.value).toBe(false);

    // Start a request and it should work normally
    setLoading(true);
    expect(isLoading.value).toBe(true);
    expect(activeRequests.value).toBe(1);
    expect(hasActiveRequests.value).toBe(true);
  });

  it('should reset all state correctly', () => {
    // Start multiple requests
    setLoading(true, 'Request 1');
    setLoading(true, 'Request 2');

    expect(isLoading.value).toBe(true);
    expect(activeRequests.value).toBe(2);
    expect(loadingMessage.value).toBe('Request 2');
    expect(hasActiveRequests.value).toBe(true);

    // Reset should clear everything
    resetLoading();

    expect(isLoading.value).toBe(false);
    expect(activeRequests.value).toBe(0);
    expect(loadingMessage.value).toBe('Loading...');
    expect(hasActiveRequests.value).toBe(false);
  });

  it('should maintain state isolation between tests', () => {
    // This test verifies that each test gets a fresh Pinia instance
    // If the previous test left any state, this test would fail
    expect(isLoading.value).toBe(false);
    expect(activeRequests.value).toBe(0);
    expect(hasActiveRequests.value).toBe(false);

    // Modify state
    setLoading(true, 'Test isolation');
    expect(isLoading.value).toBe(true);
    expect(loadingMessage.value).toBe('Test isolation');
    expect(activeRequests.value).toBe(1);
  });
});
