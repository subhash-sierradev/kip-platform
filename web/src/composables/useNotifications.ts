import { onUnmounted, ref } from 'vue';

import { OpenAPI } from '@/api/core/OpenAPI';
import { getToken } from '@/config/keycloak';
import { useNotificationStore } from '@/store/notification';
import type { AppNotification } from '@/types/notification';

const RECONNECT_DELAYS_MS = [2_000, 4_000, 8_000, 16_000, 30_000];

export function useNotifications() {
  const notificationStore = useNotificationStore();
  const eventSource = ref<EventSource | null>(null);
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let destroyed = false;
  let reconnectAttempt = 0;

  function buildStreamUrl(): string {
    const token = getToken();
    const base = (OpenAPI.BASE ?? '').replace(/\/$/, '');
    const url = `${base}/management/notifications/stream`;
    return token ? `${url}?access_token=${encodeURIComponent(token)}` : url;
  }

  function connect(): void {
    if (destroyed) return;
    const url = buildStreamUrl();
    const es = new EventSource(url);
    eventSource.value = es;

    es.addEventListener('notification', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as AppNotification & { type?: string };
        if (data.type === 'CONNECTED') {
          // Successful connection — reset backoff
          reconnectAttempt = 0;
          // Initial handshake — sync count from DB in case browser was offline
          notificationStore.fetchCount();
          if (!notificationStore.loaded) {
            notificationStore.fetchNotifications();
          }
          return;
        }
        const notification = data as AppNotification;

        notificationStore.addIncoming(notification);
        notificationStore.addNotificationToast(notification);
      } catch {
        // malformed payload — ignore
      }
    });

    es.onerror = () => {
      es.close();
      eventSource.value = null;
      if (!destroyed) {
        const delay =
          RECONNECT_DELAYS_MS[Math.min(reconnectAttempt, RECONNECT_DELAYS_MS.length - 1)];
        reconnectAttempt++;
        reconnectTimer = setTimeout(connect, delay);
      }
    };
  }

  function disconnect(): void {
    destroyed = true;
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (eventSource.value) {
      eventSource.value.close();
      eventSource.value = null;
    }
  }

  connect();

  onUnmounted(disconnect);

  return { disconnect };
}
