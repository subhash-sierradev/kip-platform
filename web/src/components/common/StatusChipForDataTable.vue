<template>
  <span
    class="kw-status-chip"
    :class="`kw-status-chip--${resolvedVariant}`"
    :aria-label="resolvedAriaLabel"
  >
    {{ resolvedLabel }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

export type StatusChipVariant = 'success' | 'error' | 'neutral' | 'warning';

interface Props {
  status?: string | null;
  label?: string | null;
  variant?: StatusChipVariant;
  ariaLabel?: string;
}

const props = withDefaults(defineProps<Props>(), {
  status: null,
  label: null,
  variant: undefined,
  ariaLabel: '',
});

const successValues = new Set([
  'SUCCESS',
  'CREATE',
  'CREATED',
  'UPDATE',
  'UPDATED',
  'ENABLED',
  'ACTIVE',
  'COMPLETE',
  'COMPLETED',
]);

const errorValues = new Set([
  'FAILED',
  'FAIL',
  'ERROR',
  'REQUIRED',
  'DELETE',
  'DELETED',
  'DISABLED',
  'INACTIVE',
]);

const warningValues = new Set([
  'EXECUTE',
  'RUN_NOW',
  'RUNNING',
  'IN_PROGRESS',
  'RETRY',
  'PENDING',
  'QUEUED',
]);

const resolvedLabel = computed(() => {
  const label = props.label ?? props.status ?? 'UNKNOWN';
  if (label === null || label === undefined || String(label).trim() === '') {
    return 'UNKNOWN';
  }
  return String(label);
});

const resolvedStatusKey = computed(() => {
  return String(props.status ?? props.label ?? '')
    .trim()
    .toUpperCase();
});

const resolvedVariant = computed<StatusChipVariant>(() => {
  if (props.variant) {
    return props.variant;
  }

  const statusKey = resolvedStatusKey.value;
  if (successValues.has(statusKey)) {
    return 'success';
  }
  if (errorValues.has(statusKey)) {
    return 'error';
  }
  if (warningValues.has(statusKey)) {
    return 'warning';
  }
  return 'neutral';
});

const resolvedAriaLabel = computed(() => {
  return props.ariaLabel || resolvedLabel.value;
});
</script>

<style scoped>
.kw-status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  line-height: 1;
  white-space: nowrap;
  border: 1px solid transparent;
}

.kw-status-chip--success {
  background: #dcfce7;
  color: #166534;
  border-color: #bbf7d0;
}

.kw-status-chip--error {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fecaca;
}

.kw-status-chip--neutral {
  background: #f3f4f6;
  color: #475569;
  border-color: #e5e7eb;
}

.kw-status-chip--warning {
  background: #fef3c7;
  color: #92400e;
  border-color: #fde68a;
}
</style>
