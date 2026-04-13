import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent } from 'vue';

import { useResponsivePageSize } from '@/composables/useResponsivePageSize';

const TestHarness = defineComponent({
  name: 'ResponsivePageSizeHarness',
  setup() {
    return useResponsivePageSize();
  },
  template: '<div>{{ pageSize }}</div>',
});

function setViewportWidth(width: number) {
  Object.defineProperty(window, 'innerWidth', {
    value: width,
    writable: true,
    configurable: true,
  });

  Object.defineProperty(document.documentElement, 'clientWidth', {
    value: width,
    writable: true,
    configurable: true,
  });
}

describe('useResponsivePageSize', () => {
  let mediaQueryChangeListeners: Record<string, Array<(event: MediaQueryListEvent) => void>>;

  beforeEach(() => {
    vi.useFakeTimers();
    mediaQueryChangeListeners = {};
    Object.defineProperty(window, 'visualViewport', {
      value: undefined,
      writable: true,
      configurable: true,
    });
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: window.innerWidth >= Number.parseInt(String(query).match(/\d+/)?.[0] ?? '0', 10),
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(
          (eventName: string, listener: (event: MediaQueryListEvent) => void) => {
            if (eventName === 'change') {
              mediaQueryChangeListeners[query] ??= [];
              mediaQueryChangeListeners[query].push(listener);
            }
          }
        ),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('updates the default page size when the viewport shrinks below the large-screen breakpoint', async () => {
    setViewportWidth(1300);
    const wrapper = mount(TestHarness);

    expect((wrapper.vm as any).pageSize).toBe(6);

    setViewportWidth(1024);
    window.dispatchEvent(new Event('resize'));
    vi.advanceTimersByTime(151);

    expect((wrapper.vm as any).pageSize).toBe(6);

    wrapper.unmount();
  });

  it('keeps a manually selected page size across resize events', async () => {
    setViewportWidth(1500);
    const wrapper = mount(TestHarness);

    (wrapper.vm as any).setPageSize(24);
    expect((wrapper.vm as any).pageSize).toBe(24);

    setViewportWidth(900);
    window.dispatchEvent(new Event('resize'));
    vi.advanceTimersByTime(151);

    expect((wrapper.vm as any).pageSize).toBe(24);

    wrapper.unmount();
  });

  it('can reset a manual page size back to the responsive default', async () => {
    setViewportWidth(1500);
    const wrapper = mount(TestHarness);

    (wrapper.vm as any).setPageSize(24);
    expect((wrapper.vm as any).pageSize).toBe(24);

    setViewportWidth(1024);
    (wrapper.vm as any).resetPageSize();

    expect((wrapper.vm as any).pageSize).toBe(6);

    wrapper.unmount();
  });

  it('uses the 12-card default on very wide viewports', async () => {
    setViewportWidth(1920);
    const wrapper = mount(TestHarness);

    expect((wrapper.vm as any).pageSize).toBe(12);

    wrapper.unmount();
  });

  it('updates when the page-size media query changes without a resize event', async () => {
    setViewportWidth(1500);
    const wrapper = mount(TestHarness);

    expect((wrapper.vm as any).pageSize).toBe(9);

    setViewportWidth(1024);
    mediaQueryChangeListeners['(min-width: 1440px)']?.forEach(listener => {
      listener({ matches: false } as MediaQueryListEvent);
    });
    vi.advanceTimersByTime(151);

    expect((wrapper.vm as any).pageSize).toBe(6);

    wrapper.unmount();
  });

  it('uses 9-card default on large viewports (1440–1799px)', async () => {
    setViewportWidth(1600);
    const wrapper = mount(TestHarness);

    expect((wrapper.vm as any).pageSize).toBe(9);

    wrapper.unmount();
  });

  it('updates page size to 12 when the 1800px media query fires', async () => {
    setViewportWidth(1600);
    const wrapper = mount(TestHarness);

    expect((wrapper.vm as any).pageSize).toBe(9);

    setViewportWidth(1920);
    mediaQueryChangeListeners['(min-width: 1800px)']?.forEach(listener => {
      listener({ matches: true } as MediaQueryListEvent);
    });
    vi.advanceTimersByTime(151);

    expect((wrapper.vm as any).pageSize).toBe(12);

    wrapper.unmount();
  });

  it('prefers visualViewport width when it is available', async () => {
    Object.defineProperty(window, 'visualViewport', {
      value: {
        width: 1810,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      },
      configurable: true,
    });
    setViewportWidth(1200);

    const wrapper = mount(TestHarness);

    expect((wrapper.vm as any).pageSize).toBe(12);

    wrapper.unmount();
  });

  it('syncs on visibility change only when the document becomes visible', async () => {
    setViewportWidth(1600);
    const wrapper = mount(TestHarness);
    expect((wrapper.vm as any).pageSize).toBe(9);

    Object.defineProperty(document, 'visibilityState', {
      value: 'hidden',
      configurable: true,
    });
    setViewportWidth(1920);
    document.dispatchEvent(new Event('visibilitychange'));
    vi.advanceTimersByTime(151);
    expect((wrapper.vm as any).pageSize).toBe(9);

    Object.defineProperty(document, 'visibilityState', {
      value: 'visible',
      configurable: true,
    });
    document.dispatchEvent(new Event('visibilitychange'));
    vi.advanceTimersByTime(151);
    expect((wrapper.vm as any).pageSize).toBe(12);

    wrapper.unmount();
  });

  it('falls back to documentElement width when visualViewport width is unavailable', async () => {
    Object.defineProperty(window, 'visualViewport', {
      value: {
        width: 0,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      },
      configurable: true,
    });
    setViewportWidth(1450);

    const wrapper = mount(TestHarness);

    expect((wrapper.vm as any).pageSize).toBe(9);
    wrapper.unmount();
  });
});
