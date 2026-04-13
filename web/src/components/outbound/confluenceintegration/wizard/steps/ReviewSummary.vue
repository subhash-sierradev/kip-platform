<template>
  <div class="rs-root">
    <div class="rs-container">
      <!-- Basic Details -->
      <section class="rs-section">
        <h3 class="rs-section-title">Basic Details</h3>
        <div class="rs-review-grid">
          <div class="rs-review-item">
            <span class="rs-label">Integration Name:</span>
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

      <!-- Document Configuration -->
      <section class="rs-section">
        <h3 class="rs-section-title">Document Configuration</h3>
        <div class="rs-review-grid">
          <div class="rs-review-item">
            <span class="rs-label">Item Type:</span>
            <span class="rs-value">{{ formData.itemType || 'Document' }}</span>
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

      <!-- Schedule -->
      <section class="rs-section">
        <h3 class="rs-section-title">
          Schedule
          <div class="rs-timezone-info">
            <i class="dx-icon-clock"></i>
            Timezone - {{ timezoneDisplay }}
          </div>
        </h3>
        <div class="rs-review-grid">
          <div class="rs-review-item">
            <span class="rs-label">Frequency:</span>
            <span class="rs-value">{{ frequencyLabel }}</span>
          </div>
          <div v-if="isDailyPattern" class="rs-review-item">
            <span class="rs-label">Interval:</span>
            <span class="rs-value">{{ dailyIntervalLabel }}</span>
          </div>
          <div v-if="isWeeklyPattern && formData.selectedDays?.length" class="rs-review-item">
            <span class="rs-label">Days:</span>
            <span class="rs-value">{{ formData.selectedDays.join(', ') }}</span>
          </div>
          <div v-if="isMonthlyPattern && formData.selectedMonths?.length" class="rs-review-item">
            <span class="rs-label">Months:</span>
            <span class="rs-value">{{ formData.selectedMonths.join(', ') }}</span>
          </div>
          <div v-if="isMonthlyPattern" class="rs-review-item">
            <span class="rs-label">Run on Last Day:</span>
            <span class="rs-value">{{
              formData.isExecuteOnMonthEnd ? 'Enabled' : 'Disabled'
            }}</span>
          </div>
          <div v-if="isCronPattern" class="rs-review-item">
            <span class="rs-label">Cron Expression:</span>
            <span class="rs-value"
              ><code>{{ formData.cronExpression }}</code></span
            >
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Execution Time:</span>
            <span class="rs-value">{{ formatTime(formData.executionTime) }}</span>
          </div>
          <div v-if="formData.executionDate" class="rs-review-item">
            <span class="rs-label">Start Date:</span>
            <span class="rs-value">{{ executionStartDateDisplay }}</span>
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

      <!-- Confluence Configuration -->
      <section class="rs-section">
        <h3 class="rs-section-title">Confluence Configuration</h3>
        <div class="rs-review-grid">
          <div class="rs-review-item">
            <span class="rs-label">Confluence Space:</span>
            <span class="rs-value">{{
              formData.confluenceSpaceLabel || formData.confluenceSpaceKey
            }}</span>
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Space Folder:</span>
            <span class="rs-value">{{
              formData.confluenceSpaceFolderLabel ||
              (formData.confluenceSpaceKey &&
              (!formData.confluenceSpaceKeyFolderKey ||
                formData.confluenceSpaceKeyFolderKey === 'ROOT' ||
                formData.confluenceSpaceKeyFolderKey === formData.confluenceSpaceKey)
                ? 'ROOT'
                : formData.confluenceSpaceKeyFolderKey) ||
              '—'
            }}</span>
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Report Name Template:</span>
            <span class="rs-value">{{ formData.reportNameTemplate }}</span>
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Languages:</span>
            <span class="rs-value">{{ languageDisplay }}</span>
          </div>
          <div class="rs-review-item">
            <span class="rs-label">Include Table of Contents:</span>
            <span class="rs-value">{{
              formData.includeTableOfContents ? 'Enabled' : 'Disabled'
            }}</span>
          </div>
        </div>
      </section>

      <!-- Duplicate name warning -->
      <div v-if="isDuplicateName" class="rs-duplicate-warning">
        ⚠ An integration with this name already exists. Please go back and choose a different name.
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  getUserTimezone,
  formatTimezoneInfo,
  convertLocalDateTimeToUtc,
} from '@/utils/timezoneUtils';
import { formatTime } from '@/utils/scheduleFormatUtils';
import { MasterDataService } from '@/api/services/MasterDataService';
import type { LanguageDto } from '@/api/models/LanguageDto';
import type { ConfluenceFormData } from '@/types/ConfluenceFormData';
import { formatScheduleExecutionDate } from '@/utils/scheduleDisplayUtils';

defineOptions({ name: 'ConfluenceReviewSummary' });

interface Props {
  formData: ConfluenceFormData;
  isDuplicateName?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'validation-change', isValid: boolean): void;
}>();

const timezoneDisplay = computed(() => formatTimezoneInfo(getUserTimezone()));

// The wizard form holds LOCAL date+time (what the user selected in their timezone).
// formatScheduleExecutionDate expects UTC inputs, so we pre-convert here.
const executionStartDateDisplay = computed(() => {
  const localDate = props.formData.executionDate;
  const localTime = props.formData.executionTime;
  if (!localDate || !localTime) return 'N/A';
  const { utcDate, utcTime } = convertLocalDateTimeToUtc(localDate, localTime);
  return formatScheduleExecutionDate(utcDate, utcTime);
});

const allLanguages = ref<LanguageDto[]>([]);

const languageDisplay = computed(() => {
  const codes = props.formData.languageCodes || [];
  if (!codes.length) return 'None selected';
  if (allLanguages.value.length) {
    const codeSet = new Map(allLanguages.value.map(l => [l.code.toLowerCase(), l]));
    return codes
      .map(c => {
        const lang = codeSet.get(c.toLowerCase());
        return lang ? `${lang.name} (${lang.nativeName})` : c.toUpperCase();
      })
      .join(', ');
  }
  return codes.map(c => c.toUpperCase()).join(', ');
});

const isDailyPattern = computed(() => props.formData.frequencyPattern === 'DAILY');
const isWeeklyPattern = computed(() => props.formData.frequencyPattern === 'WEEKLY');
const isMonthlyPattern = computed(() => props.formData.frequencyPattern === 'MONTHLY');
const isCronPattern = computed(() => props.formData.frequencyPattern === 'CUSTOM');

const frequencyLabel = computed(() => {
  switch (props.formData.frequencyPattern) {
    case 'DAILY':
      return 'Daily';
    case 'WEEKLY':
      return 'Weekly';
    case 'MONTHLY':
      return 'Monthly';
    case 'CUSTOM':
      return 'Custom (Cron)';
    default:
      return props.formData.frequencyPattern || 'Not set';
  }
});

const dailyIntervalLabel = computed(() => {
  const interval = Number(props.formData.dailyFrequency);
  if (!interval || interval <= 0) return 'Not set';
  if (interval === 1) return 'Every hour';
  if (interval === 24) return 'Once daily';
  return `Every ${interval} hours`;
});

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

// Review step is always valid from the wizard's perspective —
// the duplicate-name guard is handled by isSubmitDisabled in the wizard.
onMounted(async () => {
  emit('validation-change', true);
  try {
    allLanguages.value = await MasterDataService.getAllActiveLanguages();
  } catch {
    // non-critical — fallback to uppercase code display already handled in computed
  }
});
</script>

<style scoped src="./ReviewSummary.css"></style>
