<template>
  <span :class="['status-badge', statusClass, sizeClass]" :aria-label="ariaLabel">
    <i v-if="showIcon" :class="iconClass" class="status-icon" aria-hidden="true"></i>
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

type StatusType =
  | 'success'
  | 'failed'
  | 'active'
  | 'enabled'
  | 'disabled'
  | 'warning'
  | 'info'
  | 'neutral';
type BadgeSize = 'small' | 'medium' | 'large';

interface StatusBadgeProps {
  /** Status type */
  status?: StatusType | string;
  /** Custom label override */
  customLabel?: string;
  /** Badge size */
  size?: BadgeSize;
  /** Whether to show status icon */
  showIcon?: boolean;
  /** Custom aria label for accessibility */
  customAriaLabel?: string;
}

const props = withDefaults(defineProps<StatusBadgeProps>(), {
  status: 'neutral',
  customLabel: undefined,
  size: 'medium',
  showIcon: false,
  customAriaLabel: undefined,
});

const statusClass = computed(() => {
  const normalizedStatus = (props.status || '').toLowerCase();
  switch (normalizedStatus) {
    case 'success':
      return 'success';
    case 'failed':
    case 'error':
    case 'danger':
      return 'failed';
    case 'active':
    case 'enabled':
      return 'active';
    case 'disabled':
    case 'inactive':
      return 'disabled';
    case 'warning':
      return 'warning';
    case 'info':
      return 'info';
    default:
      return 'neutral';
  }
});

const sizeClass = computed(() => `size-${props.size}`);

const label = computed(() => {
  if (props.customLabel) return props.customLabel;

  const normalizedStatus = (props.status || '').toLowerCase();
  switch (normalizedStatus) {
    case 'success':
      return 'Success';
    case 'failed':
      return 'Failed';
    case 'active':
      return 'Active';
    case 'enabled':
      return 'Enabled';
    case 'disabled':
      return 'Disabled';
    case 'warning':
      return 'Warning';
    case 'info':
      return 'Info';
    default:
      return props.status || 'Unknown';
  }
});

const iconClass = computed(() => {
  const normalizedStatus = statusClass.value;
  switch (normalizedStatus) {
    case 'success':
    case 'active':
      return 'dx-icon dx-icon-check';
    case 'failed':
      return 'dx-icon dx-icon-close';
    case 'disabled':
      return 'dx-icon dx-icon-cursorprohibition';
    case 'warning':
      return 'dx-icon dx-icon-warning';
    case 'info':
      return 'dx-icon dx-icon-info';
    default:
      return 'dx-icon dx-icon-help';
  }
});

const ariaLabel = computed(() => {
  if (props.customAriaLabel) return props.customAriaLabel;
  return `Status: ${label.value}`;
});
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-weight: 500;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

/* Size variants */
.size-small {
  padding: 0.125rem 0.375rem;
  font-size: 0.625rem;
  letter-spacing: 0.03em;
}

.size-medium {
  padding: 0.125rem 0.5rem;
  font-size: 0.625rem;
  letter-spacing: 0.03em;
}

.size-large {
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  letter-spacing: 0.025em;
}

/* Status variants */
.success {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
  border-color: #22c55e;
  box-shadow: 0 1px 3px rgba(34, 197, 94, 0.15);
}

.failed {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
  border-color: #ef4444;
  box-shadow: 0 1px 3px rgba(239, 68, 68, 0.15);
}

.active {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
  border-color: #22c55e;
  box-shadow: 0 1px 3px rgba(34, 197, 94, 0.15);
}

.disabled {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #6b7280;
  border-color: #9ca3af;
  box-shadow: 0 1px 3px rgba(156, 163, 175, 0.15);
}

.warning {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border-color: #f59e0b;
  box-shadow: 0 1px 3px rgba(245, 158, 11, 0.15);
}

.info {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
  border-color: #3b82f6;
  box-shadow: 0 1px 3px rgba(59, 130, 246, 0.15);
}

.neutral {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #64748b;
  border-color: #cbd5e1;
  box-shadow: 0 1px 3px rgba(203, 213, 225, 0.15);
}

.status-icon {
  font-size: 0.75em;
}
</style>
