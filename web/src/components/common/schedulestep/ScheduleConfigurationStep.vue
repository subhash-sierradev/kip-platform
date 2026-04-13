<template>
  <div class="sc-root">
    <div class="sc-container">
      <section class="sc-section">
        <!-- ── Data Window Mode and Business Timezone ─────────────────────── -->
        <!-- step-level timezone indicator for simple mode (full mode shows it on Data Window Mode) -->
        <div v-if="mode === 'simple'" class="sc-step-timezone">
          <i class="dx-icon-clock"></i> {{ timezoneDisplay }}
        </div>

        <div class="sc-field-row">
          <div v-if="mode === 'full'" class="sc-field">
            <div class="sc-label-row">
              <label class="sc-label">Data Window Mode <span class="sc-required">*</span></label>
              <span class="sc-timezone-hint"
                ><i class="dx-icon-clock"></i> {{ timezoneDisplay }}</span
              >
            </div>
            <select class="sc-input" v-model="localData.timeCalculationMode">
              <option value="FIXED_DAY_BOUNDARY">Daily Window</option>
              <option value="FLEXIBLE_INTERVAL">Rolling Window</option>
            </select>
            <div class="sc-helper">{{ timeCalculationModeHelper }}</div>
          </div>

          <div
            v-if="mode === 'simple' || localData.timeCalculationMode === 'FIXED_DAY_BOUNDARY'"
            class="sc-field"
          >
            <label class="sc-label">Business Timezone <span class="sc-required">*</span></label>
            <DxSelectBox
              v-model:value="localData.businessTimeZone"
              :data-source="timezoneOptions"
              display-expr="label"
              value-expr="value"
              :search-enabled="true"
              :show-clear-button="false"
              styling-mode="outlined"
              placeholder="Select timezone..."
            />
            <div class="sc-helper">
              Determines schedule execution time and daily data boundaries.
            </div>
          </div>
        </div>

        <!-- ── SIMPLE MODE: daily execution time only ─────────────────────────── -->
        <template v-if="mode === 'simple'">
          <div class="sc-field-row-single">
            <div class="sc-field">
              <label class="sc-label"> Execution Time <span class="sc-required">*</span> </label>
              <input class="sc-input" v-model="executionTimeModel" type="time" />
              <div class="sc-helper">This integration runs daily at the specified time.</div>
            </div>
          </div>
        </template>

        <!-- ── FULL MODE: all schedule options ────────────────────────────────── -->
        <template v-else>
          <div class="sc-field-row">
            <div class="sc-field">
              <label class="sc-label"> Frequency Pattern <span class="sc-required">*</span> </label>
              <select class="sc-input" v-model="localData.frequencyPattern">
                <option value="">Select Frequency</option>
                <option value="DAILY">Daily</option>
                <option value="WEEKLY">Weekly</option>
                <option value="MONTHLY">Monthly</option>
                <option value="CUSTOM">CRON</option>
              </select>
            </div>

            <div v-if="localData.frequencyPattern === 'DAILY'" class="sc-field">
              <label class="sc-label">Daily Frequency<span class="sc-required">*</span></label>
              <select class="sc-input" v-model="dailyFrequencyRule">
                <option v-for="h in [1, 2, 3, 6, 12, 24]" :key="h" :value="String(h)">
                  Every {{ h }} hour{{ h > 1 ? 's' : '' }}
                </option>
              </select>
            </div>

            <div v-if="localData.frequencyPattern === 'CUSTOM'" class="sc-field">
              <label class="sc-label">
                Cron Expression (UTC)<span class="sc-required">*</span>
              </label>
              <input
                class="sc-input"
                :class="{ 'sc-input-error': cronValidationError }"
                v-model="cronModel"
                type="text"
                placeholder="e.g., 0 0 9 * * ?"
              />
              <div class="sc-helper">
                Enter the cron expression in UTC; the job runs based on UTC time.
              </div>
              <div class="sc-cron-feedback">
                <div v-if="cronValidationError" class="sc-error-message">
                  {{ cronValidationError }}
                </div>
                <div v-else-if="cronDescription" class="sc-cron-description">
                  <span class="sc-cron-description-text">{{ cronDescription.text }}</span>
                  <span v-if="cronDescription.timezoneLabel" class="sc-cron-timezone-chip">
                    <i class="dx-icon-clock" aria-hidden="true"></i>
                    <span class="sc-cron-timezone-value">{{ cronDescription.timezoneLabel }}</span>
                  </span>
                </div>
              </div>
            </div>

            <div v-if="localData.frequencyPattern === 'MONTHLY'" class="sc-field">
              <label
                class="sc-label"
                @mouseenter="
                  e => showTooltip(e, 'Execution will occur on the last day of selected months')
                "
                @mousemove="moveTooltip"
                @mouseleave="hideTooltip"
                :aria-describedby="tooltipId"
              >
                Run on Last Day of Month
              </label>
              <Tooltip
                :visible="tooltip.visible"
                :x="tooltip.x"
                :y="tooltip.y"
                :id="tooltipId"
                :text="tooltip.text"
              />
              <div class="sc-toggle-wrapper">
                <input
                  type="checkbox"
                  id="execute-on-month-end"
                  v-model="localData.isExecuteOnMonthEnd"
                  class="sc-toggle-input"
                />
                <label for="execute-on-month-end" class="sc-toggle-button">
                  <span class="sc-toggle-slider"></span>
                </label>
                <span class="sc-toggle-text">
                  {{ localData.isExecuteOnMonthEnd ? 'Enabled' : 'Disabled' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Weekly: days-of-week -->
          <div v-if="localData.frequencyPattern === 'WEEKLY'" class="sc-frequency-config">
            <div class="sc-weekdays-header">
              <label class="sc-label">Days of Week<span class="sc-required">*</span></label>
              <label class="sc-select-all">
                <input
                  type="checkbox"
                  class="sc-weekdays-checkbox"
                  :checked="isWeekdaysOnly"
                  @change="toggleWeekdaysOnly"
                />
                <span class="sc-weekdays-checkbox-label">Weekdays Only (Mon-Fri)</span>
              </label>
            </div>
            <div class="sc-day-checkboxes">
              <label
                v-for="day in daysOfWeek"
                :key="day.value"
                class="sc-day-checkbox-item"
                :class="{ 'sc-day-weekend': day.isWeekend }"
              >
                <input
                  type="checkbox"
                  class="sc-day-checkbox"
                  :value="day.value"
                  v-model="selectedDays"
                />
                <span class="sc-day-checkbox-label" :class="{ 'sc-day-weekend': day.isWeekend }">
                  {{ day.label }}
                </span>
              </label>
            </div>
          </div>

          <!-- Monthly: month selection -->
          <div v-if="localData.frequencyPattern === 'MONTHLY'" class="sc-frequency-config">
            <div class="sc-month-header">
              <label class="sc-label">Choose Month<span class="sc-required">*</span></label>
              <label class="sc-select-all">
                <input
                  type="checkbox"
                  class="sc-months-selectall"
                  :checked="isAllMonthsSelected"
                  @change="toggleAllMonths"
                />
                <span class="sc-weekdays-checkbox-label">Select All</span>
              </label>
            </div>
            <div class="sc-month-checkboxes">
              <label v-for="month in months" :key="month.value" class="sc-month-checkbox-item">
                <input
                  type="checkbox"
                  class="sc-month-checkbox"
                  :value="month.value"
                  v-model="selectedMonths"
                />
                <span class="sc-month-checkbox-label">{{ month.label }}</span>
              </label>
            </div>
          </div>

          <!-- Date + Time row (all non-CUSTOM patterns) -->
          <div v-if="localData.frequencyPattern !== 'CUSTOM'" class="sc-field-row">
            <div class="sc-field">
              <label class="sc-label">
                Execution Start Date
                <span v-if="isExecutionDateRequired" class="sc-required">*</span>
              </label>
              <input
                class="sc-input"
                :class="{ 'sc-input-disabled': isEndOfMonth }"
                v-model="executionDateModel"
                type="date"
                :disabled="isEndOfMonth"
                :placeholder="datePlaceholder"
              />
            </div>

            <div class="sc-field">
              <label class="sc-label"> Execution Time <span class="sc-required">*</span> </label>
              <input class="sc-input" v-model="executionTimeModel" type="time" />
            </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import './ScheduleConfigurationStep.css';
import { ref, watch, computed, onUnmounted } from 'vue';
import Tooltip from '@/components/common/Tooltip.vue';
import { getUserTimezone, formatTimezoneInfo, getCommonTimezones } from '@/utils/timezoneUtils';
import type { ScheduleConfigurationData } from '@/types/ArcGISFormData';
import { useTooltip } from '@/composables/useTooltip';
import { useCron } from '@/composables/cron/useCron';
import {
  cronToTextWithTimezoneUniversal,
  type ScheduleDescriptionResult,
} from '@/composables/cron/useCronOccurrences';
import { DxSelectBox } from 'devextreme-vue/select-box';

defineOptions({ name: 'ScheduleConfigurationStep' });

interface Props {
  modelValue: ScheduleConfigurationData;
  /** 'full' — all schedule options (ArcGIS); 'simple' — daily execution time only (Confluence). Default: 'full' */
  mode?: 'full' | 'simple';
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:modelValue', v: ScheduleConfigurationData): void;
  (e: 'validation-change', v: boolean): void;
}>();

const withScheduleDefaults = (
  input: ScheduleConfigurationData,
  mode: 'full' | 'simple' = 'full'
): ScheduleConfigurationData => {
  const normalized = { ...input };
  if (!normalized.timeCalculationMode) {
    normalized.timeCalculationMode = 'FLEXIBLE_INTERVAL';
  }
  if (
    !normalized.businessTimeZone &&
    (mode === 'simple' || normalized.timeCalculationMode === 'FIXED_DAY_BOUNDARY')
  ) {
    normalized.businessTimeZone = 'UTC';
  }
  if (mode === 'simple' && !normalized.frequencyPattern) {
    normalized.frequencyPattern = 'DAILY';
  }
  return normalized;
};

// Tooltip (full mode only)
const { tooltip, showTooltip, moveTooltip, hideTooltip } = useTooltip();
const tooltipId = 'end-of-month-tooltip';

// Cron (full mode only)
const { cron, isValidateCron, error } = useCron();

const localData = ref<ScheduleConfigurationData>(
  withScheduleDefaults(props.modelValue, props.mode || 'full')
);

// ── Timezone options ───────────────────────────────────────────────────
const timezoneOptions = getCommonTimezones();

const lastManualExecutionDate = ref(localData.value.executionDate || '');

// Clear businessTimeZone when switching to Flexible Interval
watch(
  () => localData.value.timeCalculationMode,
  newMode => {
    if (newMode === 'FLEXIBLE_INTERVAL') {
      localData.value.businessTimeZone = undefined;
    } else if (newMode === 'FIXED_DAY_BOUNDARY' && !localData.value.businessTimeZone) {
      localData.value.businessTimeZone = 'UTC';
    }
  }
);

// ── Full-mode computed helpers ─────────────────────────────────────────────

const isEndOfMonth = computed(
  () => localData.value.frequencyPattern === 'MONTHLY' && localData.value.isExecuteOnMonthEnd
);

const isExecutionDateRequired = computed(() => {
  const fp = localData.value.frequencyPattern;
  return fp === 'DAILY' || fp === 'WEEKLY' || (fp === 'MONTHLY' && !isEndOfMonth.value);
});

const executionDateModel = computed({
  get: () => (isEndOfMonth.value ? null : localData.value.executionDate),
  set: v => {
    localData.value.executionDate = isEndOfMonth.value ? null : v || null;
  },
});

const datePlaceholder = computed(() =>
  isEndOfMonth.value ? 'Date will be end of selected months' : 'mm-dd-yyyy'
);

watch(isEndOfMonth, v => {
  if (v) {
    if (localData.value.executionDate)
      lastManualExecutionDate.value = localData.value.executionDate;
    localData.value.executionDate = null;
  } else if (!localData.value.executionDate && lastManualExecutionDate.value) {
    localData.value.executionDate = lastManualExecutionDate.value;
  }
});

watch(
  () => localData.value.executionDate,
  v => {
    if (!isEndOfMonth.value && v) lastManualExecutionDate.value = v;
  }
);

const dailyFrequencyRule = computed({
  get: () => localData.value.dailyFrequency || '24',
  set: v => {
    localData.value.dailyFrequency = v || '24';
  },
});

const cronModel = computed({
  get: () => cron.value,
  set: v => {
    cron.value = v;
    if (localData.value.frequencyPattern === 'CUSTOM') {
      localData.value.cronExpression = v || '';
    }
  },
});

watch(
  () => localData.value.frequencyPattern,
  v => {
    if (v !== 'DAILY') localData.value.dailyFrequency = '';
    if (v !== 'WEEKLY') {
      localData.value.selectedDays = [];
      selectedDays.value = [];
    }
    if (v !== 'MONTHLY') {
      localData.value.selectedMonths = [];
      localData.value.isExecuteOnMonthEnd = false;
      selectedMonths.value = [];
    }
  }
);

watch(
  () => localData.value.frequencyPattern,
  fp => {
    cron.value = fp === 'CUSTOM' ? localData.value.cronExpression || '' : '';
  },
  { immediate: true }
);

const daysOfWeek = [
  { value: 'MON', label: 'Mon', isWeekend: false },
  { value: 'TUE', label: 'Tue', isWeekend: false },
  { value: 'WED', label: 'Wed', isWeekend: false },
  { value: 'THU', label: 'Thu', isWeekend: false },
  { value: 'FRI', label: 'Fri', isWeekend: false },
  { value: 'SAT', label: 'Sat', isWeekend: true },
  { value: 'SUN', label: 'Sun', isWeekend: true },
];

const months = [
  { value: 1, label: 'Jan' },
  { value: 2, label: 'Feb' },
  { value: 3, label: 'Mar' },
  { value: 4, label: 'Apr' },
  { value: 5, label: 'May' },
  { value: 6, label: 'Jun' },
  { value: 7, label: 'Jul' },
  { value: 8, label: 'Aug' },
  { value: 9, label: 'Sep' },
  { value: 10, label: 'Oct' },
  { value: 11, label: 'Nov' },
  { value: 12, label: 'Dec' },
];

const selectedDays = ref<string[]>(localData.value.selectedDays || []);
const selectedMonths = ref<number[]>(localData.value.selectedMonths || []);

watch(selectedDays, v => (localData.value.selectedDays = [...v]));
watch(selectedMonths, v => (localData.value.selectedMonths = [...v]));

const isWeekdaysOnly = computed(() => {
  const w = ['MON', 'TUE', 'WED', 'THU', 'FRI'].every(d => selectedDays.value.includes(d));
  const we = ['SAT', 'SUN'].some(d => selectedDays.value.includes(d));
  return w && !we;
});

const isAllMonthsSelected = computed(() => selectedMonths.value.length === months.length);

const toggleWeekdaysOnly = () => {
  selectedDays.value = isWeekdaysOnly.value ? [] : ['MON', 'TUE', 'WED', 'THU', 'FRI'];
};
const toggleAllMonths = () => {
  selectedMonths.value = isAllMonthsSelected.value ? [] : months.map(m => m.value);
};

const cronValidationError = computed(() => {
  if (localData.value.frequencyPattern !== 'CUSTOM') return '';
  if (!cron.value || isValidateCron.value) return '';
  return error.value || 'Invalid cron expression';
});

const cronDescription = computed<ScheduleDescriptionResult | null>(() => {
  if (localData.value.frequencyPattern !== 'CUSTOM' || !cron.value || !isValidateCron.value)
    return null;

  return cronToTextWithTimezoneUniversal(
    cron.value,
    getUserTimezone(),
    localData.value.executionDate || undefined
  );
});

// ── Shared: execution time ─────────────────────────────────────────────────

const executionTimeModel = computed({
  get: () => {
    const t = localData.value.executionTime;
    if (!t) return '';
    const [hh, mm] = t.split(':');
    return `${hh}:${mm}`;
  },
  set: (val: string) => {
    if (!val) {
      localData.value.executionTime = '';
      return;
    }
    const [hhStr, mmStr] = val.split(':');
    const h = parseInt(hhStr || '', 10);
    const m = parseInt(mmStr || '', 10);
    if (Number.isNaN(h) || Number.isNaN(m)) return;
    localData.value.executionTime = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  },
});

// ── Timezone ───────────────────────────────────────────────────────────────

const timezoneDisplay = computed(() => formatTimezoneInfo(getUserTimezone()));

// ── Data Window Mode helper ───────────────────────────────────────────

const timeCalculationModeHelper = computed(() => {
  const mode = localData.value.timeCalculationMode;
  if (mode === 'FIXED_DAY_BOUNDARY') {
    return 'Syncs data from midnight to midnight in your business timezone each day.';
  }
  if (mode === 'FLEXIBLE_INTERVAL') {
    return 'Syncs data from the last N hours before each job execution.';
  }
  return '';
});

// ── Validation ─────────────────────────────────────────────────────────────

const fullModeFrequencyValidations: Record<string, () => boolean> = {
  WEEKLY: () => selectedDays.value.length > 0,
  MONTHLY: () => selectedMonths.value.length > 0,
  CUSTOM: () => !!cron.value && isValidateCron.value,
};

function isBusinessTimeZoneValid(): boolean {
  const requiresBusinessTimeZone =
    props.mode === 'simple' || localData.value.timeCalculationMode === 'FIXED_DAY_BOUNDARY';
  if (!requiresBusinessTimeZone) return true;
  return !!(localData.value.businessTimeZone || '').trim();
}

function isSimpleModeValid(): boolean {
  return !!localData.value.executionTime;
}

function isFullModeValid(): boolean {
  if (!localData.value.timeCalculationMode) return false;

  const fp = localData.value.frequencyPattern;
  if (!fp) return false;
  if (fp !== 'CUSTOM' && !localData.value.executionTime) return false;
  if (isExecutionDateRequired.value && !localData.value.executionDate) return false;

  return fullModeFrequencyValidations[fp]?.() ?? true;
}

const isValid = computed(() => {
  if (!isBusinessTimeZoneValid()) return false;
  if (props.mode === 'simple') return isSimpleModeValid();
  return isFullModeValid();
});

// ── Watchers ───────────────────────────────────────────────────────────────

watch(localData, v => emit('update:modelValue', { ...v }), { deep: true });
watch(isValid, v => emit('validation-change', v), { immediate: true });

watch(
  () => props.modelValue,
  newVal => {
    const normalized = withScheduleDefaults(newVal, props.mode || 'full');
    if (JSON.stringify(normalized) !== JSON.stringify(localData.value)) {
      localData.value = normalized;
      selectedDays.value = normalized.selectedDays || [];
      selectedMonths.value = normalized.selectedMonths || [];
      cron.value = normalized.frequencyPattern === 'CUSTOM' ? normalized.cronExpression || '' : '';
    }
  }
);

// Initialise arrays
selectedDays.value = localData.value.selectedDays || [];
selectedMonths.value = localData.value.selectedMonths || [];

onUnmounted(() => {
  hideTooltip();
});
</script>
