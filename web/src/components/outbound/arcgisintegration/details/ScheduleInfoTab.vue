<template>
  <div class="schedule-info-tab">
    <div class="schedule-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="dx-icon dx-icon-clock loading-icon"></i>
        <p>Loading schedule information...</p>
      </div>

      <!-- No Schedule State -->
      <div v-else-if="!scheduleInfo" class="empty-state">
        <i class="dx-icon dx-icon-event empty-icon"></i>
        <h3>No Schedule Configured</h3>
        <p>This integration does not have a schedule configured yet.</p>
      </div>

      <!-- Schedule Information Display -->
      <div v-else class="schedule-content-minimal">
        <!-- Schedule Configuration Card -->
        <div class="info-card">
          <div class="card-header">
            <h4 class="card-title">
              <i class="dx-icon dx-icon-clock title-icon"></i>
              Schedule Configuration
            </h4>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <!-- Frequency Pattern -->
              <div class="info-row">
                <span class="label">Pattern:</span>
                <div class="value">
                  <span class="frequency-badge-minimal" :class="frequencyClass">
                    {{ frequencyLabel }}
                  </span>
                  <span v-if="frequencyDescription" class="frequency-detail">{{
                    frequencyDescription
                  }}</span>
                </div>
              </div>

              <!-- Daily Frequency Rule -->
              <div v-if="isDailyPattern && scheduleInfo.dailyExecutionInterval" class="info-row">
                <span class="label">Frequency:</span>
                <span class="value">{{
                  formatDailyInterval(scheduleInfo.dailyExecutionInterval)
                }}</span>
              </div>

              <!-- Weekly Days -->
              <div v-if="dayScheduleList.length" class="info-row">
                <span class="label">Days:</span>
                <div class="value">
                  <div class="tag-list">
                    <span v-for="day in dayScheduleList" :key="day" class="tag">{{ day }}</span>
                  </div>
                </div>
              </div>

              <!-- Monthly Months -->
              <div v-if="monthScheduleList.length" class="info-row">
                <span class="label">Months:</span>
                <div class="value">
                  <div class="tag-list">
                    <span v-for="month in monthScheduleList" :key="month" class="tag">{{
                      month
                    }}</span>
                  </div>
                </div>
              </div>

              <!-- Monthly Pattern -->
              <div v-if="isMonthlyPattern && scheduleInfo.isExecuteOnMonthEnd" class="info-row">
                <span class="label">Run on Last Day of Month:</span>
                <span class="value">{{
                  scheduleInfo.isExecuteOnMonthEnd ? 'Enabled' : 'Disabled'
                }}</span>
              </div>

              <!-- Cron Expression (only for custom/cron pattern) -->
              <div v-if="isCronPattern && scheduleInfo.cronExpression" class="info-row">
                <span class="label">Cron:</span>
                <code class="cron-minimal">{{ scheduleInfo.cronExpression }}</code>
              </div>

              <!-- Data Window Mode -->
              <div v-if="scheduleInfo.timeCalculationMode" class="info-row">
                <span class="label">Data Window Mode:</span>
                <span class="value">
                  <span class="timezone-chip">{{ timeCalcModeLabel }}</span>
                </span>
              </div>

              <!-- Business Timezone (only for Fixed Day Boundary mode) -->
              <div v-if="scheduleInfo.businessTimeZone && isFixedDayBoundaryMode" class="info-row">
                <span class="label">Business Timezone:</span>
                <span class="value">
                  <span class="timezone-chip">{{ scheduleInfo.businessTimeZone }}</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Execution Timeline Card -->
        <div class="info-card">
          <div class="card-header">
            <h4 class="card-title">
              <i class="dx-icon dx-icon-event title-icon"></i>
              Execution Timeline
            </h4>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <!-- Next Run (most prominent) -->
              <div v-if="nextRunInfo" class="info-row featured">
                <span class="label">Next Run:</span>
                <span class="value-primary">{{ nextRunInfo }}</span>
              </div>

              <!-- Start Date -->
              <div class="info-row">
                <span class="label">Start Date:</span>
                <span class="value">{{ formatStartDate }}</span>
              </div>

              <!-- Execution Time -->
              <div class="info-row">
                <span class="label">Time & Zone:</span>
                <span class="value">
                  {{ formatExecutionTime(scheduleInfo.executionTime) }}
                  <span class="timezone-chip">{{ timezoneDisplay }}</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue';
import type { HasSchedule } from '@/types/HasSchedule';
import { FrequencyPattern } from '@/api/models/FrequencyPattern';
import { sortMonthsChronologically } from '@/utils/scheduleFormatUtils';
import { formatDateOnlyDisplay } from '@/utils/scheduleDisplayUtils';
import { formatTimezoneInfo, getUserTimezone } from '@/utils/timezoneUtils';
import { formatDateUTC } from '@/utils/dateUtils';
// Removed schedule index parsing here; shared utils handle computation

const props = defineProps({
  integrationData: { type: Object as PropType<HasSchedule | null>, default: null },
  integrationId: { type: String, required: true },
  loading: { type: Boolean, default: false },
});

// Use schedule from parent integrationData instead of API call
const scheduleInfo = computed(() => props.integrationData?.schedule || null);

