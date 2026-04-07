<template>
  <div class="integration-card" @click="onOpen">
    <div class="card-top-row">
      <div class="icon-box">
        <i class="dx-icon-doc" style="font-size: 24px"></i>
      </div>
      <div class="title-box" style="flex: 1">
        <div class="title-text">{{ integration.name || 'Unnamed Integration' }}</div>
        <div class="created-text">
          <span class="label">Created:</span> {{ formatMetadataDate(integration.createdDate) }}
        </div>
      </div>

      <ActionMenu :items="menuItems" @action="onAction" trigger-aria-label="Integration options" />
    </div>

    <div class="chips-row" style="margin-bottom: 10px">
      <span
        class="status-chip"
        :class="integration.isEnabled ? 'status-active' : 'status-disabled'"
      >
        {{ integration.isEnabled ? 'Enabled' : 'Disabled' }}
      </span>
      <span class="environment-chip">{{
        integration.itemSubtypeLabel || integration.itemSubtype
      }}</span>
      <span v-if="dynamicDocumentTypeDisplay" class="dynamic-doc-chip">
        {{ dynamicDocumentTypeDisplay }}
      </span>
      <span v-if="languageChipText" class="environment-chip">
        {{ languageChipText }}
      </span>
    </div>

    <div class="info-row">
      <div class="info-cell">
        <span class="label">Schedule:</span>
        <span class="value">{{ scheduleText }}</span>
      </div>
    </div>

    <div class="info-row">
      <div class="info-cell">
        <span class="label">Last Run:</span>
        <Tooltip
          :visible="statusTooltipVisible"
          :x="statusTooltipX"
          :y="statusTooltipY"
          :text="statusTooltip"
        />
        <i
          v-if="statusIcon"
          :class="statusIcon"
          :style="{
            color: statusColor,
            marginLeft: '4px',
            marginRight: '4px',
            fontSize: '13px',
            verticalAlign: 'middle',
          }"
          @mouseenter="onStatusIconEnter"
          @mousemove="onStatusIconMove"
          @mouseleave="onStatusIconLeave"
        ></i>
        <span class="value">{{ lastRunText }}</span>
      </div>
      <div class="info-cell align-right">
        <span class="label">Next Run:</span>
        <span class="value">{{ nextRunText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import ActionMenu, { type ActionMenuItem } from '@/components/common/ActionMenu.vue';
import Tooltip from '@/components/common/Tooltip.vue';
import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import { formatTime as formatScheduleTime } from '@/utils/scheduleFormatUtils';
import { formatMetadataDate, formatDate } from '@/utils/dateUtils';
import type { JobExecutionStatus } from '@/types/ArcGISIntegrationExecution';
import { getUserTimezone } from '@/utils/timezoneUtils';

const emit = defineEmits<{ (e: 'action', id: string): void; (e: 'open', id: string): void }>();

const props = defineProps<{
  integration: ConfluenceIntegrationSummaryResponse;
  menuItems: ActionMenuItem[];
}>();

// Tooltip state for status icon
const statusTooltipVisible = ref(false);
const statusTooltipX = ref(0);
const statusTooltipY = ref(0);

function onStatusIconEnter(e: MouseEvent) {
  statusTooltipVisible.value = true;
  statusTooltipX.value = e.clientX + 12;
  statusTooltipY.value = e.clientY + 12;
}

function onStatusIconMove(e: MouseEvent) {
  statusTooltipX.value = e.clientX + 12;
  statusTooltipY.value = e.clientY + 12;
}

function onStatusIconLeave() {
  statusTooltipVisible.value = false;
}

// Confluence always runs daily; show formatted local time
const scheduleText = computed(() => {
  const rawTime = props.integration.executionTime;
  if (!rawTime) return 'Not scheduled';

  try {
    const [hhStr, mmStr] = rawTime.split(':');
    const hours = parseInt(hhStr ?? '0', 10);
    const minutes = parseInt(mmStr ?? '0', 10);
    const today = new Date();
    const utcDate = new Date(
      Date.UTC(today.getFullYear(), today.getMonth(), today.getDate(), hours, minutes, 0)
    );
    const userTz = getUserTimezone();
    const displayTime = new Intl.DateTimeFormat('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
      timeZone: userTz,
    }).format(utcDate);
    return `Daily at ${displayTime}`;
  } catch {
    return `Daily at ${formatScheduleTime(rawTime)}`;
  }
});

const dynamicDocumentTypeDisplay = computed(() => {
  const docType = props.integration.dynamicDocumentType;
  if (!docType || docType.trim() === '') return '';
  return props.integration.dynamicDocumentTypeLabel || docType;
});

const languageChipText = computed(() => {
  const codes = props.integration.languageCodes;
  if (!codes || codes.length === 0) return '';
  const MAX_VISIBLE = 3;
  const visible = codes
    .slice(0, MAX_VISIBLE)
    .map(c => c.toUpperCase())
    .join(', ');
  const remaining = codes.length - MAX_VISIBLE;
  return remaining > 0 ? `${visible} +${remaining} more` : visible;
});

const nextRunText = computed(() => {
  if (!props.integration.nextRunAtUtc) return 'Not scheduled';
  return formatDate(props.integration.nextRunAtUtc, {
    includeTime: true,
    includeSeconds: false,
    includeTimezone: false,
    format: 'short',
  });
});

const lastRunText = computed(() => {
  const lastIso = props.integration.lastAttemptTimeUtc;
  if (!lastIso) return 'Never Run';
  return formatDate(lastIso, {
    includeTime: true,
    includeSeconds: false,
    includeTimezone: false,
    format: 'short',
  });
});

const statusIcon = computed(() => {
  const status = props.integration.lastStatus as JobExecutionStatus | undefined;
  if (!status) return '';
  switch (status) {
    case 'SUCCESS':
      return 'dx-icon-check';
    case 'FAILED':
      return 'dx-icon-close';
    case 'RUNNING':
      return 'dx-icon-refresh';
    case 'ABORTED':
      return 'dx-icon-remove';
    case 'SCHEDULED':
      return 'dx-icon-clock';
    default:
      return '';
  }
});

const statusColor = computed(() => {
  const status = props.integration.lastStatus as JobExecutionStatus | undefined;
  if (!status) return '';
  switch (status) {
    case 'SUCCESS':
      return '#28a745';
    case 'FAILED':
      return '#dc3545';
    case 'RUNNING':
      return '#007bff';
    case 'ABORTED':
      return '#ff9800';
    case 'SCHEDULED':
      return '#6c757d';
    default:
      return '#6c757d';
  }
});

const statusTooltip = computed(() => {
  const status = props.integration.lastStatus as JobExecutionStatus | undefined;
  if (!status) return '';
  switch (status) {
    case 'SUCCESS':
      return 'Execution completed successfully';
    case 'FAILED':
      return 'Execution failed - check logs for details';
    case 'RUNNING':
      return 'Currently executing';
    case 'ABORTED':
      return 'Execution was manually aborted';
    case 'SCHEDULED':
      return 'Scheduled for execution - waiting for executor to pick up';
    default:
      return status;
  }
});

function onAction(id: string) {
  emit('action', id);
}

function onOpen() {
  emit('open', props.integration.id);
}
</script>

<style scoped>
/* Card-specific styles are shared via ArcGISIntegrationPage.css */
</style>
