<template>
  <Teleport to="body">
    <Transition name="loader">
      <div v-if="isLoading" class="subtle-loader-overlay">
        <div class="subtle-loader-content">
          <div class="subtle-spinner"></div>
          <div class="subtle-loader-message">{{ loadingMessage }}</div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { useGlobalLoading } from '../../composables/useGlobalLoading';

const { isLoading, loadingMessage } = useGlobalLoading();
</script>

<style scoped>
.subtle-loader-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(1px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  pointer-events: none;
}

.subtle-loader-content {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  padding: 16px 24px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  gap: 12px;
  backdrop-filter: blur(10px);
  max-width: 280px;
}

.subtle-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid color-mix(in srgb, var(--color-primary-500) 10%, transparent);
  border-top: 2px solid var(--color-primary-500);
  border-radius: 50%;
  animation: subtleSpin 1s linear infinite;
  flex-shrink: 0;
}

.subtle-loader-message {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

@keyframes subtleSpin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Minimal transition animations */
.loader-enter-active {
  transition: all 0.2s ease-out;
}

.loader-leave-active {
  transition: all 0.15s ease-in;
}

.loader-enter-from {
  opacity: 0;
}

.loader-leave-to {
  opacity: 0;
}

.loader-enter-to,
.loader-leave-from {
  opacity: 1;
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .subtle-loader-overlay {
    background: rgba(0, 0, 0, 0.6);
  }

  .subtle-loader-content {
    background: rgba(30, 41, 59, 0.95);
    border-color: rgba(255, 255, 255, 0.05);
  }

  .subtle-loader-message {
    color: #cbd5e1;
  }

  .subtle-spinner {
    border-color: color-mix(in srgb, var(--color-primary-500) 15%, transparent);
    border-top-color: var(--color-primary-500);
  }
}

/* Mobile adjustments */
@media (max-width: 480px) {
  .subtle-loader-content {
    padding: 12px 20px;
    max-width: 240px;
  }

  .subtle-spinner {
    width: 16px;
    height: 16px;
  }

  .subtle-loader-message {
    font-size: 13px;
  }
}
</style>
