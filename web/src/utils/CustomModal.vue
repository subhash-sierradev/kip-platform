<template>
  <teleport to="body">
    <div v-if="modelValue" class="modal-overlay" @click="handleOverlayClick">
      <div class="modal-container" :style="{ width: width, maxWidth: maxWidth }" @click.stop>
        <!-- Modal Header -->
        <div class="modal-header">
          <h2 class="modal-title">{{ title }}</h2>
          <button class="modal-close-btn" aria-label="Close modal" @click="closeModal">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <!-- Modal Content -->
        <div class="modal-content">
          <slot />
        </div>

        <!-- Modal Footer (Optional) -->
        <div v-if="$slots.footer" class="modal-footer">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { nextTick, watch } from 'vue';

interface Props {
  modelValue: boolean;
  title: string;
  width?: string;
  maxWidth?: string;
  closeOnOverlay?: boolean;
}

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  close: [];
}>();

const props = withDefaults(defineProps<Props>(), {
  width: '500px',
  maxWidth: '90vw',
  closeOnOverlay: true,
});

const closeModal = () => {
  emit('update:modelValue', false);
  emit('close');
};

const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    closeModal();
  }
};

// Handle escape key
const handleEscapeKey = (event: { key: string }) => {
  if (event.key === 'Escape' && props.modelValue) {
    closeModal();
  }
};

// Watch for modal state changes
watch(
  () => props.modelValue,
  async isOpen => {
    if (isOpen) {
      await nextTick();
      document.addEventListener('keydown', handleEscapeKey);
      document.body.style.overflow = 'hidden';
    } else {
      document.removeEventListener('keydown', handleEscapeKey);
      document.body.style.overflow = 'auto';
    }
  }
);
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
  animation: fadeIn 0.2s ease-out;
}

.modal-container {
  background: white;
  border-radius: 0.75rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s ease-out;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.modal-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.modal-close-btn {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.modal-close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-close-btn:focus {
  outline: none;
  background: #f3f4f6;
  box-shadow: 0 0 0 2px #3b82f6;
}

.modal-content {
  padding: 18px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 1rem;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-1rem) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .modal-container {
    margin: 0.5rem;
    width: calc(100% - 1rem) !important;
    max-width: none !important;
  }

  .modal-header {
    padding: 0.5rem 1.5rem 0.75rem;
  }

  .modal-content {
    padding: 1rem 1.5rem;
  }

  .modal-footer {
    padding: 0.75rem 1.5rem 1rem;
  }

  .modal-title {
    font-size: 1.125rem;
  }
}
</style>
