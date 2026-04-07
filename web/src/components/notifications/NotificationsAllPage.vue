<template>
  <div class="notifications-page">
    <div class="notifications-header">
      <div class="notifications-header-left">
        <div class="notifications-filter-bar">
          <button
            v-for="tab in primaryFilterTabs"
            :key="tab.value"
            class="filter-tab"
            :class="{ active: activeFilterTab === tab.value }"
            @click="handleFilterTabChange(tab.value)"
          >
            {{ tab.label }}
            <span v-if="tab.badge" class="filter-tab-badge">{{ tab.badge }}</span>
          </button>

          <span class="filter-tabs-divider" aria-hidden="true"></span>

          <button
            v-for="tab in severityFilterTabs"
            :key="tab.value"
            class="filter-tab"
            :class="{ active: activeFilterTab === tab.value }"
            @click="handleFilterTabChange(tab.value)"
          >
            {{ tab.label }}
            <span v-if="tab.badge" class="filter-tab-badge">{{ tab.badge }}</span>
          </button>
        </div>
      </div>
      <div class="notifications-header-actions">
        <span class="notifications-count">{{
          `${filteredNotifications.length} of ${activeTabTotalCount} total`
        }}</span>
        <DxButton
          text="Mark all read"
          styling-mode="outlined"
          :disabled="markingAll || store.unreadCount === 0"
          @click="handleMarkAllRead"
        />
        <DxButton
          text="Delete all"
          icon="trash"
          type="danger"
          styling-mode="outlined"
          :disabled="deletingAll || activeTabTotalCount === 0"
          @click="handleDeleteAll"
        />
      </div>
    </div>

    <div
      v-if="!store.filteredLoaded && filteredNotifications.length === 0"
      class="notifications-loading"
    >
      <div v-for="i in 5" :key="i" class="skeleton-row">
        <div class="skeleton-dot"></div>
        <div class="skeleton-body">
          <div class="skeleton-title"></div>
          <div class="skeleton-message"></div>
        </div>
      </div>
    </div>

    <div v-else-if="filteredNotifications.length === 0" class="notifications-empty">
      <i class="dx-icon dx-icon-bell" style="font-size: 3rem; opacity: 0.2"></i>
      <p class="notifications-empty-title">You're all caught up</p>
      <p class="notifications-empty-sub">No notifications match the selected filters.</p>
    </div>

    <div v-else ref="splitContainer" class="notifications-content">
      <div
        ref="listPanelRef"
        class="notifications-list-panel"
        :style="listPanelStyle"
        tabindex="0"
        role="listbox"
        aria-label="Notifications list"
        @keydown="handleListKeydown"
      >
        <div
          v-for="n in filteredNotifications"
          :key="n.id"
          :data-notification-id="n.id"
          class="notification-card"
          role="option"
          :aria-selected="selectedNotification?.id === n.id"
          :class="[
            { unread: !n.isRead, selected: selectedNotification?.id === n.id },
            `notification-card--${getSeverityClass(n.severity)}`,
          ]"
          @click="handleSelect(n)"
        >
          <div class="notification-card-body">
            <div class="notification-card-top">
              <span class="notification-card-title">{{ n.title }}</span>
              <span class="notification-card-time" :title="formatAbsoluteTime(n.createdDate)">
                {{ formatRelativeTime(n.createdDate) || 'Just now' }}
              </span>
            </div>
            <p class="notification-card-message">
              {{ localizeMessageTimestamps(n.message) }}
            </p>
          </div>
          <div
            v-if="!n.isRead"
            class="unread-indicator"
            :class="getSeverityClass(n.severity)"
          ></div>
        </div>

        <div v-if="store.filteredHasMore" class="load-more-section">
          <DxButton
            text="Load more"
            styling-mode="outlined"
            :disabled="store.filteredLoadingMore"
            @click="store.loadMoreTabNotifications()"
          />
        </div>
      </div>

      <div
        class="notifications-resize-handle"
        role="separator"
        aria-orientation="vertical"
        @mousedown="startResize"
      ></div>

      <div class="notifications-detail-panel">
        <template v-if="selectedNotification">
          <div class="details-header">
            <div class="details-title-wrap">
              <h2 class="details-title">
                {{ selectedNotification.title }}
              </h2>
              <span class="severity-chip" :class="getSeverityClass(selectedNotification.severity)">
                {{ getSeverityValue(selectedNotification.severity) }}
              </span>
            </div>
            <div class="details-actions">
              <DxButton
                icon="close"
                type="danger"
                styling-mode="text"
                hint="Delete notification"
                :element-attr="{ 'aria-label': 'Delete notification' }"
                @click="handleDelete(selectedNotification.id)"
              />
            </div>
          </div>

          <div class="details-meta">
            <div class="details-meta-item">
              <span class="details-meta-label">Triggered By</span>
              <span class="details-meta-value">{{
                getNotificationCreatedBy(selectedNotification)
              }}</span>
            </div>
            <div class="details-meta-item">
              <span class="details-meta-label">Timestamp</span>
              <span class="details-meta-value">{{
                formatAbsoluteTime(selectedNotification.createdDate) || 'Just now'
              }}</span>
            </div>
          </div>

          <div v-if="parsedMessage?.type === 'structured'" class="details-message-structured">
            <p v-if="parsedMessage.summary" class="details-message-summary">
              {{ parsedMessage.summary }}
            </p>
            <div class="details-error-list">
              <div v-for="item in parsedMessage.items" :key="item.index" class="details-error-item">
                <span class="details-error-num">{{ item.index }}</span>
                <div class="details-error-body">
                  <div class="details-error-ids">
                    <span class="details-error-id-label">Doc</span>
                    <code class="details-error-id" :title="item.doc">{{ item.doc }}</code>
                    <span class="details-error-id-sep">·</span>
                    <span class="details-error-id-label">Location</span>
                    <code class="details-error-id" :title="item.location">{{ item.location }}</code>
                  </div>
                  <span class="details-error-reason">{{ item.reason }}</span>
                </div>
              </div>
            </div>
          </div>

          <p v-else class="details-message">{{ parsedMessageText }}</p>
          <div v-if="primaryAction" class="details-action-link-wrap">
            <a href="#" class="details-action-link" @click.prevent="handlePrimaryAction()">
              {{ primaryAction.label }}
            </a>
          </div>
        </template>

        <div v-else class="details-empty">Select a notification to view details.</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { DxButton } from 'devextreme-vue/button';
