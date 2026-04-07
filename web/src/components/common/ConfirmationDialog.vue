<template>
  <teleport to="body">
    <div v-if="open" class="confirm-overlay" @keydown.esc.prevent="onEsc">
      <div
        ref="cardRef"
        class="confirm-card"
        :style="{ maxWidth: computedMaxWidth }"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="headingId"
        :aria-describedby="descId"
        :aria-busy="loading ? 'true' : 'false'"
        tabindex="-1"
      >
        <h3 class="confirm-title" :id="headingId">{{ finalTitle }}</h3>
        <div v-if="useSlot" class="confirm-slot">
          <slot />
        </div>
        <p v-else-if="finalDescription" class="confirm-description" :id="descId">
          {{ finalDescription }}
        </p>
        <div class="confirm-actions">
          <button class="btn btn-outlined" type="button" @click="onCancel" :disabled="loading">
            {{ cancelLabel }}
          </button>
          <button
            class="btn"
            :class="colorClass"
            type="button"
            @click="onConfirm"
            :disabled="loading"
          >
            <span v-if="loading" class="loader" aria-hidden="true"></span>
            <span>{{ finalConfirmLabel }}</span>
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch, useSlots } from 'vue';

type DialogType =
  | 'enable'
  | 'disable'
  | 'delete'
  | 'test'
  | 'runNow'
  | 'cancel'
  | 'clone'
  | 'custom';

interface BaseConfig {
  title: string;
  description?: string;
  confirmLabel: string;
  confirmColor: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
}

const typeConfigs: Record<DialogType, BaseConfig> = {
  enable: {
    title: 'Are you sure you want to Enable this connection?',
    description:
      'This will enable the Jira connection and allow linked integrations to access Jira. Do you want to continue?',
    confirmLabel: 'Enable',
    confirmColor: 'success',
  },
  disable: {
    title: 'Are you sure you want to Disable this connection?',
    description: 'Disabling this connection will stop all linked integrations from accessing Jira.',
    confirmLabel: 'Disable',
    confirmColor: 'error',
  },
  delete: {
    title: 'Delete Confirmation',
    description: 'Deleting this connection will stop all linked integrations from accessing Jira.',
    confirmLabel: 'Delete',
    confirmColor: 'error',
  },
  test: {
    title: 'Test Connection Confirmation',
    description:
      'This will test the Jira connection using the selected credentials to verify connectivity. Do you want to continue?',
    confirmLabel: 'Test Run',
    confirmColor: 'warning',
  },
  runNow: {
    title: 'Run Now',
    description: 'Execute immediately without altering schedule.',
    confirmLabel: 'Run',
    confirmColor: 'warning',
  },
  cancel: {
    title: 'Cancel Execution',
    description: 'Attempt to abort the current running job.',
    confirmLabel: 'Cancel Job',
    confirmColor: 'error',
  },
  clone: {
    title: 'Clone Connection',
    description: 'Create a copy of this connection.',
    confirmLabel: 'Clone',
    confirmColor: 'primary',
  },
  custom: {
    title: 'Confirm Action',
    description: '',
    confirmLabel: 'Confirm',
    confirmColor: 'primary',
  },
};

const props = defineProps({
  open: { type: Boolean, required: true },
  type: { type: String as () => DialogType, default: 'custom' },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  cancelLabel: { type: String, default: 'Cancel' },
  confirmLabel: { type: String, default: '' },
  confirmColor: {
    type: String as () => 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success',
    default: '',
  },
  maxWidth: { type: [Number, String], default: 400 },
  loading: { type: Boolean, default: false },
  autoFocus: { type: Boolean, default: true },
});

const emits = defineEmits(['cancel', 'confirm']);

const slots = useSlots();
const useSlot = computed(() => !!slots.default);

const base = computed(() => typeConfigs[props.type as DialogType] || typeConfigs.custom);
const finalTitle = computed(() => props.title || base.value.title);
const finalDescription = computed(() => props.description || base.value.description);
const finalConfirmLabel = computed(() => props.confirmLabel || base.value.confirmLabel);
const finalConfirmColor = computed(() => props.confirmColor || base.value.confirmColor);

const computedMaxWidth = computed(() =>
  typeof props.maxWidth === 'number' ? props.maxWidth + 'px' : props.maxWidth
);
const colorClass = computed(() => `btn-${finalConfirmColor.value}`);

const headingId = `dialog-title-${Math.random().toString(36).slice(2)}`;
const descId = `dialog-desc-${Math.random().toString(36).slice(2)}`;

// Use generic HTMLElement to avoid ESLint no-undef on HTMLDivElement in Vue SFC parsing
const cardRef = ref<HTMLElement | null>(null);

onMounted(() => {
  if (props.open && props.autoFocus && cardRef.value) {
    cardRef.value.focus();
  }
});
watch(
  () => props.open,
  val => {
    if (val && props.autoFocus && cardRef.value) {
      setTimeout(() => cardRef.value?.focus(), 0);
    }
  }
);

function onCancel() {
  emits('cancel');
}
function onConfirm() {
  if (!props.loading) emits('confirm');
}
function onEsc() {
  onCancel();
}
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 16px;
}
.confirm-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px 24px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  width: 100%;
}
.confirm-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #000;
}
.confirm-description,
.confirm-slot {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.4;
  color: #424242;
}
.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.btn {
  border: none;
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
  padding: 8px 14px;
  border-radius: 6px;
  font-weight: 500;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}
.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.btn-outlined {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #374151;
}
.btn-outlined:hover:not(:disabled) {
  background: #f1f5f9;
}

/* Color variants */
/* Primary */
.btn-primary {
  background: #2563eb;
  color: #fff;
}
.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-secondary {
  background: #64748b;
  color: #fff;
}
.btn-secondary:hover:not(:disabled) {
  background: #475569;
}

.btn-success {
  background: #16a34a;
  color: #fff;
}
.btn-success:hover:not(:disabled) {
  background: #15803d;
}

.btn-error {
  background: #dc2626;
  color: #fff;
}
.btn-error:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-warning {
  background: #d97706;
  color: #fff;
}
.btn-warning:hover:not(:disabled) {
  background: #b45309;
}

.btn-info {
  background: #0ea5e9;
  color: #fff;
}
.btn-info:hover:not(:disabled) {
  background: #0284c7;
}

/* Loader */
.loader {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-top-color: #fff;
  animation: spin 0.7s linear infinite;
  vertical-align: middle;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
