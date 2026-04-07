import { describe, expect, it, vi } from 'vitest';

import { CancelablePromise, CancelError } from '@/api/core/CancelablePromise';

describe('CancelablePromise', () => {
  it('resolves and ignores cancel afterwards', async () => {
    const p = new CancelablePromise<string>(resolve => {
      setTimeout(() => resolve('ok'));
    });

    const result = await p;
    expect(result).toBe('ok');

    // cancel after resolve should be no-op
    expect(() => p.cancel()).not.toThrow();
    expect(p.isCancelled).toBe(false);
  });

  it('rejects and ignores cancel afterwards', async () => {
    const p = new CancelablePromise<string>((_resolve, reject) => {
      setTimeout(() => reject(new Error('bad')));
    });

    await expect(p).rejects.toThrowError('bad');
    // cancel after reject should be no-op
    expect(() => p.cancel()).not.toThrow();
    expect(p.isCancelled).toBe(false);
  });

  it('invokes cancel handlers and rejects with CancelError', async () => {
    const onCleanup = vi.fn();
    const p = new CancelablePromise<string>((_resolve, _reject, onCancel) => {
      onCancel(onCleanup);
    });

    p.cancel();
    expect(p.isCancelled).toBe(true);
    expect(onCleanup).toHaveBeenCalledTimes(1);

    await expect(p).rejects.toBeInstanceOf(CancelError);
  });

  it('is idempotent when cancel() is called multiple times', async () => {
    const onCleanup = vi.fn();
    const p = new CancelablePromise<string>((_resolve, _reject, onCancel) => {
      onCancel(onCleanup);
    });

    p.cancel();
    p.cancel();
    p.cancel();

    expect(onCleanup).toHaveBeenCalledTimes(1);
    await expect(p).rejects.toBeInstanceOf(CancelError);
  });

  it('does not register cancel handlers after resolve/reject', async () => {
    const p1 = new CancelablePromise<string>((resolve, _reject, onCancel) => {
      resolve('done');
      // handler should be ignored after resolve
      onCancel(() => {
        throw new Error('should not be called');
      });
    });
    await expect(p1).resolves.toBe('done');
    expect(() => p1.cancel()).not.toThrow();

    const p2 = new CancelablePromise<string>((_resolve, reject, onCancel) => {
      reject(new Error('x'));
      // handler should be ignored after reject
      onCancel(() => {
        throw new Error('should not be called');
      });
    });
    await expect(p2).rejects.toThrow('x');
    expect(() => p2.cancel()).not.toThrow();
  });
});
