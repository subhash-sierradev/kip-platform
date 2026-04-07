<template>
  <div class="rs-root">
    <div class="rs-container">
      <section class="rs-section">
        <h3 class="rs-section-title">Basic Details</h3>
        <div class="rs-review-grid">
          <div class="rs-review-item">
            <span class="rs-label">ArcGIS Integration Name:</span>
            <span class="rs-value">{{ formData.name }}</span>
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Description:</span>
            <pre
              class="rs-value rs-description-value"
              :class="{ 'rs-placeholder-text': !formData.description }"
              >{{ formData.description || 'No description provided' }}</pre
            >
          </div>
        </div>
      </section>

      <section class="rs-section">
        <h3 class="rs-section-title">Document Selection</h3>
        <div class="rs-review-grid">
          <div class="rs-review-item">
            <span class="rs-label">Item Type:</span>
            <span class="rs-value">{{ getItemTypeLabel(formData.itemType) }}</span>
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Item Subtype:</span>
            <span class="rs-value">{{
              formData.subTypeLabel || formData.subType || 'Not specified'
            }}</span>
          </div>
          <div v-if="formData.dynamicDocument" class="rs-review-item">
            <span class="rs-label">Dynamic Document:</span>
            <span class="rs-value">{{
              formData.dynamicDocumentLabel || formData.dynamicDocument
            }}</span>
          </div>
        </div>
      </section>

      <section class="rs-section">
        <h3 class="rs-section-title">
          Schedule Configuration
          <div class="sc-timezone-info">
            <i class="dx-icon-clock"></i>
            Timezone - {{ timezoneDisplay }}
          </div>
        </h3>
        <div class="rs-review-grid">
          <div v-if="formData.frequencyPattern !== 'CUSTOM'" class="rs-review-item">
            <span class="rs-label">Execution Start Date:</span>
            <span class="rs-value">{{ executionStartDateDisplay }}</span>
          </div>
          <div v-if="formData.frequencyPattern !== 'CUSTOM'" class="rs-review-item">
            <span class="rs-label">Execution Time:</span>
            <span class="rs-value">{{ formatTime(props.formData.executionTime) }}</span>
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Frequency Pattern:</span>
            <span class="rs-value">{{ frequencyPatternDisplay }}</span>
          </div>
          <div
            v-if="formData.selectedMonths && formData.selectedMonths.length > 0"
            class="rs-review-item"
          >
            <span class="rs-label">Selected Month(s):</span>
            <span class="rs-value">{{ selectedMonthsDisplay }}</span>
          </div>
          <div
            v-if="formData.selectedMonths && formData.selectedMonths.length > 0"
            class="rs-review-item"
          >
            <span class="rs-label">Run on Last Day of Month:</span>
            <span class="rs-value">{{
              formData.isExecuteOnMonthEnd ? 'Enabled' : 'Disabled'
            }}</span>
          </div>
          <div
            v-if="
              formData.frequencyPattern === 'WEEKLY' &&
              formData.selectedDays &&
              formData.selectedDays.length > 0
            "
            class="rs-review-item"
          >
            <span class="rs-label">Selected Day(s):</span>
            <span class="rs-value">{{ selectedDaysDisplay }}</span>
          </div>
          <div v-if="formData.dailyFrequency" class="rs-review-item">
            <span class="rs-label">Daily Frequency:</span>
            <span class="rs-value">Every {{ formData.dailyFrequency }} hour(s)</span>
          </div>
          <div v-if="formData.timeCalculationMode" class="rs-review-item">
            <span class="rs-label">Data Window Mode:</span>
            <span class="rs-value">{{ dataWindowModeDisplay }}</span>
          </div>
          <div
            v-if="
              formData.businessTimeZone && formData.timeCalculationMode === 'FIXED_DAY_BOUNDARY'
            "
            class="rs-review-item"
          >
            <span class="rs-label">Business Timezone:</span>
            <span class="rs-value">{{ formData.businessTimeZone }}</span>
          </div>
        </div>
      </section>

      <section class="rs-section">
        <h3 class="rs-section-title">Field Mapping</h3>
        <div class="rs-review-grid">
          <div class="rs-review-item">
            <span class="rs-label">Total Mappings:</span>
            <span class="rs-value">{{ formData.fieldMappings.length }}</span>
          </div>
        </div>
        <div class="rs-field-mappings-summary">
          <div
            v-for="(mapping, index) in sortedFieldMappings"
            :key="`review-mapping-${index}`"
            class="rs-mapping-summary-item"
          >
            <span class="rs-mapping-index">{{ index + 1 }}.</span>
            <span class="rs-mapping-details">
              <strong>{{ mapping.sourceField || 'Source not selected' }}</strong>
              <template v-if="mapping.transformationType">
                → <span class="rs-transformation">{{ mapping.transformationType }}</span>
              </template>
              → <strong>{{ mapping.targetField || 'Target not selected' }}</strong>
              <span v-if="mapping.isMandatory" class="rs-mandatory">Required</span>
            </span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { ArcGISFormData } from '../../../../../types/ArcGISFormData';
