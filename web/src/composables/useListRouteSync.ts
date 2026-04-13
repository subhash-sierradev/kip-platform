import { nextTick, type Ref, ref, watch } from 'vue';
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router';

import type { DashboardViewMode } from '@/types/dashboard';
import { debounce } from '@/utils/debounceUtils';

/**
 * Debounce delay (ms) used when syncing list state to the route query.
 * Extracted to a constant for easier maintenance and shared understanding.
 */
export const ROUTE_UPDATE_DEBOUNCE_MS = 300;
/**
 * Reactive state contract for list pages using route synchronization.
 * Consumers provide these refs so `useListRouteSync` can read/write list UX state
 * and keep it in sync with the URL query string (including pagination and sorting).
 */
export interface RouteSyncState {
  search: Ref<string>;
  sortBy: Ref<string>;
  viewMode: Ref<DashboardViewMode>;
  currentPage: Ref<number>;
  pageSize: Ref<number>;
  pageSizeOptions: number[];
  manualPageSize?: Ref<number | null>;
  resetPageSize?: () => void;
  setPageSize?: (newPageSize: number) => void;
}
/**
 * Configuration required for route synchronization.
 * - router/route: Vue Router instances used to read current query and update it
 * - validSortOptions: whitelist of allowed sort keys to guard query tampering
 * - validViewModes: allowed view modes (e.g., 'grid' | 'list') for type-safe validation
 * - totalPages: computed total pages, used for clamping current page when inputs change
 */
export interface RouteSyncConfig {
  router: Router;
  route: RouteLocationNormalizedLoaded;
  validSortOptions: string[];
  validViewModes: Array<DashboardViewMode>;
  totalPages: Ref<number>;
  persistPageSizeInRoute?: boolean;
}

function shouldPersistPageSizeInRoute(config: RouteSyncConfig): boolean {
  return config.persistPageSizeInRoute !== false;
}

/**
 * Narrow a runtime value to `DashboardViewMode` using the provided allow-list.
 */
function isValidViewMode(
  val: unknown,
  modes: ReadonlyArray<DashboardViewMode>
): val is DashboardViewMode {
  return typeof val === 'string' && (modes as ReadonlyArray<string>).includes(val);
}

function applyPageFromQuery(pageQuery: unknown, currentPage: Ref<number>): void {
  if (typeof pageQuery !== 'string') {
    return;
  }

  const parsedPage = Number(pageQuery);
  if (!Number.isNaN(parsedPage) && parsedPage >= 1) {
    currentPage.value = parsedPage;
  }
}

function applyPageSizeFromQuery(sizeQuery: unknown, state: RouteSyncState): void {
  if (typeof sizeQuery !== 'string') {
    state.resetPageSize?.();
    return;
  }

  const parsedPageSize = Number(sizeQuery);
  if (!state.pageSizeOptions.includes(parsedPageSize)) {
    return;
  }

  if (state.setPageSize) {
    state.setPageSize(parsedPageSize);
    return;
  }

  state.pageSize.value = parsedPageSize;
}

// Internal helpers to keep the main composable small
function applyStateFromRouteInternal(config: RouteSyncConfig, state: RouteSyncState): void {
  const q = config.route.query;
  if (typeof q.search === 'string') state.search.value = q.search;

  if (typeof q.sort === 'string' && config.validSortOptions.includes(q.sort)) {
    state.sortBy.value = q.sort;
  }

  if (isValidViewMode(q.view, config.validViewModes)) {
    state.viewMode.value = q.view;
  }

  applyPageFromQuery(q.page, state.currentPage);

  if (!shouldPersistPageSizeInRoute(config)) {
    state.resetPageSize?.();
    return;
  }

  applyPageSizeFromQuery(q.size, state);
}

function buildStateQueryInternal(
  config: RouteSyncConfig,
  state: RouteSyncState,
  includeScroll = false
): Record<string, string | number> {
  const q: Record<string, string | number> = {
    search: state.search.value,
    sort: state.sortBy.value,
    view: state.viewMode.value,
    page: state.currentPage.value,
  };

  if (shouldPersistPageSizeInRoute(config) && state.manualPageSize?.value !== null) {
    q.size = state.pageSize.value;
  }

  if (includeScroll) q.scroll = Math.max(0, Math.round(window.scrollY));
  return q;
}

function createUpdateRouteQuery(router: Router, getQuery: () => Record<string, string | number>) {
  return debounce(() => {
    router.replace({ query: getQuery() });
  }, ROUTE_UPDATE_DEBOUNCE_MS);
}

function setupPaginationClampWatcher(
  config: RouteSyncConfig,
  state: RouteSyncState,
  initialRestorationDone: Ref<boolean>,
  suppressRouteUpdate: Ref<boolean>
): void {
  watch(
    [state.search, state.sortBy, state.viewMode, state.pageSize],
    ([_s, _sort, _view, _size], [prevS, _prevSort, _prevView, prevSize]) => {
      if (!initialRestorationDone.value) return;
      const max = config.totalPages.value;
      if (state.currentPage.value > max) {
        suppressRouteUpdate.value = true;
        state.currentPage.value = Math.max(1, max);
        return;
      }
      const pageSizeChanged = _size !== prevSize;
      const searchChanged = _s !== prevS;
      if ((pageSizeChanged || searchChanged) && state.currentPage.value !== 1) {
        suppressRouteUpdate.value = true;
        state.currentPage.value = 1;
      }
    }
  );
}

