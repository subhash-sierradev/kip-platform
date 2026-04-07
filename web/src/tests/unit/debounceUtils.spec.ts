import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { debounce } from '@/utils/debounceUtils';

// Use fake timers to control setTimeout/clearTimeout behavior
beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.clearAllTimers();
  vi.useRealTimers();
});

describe('debounceUtils.debounce', () => {
  it('delays execution by the specified wait time', () => {
    const spy = vi.fn();
    const debounced = debounce(spy, 100);

    debounced();
    // Should not call immediately
    expect(spy).not.toHaveBeenCalled();

    // Advance just before the wait
    vi.advanceTimersByTime(99);
    expect(spy).not.toHaveBeenCalled();

    // Advance to reach the wait
    vi.advanceTimersByTime(1);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('coalesces rapid calls and invokes only once with the last arguments', () => {
    const spy = vi.fn<(a: string, b: number) => void>();
    const debounced = debounce(spy, 100);

    // t=0
    debounced('a', 1);
    // t=50 -> push out to t=150
    vi.advanceTimersByTime(50);
    debounced('b', 2);
    // t=80 -> push out to t=180
    vi.advanceTimersByTime(30);
    debounced('c', 3);

    // Not called yet
    expect(spy).not.toHaveBeenCalled();

    // Run past final wait to trigger once
    vi.advanceTimersByTime(200);
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith('c', 3);
  });

  it('clears the previous timeout when called again before wait', () => {
    const spy = vi.fn();
    const debounced = debounce(spy, 100);

    const clearTimeoutSpy = vi.spyOn(globalThis, 'clearTimeout');

    debounced('first');
    // Call again before the timeout expires
    vi.advanceTimersByTime(20);
    debounced('second');

    // We expect clearTimeout to be used to cancel the previous schedule
    expect(clearTimeoutSpy).toHaveBeenCalledTimes(1);

    // And only the last call should execute after wait
    vi.advanceTimersByTime(100);
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith('second');

    clearTimeoutSpy.mockRestore();
  });

  it('passes through multiple argument types correctly', () => {
    const spy = vi.fn();
    const debounced = debounce(spy, 50);

    const obj = { x: 1 };
    debounced('msg', 42, obj, true);

    vi.advanceTimersByTime(50);
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith('msg', 42, obj, true);
  });
});
