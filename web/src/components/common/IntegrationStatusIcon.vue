<template>
  <span style="display: contents">
    <i
      v-if="entry"
      :class="[entry.icon, 'integration-status-icon']"
      :style="{ color: entry.color }"
      tabindex="0"
      :aria-describedby="tooltipVisible ? tooltipId : undefined"
      @mouseenter="onEnter"
      @mousemove="onMove"
      @mouseleave="onLeave"
      @focus="onFocus"
      @blur="onBlur"
    ></i>
    <Tooltip
      v-if="entry"
      :id="tooltipId"
      :visible="tooltipVisible"
      :x="tooltipX"
      :y="tooltipY"
      :text="entry.tooltip"
    />
  </span>
</template>

<script setup lang="ts">
import { computed, ref, useId } from 'vue';
import Tooltip from '@/components/common/Tooltip.vue';

const props = defineProps<{ status?: string | null }>();

const tooltipId = `status-tooltip-${useId()}`;

const STATUS_MAP: Record<string, { icon: string; color: string; tooltip: string }> = {
  SUCCESS: { icon: 'dx-icon-check', color: '#28a745', tooltip: 'Execution completed successfully' },
  FAILED: {
    icon: 'dx-icon-close',
    color: '#dc3545',
    tooltip: 'Execution failed — check logs for details',
  },
  RUNNING: { icon: 'dx-icon-refresh', color: '#007bff', tooltip: 'Currently executing' },
  RETRYING: {
    icon: 'dx-icon-refresh',
    color: '#007bff',
    tooltip: 'Retrying after a transient failure',
  },
  ABORTED: { icon: 'dx-icon-remove', color: '#ff9800', tooltip: 'Execution was manually aborted' },
  SCHEDULED: {
    icon: 'dx-icon-clock',
    color: '#6c757d',
    tooltip: 'Scheduled — waiting for executor to pick up',
  },
  PENDING: { icon: 'dx-icon-clock', color: '#6c757d', tooltip: 'Pending — queued for processing' },
};

const entry = computed(() => STATUS_MAP[(props.status ?? '').toUpperCase()] ?? null);

const tooltipVisible = ref(false);
const tooltipX = ref(0);
const tooltipY = ref(0);

function onEnter(e: MouseEvent) {
  tooltipVisible.value = true;
  tooltipX.value = e.clientX + 12;
  tooltipY.value = e.clientY + 12;
}

function onMove(e: MouseEvent) {
  tooltipX.value = e.clientX + 12;
  tooltipY.value = e.clientY + 12;
}

function onLeave() {
  tooltipVisible.value = false;
}

function onFocus(e: FocusEvent) {
  const rect = (e.target as HTMLElement).getBoundingClientRect();
  tooltipVisible.value = true;
  tooltipX.value = rect.right + 12;
  tooltipY.value = rect.top;
}

function onBlur() {
  tooltipVisible.value = false;
}
</script>
