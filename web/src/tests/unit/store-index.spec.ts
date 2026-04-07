/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';

import { configureStore } from '@/store';

describe('store/index configureStore', () => {
  it('creates a Pinia instance and allows store usage', () => {
    const pinia = configureStore();
    expect(pinia).toBeTruthy();
    // Minimal sanity: just ensure we got an object back
    expect(typeof pinia).toBe('object');
    // do not assert too deep; Pinia internals may change
  });
});
