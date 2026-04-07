<template>
  <teleport to="body">
    <div class="toast-container">
      <transition-group name="toast" tag="div" class="toast-list">
        <div
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          :class="['toast', `toast-${toast.type}`]"
        >
          <div class="toast-icon">
            <component :is="getToastIcon(toast.type)" :size="20" />
          </div>

          <div class="toast-content">
            <p class="toast-message">{{ toast.message }}</p>
          </div>

          <button
            class="toast-close"
            aria-label="Close notification"
            @click="toastStore.hideToast(toast.id)"
          >
            <X :size="16" />
          </button>
        </div>
      </transition-group>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { X } from 'lucide-vue-next';

import { useToastStore } from '../../store/toast';
import { getToastIcon } from '../../utils/notificationUtils';

const toastStore = useToastStore();

// Initialize the global toast function when component mounts
onMounted(() => {
  toastStore.initializeToast();
});

// Clear all toasts when component unmounts
onUnmounted(() => {
  toastStore.clearAllToasts();
});
</script>

<script lang="ts">
export default {
  name: 'ToastContainer',
};
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 1000;
  max-width: 400px;
  width: auto;
}

.toast-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toast {
  display: flex;
  align-items: flex-start;
  min-width: 300px;
  max-width: 400px;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
}

.toast-success {
  background-color: #f0f9f0;
  border-color: #86efac;
  color: #065f46;
}

.toast-error {
  background-color: #fef2f2;
  border-color: #fca5a5;
  color: #7f1d1d;
}

.toast-warning {
  background-color: #fef2f2;
  border-color: #fb7185;
  color: #991b1b;
}

.toast-info {
  background-color: #f0f8ff;
  border-color: #93c5fd;
  color: #1e40af;
}

.toast-icon {
  flex-shrink: 0;
  margin-right: 12px;
  margin-top: 1px;
}

.toast-success .toast-icon {
  color: #16a34a;
}

.toast-error .toast-icon {
  color: #dc2626;
}

.toast-warning .toast-icon {
  color: #dc2626;
}

.toast-info .toast-icon {
  color: #2563eb;
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-message {
  margin: 0;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
}

.toast-close {
  flex-shrink: 0;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  margin-left: 8px;
  border-radius: 4px;
  opacity: 0.7;
  transition: all 0.2s ease;
  color: inherit;
}

.toast-close:hover {
  opacity: 1;
  background-color: rgba(0, 0, 0, 0.05);
}

/* Transition animations */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.95);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.95);
}

.toast-move {
  transition: transform 0.3s ease;
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .toast-container {
    top: 8px;
    right: 8px;
    left: 8px;
    max-width: none;
  }

  .toast {
    min-width: 0;
    max-width: none;
    width: 100%;
  }
}
</style>
