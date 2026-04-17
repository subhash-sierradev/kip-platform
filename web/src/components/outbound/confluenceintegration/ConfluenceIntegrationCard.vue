<template>
  <div class="integration-card" @click="onOpen">
    <div class="card-top-row">
      <div class="icon-box">
        <i class="dx-icon-doc integration-type-icon"></i>
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
import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import { formatTime as formatScheduleTime } from '@/utils/scheduleFormatUtils';
import { formatMetadataDate, formatDate } from '@/utils/dateUtils';
import { getUserTimezone } from '@/utils/timezoneUtils';

const emit = defineEmits<{ (e: 'action', id: string): void; (e: 'open', id: string): void }>();

const props = defineProps<{
  integration: ConfluenceIntegrationSummaryResponse;
  menuItems: ActionMenuItem[];
}>();

// Confluence always runs daily; show formatted local time
const scheduleText = computed(() => {
  const rawTime = props.integration.executionTime;
  if (!rawTime) return 'Not scheduled';

  try {
    const [hhStr, mmStr] = rawTime.split(':');
    const hours = parseInt(hhStr ?? '0', 10);
    const minutes = parseInt(mmStr ?? '0', 10);
    // executionTime is stored as UTC; anchor to today's UTC calendar date so the
    // reconstructed instant is correct across midnight and DST boundaries.
    const dateStr = new Date().toISOString().slice(0, 10);
    const [yy, mm, dd] = dateStr.split('-').map(v => parseInt(v, 10));
    const utcDate = new Date(Date.UTC(yy, mm - 1, dd, hours, minutes, 0));
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

const executionStatus = computed(() => props.integration.lastStatus as string | undefined);

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
