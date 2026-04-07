import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { computed, nextTick, ref } from 'vue';

import SimpleLoader from '@/components/common/SimpleLoader.vue';
import { useGlobalLoading } from '@/composables/useGlobalLoading';

// Mock the composable
vi.mock('@/composables/useGlobalLoading', () => ({
  useGlobalLoading: vi.fn(),
}));

describe('SimpleLoader', () => {
  const mockUseGlobalLoading = vi.mocked(useGlobalLoading);

  beforeEach(() => {
    // Create body element for Teleport
    document.body.innerHTML = '<div id="app"></div>';
    // Reset the mock for each test
    mockUseGlobalLoading.mockReset();
  });

  it('should render when loading is true', async () => {
    // Use actual refs to properly simulate reactivity
    const isLoading = ref(true);
    const loadingMessage = ref('Loading data...');
    const activeRequests = ref(1);

    mockUseGlobalLoading.mockReturnValue({
      isLoading,
      loadingMessage,
      activeRequests,
      hasActiveRequests: computed(() => activeRequests.value > 0),
      setLoading: vi.fn(),
      resetLoading: vi.fn(),
    });

    const wrapper = mount(SimpleLoader, {
      attachTo: document.body,
    });
    await nextTick();

    // Since it uses Teleport, check if the content was teleported to body
    expect(document.body.innerHTML).toContain('subtle-loader-overlay');
    expect(document.body.innerHTML).toContain('subtle-spinner');
    expect(document.body.innerHTML).toContain('Loading data...');

    wrapper.unmount();
  });

  it('should not render when loading is false', async () => {
    // Use actual refs to properly simulate reactivity
    const isLoading = ref(false);
    const loadingMessage = ref('Loading...');
    const activeRequests = ref(0);

    mockUseGlobalLoading.mockReturnValue({
      isLoading,
      loadingMessage,
      activeRequests,
      hasActiveRequests: computed(() => activeRequests.value > 0),
      setLoading: vi.fn(),
      resetLoading: vi.fn(),
    });

    const wrapper = mount(SimpleLoader, {
      attachTo: document.body,
    });
    await nextTick();

    expect(document.body.innerHTML).not.toContain('subtle-loader-overlay');
    wrapper.unmount();
  });

  it('should update when loading state changes', async () => {
    // Use actual refs to test reactivity
    const isLoading = ref(false);
    const loadingMessage = ref('Loading...');
    const activeRequests = ref(0);

    mockUseGlobalLoading.mockReturnValue({
      isLoading,
      loadingMessage,
      activeRequests,
      hasActiveRequests: computed(() => activeRequests.value > 0),
      setLoading: vi.fn(),
      resetLoading: vi.fn(),
    });

    const wrapper = mount(SimpleLoader, {
      attachTo: document.body,
    });
    await nextTick();

    // Initially not loading
    expect(document.body.innerHTML).not.toContain('subtle-loader-overlay');

    // Change to loading
    isLoading.value = true;
    loadingMessage.value = 'Processing...';
    await nextTick();

    expect(document.body.innerHTML).toContain('subtle-loader-overlay');
    expect(document.body.innerHTML).toContain('Processing...');

    wrapper.unmount();
  });
});
