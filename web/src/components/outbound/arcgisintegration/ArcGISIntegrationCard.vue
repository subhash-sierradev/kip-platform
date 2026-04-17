<template>
  <div class="integration-card" @click="onOpen">
    <div class="card-top-row">
      <div class="icon-box">
        <i class="dx-icon-map integration-type-icon"></i>
      </div>
      <div class="title-box">
        <div class="title-text">{{ integration.name || 'Unnamed Integration' }}</div>
        <div class="created-updated-row">
          <div class="created-text">
            <span class="label">Created:</span> {{ formatMetadataDate(integration.createdDate) }}
          </div>
          <div class="created-text">
            <span class="label">Updated:</span>
            {{ formatMetadataDate(integration.lastModifiedDate) }}
          </div>
        </div>
      </div>

      <ActionMenu :items="menuItems" @action="onAction" trigger-aria-label="Integration options" />
    </div>

    <div class="chips-row integration-chips-row">
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
        <IntegrationStatusIcon v-if="executionStatus" :status="executionStatus" />
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
import { computed } from 'vue';
import ActionMenu, { type ActionMenuItem } from '@/components/common/ActionMenu.vue';
import IntegrationStatusIcon from '@/components/common/IntegrationStatusIcon.vue';
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

const executionStatus = computed(() => {
  const exec = props.integration as unknown as ArcGISIntegrationExecutionSummary;
  return (exec.execution?.lastStatus ?? exec.lastStatus) as JobExecutionStatus | undefined;
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