import { UserNotificationService } from '@/api/services/UserNotificationService';
import { type NotificationFilterTab, useNotificationStore } from '@/store/notification';
import type { AppNotification } from '@/types/notification';
import {
  formatAbsoluteTime,
  formatRelativeTime,
  getNotificationCreatedBy,
  getPrimaryAction,
  getSeverityClass,
  getSeverityValue,
  localizeMessageTimestamps,
  parseNotificationMessage,
} from '@/utils/notificationDisplay';

defineOptions({ name: 'NotificationsAllPage' });

const router = useRouter();
const store = useNotificationStore();

const activeFilterTab = ref<NotificationFilterTab>('all');
const selectedNotificationId = ref<string | null>(null);
const markingAll = ref(false);
const deletingAll = ref(false);
const splitContainer = ref<HTMLElement | null>(null);
const listPanelRef = ref<HTMLElement | null>(null);
const listPanelWidth = ref<number>(420);
const isResizing = ref(false);

const filterTabs = computed(() => [
  { label: 'All', value: 'all' as const, badge: undefined },
  {
    label: 'Unread',
    value: 'unread' as const,
    badge: store.getTabCount('unread') > 0 ? store.getTabCount('unread') : undefined,
  },
  { label: 'Read', value: 'read' as const, badge: undefined },
  { label: 'Error', value: 'error' as const, badge: undefined },
  { label: 'Info', value: 'info' as const, badge: undefined },
  { label: 'Success', value: 'success' as const, badge: undefined },
  { label: 'Warning', value: 'warning' as const, badge: undefined },
]);

const primaryFilterTabs = computed(() => filterTabs.value.slice(0, 3));
const severityFilterTabs = computed(() => filterTabs.value.slice(3));

const filteredNotifications = computed<AppNotification[]>(() => store.filteredNotifications);
const activeTabTotalCount = computed<number>(() => store.getTabCount(activeFilterTab.value));

const selectedNotification = computed<AppNotification | null>(() =>
  !selectedNotificationId.value
    ? (filteredNotifications.value[0] ?? null)
    : (filteredNotifications.value.find(item => item.id === selectedNotificationId.value) ?? null)
);

const primaryAction = computed(() => {
  if (!selectedNotification.value) return null;
  return getPrimaryAction(selectedNotification.value);
});

const parsedMessage = computed(() => {
  if (!selectedNotification.value) return null;
  return parseNotificationMessage(
    selectedNotification.value.message,
    selectedNotification.value.type
  );
});

const parsedMessageText = computed(() => {
  const parsed = parsedMessage.value;
  if (!parsed || parsed.type !== 'plain') return '';
  return parsed.text;
});

// Optimistic update: mark read locally, then refresh both stores on API failure for consistency.
async function markNotificationsAsRead(notificationIds: string[]): Promise<void> {
  const uniqueIds = Array.from(new Set(notificationIds.filter(Boolean)));
  if (uniqueIds.length === 0) return;
  store.markReadLocal(uniqueIds);
  try {
    await UserNotificationService.markAsRead(uniqueIds);
  } catch {
    await Promise.all([store.refreshActiveTabNotifications(), store.refreshNotifications()]);
  }
}

const listPanelStyle = computed(() => ({
  width: `${listPanelWidth.value}px`,
}));

function scrollSelectedIntoView(notificationId: string | null): void {
  if (!notificationId || !listPanelRef.value) return;
  const selectedCard = listPanelRef.value.querySelector<HTMLElement>(
    `[data-notification-id="${notificationId}"]`
  );
  selectedCard?.scrollIntoView({ block: 'nearest' });
}

