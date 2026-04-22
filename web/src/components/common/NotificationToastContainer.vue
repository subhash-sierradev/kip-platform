<template>
  <Teleport to="body">
    <div class="ntf-stack" aria-live="polite" aria-label="Incoming notifications">
      <TransitionGroup name="ntf" tag="div" class="ntf-group">
        <div
          v-for="toast in notificationStore.notificationToasts"
          :key="toast.id"
          class="ntf-card"
          :class="`ntf-card--${toast.severity.toLowerCase()}`"
          role="alert"
        >
          <div class="ntf-header">
            <component :is="BellRing" :size="14" class="ntf-bell" />
            <span class="ntf-label">Notification</span>
            <span class="ntf-severity-chip">{{ toast.severity }}</span>
            <button
              class="ntf-close"
              aria-label="Dismiss"
              @click="notificationStore.removeNotificationToast(toast.id)"
            >
              <component :is="X" :size="13" />
            </button>
          </div>

          <div class="ntf-body">
            <p class="ntf-title">{{ toast.title }}</p>
            <p v-if="toast.message && toast.message !== toast.title" class="ntf-message">
              {{ formatToastMessage(toast.message) }}
            </p>
            <a
              v-if="getToastPrimaryAction(toast)"
              href="#"
              class="ntf-action-link"
              @click.prevent="handleToastPrimaryAction(toast)"
            >
              {{ getToastPrimaryAction(toast)?.label }}
            </a>
          </div>

          <div class="ntf-progress">
            <div class="ntf-progress-bar" />
          </div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { BellRing, X } from 'lucide-vue-next';
import { useRouter } from 'vue-router';

import { useAuthStore } from '@/store/auth';
import { useNotificationStore } from '@/store/notification';
import type { AppNotification } from '@/types/notification';
import { getPrimaryAction } from '@/utils/notificationDisplay';
import { localizeEmbeddedIsoTimestamps } from '@/utils/notificationMessageUtils';

const router = useRouter();
const notificationStore = useNotificationStore();
const authStore = useAuthStore();

function formatToastMessage(message: string): string {
  return localizeEmbeddedIsoTimestamps(message);
}

function getToastPrimaryAction(notification: AppNotification) {
  return getPrimaryAction(notification, authStore.userRoles);
}

function handleToastPrimaryAction(notification: AppNotification): void {
  const action = getToastPrimaryAction(notification);
  if (!action) return;

  if (action.external) {
    window.open(action.target, '_blank', 'noopener,noreferrer');
    return;
  }

  router.push(action.target);
}
</script>

<style scoped>
/* ── Stack container ─────────────────────────────────────── */
.ntf-stack {
  position: fixed;
  bottom: 16px;
  right: 16px;
  z-index: 1050;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.ntf-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ── Card ────────────────────────────────────────────────── */
.ntf-card {
  pointer-events: all;
  width: 340px;
  background: #2c3e50;
  border-radius: 10px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.28),
    0 2px 8px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  border-left: 4px solid #f59e0b;
}

.ntf-card--info {
  border-left-color: #3b82f6;
}
.ntf-card--success {
  border-left-color: #10b981;
}
.ntf-card--warning {
  border-left-color: #f59e0b;
}
.ntf-card--error {
  border-left-color: #ef4444;
}

/* ── Header ──────────────────────────────────────────────── */
.ntf-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.ntf-bell {
  color: #f59e0b;
  flex-shrink: 0;
}

.ntf-card--info .ntf-bell {
  color: #3b82f6;
}
.ntf-card--success .ntf-bell {
  color: #10b981;
}
.ntf-card--warning .ntf-bell {
  color: #f59e0b;
}
.ntf-card--error .ntf-bell {
  color: #ef4444;
}

.ntf-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #bdc3c7;
  flex: 1;
}

.ntf-severity-chip {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  color: #95a5a6;
}

.ntf-card--info .ntf-severity-chip {
  color: #93c5fd;
  background: rgba(59, 130, 246, 0.15);
}

.ntf-card--success .ntf-severity-chip {
  color: #6ee7b7;
  background: rgba(16, 185, 129, 0.15);
}

.ntf-card--warning .ntf-severity-chip {
  color: #fcd34d;
  background: rgba(245, 158, 11, 0.15);
}

.ntf-card--error .ntf-severity-chip {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.15);
}

.ntf-close {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: #7f8c8d;
  padding: 2px;
  border-radius: 4px;
  flex-shrink: 0;
  transition:
    color 0.15s,
    background 0.15s;
}

.ntf-close:hover {
  color: #ecf0f1;
  background: rgba(255, 255, 255, 0.1);
}

/* ── Body ────────────────────────────────────────────────── */
.ntf-body {
  padding: 8px 12px 12px;
}

.ntf-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #ecf0f1;
  line-height: 1.4;
}

.ntf-message {
  margin: 4px 0 0;
  font-size: 12px;
  color: #bdc3c7;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ntf-action-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  font-size: 11.5px;
  font-weight: 600;
  color: #f59e0b;
  text-decoration: none;
  border-bottom: 1px solid rgba(245, 158, 11, 0.35);
  padding-bottom: 1px;
  transition:
    color 0.15s ease,
    border-color 0.15s ease;
}

.ntf-action-link::after {
  content: '↗';
  font-size: 10px;
  opacity: 0.7;
}

.ntf-card--info .ntf-action-link {
  color: #93c5fd;
  border-bottom-color: rgba(147, 197, 253, 0.35);
}

.ntf-card--success .ntf-action-link {
  color: #6ee7b7;
  border-bottom-color: rgba(110, 231, 183, 0.35);
}

.ntf-card--warning .ntf-action-link {
  color: #fcd34d;
  border-bottom-color: rgba(252, 211, 77, 0.35);
}

.ntf-card--error .ntf-action-link {
  color: #fca5a5;
  border-bottom-color: rgba(252, 165, 165, 0.35);
}

.ntf-action-link:hover {
  color: #fff;
  border-bottom-color: rgba(255, 255, 255, 0.45);
}

/* ── Progress bar ────────────────────────────────────────── */
.ntf-progress {
  height: 3px;
  background: rgba(255, 255, 255, 0.06);
}

.ntf-progress-bar {
  height: 100%;
  background: #f59e0b;
  animation: ntf-drain 6s linear forwards;
  transform-origin: left;
}

.ntf-card--info .ntf-progress-bar {
  background: #3b82f6;
}
.ntf-card--success .ntf-progress-bar {
  background: #10b981;
}
.ntf-card--warning .ntf-progress-bar {
  background: #f59e0b;
}
.ntf-card--error .ntf-progress-bar {
  background: #ef4444;
}

@keyframes ntf-drain {
  from {
    transform: scaleX(1);
  }
  to {
    transform: scaleX(0);
  }
}

/* ── Transition animations ───────────────────────────────── */
.ntf-enter-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.ntf-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.ntf-enter-from {
  opacity: 0;
  transform: translateX(110%) scale(0.92);
}

.ntf-leave-to {
  opacity: 0;
  transform: translateX(110%) scale(0.92);
}

.ntf-move {
  transition: transform 0.2s ease;
}
</style>
