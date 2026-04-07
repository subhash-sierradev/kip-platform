// Test environment setup with necessary polyfills and DevExtreme configuration

// Critical: Set up window.getComputedStyle BEFORE any other imports
// This must be done at the very top to prevent DevExtreme from failing
if (typeof globalThis !== 'undefined') {
  if (typeof globalThis.window === 'undefined') {
    globalThis.window = {} as any;
  }

  if (!globalThis.window.getComputedStyle) {
    Object.defineProperty(globalThis.window, 'getComputedStyle', {
      value: (_element?: Element) => ({
        getPropertyValue: (_prop: string) => '',
        getPropertyPriority: (_prop: string) => '',
        item: (_index: number) => '',
        length: 0,
        parentRule: null,
        cssText: '',
        setProperty: (_prop: string, _value: string, _priority?: string) => {},
        removeProperty: (_prop: string) => '',
        [Symbol.iterator]: function* () {},
        // Add commonly accessed CSS properties
        display: 'block',
        position: 'static',
        visibility: 'visible',
        opacity: '1',
      }),
      writable: true,
      configurable: true,
    });
  }
}

import { config } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeAll, beforeEach, vi } from 'vitest';
import { defineComponent, h } from 'vue';

// Ensure window.getComputedStyle is available before any imports (redundant safety check)
if (typeof window !== 'undefined' && !window.getComputedStyle) {
  Object.defineProperty(window, 'getComputedStyle', {
    value: (_element?: Element) => ({
      getPropertyValue: (_prop: string) => '',
      getPropertyPriority: (_prop: string) => '',
      item: (_index: number) => '',
      length: 0,
      parentRule: null,
      cssText: '',
      setProperty: (_prop: string, _value: string, _priority?: string) => {},
      removeProperty: (_prop: string) => '',
      [Symbol.iterator]: function* () {},
      // Add commonly accessed CSS properties that DevExtreme might need
      display: 'block',
      position: 'static',
      visibility: 'visible',
      opacity: '1',
    }),
    writable: true,
    configurable: true,
  });
}

// DevExtreme Vue submodule stubs with named exports used by our components/tests
const dxStub = (name: string) =>
  defineComponent({
    name,
    setup:
      (_props, { slots }) =>
      () =>
        h('div', slots.default?.()),
  });
vi.mock('devextreme-vue/data-grid', () => ({
  DxDataGrid: dxStub('DxDataGrid'),
  DxColumn: dxStub('DxColumn'),
  DxToolbar: dxStub('DxToolbar'),
  DxItem: dxStub('DxItem'),
  DxHeaderFilter: dxStub('DxHeaderFilter'),
  DxPager: dxStub('DxPager'),
  DxPaging: dxStub('DxPaging'),
  DxExport: dxStub('DxExport'),
}));
vi.mock('devextreme-vue/button', () => ({
  DxButton: defineComponent({
    name: 'DxButton',
    emits: ['click'],
    setup(_props, { emit, slots }) {
      return () =>
        h('button', { class: 'dx-button', onClick: () => emit('click') }, slots.default?.());
    },
  }),
}));

// jsdom fallback (safety)
if (!window.getComputedStyle) {
  Object.defineProperty(window, 'getComputedStyle', {
    value: () => ({
      getPropertyValue: () => '',
    }),
  });
}

// silence ResizeObserver (DevExtreme uses it)
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
vi.stubGlobal('ResizeObserver', ResizeObserver);

// Mock EventSource (jsdom does not implement SSE)
class EventSourceMock {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSED = 2;
  readyState = EventSourceMock.CONNECTING;
  url: string;
  withCredentials: boolean;
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  constructor(url: string, options?: { withCredentials?: boolean }) {
    this.url = url;
    this.withCredentials = options?.withCredentials ?? false;
  }
  addEventListener() {}
  removeEventListener() {}
  dispatchEvent() {
    return true;
  }
  close() {
    this.readyState = EventSourceMock.CLOSED;
  }
}
vi.stubGlobal('EventSource', EventSourceMock);

// Mock window.matchMedia for mobile detection
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  configurable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Activate a fresh Pinia instance before each test to avoid store injection errors
beforeEach(() => {
  setActivePinia(createPinia());
});

const ensureMinimalDomGlobals = () => {
  const g: any = globalThis as any;
  if (typeof g.document === 'undefined') {
    const docStub = {
      body: {
        appendChild: () => {},
        removeChild: () => {},
        querySelector: () => null,
      },
      createElement: () => ({ style: {}, remove: () => {} }),
      querySelector: () => null,
      documentElement: { style: {} },
    };
    g.document = docStub;
  }
  if (typeof g.window === 'undefined') {
    g.window = {
      document: g.document,
      getComputedStyle: () => ({
        getPropertyValue: () => '',
        getPropertyPriority: () => '',
        item: () => '',
        length: 0,
        parentRule: null,
        cssText: '',
        setProperty: () => {},
        removeProperty: () => '',
        [Symbol.iterator]: function* () {},
      }),
    };
  } else if (g.window && !g.window.getComputedStyle) {
    g.window.getComputedStyle = () => ({
      getPropertyValue: () => '',
      getPropertyPriority: () => '',
      item: () => '',
      length: 0,
      parentRule: null,
      cssText: '',
      setProperty: () => {},
      removeProperty: () => '',
      [Symbol.iterator]: function* () {},
    });
  }
};
ensureMinimalDomGlobals();

