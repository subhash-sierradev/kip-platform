import { computed, ref } from 'vue';

import type { CreateEventCatalogRequest, NotificationEventCatalogResponse } from '@/api';
import { NotificationAdminService } from '@/api/services/NotificationAdminService';
import Alert from '@/utils/notificationUtils';

export function useNotificationEvents() {
  const events = ref<NotificationEventCatalogResponse[]>([]);
  const loadingEvents = ref(false);
  const saving = ref(false);

  async function fetchEvents(): Promise<void> {
    loadingEvents.value = true;
    try {
      events.value = await NotificationAdminService.getEvents();
    } catch {
      Alert.error('Failed to load event catalog');
    } finally {
      loadingEvents.value = false;
    }
  }

  async function createEvent(payload: CreateEventCatalogRequest): Promise<boolean> {
    saving.value = true;
    try {
      const created = await NotificationAdminService.createEvent({ requestBody: payload });
      events.value = [created, ...events.value];
      Alert.success('Event created successfully');
      return true;
    } catch {
      Alert.error('Failed to create event');
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deactivateEvent(id: string): Promise<void> {
    try {
      await NotificationAdminService.deactivateEvent({ id });
      events.value = events.value.map(e => (e.id === id ? { ...e, isActive: false } : e));
      Alert.success('Event deactivated');
    } catch {
      Alert.error('Failed to deactivate event');
    }
  }

  /** Set of event keys the server returned — already scoped to tenant's active integrations. */
  const activeEventKeys = computed(
    () => new Set(events.value.map(e => e.eventKey).filter((k): k is string => !!k))
  );

  return {
    events,
    loadingEvents,
    saving,
    fetchEvents,
    createEvent,
    deactivateEvent,
    activeEventKeys,
  };
}
