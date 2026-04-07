<template>
  <div class="notification-panel" @click.stop>
    <div class="notification-panel-header">
      <span class="notification-panel-title">Recent Notifications</span>
      <div class="notification-panel-actions">
        <DxButton
          class="mark-all-read-btn"
          text="Mark all read"
          styling-mode="text"
          :disabled="markingAll || notificationStore.unreadCount === 0"
          @click="handleMarkAllRead"
        />
      </div>
    </div>

    <div v-if="notifications.length === 0" class="notification-empty">
      <Bell class="notification-bell" aria-hidden="true" />
      <p>No notifications</p>
    </div>

    <ul v-else class="notification-list" :class="{ 'notification-list-scroll': hasMoreThanThree }">
      <li
        v-for="n in notifications"
        :key="n.id"
        class="notification-item-row"
        :class="[{ unread: !n.isRead }, `notification-item-row--${getSeverityClass(n.severity)}`]"
        @click="handleSelect(n)"
      >
        <div class="notification-row-body">
          <div class="notification-row-header">
            <span class="notification-row-title">{{ n.title }}</span>
            <span class="notification-row-time" :title="formatAbsoluteTime(n.createdDate)">
              {{ formatRelativeTime(n.createdDate) || 'Just now' }}
            </span>
          </div>
          <span class="notification-row-message">
            {{ truncateMessage(n.message) }}
          </span>
        </div>
        <div class="notification-row-actions">
          <button
            class="row-action-btn danger"
            aria-label="Delete notification"
            @click.stop="handleDelete(n.id)"
          >
            <i class="dx-icon dx-icon-close" aria-hidden="true"></i>
          </button>
        </div>
      </li>
    </ul>

    <div v-if="notifications.length > 0" class="notification-panel-footer">
      <button class="view-all-btn" @click="handleViewAll">Show All Notifications</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { DxButton } from 'devextreme-vue/button';
import { Bell } from 'lucide-vue-next';
import { UserNotificationService } from '@/api/services/UserNotificationService';
import { useNotificationStore } from '@/store/notification';
import type { AppNotification } from '@/types/notification';
import {
  formatAbsoluteTime,
  formatRelativeTime,
  getSeverityClass,
  localizeMessageTimestamps,
} from '@/utils/notificationDisplay';

const emit = defineEmits<{ close: [] }>();
const router = useRouter();
const notificationStore = useNotificationStore();

const markingAll = ref(false);

const notifications = computed<AppNotification[]>(() => notificationStore.notifications);
const hasMoreThanThree = computed(() => notifications.value.length > 3);

function handleViewAll(): void {
  emit('close');
  router.push('/notifications');
}

async function handleSelect(notification: AppNotification): Promise<void> {
  if (notification.isRead) return;

  notificationStore.markReadLocal([notification.id]);
  try {
    await UserNotificationService.markAsRead([notification.id]);
  } catch {
    await notificationStore.refreshNotifications();
  }
}

async function handleMarkAllRead(): Promise<void> {
  if (notificationStore.unreadCount === 0) return;

  markingAll.value = true;
  notificationStore.markAllReadLocal();
  try {
    await UserNotificationService.markAllAsRead();
  } catch {
    await notificationStore.refreshNotifications();
  } finally {
    markingAll.value = false;
  }
}

async function handleDelete(notificationId: string): Promise<void> {
  notificationStore.removeNotificationLocal(notificationId);
  try {
    await UserNotificationService.deleteNotification(notificationId);
  } catch {
    await notificationStore.refreshNotifications();
  }
}

function truncateMessage(message: string): string {
  const localizedMessage = localizeMessageTimestamps(message);
  if (!localizedMessage) return '';
  if (localizedMessage.length <= 50) return localizedMessage;
  return `${localizedMessage.slice(0, 50)}...`;
}
</script>

<style scoped>
.notification-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 450px;
  max-height: 840px;
  background: var(--kw-panel-bg);
  border-radius: 10px;
  border: 1px solid var(--kw-panel-border);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 1000;
}

.notification-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--kw-panel-border);
}

.notification-panel-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--kw-layout-color);
}

.notification-panel-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

:deep(.mark-all-read-btn.dx-button) {
  background: color-mix(in srgb, var(--kw-panel-border) 45%, var(--kw-panel-bg));
  border: 1px solid color-mix(in srgb, var(--kw-panel-border) 78%, var(--kw-layout-text-muted));
  border-radius: 8px;
  color: var(--kw-layout-color);
  min-height: 34px;
  padding: 0 10px;
}

:deep(.mark-all-read-btn.dx-button.dx-state-disabled) {
  opacity: 0.6;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 34px 16px;
  color: var(--kw-text-muted);
  gap: 8px;
}

.notification-bell {
  width: 32px;
  height: 32px;
  opacity: 0.45;
}

.notification-list {
  list-style: none;
  margin: 0;
  padding: 12px;
  overflow-y: auto;
  flex: 1;
}

.notification-list.notification-list-scroll {
  max-height: 332px;
}

.notification-item-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  border: none;
  border-left: 4px solid var(--kw-panel-border);
  border-radius: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  background: var(--kw-content-bg);
  box-shadow: 0 1px 4px var(--kw-shadow);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-left-color 0.2s ease;
}

.notification-item-row:hover {
  box-shadow: 0 2px 8px var(--kw-shadow);
  transform: translateY(-1px);
}

.notification-item-row.unread {
  background: color-mix(in srgb, var(--kw-layout-text-muted) 8%, var(--kw-content-bg));
}

.notification-item-row--info {
  border-left-color: #3b82f6;
}

.notification-item-row--success {
  border-left-color: #10b981;
}

.notification-item-row--warning {
  border-left-color: #f59e0b;
}

.notification-item-row--error {
  border-left-color: #ef4444;
}

.notification-row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.notification-row-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.notification-row-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--kw-layout-color);
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-row-message {
  font-size: 0.84rem;
  color: var(--kw-secondary-dark);
  line-height: 1.45;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-row-time {
  font-size: 0.8rem;
  color: var(--kw-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.notification-row-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-action-btn {
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--kw-layout-color);
  font-size: 0.82rem;
  font-weight: 600;
  width: 36px;
  height: 36px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.row-action-btn:hover {
  background: transparent;
}

.row-action-btn.danger {
  color: var(--kw-accent-orange-dark);
}

.notification-panel-footer {
  border-top: 1px solid var(--kw-panel-border);
  padding: 12px 16px;
  display: flex;
  justify-content: center;
}

.view-all-btn {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--kw-accent-orange);
  background: none;
  border: none;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .notification-panel {
    width: min(540px, calc(100vw - 24px));
    right: -8px;
  }
}
</style>