// Stub window.location.assign to avoid jsdom navigation errors from libraries (e.g., keycloak-js)
try {
  if (
    typeof window !== 'undefined' &&
    window.location &&
    typeof window.location.assign === 'function'
  ) {
    vi.spyOn(window.location, 'assign').mockImplementation(() => {
      /* noop */
    });
  }
} catch {
  /* no-op */
}

// Create mock crypto object
const createMockCrypto = () => ({
  getRandomValues: (arr: Uint8Array) => {
    for (let i = 0; i < arr.length; i++) {
      arr[i] = Math.floor(Math.random() * 256);
    }
    return arr;
  },
  randomUUID: () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  },
  hash: vi.fn().mockReturnValue('mocked-hash'),
  subtle: {
    digest: vi.fn().mockResolvedValue(new ArrayBuffer(32)),
    generateKey: vi.fn(),
    sign: vi.fn(),
    verify: vi.fn(),
  },
});

// Setup global crypto
const setupGlobalCrypto = (mockCrypto: any) => {
  Object.defineProperty(globalThis, 'crypto', {
    value: mockCrypto,
    writable: true,
    configurable: true,
  });

  if (typeof window !== 'undefined') {
    Object.defineProperty(window, 'crypto', {
      value: mockCrypto,
      writable: true,
      configurable: true,
    });
  }

  vi.doMock('crypto', () => ({
    default: mockCrypto,
    hash: vi.fn().mockReturnValue('mocked-hash'),
    createHash: vi.fn().mockReturnValue({
      update: vi.fn().mockReturnThis(),
      digest: vi.fn().mockReturnValue('mocked-digest'),
    }),
    webcrypto: mockCrypto,
  }));
};

// Setup DOM observers
const setupDOMObservers = () => {
  class RO {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
  class IO {
    observe() {}
    unobserve() {}
    disconnect() {}
  }

  Object.defineProperty(globalThis, 'ResizeObserver', {
    value: RO,
    configurable: true,
  });

  Object.defineProperty(globalThis, 'IntersectionObserver', {
    value: IO,
    configurable: true,
  });
};

// Setup DevExtreme globals
const setupDevExtreme = () => {
  Object.defineProperty(globalThis, 'DevExpress', {
    value: {
      ui: {},
      data: {},
    },
    configurable: true,
  });

  // Globally mock DevExtreme Vue components to avoid loading heavy DOM-dependent code
  vi.mock('devextreme-vue', () => {
    const stub = { name: 'DxStub', template: '<div />' };
    return {
      DxButton: stub,
      DxLoadPanel: stub,
      DxDataGrid: stub,
      DxForm: stub,
      DxPopup: stub,
      DxTooltip: stub,
      DxSelectBox: stub,
      DxTextBox: stub,
      DxNumberBox: stub,
      DxCheckBox: stub,
      DxDateBox: stub,
      DxSwitch: stub,
      DxTreeList: stub,
    };
  });

  // Mock core DevExtreme modules that sometimes get imported indirectly
  vi.mock('devextreme/core/config', () => ({
    default: (cfg: unknown) => cfg,
  }));
  vi.mock('devextreme/ui/themes', () => ({
    // Minimal noop implementations
    initialized: () => true,
    current: () => 'generic.light',
    ready: (cb: () => void) => cb(),
  }));

  // Explicitly mock CJS internal themes module to avoid DOM/timer access
  vi.mock('devextreme/cjs/__internal/ui/themes.js', () => ({
    isPendingThemeLoaded: () => false,
    readThemeMarker: () => ({}),
    themeReady: () => Promise.resolve(),
    default: {
      isPendingThemeLoaded: () => false,
      readThemeMarker: () => ({}),
      themeReady: () => Promise.resolve(),
    },
  }));

  // Silence DevExtreme trial panel client to avoid touching DOM during tests
  vi.mock('devextreme/cjs/__internal/core/license/trial_panel.client', () => ({ default: {} }));
  vi.mock('devextreme/cjs/__internal/core/license/trial_panel.client.js', () => ({ default: {} }));

  // Explicitly mock DevExtreme internal theme engine to avoid getComputedStyle usage
  vi.mock('devextreme/cjs/__internal/ui/themes.js', () => ({
    isPendingThemeLoaded: () => false,
    readThemeMarker: () => ({}),
    themeReady: () => Promise.resolve(),
    default: {
      isPendingThemeLoaded: () => false,
      readThemeMarker: () => ({}),
      themeReady: () => Promise.resolve(),
    },
  }));
};

// Main setup function
beforeAll(() => {
  const mockCrypto = createMockCrypto();
  setupGlobalCrypto(mockCrypto);
  setupDOMObservers();
  setupDevExtreme();

  // Pinia activation is handled per-test where needed; avoid global interference

  // Ensure jsdom-like globals exist and have required methods
  // Some DevExtreme internals expect these to always be present
  if (typeof window !== 'undefined') {
    if (typeof window.getComputedStyle !== 'function') {
      // Provide a lightweight stub compatible with theme checks
      // Return object with getPropertyValue to avoid crashes

      (window as any).getComputedStyle = () => ({
        getPropertyValue: () => '',
      });
    }
    // Ensure document is set (already handled in early hardening, keep as safety)

    if (typeof document === 'undefined' && typeof window !== 'undefined')
      (globalThis as any).document = (window as unknown as { document: Document }).document;
  }
});

// Global Vue Test Utils configuration
config.global.mocks = {
  $t: (key: string) => key, // Mock i18n if needed
};
