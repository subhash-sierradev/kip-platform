import { onBeforeUnmount, onMounted, type Ref, ref } from 'vue';

const LARGE_PAGE_SIZE_BREAKPOINT = 1440;
const EXTRA_LARGE_PAGE_SIZE_BREAKPOINT = 1800;
const SMALL_SCREEN_PAGE_SIZE = 6;
const LARGE_SCREEN_PAGE_SIZE = 9;
const EXTRA_LARGE_SCREEN_PAGE_SIZE = 12;
const VIEWPORT_SYNC_DEBOUNCE_MS = 150;

type MediaQueryChangeListener = (event: MediaQueryListEvent) => void;

function getViewportWidth(): number {
  if (typeof window === 'undefined') {
    return EXTRA_LARGE_PAGE_SIZE_BREAKPOINT - 1;
  }

  if (typeof window.visualViewport?.width === 'number' && window.visualViewport.width > 0) {
    return Math.round(window.visualViewport.width);
  }

  if (
    typeof document.documentElement?.clientWidth === 'number' &&
    document.documentElement.clientWidth > 0
  ) {
    return document.documentElement.clientWidth;
  }

  return window.innerWidth;
}

function addMediaQueryChangeListener(
  query: MediaQueryList,
  listener: MediaQueryChangeListener
): void {
  query.addEventListener('change', listener);
}

function removeMediaQueryChangeListener(
  query: MediaQueryList,
  listener: MediaQueryChangeListener
): void {
  query.removeEventListener('change', listener);
}

/**
 * Composable for responsive page size based on screen width
 * - Standard screens (<1440px): 6 items per page
 * - Large screens (≥1440px): 9 items per page
 * - Extra-large screens (≥1800px): 12 items per page
 */
export function useResponsivePageSize() {
  let currentWidth = getViewportWidth();
  const manualPageSize = ref<number | null>(null);
  const pageSize: Ref<number> = ref(getResponsivePageSize(currentWidth));

  function getResponsivePageSize(width: number): number {
    if (width >= EXTRA_LARGE_PAGE_SIZE_BREAKPOINT) {
      return EXTRA_LARGE_SCREEN_PAGE_SIZE;
    }

    if (width >= LARGE_PAGE_SIZE_BREAKPOINT) {
      return LARGE_SCREEN_PAGE_SIZE;
    }

    return SMALL_SCREEN_PAGE_SIZE;
  }

  function setPageSize(newPageSize: number) {
    manualPageSize.value = newPageSize;
    pageSize.value = newPageSize;
  }

  function resetPageSize() {
    manualPageSize.value = null;
    syncResponsivePageSize();
  }

  /**
   * Update page size based on current screen width
   */
  function syncResponsivePageSize() {
    currentWidth = getViewportWidth();

    if (manualPageSize.value !== null) {
      return;
    }

    const newSize = getResponsivePageSize(currentWidth);

    if (pageSize.value !== newSize) {
      pageSize.value = newSize;
    }
  }

  /**
   * Handle window resize with debouncing
   */
  let resizeTimeout: number | undefined;
  let pageSizeQueries: MediaQueryList[] = [];

  function scheduleViewportSync() {
    clearTimeout(resizeTimeout);
    resizeTimeout = window.setTimeout(() => {
      syncResponsivePageSize();
    }, VIEWPORT_SYNC_DEBOUNCE_MS);
  }

  function handleVisibilityChange() {
    if (document.visibilityState === 'visible') {
      scheduleViewportSync();
    }
  }

  onMounted(() => {
    pageSizeQueries = [
      window.matchMedia(`(min-width: ${LARGE_PAGE_SIZE_BREAKPOINT}px)`),
      window.matchMedia(`(min-width: ${EXTRA_LARGE_PAGE_SIZE_BREAKPOINT}px)`),
    ];

    syncResponsivePageSize();
    window.addEventListener('resize', scheduleViewportSync);
    window.visualViewport?.addEventListener('resize', scheduleViewportSync);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    pageSizeQueries.forEach(query => {
      addMediaQueryChangeListener(query, scheduleViewportSync);
    });
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', scheduleViewportSync);
    window.visualViewport?.removeEventListener('resize', scheduleViewportSync);
    document.removeEventListener('visibilitychange', handleVisibilityChange);

    pageSizeQueries.forEach(query => {
      removeMediaQueryChangeListener(query, scheduleViewportSync);
    });

    clearTimeout(resizeTimeout);
  });

  return {
    manualPageSize,
    pageSize,
    resetPageSize,
    setPageSize,
  };
}
