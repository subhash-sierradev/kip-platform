<template>
  <div ref="bellRef" class="notification-container" @click.stop="togglePanel">
    <div class="notification-item" :class="{ 'has-notifications': unreadCount > 0 }">
      <Bell :size="18" :stroke-width="1.75" class="notification-bell" />
      <span v-if="unreadCount > 0" class="notification-badge">
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </div>

    <Transition name="panel">
      <NotificationPanel v-if="showPanel" @close="showPanel = false" />
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Bell } from 'lucide-vue-next';
import NotificationPanel from './NotificationPanel.vue';
import { useNotificationStore } from '@/store/notification';

const notificationStore = useNotificationStore();
const showPanel = ref(false);
const bellRef = ref<HTMLElement | null>(null);

const unreadCount = computed(() => notificationStore.unreadCount);

function togglePanel(): void {
  showPanel.value = !showPanel.value;
}

function handleClickOutside(event: Event): void {
  const target = event.target as Node;
  if (bellRef.value && !bellRef.value.contains(target)) {
    showPanel.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.notification-container {
  position: relative;
  cursor: pointer;
}

.notification-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  transition: background 0.2s;
}

.notification-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.notification-item.has-notifications {
  background: rgba(245, 158, 11, 0.1);
}

.notification-item.has-notifications:hover {
  background: rgba(245, 158, 11, 0.18);
}

.notification-bell {
  color: rgba(255, 255, 255, 0.75);
  display: block;
  transition: color 0.2s;
}

.notification-item.has-notifications .notification-bell {
  color: #f59e0b;
}

.notification-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #f59e0b;
  color: #ffffff;
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.01em;
  line-height: 1;
  min-width: 17px;
  height: 17px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 2px solid var(--topbar-bg, #1a1a2e);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
}

/* Panel slide-down transition */
.panel-enter-active,
.panel-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
