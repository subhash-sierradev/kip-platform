import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick, ref } from 'vue';
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router';

import { ROUTE_UPDATE_DEBOUNCE_MS, useListRouteSync } from '@/composables/useListRouteSync';
import type { DashboardViewMode } from '@/types/dashboard';

function createRouterMock() {
  const replace = vi.fn();
  const addRoute = vi.fn();
  const getRoutes = vi.fn().mockReturnValue([] as Array<{ path: string }>);
  const push = vi.fn();
  return { replace, addRoute, getRoutes, push } as unknown as Router & {
    replace: ReturnType<typeof vi.fn>;
    addRoute: ReturnType<typeof vi.fn>;
    getRoutes: ReturnType<typeof vi.fn>;
    push: ReturnType<typeof vi.fn>;
  };
}

function createRouteMock(query: Record<string, unknown> = {}): RouteLocationNormalizedLoaded {
  return { query } as unknown as RouteLocationNormalizedLoaded;
}

describe('useListRouteSync', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: (time: number) => void) => {
      cb(0);
      return 0 as unknown as number;
    });
  });

  it('applies valid query params to state and ignores invalid ones', () => {
    const router = createRouterMock();
    const route = createRouteMock({
      search: 'abc',
      sort: 'name',
      view: 'list',
      page: '3',
      size: '12',
      unknown: 'ignored',
    });

    const search = ref('');
    const sortBy = ref('createdDate');
    const viewMode = ref<DashboardViewMode>('grid');
    const currentPage = ref(1);
    const pageSize = ref(6);
    const totalPages = ref(10);
    const manualPageSize = ref<number | null>(12);

    const { applyStateFromRoute, buildStateQuery } = useListRouteSync(
      {
        router,
        route,
        validSortOptions: ['name', 'createdDate'],
        validViewModes: ['grid', 'list'],
        totalPages,
      },
      {
        search,
        sortBy,
        viewMode,
        currentPage,
        manualPageSize,
        pageSize,
        pageSizeOptions: [6, 12, 24],
      }
    );

    applyStateFromRoute();

    expect(search.value).toBe('abc');
    expect(sortBy.value).toBe('name');
    expect(viewMode.value).toBe('list');
    expect(currentPage.value).toBe(3);
    expect(pageSize.value).toBe(12);

    const q = buildStateQuery(false);
    expect(q).toMatchObject({ search: 'abc', sort: 'name', view: 'list', page: 3, size: 12 });
  });

  it('ignores invalid sort/view/size and keeps defaults', () => {
    const router = createRouterMock();
    const route = createRouteMock({ sort: 'bad', view: 'nope', size: '7' });

    const search = ref('');
    const sortBy = ref('createdDate');
    const viewMode = ref<DashboardViewMode>('grid');
    const currentPage = ref(2);
    const pageSize = ref(6);
    const totalPages = ref(5);

    const { applyStateFromRoute } = useListRouteSync(
      {
        router,
        route,
        validSortOptions: ['name', 'createdDate'],
        validViewModes: ['grid', 'list'],
        totalPages,
      },
      { search, sortBy, viewMode, currentPage, pageSize, pageSizeOptions: [6, 12, 24] }
    );

    applyStateFromRoute();

    expect(sortBy.value).toBe('createdDate');
    expect(viewMode.value).toBe('grid');
    expect(pageSize.value).toBe(6);
  });

  it('debounces route.replace calls when state changes rapidly', async () => {
    const router = createRouterMock();
    const route = createRouteMock({});

    const search = ref('');
    const sortBy = ref('createdDate');
    const viewMode = ref<DashboardViewMode>('grid');
    const currentPage = ref(1);
    const pageSize = ref(6);
    const totalPages = ref(3);

    const { markRestored } = useListRouteSync(
      {
        router,
        route,
        validSortOptions: ['name', 'createdDate'],
        validViewModes: ['grid', 'list'],
        totalPages,
      },
      { search, sortBy, viewMode, currentPage, pageSize, pageSizeOptions: [6, 12, 24] }
    );

    markRestored();

    search.value = 'a';
    search.value = 'ab';
    search.value = 'abc';
    await nextTick();

    vi.advanceTimersByTime(ROUTE_UPDATE_DEBOUNCE_MS - 1);
    expect(router.replace).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(router.replace).toHaveBeenCalledTimes(1);
    const call = (router.replace as unknown as ReturnType<typeof vi.fn>).mock.calls[0][0];
    expect(call).toHaveProperty('query');
    expect(call.query).toMatchObject({ search: 'abc' });
  });

  it('clamps currentPage to 1 when pageSize or search changes', async () => {
    const router = createRouterMock();
    const route = createRouteMock({});

    const search = ref('');
    const sortBy = ref('createdDate');
    const viewMode = ref<DashboardViewMode>('grid');
    const currentPage = ref(3);
    const pageSize = ref(6);
    const totalPages = ref(5);

    const { markRestored } = useListRouteSync(
      {
        router,
        route,
        validSortOptions: ['name', 'createdDate'],
        validViewModes: ['grid', 'list'],
        totalPages,
      },
      { search, sortBy, viewMode, currentPage, pageSize, pageSizeOptions: [6, 12, 24] }
    );

    markRestored();

    // Change page size -> should clamp to 1 and suppress one route update
    pageSize.value = 12;
    await nextTick();

    // Debounce won't run because suppress prevents scheduling; ensure no calls yet
    expect(router.replace).toHaveBeenCalledTimes(0);

    // Next state change should trigger replace
    search.value = 'x';
    await nextTick();
    vi.advanceTimersByTime(ROUTE_UPDATE_DEBOUNCE_MS);
    expect(router.replace).toHaveBeenCalledTimes(1);

    // And current page should be reset to 1
    expect(currentPage.value).toBe(1);
  });

  it('buildStateQuery optionally includes scroll param', () => {
    const router = createRouterMock();
    const route = createRouteMock({});

    const search = ref('s');
    const sortBy = ref('name');
    const viewMode = ref<DashboardViewMode>('list');
    const currentPage = ref(2);
    const pageSize = ref(12);
    const manualPageSize = ref<number | null>(null);
    const totalPages = ref(4);

    const { buildStateQuery } = useListRouteSync(
      { router, route, validSortOptions: ['name'], validViewModes: ['grid', 'list'], totalPages },
      {
        search,
        sortBy,
        viewMode,
        currentPage,
        manualPageSize,
        pageSize,
        pageSizeOptions: [6, 12, 24],
      }
    );

    Object.defineProperty(window, 'scrollY', { value: 200, configurable: true });
    const qWithScroll = buildStateQuery(true);
    expect(qWithScroll).toMatchObject({ scroll: 200 });
    expect(qWithScroll).not.toHaveProperty('size');

    const qNoScroll = buildStateQuery(false);
    expect(qNoScroll).not.toHaveProperty('scroll');
  });

  it('does not restore or persist page size when route persistence is disabled', () => {
    const router = createRouterMock();
    const route = createRouteMock({ size: '12' });

    const search = ref('');
    const sortBy = ref('createdDate');
    const viewMode = ref<DashboardViewMode>('grid');
    const currentPage = ref(1);
    const pageSize = ref(6);
    const totalPages = ref(5);
    const manualPageSize = ref<number | null>(null);
    const resetPageSize = vi.fn();
    const setPageSize = vi.fn();

    const { applyStateFromRoute, buildStateQuery } = useListRouteSync(
      {
        router,
        route,
        validSortOptions: ['name', 'createdDate'],
        validViewModes: ['grid', 'list'],
        totalPages,
        persistPageSizeInRoute: false,
      },
      {
        search,
        sortBy,
        viewMode,
        currentPage,
        manualPageSize,
        pageSize,
        pageSizeOptions: [6, 12, 24],
        resetPageSize,
        setPageSize,
      }
    );

    applyStateFromRoute();

    expect(resetPageSize).toHaveBeenCalledTimes(1);
    expect(setPageSize).not.toHaveBeenCalled();
    expect(buildStateQuery(false)).not.toHaveProperty('size');
  });
});