function setupTotalPagesClampWatcher(
  config: RouteSyncConfig,
  state: RouteSyncState,
  initialRestorationDone: Ref<boolean>,
  suppressRouteUpdate: Ref<boolean>
): void {
  watch(config.totalPages, newTotal => {
    if (!initialRestorationDone.value) return;
    if (state.currentPage.value > newTotal) {
      suppressRouteUpdate.value = true;
      state.currentPage.value = Math.max(1, newTotal);
    }
  });
}

function setupDebouncedQuerySyncWatcher(
  state: RouteSyncState,
  suppressRouteUpdate: Ref<boolean>,
  updateRouteQuery: () => void
): void {
  watch([state.search, state.sortBy, state.viewMode, state.currentPage, state.pageSize], () => {
    if (suppressRouteUpdate.value) {
      suppressRouteUpdate.value = false;
      return;
    }
    updateRouteQuery();
  });
}

async function restoreScrollFromQueryInternal(route: RouteLocationNormalizedLoaded): Promise<void> {
  const scrollQ = route.query.scroll as string | undefined;
  const y = scrollQ ? Number(scrollQ) : 0;
  if (!Number.isNaN(y) && y > 0) {
    await nextTick();
    window.requestAnimationFrame(() => {
      window.scrollTo({ top: y, behavior: 'auto' });
    });
  }
}

/**
 * Ensure a details route exists in the router at runtime.
 *
 * Purpose:
 * - Some list pages navigate to a lazily loaded details view that may not be
 *   statically declared in the router configuration (e.g., modular features or tests).
 * - This helper defensively adds the route once to avoid navigation errors when
 *   calling `router.push()` to a path that isn't yet registered.
 *
 * Parameters:
 * - router: Vue Router instance used to query and register routes.
 * - path: Path pattern to register (e.g., '/outbound/integration/arcgis/:id').
 * - name: Unique route name for Vue Router.
 * - componentLoader: Lazy loader returning a Promise for the route component
 *   (typically `() => import('...')`).
 *
 * Notes:
 * - No-op when a route with the same path already exists.
 * - Safe to call from `onMounted` in list pages and within unit tests.
 */
function ensureDetailsRouteInternal(
  router: Router,
  path: string,
  name: string,
  componentLoader: () => Promise<unknown>
): void {
  const existingRoute = router.getRoutes().find(r => r.path === path);
  if (!existingRoute) {
    router.addRoute({ path, name, component: componentLoader });
  }
}

/**
 * Synchronize a list page's UI state with the route query string.
 *
 * Responsibilities:
 * - Parse initial query into provided state refs (search/sort/view/page/size)
 * - Clamp pagination when inputs or total pages change
 * - Debounce and write state changes back to the route query
 * - Optionally restore scroll from query (when navigating back from details)
 * - Ensure a lazy-loaded details route exists (optional helper)
 *
 * Usage pattern:
 * - Call once in setup() of the list page component with the relevant refs
 * - On mount: `applyStateFromRoute()`, load data, then `markRestored()` and `restoreScrollFromQuery()`
 */
export function useListRouteSync(config: RouteSyncConfig, state: RouteSyncState) {
  const suppressRouteUpdate = ref(false);
  const initialRestorationDone = ref(false);

  const applyStateFromRoute = () => applyStateFromRouteInternal(config, state);
  const buildStateQuery = (includeScroll = false) =>
    buildStateQueryInternal(config, state, includeScroll);
  const restoreScrollFromQuery = () => restoreScrollFromQueryInternal(config.route);
  const ensureDetailsRoute = (
    path: string,
    name: string,
    componentLoader: () => Promise<unknown>
  ) => ensureDetailsRouteInternal(config.router, path, name, componentLoader);

  const updateRouteQuery = createUpdateRouteQuery(config.router, () => buildStateQuery(false));

  setupPaginationClampWatcher(config, state, initialRestorationDone, suppressRouteUpdate);
  setupTotalPagesClampWatcher(config, state, initialRestorationDone, suppressRouteUpdate);
  setupDebouncedQuerySyncWatcher(state, suppressRouteUpdate, updateRouteQuery);

  const markRestored = () => {
    initialRestorationDone.value = true;
  };

  return {
    /** Read current route query and populate state refs. */
    applyStateFromRoute,
    /** Build a serializable query object from current state refs. */
    buildStateQuery,
    /** Restore scroll position from the `scroll` query param, if present. */
    restoreScrollFromQuery,
    /** Mark that initial state restoration is complete (enables clamping behavior). */
    markRestored,
    /** Ensure a details route exists (handy for dynamic/lazy details pages). */
    ensureDetailsRoute,
  };
}