async function handleListKeydown(event: KeyboardEvent): Promise<void> {
  if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;

  const notifications = filteredNotifications.value;
  if (notifications.length === 0) return;

  event.preventDefault();

  const currentIndex = selectedNotificationId.value
    ? notifications.findIndex(item => item.id === selectedNotificationId.value)
    : -1;

  const nextIndex =
    event.key === 'ArrowDown'
      ? Math.min(currentIndex + 1, notifications.length - 1)
      : Math.max(currentIndex < 0 ? notifications.length - 1 : currentIndex - 1, 0);

  const nextNotification = notifications[nextIndex];
  if (!nextNotification) return;

  await handleSelect(nextNotification);
  scrollSelectedIntoView(nextNotification.id);
}

watch(filteredNotifications, notifications => {
  if (notifications.length === 0) {
    selectedNotificationId.value = null;
    return;
  }
  if (!selectedNotification.value) selectedNotificationId.value = notifications[0].id;
});

async function handleSelect(notification: AppNotification): Promise<void> {
  const previousSelectedId = selectedNotificationId.value;
  selectedNotificationId.value = notification.id;

  if (activeFilterTab.value === 'unread') {
    // Defer mark-as-read until user navigates away; mark the previously viewed one now.
    if (previousSelectedId && previousSelectedId !== notification.id) {
      await markNotificationsAsRead([previousSelectedId]);
    }
    return;
  }

  if (notification.isRead) return;
  await markNotificationsAsRead([notification.id]);
}

async function handleMarkAllRead(): Promise<void> {
  if (store.unreadCount === 0) return;

  markingAll.value = true;
  store.markAllReadLocal();
  try {
    await UserNotificationService.markAllAsRead();
  } catch {
    await store.refreshActiveTabNotifications();
  } finally {
    markingAll.value = false;
  }
}

async function handleDelete(notificationId: string): Promise<void> {
  store.removeNotificationLocal(notificationId);
  if (selectedNotificationId.value === notificationId) {
    selectedNotificationId.value = null;
  }

  try {
    await UserNotificationService.deleteNotification(notificationId);
  } catch {
    await store.refreshActiveTabNotifications();
  }
}

async function handleDeleteAll(): Promise<void> {
  if (activeTabTotalCount.value === 0) return;

  deletingAll.value = true;
  store.removeAllNotificationsLocal();
  selectedNotificationId.value = null;
  try {
    await UserNotificationService.deleteAllNotifications();
  } catch {
    await store.refreshActiveTabNotifications();
  } finally {
    deletingAll.value = false;
  }
}

async function handleFilterTabChange(tab: NotificationFilterTab): Promise<void> {
  if (activeFilterTab.value === tab) {
    return;
  }

  if (activeFilterTab.value === 'unread' && selectedNotificationId.value) {
    await markNotificationsAsRead([selectedNotificationId.value]);
  }

  activeFilterTab.value = tab;
  selectedNotificationId.value = null;
  await store.fetchTabNotifications(tab);
}

function handlePrimaryAction(): void {
  if (!primaryAction.value) return;

  if (primaryAction.value.external) {
    window.open(primaryAction.value.target, '_blank', 'noopener,noreferrer');
    return;
  }

  router.push(primaryAction.value.target);
}

function startResize(event: MouseEvent): void {
  event.preventDefault();
  isResizing.value = true;
  document.addEventListener('mousemove', resizePanels);
  document.addEventListener('mouseup', stopResize);
}

function resizePanels(event: MouseEvent): void {
  if (!isResizing.value || !splitContainer.value) return;

  const containerRect = splitContainer.value.getBoundingClientRect();
  const nextWidth = event.clientX - containerRect.left;
  const minListWidth = 320;
  const minDetailsWidth = 420;
  const maxListWidth = Math.max(minListWidth, containerRect.width - minDetailsWidth);
  listPanelWidth.value = Math.min(Math.max(nextWidth, minListWidth), maxListWidth);
}

function stopResize(): void {
  if (!isResizing.value) return;
  isResizing.value = false;
  document.removeEventListener('mousemove', resizePanels);
  document.removeEventListener('mouseup', stopResize);
}

onMounted(async () => {
  await nextTick();
  if (splitContainer.value) {
    const width = splitContainer.value.getBoundingClientRect().width;
    const preferredWidth = Math.floor(width * 0.34);
    listPanelWidth.value = Math.min(560, Math.max(360, preferredWidth));
  }

  listPanelRef.value?.focus({ preventScroll: true });

  await store.fetchCount();
  await store.fetchTabNotifications(activeFilterTab.value);

  if (!selectedNotificationId.value && filteredNotifications.value[0]) {
    selectedNotificationId.value = filteredNotifications.value[0].id;
  }
});

onUnmounted(() => {
  if (activeFilterTab.value === 'unread' && selectedNotificationId.value) {
    void markNotificationsAsRead([selectedNotificationId.value]);
  }
  stopResize();
});
</script>

<style src="./NotificationsAllPage.css" scoped></style>
