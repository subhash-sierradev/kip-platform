<template>
  <div class="integration-card" @click="onOpen">
    <div class="card-top-row">
      <div class="icon-box">
        <i class="dx-icon-map" style="font-size: 24px"></i>
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
        :class="(integration.isEnabled ?? true) ? 'status-active' : 'status-disabled'"
      >
        {{ (integration.isEnabled ?? true) ? 'Enabled' : 'Disabled' }}
      </span>
      <span class="environment-chip">{{
        integration.itemSubtypeLabel || integration.itemSubtype
      }}</span>
      <span v-if="dynamicDocumentTypeDisplay" class="dynamic-doc-chip">
        {{ dynamicDocumentTypeDisplay }}
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
import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import {
  type ScheduleInfo,
  formatScheduleByPattern,
  formatTime as formatScheduleTime,
} from '@/utils/scheduleFormatUtils';
import { formatMetadataDate, formatDate } from '@/utils/dateUtils';
import type {
  ArcGISIntegrationExecutionSummary,
  JobExecutionStatus,
} from '@/types/ArcGISIntegrationExecution';
import { getUserTimezone, getLocalDateString } from '@/utils/timezoneUtils';
import { getScheduleTextForCustomPattern } from '@/utils/cronTextFormatter';

const emit = defineEmits<{ (e: 'action', id: string): void; (e: 'open', id: string): void }>();

const props = defineProps<{
  integration: ArcGISIntegrationSummaryResponse;
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

const scheduleText = computed(() => {
  const info = props.integration as unknown as ScheduleInfo & {
    frequencyPattern?: string;
    executionDate?: string;
  };
  if (!info.frequencyPattern) return 'Not scheduled';

  // For CUSTOM cron patterns, use cron expression text
  if (String(info.frequencyPattern).toUpperCase() === 'CUSTOM') {
    return getScheduleTextForCustomPattern(props.integration);
  }

  // executionDate is the UTC calendar date; use local date as fallback so the display
  // stays consistent with the user's day even when no date has been saved yet.
  const dateStr = info.executionDate || getLocalDateString();
  const rawTime = info.executionTime || '09:00:00';
  let displayTime: string;
  try {
    const [yy, mm, dd] = dateStr.split('-').map(v => parseInt(v, 10));
    const [hhStr, mmStr] = rawTime.split(':');
    const hours = parseInt(hhStr || '0', 10);
    const minutes = parseInt(mmStr || '0', 10);
    const utcDate = new Date(Date.UTC(yy, mm - 1, dd, hours, minutes, 0));
    const userTz = getUserTimezone();
    displayTime = new Intl.DateTimeFormat('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
      timeZone: userTz,
    }).format(utcDate);
  } catch {
    // Fallback to utility time formatter if parsing fails
    displayTime = formatScheduleTime(rawTime);
  }
  return formatScheduleByPattern(String(info.frequencyPattern), info, displayTime);
});

const dynamicDocumentTypeDisplay = computed(() => {
  const docType = props.integration.dynamicDocumentType;
  // Only display if dynamicDocumentType is configured
  if (!docType || docType.trim() === '') return '';

  // Prefer label over raw value
  return props.integration.dynamicDocumentTypeLabel || docType;
});

const nextRunText = computed(() => {
  const nextRunAtUtc = props.integration.nextRunAtUtc;
  if (!nextRunAtUtc) return 'Not scheduled';

  return formatDate(nextRunAtUtc, {
    includeTime: true,
    includeSeconds: false,
    includeTimezone: false,
    format: 'short',
  });
});

const lastRunText = computed(() => {
  const exec = props.integration as unknown as ArcGISIntegrationExecutionSummary;
  const lastSuccessIso = exec.execution?.lastSuccessTimeUtc ?? exec.lastSuccessTimeUtc;
  const lastAttemptIso = exec.execution?.lastAttemptTimeUtc ?? exec.lastAttemptTimeUtc;
  const lastIso = lastSuccessIso ?? lastAttemptIso;
  if (lastIso) {
    return formatDate(lastIso, {
      includeTime: true,
      includeSeconds: false,
      includeTimezone: false,
      format: 'short',
    });
  }
  return 'Never Run';
});

const statusIcon = computed(() => {
  const exec = props.integration as unknown as ArcGISIntegrationExecutionSummary;
  const status = (exec.execution?.lastStatus ?? exec.lastStatus) as JobExecutionStatus | undefined;

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
  const exec = props.integration as unknown as ArcGISIntegrationExecutionSummary;
  const status = (exec.execution?.lastStatus ?? exec.lastStatus) as JobExecutionStatus | undefined;

  if (!status) return '';

  switch (status) {
    case 'SUCCESS':
      return '#28a745'; // Green
    case 'FAILED':
      return '#dc3545'; // Red
    case 'RUNNING':
      return '#007bff'; // Blue
    case 'ABORTED':
      return '#ff9800'; // Orange
    case 'SCHEDULED':
      return '#6c757d'; // Gray
    default:
      return '#6c757d';
  }
});

const statusTooltip = computed(() => {
  const exec = props.integration as unknown as ArcGISIntegrationExecutionSummary;
  const status = (exec.execution?.lastStatus ?? exec.lastStatus) as JobExecutionStatus | undefined;

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
  // prevent card click from triggering
  emit('action', id);
}

function onOpen() {
  emit('open', props.integration.id);
}
</script>

<style scoped>
/* Card-specific styles can be shared via ArcGISIntegrationPage.css */
</style>
