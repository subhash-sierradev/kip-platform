// Mock DevExtreme theme engine to prevent DOM access & timers

// Ensure global DOM methods are available
if (typeof globalThis !== 'undefined' && typeof globalThis.window !== 'undefined') {
  if (!globalThis.window.getComputedStyle) {
    globalThis.window.getComputedStyle = () =>
      ({
        getPropertyValue: () => '',
        getPropertyPriority: () => '',
        item: () => '',
        length: 0,
        parentRule: null,
        cssText: '',
        setProperty: () => {},
        removeProperty: () => '',
        [Symbol.iterator]: function* (): Generator<string, undefined, unknown> {
          // Empty iterator for CSSStyleDeclaration compatibility
          yield* [];
        },
      }) as unknown as CSSStyleDeclaration;
  }
}

export function isPendingThemeLoaded() {
  return false;
}

export function readThemeMarker() {
  // Mock implementation that doesn't rely on DOM
  return {};
}

export function themeReady() {
  return Promise.resolve();
}

export default {
  isPendingThemeLoaded,
  readThemeMarker,
  themeReady,
};
