/* eslint-disable simple-import-sort/imports */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, nextTick } from 'vue';
import { mount } from '@vue/test-utils';

const {
  getToken,
  fetchCount,
  fetchNotifications,
  addIncoming,
  addNotificationToast,
  notificationStoreState,
} = vi.hoisted(() => ({
  getToken: vi.fn(),
  fetchCount: vi.fn(),
  fetchNotifications: vi.fn(),
  addIncoming: vi.fn(),
  addNotificationToast: vi.fn(),
  notificationStoreState: { loaded: false },
}));

vi.mock('@/config/keycloak', () => ({
  getToken,
}));

vi.mock('@/api/core/OpenAPI', () => ({
  OpenAPI: { BASE: 'http://localhost:8085' },
}));

vi.mock('@/store/notification', () => ({
  useNotificationStore: () => ({
    get loaded() {
      return notificationStoreState.loaded;
    },
    fetchCount,
    fetchNotifications,
    addIncoming,
    addNotificationToast,
  }),
}));

import { useNotifications } from '@/composables/useNotifications';

type ListenerMap = Record<string, ((event: any) => void)[]>;

class FakeEventSource {
  static instances: FakeEventSource[] = [];

  url: string;
  listeners: ListenerMap = {};
  onerror: ((event?: unknown) => void) | null = null;
  close = vi.fn();

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, cb: (event: any) => void): void {
    this.listeners[type] ??= [];
    this.listeners[type].push(cb);
  }

  emit(type: string, payload: unknown): void {
    (this.listeners[type] ?? []).forEach(cb => cb({ data: JSON.stringify(payload) }));
  }
}

function mountComposable() {
  const wrapper = mount(
    defineComponent({
      setup() {
        const api = useNotifications();
        return { api };
      },
      template: '<div />',
    })
  );
  return wrapper;
}

describe('useNotifications', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    FakeEventSource.instances = [];
    getToken.mockReturnValue('tkn');
    notificationStoreState.loaded = false;
    (globalThis as any).EventSource = FakeEventSource;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('connects to SSE stream with token and handles CONNECTED handshake', async () => {
    mountComposable();

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.instances[0].url).toContain('access_token=tkn');

    FakeEventSource.instances[0].emit('notification', { type: 'CONNECTED' });
    await nextTick();

    expect(fetchCount).toHaveBeenCalled();
    expect(fetchNotifications).toHaveBeenCalled();
    expect(addIncoming).not.toHaveBeenCalled();
  });

  it('handles CONNECTED handshake without fetching list when store is already loaded', async () => {
    notificationStoreState.loaded = true;
    mountComposable();

    FakeEventSource.instances[0].emit('notification', { type: 'CONNECTED' });
    await nextTick();

    expect(fetchCount).toHaveBeenCalled();
    expect(fetchNotifications).not.toHaveBeenCalled();
  });

  it('handles regular notification payloads and malformed payloads', async () => {
    mountComposable();

    FakeEventSource.instances[0].emit('notification', {
      id: 'n1',
      title: 'Hello',
      message: 'World',
      severity: 'INFO',
      type: 'A',
      createdDate: new Date().toISOString(),
      isRead: false,
    });

    expect(addIncoming).toHaveBeenCalledTimes(1);
    expect(addNotificationToast).toHaveBeenCalledTimes(1);

    // malformed JSON path (caught and ignored)
    FakeEventSource.instances[0].listeners.notification[0]({ data: '{invalid-json' });
    expect(addIncoming).toHaveBeenCalledTimes(1);
  });

  it('forwards notification payload as-is (store handles normalization)', async () => {
    mountComposable();

    FakeEventSource.instances[0].emit('notification', {
      id: 'n-raw',
      userId: 'user-2',
      title: 'Needs date',
      message: 'No createdDate present',
      severity: 'WARNING',
      type: 'A',
      isRead: false,
    });

    expect(addIncoming).toHaveBeenCalledTimes(1);
    expect(addNotificationToast).toHaveBeenCalledTimes(1);
    const forwardedPayload = addIncoming.mock.calls[0][0] as { id?: string; createdDate?: string };
    expect(forwardedPayload.id).toBe('n-raw');
    expect(forwardedPayload.createdDate).toBeUndefined();
  });

  it('connects to SSE even when token is missing', async () => {
    getToken.mockReturnValue('');
    mountComposable();

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.instances[0].url).not.toContain('access_token=');
  });

  it('reconnects with backoff on error and disconnects cleanly on unmount', async () => {
    const wrapper = mountComposable();

    const first = FakeEventSource.instances[0];
    first.onerror?.();
    expect(first.close).toHaveBeenCalled();

    vi.advanceTimersByTime(2000);
    expect(FakeEventSource.instances).toHaveLength(2);

    wrapper.unmount();
    expect(FakeEventSource.instances[1].close).toHaveBeenCalled();
  });

  it('exposes disconnect method for manual stop', async () => {
    const wrapper = mountComposable();
    const vmApi = (wrapper.vm as any).api as { disconnect: () => void };

    vmApi.disconnect();
    expect(FakeEventSource.instances[0].close).toHaveBeenCalled();
  });

  it('does not reconnect after manual disconnect even if error fires', async () => {
    const wrapper = mountComposable();
    const first = FakeEventSource.instances[0];
    const vmApi = (wrapper.vm as any).api as { disconnect: () => void };

    vmApi.disconnect();
    first.onerror?.();
    vi.advanceTimersByTime(30_000);

    expect(FakeEventSource.instances).toHaveLength(1);
    wrapper.unmount();
  });
});