// Pattern checks
const isDailyPattern = computed(
  () => scheduleInfo.value?.frequencyPattern === FrequencyPattern.DAILY
);
const isMonthlyPattern = computed(
  () => scheduleInfo.value?.frequencyPattern === FrequencyPattern.MONTHLY
);
const isCronPattern = computed(
  () => scheduleInfo.value?.frequencyPattern === FrequencyPattern.CRON_EXPRESSION
);
const isFixedDayBoundaryMode = computed(
  () => scheduleInfo.value?.timeCalculationMode === 'FIXED_DAY_BOUNDARY'
);
// Computed properties for display
const frequencyLabel = computed(() => {
  if (!scheduleInfo.value?.frequencyPattern) return 'Not Set';

  switch (scheduleInfo.value.frequencyPattern) {
    case FrequencyPattern.DAILY:
      return 'Daily';
    case FrequencyPattern.WEEKLY:
      return 'Weekly';
    case FrequencyPattern.MONTHLY:
      return 'Monthly';
    case FrequencyPattern.CRON_EXPRESSION:
      return 'Custom';
    default:
      return (
        String(scheduleInfo.value.frequencyPattern).charAt(0).toUpperCase() +
        String(scheduleInfo.value.frequencyPattern).slice(1).toLowerCase()
      );
  }
});

const frequencyClass = computed(() => {
  if (!scheduleInfo.value?.frequencyPattern) return 'frequency-unknown';
  return `frequency-${String(scheduleInfo.value.frequencyPattern).toLowerCase()}`;
});

const frequencyDescription = computed(() => {
  if (!scheduleInfo.value) return '';

  const pattern = scheduleInfo.value.frequencyPattern;
  const interval = scheduleInfo.value.dailyExecutionInterval;

  switch (pattern) {
    case FrequencyPattern.DAILY:
      if (interval === 1) return 'Executes every hour';
      if (interval === 24) return 'Executes once daily';
      return `Executes every ${interval || 1} hours`;
    case FrequencyPattern.WEEKLY:
      return 'Executes on selected weekdays';
    case FrequencyPattern.MONTHLY:
      return 'Executes on selected months';
    case FrequencyPattern.CRON_EXPRESSION:
      return 'Custom schedule pattern';
    default:
      return '';
  }
});

const dayScheduleList = computed(() => {
  if (!scheduleInfo.value?.daySchedule) return [];
  const days = Array.isArray(scheduleInfo.value.daySchedule)
    ? scheduleInfo.value.daySchedule
    : String(scheduleInfo.value.daySchedule)
        .split(',')
        .map(d => d.trim());

  return days.map((day: string | number) => {
    const dayStr = String(day).toUpperCase();
    const dayMap: Record<string, string> = {
      MON: 'Monday',
      MONDAY: 'Monday',
      TUE: 'Tuesday',
      TUESDAY: 'Tuesday',
      WED: 'Wednesday',
      WEDNESDAY: 'Wednesday',
      THU: 'Thursday',
      THURSDAY: 'Thursday',
      FRI: 'Friday',
      FRIDAY: 'Friday',
      SAT: 'Saturday',
      SATURDAY: 'Saturday',
      SUN: 'Sunday',
      SUNDAY: 'Sunday',
    };
    return dayMap[dayStr] || dayStr.charAt(0).toUpperCase() + dayStr.slice(1).toLowerCase();
  });
});

const monthScheduleList = computed(() => {
  if (!scheduleInfo.value?.monthSchedule) return [];
  const months = Array.isArray(scheduleInfo.value.monthSchedule)
    ? scheduleInfo.value.monthSchedule
    : String(scheduleInfo.value.monthSchedule)
        .split(',')
        .map(m => m.trim());

  return sortMonthsChronologically(months);
});

const nextRunInfo = computed(() => {
  if (!props.integrationData) return null;
  const nextRunAtUtc = props.integrationData.nextRunAtUtc;
  if (!nextRunAtUtc) return null;

  return formatDateUTC(nextRunAtUtc, {
    includeTime: true,
    includeWeekday: true,
    includeSeconds: false,
    includeTimezone: false,
    format: 'medium',
  });
});

// Fixed: Properly format start date (date only, no time)
const formatStartDate = computed(() => {
  return formatDateOnlyDisplay(
    scheduleInfo.value?.executionDate,
    {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    },
    'Not configured',
    'Invalid Date'
  );
});

// Helper functions

const formatExecutionTime = (timeString?: string): string => {
  if (!timeString) return 'Not set';
  try {
    const [hhStr, mmStr] = timeString.split(':');
    const hours = parseInt(hhStr || '', 10);
    const minutes = parseInt(mmStr || '', 10);
    if (Number.isNaN(hours) || Number.isNaN(minutes)) return timeString;

    // Use next run date when available so DST offset matches the shown "Next Run" value.
    const dateStr =
      props.integrationData?.nextRunAtUtc?.split('T')[0] ||
      scheduleInfo.value?.executionDate ||
      new Date().toISOString().split('T')[0];
    // Treat DB time as UTC instant, then format in user's timezone
    const utcDate = new Date(
      `${dateStr}T${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:00Z`
    );
    const userTz = getUserTimezone();
    return new Intl.DateTimeFormat('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
      timeZone: userTz,
    }).format(utcDate);
  } catch {
    return timeString;
  }
};

const formatDailyInterval = (interval: number): string => {
  if (!interval || interval <= 0) return 'Invalid interval';
  if (interval === 1) return 'Every hour';
  if (interval === 24) return 'Once daily';
  return `Every ${interval} hours`;
};

// Pattern description function removed - customConfig not available in IntegrationScheduleDto

const timezoneDisplay = computed(() => `${formatTimezoneInfo(getUserTimezone())}`);

// Data Window Mode display
const timeCalcModeLabel = computed(() => {
  if (!scheduleInfo.value?.timeCalculationMode) return 'Not Set';

  switch (scheduleInfo.value.timeCalculationMode) {
    case 'FIXED_DAY_BOUNDARY':
      return 'Daily Window';
    case 'FLEXIBLE_INTERVAL':
      return 'Rolling Window';
    default:
      return scheduleInfo.value.timeCalculationMode;
  }
});
</script>

<style src="./ArcGISScheduleInfoTab.css" scoped></style>