import { getUserTimezone, formatTimezoneInfo } from '../../../../../utils/timezoneUtils';
import {
  MONTH_LABELS,
  formatSelectedDaysDisplay,
  formatDateOnlyDisplay,
} from '../../../../../utils/scheduleDisplayUtils';
import { formatTime } from '../../../../../utils/scheduleFormatUtils';

defineOptions({ name: 'ReviewSummary' });

interface Props {
  formData: ArcGISFormData;
  isActive: boolean;
  isDuplicateName: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:isActive': [value: boolean];
}>();

const localIsActive = ref(props.isActive);

const selectedMonthsDisplay = computed(() => {
  const months = props.formData.selectedMonths || [];
  if (!months.length) return '';
  // Sort months chronologically (1-12) and remove duplicates
  const sortedMonths = Array.from(new Set(months)).sort((a, b) => a - b);
  return sortedMonths.map(m => (m >= 1 && m <= 12 ? MONTH_LABELS[m - 1] : String(m))).join(', ');
});

// Display selected days using shared formatter
const selectedDaysDisplay = computed(() => formatSelectedDaysDisplay(props.formData.selectedDays));

// Timezone display formatted same as details tab/wizard chip
const timezoneDisplay = computed(() => formatTimezoneInfo(getUserTimezone()));

const frequencyPatternDisplay = computed(() => {
  const frequencyPattern = props.formData.frequencyPattern;
  if (frequencyPattern !== 'CUSTOM') {
    return frequencyPattern;
  }

  const cronExpression = props.formData.cronExpression?.trim();
  if (!cronExpression) {
    return frequencyPattern;
  }

  return `${frequencyPattern} (${cronExpression})`;
});

const executionStartDateDisplay = computed(() =>
  formatDateOnlyDisplay(props.formData.executionDate)
);

const dataWindowModeDisplay = computed(() => {
  switch (props.formData.timeCalculationMode) {
    case 'FIXED_DAY_BOUNDARY':
      return 'Daily Window';
    case 'FLEXIBLE_INTERVAL':
      return 'Rolling Window';
    default:
      return props.formData.timeCalculationMode || 'Not set';
  }
});

// Sort field mappings by displayOrder to maintain same order as Field Mapping step
const sortedFieldMappings = computed(() => {
  return [...props.formData.fieldMappings].sort(
    (a, b) => (a.displayOrder || 0) - (b.displayOrder || 0)
  );
});

// Helper function to convert raw item type to display label
const getItemTypeLabel = (itemType: string | undefined) => {
  switch (itemType) {
    case 'DOCUMENT':
      return 'Document';
    default:
      return itemType || 'Not specified';
  }
};

watch(localIsActive, newValue => {
  emit('update:isActive', newValue);
});
</script>

<style scoped src="./ReviewSummary.css"></style>
